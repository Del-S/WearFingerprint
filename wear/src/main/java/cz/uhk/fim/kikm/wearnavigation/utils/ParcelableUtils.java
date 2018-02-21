package cz.uhk.fim.kikm.wearnavigation.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;

/**
 * This class is used to put Parcelable data into DataMap to be send to other devices
 * via GoogleAPI.
 */
public class ParcelableUtils {

    /**
     * Puts a Parcelable to the DataMap with specific String key.
     *
     * @param dataMap to save data to
     * @param key to identify data
     * @param parcelable data to save
     */
    public static void putParcelable(DataMap dataMap, String key, Parcelable parcelable) {
        final Parcel parcel = Parcel.obtain();          // Obtain Parcel instance
        parcelable.writeToParcel(parcel, 0);       // Write Parcelable data to Parcel
        parcel.setDataPosition(0);                      // Resets write/read cursor to the start
        dataMap.putByteArray(key, parcel.marshall());   // Add parcel to the map as raw bytes
        parcel.recycle();                               // Resets parcel for next use
    }

    /**
     * Returns specific object from DataMap based on Parcel data.
     *
     * @param dataMap to get data from
     * @param key identification of the data in the map
     * @param creator object creator from parcel
     * @return specific Object instance
     */
    public static <T> T getParcelable(DataMap dataMap, String key, Parcelable.Creator<T> creator) {
        final byte[] byteArray = dataMap.getByteArray(key);         // Get byte data from DataMap
        final Parcel parcel = Parcel.obtain();                      // Obtain Parcel instance
        parcel.unmarshall(byteArray, 0, byteArray.length);    // Deserialize byte to parcel data
        parcel.setDataPosition(0);                                  // Resets write/read cursor to the start
        final T object = creator.createFromParcel(parcel);          // Create specific object from Parcel
        parcel.recycle();                                           // Resets parcel for next use
        return object;                                              // Return object
    }
}
