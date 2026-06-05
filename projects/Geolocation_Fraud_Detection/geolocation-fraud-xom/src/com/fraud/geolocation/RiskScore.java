package com.fraud.geolocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the accumulated risk score and triggered rule details for a transaction.
 * Rules add points and record triggered rule names during evaluation.
 */
public class RiskScore {

    private int totalScore;
    private int maxScore;
    private List<String> triggeredRules;
    private List<String> riskFactors;
    private String confidenceLevel;
    private boolean impossibleTravelDetected;
    private boolean highRiskCountryDetected;
    private boolean ipLocationMismatchDetected;
    private boolean unusualCorridorDetected;
    private boolean velocityAnomalyDetected;
    private boolean cardPresenceMismatchDetected;

    public RiskScore() {
        this.totalScore = 0;
        this.maxScore = 100;
        this.triggeredRules = new ArrayList<String>();
        this.riskFactors = new ArrayList<String>();
        this.confidenceLevel = "LOW";
    }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public boolean isImpossibleTravelDetected() { return impossibleTravelDetected; }
    public void setImpossibleTravelDetected(boolean impossibleTravelDetected) {
        this.impossibleTravelDetected = impossibleTravelDetected;
    }

    public boolean isHighRiskCountryDetected() { return highRiskCountryDetected; }
    public void setHighRiskCountryDetected(boolean highRiskCountryDetected) {
        this.highRiskCountryDetected = highRiskCountryDetected;
    }

    public boolean isIpLocationMismatchDetected() { return ipLocationMismatchDetected; }
    public void setIpLocationMismatchDetected(boolean ipLocationMismatchDetected) {
        this.ipLocationMismatchDetected = ipLocationMismatchDetected;
    }

    public boolean isUnusualCorridorDetected() { return unusualCorridorDetected; }
    public void setUnusualCorridorDetected(boolean unusualCorridorDetected) {
        this.unusualCorridorDetected = unusualCorridorDetected;
    }

    public boolean isVelocityAnomalyDetected() { return velocityAnomalyDetected; }
    public void setVelocityAnomalyDetected(boolean velocityAnomalyDetected) {
        this.velocityAnomalyDetected = velocityAnomalyDetected;
    }

    public boolean isCardPresenceMismatchDetected() { return cardPresenceMismatchDetected; }
    public void setCardPresenceMismatchDetected(boolean cardPresenceMismatchDetected) {
        this.cardPresenceMismatchDetected = cardPresenceMismatchDetected;
    }

    /**
     * Adds points to the total score (capped at maxScore) and records the rule name.
     */
    public void addScore(int points, String ruleName) {
        this.totalScore = Math.min(this.totalScore + points, this.maxScore);
        if (ruleName != null && !triggeredRules.contains(ruleName)) {
            triggeredRules.add(ruleName);
        }
    }

    /**
     * Adds points to the total score only (capped at maxScore).
     */
    public void addScore(int points) {
        this.totalScore = Math.min(this.totalScore + points, this.maxScore);
    }

    /**
     * Records a triggered rule name.
     */
    public void addTriggeredRule(String ruleName) {
        if (ruleName != null && !triggeredRules.contains(ruleName)) {
            triggeredRules.add(ruleName);
        }
    }

    /**
     * Adds a risk factor description to the list.
     */
    public void addRiskFactor(String factor) {
        if (factor != null && !riskFactors.contains(factor)) {
            riskFactors.add(factor);
        }
    }

    /**
     * Computes the normalized score as a percentage (0-100).
     */
    public int getNormalizedScore() {
        if (maxScore == 0) return 0;
        return (int) Math.round((double) totalScore / maxScore * 100);
    }

    /**
     * Returns true if the total score meets or exceeds the given threshold.
     */
    public boolean exceedsThreshold(int threshold) {
        return totalScore >= threshold;
    }

    @Override
    public String toString() {
        return "RiskScore{total=" + totalScore + ", confidence=" + confidenceLevel
                + ", rules=" + triggeredRules.size() + "}";
    }
}

// Made with Bob
