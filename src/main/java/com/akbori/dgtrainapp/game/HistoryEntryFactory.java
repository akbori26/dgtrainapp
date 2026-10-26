package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.model.CachedAd;
import com.akbori.dgtrainapp.model.CachedItem;
import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;

/**
 * Creates the mission_log rows for the game history (Factory pattern).
 */
public final class HistoryEntryFactory {

    private HistoryEntryFactory() {}

    /**
     * @param gameId Game the mission belongs to
     * @param ad     The ad that was attempted
     * @param res    The server's verdict
     * @return A MISSION entry with the ad's reward and risk category
     */
    public static MissionLog mission(String gameId, CachedAd ad, SolutionResponse res) {
        return new MissionLog(gameId, ad.getAdId(), ad.getReward(), ad.getRiskCategory(), res.success(), res.message());
    }

    /**
     * The trade response carries no message, so one is written from the item.
     *
     * @param gameId Game the purchase belongs to
     * @param item   The item that was bought
     * @param res    The server's answer
     * @return A PURCHASE entry with the item ID and the gold spent
     */
    public static MissionLog purchase(String gameId, CachedItem item, TradeResponse res) {
        return new MissionLog(EntryType.PURCHASE, gameId, item.getItemId(), item.getCost(), null,
                res.shoppingSuccess(), purchaseMessage(item, res.shoppingSuccess()));
    }

    /**
     * @return e.g. "Bought Healing potion for 50 gold" or "Could not buy Healing potion for 50 gold"
     */
    public static String purchaseMessage(CachedItem item, boolean success) {
        return (success ? "Bought " : "Could not buy ") + item.getName() + " for " + item.getCost() + " gold";
    }
}