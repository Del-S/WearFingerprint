package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class WirelessEntry implements Parcelable {

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
    private long scanTime;      // Device was found at this time during the scan (seconds)
    /**
     * Difference between scanTime and last scanDifference (device based by bssid).
     * Informs about the time difference between this entry and previous one.
     */
    private long scanDifference;

    // Default constructor used for Gson
    public WirelessEntry() {
    }

    private WirelessEntry(Parcel in) {
        id = in.readInt();
        fingerprintId = in.readInt();
        ssid = in.readString();
        bssid = in.readString();
        rssi = in.readInt();
        frequency = in.readInt();
        channel = in.readInt();
        distance = in.readFloat();
        timestamp = in.readLong();
        scanTime = in.readLong();
        scanDifference = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(fingerprintId);
        dest.writeString(ssid);
        dest.writeString(bssid);
        dest.writeInt(rssi);
        dest.writeInt(frequency);
        dest.writeInt(channel);
        dest.writeFloat(distance);
        dest.writeLong(timestamp);
        dest.writeLong(scanTime);
        dest.writeLong(scanDifference);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WirelessEntry> CREATOR = new Creator<WirelessEntry>() {
        @Override
        public WirelessEntry createFromParcel(Parcel in) {
            return new WirelessEntry(in);
        }

        @Override
        public WirelessEntry[] newArray(int size) {
            return new WirelessEntry[size];
        }
    };

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

    /**
     * Parse frequency into channel number.
     * Found here: https://stackoverflow.com/questions/5485759/how-to-determine-a-wifi-channel-number-used-by-wifi-ap-network
     *
     * @param frequency to parse
     */
    public void setChannelByFrequency(int frequency) {
        if (frequency == 2484) {
            this.channel = 14;
            return;
        }

        if (frequency < 2484) {
            this.channel = (frequency - 2407) / 5;
            return;
        }

        this.channel = (frequency / 5) - 1000;
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
    public boolean equals(Object o) {
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
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
