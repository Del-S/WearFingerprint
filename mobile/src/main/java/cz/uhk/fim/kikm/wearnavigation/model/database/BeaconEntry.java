package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BeaconEntry implements Parcelable {

    // Database labels for database
    public final static String DB_TABLE = "beacon";
    public final static String DB_ID = "id";
    public final static String DB_FINGERPRINT_DB_ID = "fingerprintId";
    public final static String DB_BSSID = "bssid";
    public final static String DB_DISTANCE = "distance";
    public final static String DB_RSSI = "rssi";
    public final static String DB_TIMESTAMP = "timestamp";
    public final static String DB_SCAN_TIME = "scanTime";
    public final static String DB_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @Expose(serialize = false)
    private long id;             // Database id (its inner id and it is not exported)
    private int fingerprintId; // If of fingerprint that this entry belongs to
    private String bssid;       // Bssid (MAC) address of the beacon
    private float distance;     // Distance of the beacon from the device
    private int rssi;           // Signal strength of the beacon
    private long timestamp;      // Device was found at this timestamp
    @SerializedName("time")
    @JsonProperty("time")
    private long scanTime;       // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    @SerializedName("difference")
    @JsonProperty("difference")
    private long scanDifference;

    // Default constructor used for Gson
    public BeaconEntry() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(fingerprintId);
        dest.writeString(bssid);
        dest.writeFloat(distance);
        dest.writeInt(rssi);
        dest.writeLong(timestamp);
        dest.writeLong(scanTime);
        dest.writeLong(scanDifference);
    }

    private BeaconEntry(Parcel in) {
        id = in.readLong();
        fingerprintId = in.readInt();
        bssid = in.readString();
        distance = in.readFloat();
        rssi = in.readInt();
        timestamp = in.readLong();
        scanTime = in.readLong();
        scanDifference = in.readLong();
    }

    public static final Creator<BeaconEntry> CREATOR = new Creator<BeaconEntry>() {
        @Override
        public BeaconEntry createFromParcel(Parcel in) {
            return new BeaconEntry(in);
        }

        @Override
        public BeaconEntry[] newArray(int size) {
            return new BeaconEntry[size];
        }
    };

    // Default constructor used for Gson
    public BeaconEntry(String bssid) {
        this.bssid = bssid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getFingerprintId() {
        return fingerprintId;
    }

    public void setFingerprintId(int fingerprintId) {
        this.fingerprintId = fingerprintId;
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
