package com.fraud.geolocation;

/**
 * Represents a geographic location with latitude, longitude, and country information.
 * Used for merchant location, IP geolocation, and customer home location.
 */
public class Location {

    private double latitude;
    private double longitude;
    private String country;
    private String countryCode;
    private String city;
    private String region;
    private String postalCode;

    public Location() {
    }

    public Location(double latitude, double longitude, String country, String countryCode) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
        this.countryCode = countryCode;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    /**
     * Computes the Haversine distance in kilometers between this location and another.
     */
    public double distanceTo(Location other) {
        if (other == null) return 0.0;
        final double R = 6371.0;
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public String toString() {
        return "Location{lat=" + latitude + ", lon=" + longitude + ", country=" + country + "}";
    }
}

// Made with Bob
