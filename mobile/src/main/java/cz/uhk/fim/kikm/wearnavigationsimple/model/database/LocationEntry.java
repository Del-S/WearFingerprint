package cz.uhk.fim.kikm.wearnavigationsimple.model.database;

import com.google.gson.annotations.Expose;

import java.util.Objects;

/**
 * This class keeps information about location of the fingerprint.
 * To enable multiple buildings and floors.
 */
public class LocationEntry {

    // Database labels for database
    private final static String DB_LOCATION_ID = "id";
    private final static String DB_BUILDING_ID = "building";
    private final static String DB_FLOOR_ID = "floor";

    // Variables of this class
    @Expose(serialize = false)
    private int id;             // Database id (its inner id and it is not exported)
    private String building;    // Name of the building
    private int floor;          // Floor number inside the building

    // Default constructor used for Gson
    public LocationEntry() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocationEntry locationEntry = (LocationEntry) o;
        return Objects.equals(this.building, locationEntry.building) &&
               Objects.equals(this.floor, locationEntry.floor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(building, floor);
    }


    @Override
    public String toString() {
        return "class LocationEntry {\n" +
                "    dbId: " + toIndentedString(id) + "\n" +
                "    building: " + toIndentedString(building) + "\n" +
                "    floor: " + toIndentedString(floor) + "\n" +
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
