package com.skywings.luggage;

/**
 * Represents a passenger with travel class and age category
 */
public class Passenger {
    private String passengerId;
    private String name;
    private TravelClass travelClass;
    private AgeCategory ageCategory;
    private boolean hasDisability;
    
    public Passenger() {
        this.travelClass = TravelClass.ECONOMY;
        this.ageCategory = AgeCategory.ADULT;
        this.hasDisability = false;
    }
    
    public Passenger(String passengerId, String name, TravelClass travelClass, AgeCategory ageCategory) {
        this.passengerId = passengerId;
        this.name = name;
        this.travelClass = travelClass;
        this.ageCategory = ageCategory;
        this.hasDisability = false;
    }
    
    // Getters and Setters
    public String getPassengerId() {
        return passengerId;
    }
    
    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public TravelClass getTravelClass() {
        return travelClass;
    }
    
    public void setTravelClass(TravelClass travelClass) {
        this.travelClass = travelClass;
    }
    
    public AgeCategory getAgeCategory() {
        return ageCategory;
    }
    
    public void setAgeCategory(AgeCategory ageCategory) {
        this.ageCategory = ageCategory;
    }
    
    public boolean isDisability() {
        return hasDisability;
    }
    
    public void setDisability(boolean hasDisability) {
        this.hasDisability = hasDisability;
    }
    
    // Helper methods for BAL rules to check travel class
    public boolean isEconomyClass() {
        return travelClass == TravelClass.ECONOMY;
    }
    
    public boolean isBusinessClass() {
        return travelClass == TravelClass.BUSINESS;
    }
    
    public boolean isFirstClass() {
        return travelClass == TravelClass.FIRST;
    }
    
    public boolean isPremiumClass() {
        return travelClass == TravelClass.BUSINESS || travelClass == TravelClass.FIRST;
    }
}

// Made with Bob
