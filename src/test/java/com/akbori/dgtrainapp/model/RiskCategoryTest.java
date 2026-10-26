package com.akbori.dgtrainapp.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCategoryTest {

    @ParameterizedTest
    @CsvSource({
            "Piece of cake, SAFE",
            "Sure thing, SAFE",
            "Walk in the park, SAFE",
            "Quite likely, LIKELY",
            "Hmmm...., LIKELY",
            "Gamble, GAMBLE",
            "Risky, RISKY",
            "Playing with fire, RISKY",
            "Rather detrimental, DANGEROUS",
            "Suicide mission, FATAL",
            "Impossible, FATAL"
    })
    void mapsEveryKnownProbability(String probability, RiskCategory expected) {
        assertThat(RiskCategory.fromProbability(probability)).isEqualTo(expected);
    }

    @Test
    void ignoresCaseAndSurroundingSpaces() {
        assertThat(RiskCategory.fromProbability("  PIECE OF CAKE ")).isEqualTo(RiskCategory.SAFE);
    }

    @Test
    void unknownOrMissingTextIsUnknown() {
        assertThat(RiskCategory.fromProbability("Totally new wording")).isEqualTo(RiskCategory.UNKNOWN);
        assertThat(RiskCategory.fromProbability(null)).isEqualTo(RiskCategory.UNKNOWN);
    }

    @Test
    void displayNameJoinsIconAndLabel() {
        assertThat(RiskCategory.SAFE.getDisplayName()).isEqualTo("🟢 Safe");
    }
}