package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import java.util.Objects;

/**
 * This class keeps information about location of the fingerprint.
 * To enable multiple buildings and floors.
 */
public class LocationEntry implements Parcelable {

    // Variables of this class
    @Expose(serialize = false)
    private int id;                 // Database id (its inner id and it is not exported)
    private String building;        // Name of the building
    private int floor;              // Floor number inside the building

    // Default constructor used for Gson
    public LocationEntry() {}

    // Default constructor used for Gson
    public LocationEntry(String location) {
        switch(location) {
            case "J1NP":
                this.building = "UHK";
                this.floor = 1;
                break;
            case "J2NP":
                this.building = "UHK";
                this.floor = 2;
                break;
            case "J3NP":
                this.building = "UHK";
                this.floor = 3;
                break;
            case "J4NP":
                this.building = "UHK";
                this.floor = 4;
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(building);
        dest.writeInt(floor);
    }

    private LocationEntry(Parcel in) {
        id = in.readInt();
        building = in.readString();
        floor = in.readInt();
    }

    public static final Creator<LocationEntry> CREATOR = new Creator<LocationEntry>() {
        @Override
        public LocationEntry createFromParcel(Parcel in) {
            return new LocationEntry(in);
        }

        @Override
        public LocationEntry[] newArray(int size) {
            return new LocationEntry[size];
        }
    };

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
    public boolean equals(Object o) {
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
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
