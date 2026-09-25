package com.aviation.compliance;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a flight with emissions and compliance data
 */
public class Flight {
    private String aircraftId;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private Date flightDate;
    private double flightTimeHours;
    private String fuelType;
    private double fuelVolumeKg;
    private double co2EmissionsKg;
    private double noxEmissionsGrams;
    private double particulateMatterMg;
    private double safBlendPercentage;
    private boolean safCertified;
    private double safLifecycleReduction;
    private boolean apuUsed;
    private boolean fegpAvailable;
    private int passengerCount;
    private double distanceKm;
    private List<String> violations;
    private List<String> messages;
    private String complianceStatus;
    
    public Flight() {
        this.violations = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.complianceStatus = "PENDING";
    }
    
    public Flight(String aircraftId, String flightNumber) {
        this();
        this.aircraftId = aircraftId;
        this.flightNumber = flightNumber;
    }
    
    // Getters and Setters
    public String getAircraftId() {
        return aircraftId;
    }
    
    public void setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
    }
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
    
    public String getDepartureAirport() {
        return departureAirport;
    }
    
    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }
    
    public String getArrivalAirport() {
        return arrivalAirport;
    }
    
    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }
    
    public Date getFlightDate() {
        return flightDate;
    }
    
    public void setFlightDate(Date flightDate) {
        this.flightDate = flightDate;
    }
    
    public double getFlightTimeHours() {
        return flightTimeHours;
    }
    
    public void setFlightTimeHours(double flightTimeHours) {
        this.flightTimeHours = flightTimeHours;
    }
    
    public String getFuelType() {
        return fuelType;
    }
    
    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
    
    public double getFuelVolumeKg() {
        return fuelVolumeKg;
    }
    
    public void setFuelVolumeKg(double fuelVolumeKg) {
        this.fuelVolumeKg = fuelVolumeKg;
    }
    
    public double getCo2EmissionsKg() {
        return co2EmissionsKg;
    }
    
    public void setCo2EmissionsKg(double co2EmissionsKg) {
        this.co2EmissionsKg = co2EmissionsKg;
    }
    
    public double getNoxEmissionsGrams() {
        return noxEmissionsGrams;
    }
    
    public void setNoxEmissionsGrams(double noxEmissionsGrams) {
        this.noxEmissionsGrams = noxEmissionsGrams;
    }
    
    public double getParticulateMatterMg() {
        return particulateMatterMg;
    }
    
    public void setParticulateMatterMg(double particulateMatterMg) {
        this.particulateMatterMg = particulateMatterMg;
    }
    
    public double getSafBlendPercentage() {
        return safBlendPercentage;
    }
    
    public void setSafBlendPercentage(double safBlendPercentage) {
        this.safBlendPercentage = safBlendPercentage;
    }
    
    public boolean isSafCertified() {
        return safCertified;
    }
    
    public void setSafCertified(boolean safCertified) {
        this.safCertified = safCertified;
    }
    
    public double getSafLifecycleReduction() {
        return safLifecycleReduction;
    }
    
    public void setSafLifecycleReduction(double safLifecycleReduction) {
        this.safLifecycleReduction = safLifecycleReduction;
    }
    
    public boolean isApuUsed() {
        return apuUsed;
    }
    
    public void setApuUsed(boolean apuUsed) {
        this.apuUsed = apuUsed;
    }
    
    public boolean isFegpAvailable() {
        return fegpAvailable;
    }
    
    public void setFegpAvailable(boolean fegpAvailable) {
        this.fegpAvailable = fegpAvailable;
    }
    
    public int getPassengerCount() {
        return passengerCount;
    }
    
    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }
    
    public double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
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
    
    public String getComplianceStatus() {
        return complianceStatus;
    }
    
    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }
    
    // Business methods
    public void addViolation(String violation) {
        this.violations.add(violation);
    }
    
    public void addMessage(String message) {
        this.messages.add(message);
    }
    
    // Computed properties
    public double getCo2PerRpk() {
        if (passengerCount == 0 || distanceKm == 0) {
            return 0;
        }
        double rpk = passengerCount * distanceKm;
        return (co2EmissionsKg * 1000) / rpk; // Convert kg to grams
    }
    
    public double getPmPerKgFuel() {
        if (fuelVolumeKg == 0) {
            return 0;
        }
        return particulateMatterMg / fuelVolumeKg;
    }
    
    public boolean isCompliant() {
        return violations.isEmpty() && "COMPLIANT".equals(complianceStatus);
    }
    
    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}

// Made with Bob
