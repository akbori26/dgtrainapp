package com.akbori.dgtrainapp.web.dto;

import com.akbori.dgtrainapp.model.RiskCategory;

/**
 * A risk category as the front end shows it, e.g. {"code":"SAFE","icon":"🟢","label":"Safe"}.
 */
public record RiskView(RiskCategory code, String icon, String label) {

    /**
     * @return The view, or null for history entries that have no risk (purchases)
     */
    public static RiskView of(RiskCategory category) {
        if (category == null) return null;
        return new RiskView(category, category.getIcon(), category.getLabel());
    }
}
