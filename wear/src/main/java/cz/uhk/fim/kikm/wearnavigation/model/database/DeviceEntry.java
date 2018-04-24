package cz.uhk.fim.kikm.wearnavigation.model.database;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class DeviceEntry implements Parcelable {

    // Variables of this class
    @Expose(serialize = false)
    private int dbId;                  // UUID of this device
    private String type;                // Device type (phone, wear, TV ...)
    private String deviceId;            // Either a changelist number, or a label like "M4-rc20".
    private String deviceName;          // The name of the industrial design.
    private String model;               // The end-user-visible name for the end product.
    private String brand;               // The consumer-visible brand with which the product/hardware will be associated, if any.
    private String manufacturer;        // The manufacturer of the product/hardware.
    private String display;             // A build ID string meant for displaying to the user
    private String hardware;            // The name of the hardware (from the kernel command line or /proc).
    private String serialNumber;        // Gets the hardware serial, if available.
    private String telephone;
    private String deviceFingerprint;   // A string that uniquely identifies this build.
    private String os;                  // CODENAME-RELEASE = The current development codename and the user-visible version string.
    private int api;                    // The user-visible SDK version of the framework; its possible values are defined in Build.VERSION_CODES.

    // Default constructor used for Gson
    public DeviceEntry() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dbId);
        dest.writeString(type);
        dest.writeString(deviceId);
        dest.writeString(deviceName);
        dest.writeString(model);
        dest.writeString(brand);
        dest.writeString(manufacturer);
        dest.writeString(display);
        dest.writeString(hardware);
        dest.writeString(serialNumber);
        dest.writeString(telephone);
        dest.writeString(deviceFingerprint);
        dest.writeString(os);
        dest.writeInt(api);
    }

    private DeviceEntry(Parcel in) {
        dbId = in.readInt();
        type = in.readString();
        deviceId = in.readString();
        deviceName = in.readString();
        model = in.readString();
        brand = in.readString();
        manufacturer = in.readString();
        display = in.readString();
        hardware = in.readString();
        serialNumber = in.readString();
        telephone = in.readString();
        deviceFingerprint = in.readString();
        os = in.readString();
        api = in.readInt();
    }

    public static final Creator<DeviceEntry> CREATOR = new Creator<DeviceEntry>() {
        @Override
        public DeviceEntry createFromParcel(Parcel in) {
            return new DeviceEntry(in);
        }

        @Override
        public DeviceEntry[] newArray(int size) {
            return new DeviceEntry[size];
        }
    };

    // Creates instance of this class with current device information
    public static DeviceEntry createInstance(Context context) {
        return new DeviceEntry(context);
    }

    // Constructor that creates class with data from this device
    private DeviceEntry(Context context) {
        this.type = "wear";
        this.deviceId = Build.ID;
        this.deviceName = Build.DEVICE;
        this.model = Build.MODEL;
        this.brand = Build.BRAND;
        this.manufacturer = Build.MANUFACTURER;
        this.display = Build.DISPLAY;
        this.hardware = Build.HARDWARE;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            this.serialNumber = Build.SERIAL;
        } else {
            try {
                this.serialNumber = Build.getSerial();
            } catch (SecurityException e) {
                Log.e("DeviceEntry", "Could not get device serialNumber", e);
            }
        }
        this.deviceFingerprint = Build.FINGERPRINT;
        this.os = Build.VERSION.CODENAME + "-" + Build.VERSION.RELEASE;
        this.api = Build.VERSION.SDK_INT;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbid) {
        this.dbId = dbid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public int getApi() {
        return api;
    }

    public void setApi(int api) {
        this.api = api;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceEntry deviceEntry = (DeviceEntry) o;
        return Objects.equals(this.type, deviceEntry.type) &&
                Objects.equals(this.deviceId, deviceEntry.deviceId) &&
                Objects.equals(this.deviceName, deviceEntry.deviceName) &&
                Objects.equals(this.model, deviceEntry.model) &&
                Objects.equals(this.brand, deviceEntry.brand) &&
                Objects.equals(this.manufacturer, deviceEntry.manufacturer) &&
                Objects.equals(this.display, deviceEntry.display) &&
                Objects.equals(this.hardware, deviceEntry.hardware) &&
                Objects.equals(this.serialNumber, deviceEntry.serialNumber) &&
                Objects.equals(this.telephone, deviceEntry.telephone) &&
                Objects.equals(this.deviceFingerprint, deviceEntry.deviceFingerprint) &&
                Objects.equals(this.os, deviceEntry.os) &&
                Objects.equals(this.api, deviceEntry.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type,
                deviceId,
                deviceName,
                model,
                brand,
                manufacturer,
                display,
                hardware,
                serialNumber,
                telephone,
                deviceFingerprint,
                os,
                api );
    }


    @Override
    public String toString() {
        return "class DeviceEntry {\n" +
                "    dbId: " + toIndentedString(dbId) + "\n" +
                "    type: " + toIndentedString(type) + "\n" +
                "    deviceId: " + toIndentedString(deviceId) + "\n" +
                "    deviceName: " + toIndentedString(deviceName) + "\n" +
                "    model: " + toIndentedString(model) + "\n" +
                "    brand: " + toIndentedString(brand) + "\n" +
                "    manufacturer: " + toIndentedString(manufacturer) + "\n" +
                "    display: " + toIndentedString(display) + "\n" +
                "    hardware: " + toIndentedString(hardware) + "\n" +
                "    serialNumber: " + toIndentedString(serialNumber) + "\n" +
                "    telephone: " + toIndentedString(telephone) + "\n" +
                "    deviceFingerprint: " + toIndentedString(deviceFingerprint) + "\n" +
                "    os: " + toIndentedString(os) + "\n" +
                "    api: " + toIndentedString(api) + "\n" +
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
