package com.asg.hr.competencyevaluation.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompetencyEvaluationScoresTest {

    @Test
    void calculate_emptyRatings_returnsZeros() {
        CompetencyEvaluationScores.ScoreResult r = CompetencyEvaluationScores.calculate(List.of(), List.of());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.totalRating()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.avgRatingPercent()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.employeeAgreedPercent()));
    }

    @Test
    void calculate_allExcellent_allAgree() {
        CompetencyEvaluationScores.ScoreResult r = CompetencyEvaluationScores.calculate(
                List.of("EXCELLENT", "excellent"),
                List.of("AGREE", "agree")
        );
        assertEquals(0, new BigDecimal("8.00").compareTo(r.totalRating()));
        assertEquals(0, new BigDecimal("100.00").compareTo(r.avgRatingPercent()));
        assertEquals(0, new BigDecimal("100.00").compareTo(r.employeeAgreedPercent()));
    }

    @Test
    void calculate_mixedRatings_mixedAgreement() {
        CompetencyEvaluationScores.ScoreResult r = CompetencyEvaluationScores.calculate(
                Arrays.asList("GOOD", "FAIR", "POOR", "UNKNOWN"),
                Arrays.asList("DISAGREE", "AGREE", null, "AGREE")
        );
        assertEquals(0, new BigDecimal("6.00").compareTo(r.totalRating()));
        assertEquals(0, new BigDecimal("37.50").compareTo(r.avgRatingPercent()));
        assertEquals(0, new BigDecimal("50.00").compareTo(r.employeeAgreedPercent()));
    }

    @Test
    void ratingPoints_trimsAndCaseInsensitive() {
        assertEquals(0, new BigDecimal("4").compareTo(CompetencyEvaluationScores.ratingPoints("  excellent ")));
        assertEquals(0, BigDecimal.ZERO.compareTo(CompetencyEvaluationScores.ratingPoints("OTHER")));
    }

    @Test
    void agreementPoints_disagreeAndBlank() {
        assertEquals(0, BigDecimal.ZERO.compareTo(CompetencyEvaluationScores.agreementPoints("DISAGREE")));
        assertEquals(0, BigDecimal.ZERO.compareTo(CompetencyEvaluationScores.agreementPoints("")));
        assertEquals(0, BigDecimal.ONE.compareTo(CompetencyEvaluationScores.agreementPoints("Agree")));
    }
}
