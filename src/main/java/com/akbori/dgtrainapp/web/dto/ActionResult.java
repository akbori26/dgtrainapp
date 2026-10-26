package com.akbori.dgtrainapp.web.dto;

/**
 * Answer to an action that changes the game: what happened, plus the updated status,
 * so the front end can refresh its numbers without a second request.
 *
 * @param <T> The action's outcome, e.g. SolveOutcome
 */
public record ActionResult<T>(T outcome, GameStatusView status) {}
