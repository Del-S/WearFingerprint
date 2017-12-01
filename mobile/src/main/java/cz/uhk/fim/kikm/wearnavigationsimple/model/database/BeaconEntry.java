package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class BeaconEntry {

    // Database labels for database
    private final static String DB_BEACON_ID = "id";
    private final static String DB_BEACON_BSSID = "bssid";
    private final static String DB_BEACON_DISTANCE = "distance";
    private final static String DB_BEACON_RSSI = "rssi";
    private final static String DB_BEACON_TIMESTAMP = "timestamp";
    private final static String DB_BEACON_SCAN_TIME = "scanTime";
    private final static String DB_BEACON_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private String bssid;       // Bssid (MAC) address of the beacon
    private float distance;     // Distance of the beacon from the device
    private int rssi;           // Signal strength of the beacon
    private int timestamp;      // Device was found at this timestamp
    private int scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private int scanDifference;

    // Default constructor used for Gson
    public BeaconEntry() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
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

        BeaconEntry beaconEntry = (BeaconEntry) o;
        return Objects.equals(this.bssid, beaconEntry.bssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bssid);
    }


    @Override
    public String toString() {
        return "class BeaconEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    bssid: " + toIndentedString(bssid) + "\n" +
                "    distance: " + toIndentedString(distance) + "\n" +
                "    rssi: " + toIndentedString(rssi) + "\n" +
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
