package cz.uhk.fim.kikm.wearnavigation.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.wearable.DataMap;

public class ParcelableUtils {

    public static void putParcelable(DataMap dataMap, String key, Parcelable parcelable) {
        final Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        dataMap.putByteArray(key, parcel.marshall());
        parcel.recycle();
    }

    public static <T> T getParcelable(DataMap dataMap, String key, Parcelable.Creator<T> creator) {
        final byte[] byteArray = dataMap.getByteArray(key);
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);
        final T object = creator.createFromParcel(parcel);
        parcel.recycle();
        return object;
    }

}
