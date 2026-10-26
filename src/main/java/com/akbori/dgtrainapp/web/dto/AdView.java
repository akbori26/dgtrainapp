package com.akbori.dgtrainapp.web.dto;

import com.akbori.dgtrainapp.model.CachedAd;

/**
 * One ad on the message board. solvable tells the front end whether to enable the Solve button.
 */
public record AdView(
        String adId,
        String message,
        int reward,
        String probability,
        RiskView risk,
        int turnsLeft,
        boolean expired,
        boolean used,
        boolean solvable
) {
    /**
     * @param currentTurn Player turn now, for the expiry countdown
     */
    public static AdView of(CachedAd ad, int currentTurn) {
        boolean expired = ad.isExpired(currentTurn);
        return new AdView(ad.getAdId(), ad.getMessage(), ad.getReward(), ad.getProbability(),
                RiskView.of(ad.getRiskCategory()), Math.max(0, ad.remainingExpiresIn(currentTurn)),
                expired, ad.isUsed(), !expired && !ad.isUsed());
    }
}
