package com.akbori.dgtrainapp.game.check;

/**
 * One game rule that a target must pass before an action goes to the server (Strategy pattern).
 * Checks chain with and(): the next check only runs when the previous one passed,
 * so the player sees the first rule that was broken (Chain of Responsibility).
 *
 * @param <T> What is checked, e.g. PlayerStatus or CachedAd
 */
@FunctionalInterface
public interface GameCheck<T> {

    CheckResult check(T target);

    /**
     * @param next Check to run when this one passes
     * @return A check that passes only when both pass, reporting the first failure
     */
    default GameCheck<T> and(GameCheck<? super T> next) {
        return target -> {
            CheckResult result = check(target);
            return result.passed() ? next.check(target) : result;
        };
    }
}