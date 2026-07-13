package com.freshlab.freshdoctor.dto;

public record RiskFactorResponse(
        String name,
        Integer score,
        Integer maxScore,
        Integer displayRatio
) {

    public static RiskFactorResponse of(String name, Integer score, int maxScore) {
        int resolvedScore = score == null ? 0 : score;
        int displayRatio = maxScore == 0 ? 0 : (int) Math.round(resolvedScore * 100.0 / maxScore);
        return new RiskFactorResponse(
                name,
                score,
                maxScore,
                Math.min(displayRatio, 100)
        );
    }
}
