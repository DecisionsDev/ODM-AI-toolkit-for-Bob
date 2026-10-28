package com.banking.aml;

import java.util.Date;

public class Transaction {
    private String transactionId;
    private String customerId;
    private String accountId;
    private double amount;
    private String currency;
    private Date timestamp;
    private String transactionType; // DEBIT, CREDIT, TRANSFER
    private String counterpartyAccountId;
    private String counterpartyJurisdiction;
    private boolean crossBorder;
    private boolean roundNumber;
    
    public Transaction() {
    }
    
    public Transaction(String transactionId, String customerId, String accountId, double amount, String currency, Date timestamp) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
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
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public String getCounterpartyAccountId() {
        return counterpartyAccountId;
    }
    
    public void setCounterpartyAccountId(String counterpartyAccountId) {
        this.counterpartyAccountId = counterpartyAccountId;
    }
    
    public String getCounterpartyJurisdiction() {
        return counterpartyJurisdiction;
    }
    
    public void setCounterpartyJurisdiction(String counterpartyJurisdiction) {
        this.counterpartyJurisdiction = counterpartyJurisdiction;
    }
    
    public boolean isCrossBorder() {
        return crossBorder;
    }
    
    public void setCrossBorder(boolean crossBorder) {
        this.crossBorder = crossBorder;
    }
    
    public boolean isRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(boolean roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    // Computed property - check if amount is a round number
    public boolean isAmountRoundNumber() {
        return amount % 1000 == 0 || amount % 5000 == 0 || amount % 10000 == 0;
    }
}

// Made with Bob
