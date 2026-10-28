package com.skywings.luggage;

import java.util.ArrayList;
import java.util.List;

/**
 * Main request object for luggage compliance checking
 */
public class LuggageRequest {
    private String requestId;
    private Passenger passenger;
    private List<BaggageItem> baggageItems;
    private boolean isDomesticFlight;
    private String status;
    private double totalFees;
    private List<String> violations;
    private List<String> messages;
    
    public LuggageRequest() {
        this.baggageItems = new ArrayList<>();
        this.violations = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.status = "PENDING";
        this.totalFees = 0.0;
        this.isDomesticFlight = true;
    }
    
    public LuggageRequest(String requestId, Passenger passenger, boolean isDomesticFlight) {
        this.requestId = requestId;
        this.passenger = passenger;
        this.isDomesticFlight = isDomesticFlight;
        this.baggageItems = new ArrayList<>();
        this.violations = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.status = "PENDING";
        this.totalFees = 0.0;
    }
    
    // Helper methods
    public void addBaggageItem(BaggageItem item) {
        this.baggageItems.add(item);
    }
    
    public void addViolation(String violation) {
        this.violations.add(violation);
    }
    
    public void addMessage(String message) {
        this.messages.add(message);
    }
    
    public void addFee(double fee) {
        this.totalFees += fee;
    }
    
    // Computed properties
    public int getCarryOnCount() {
        int count = 0;
        for (BaggageItem item : baggageItems) {
            if (item.getBaggageType() == BaggageType.CARRY_ON) {
                count++;
            }
        }
        return count;
    }
    
    public int getCheckedBaggageCount() {
        int count = 0;
        for (BaggageItem item : baggageItems) {
            if (item.getBaggageType() == BaggageType.CHECKED) {
                count++;
            }
        }
        return count;
    }
    
    public double getTotalCarryOnWeightKg() {
        double total = 0.0;
        for (BaggageItem item : baggageItems) {
            if (item.getBaggageType() == BaggageType.CARRY_ON) {
                total += item.getWeightKg();
            }
        }
        return total;
    }
    
    public boolean hasViolations() {
        return !violations.isEmpty();
    }
    
    public boolean isCompliant() {
        return violations.isEmpty();
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Passenger getPassenger() {
        return passenger;
    }
    
    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }
    
    public List<BaggageItem> getBaggageItems() {
        return baggageItems;
    }
    
    public void setBaggageItems(List<BaggageItem> baggageItems) {
        this.baggageItems = baggageItems;
    }
    
    public boolean isDomesticFlight() {
        return isDomesticFlight;
    }
    
    public void setDomesticFlight(boolean isDomesticFlight) {
        this.isDomesticFlight = isDomesticFlight;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public double getTotalFees() {
        return totalFees;
    }
    
    public void setTotalFees(double totalFees) {
        this.totalFees = totalFees;
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
}

// Made with Bob
