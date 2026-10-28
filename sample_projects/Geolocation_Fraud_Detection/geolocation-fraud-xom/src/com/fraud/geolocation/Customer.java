package com.fraud.geolocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank customer with home location, transaction history metadata,
 * and known travel corridors used for geolocation fraud detection.
 */
public class Customer {

    private String customerId;
    private String firstName;
    private String lastName;
    private String homeCountry;
    private String homeCountryCode;
    private Location homeLocation;
    private int accountAgeDays;
    private boolean highValueCustomer;
    private List<String> knownTransactionCountries;
    private List<String> travelNotifications;
    private int totalTransactionCount;
    private double averageTransactionAmount;

    public Customer() {
        this.knownTransactionCountries = new ArrayList<String>();
        this.travelNotifications = new ArrayList<String>();
    }

    public Customer(String customerId, String homeCountry, String homeCountryCode) {
        this();
        this.customerId = customerId;
        this.homeCountry = homeCountry;
        this.homeCountryCode = homeCountryCode;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getHomeCountry() { return homeCountry; }
    public void setHomeCountry(String homeCountry) { this.homeCountry = homeCountry; }

    public String getHomeCountryCode() { return homeCountryCode; }
    public void setHomeCountryCode(String homeCountryCode) { this.homeCountryCode = homeCountryCode; }

    public Location getHomeLocation() { return homeLocation; }
    public void setHomeLocation(Location homeLocation) { this.homeLocation = homeLocation; }

    public int getAccountAgeDays() { return accountAgeDays; }
    public void setAccountAgeDays(int accountAgeDays) { this.accountAgeDays = accountAgeDays; }

    public boolean isHighValueCustomer() { return highValueCustomer; }
    public void setHighValueCustomer(boolean highValueCustomer) { this.highValueCustomer = highValueCustomer; }

    public List<String> getKnownTransactionCountries() { return knownTransactionCountries; }
    public void setKnownTransactionCountries(List<String> knownTransactionCountries) {
        this.knownTransactionCountries = knownTransactionCountries;
    }

    public List<String> getTravelNotifications() { return travelNotifications; }
    public void setTravelNotifications(List<String> travelNotifications) {
        this.travelNotifications = travelNotifications;
    }

    public int getTotalTransactionCount() { return totalTransactionCount; }
    public void setTotalTransactionCount(int totalTransactionCount) {
        this.totalTransactionCount = totalTransactionCount;
    }

    public double getAverageTransactionAmount() { return averageTransactionAmount; }
    public void setAverageTransactionAmount(double averageTransactionAmount) {
        this.averageTransactionAmount = averageTransactionAmount;
    }

    /**
     * Returns true if the given country code is in the customer's known transaction history.
     */
    public boolean hasTransactionHistoryIn(String countryCode) {
        if (countryCode == null || knownTransactionCountries == null) return false;
        return knownTransactionCountries.contains(countryCode);
    }

    /**
     * Returns true if the customer has filed a travel notification for the given country code.
     */
    public boolean hasTravelNotificationFor(String countryCode) {
        if (countryCode == null || travelNotifications == null) return false;
        return travelNotifications.contains(countryCode);
    }

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

    @Override
    public String toString() {
        return "Customer{id=" + customerId + ", homeCountry=" + homeCountry + "}";
    }
}

// Made with Bob
