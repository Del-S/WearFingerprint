package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class CellularEntry {

    // Database labels for database
    private final static String DB_CELLULAR_ID = "id";
    private final static String DB_CELLULAR_BSIC = "bsic";
    private final static String DB_CELLULAR_CID = "cid";
    private final static String DB_CELLULAR_LAC = "lac";
    private final static String DB_CELLULAR_RSSI = "rssi";
    private final static String DB_CELLULAR_DISTANCE = "distance";
    private final static String DB_CELLULAR_TIMESTAMP = "timestamp";
    private final static String DB_CELLULAR_SCAN_TIME = "scanTime";
    private final static String DB_CELLULAR_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private int bsic;           // Base Station Identity Code
    private int cid;            // GSM Cell Identity (CID Either 16-bit described in TS 27.007)
    private int lac;            // 16-bit Location Area Code
    private int rssi;           // Signal strength of the access point
    private float distance;     // Distance between access point and device
    private int timestamp;      // Device was found at this timestamp
    private int scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private int scanDifference;

    // Default constructor used for Gson
    public CellularEntry() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBsic() {
        return bsic;
    }

    public void setBsic(int bsic) {
        this.bsic = bsic;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
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

        CellularEntry cellularEntry = (CellularEntry) o;
        return Objects.equals(this.bsic, cellularEntry.bsic) &&
                Objects.equals(this.cid, cellularEntry.cid) &&
                Objects.equals(this.lac, cellularEntry.lac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bsic, cid, lac);
    }


    @Override
    public String toString() {
        return "class CellularEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    bsic: " + toIndentedString(bsic) + "\n" +
                "    cid: " + toIndentedString(cid) + "\n" +
                "    lac: " + toIndentedString(lac) + "\n" +
                "    rssi: " + toIndentedString(rssi) + "\n" +
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
