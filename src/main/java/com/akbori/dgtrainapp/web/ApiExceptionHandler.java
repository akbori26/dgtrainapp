package com.akbori.dgtrainapp.web;

import com.akbori.dgtrainapp.game.check.CheckFailure;
import com.akbori.dgtrainapp.game.check.GameRuleException;
import com.akbori.dgtrainapp.service.api.MugloarApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

/**
 * Answers every error in the same format, Spring's ProblemDetail (RFC 9457), with an extra "code"
 * the front end can switch on, e.g. {"status":409,"title":"Ad expired","detail":"…","code":"AD_EXPIRED"}.
 * Spring MVC's own errors (unknown URL, wrong method) keep their status via ResponseEntityExceptionHandler.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(GameRuleException.class)
    ProblemDetail handleGameRule(GameRuleException e) {
        return problem(statusFor(e.getFailure()), e.getFailure().name(), e.getMessage());
    }

    @ExceptionHandler(MugloarApiException.class)
    ProblemDetail handleMugloarApi(MugloarApiException e) {
        ProblemDetail problem = problem(e.getError().getResponseStatus(), e.getError().name(), e.getMessage());
        if (e.getStatusCode() != null) problem.setProperty("mugloarStatus", e.getStatusCode());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        // Details stay in the log; the client only learns that something failed
        log.error("Unexpected error while handling a request", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again.");
    }

    /**
     * Missing things answer 404, a bad number 400, and actions the game state doesn't allow 409.
     */
    static HttpStatus statusFor(CheckFailure failure) {
        return switch (failure) {
            case NO_ACTIVE_GAME, PLAYER_STATUS_MISSING, AD_NOT_FOUND, ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INDEX_OUT_OF_RANGE -> HttpStatus.BAD_REQUEST;
            case GAME_OVER, BOARD_EMPTY, MARKET_EMPTY, AD_ALREADY_USED, AD_EXPIRED, NOT_ENOUGH_GOLD -> HttpStatus.CONFLICT;
        };
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title(code));
        problem.setProperty("code", code);
        return problem;
    }

    // AD_EXPIRED -> "Ad expired"
    private static String title(String code) {
        String words = code.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
