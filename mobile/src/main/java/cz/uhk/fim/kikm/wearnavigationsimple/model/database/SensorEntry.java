package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class SensorEntry {

    // Database labels for database
    private final static String DB_SENSOR_ID = "id";
    private final static String DB_SENSOR_TYPE = "type";
    private final static String DB_SENSOR_X = "x";
    private final static String DB_SENSOR_Y = "y";
    private final static String DB_SENSOR_Z = "z";
    private final static String DB_SENSOR_TIMESTAMP = "timestamp";
    private final static String DB_SENSOR_SCAN_TIME = "scanTime";
    private final static String DB_SENSOR_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private int type;           // Identification of sensor type
    private float x,y,z;        // Sensor axis information
    private int timestamp;      // Device was found at this timestamp
    private int scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private int scanDifference;

    // Default constructor used for Gson
    public SensorEntry() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
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

        SensorEntry sensorEntry = (SensorEntry) o;
        return Objects.equals(this.type, sensorEntry.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }


    @Override
    public String toString() {
        return "class CellularEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    type: " + toIndentedString(type) + "\n" +
                "    x: " + toIndentedString(x) + "\n" +
                "    y: " + toIndentedString(y) + "\n" +
                "    z: " + toIndentedString(z) + "\n" +
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
