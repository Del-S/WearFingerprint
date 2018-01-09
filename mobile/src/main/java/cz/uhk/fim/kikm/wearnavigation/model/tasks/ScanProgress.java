package cz.uhk.fim.kikm.wearnavigation.model.tasks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class is used to post progress of the scan via Broadcast.
 * This classed is passed in the bundle as Parcelable.
 */
public class ScanProgress implements Parcelable {
    private int state;             // State of current scan
    private String stateString;    // State string of the current scan
    private int scanLength;        // Max length of current scan
    private int currentTime;       // Current elapsed time in the current scan

    // Counts of entries found
    private int beaconCount;        // BLE (beacon) entries
    private int wirelessCount;      // Wireless networks entries
    private int cellularCount;      // Cellular entries
    private int sensorCount;        // Sensor entries

    public ScanProgress() {
    }

    private ScanProgress(Parcel in) {
        setState(in.readInt());
        stateString = in.readString();
        scanLength = in.readInt();
        currentTime = in.readInt();
        beaconCount = in.readInt();
        wirelessCount = in.readInt();
        cellularCount = in.readInt();
        sensorCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getState());
        dest.writeString(stateString);
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

    public String getStateString() {
        return stateString;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setStateString(String stateString) {
        this.stateString = stateString;
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
