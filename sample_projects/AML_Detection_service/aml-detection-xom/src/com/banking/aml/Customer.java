package com.banking.aml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Customer {
    private String customerId;
    private String name;
    private String industry;
    private String geographicLocation;
    private double riskScore; // 0.0 to 1.0
    private boolean kycComplete;
    private Date kycLastUpdated;
    private boolean hasPriorCrossBorderActivity;
    private List<String> linkedAccountIds;
    
    public Customer() {
        this.linkedAccountIds = new ArrayList<>();
    }
    
    public Customer(String customerId, String name, double riskScore) {
        this.customerId = customerId;
        this.name = name;
        this.riskScore = riskScore;
        this.linkedAccountIds = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public String getGeographicLocation() {
        return geographicLocation;
    }
    
    public void setGeographicLocation(String geographicLocation) {
        this.geographicLocation = geographicLocation;
    }
    
    public double getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }
    
    public boolean isKycComplete() {
        return kycComplete;
    }
    
    public void setKycComplete(boolean kycComplete) {
        this.kycComplete = kycComplete;
    }
    
    public Date getKycLastUpdated() {
        return kycLastUpdated;
    }
    
    public void setKycLastUpdated(Date kycLastUpdated) {
        this.kycLastUpdated = kycLastUpdated;
    }
    
    public boolean isPriorCrossBorderActivity() {
        return hasPriorCrossBorderActivity;
    }
    
    public void setPriorCrossBorderActivity(boolean priorCrossBorderActivity) {
        this.hasPriorCrossBorderActivity = priorCrossBorderActivity;
    }
    
    public List<String> getLinkedAccountIds() {
        return linkedAccountIds;
    }
    
    public void setLinkedAccountIds(List<String> linkedAccountIds) {
        this.linkedAccountIds = linkedAccountIds;
    }
    
    public void addLinkedAccount(String accountId) {
        if (!this.linkedAccountIds.contains(accountId)) {
            this.linkedAccountIds.add(accountId);
        }
    }
    
    // Computed properties
    public boolean isHighRisk() {
        return riskScore >= 0.8;
    }
    
    public boolean isMediumRisk() {
        return riskScore >= 0.5 && riskScore < 0.8;
    }
    
    public boolean isKycOutdated() {
        if (kycLastUpdated == null) {
            return true;
        }
        long daysSinceUpdate = (new Date().getTime() - kycLastUpdated.getTime()) / (1000 * 60 * 60 * 24);
        return daysSinceUpdate > 365; // More than 1 year old
    }
}

// Made with Bob
