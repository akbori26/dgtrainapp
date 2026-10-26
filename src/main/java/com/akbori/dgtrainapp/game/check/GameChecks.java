package com.akbori.dgtrainapp.game.check;

import com.akbori.dgtrainapp.game.GoldRules;
import com.akbori.dgtrainapp.game.LifeState;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.PlayerStatus;

import java.util.List;

/**
 * The game's rules as ready-made checks (factory methods), plus picking an ad or item by its board number.
 * Read-only views (board, status, history, market) need no checks; actions that call the server do,
 * because the server rejects every request for a finished game.
 *
 * <pre>
 * GameChecks.canPlay().check(status).orThrow();
 * CachedAd ad = GameChecks.pick(ads, number, CheckFailure.BOARD_EMPTY);
 * GameChecks.canSolve(status.getTurn()).check(ad).orThrow();
 * </pre>
 */
public final class GameChecks {

    private GameChecks() {}

    /**
     * Passes when a game is selected (from the request or LAST_GAME_ID in app_config).
     */
    public static GameCheck<String> gameSelected() {
        return gameId -> gameId == null || gameId.isBlank()
                ? CheckResult.fail(CheckFailure.NO_ACTIVE_GAME)
                : CheckResult.PASSED;
    }

    /**
     * Passes when a player status row exists for the game.
     */
    public static GameCheck<PlayerStatus> playerStored() {
        return status -> status == null
                ? CheckResult.fail(CheckFailure.PLAYER_STATUS_MISSING)
                : CheckResult.PASSED;
    }

    /**
     * Passes while the dragon has lives left.
     */
    public static GameCheck<PlayerStatus> alive() {
        return status -> LifeState.of(status.getLives()).isGameOver()
                ? CheckResult.fail(CheckFailure.GAME_OVER)
                : CheckResult.PASSED;
    }

    /**
     * Rules for every action that calls the server: a stored player that is still alive.
     */
    public static GameCheck<PlayerStatus> canPlay() {
        return playerStored().and(alive());
    }

    /**
     * Passes for an ad not solved before in this game.
     */
    public static GameCheck<CachedAd> adNotUsed() {
        return ad -> ad.isUsed()
                ? CheckResult.fail(CheckFailure.AD_ALREADY_USED, ad.getAdId())
                : CheckResult.PASSED;
    }

    /**
     * @param currentTurn Player turn now
     * @return A check that passes while the ad's expiry countdown is above 0
     */
    public static GameCheck<CachedAd> adNotExpired(int currentTurn) {
        return ad -> ad.isExpired(currentTurn)
                ? CheckResult.fail(CheckFailure.AD_EXPIRED, ad.getAdId())
                : CheckResult.PASSED;
    }

    /**
     * Rules for solving an ad: not used yet and not expired.
     *
     * @param currentTurn Player turn now
     */
    public static GameCheck<CachedAd> canSolve(int currentTurn) {
        return adNotUsed().and(adNotExpired(currentTurn));
    }

    /**
     * Rule for buying an item: the player has enough gold.
     *
     * @param status The buyer's stored status
     */
    public static GameCheck<CachedItem> canAfford(PlayerStatus status) {
        return item -> GoldRules.canAfford(status.getGold(), item.getCost())
                ? CheckResult.PASSED
                : CheckResult.fail(CheckFailure.NOT_ENOUGH_GOLD, item.getName(), item.getCost(),
                        status.getGold(), GoldRules.shortfall(status.getGold(), item.getCost()));
    }

    /**
     * Picks an ad or item by the number shown on the board, which starts from 1.
     *
     * @param entries      The stored board, in display order
     * @param number       Board number the player chose
     * @param emptyFailure Failure to report when nothing is stored (BOARD_EMPTY or MARKET_EMPTY)
     * @return The chosen entry
     * @throws GameRuleException if the board is empty or the number is out of range
     */
    public static <E> E pick(List<E> entries, int number, CheckFailure emptyFailure) {
        if (entries.isEmpty()) CheckResult.fail(emptyFailure).orThrow();
        if (number < 1 || number > entries.size()) {
            CheckResult.fail(CheckFailure.INDEX_OUT_OF_RANGE, entries.size()).orThrow();
        }
        return entries.get(number - 1);
    }
}