package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.ShopItem;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;
import com.akbori.dgtrainapp.model.PlayerStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// PlayerStatusUpdater and HistoryEntryFactory: what is stored after each server response
class PlayerStatusUpdaterTest {

    @Test
    void gameStartCopiesEveryFieldIncludingLevel() {
        PlayerStatus status = PlayerStatusUpdater.fromGameStart(new GameStartResponse("g", 3, 0, 1, 0, 0, 0));

        assertThat(status.getGameId()).isEqualTo("g");
        assertThat(status.getLives()).isEqualTo(3);
        assertThat(status.getLevel()).isEqualTo(1);
    }

    @Test
    void solutionAndTradeUpdateTheStatus() {
        PlayerStatus status = new PlayerStatus("g", 3, 0, 0, 0, 0);

        PlayerStatusUpdater.applySolution(status, new SolutionResponse(true, 3, 60, 40, 0, 1, "Done"));
        assertThat(status.getGold()).isEqualTo(60);
        assertThat(status.getScore()).isEqualTo(40);
        assertThat(status.getTurn()).isEqualTo(1);

        PlayerStatusUpdater.applyTrade(status, new TradeResponse(true, 10, 4, 2, 2));
        assertThat(status.getGold()).isEqualTo(10);
        assertThat(status.getLives()).isEqualTo(4);
        assertThat(status.getLevel()).isEqualTo(2);
        assertThat(status.getTurn()).isEqualTo(2);
    }

    @Test
    void reputationCostsOneTurn() {
        PlayerStatus status = new PlayerStatus("g", 3, 0, 0, 0, 5);
        PlayerStatusUpdater.applyReputationInvestigated(status);
        assertThat(status.getTurn()).isEqualTo(6);
    }

    @Test
    void highScoreIsTheBestStoredScore() {
        PlayerStatus current = new PlayerStatus("g", 3, 0, 40, 0, 0);
        assertThat(PlayerStatusUpdater.highScore(current, Optional.of(new PlayerStatus("old", 0, 0, 90, 0, 0)))).isEqualTo(90);
        assertThat(PlayerStatusUpdater.highScore(current, Optional.empty())).isEqualTo(40);
    }

    @Test
    void purchaseEntryRecordsTheGoldSpent() {
        CachedItem item = new CachedItem("g", 0, new ShopItem("hpot", "Healing potion", 50));

        MissionLog entry = HistoryEntryFactory.purchase("g", item, new TradeResponse(true, 10, 4, 1, 3));

        assertThat(entry.getEntryType()).isEqualTo(EntryType.PURCHASE);
        assertThat(entry.getAdId()).isEqualTo("hpot");
        assertThat(entry.getReward()).isEqualTo(50);
        assertThat(entry.getServerMessage()).isEqualTo("Bought Healing potion for 50 gold");
    }
}