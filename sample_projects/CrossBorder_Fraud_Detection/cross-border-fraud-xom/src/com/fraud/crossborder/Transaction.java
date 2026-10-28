package com.fraud.crossborder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cross-border financial transaction.
 * Primary input/output object for the CrossBorderFraudDetection decision service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transaction {

    // Core identifiers
    private String transactionId;
    private String currency;
    private double amount;

    // Card information
    private String cardIssuingCountry;

    // Merchant information
    private String merchantCountryCode;
    private String merchantCategory;
    private boolean merchantIsOffshore;
    private boolean merchantIsOnGreyList;
    private boolean merchantIsOnRegulatedWhitelist;

    // IP / Device origin
    private String ipCountryCode;

    // Customer profile
    private Customer customer;

    // Velocity / history metrics
    private int crossBorderTransactionCountLast24Hours;
    private double totalCrossBorderAmountLast24Hours;
    private int distinctCurrencyZonesLast24Hours;
    private boolean travelDeclared;

    // Compliance flags (pre-enriched by calling system)
    private boolean sanctionedCountry;
    private boolean highRiskAmlJurisdiction;
    private boolean exoticCurrency;
    private boolean mandatoryReporting;

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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCardIssuingCountry() { return cardIssuingCountry; }
    public void setCardIssuingCountry(String cardIssuingCountry) { this.cardIssuingCountry = cardIssuingCountry; }

    public String getMerchantCountryCode() { return merchantCountryCode; }
    public void setMerchantCountryCode(String merchantCountryCode) { this.merchantCountryCode = merchantCountryCode; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public boolean isMerchantIsOffshore() { return merchantIsOffshore; }
    public void setMerchantIsOffshore(boolean merchantIsOffshore) { this.merchantIsOffshore = merchantIsOffshore; }

    public boolean isMerchantIsOnGreyList() { return merchantIsOnGreyList; }
    public void setMerchantIsOnGreyList(boolean merchantIsOnGreyList) { this.merchantIsOnGreyList = merchantIsOnGreyList; }

    public boolean isMerchantIsOnRegulatedWhitelist() { return merchantIsOnRegulatedWhitelist; }
    public void setMerchantIsOnRegulatedWhitelist(boolean merchantIsOnRegulatedWhitelist) {
        this.merchantIsOnRegulatedWhitelist = merchantIsOnRegulatedWhitelist;
    }

    public String getIpCountryCode() { return ipCountryCode; }
    public void setIpCountryCode(String ipCountryCode) { this.ipCountryCode = ipCountryCode; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public int getCrossBorderTransactionCountLast24Hours() { return crossBorderTransactionCountLast24Hours; }
    public void setCrossBorderTransactionCountLast24Hours(int v) { this.crossBorderTransactionCountLast24Hours = v; }

    public double getTotalCrossBorderAmountLast24Hours() { return totalCrossBorderAmountLast24Hours; }
    public void setTotalCrossBorderAmountLast24Hours(double v) { this.totalCrossBorderAmountLast24Hours = v; }

    public int getDistinctCurrencyZonesLast24Hours() { return distinctCurrencyZonesLast24Hours; }
    public void setDistinctCurrencyZonesLast24Hours(int v) { this.distinctCurrencyZonesLast24Hours = v; }

    public boolean isTravelDeclared() { return travelDeclared; }
    public void setTravelDeclared(boolean travelDeclared) { this.travelDeclared = travelDeclared; }

    public boolean isSanctionedCountry() { return sanctionedCountry; }
    public void setSanctionedCountry(boolean sanctionedCountry) { this.sanctionedCountry = sanctionedCountry; }

    public boolean isHighRiskAmlJurisdiction() { return highRiskAmlJurisdiction; }
    public void setHighRiskAmlJurisdiction(boolean v) { this.highRiskAmlJurisdiction = v; }

    public boolean isExoticCurrency() { return exoticCurrency; }
    public void setExoticCurrency(boolean exoticCurrency) { this.exoticCurrency = exoticCurrency; }

    public boolean isMandatoryReporting() { return mandatoryReporting; }
    public void setMandatoryReporting(boolean mandatoryReporting) { this.mandatoryReporting = mandatoryReporting; }

    public RiskScore getRiskScore() { return riskScore; }
    public void setRiskScore(RiskScore riskScore) { this.riskScore = riskScore; }

    public String getFraudDecision() { return fraudDecision; }
    public void setFraudDecision(String fraudDecision) { this.fraudDecision = fraudDecision; }

    public String getDecisionReasonCode() { return decisionReasonCode; }
    public void setDecisionReasonCode(String decisionReasonCode) { this.decisionReasonCode = decisionReasonCode; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public List<String> getAuditMessages() { return auditMessages; }
    public void setAuditMessages(List<String> auditMessages) { this.auditMessages = auditMessages; }

    // --- Computed Properties (no setters → @JsonIgnore required) ---

    /**
     * Returns true if the merchant country differs from the card issuing country.
     */
    @JsonIgnore
    public boolean isCrossBorder() {
        if (merchantCountryCode == null || cardIssuingCountry == null) return false;
        return !merchantCountryCode.equals(cardIssuingCountry);
    }

    /**
     * Returns true if the IP country differs from the merchant country (geo-IP mismatch).
     */
    @JsonIgnore
    public boolean isIpMerchantCountryMismatch() {
        if (ipCountryCode == null || merchantCountryCode == null) return false;
        return !ipCountryCode.equals(merchantCountryCode);
    }

    /** Adds an audit message. */
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
