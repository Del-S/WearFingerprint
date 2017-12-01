package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class WirelessEntry {

    // Database labels for database
    private final static String DB_WIRELESS_ID = "id";
    private final static String DB_WIRELESS_SSID = "ssid";
    private final static String DB_WIRELESS_BSSID = "bssid";
    private final static String DB_WIRELESS_RSSI = "rssi";
    private final static String DB_WIRELESS_FREQUENCY = "frequency";
    private final static String DB_WIRELESS_CHANNEL = "channel";
    private final static String DB_WIRELESS_DISTANCE = "distance";
    private final static String DB_WIRELESS_TIMESTAMP = "timestamp";
    private final static String DB_WIRELESS_SCAN_TIME = "scanTime";
    private final static String DB_WIRELESS_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private String ssid;        // Wifi network public ssid
    private String bssid;       // The address of the access point
    private int rssi;           // Signal strength of the access point
    private int frequency;      // Frequency on which access point broadcasts
    private int channel;        // Channel on which access point broadcasts
    private float distance;     // Distance between access point and device
    private int timestamp;      // Device was found at this timestamp
    private int scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private int scanDifference;

    // Default constructor used for Gson
    public WirelessEntry() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getScanTime() {
        return scanTime;
    }

    public void setScanTime(int scanTime) {
        this.scanTime = scanTime;
    }

    public int getScanDifference() {
        return scanDifference;
    }

    public void setScanDifference(int scanDifference) {
        this.scanDifference = scanDifference;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WirelessEntry wirelessEntry = (WirelessEntry) o;
        return Objects.equals(this.ssid, wirelessEntry.ssid) &&
                Objects.equals(this.bssid, wirelessEntry.bssid) &&
                Objects.equals(this.frequency, wirelessEntry.frequency) &&
                Objects.equals(this.channel, wirelessEntry.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssid, bssid, frequency, channel);
    }


    @Override
    public String toString() {
        return "class WirelessEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    ssid: " + toIndentedString(ssid) + "\n" +
                "    bssid: " + toIndentedString(bssid) + "\n" +
                "    rssi: " + toIndentedString(rssi) + "\n" +
                "    frequency: " + toIndentedString(frequency) + "\n" +
                "    channel: " + toIndentedString(channel) + "\n" +
                "    distance: " + toIndentedString(distance) + "\n" +
                "    timestamp: " + toIndentedString(timestamp) + "\n" +
                "    scanTime: " + toIndentedString(scanTime) + "\n" +
                "    scanDifference: " + toIndentedString(scanDifference) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
