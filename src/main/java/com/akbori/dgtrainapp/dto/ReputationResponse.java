package com.akbori.dgtrainapp.dto;


/**
 * Maps the properties returned from the /{gameId}/investigate/reputation endpoint.
 *
 */
public record ReputationResponse(
        double people,
        double state,
        double underworld
) {}
