package com.akbori.dgtrainapp.model;

import com.akbori.dgtrainapp.dto.AdItem;
import com.akbori.dgtrainapp.game.AdExpiry;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@IdClass(CachedAd.Key.class)
public class
CachedAd {
    @Id private String gameId;
    @Id private int displayIndex;
    private String adId;
    private String message;
    private int reward;
    private int expiresIn;
    private String probability;
    @Enumerated(EnumType.STRING)
    private RiskCategory riskCategory;
    private boolean used; // SQLite stores it as 0/1
    private int fetchedTurn; // player turn when the board was downloaded; expiresIn counts from here

    // Composite primary key: (game_id, display_index)
    public record Key(String gameId, int displayIndex) implements Serializable {
        public Key() { this(null, 0); }
    }

    public CachedAd() {}
    public CachedAd(String gameId, int displayIndex, AdItem ad, int fetchedTurn) {
        this.gameId = gameId; this.displayIndex = displayIndex; this.adId = ad.adId(); this.message = ad.message();
        this.reward = ad.reward(); this.expiresIn = ad.expiresIn(); this.probability = ad.probability(); this.riskCategory = RiskCategory.fromProbability(ad.probability());
        this.fetchedTurn = fetchedTurn;
    }

    // Turns left before the ad expires, given the player's current turn
    public int remainingExpiresIn(int currentTurn) {
        return AdExpiry.remainingTurns(expiresIn, fetchedTurn, currentTurn);
    }

    public boolean isExpired(int currentTurn) {
        return AdExpiry.isExpired(expiresIn, fetchedTurn, currentTurn);
    }

    public String getGameId() { return gameId; }
    public int getDisplayIndex() { return displayIndex; }
    public String getAdId() { return adId; }
    public String getMessage() { return message; }
    public int getReward() { return reward; }
    public int getExpiresIn() { return expiresIn; }
    public String getProbability() { return probability; }
    public RiskCategory getRiskCategory() { return riskCategory; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public int getFetchedTurn() { return fetchedTurn; }
}
