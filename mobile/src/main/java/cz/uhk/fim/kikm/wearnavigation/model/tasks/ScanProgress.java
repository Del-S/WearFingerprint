package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class is used to post progress oft he scan via Broadcast.
 * This classed is parsed to json and then send as a Broadcast.
 */
public class ScanProgress implements Parcelable {
    private String state;          // State of the current scan
    private int scanLength;        // Max length of current scan
    private int currentTime;       // Current elapsed time in the current scan

    // Counts of entries found
    private int beaconCount;        // BLE (beacon) entries
    private int wirelessCount;      // Wireless networks entries
    private int cellularCount;      // Cellular entries
    private int sensorCount;        // Sensor entries

    ScanProgress() {
    }

    ScanProgress(Parcel in) {
        state = in.readString();
        scanLength = in.readInt();
        currentTime = in.readInt();
        beaconCount = in.readInt();
        wirelessCount = in.readInt();
        cellularCount = in.readInt();
        sensorCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(state);
        dest.writeInt(scanLength);
        dest.writeInt(currentTime);
        dest.writeInt(beaconCount);
        dest.writeInt(wirelessCount);
        dest.writeInt(cellularCount);
        dest.writeInt(sensorCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScanProgress> CREATOR = new Creator<ScanProgress>() {
        @Override
        public ScanProgress createFromParcel(Parcel in) {
            return new ScanProgress(in);
        }

        @Override
        public ScanProgress[] newArray(int size) {
            return new ScanProgress[size];
        }
    };

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getScanLength() {
        return scanLength;
    }

    public void setScanLength(int scanLength) {
        this.scanLength = scanLength;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    public int getBeaconCount() {
        return beaconCount;
    }

    public void setBeaconCount(int beaconCount) {
        this.beaconCount = beaconCount;
    }

    public int getWirelessCount() {
        return wirelessCount;
    }

    public void setWirelessCount(int wirelessCount) {
        this.wirelessCount = wirelessCount;
    }

    public int getCellularCount() {
        return cellularCount;
    }

    public void setCellularCount(int cellularCount) {
        this.cellularCount = cellularCount;
    }

    public int getSensorCount() {
        return sensorCount;
    }

    public void setSensorCount(int sensorCount) {
        this.sensorCount = sensorCount;
    }
}
