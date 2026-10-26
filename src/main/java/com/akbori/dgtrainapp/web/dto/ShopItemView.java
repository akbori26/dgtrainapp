package com.akbori.dgtrainapp.web.dto;

import com.akbori.dgtrainapp.game.GoldRules;
import com.akbori.dgtrainapp.model.CachedItem;

/**
 * One shop item. affordable tells the front end whether to enable the Buy button.
 */
public record ShopItemView(String itemId, String name, int cost, boolean affordable) {

    /**
     * @param gold Gold the player has now
     */
    public static ShopItemView of(CachedItem item, int gold) {
        return new ShopItemView(item.getItemId(), item.getName(), item.getCost(), GoldRules.canAfford(gold, item.getCost()));
    }
}
