package com.loan.compliance;

import java.io.Serializable;

public class Address implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String country;
    private String state;
    private String city;
    private String zipCode;
    
    public Address() {
    }
    
    public Address(String country) {
        this.country = country;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}

// Made with Bob
