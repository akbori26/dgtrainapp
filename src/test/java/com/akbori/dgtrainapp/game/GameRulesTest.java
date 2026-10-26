package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;
import com.akbori.dgtrainapp.model.RiskCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// AdExpiry, GoldRules and LifeState: the small calculations behind the checks
class GameRulesTest {

    @Test
    void expiryCountsDownFromTheTurnTheBoardWasDownloaded() {
        // Downloaded at turn 3 with 5 turns left; at turn 6 there are 2 left
        assertThat(AdExpiry.remainingTurns(5, 3, 6)).isEqualTo(2);
        assertThat(AdExpiry.isExpired(5, 3, 6)).isFalse();
    }

    @Test
    void adIsExpiredOnceTheCountdownReachesZero() {
        assertThat(AdExpiry.isExpired(2, 0, 2)).isTrue();
        assertThat(AdExpiry.isExpired(2, 0, 5)).isTrue();
    }

    @Test
    void canAffordAtExactlyTheItemCost() {
        assertThat(GoldRules.canAfford(50, 50)).isTrue();
        assertThat(GoldRules.canAfford(49, 50)).isFalse();
    }

    @Test
    void shortfallIsZeroWhenAffordable() {
        assertThat(GoldRules.shortfall(30, 50)).isEqualTo(20);
        assertThat(GoldRules.shortfall(80, 50)).isZero();
    }

    @Test
    void goldChangeIsPositiveForMissionsNegativeForPurchasesZeroForFailures() {
        assertThat(GoldRules.goldChange(new MissionLog("g", "ad", 20, RiskCategory.SAFE, true, "ok"))).isEqualTo(20);
        assertThat(GoldRules.goldChange(new MissionLog(EntryType.PURCHASE, "g", "hpot", 50, null, true, "ok"))).isEqualTo(-50);
        assertThat(GoldRules.goldChange(new MissionLog("g", "ad", 20, RiskCategory.FATAL, false, "lost"))).isZero();
    }

    @Test
    void lifeStateWarnsAtOneLifeAndEndsAtZero() {
        assertThat(LifeState.of(0)).isEqualTo(LifeState.GAME_OVER);
        assertThat(LifeState.of(1)).isEqualTo(LifeState.CRITICAL);
        assertThat(LifeState.of(3)).isEqualTo(LifeState.HEALTHY);
        assertThat(LifeState.HEALTHY.getWarning()).isNull();
        assertThat(LifeState.GAME_OVER.isGameOver()).isTrue();
    }
}