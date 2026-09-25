package com.fraud.geolocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a financial transaction with all geolocation-relevant attributes.
 * This is the primary input object for the fraud detection decision service.
 */
public class Transaction {

    // Core transaction identifiers
    private String transactionId;
    private String cardNumber;
    private String cardType;
    private boolean cardPresent;

    // Financial details
    private double amount;
    private String currency;
    private String merchantId;
    private String merchantName;
    private String merchantCategory;

    // Timing
    private long transactionTimestampMs;
    private long previousTransactionTimestampMs;

    // Geolocation
    private Location merchantLocation;
    private Location previousTransactionLocation;
    private DeviceInfo deviceInfo;
    private Customer customer;

    // Velocity metrics
    private int transactionCountLast1Hour;
    private int transactionCountLast24Hours;
    private int distinctCountriesLast24Hours;
    private int distinctCountriesLast7Days;
    private double totalAmountLast1Hour;
    private double totalAmountLast24Hours;

    // Risk assessment output
    private RiskScore riskScore;
    private String fraudDecision;
    private String decisionReasonCode;
    private String recommendedAction;
    private List<String> auditMessages;

    public Transaction() {
        this.riskScore = new RiskScore();
        this.auditMessages = new ArrayList<String>();
        this.fraudDecision = "PENDING";
        this.decisionReasonCode = "";
        this.recommendedAction = "";
    }

    public Transaction(String transactionId, double amount, String currency) {
        this();
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
    }

    // --- Getters and Setters ---

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public boolean isCardPresent() { return cardPresent; }
    public void setCardPresent(boolean cardPresent) { this.cardPresent = cardPresent; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public long getTransactionTimestampMs() { return transactionTimestampMs; }
    public void setTransactionTimestampMs(long transactionTimestampMs) {
        this.transactionTimestampMs = transactionTimestampMs;
    }

    public long getPreviousTransactionTimestampMs() { return previousTransactionTimestampMs; }
    public void setPreviousTransactionTimestampMs(long previousTransactionTimestampMs) {
        this.previousTransactionTimestampMs = previousTransactionTimestampMs;
    }

    public Location getMerchantLocation() { return merchantLocation; }
    public void setMerchantLocation(Location merchantLocation) { this.merchantLocation = merchantLocation; }

    public Location getPreviousTransactionLocation() { return previousTransactionLocation; }
    public void setPreviousTransactionLocation(Location previousTransactionLocation) {
        this.previousTransactionLocation = previousTransactionLocation;
    }

    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public int getTransactionCountLast1Hour() { return transactionCountLast1Hour; }
    public void setTransactionCountLast1Hour(int transactionCountLast1Hour) {
        this.transactionCountLast1Hour = transactionCountLast1Hour;
    }

    public int getTransactionCountLast24Hours() { return transactionCountLast24Hours; }
    public void setTransactionCountLast24Hours(int transactionCountLast24Hours) {
        this.transactionCountLast24Hours = transactionCountLast24Hours;
    }

    public int getDistinctCountriesLast24Hours() { return distinctCountriesLast24Hours; }
    public void setDistinctCountriesLast24Hours(int distinctCountriesLast24Hours) {
        this.distinctCountriesLast24Hours = distinctCountriesLast24Hours;
    }

    public int getDistinctCountriesLast7Days() { return distinctCountriesLast7Days; }
    public void setDistinctCountriesLast7Days(int distinctCountriesLast7Days) {
        this.distinctCountriesLast7Days = distinctCountriesLast7Days;
    }

    public double getTotalAmountLast1Hour() { return totalAmountLast1Hour; }
    public void setTotalAmountLast1Hour(double totalAmountLast1Hour) {
        this.totalAmountLast1Hour = totalAmountLast1Hour;
    }

    public double getTotalAmountLast24Hours() { return totalAmountLast24Hours; }
    public void setTotalAmountLast24Hours(double totalAmountLast24Hours) {
        this.totalAmountLast24Hours = totalAmountLast24Hours;
    }

    public RiskScore getRiskScore() { return riskScore; }
    public void setRiskScore(RiskScore riskScore) { this.riskScore = riskScore; }

    public String getFraudDecision() { return fraudDecision; }
    public void setFraudDecision(String fraudDecision) { this.fraudDecision = fraudDecision; }

    public String getDecisionReasonCode() { return decisionReasonCode; }
    public void setDecisionReasonCode(String decisionReasonCode) {
        this.decisionReasonCode = decisionReasonCode;
    }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public List<String> getAuditMessages() { return auditMessages; }
    public void setAuditMessages(List<String> auditMessages) { this.auditMessages = auditMessages; }

    // --- Computed Properties ---

    /**
     * Returns the elapsed time in minutes between the previous and current transaction.
     */
    public double getElapsedMinutesSincePreviousTransaction() {
        if (previousTransactionTimestampMs == 0 || transactionTimestampMs == 0) return 0.0;
        long diffMs = transactionTimestampMs - previousTransactionTimestampMs;
        if (diffMs < 0) return 0.0;
        return diffMs / 60000.0;
    }

    /**
     * Returns the distance in km between the current merchant location and the previous transaction location.
     */
    public double getDistanceFromPreviousTransactionKm() {
        if (merchantLocation == null || previousTransactionLocation == null) return 0.0;
        return merchantLocation.distanceTo(previousTransactionLocation);
    }

    /**
     * Returns the minimum required travel speed in km/h to cover the distance since the last transaction.
     * Returns 0 if elapsed time is zero or locations are missing.
     */
    public double getRequiredTravelSpeedKmh() {
        double distanceKm = getDistanceFromPreviousTransactionKm();
        double elapsedHours = getElapsedMinutesSincePreviousTransaction() / 60.0;
        if (elapsedHours <= 0 || distanceKm <= 0) return 0.0;
        return distanceKm / elapsedHours;
    }

    /**
     * Returns the distance in km between the IP geolocation and the merchant location.
     */
    public double getIpToMerchantDistanceKm() {
        if (deviceInfo == null || merchantLocation == null) return 0.0;
        return deviceInfo.ipToMerchantDistance(merchantLocation);
    }

    /**
     * Returns true if the transaction country is in the customer's known transaction history.
     */
    public boolean isInKnownTransactionCorridor() {
        if (customer == null || merchantLocation == null) return false;
        String countryCode = merchantLocation.getCountryCode();
        return customer.hasTransactionHistoryIn(countryCode)
                || customer.hasTravelNotificationFor(countryCode);
    }

    /**
     * Adds an audit message to the transaction log.
     */
    public void addAuditMessage(String message) {
        if (message != null) {
            auditMessages.add(message);
        }
    }

    @Override
    public String toString() {
        return "Transaction{id=" + transactionId + ", amount=" + amount + " " + currency
                + ", decision=" + fraudDecision + "}";
    }
}

// Made with Bob
