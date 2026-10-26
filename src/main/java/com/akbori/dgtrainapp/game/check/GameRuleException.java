package com.akbori.dgtrainapp.game.check;

/**
 * Thrown when a game action breaks a rule, such as solving an expired ad or buying without enough gold.
 * The web layer can map it to a 4xx response and show getMessage() to the player.
 */
public class GameRuleException extends RuntimeException {

    private final CheckFailure failure;

    public GameRuleException(CheckResult result) {
        super(result.message());
        this.failure = result.failure();
    }

    public CheckFailure getFailure() { return failure; }
}