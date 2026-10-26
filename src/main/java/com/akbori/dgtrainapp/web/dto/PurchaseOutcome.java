package com.akbori.dgtrainapp.web.dto;

/**
 * What happened when an item was bought.
 */
public record PurchaseOutcome(String itemId, String name, int cost, boolean success, String message) {}
