package com.akbori.dgtrainapp.game;

/**
 * The dragon's health from its lives count, so the page can warn the player in time.
 */
public enum LifeState {
    GAME_OVER("Your dragon has no lives left. Start a new game."),
    CRITICAL("Only 1 life left! Buy a Healing potion before taking another risk."),
    HEALTHY(null);

    private final String warning;

    LifeState(String warning) {
        this.warning = warning;
    }

    /**
     * @param lives Lives left
     * @return GAME_OVER at 0 or less, CRITICAL at 1, otherwise HEALTHY
     */
    public static LifeState of(int lives) {
        if (lives <= 0) return GAME_OVER;
        if (lives == 1) return CRITICAL;
        return HEALTHY;
    }

    /**
     * @return Message to show the player, or null when there is nothing to warn about
     */
    public String getWarning() { return warning; }

    public boolean isGameOver() { return this == GAME_OVER; }
}