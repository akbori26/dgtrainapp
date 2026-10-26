package com.akbori.dgtrainapp.game;

/**
 * Expiry countdown for message board ads.
 * The API sends expiresIn as turns left at the moment the board is downloaded, and every action
 * that costs a turn brings each ad one step closer to expiring. The server stops accepting an ad
 * once the countdown reaches 0.
 */
public final class AdExpiry {

    private AdExpiry() {}

    /**
     * @param expiresIn   Turns left when the board was downloaded
     * @param fetchedTurn Player turn when the board was downloaded
     * @param currentTurn Player turn now
     * @return Turns left before the ad expires; 0 or less means expired
     */
    public static int remainingTurns(int expiresIn, int fetchedTurn, int currentTurn) {
        return expiresIn - (currentTurn - fetchedTurn);
    }

    /**
     * @return true if the ad can no longer be solved
     */
    public static boolean isExpired(int expiresIn, int fetchedTurn, int currentTurn) {
        return remainingTurns(expiresIn, fetchedTurn, currentTurn) <= 0;
    }
}