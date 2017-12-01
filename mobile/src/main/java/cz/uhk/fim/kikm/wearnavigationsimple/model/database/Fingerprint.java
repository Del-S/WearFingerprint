package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Fingerprint {

    // Database labels for database
    private final static String DB_FINGERPRINT_ID = "id";
    private final static String DB_FINGERPRINT_SCAN_UUID = "scanUUID";
    private final static String DB_FINGERPRINT_X = "x";
    private final static String DB_FINGERPRINT_Y = "y";
    private final static String DB_FINGERPRINT_SCAN_START = "scanStart";
    private final static String DB_FINGERPRINT_SCAN_END = "scanEnd";

    // Variables of this class
    @Expose(serialize = false)
    private int id;                                 // Database id (its inner id and it is not exported)
    private UUID scanUUID;                          // UUID to enable fingerprint grouping
    private int x,y;                                // Calculated X and Y locations
    private int scanStart, scanEnd;                 // Timestamps of scan start/end
    private LocationEntry locationEntry;            // Location of fingerprint to enable multiple buildings and floors
    private List<BeaconEntry> beaconEntries;        // List of beacon entries scanned for this fingerprint
    private List<WirelessEntry> wirelessEntries;    // List of wireless entries scanned for this fingerprint
    private List<CellularEntry> cellularEntries;    // List of cellular entries scanned for this fingerprint
    private List<SensorEntry> sensorEntries;        // List of beacon entries scanned for this fingerprint
    private DeviceEntry deviceEntry;                // Device that created this fingerprint

    public Fingerprint() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getScanUUID() {
        return scanUUID;
    }

    public void setScanUUID(UUID scanUUID) {
        this.scanUUID = scanUUID;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getScanStart() {
        return scanStart;
    }

    public void setScanStart(int scanStart) {
        this.scanStart = scanStart;
    }

    public int getScanEnd() {
        return scanEnd;
    }

    public void setScanEnd(int scanEnd) {
        this.scanEnd = scanEnd;
    }

    public LocationEntry getLocationEntry() {
        return locationEntry;
    }

    public void setLocationEntry(LocationEntry locationEntry) {
        this.locationEntry = locationEntry;
    }

    public List<BeaconEntry> getBeaconEntries() {
        return beaconEntries;
    }

    public void setBeaconEntries(List<BeaconEntry> beaconEntries) {
        this.beaconEntries = beaconEntries;
    }

    public List<WirelessEntry> getWirelessEntries() {
        return wirelessEntries;
    }

    public void setWirelessEntries(List<WirelessEntry> wirelessEntries) {
        this.wirelessEntries = wirelessEntries;
    }

    public List<CellularEntry> getCellularEntries() {
        return cellularEntries;
    }

    public void setCellularEntries(List<CellularEntry> cellularEntries) {
        this.cellularEntries = cellularEntries;
    }

    public List<SensorEntry> getSensorEntries() {
        return sensorEntries;
    }

    public void setSensorEntries(List<SensorEntry> sensorEntries) {
        this.sensorEntries = sensorEntries;
    }

    public DeviceEntry getDeviceEntry() {
        return deviceEntry;
    }

    public void setDeviceEntry(DeviceEntry deviceEntry) {
        this.deviceEntry = deviceEntry;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Fingerprint fingerprint = (Fingerprint) o;
        return Objects.equals(this.scanUUID, fingerprint.scanUUID) &&
                Objects.equals(this.x, fingerprint.x) &&
                Objects.equals(this.y, fingerprint.y) &&
                Objects.equals(this.scanStart, fingerprint.scanStart) &&
                Objects.equals(this.scanEnd, fingerprint.scanEnd) &&
                Objects.equals(this.locationEntry, fingerprint.locationEntry) &&
                Objects.equals(this.beaconEntries, fingerprint.beaconEntries) &&
                Objects.equals(this.wirelessEntries, fingerprint.wirelessEntries) &&
                Objects.equals(this.cellularEntries, fingerprint.cellularEntries) &&
                Objects.equals(this.sensorEntries, fingerprint.sensorEntries) &&
                Objects.equals(this.deviceEntry, fingerprint.deviceEntry);

    }

    @Override
    public int hashCode() {
        return Objects.hash(scanUUID, x, y, scanStart, scanEnd, locationEntry, beaconEntries, wirelessEntries, cellularEntries, sensorEntries, deviceEntry);
    }


    @Override
    public String toString() {
        return "class BeaconEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    scanUUID: " + toIndentedString(scanUUID) + "\n" +
                "    x: " + toIndentedString(x) + "\n" +
                "    y: " + toIndentedString(y) + "\n" +
                "    scanStart: " + toIndentedString(scanStart) + "\n" +
                "    scanEnd: " + toIndentedString(scanEnd) + "\n" +
                "    locationEntry: " + toIndentedString(locationEntry) + "\n" +
                "    beaconEntries: " + toIndentedString(beaconEntries.size()) + "\n" +
                "    wirelessEntries: " + toIndentedString(wirelessEntries.size()) + "\n" +
                "    cellularEntries: " + toIndentedString(cellularEntries.size()) + "\n" +
                "    sensorEntries: " + toIndentedString(sensorEntries.size()) + "\n" +
                "    deviceEntry: " + toIndentedString(deviceEntry) + "\n" +
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
