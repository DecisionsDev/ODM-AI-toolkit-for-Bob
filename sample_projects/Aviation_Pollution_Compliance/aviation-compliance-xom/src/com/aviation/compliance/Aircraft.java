package com.aviation.compliance;

import java.util.Date;

/**
 * Represents an aircraft with certification and compliance data
 */
public class Aircraft {
    private String aircraftId;
    private String aircraftModel;
    private Date certificationDate;
    private double ratedThrustKn;
    private double certifiedNoxGramsPerKn;
    private double certifiedCo2PerRpk;
    private boolean meetsGaea2025Standard;
    private boolean requiresRetrofit;
    private Date retrofitDeadline;
    private Date phaseOutDate;
    
    public Aircraft() {
    }
    
    public Aircraft(String aircraftId, String aircraftModel) {
        this.aircraftId = aircraftId;
        this.aircraftModel = aircraftModel;
    }
    
    // Getters and Setters
    public String getAircraftId() {
        return aircraftId;
    }
    
    public void setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
    }
    
    public String getAircraftModel() {
        return aircraftModel;
    }
    
    public void setAircraftModel(String aircraftModel) {
        this.aircraftModel = aircraftModel;
    }
    
    public Date getCertificationDate() {
        return certificationDate;
    }
    
    public void setCertificationDate(Date certificationDate) {
        this.certificationDate = certificationDate;
    }
    
    public double getRatedThrustKn() {
        return ratedThrustKn;
    }
    
    public void setRatedThrustKn(double ratedThrustKn) {
        this.ratedThrustKn = ratedThrustKn;
    }
    
    public double getCertifiedNoxGramsPerKn() {
        return certifiedNoxGramsPerKn;
    }
    
    public void setCertifiedNoxGramsPerKn(double certifiedNoxGramsPerKn) {
        this.certifiedNoxGramsPerKn = certifiedNoxGramsPerKn;
    }
    
    public double getCertifiedCo2PerRpk() {
        return certifiedCo2PerRpk;
    }
    
    public void setCertifiedCo2PerRpk(double certifiedCo2PerRpk) {
        this.certifiedCo2PerRpk = certifiedCo2PerRpk;
    }
    
    public boolean isMeetsGaea2025Standard() {
        return meetsGaea2025Standard;
    }
    
    public void setMeetsGaea2025Standard(boolean meetsGaea2025Standard) {
        this.meetsGaea2025Standard = meetsGaea2025Standard;
    }
    
    public boolean isRequiresRetrofit() {
        return requiresRetrofit;
    }
    
    public void setRequiresRetrofit(boolean requiresRetrofit) {
        this.requiresRetrofit = requiresRetrofit;
    }
    
    public Date getRetrofitDeadline() {
        return retrofitDeadline;
    }
    
    public void setRetrofitDeadline(Date retrofitDeadline) {
        this.retrofitDeadline = retrofitDeadline;
    }
    
    public Date getPhaseOutDate() {
        return phaseOutDate;
    }
    
    public void setPhaseOutDate(Date phaseOutDate) {
        this.phaseOutDate = phaseOutDate;
    }
    
    // Computed properties
    public boolean isCertifiedAfter2025() {
        if (certificationDate == null) {
            return false;
        }
        // Check if certification date is on or after Jan 1, 2025
        Date gaea2025Date = new Date(125, 0, 1); // Year 2025
        return !certificationDate.before(gaea2025Date);
    }
}

// Made with Bob
