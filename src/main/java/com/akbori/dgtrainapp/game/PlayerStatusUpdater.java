package com.akbori.dgtrainapp.game;

import com.akbori.dgtrainapp.dto.GameStartResponse;
import com.akbori.dgtrainapp.dto.SolutionResponse;
import com.akbori.dgtrainapp.dto.TradeResponse;
import com.akbori.dgtrainapp.model.PlayerStatus;

import java.util.Optional;

/**
 * Keeps the stored player status in step with the server's responses.
 * The caller saves the status afterwards.
 */
public final class PlayerStatusUpdater {

    private PlayerStatusUpdater() {}

    /**
     * @param res Response from starting a new game
     * @return The new game's first player status
     */
    public static PlayerStatus fromGameStart(GameStartResponse res) {
        PlayerStatus status = new PlayerStatus(res.gameId(), res.lives(), res.gold(), res.score(), res.highScore(), res.turn());
        status.setLevel(res.level());
        return status;
    }

    /**
     * Copies lives, gold, score, high score and turn from a solved (or failed) ad.
     */
    public static void applySolution(PlayerStatus status, SolutionResponse res) {
        status.setLives(res.lives());
        status.setGold(res.gold());
        status.setScore(res.score());
        status.setHighScore(res.highScore());
        status.setTurn(res.turn());
    }

    /**
     * Copies lives, gold, level and turn from a purchase.
     */
    public static void applyTrade(PlayerStatus status, TradeResponse res) {
        status.setLives(res.lives());
        status.setGold(res.gold());
        status.setLevel(res.level());
        status.setTurn(res.turn());
    }

    /**
     * Investigating reputation costs a turn, but the response has no turn field, so it is counted locally.
     */
    public static void applyReputationInvestigated(PlayerStatus status) {
        status.setTurn(status.getTurn() + 1);
    }

    /**
     * The server always reports highScore as 0, so the high score is the best score across all stored games.
     *
     * @param status    The current game's status
     * @param bestGame  The stored game with the highest score (PlayerStatusRepository.findTopByOrderByScoreDesc)
     * @return The higher of the current score and the best stored score
     */
    public static int highScore(PlayerStatus status, Optional<PlayerStatus> bestGame) {
        return Math.max(status.getScore(), bestGame.map(PlayerStatus::getScore).orElse(0));
    }
}