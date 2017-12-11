package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(value = { "acceleration" })
public class SensorEntry {

    private final String TAG = "SensorEntry";

    // Database labels for database
    public final static String DB_TABLE = "sensor";
    public final static String DB_ID = "id";
    public final static String DB_FINGERPRINT_DB_ID = "fingerprintId";
    public final static String DB_TYPE = "type";
    public final static String DB_TYPE_STRING = "typeString";
    public final static String DB_X = "x";
    public final static String DB_Y = "y";
    public final static String DB_Z = "z";
    public final static String DB_TIMESTAMP = "timestamp";
    public final static String DB_SCAN_TIME = "scanTime";
    public final static String DB_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private int fingerprintId; // If of fingerprint that this entry belongs to
    private int type;           // Identification of sensor type
    /**
     * @Deprecated
     * Use type instead of typeString.
     */
    private String typeString;           // Identification of sensor type
    private double x,y,z;        // Sensor axis information
    private long timestamp;      // Device was found at this timestamp
    private long scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private long scanDifference;

    // Default constructor used for Gson
    public SensorEntry() {}

    public SensorEntry(Object object) {
        Map<String, Object> baseSensorRecord = (HashMap<String, Object>) object;
        this.x = Float.parseFloat(baseSensorRecord.get("x").toString());
        this.y = Float.parseFloat(baseSensorRecord.get("y").toString());
        this.z = Float.parseFloat(baseSensorRecord.get("z").toString());
        //this.time = Integer.parseInt(baseSensorRecord.get("time").toString());
        //this.difference = Integer.parseInt(baseSensorRecord.get("difference").toString());
    }

    @JsonAnySetter
    public void setSensorEntry(String key, Object value) {
        typeString = key;
        // TODO: set type by typeString (create array of types ints and strings). Switch case them and set the (int) type from (string) typeString
        try {
            Map<String, Double> map = (Map<String, Double>) value;

            x = map.get("x");
            y = map.get("y");
            z = map.get("z");
        } catch(ClassCastException e) {
            Log.e(TAG, "CastException in setSensorEntry().", e);
        }
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
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
