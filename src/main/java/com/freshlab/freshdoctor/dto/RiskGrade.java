package com.freshlab.freshdoctor.dto;

public enum RiskGrade {
    SAFE,
    INTEREST,
    CAUTION,
    ALERT,
    CRITICAL;

    public static RiskGrade fromScore(int finalScore) {
        if (finalScore < 0 || finalScore > 100) {
            throw new IllegalArgumentException("finalScore must be between 0 and 100.");
        }
        if (finalScore <= 20) {
            return SAFE;
        }
        if (finalScore <= 40) {
            return INTEREST;
        }
        if (finalScore <= 60) {
            return CAUTION;
        }
        if (finalScore <= 80) {
            return ALERT;
        }
        return CRITICAL;
    }
}
