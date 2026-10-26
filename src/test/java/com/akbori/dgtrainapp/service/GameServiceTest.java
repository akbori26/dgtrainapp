package com.akbori.dgtrainapp.service;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.ReputationResponse;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.game.LifeState;
import com.akbori.dgtrainapp.game.check.CheckFailure;
import com.akbori.dgtrainapp.game.check.GameRuleException;
import com.akbori.dgtrainapp.model.AppConfig;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;
import com.akbori.dgtrainapp.model.PlayerStatus;
import com.akbori.dgtrainapp.model.RiskCategory;
import com.akbori.dgtrainapp.repository.AppConfigRepository;
import com.akbori.dgtrainapp.repository.CachedAdRepository;
import com.akbori.dgtrainapp.repository.CachedItemRepository;
import com.akbori.dgtrainapp.repository.MissionLogRepository;
import com.akbori.dgtrainapp.repository.PlayerStatusRepository;
import com.akbori.dgtrainapp.web.dto.ActionResult;
import com.akbori.dgtrainapp.web.dto.AdView;
import com.akbori.dgtrainapp.web.dto.GameStatusView;
import com.akbori.dgtrainapp.web.dto.PurchaseOutcome;
import com.akbori.dgtrainapp.web.dto.ShopItemView;
import com.akbori.dgtrainapp.web.dto.SolveOutcome;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private static final String GAME = "g1";

    @Mock private MugloarService mugloarService;
    @Mock private PlayerStatusRepository playerRepo;
    @Mock private MissionLogRepository logRepo;
    @Mock private AppConfigRepository configRepo;
    @Mock private CachedAdRepository adRepo;
    @Mock private CachedItemRepository itemRepo;

    @InjectMocks private GameService gameService;

    private PlayerStatus storedPlayer(int lives, int gold, int turn) {
        PlayerStatus status = new PlayerStatus(GAME, lives, gold, 0, 0, turn);
        when(playerRepo.findById(GAME)).thenReturn(Optional.of(status));
        return status;
    }

    // Downloaded at turn 0 with 3 turns left
    private static CachedAd ad(String adId) {
        return new CachedAd(GAME, 0, new AdItem(adId, "Help a farmer", 25, 3, null, "Piece of cake"), 0);
    }

    private static CachedItem item(String itemId, int cost) {
        return new CachedItem(GAME, 0, new ShopItem(itemId, "Healing potion", cost));
    }

    private static void assertRefused(ThrowingCallable call, CheckFailure failure) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(GameRuleException.class, e -> assertThat(e.getFailure()).isEqualTo(failure));
    }

    // --- Start and resume ---

    @Test
    void startGameStoresTheStatusAndRemembersTheGame() {
        when(mugloarService.startNewGame()).thenReturn(new GameStartResponse(GAME, 3, 0, 0, 0, 0, 0));
        when(playerRepo.save(any())).thenAnswer(call -> call.getArgument(0));

        GameStatusView view = gameService.startGame();

        assertThat(view.gameId()).isEqualTo(GAME);
        assertThat(view.lives()).isEqualTo(3);
        assertThat(view.lifeState()).isEqualTo(LifeState.HEALTHY);
        ArgumentCaptor<AppConfig> config = ArgumentCaptor.forClass(AppConfig.class);
        verify(configRepo).save(config.capture());
        assertThat(config.getValue().getConfigValue()).isEqualTo(GAME);
    }

    @Test
    void currentGameIsRefusedWhenNoGameWasStarted() {
        when(configRepo.findById(GameService.LAST_GAME_ID)).thenReturn(Optional.empty());

        assertRefused(() -> gameService.currentGame(), CheckFailure.NO_ACTIVE_GAME);
    }

    @Test
    void currentGameReturnsTheLastGameStarted() {
        when(configRepo.findById(GameService.LAST_GAME_ID)).thenReturn(Optional.of(new AppConfig(GameService.LAST_GAME_ID, GAME)));
        storedPlayer(1, 10, 4);

        GameStatusView view = gameService.currentGame();

        assertThat(view.turn()).isEqualTo(4);
        assertThat(view.lifeState()).isEqualTo(LifeState.CRITICAL);
        assertThat(view.warning()).contains("Healing potion");
    }

    @Test
    void statusIsRefusedForAnUnknownGame() {
        when(playerRepo.findById("nope")).thenReturn(Optional.empty());

        assertRefused(() -> gameService.status("nope"), CheckFailure.PLAYER_STATUS_MISSING);
    }

    @Test
    void highScoreIsTheBestStoredScore() {
        storedPlayer(3, 0, 0);
        when(playerRepo.findTopByOrderByScoreDesc()).thenReturn(Optional.of(new PlayerStatus("old", 0, 0, 500, 0, 0)));

        assertThat(gameService.status(GAME).highScore()).isEqualTo(500);
    }

    // --- Message board ---

    @Test
    void boardShowsTurnsLeftAndWhetherAnAdCanBeSolved() {
        storedPlayer(3, 0, 1);
        CachedAd used = ad("a2");
        used.setUsed(true);
        when(adRepo.findByGameIdOrderByDisplayIndexAsc(GAME)).thenReturn(List.of(ad("a1"), used));

        List<AdView> board = gameService.board(GAME);

        assertThat(board).extracting(AdView::turnsLeft).containsExactly(2, 2);
        assertThat(board).extracting(AdView::solvable).containsExactly(true, false);
        assertThat(board.get(0).risk().code()).isEqualTo(RiskCategory.SAFE);
    }

    @Test
    void refreshBoardReplacesTheStoredBoard() {
        storedPlayer(3, 0, 2);
        when(mugloarService.getMessageBoard(GAME)).thenReturn(List.of(new AdItem("new", "Escort", 40, 5, null, "Risky")));

        gameService.refreshBoard(GAME);

        verify(adRepo).deleteByGameId(GAME);
        verify(adRepo).saveAll(anyList());
    }

    @Test
    void refreshBoardKeepsTheStoredBoardWhenNothingIsDownloaded() {
        storedPlayer(3, 0, 2);
        when(mugloarService.getMessageBoard(GAME)).thenReturn(List.of());

        gameService.refreshBoard(GAME);

        verify(adRepo, never()).deleteByGameId(anyString());
    }

    // --- Solve ---

    @Test
    void solveAdLogsMarksUsedAndUpdatesThePlayer() {
        PlayerStatus status = storedPlayer(3, 0, 0);
        CachedAd ad = ad("a1");
        when(adRepo.findByGameIdAndAdId(GAME, "a1")).thenReturn(Optional.of(ad));
        when(mugloarService.solveAd(GAME, "a1")).thenReturn(new SolutionResponse(true, 3, 25, 10, 0, 1, "Done!"));

        ActionResult<SolveOutcome> result = gameService.solveAd(GAME, "a1");

        assertThat(result.outcome().success()).isTrue();
        assertThat(result.outcome().reward()).isEqualTo(25);
        assertThat(result.status().gold()).isEqualTo(25);
        assertThat(ad.isUsed()).isTrue();
        assertThat(status.getTurn()).isEqualTo(1);
        verify(adRepo).save(ad);
        verify(playerRepo).save(status);
        ArgumentCaptor<MissionLog> entry = ArgumentCaptor.forClass(MissionLog.class);
        verify(logRepo).save(entry.capture());
        assertThat(entry.getValue().getEntryType()).isEqualTo(EntryType.MISSION);
        assertThat(entry.getValue().getRiskCategory()).isEqualTo(RiskCategory.SAFE);
    }

    @Test
    void solveAdRefusesAnExpiredAdWithoutCallingTheServer() {
        storedPlayer(3, 0, 3);
        when(adRepo.findByGameIdAndAdId(GAME, "a1")).thenReturn(Optional.of(ad("a1")));

        assertRefused(() -> gameService.solveAd(GAME, "a1"), CheckFailure.AD_EXPIRED);
        verify(mugloarService, never()).solveAd(anyString(), anyString());
    }

    @Test
    void solveAdRefusesAnAdNotOnTheBoard() {
        storedPlayer(3, 0, 0);
        when(adRepo.findByGameIdAndAdId(GAME, "missing")).thenReturn(Optional.empty());

        assertRefused(() -> gameService.solveAd(GAME, "missing"), CheckFailure.AD_NOT_FOUND);
    }

    @Test
    void actionsAreRefusedOnceTheGameIsOver() {
        storedPlayer(0, 100, 0);

        assertRefused(() -> gameService.solveAd(GAME, "a1"), CheckFailure.GAME_OVER);
        assertRefused(() -> gameService.refreshBoard(GAME), CheckFailure.GAME_OVER);
        assertRefused(() -> gameService.investigateReputation(GAME), CheckFailure.GAME_OVER);
        assertRefused(() -> gameService.buyItem(GAME, "hpot"), CheckFailure.GAME_OVER);
        verify(mugloarService, never()).solveAd(anyString(), anyString());
    }

    // --- Reputation ---

    @Test
    void reputationCostsATurn() {
        PlayerStatus status = storedPlayer(3, 0, 5);
        when(mugloarService.investigateReputation(GAME)).thenReturn(new ReputationResponse(1, 0, -1));

        ActionResult<ReputationResponse> result = gameService.investigateReputation(GAME);

        assertThat(result.outcome().people()).isEqualTo(1);
        assertThat(result.status().turn()).isEqualTo(6);
        verify(playerRepo).save(status);
    }

    // --- Shop ---

    @Test
    void shopIsDownloadedTheFirstTimeAndMarksAffordableItems() {
        storedPlayer(3, 60, 0);
        when(itemRepo.findByGameIdOrderByDisplayIndexAsc(GAME)).thenReturn(List.of());
        when(mugloarService.getMarketItems(GAME)).thenReturn(List.of(
                new ShopItem("hpot", "Healing potion", 50), new ShopItem("wax", "Copper plating", 100)));
        when(itemRepo.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        List<ShopItemView> shop = gameService.shop(GAME);

        assertThat(shop).extracting(ShopItemView::itemId).containsExactly("hpot", "wax");
        assertThat(shop).extracting(ShopItemView::affordable).containsExactly(true, false);
    }

    @Test
    void shopIsReadFromTheDatabaseOnceStored() {
        storedPlayer(3, 60, 0);
        when(itemRepo.findByGameIdOrderByDisplayIndexAsc(GAME)).thenReturn(List.of(item("hpot", 50)));

        assertThat(gameService.shop(GAME)).hasSize(1);
        verify(mugloarService, never()).getMarketItems(anyString());
    }

    @Test
    void buyItemRefusesWithoutEnoughGoldWithoutCallingTheServer() {
        storedPlayer(3, 30, 0);
        when(itemRepo.findByGameIdAndItemId(GAME, "hpot")).thenReturn(Optional.of(item("hpot", 50)));

        assertRefused(() -> gameService.buyItem(GAME, "hpot"), CheckFailure.NOT_ENOUGH_GOLD);
        verify(mugloarService, never()).buyItem(anyString(), anyString());
    }

    @Test
    void buyItemLogsThePurchaseAndUpdatesThePlayer() {
        PlayerStatus status = storedPlayer(1, 60, 0);
        when(itemRepo.findByGameIdAndItemId(GAME, "hpot")).thenReturn(Optional.of(item("hpot", 50)));
        when(mugloarService.buyItem(GAME, "hpot")).thenReturn(new TradeResponse(true, 10, 2, 0, 1));
        when(logRepo.save(any())).thenAnswer(call -> call.getArgument(0));

        ActionResult<PurchaseOutcome> result = gameService.buyItem(GAME, "hpot");

        assertThat(result.outcome().message()).isEqualTo("Bought Healing potion for 50 gold");
        assertThat(result.status().gold()).isEqualTo(10);
        assertThat(result.status().lives()).isEqualTo(2);
        verify(playerRepo).save(status);
    }

    @Test
    void buyItemRefusesAnItemNotInTheShop() {
        storedPlayer(3, 60, 0);
        when(itemRepo.findByGameIdAndItemId(GAME, "nope")).thenReturn(Optional.empty());

        assertRefused(() -> gameService.buyItem(GAME, "nope"), CheckFailure.ITEM_NOT_FOUND);
    }
}
