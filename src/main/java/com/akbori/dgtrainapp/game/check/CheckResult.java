package com.akbori.dgtrainapp.game.check;

/**
 * Outcome of a GameCheck: passed, or failed with a reason and a message for the player.
 *
 * @param failure Why the check failed, or null if it passed
 * @param message Message for the player, or null if it passed
 */
public record CheckResult(CheckFailure failure, String message) {

    public static final CheckResult PASSED = new CheckResult(null, null);

    /**
     * @param failure Why the check failed
     * @param args    Values for the failure's message template
     */
    public static CheckResult fail(CheckFailure failure, Object... args) {
        return new CheckResult(failure, failure.format(args));
    }

    public boolean passed() {
        return failure == null;
    }

    /**
     * Turns a failed result into an exception, so a controller can stop the action in one line.
     *
     * @throws GameRuleException if the check failed
     */
    public void orThrow() {
        if (!passed()) throw new GameRuleException(this);
    }
}