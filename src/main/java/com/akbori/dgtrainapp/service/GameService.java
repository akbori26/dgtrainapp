package com.akbori.dgtrainapp.service;

import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.ReputationResponse;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.game.BoardFactory;
import com.akbori.dgtrainapp.game.HistoryEntryFactory;
import com.akbori.dgtrainapp.game.PlayerStatusUpdater;
import com.akbori.dgtrainapp.game.check.CheckFailure;
import com.akbori.dgtrainapp.game.check.CheckResult;
import com.akbori.dgtrainapp.game.check.GameChecks;
import com.akbori.dgtrainapp.game.check.GameRuleException;
import com.akbori.dgtrainapp.model.AppConfig;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.MissionLog;
import com.akbori.dgtrainapp.model.PlayerStatus;
import com.akbori.dgtrainapp.repository.AppConfigRepository;
import com.akbori.dgtrainapp.repository.CachedAdRepository;
import com.akbori.dgtrainapp.repository.CachedItemRepository;
import com.akbori.dgtrainapp.repository.MissionLogRepository;
import com.akbori.dgtrainapp.repository.PlayerStatusRepository;
import com.akbori.dgtrainapp.web.dto.ActionResult;
import com.akbori.dgtrainapp.web.dto.AdView;
import com.akbori.dgtrainapp.web.dto.GameStatusView;
import com.akbori.dgtrainapp.web.dto.HistoryEntryView;
import com.akbori.dgtrainapp.web.dto.PurchaseOutcome;
import com.akbori.dgtrainapp.web.dto.ShopItemView;
import com.akbori.dgtrainapp.web.dto.SolveOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One method per player action. Each checks the game rules, calls the Mugloar server when needed,
 * stores the result in SQLite and returns a view for the front end.
 * Broken rules throw GameRuleException and server failures throw MugloarApiException.
 */
@Service
public class GameService {

    static final String LAST_GAME_ID = "LAST_GAME_ID";

    private final MugloarService mugloarService;
    private final PlayerStatusRepository playerRepo;
    private final MissionLogRepository logRepo;
    private final AppConfigRepository configRepo;
    private final CachedAdRepository adRepo;
    private final CachedItemRepository itemRepo;

    public GameService(MugloarService mugloarService, PlayerStatusRepository playerRepo,
                       MissionLogRepository logRepo, AppConfigRepository configRepo,
                       CachedAdRepository adRepo, CachedItemRepository itemRepo) {
        this.mugloarService = mugloarService;
        this.playerRepo = playerRepo;
        this.logRepo = logRepo;
        this.configRepo = configRepo;
        this.adRepo = adRepo;
        this.itemRepo = itemRepo;
    }

    /**
     * Starts a new game, stores its first status and remembers it as the game to resume.
     */
    @Transactional
    public GameStatusView startGame() {
        GameStartResponse res = mugloarService.startNewGame();
        PlayerStatus status = playerRepo.save(PlayerStatusUpdater.fromGameStart(res));
        configRepo.save(new AppConfig(LAST_GAME_ID, res.gameId()));
        return statusView(status);
    }

    /**
     * @return The status of the last game started, for resuming
     * @throws GameRuleException NO_ACTIVE_GAME if no game was started yet
     */
    @Transactional(readOnly = true)
    public GameStatusView currentGame() {
        String gameId = configRepo.findById(LAST_GAME_ID).map(AppConfig::getConfigValue).orElse(null);
        return statusView(requireStatus(gameId));
    }

    @Transactional(readOnly = true)
    public GameStatusView status(String gameId) {
        return statusView(requireStatus(gameId));
    }

    /**
     * @return The stored message board; empty until it is downloaded with refreshBoard()
     */
    @Transactional(readOnly = true)
    public List<AdView> board(String gameId) {
        PlayerStatus status = requireStatus(gameId);
        return adRepo.findByGameIdOrderByDisplayIndexAsc(gameId).stream()
                .map(ad -> AdView.of(ad, status.getTurn()))
                .toList();
    }

    /**
     * Downloads a new message board and replaces the stored one. If the server sends no ads,
     * the stored board is kept.
     */
    @Transactional
    public List<AdView> refreshBoard(String gameId) {
        PlayerStatus status = requirePlayable(gameId);
        List<CachedAd> stored = adRepo.findByGameIdOrderByDisplayIndexAsc(gameId);
        BoardFactory.messageBoard(gameId, mugloarService.getMessageBoard(gameId), stored, status.getTurn())
                .ifPresent(rows -> {
                    adRepo.deleteByGameId(gameId);
                    adRepo.saveAll(rows);
                });
        return board(gameId);
    }

    /**
     * Sends the dragon on an ad from the stored board, logs the attempt, marks the ad as used
     * and updates the player.
     */
    @Transactional
    public ActionResult<SolveOutcome> solveAd(String gameId, String adId) {
        PlayerStatus status = requirePlayable(gameId);
        CachedAd ad = adRepo.findByGameIdAndAdId(gameId, adId)
                .orElseThrow(() -> new GameRuleException(CheckResult.fail(CheckFailure.AD_NOT_FOUND, adId)));
        GameChecks.canSolve(status.getTurn()).check(ad).orThrow();

        SolutionResponse res = mugloarService.solveAd(gameId, adId);

        logRepo.save(HistoryEntryFactory.mission(gameId, ad, res));
        ad.setUsed(true);
        adRepo.save(ad);
        PlayerStatusUpdater.applySolution(status, res);
        playerRepo.save(status);

        return new ActionResult<>(new SolveOutcome(adId, res.success(), ad.getReward(), res.message()), statusView(status));
    }

    /**
     * Investigates the dragon's reputation, which costs a turn.
     */
    @Transactional
    public ActionResult<ReputationResponse> investigateReputation(String gameId) {
        PlayerStatus status = requirePlayable(gameId);
        ReputationResponse reputation = mugloarService.investigateReputation(gameId);
        PlayerStatusUpdater.applyReputationInvestigated(status);
        playerRepo.save(status);
        return new ActionResult<>(reputation, statusView(status));
    }

    /**
     * @return The shop items; downloaded from the server the first time for each game, then read from SQLite
     */
    @Transactional
    public List<ShopItemView> shop(String gameId) {
        PlayerStatus status = requireStatus(gameId);
        List<CachedItem> items = itemRepo.findByGameIdOrderByDisplayIndexAsc(gameId);
        if (BoardFactory.marketNeedsDownload(items)) {
            items = itemRepo.saveAll(BoardFactory.market(gameId, mugloarService.getMarketItems(gameId)));
        }
        return items.stream().map(item -> ShopItemView.of(item, status.getGold())).toList();
    }

    /**
     * Buys an item from the stored shop, logs the purchase and updates the player.
     */
    @Transactional
    public ActionResult<PurchaseOutcome> buyItem(String gameId, String itemId) {
        PlayerStatus status = requirePlayable(gameId);
        CachedItem item = itemRepo.findByGameIdAndItemId(gameId, itemId)
                .orElseThrow(() -> new GameRuleException(CheckResult.fail(CheckFailure.ITEM_NOT_FOUND, itemId)));
        GameChecks.canAfford(status).check(item).orThrow();

        TradeResponse res = mugloarService.buyItem(gameId, itemId);

        MissionLog entry = logRepo.save(HistoryEntryFactory.purchase(gameId, item, res));
        PlayerStatusUpdater.applyTrade(status, res);
        playerRepo.save(status);

        PurchaseOutcome outcome = new PurchaseOutcome(itemId, item.getName(), item.getCost(),
                res.shoppingSuccess(), entry.getServerMessage());
        return new ActionResult<>(outcome, statusView(status));
    }

    /**
     * @return Missions and purchases of the game, newest first
     */
    @Transactional(readOnly = true)
    public List<HistoryEntryView> history(String gameId) {
        requireStatus(gameId);
        return logRepo.findByGameIdOrderByIdDesc(gameId).stream().map(HistoryEntryView::of).toList();
    }

    // A game ID was given and its player status is stored
    private PlayerStatus requireStatus(String gameId) {
        GameChecks.gameSelected().check(gameId).orThrow();
        PlayerStatus status = playerRepo.findById(gameId).orElse(null);
        GameChecks.playerStored().check(status).orThrow();
        return status;
    }

    // As requireStatus, and the dragon is still alive, for actions the server would reject for a finished game
    private PlayerStatus requirePlayable(String gameId) {
        PlayerStatus status = requireStatus(gameId);
        GameChecks.alive().check(status).orThrow();
        return status;
    }

    private GameStatusView statusView(PlayerStatus status) {
        return GameStatusView.of(status, PlayerStatusUpdater.highScore(status, playerRepo.findTopByOrderByScoreDesc()));
    }
}
