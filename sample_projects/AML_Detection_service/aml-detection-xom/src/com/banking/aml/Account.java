package com.banking.aml;

import java.util.Date;

public class Account {
    private String accountId;
    private String customerId;
    private Date lastActivityDate;
    private boolean dormant;
    private int daysSinceLastActivity;
    private Date reactivationDate;
    
    public Account() {
    }
    
    public Account(String accountId, String customerId, Date lastActivityDate) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.lastActivityDate = lastActivityDate;
    }
    
    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public Date getLastActivityDate() {
        return lastActivityDate;
    }
    
    public void setLastActivityDate(Date lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }
    
    public boolean isDormant() {
        return dormant;
    }
    
    public void setDormant(boolean dormant) {
        this.dormant = dormant;
    }
    
    public int getDaysSinceLastActivity() {
        return daysSinceLastActivity;
    }
    
    public void setDaysSinceLastActivity(int daysSinceLastActivity) {
        this.daysSinceLastActivity = daysSinceLastActivity;
    }
    
    public Date getReactivationDate() {
        return reactivationDate;
    }
    
    public void setReactivationDate(Date reactivationDate) {
        this.reactivationDate = reactivationDate;
    }
    
    // Computed properties
    public boolean isDormantAccount() {
        if (lastActivityDate == null) {
            return false;
        }
        long daysSince = (new Date().getTime() - lastActivityDate.getTime()) / (1000 * 60 * 60 * 24);
        return daysSince >= 180;
    }
    
    public boolean isRecentlyReactivated() {
        if (reactivationDate == null) {
            return false;
        }
        long daysSinceReactivation = (new Date().getTime() - reactivationDate.getTime()) / (1000 * 60 * 60 * 24);
        return daysSinceReactivation <= 7;
    }
}

// Made with Bob
