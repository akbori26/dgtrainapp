package com.akbori.dgtrainapp.web.dto;

import com.akbori.dgtrainapp.game.LifeState;
import com.akbori.dgtrainapp.model.PlayerStatus;

/**
 * The player's state for the status panel. warning is null when there is nothing to warn about.
 */
public record GameStatusView(
        String gameId,
        int turn,
        int lives,
        int gold,
        int level,
        int score,
        int highScore,
        LifeState lifeState,
        String warning
) {
    /**
     * @param highScore Best score across all stored games (PlayerStatusUpdater.highScore)
     */
    public static GameStatusView of(PlayerStatus status, int highScore) {
        LifeState lifeState = LifeState.of(status.getLives());
        return new GameStatusView(status.getGameId(), status.getTurn(), status.getLives(), status.getGold(),
                status.getLevel(), status.getScore(), highScore, lifeState, lifeState.getWarning());
    }
}
