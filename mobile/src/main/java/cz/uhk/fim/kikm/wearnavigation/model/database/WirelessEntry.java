package cz.uhk.fim.kikm.wearnavigation.model.database;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(value = { "technology" })
public class WirelessEntry {

    // Database labels for database
    public final static String DB_TABLE = "wireless";
    public final static String DB_ID = "id";
    public final static String DB_FINGERPRINT_DB_ID = "fingerprintId";
    public final static String DB_SSID = "ssid";
    public final static String DB_BSSID = "bssid";
    public final static String DB_RSSI = "rssi";
    public final static String DB_FREQUENCY = "frequency";
    public final static String DB_CHANNEL = "channel";
    public final static String DB_DISTANCE = "distance";
    public final static String DB_TIMESTAMP = "timestamp";
    public final static String DB_SCAN_TIME = "scanTime";
    public final static String DB_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private int fingerprintId;  // If of fingerprint that this entry belongs to
    private String ssid;        // Wifi network public ssid
    private String bssid;       // The address of the access point
    private int rssi;           // Signal strength of the access point
    private int frequency;      // Frequency on which access point broadcasts
    private int channel;        // Channel on which access point broadcasts
    private float distance;     // Distance between access point and device
    private long timestamp;     // Device was found at this timestamp
    @JsonProperty("time")
    private long scanTime;      // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    @JsonProperty("difference")
    private long scanDifference;

    // Default constructor used for Gson
    public WirelessEntry() {
    }

    public WirelessEntry(Object object) {
        Map<String, Object> wirelessRecord = (HashMap<String, Object>) object;
        this.timestamp = Long.valueOf(wirelessRecord.get("timestamp").toString());
        this.bssid = wirelessRecord.get("bssid").toString();
        this.ssid = wirelessRecord.get("ssid").toString();
        this.rssi = Integer.valueOf(wirelessRecord.get("rssi").toString());
        this.distance = Float.valueOf(wirelessRecord.get("distance").toString());
        this.scanTime = Long.valueOf(wirelessRecord.get("time").toString());
        this.scanDifference = Long.valueOf(wirelessRecord.get("difference").toString());
        this.frequency = Integer.valueOf(wirelessRecord.get("frequency").toString());
        this.channel = Integer.valueOf(wirelessRecord.get("channel").toString());
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFingerprintId() {
        return fingerprintId;
    }

    public void setFingerprintId(int fingerprintId) {
        this.fingerprintId = fingerprintId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getScanTime() {
        return scanTime;
    }

    public void setScanTime(long scanTime) {
        this.scanTime = scanTime;
    }

    public long getScanDifference() {
        return scanDifference;
    }

    public void setScanDifference(long scanDifference) {
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
