package com.aviation.compliance;

import java.util.ArrayList;
import java.util.List;

/**
 * Main request object for aviation compliance assessment
 */
public class ComplianceRequest {
    private Flight flight;
    private Aircraft aircraft;
    private Operator airlineOperator;
    private List<String> overallViolations;
    private List<String> overallMessages;
    private String overallComplianceStatus;
    private double totalPenaltyAmount;
    
    public ComplianceRequest() {
        this.overallViolations = new ArrayList<>();
        this.overallMessages = new ArrayList<>();
        this.overallComplianceStatus = "PENDING";
    }
    
    public ComplianceRequest(Flight flight, Aircraft aircraft, Operator airlineOperator) {
        this();
        this.flight = flight;
        this.aircraft = aircraft;
        this.airlineOperator = airlineOperator;
    }
    
    // Getters and Setters
    public Flight getFlight() {
        return flight;
    }
    
    public void setFlight(Flight flight) {
        this.flight = flight;
    }
    
    public Aircraft getAircraft() {
        return aircraft;
    }
    
    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }
    
    public Operator getAirlineOperator() {
        return airlineOperator;
    }
    
    public void setAirlineOperator(Operator airlineOperator) {
        this.airlineOperator = airlineOperator;
    }
    
    public List<String> getOverallViolations() {
        return overallViolations;
    }
    
    public void setOverallViolations(List<String> overallViolations) {
        this.overallViolations = overallViolations;
    }
    
    public List<String> getOverallMessages() {
        return overallMessages;
    }
    
    public void setOverallMessages(List<String> overallMessages) {
        this.overallMessages = overallMessages;
    }
    
    public String getOverallComplianceStatus() {
        return overallComplianceStatus;
    }
    
    public void setOverallComplianceStatus(String overallComplianceStatus) {
        this.overallComplianceStatus = overallComplianceStatus;
    }
    
    public double getTotalPenaltyAmount() {
        return totalPenaltyAmount;
    }
    
    public void setTotalPenaltyAmount(double totalPenaltyAmount) {
        this.totalPenaltyAmount = totalPenaltyAmount;
    }
    
    // Business methods
    public void addOverallViolation(String violation) {
        this.overallViolations.add(violation);
    }
    
    public void addOverallMessage(String message) {
        this.overallMessages.add(message);
    }
    
    public void addToPenalty(double amount) {
        this.totalPenaltyAmount += amount;
    }
    
    // Computed properties
    public boolean hasAnyViolations() {
        boolean flightViolations = flight != null && flight.hasViolations();
        boolean operatorViolations = airlineOperator != null && airlineOperator.hasViolations();
        return !overallViolations.isEmpty() || flightViolations || operatorViolations;
    }
    
    public int getTotalViolationCount() {
        int count = overallViolations.size();
        if (flight != null) {
            count += flight.getViolations().size();
        }
        if (airlineOperator != null) {
            count += airlineOperator.getViolations().size();
        }
        return count;
    }
}

// Made with Bob
