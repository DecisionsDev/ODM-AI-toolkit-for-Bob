package com.healthcare.cvd;

import java.util.ArrayList;
import java.util.List;

public class Patient {
    private int age;
    private String sex;
    private int systolicBP;
    private int diastolicBP;
    private int ldlCholesterol;
    private int hdlCholesterol;
    private int triglycerides;
    private String smokingHistory;
    private boolean hasDiabetes;
    private boolean hasFamilyHistory;
    private boolean onBPMedication;
    private boolean onCholesterolMedication;
    private String riskLevel;
    private List<String> messages;
    private boolean requiresManualReview;

    public Patient() {
        this.messages = new ArrayList<>();
        this.riskLevel = "";
        this.requiresManualReview = false;
    }

    public Patient(int age, String sex, int systolicBP, int diastolicBP, 
                   int ldlCholesterol, int hdlCholesterol, int triglycerides,
                   String smokingHistory, boolean hasDiabetes, boolean hasFamilyHistory,
                   boolean onBPMedication, boolean onCholesterolMedication) {
        this();
        this.age = age;
        this.sex = sex;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.ldlCholesterol = ldlCholesterol;
        this.hdlCholesterol = hdlCholesterol;
        this.triglycerides = triglycerides;
        this.smokingHistory = smokingHistory;
        this.hasDiabetes = hasDiabetes;
        this.hasFamilyHistory = hasFamilyHistory;
        this.onBPMedication = onBPMedication;
        this.onCholesterolMedication = onCholesterolMedication;
    }

    // Getters and Setters
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getSystolicBP() {
        return systolicBP;
    }

    public void setSystolicBP(int systolicBP) {
        this.systolicBP = systolicBP;
    }

    public int getDiastolicBP() {
        return diastolicBP;
    }

    public void setDiastolicBP(int diastolicBP) {
        this.diastolicBP = diastolicBP;
    }

    public int getLdlCholesterol() {
        return ldlCholesterol;
    }

    public void setLdlCholesterol(int ldlCholesterol) {
        this.ldlCholesterol = ldlCholesterol;
    }

    public int getHdlCholesterol() {
        return hdlCholesterol;
    }

    public void setHdlCholesterol(int hdlCholesterol) {
        this.hdlCholesterol = hdlCholesterol;
    }

    public int getTriglycerides() {
        return triglycerides;
    }

    public void setTriglycerides(int triglycerides) {
        this.triglycerides = triglycerides;
    }

    public String getSmokingHistory() {
        return smokingHistory;
    }

    public void setSmokingHistory(String smokingHistory) {
        this.smokingHistory = smokingHistory;
    }

    public boolean isHasDiabetes() {
        return hasDiabetes;
    }

    public void setHasDiabetes(boolean hasDiabetes) {
        this.hasDiabetes = hasDiabetes;
    }

    public boolean isHasFamilyHistory() {
        return hasFamilyHistory;
    }

    public void setHasFamilyHistory(boolean hasFamilyHistory) {
        this.hasFamilyHistory = hasFamilyHistory;
    }

    public boolean isOnBPMedication() {
        return onBPMedication;
    }

    public void setOnBPMedication(boolean onBPMedication) {
        this.onBPMedication = onBPMedication;
    }

    public boolean isOnCholesterolMedication() {
        return onCholesterolMedication;
    }

    public void setOnCholesterolMedication(boolean onCholesterolMedication) {
        this.onCholesterolMedication = onCholesterolMedication;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }

    // Helper methods
    public void addMessage(String message) {
        this.messages.add(message);
    }

    public void flagForManualReview(String reason) {
        this.requiresManualReview = true;
        this.addMessage("MANUAL REVIEW REQUIRED: " + reason);
    }
}

// Made with Bob
