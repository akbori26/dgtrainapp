package com.akbori.dgtrainapp.service.api;

import org.springframework.http.HttpStatusCode;

/**
 * Thrown by MugloarService when a call fails: an error status, no network, or a response that can't be read.
 * getMessage() is meant for the player; getError() tells the web layer how to answer.
 */
public class MugloarApiException extends RuntimeException {

    private final MugloarError error;
    private final MugloarOperation operation;
    private final Integer statusCode;

    private MugloarApiException(MugloarError error, MugloarOperation operation, Integer statusCode,
                                Throwable cause, String... ids) {
        super(error.message(operation, statusCode, ids), cause);
        this.error = error;
        this.operation = operation;
        this.statusCode = statusCode;
    }

    /**
     * @param status The non-2xx status the server answered with
     * @param ids    Game ID, then the ad or item ID if the call has one
     */
    public static MugloarApiException fromStatus(MugloarOperation operation, HttpStatusCode status, String... ids) {
        return new MugloarApiException(MugloarError.fromStatus(status), operation, status.value(), null, ids);
    }

    /**
     * The server could not be reached: no internet, a timeout or a DNS failure.
     */
    public static MugloarApiException networkUnavailable(MugloarOperation operation, Throwable cause, String... ids) {
        return new MugloarApiException(MugloarError.NETWORK_UNAVAILABLE, operation, null, cause, ids);
    }

    /**
     * The server answered, but its body could not be read into the expected record.
     */
    public static MugloarApiException unreadableResponse(MugloarOperation operation, Throwable cause, String... ids) {
        return new MugloarApiException(MugloarError.UNREADABLE_RESPONSE, operation, null, cause, ids);
    }

    /**
     * Starting a game succeeded, but the body has no game ID to play with.
     */
    public static MugloarApiException missingGameId() {
        return new MugloarApiException(MugloarError.MISSING_GAME_ID, MugloarOperation.START_GAME, null, null);
    }

    public MugloarError getError() { return error; }
    public MugloarOperation getOperation() { return operation; }

    /**
     * @return The status the Mugloar server answered with, or null when there was no usable answer
     */
    public Integer getStatusCode() { return statusCode; }
}
