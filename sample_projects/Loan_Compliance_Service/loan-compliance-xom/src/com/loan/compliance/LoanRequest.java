package com.loan.compliance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class LoanRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Applicant applicant;
    private Applicant coSigner;
    private double loanAmount;
    private boolean eligible;
    private double interestRate;
    private String reason;
    private Collection<String> messages;
    private Collection<String> violations;
    
    public LoanRequest() {
        this.messages = new ArrayList<String>();
        this.violations = new ArrayList<String>();
        this.eligible = false;
        this.interestRate = 0.0;
    }
    
    public LoanRequest(Applicant applicant, double loanAmount) {
        this();
        this.applicant = applicant;
        this.loanAmount = loanAmount;
    }
    
    public Applicant getApplicant() {
        return applicant;
    }
    
    public void setApplicant(Applicant applicant) {
        this.applicant = applicant;
    }
    
    public Applicant getCoSigner() {
        return coSigner;
    }
    
    public void setCoSigner(Applicant coSigner) {
        this.coSigner = coSigner;
    }
    
    public double getLoanAmount() {
        return loanAmount;
    }
    
    public void setLoanAmount(double loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public boolean isEligible() {
        return eligible;
    }
    
    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }
    
    public double getInterestRate() {
        return interestRate;
    }
    
    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Collection<String> getMessages() {
        return messages;
    }
    
    public void setMessages(Collection<String> messages) {
        this.messages = messages;
    }
    
    public void addMessage(String message) {
        this.messages.add(message);
    }
    
    public Collection<String> getViolations() {
        return violations;
    }
    
    public void setViolations(Collection<String> violations) {
        this.violations = violations;
    }
    
    public void addViolation(String violation) {
        this.violations.add(violation);
    }
    
    // Computed property: has co-signer
    public boolean hasCoSigner() {
        return coSigner != null;
    }
    
    // Computed property: combined credit score (average if co-signer exists)
    public int getCombinedCreditScore() {
        if (applicant == null) {
            return 0;
        }
        if (coSigner == null) {
            return applicant.getCreditScore();
        }
        return (applicant.getCreditScore() + coSigner.getCreditScore()) / 2;
    }
    
    // Computed property: combined annual income
    public double getCombinedAnnualIncome() {
        double total = 0;
        if (applicant != null) {
            total += applicant.getAnnualIncome();
        }
        if (coSigner != null) {
            total += coSigner.getAnnualIncome();
        }
        return total;
    }
    
    // Computed property: loan-to-income ratio as percentage
    public double getLoanToIncomeRatio() {
        double combinedIncome = getCombinedAnnualIncome();
        if (combinedIncome == 0) {
            return 0;
        }
        return (loanAmount / combinedIncome) * 100;
    }
}

// Made with Bob
