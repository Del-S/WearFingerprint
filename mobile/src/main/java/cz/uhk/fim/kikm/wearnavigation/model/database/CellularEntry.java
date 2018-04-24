package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.database.Cursor;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.gsm.GsmCellLocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(value = { "psc", "user", "type" })
public class CellularEntry implements Parcelable {

    // Database labels for database
    public final static String DB_TABLE = "cellular";
    public final static String DB_ID = "id";
    public final static String DB_FINGERPRINT_DB_ID = "fingerprintId";
    public final static String DB_BSIC = "bsic";
    public final static String DB_CID = "cid";
    public final static String DB_LAC = "lac";
    public final static String DB_RSSI = "rssi";
    public final static String DB_DISTANCE = "distance";
    public final static String DB_TIMESTAMP = "timestamp";
    public final static String DB_SCAN_TIME = "scanTime";
    public final static String DB_SCAN_DIFFERENCE = "scanDifference";

    // Variables of this class
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Expose(serialize = false)
    private int fingerprintId;  // If of fingerprint that this entry belongs to
    private int bsic;           // Base Station Identity Code
    private int cid;            // GSM Cell Identity (CID Either 16-bit described in TS 27.007)
    private int lac;            // 16-bit Location Area Code
    private int rssi;           // Signal strength of the access point
    private float distance;     // Distance between access point and device
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
    public CellularEntry() {}


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(fingerprintId);
        dest.writeInt(bsic);
        dest.writeInt(cid);
        dest.writeInt(lac);
        dest.writeInt(rssi);
        dest.writeFloat(distance);
        dest.writeLong(timestamp);
        dest.writeLong(scanTime);
        dest.writeLong(scanDifference);
    }

    private CellularEntry(Parcel in) {
        id = in.readInt();
        fingerprintId = in.readInt();
        bsic = in.readInt();
        cid = in.readInt();
        lac = in.readInt();
        rssi = in.readInt();
        distance = in.readFloat();
        timestamp = in.readLong();
        scanTime = in.readLong();
        scanDifference = in.readLong();
    }

    public static final Creator<CellularEntry> CREATOR = new Creator<CellularEntry>() {
        @Override
        public CellularEntry createFromParcel(Parcel in) {
            return new CellularEntry(in);
        }

        @Override
        public CellularEntry[] newArray(int size) {
            return new CellularEntry[size];
        }
    };

    /**
     * Create instance of CellularEntry from NeighboringCellInfo.
     *
     * @param neighboringCellInfo to get data from.
     */
    public CellularEntry(NeighboringCellInfo neighboringCellInfo) {
        this.cid = neighboringCellInfo.getCid();
        this.lac = neighboringCellInfo.getLac();
        this.rssi = neighboringCellInfo.getRssi();
    }

    /**
     * Create instance of CellularEntry from GsmCellLocation.
     *
     * @param gsmCellLocation to get data from.
     */
    public CellularEntry(GsmCellLocation gsmCellLocation, int rssi) {
        this.cid = gsmCellLocation.getCid();
        this.lac = gsmCellLocation.getLac();
        this.rssi = rssi;
    }

    /**
     * Create instance of CellularEntry from CellInfoGsm.
     *
     * @param cellInfoGsm to get data from.
     */
    private CellularEntry(CellInfoGsm cellInfoGsm) {
        CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.bsic = cellIdentityGsm.getBsic();
        }
        this.cid = cellIdentityGsm.getCid();
        this.lac = cellIdentityGsm.getLac();
        this.rssi = cellInfoGsm.getCellSignalStrength().getDbm();
    }

    /**
     * Create instance of CellularEntry from CellInfoLte.
     *
     * @param cellInfoLte to get data from.
     */
    private CellularEntry(CellInfoLte cellInfoLte) {
        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();

        this.cid = cellIdentityLte.getCi();
        this.lac = cellIdentityLte.getTac();
        this.rssi = cellInfoLte.getCellSignalStrength().getDbm();
    }

    /**
     * Create instance of CellularEntry from CellInfoWcdma.
     *
     * @param cellInfoWcdma to get data from.
     */
    private CellularEntry(CellInfoWcdma cellInfoWcdma) {
        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();

        this.cid = cellIdentityWcdma.getCid();
        this.lac = cellIdentityWcdma.getLac();
        this.rssi = cellInfoWcdma.getCellSignalStrength().getDbm();
    }

    /**
     * Load instance based on CellInfo. This can be instance of different classes so
     * we create instance of CellularEntry based on specific CellInfo instance.
     *
     * @param cellInfo to identify and get data from
     * @return instance of CellularEntry
     */
    public static CellularEntry createCellularEntry(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoGsm) {
            return new CellularEntry( (CellInfoGsm) cellInfo );
        } else if (cellInfo instanceof CellInfoLte) {
            return new CellularEntry( (CellInfoLte) cellInfo );
        } else if (cellInfo instanceof CellInfoWcdma) {
            return new CellularEntry( (CellInfoWcdma) cellInfo );
        }
        return new CellularEntry();
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
                "    fingerprintId: " + toIndentedString(fingerprintId) + "\n" +
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
