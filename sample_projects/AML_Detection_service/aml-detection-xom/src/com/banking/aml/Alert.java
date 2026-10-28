package com.banking.aml;

import java.util.Date;

public class Alert {
    private String alertId;
    private String ruleTriggered;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
    private String transactionId;
    private String customerId;
    private String accountId;
    private Date alertTimestamp;
    private boolean escalated;
    
    public Alert() {
        this.alertTimestamp = new Date();
    }
    
    public Alert(String ruleTriggered, String severity, String description) {
        this.ruleTriggered = ruleTriggered;
        this.severity = severity;
        this.description = description;
        this.alertTimestamp = new Date();
    }
    
    // Getters and Setters
    public String getAlertId() {
        return alertId;
    }
    
    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }
    
    public String getRuleTriggered() {
        return ruleTriggered;
    }
    
    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public Date getAlertTimestamp() {
        return alertTimestamp;
    }
    
    public void setAlertTimestamp(Date alertTimestamp) {
        this.alertTimestamp = alertTimestamp;
    }
    
    public boolean isEscalated() {
        return escalated;
    }
    
    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }
}

// Made with Bob
