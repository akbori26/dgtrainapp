package com.akbori.dgtrainapp.service.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Every way a Mugloar API call can fail, with the message for the player and the status
 * the web layer should answer with. The HTTP status mapping lives only in fromStatus().
 */
public enum MugloarError {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, (op, status, ids) -> op.badRequest(ids) + "."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, (op, status, ids) -> "Game " + gameId(ids) + " was not found or is already over."),
    GAME_EXPIRED(HttpStatus.GONE, (op, status, ids) -> "Game " + gameId(ids) + " has expired."),
    SERVER_ERROR(HttpStatus.BAD_GATEWAY,
            (op, status, ids) -> "Mugloar server error (" + status + ") while " + op.action(ids) + "."),
    UNEXPECTED_STATUS(HttpStatus.BAD_GATEWAY,
            (op, status, ids) -> "Unexpected response (" + status + ") while " + op.action(ids) + "."),
    NETWORK_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            (op, status, ids) -> "Could not reach the Mugloar server while " + op.action(ids)
                    + ". Check your internet connection and try again."),
    UNREADABLE_RESPONSE(HttpStatus.BAD_GATEWAY,
            (op, status, ids) -> "The Mugloar server sent a response that could not be read while " + op.action(ids) + "."),
    MISSING_GAME_ID(HttpStatus.BAD_GATEWAY,
            (op, status, ids) -> "The server did not return a game ID for the new game.");

    @FunctionalInterface
    interface MessageBuilder {
        String build(MugloarOperation op, Integer status, String[] ids);
    }

    private final HttpStatus responseStatus;
    private final MessageBuilder messageBuilder;

    MugloarError(HttpStatus responseStatus, MessageBuilder messageBuilder) {
        this.responseStatus = responseStatus;
        this.messageBuilder = messageBuilder;
    }

    /**
     * @param status A non-2xx status from the Mugloar server
     * @return The matching error
     */
    public static MugloarError fromStatus(HttpStatusCode status) {
        if (status.is5xxServerError()) return SERVER_ERROR;
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 404 -> GAME_NOT_FOUND;
            case 410 -> GAME_EXPIRED;
            default -> UNEXPECTED_STATUS;
        };
    }

    /**
     * @return The status the web app should answer its own client with
     */
    public HttpStatus getResponseStatus() { return responseStatus; }

    String message(MugloarOperation op, Integer status, String[] ids) {
        return messageBuilder.build(op, status, ids);
    }

    private static String gameId(String[] ids) {
        return ids.length > 0 ? ids[0] : "(none)";
    }
}
