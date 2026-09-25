package com.fraud.crossborder;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the compliance risk score and triggered rule details
 * for a cross-border transaction.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskScore {

    private int totalScore;
    private int maxScore;
    private String confidenceLevel;
    private List<String> triggeredRules;
    private List<String> riskFactors;

    // Detected signals
    private boolean sanctionedCountryDetected;
    private boolean amlJurisdictionDetected;
    private boolean spendingSurgeDetected;
    private boolean exoticCurrencyDetected;
    private boolean offshoreProviderDetected;
    private boolean greyZoneMerchantDetected;
    private boolean kycIncompleteDetected;
    private boolean mandatoryReportingRequired;

    public RiskScore() {
        this.totalScore = 0;
        this.maxScore = 100;
        this.confidenceLevel = "LOW";
        this.triggeredRules = new ArrayList<String>();
        this.riskFactors = new ArrayList<String>();
    }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public boolean isSanctionedCountryDetected() { return sanctionedCountryDetected; }
    public void setSanctionedCountryDetected(boolean v) { this.sanctionedCountryDetected = v; }

    public boolean isAmlJurisdictionDetected() { return amlJurisdictionDetected; }
    public void setAmlJurisdictionDetected(boolean v) { this.amlJurisdictionDetected = v; }

    public boolean isSpendingSurgeDetected() { return spendingSurgeDetected; }
    public void setSpendingSurgeDetected(boolean v) { this.spendingSurgeDetected = v; }

    public boolean isExoticCurrencyDetected() { return exoticCurrencyDetected; }
    public void setExoticCurrencyDetected(boolean v) { this.exoticCurrencyDetected = v; }

    public boolean isOffshoreProviderDetected() { return offshoreProviderDetected; }
    public void setOffshoreProviderDetected(boolean v) { this.offshoreProviderDetected = v; }

    public boolean isGreyZoneMerchantDetected() { return greyZoneMerchantDetected; }
    public void setGreyZoneMerchantDetected(boolean v) { this.greyZoneMerchantDetected = v; }

    public boolean isKycIncompleteDetected() { return kycIncompleteDetected; }
    public void setKycIncompleteDetected(boolean v) { this.kycIncompleteDetected = v; }

    public boolean isMandatoryReportingRequired() { return mandatoryReportingRequired; }
    public void setMandatoryReportingRequired(boolean v) { this.mandatoryReportingRequired = v; }

    /** Adds points to the total score (capped at maxScore). */
    public void addScore(int points) {
        this.totalScore = Math.min(this.totalScore + points, this.maxScore);
    }

    /** Adds points and records the rule name. */
    public void addScore(int points, String ruleName) {
        this.totalScore = Math.min(this.totalScore + points, this.maxScore);
        if (ruleName != null && !triggeredRules.contains(ruleName)) {
            triggeredRules.add(ruleName);
        }
    }

    /** Records a triggered rule name. */
    public void addTriggeredRule(String ruleName) {
        if (ruleName != null && !triggeredRules.contains(ruleName)) {
            triggeredRules.add(ruleName);
        }
    }

    /** Adds a risk factor description. */
    public void addRiskFactor(String factor) {
        if (factor != null && !riskFactors.contains(factor)) {
            riskFactors.add(factor);
        }
    }

    /** Returns true if the total score meets or exceeds the given threshold. */
    public boolean exceedsThreshold(int threshold) {
        return totalScore >= threshold;
    }

    @Override
    public String toString() {
        return "RiskScore{total=" + totalScore + ", confidence=" + confidenceLevel + "}";
    }
}

// Made with Bob
