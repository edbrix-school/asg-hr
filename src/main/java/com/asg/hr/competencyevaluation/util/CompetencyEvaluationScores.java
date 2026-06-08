package com.asg.hr.competencyevaluation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CompetencyEvaluationScores {

    private CompetencyEvaluationScores() {
    }

    public record ScoreResult(BigDecimal totalRating, BigDecimal avgRatingPercent, BigDecimal employeeAgreedPercent) {
    }

    public static ScoreResult calculate(List<String> ratings, List<String> employeeAgreedValues) {
        if (ratings == null || ratings.isEmpty()) {
            return new ScoreResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        int count = ratings.size();
        BigDecimal totalScoreReceived = BigDecimal.ZERO;
        BigDecimal agreeSum = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            totalScoreReceived = totalScoreReceived.add(ratingPoints(ratings.get(i)));
            agreeSum = agreeSum.add(agreementPoints(employeeAgreedValues != null && i < employeeAgreedValues.size()
                    ? employeeAgreedValues.get(i) : null));
        }

        BigDecimal avgScorePercent = BigDecimal.ZERO;
        BigDecimal avgAgreePercent = BigDecimal.ZERO;
        if (count > 0) {
            BigDecimal denom = BigDecimal.valueOf((long) count * 4L);
            avgScorePercent = totalScoreReceived.divide(denom, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            avgAgreePercent = agreeSum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ScoreResult(totalScoreReceived.setScale(2, RoundingMode.HALF_UP), avgScorePercent, avgAgreePercent);
    }

    static BigDecimal ratingPoints(String rating) {
        if (rating == null || rating.isBlank()) {
            return BigDecimal.ZERO;
        }
        return switch (rating.trim().toUpperCase()) {
            case "EXCELLENT" -> BigDecimal.valueOf(4);
            case "GOOD" -> BigDecimal.valueOf(3);
            case "FAIR" -> BigDecimal.valueOf(2);
            case "POOR" -> BigDecimal.valueOf(1);
            default -> BigDecimal.ZERO;
        };
    }

    static BigDecimal agreementPoints(String agreed) {
        if (agreed == null || agreed.isBlank()) {
            return BigDecimal.ZERO;
        }
        return switch (agreed.trim().toUpperCase()) {
            case "AGREE" -> BigDecimal.ONE;
            case "DISAGREE" -> BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }
}
