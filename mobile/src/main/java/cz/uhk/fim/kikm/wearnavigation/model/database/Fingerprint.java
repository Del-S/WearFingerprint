package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(value = { "_rev", "user" })
public class Fingerprint {

    // Database labels for database
    public final static String DB_TABLE = "fingerprint";
    public final static String DB_ID = "dbId";
    public final static String DB_FINGERPRINT_ID = "id";
    public final static String DB_FINGERPRINT_SCAN_ID = "scanID";
    public final static String DB_X = "x";
    public final static String DB_Y = "y";
    public final static String DB_SCAN_START = "scanStart";
    public final static String DB_SCAN_END = "scanEnd";
    public final static String DB_LEVEL = "level";
    public final static String DB_LOCATION_ID = "location_id";
    public final static String DB_DEVICE_ID = "device_id";

    // Variables of this class
    @Expose(serialize = false)
    private int dbId;                              // Database id (its inner id and it is not exported)
    @JsonProperty("_id")
    private UUID id;                                // UUID of this scan
    private UUID scanID;                            // UUID to enable fingerprint grouping
    private int x,y;                                // Calculated X and Y locations
    @JsonProperty("timestamp")
    private long scanStart;                         // Timestamps of scan start
    @JsonProperty("finish")
    private long  scanEnd;                          // Timestamps of scan end
    /**
     * @Deprecated
     * Use LocationEntry instead
     */
    private String level;
    private long location_id;
    private LocationEntry locationEntry;            // Location of fingerprint to enable multiple buildings and floors
    private long device_id;
    @JsonProperty("deviceRecord")
    private DeviceEntry deviceEntry;                // Device that created this fingerprint
    @JsonProperty("bluetoothRecords")
    private List<BeaconEntry> beaconEntries;        // List of beacon entries scanned for this fingerprint
    @JsonProperty("wirelessRecords")
    private List<WirelessEntry> wirelessEntries;    // List of wireless entries scanned for this fingerprint
    @JsonProperty("cellularRecords")
    private List<CellularEntry> cellularEntries;    // List of cellular entries scanned for this fingerprint
    @JsonProperty("sensorRecords")
    private List<SensorEntry> sensorEntries;        // List of beacon entries scanned for this fingerprint

    public Fingerprint() {
        // Set id and scan UUID to send into other device
        id = UUID.randomUUID();
        scanID = UUID.randomUUID();

        // Initiate lists
        beaconEntries = new ArrayList<>();
        wirelessEntries = new ArrayList<>();
        cellularEntries = new ArrayList<>();
        sensorEntries = new ArrayList<>();

        // Set device
        deviceEntry = DeviceEntry.createInstance();
    }

    /**
     * Create instance of Fingerprint and set variables from Map.
     * Reflection is useless because multiple variables have different names.
     */
    public Fingerprint(Map<String, Object> map) {
        if(map.containsKey("_id")) {
            this.id = UUID.fromString(map.get("_id").toString());
        }

        if(map.containsKey("scan_id")) {
            this.scanID = UUID.fromString(map.get("scan_id").toString());
        }

        if(map.containsKey("x")) {
            this.x = Integer.valueOf(map.get("x").toString());
        }

        if(map.containsKey("y")) {
            this.y = Integer.valueOf(map.get("y").toString());
        }

        if(map.containsKey("timestamp")) {
            this.scanStart = Long.valueOf(map.get("timestamp").toString());
        }

        if(map.containsKey("finish")) {
            this.scanEnd = Long.valueOf(map.get("finish").toString());
        }

        // TODO: create location entry by locationRecords (not in the json yet)
        if(map.containsKey("level")) {
            this.locationEntry = new LocationEntry(map.get("level").toString());
        }

        if(map.containsKey("deviceRecord")) {
            this.deviceEntry = new DeviceEntry(map.get("deviceRecord"));
        }

        if(map.containsKey("bluetoothRecords")) {
            Object bluetoothRecords = map.get("bluetoothRecords");
            List<BeaconEntry> beaconEntries = new ArrayList<>();
            for (Object object : (List) bluetoothRecords) beaconEntries.add(new BeaconEntry(object));
            this.beaconEntries = beaconEntries;
        }

        if(map.containsKey("wirelessRecords")) {
            Object wirelessRecords = map.get("wirelessRecords");
            List<WirelessEntry> wirelessEntries = new ArrayList<>();
            for (Object object : (List) wirelessRecords) wirelessEntries.add(new WirelessEntry(object));
            this.wirelessEntries = wirelessEntries;
        }

        if(map.containsKey("cellularRecords")) {
            Object cellularRecords = map.get("cellularRecords");
            Log.d("svdsvsvsdv", cellularRecords.toString());
            List<CellularEntry> cellularEntries = new ArrayList<>();
            for (Object object : (List) cellularRecords) cellularEntries.add(new CellularEntry(object));
            this.cellularEntries = cellularEntries;
        }

        if(map.containsKey("sensorRecords")) {
            Object sensorRecords = map.get("sensorRecords");
            Log.d("svdsvsvsdv", sensorRecords.toString());
            List<SensorEntry> sensorEntries = new ArrayList<>();
            for (Object object : ((LinkedHashMap) sensorRecords).values()) sensorEntries.add(new SensorEntry(object));
            this.sensorEntries = sensorEntries;
        }


    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScanID() {
        return scanID;
    }

    public void setScanID(UUID scanID) {
        this.scanID = scanID;
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

    public long getScanStart() {
        return scanStart;
    }

    public void setScanStart(long scanStart) {
        this.scanStart = scanStart;
    }

    public long getScanEnd() {
        return scanEnd;
    }

    public void setScanEnd(long scanEnd) {
        this.scanEnd = scanEnd;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        if(level != null) {
            this.locationEntry = new LocationEntry(level);
        }
        this.level = level;
    }

    public long getLocation_id() {
        return location_id;
    }

    public void setLocation_id(long location_id) {
        this.location_id = location_id;
    }

    public LocationEntry getLocationEntry() {
        return locationEntry;
    }

    public void setLocationEntry(LocationEntry locationEntry) {
        this.locationEntry = locationEntry;
    }

    public long getDevice_id() {
        return device_id;
    }

    public void setDevice_id(long device_id) {
        this.device_id = device_id;
    }

    public DeviceEntry getDeviceEntry() {
        return deviceEntry;
    }

    public void setDeviceEntry(DeviceEntry deviceEntry) {
        this.deviceEntry = deviceEntry;
    }

    public List<BeaconEntry> getBeaconEntries() {
        if(beaconEntries == null) {
            beaconEntries = new ArrayList<>();
        }
        return beaconEntries;
    }

    public void setBeaconEntries(List<BeaconEntry> beaconEntries) {
        this.beaconEntries = beaconEntries;
    }

    public List<WirelessEntry> getWirelessEntries() {
        if(wirelessEntries == null) {
            wirelessEntries = new ArrayList<>();
        }
        return wirelessEntries;
    }

    public void setWirelessEntries(List<WirelessEntry> wirelessEntries) {
        this.wirelessEntries = wirelessEntries;
    }

    public List<CellularEntry> getCellularEntries() {
        if(cellularEntries == null) {
            cellularEntries = new ArrayList<>();
        }
        return cellularEntries;
    }

    public void setCellularEntries(List<CellularEntry> cellularEntries) {
        this.cellularEntries = cellularEntries;
    }

    public List<SensorEntry> getSensorEntries() {
        if(sensorEntries == null) {
            sensorEntries = new ArrayList<>();
        }
        return sensorEntries;
    }

    public void setSensorEntries(List<SensorEntry> sensorEntries) {
        this.sensorEntries = sensorEntries;
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
        return Objects.equals(this.id, fingerprint.id) &&
                Objects.equals(this.scanID, fingerprint.scanID) &&
                Objects.equals(this.x, fingerprint.x) &&
                Objects.equals(this.y, fingerprint.y) &&
                Objects.equals(this.scanStart, fingerprint.scanStart) &&
                Objects.equals(this.scanEnd, fingerprint.scanEnd) &&
                Objects.equals(this.locationEntry, fingerprint.locationEntry) &&
                Objects.equals(this.deviceEntry, fingerprint.deviceEntry) &&
                Objects.equals(this.beaconEntries, fingerprint.beaconEntries) &&
                Objects.equals(this.wirelessEntries, fingerprint.wirelessEntries) &&
                Objects.equals(this.cellularEntries, fingerprint.cellularEntries) &&
                Objects.equals(this.sensorEntries, fingerprint.sensorEntries);

    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scanID, x, y, scanStart, scanEnd, locationEntry, deviceEntry, beaconEntries, wirelessEntries, cellularEntries, sensorEntries);
    }


    @Override
    public String toString() {
        return "class BeaconEntry {\n" +
                "    dbId: " + toIndentedString(dbId) + "\n" +
                "    id: " + toIndentedString(id) + "\n" +
                "    scanID: " + toIndentedString(scanID) + "\n" +
                "    x: " + toIndentedString(x) + "\n" +
                "    y: " + toIndentedString(y) + "\n" +
                "    scanStart: " + toIndentedString(scanStart) + "\n" +
                "    scanEnd: " + toIndentedString(scanEnd) + "\n" +
                "    locationEntry: " + toIndentedString(locationEntry) + "\n" +
                "    deviceEntry: " + toIndentedString(deviceEntry) + "\n" +
                //"    beaconEntries: " + toIndentedString(beaconEntries.size()) + "\n" +
                //"    wirelessEntries: " + toIndentedString(wirelessEntries.size()) + "\n" +
                //"    cellularEntries: " + toIndentedString(cellularEntries.size()) + "\n" +
                //"    sensorEntries: " + toIndentedString(sensorEntries.size()) + "\n" +
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
