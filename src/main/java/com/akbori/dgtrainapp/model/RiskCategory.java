package com.akbori.dgtrainapp.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * How dangerous an ad is, derived from the probability text the Mugloar API sends.
 * Constants run from safest to deadliest; UNKNOWN is for text the game has not used before.
 * Stored by name in the risk_category column of cached_ad and mission_log.
 */
public enum RiskCategory {
    SAFE("🟢", "Safe", "piece of cake", "sure thing", "walk in the park"),
    LIKELY("🟡", "Likely", "quite likely", "hmmm...."),
    GAMBLE("🎲", "Gamble", "gamble"),
    RISKY("🟠", "Risky", "risky", "playing with fire"),
    DANGEROUS("🔴", "Dangerous", "rather detrimental"),
    FATAL("☠️", "Fatal", "suicide mission", "impossible"),
    UNKNOWN("⚪", "Unknown");

    private final String icon;
    private final String label;
    private final String[] probabilities;

    // Lower-case probability text -> category, built once from the constants above
    private static final Map<String, RiskCategory> BY_PROBABILITY = Arrays.stream(values())
            .flatMap(category -> Arrays.stream(category.probabilities)
                    .map(probability -> Map.entry(probability, category)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    RiskCategory(String icon, String label, String... probabilities) {
        this.icon = icon;
        this.label = label;
        this.probabilities = probabilities;
    }

    /**
     * Maps the API's probability text to a category.
     * Encrypted ads are decoded by the service, so the text is always plain here.
     *
     * @param probability Probability text from the ad, e.g. "Piece of cake"
     * @return The matching category, or UNKNOWN for null or text the game has not used before
     */
    public static RiskCategory fromProbability(String probability) {
        if (probability == null) return UNKNOWN;
        // Locale.ROOT so a Turkish default locale doesn't turn "I" into "ı"
        return BY_PROBABILITY.getOrDefault(probability.trim().toLowerCase(Locale.ROOT), UNKNOWN);
    }

    public String getIcon() { return icon; }
    public String getLabel() { return label; }

    /**
     * @return Icon and label for display, e.g. "🟢 Safe"
     */
    public String getDisplayName() { return icon + " " + label; }
}