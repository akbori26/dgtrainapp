package com.akbori.dgtrainapp.game.check;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.PlayerStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameChecksTest {

    private static PlayerStatus player(int lives, int gold) {
        return new PlayerStatus("g", lives, gold, 0, 0, 0);
    }

    // Downloaded at turn 0 with 3 turns left
    private static CachedAd ad(boolean used) {
        CachedAd ad = new CachedAd("g", 0, new AdItem("a1", "Help a farmer", 25, 3, null, "Risky"), 0);
        ad.setUsed(used);
        return ad;
    }

    @Test
    void gameSelectedNeedsAGameId() {
        assertThat(GameChecks.gameSelected().check("g").passed()).isTrue();
        assertThat(GameChecks.gameSelected().check(null).failure()).isEqualTo(CheckFailure.NO_ACTIVE_GAME);
        assertThat(GameChecks.gameSelected().check(" ").failure()).isEqualTo(CheckFailure.NO_ACTIVE_GAME);
    }

    @Test
    void canPlayReportsTheFirstBrokenRule() {
        // Missing status is reported before lives are read, so a null status can't throw
        assertThat(GameChecks.canPlay().check(null).failure()).isEqualTo(CheckFailure.PLAYER_STATUS_MISSING);
        assertThat(GameChecks.canPlay().check(player(0, 0)).failure()).isEqualTo(CheckFailure.GAME_OVER);
        assertThat(GameChecks.canPlay().check(player(1, 0)).passed()).isTrue();
    }

    @Test
    void canSolveRefusesUsedAndExpiredAds() {
        assertThat(GameChecks.canSolve(0).check(ad(false)).passed()).isTrue();
        assertThat(GameChecks.canSolve(0).check(ad(true)).failure()).isEqualTo(CheckFailure.AD_ALREADY_USED);
        assertThat(GameChecks.canSolve(3).check(ad(false)).failure()).isEqualTo(CheckFailure.AD_EXPIRED);
    }

    @Test
    void canAffordExplainsHowMuchGoldIsMissing() {
        CachedItem item = new CachedItem("g", 0, new ShopItem("hpot", "Healing potion", 50));

        assertThat(GameChecks.canAfford(player(3, 50)).check(item).passed()).isTrue();

        CheckResult result = GameChecks.canAfford(player(3, 30)).check(item);
        assertThat(result.failure()).isEqualTo(CheckFailure.NOT_ENOUGH_GOLD);
        assertThat(result.message()).isEqualTo("Healing potion costs 50 gold, but you only have 30. You need 20 more.");
    }

    @Test
    void pickUsesBoardNumbersStartingFromOne() {
        assertThat(GameChecks.pick(List.of("first", "second"), 2, CheckFailure.BOARD_EMPTY)).isEqualTo("second");
    }

    @Test
    void pickRejectsEmptyBoardsAndNumbersOutOfRange() {
        assertThatThrownBy(() -> GameChecks.pick(List.of(), 1, CheckFailure.MARKET_EMPTY))
                .isInstanceOf(GameRuleException.class)
                .extracting(e -> ((GameRuleException) e).getFailure()).isEqualTo(CheckFailure.MARKET_EMPTY);
        assertThatThrownBy(() -> GameChecks.pick(List.of("only"), 0, CheckFailure.BOARD_EMPTY))
                .hasMessage("Pick a number from 1 to 1.");
        assertThatThrownBy(() -> GameChecks.pick(List.of("only"), 2, CheckFailure.BOARD_EMPTY))
                .isInstanceOf(GameRuleException.class);
    }

    @Test
    void orThrowDoesNothingWhenTheCheckPassed() {
        CheckResult.PASSED.orThrow();
    }
}