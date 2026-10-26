package com.akbori.dgtrainapp.web.dto;

/**
 * What happened when the dragon was sent on an ad.
 */
public record SolveOutcome(String adId, boolean success, int reward, String message) {}
