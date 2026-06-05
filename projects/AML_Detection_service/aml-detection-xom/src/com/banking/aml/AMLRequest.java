package com.banking.aml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AMLRequest {
    private Transaction currentTransaction;
    private Customer customer;
    private Account account;
    private List<Transaction> recentTransactions; // Transactions in analysis window
    private List<Alert> alerts;
    private Set<String> highRiskJurisdictions;
    private int ruleTriggeredCount;
    private boolean requiresManualReview;
    private boolean escalateToCompliance;
    private String overallRiskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    public AMLRequest() {
        this.recentTransactions = new ArrayList<>();
        this.alerts = new ArrayList<>();
        this.highRiskJurisdictions = new HashSet<>();
        this.ruleTriggeredCount = 0;
        this.overallRiskLevel = "LOW";
    }
    
    public AMLRequest(Transaction currentTransaction, Customer customer, Account account) {
        this();
        this.currentTransaction = currentTransaction;
        this.customer = customer;
        this.account = account;
    }
    
    // Getters and Setters
    public Transaction getCurrentTransaction() {
        return currentTransaction;
    }
    
    public void setCurrentTransaction(Transaction currentTransaction) {
        this.currentTransaction = currentTransaction;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }
    
    public void setRecentTransactions(List<Transaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
    
    public List<Alert> getAlerts() {
        return alerts;
    }
    
    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
    
    public Set<String> getHighRiskJurisdictions() {
        return highRiskJurisdictions;
    }
    
    public void setHighRiskJurisdictions(Set<String> highRiskJurisdictions) {
        this.highRiskJurisdictions = highRiskJurisdictions;
    }
    
    public int getRuleTriggeredCount() {
        return ruleTriggeredCount;
    }
    
    public void setRuleTriggeredCount(int ruleTriggeredCount) {
        this.ruleTriggeredCount = ruleTriggeredCount;
    }
    
    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }
    
    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }
    
    public boolean isEscalateToCompliance() {
        return escalateToCompliance;
    }
    
    public void setEscalateToCompliance(boolean escalateToCompliance) {
        this.escalateToCompliance = escalateToCompliance;
    }
    
    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }
    
    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }
    
    // Helper methods
    public void addAlert(Alert alert) {
        this.alerts.add(alert);
        this.ruleTriggeredCount++;
    }
    
    public void addRecentTransaction(Transaction transaction) {
        this.recentTransactions.add(transaction);
    }
    
    public void addHighRiskJurisdiction(String jurisdiction) {
        this.highRiskJurisdictions.add(jurisdiction);
    }
    
    public void incrementRuleCount() {
        this.ruleTriggeredCount++;
    }
    
    public void createAndAddAlert(String ruleTriggered, String severity, String description) {
        Alert alert = new Alert(ruleTriggered, severity, description);
        this.addAlert(alert);
    }
    
    // Computed properties
    public double getTotalRecentTransactionAmount() {
        double total = 0.0;
        for (Transaction t : recentTransactions) {
            total += t.getAmount();
        }
        return total;
    }
    
    public int getRecentTransactionCount() {
        return recentTransactions.size();
    }
    
    public boolean isMultipleRulesTriggered() {
        return ruleTriggeredCount >= 3;
    }
    
    public boolean isHighRiskJurisdiction(String jurisdiction) {
        return highRiskJurisdictions.contains(jurisdiction);
    }
}

// Made with Bob
