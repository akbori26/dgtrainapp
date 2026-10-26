package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;

/**
 * Gold arithmetic for buying items and for the game history.
 */
public final class GoldRules {

    private GoldRules() {}

    /**
     * @param gold Gold the player has
     * @param cost Price of the item
     * @return true if the player can pay for the item
     */
    public static boolean canAfford(int gold, int cost) {
        return gold >= cost;
    }

    /**
     * @param gold Gold the player has
     * @param cost Price of the item
     * @return Gold still needed to buy the item, or 0 if the player can afford it
     */
    public static int shortfall(int gold, int cost) {
        return Math.max(0, cost - gold);
    }

    /**
     * Gold won or spent by one history entry. Missions earn their reward and purchases
     * spend the item cost, but only when they succeed.
     *
     * @param entry Mission or purchase from mission_log
     * @return Positive for gold earned, negative for gold spent, 0 for a failed attempt
     */
    public static int goldChange(MissionLog entry) {
        if (!entry.isSuccess()) return 0;
        return entry.getEntryType() == EntryType.PURCHASE ? -entry.getReward() : entry.getReward();
    }
}