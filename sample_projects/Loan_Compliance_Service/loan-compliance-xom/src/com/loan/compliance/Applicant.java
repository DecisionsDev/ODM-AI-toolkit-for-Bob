package com.loan.compliance;

import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;

public class Applicant implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Date birthDate;
    private Address address;
    private int creditScore;
    private double annualIncome;
    private String incomeDocument;
    private String employmentStatus;
    private boolean financialRecordPresent;
    private double monthlyDebtAmount;
    private double monthlyGrossIncome;
    
    public Applicant() {
    }
    
    public Applicant(Date birthDate, Address address, int creditScore, double annualIncome) {
        this.birthDate = birthDate;
        this.address = address;
        this.creditScore = creditScore;
        this.annualIncome = annualIncome;
    }
    
    public Date getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
    
    public Address getAddress() {
        return address;
    }
    
    public void setAddress(Address address) {
        this.address = address;
    }
    
    public int getCreditScore() {
        return creditScore;
    }
    
    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }
    
    public double getAnnualIncome() {
        return annualIncome;
    }
    
    public void setAnnualIncome(double annualIncome) {
        this.annualIncome = annualIncome;
    }
    
    public String getIncomeDocument() {
        return incomeDocument;
    }
    
    public void setIncomeDocument(String incomeDocument) {
        this.incomeDocument = incomeDocument;
    }
    
    public String getEmploymentStatus() {
        return employmentStatus;
    }
    
    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }
    
    public boolean isFinancialRecordPresent() {
        return financialRecordPresent;
    }
    
    public void setFinancialRecordPresent(boolean financialRecordPresent) {
        this.financialRecordPresent = financialRecordPresent;
    }
    
    public double getMonthlyDebtAmount() {
        return monthlyDebtAmount;
    }
    
    public void setMonthlyDebtAmount(double monthlyDebtAmount) {
        this.monthlyDebtAmount = monthlyDebtAmount;
    }
    
    public double getMonthlyGrossIncome() {
        return monthlyGrossIncome;
    }
    
    public void setMonthlyGrossIncome(double monthlyGrossIncome) {
        this.monthlyGrossIncome = monthlyGrossIncome;
    }
    
    // Computed property: age in years
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(birthDate);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    // Computed property: debt-to-income ratio as percentage
    public double getDebtToIncomeRatio() {
        if (monthlyGrossIncome == 0) {
            return 0;
        }
        return (monthlyDebtAmount / monthlyGrossIncome) * 100;
    }
}

// Made with Bob
