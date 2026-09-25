package com.aviation.compliance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an aircraft operator with annual emissions and compliance data
 */
public class Operator {
    private String operatorId;
    private String operatorName;
    private double annualCo2EmissionsMetricTons;
    private double baselineEmissions2019_2020;
    private double offsetCreditsRequired;
    private double offsetCreditsPurchased;
    private double safCreditsEarned;
    private List<String> violations;
    private List<String> messages;
    private double penaltyAmount;
    private boolean routeRightsSuspended;
    
    public Operator() {
        this.violations = new ArrayList<>();
        this.messages = new ArrayList<>();
    }
    
    public Operator(String operatorId, String operatorName) {
        this();
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }
    
    // Getters and Setters
    public String getOperatorId() {
        return operatorId;
    }
    
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
    
    public String getOperatorName() {
        return operatorName;
    }
    
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
    
    public double getAnnualCo2EmissionsMetricTons() {
        return annualCo2EmissionsMetricTons;
    }
    
    public void setAnnualCo2EmissionsMetricTons(double annualCo2EmissionsMetricTons) {
        this.annualCo2EmissionsMetricTons = annualCo2EmissionsMetricTons;
    }
    
    public double getBaselineEmissions2019_2020() {
        return baselineEmissions2019_2020;
    }
    
    public void setBaselineEmissions2019_2020(double baselineEmissions2019_2020) {
        this.baselineEmissions2019_2020 = baselineEmissions2019_2020;
    }
    
    public double getOffsetCreditsRequired() {
        return offsetCreditsRequired;
    }
    
    public void setOffsetCreditsRequired(double offsetCreditsRequired) {
        this.offsetCreditsRequired = offsetCreditsRequired;
    }
    
    public double getOffsetCreditsPurchased() {
        return offsetCreditsPurchased;
    }
    
    public void setOffsetCreditsPurchased(double offsetCreditsPurchased) {
        this.offsetCreditsPurchased = offsetCreditsPurchased;
    }
    
    public double getSafCreditsEarned() {
        return safCreditsEarned;
    }
    
    public void setSafCreditsEarned(double safCreditsEarned) {
        this.safCreditsEarned = safCreditsEarned;
    }
    
    public List<String> getViolations() {
        return violations;
    }
    
    public void setViolations(List<String> violations) {
        this.violations = violations;
    }
    
    public List<String> getMessages() {
        return messages;
    }
    
    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
    
    public double getPenaltyAmount() {
        return penaltyAmount;
    }
    
    public void setPenaltyAmount(double penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }
    
    public boolean isRouteRightsSuspended() {
        return routeRightsSuspended;
    }
    
    public void setRouteRightsSuspended(boolean routeRightsSuspended) {
        this.routeRightsSuspended = routeRightsSuspended;
    }
    
    // Business methods
    public void addViolation(String violation) {
        this.violations.add(violation);
    }
    
    public void addMessage(String message) {
        this.messages.add(message);
    }
    
    public void addPenalty(double amount) {
        this.penaltyAmount += amount;
    }
    
    // Computed properties
    public double getExcessEmissions() {
        if (annualCo2EmissionsMetricTons <= baselineEmissions2019_2020) {
            return 0;
        }
        return annualCo2EmissionsMetricTons - baselineEmissions2019_2020;
    }
    
    public double getNetOffsetObligation() {
        double netObligation = offsetCreditsRequired - safCreditsEarned;
        return netObligation > 0 ? netObligation : 0;
    }
    
    public double getOffsetDeficit() {
        double netObligation = getNetOffsetObligation();
        if (offsetCreditsPurchased >= netObligation) {
            return 0;
        }
        return netObligation - offsetCreditsPurchased;
    }
    
    public boolean requiresCorsiaParticipation() {
        return annualCo2EmissionsMetricTons > 10000;
    }
    
    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}

// Made with Bob
