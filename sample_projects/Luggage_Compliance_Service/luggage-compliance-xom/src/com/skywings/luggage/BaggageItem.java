package com.skywings.luggage;

/**
 * Represents a single piece of baggage
 */
public class BaggageItem {
    private String itemId;
    private BaggageType baggageType;
    private double weightKg;
    private int lengthCm;
    private int widthCm;
    private int heightCm;
    private SpecialItemType specialItemType;
    private boolean properlyPacked;
    
    public BaggageItem() {
        this.baggageType = BaggageType.CHECKED;
        this.specialItemType = SpecialItemType.NONE;
        this.properlyPacked = true;
    }
    
    public BaggageItem(String itemId, BaggageType baggageType, double weightKg, 
                       int lengthCm, int widthCm, int heightCm) {
        this.itemId = itemId;
        this.baggageType = baggageType;
        this.weightKg = weightKg;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.specialItemType = SpecialItemType.NONE;
        this.properlyPacked = true;
    }
    
    // Computed property: total dimensions (L + W + H)
    public int getTotalDimensionsCm() {
        return lengthCm + widthCm + heightCm;
    }
    
    // Computed property: check if overweight for economy class
    public boolean isOverweightEconomy() {
        return baggageType == BaggageType.CHECKED && weightKg > 23;
    }
    
    // Computed property: check if oversized
    public boolean isOversized() {
        int totalDim = getTotalDimensionsCm();
        return totalDim > 158 && totalDim <= 203;
    }
    
    // Computed property: check if exceeds maximum limits
    public boolean isExceedsMaximumLimits() {
        return weightKg > 32 || getTotalDimensionsCm() > 203;
    }
    
    // Getters and Setters
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public BaggageType getBaggageType() {
        return baggageType;
    }
    
    public void setBaggageType(BaggageType baggageType) {
        this.baggageType = baggageType;
    }
    
    public double getWeightKg() {
        return weightKg;
    }
    
    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }
    
    public int getLengthCm() {
        return lengthCm;
    }
    
    public void setLengthCm(int lengthCm) {
        this.lengthCm = lengthCm;
    }
    
    public int getWidthCm() {
        return widthCm;
    }
    
    public void setWidthCm(int widthCm) {
        this.widthCm = widthCm;
    }
    
    public int getHeightCm() {
        return heightCm;
    }
    
    public void setHeightCm(int heightCm) {
        this.heightCm = heightCm;
    }
    
    public SpecialItemType getSpecialItemType() {
        return specialItemType;
    }
    
    public void setSpecialItemType(SpecialItemType specialItemType) {
        this.specialItemType = specialItemType;
    }
    
    public boolean isProperlyPacked() {
        return properlyPacked;
    }
    
    public void setProperlyPacked(boolean properlyPacked) {
        this.properlyPacked = properlyPacked;
    }
}

// Made with Bob
