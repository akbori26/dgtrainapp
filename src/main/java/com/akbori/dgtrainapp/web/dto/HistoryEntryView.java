package com.akbori.dgtrainapp.web.dto;

import com.akbori.dgtrainapp.game.GoldRules;
import com.akbori.dgtrainapp.model.EntryType;
import com.akbori.dgtrainapp.model.MissionLog;

import java.time.Instant;

/**
 * One mission or purchase in the game history. goldChange is positive for gold earned,
 * negative for gold spent and 0 for a failed attempt. targetId is the ad ID or, for a purchase, the item ID.
 */
public record HistoryEntryView(
        Long id,
        Instant time,
        EntryType type,
        String targetId,
        RiskView risk,
        int goldChange,
        boolean success,
        String message
) {
    public static HistoryEntryView of(MissionLog entry) {
        return new HistoryEntryView(entry.getId(), Instant.ofEpochMilli(entry.getTimestamp()), entry.getEntryType(),
                entry.getAdId(), RiskView.of(entry.getRiskCategory()), GoldRules.goldChange(entry),
                entry.isSuccess(), entry.getServerMessage());
    }
}
