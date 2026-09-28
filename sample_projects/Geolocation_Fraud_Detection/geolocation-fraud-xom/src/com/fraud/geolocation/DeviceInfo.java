package com.fraud.geolocation;

/**
 * Represents device information associated with a transaction.
 * Includes device fingerprint, IP address, and geolocation derived from IP.
 */
public class DeviceInfo {

    private String deviceFingerprint;
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private boolean knownDevice;
    private Location ipGeolocation;
    private boolean vpnDetected;
    private boolean proxyDetected;
    private boolean torDetected;

    public DeviceInfo() {
    }

    public DeviceInfo(String deviceFingerprint, String ipAddress) {
        this.deviceFingerprint = deviceFingerprint;
        this.ipAddress = ipAddress;
    }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public boolean isKnownDevice() { return knownDevice; }
    public void setKnownDevice(boolean knownDevice) { this.knownDevice = knownDevice; }

    public Location getIpGeolocation() { return ipGeolocation; }
    public void setIpGeolocation(Location ipGeolocation) { this.ipGeolocation = ipGeolocation; }

    public boolean isVpnDetected() { return vpnDetected; }
    public void setVpnDetected(boolean vpnDetected) { this.vpnDetected = vpnDetected; }

    public boolean isProxyDetected() { return proxyDetected; }
    public void setProxyDetected(boolean proxyDetected) { this.proxyDetected = proxyDetected; }

    public boolean isTorDetected() { return torDetected; }
    public void setTorDetected(boolean torDetected) { this.torDetected = torDetected; }

    /**
     * Computes the distance in km between IP geolocation and a given merchant location.
     */
    public double ipToMerchantDistance(Location merchantLocation) {
        if (ipGeolocation == null || merchantLocation == null) return 0.0;
        return ipGeolocation.distanceTo(merchantLocation);
    }

    @Override
    public String toString() {
        return "DeviceInfo{fingerprint=" + deviceFingerprint + ", ip=" + ipAddress + "}";
    }
}

// Made with Bob
