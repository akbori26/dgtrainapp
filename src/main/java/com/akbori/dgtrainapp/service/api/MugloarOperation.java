package com.akbori.dgtrainapp.service.api;

/**
 * The Mugloar API calls, with the wording used when one of them fails.
 * Templates use positional arguments: %1$s is the game ID and %2$s the ad or item ID.
 */
public enum MugloarOperation {
    START_GAME("starting a new game", "Bad request while starting a new game"),
    INVESTIGATE_REPUTATION("investigating reputation", "Bad request while investigating reputation for game %1$s"),
    FETCH_MESSAGES("fetching the message board", "Bad request while fetching the message board for game %1$s"),
    SOLVE_AD("solving ad %2$s", "Ad %2$s could not be solved (unknown or already expired) for game %1$s"),
    FETCH_SHOP("fetching the shop items", "Bad request while fetching the shop items for game %1$s"),
    BUY_ITEM("buying item %2$s", "Item %2$s could not be bought for game %1$s");

    private final String actionTemplate;
    private final String badRequestTemplate;

    MugloarOperation(String actionTemplate, String badRequestTemplate) {
        this.actionTemplate = actionTemplate;
        this.badRequestTemplate = badRequestTemplate;
    }

    /**
     * @param ids Game ID, then the ad or item ID if the call has one
     * @return What the call was doing, e.g. "solving ad XFyAyerI"
     */
    public String action(String... ids) {
        return String.format(actionTemplate, (Object[]) ids);
    }

    /**
     * @param ids Game ID, then the ad or item ID if the call has one
     * @return Why the server refused the call with 400, e.g. "Item hpot could not be bought for game abc123"
     */
    public String badRequest(String... ids) {
        return String.format(badRequestTemplate, (Object[]) ids);
    }
}
