package com.fraud.crossborder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cardholder / customer profile for cross-border compliance.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Customer {

    private String customerId;
    private String residenceCountryCode;
    private boolean kycComplete;
    private boolean hasVerifiedAddress;
    private boolean hasIdDocument;
    private boolean hasBusinessProfile;
    private String riskTier;
    private List<String> knownTransactionCountries;
    private List<String> travelNotifications;

    public Customer() {
        this.knownTransactionCountries = new ArrayList<String>();
        this.travelNotifications = new ArrayList<String>();
        this.riskTier = "STANDARD";
    }

    public Customer(String customerId, String residenceCountryCode) {
        this();
        this.customerId = customerId;
        this.residenceCountryCode = residenceCountryCode;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getResidenceCountryCode() { return residenceCountryCode; }
    public void setResidenceCountryCode(String residenceCountryCode) {
        this.residenceCountryCode = residenceCountryCode;
    }

    public boolean isKycComplete() { return kycComplete; }
    public void setKycComplete(boolean kycComplete) { this.kycComplete = kycComplete; }

    public boolean isHasVerifiedAddress() { return hasVerifiedAddress; }
    public void setHasVerifiedAddress(boolean hasVerifiedAddress) { this.hasVerifiedAddress = hasVerifiedAddress; }

    public boolean isHasIdDocument() { return hasIdDocument; }
    public void setHasIdDocument(boolean hasIdDocument) { this.hasIdDocument = hasIdDocument; }

    public boolean isHasBusinessProfile() { return hasBusinessProfile; }
    public void setHasBusinessProfile(boolean hasBusinessProfile) { this.hasBusinessProfile = hasBusinessProfile; }

    public String getRiskTier() { return riskTier; }
    public void setRiskTier(String riskTier) { this.riskTier = riskTier; }

    public List<String> getKnownTransactionCountries() { return knownTransactionCountries; }
    public void setKnownTransactionCountries(List<String> v) { this.knownTransactionCountries = v; }

    public List<String> getTravelNotifications() { return travelNotifications; }
    public void setTravelNotifications(List<String> v) { this.travelNotifications = v; }

    public void addKnownTransactionCountry(String countryCode) {
        if (countryCode != null && !knownTransactionCountries.contains(countryCode)) {
            knownTransactionCountries.add(countryCode);
        }
    }

    public void addTravelNotification(String countryCode) {
        if (countryCode != null && !travelNotifications.contains(countryCode)) {
            travelNotifications.add(countryCode);
        }
    }

    /**
     * Returns true if the customer has transacted in the given country before.
     */
    public boolean hasTransactionHistoryIn(String countryCode) {
        if (countryCode == null || knownTransactionCountries == null) return false;
        return knownTransactionCountries.contains(countryCode);
    }

    /**
     * Returns true if a travel notification for the given country is on file.
     */
    public boolean hasTravelNotificationFor(String countryCode) {
        if (countryCode == null || travelNotifications == null) return false;
        return travelNotifications.contains(countryCode);
    }

    @Override
    public String toString() {
        return "Customer{id=" + customerId + ", residence=" + residenceCountryCode + "}";
    }
}

// Made with Bob
