package com.akbori.dgtrainapp.game.check;

/**
 * Every reason a game action can be refused before it is sent to the Mugloar server.
 * Each message is a String.format template filled in by CheckResult.fail().
 */
public enum CheckFailure {
    NO_ACTIVE_GAME("No game is active. Start a new game or resume one first."),
    PLAYER_STATUS_MISSING("No player status is stored for this game. Start a new game first."),
    GAME_OVER("Your dragon has no lives left. Start a new game."),
    BOARD_EMPTY("No ads are stored for this game. Download the message board first."),
    MARKET_EMPTY("No market items are stored for this game. Open the market first."),
    INDEX_OUT_OF_RANGE("Pick a number from 1 to %d."),
    AD_NOT_FOUND("Ad %s is not on this game's message board. Download the message board first."),
    ITEM_NOT_FOUND("Item %s is not in this game's shop. Open the market first."),
    AD_ALREADY_USED("Ad %s has already been used in this game. Pick a different ad, or download a new board."),
    AD_EXPIRED("Ad %s has expired and can no longer be solved. Pick a different ad, or download a new board."),
    NOT_ENOUGH_GOLD("%s costs %d gold, but you only have %d. You need %d more.");

    private final String messageTemplate;

    CheckFailure(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    String format(Object... args) {
        return String.format(messageTemplate, args);
    }
}