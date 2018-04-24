package cz.uhk.fim.kikm.wearnavigation.model.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class FingerprintMeta implements Parcelable {
    @SerializedName("countNew")
    private long countNew = -1;
    @SerializedName("lastInsert")
    private long lastInsert = -1;

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FingerprintMeta> CREATOR = new Creator<FingerprintMeta>() {
        @Override
        public FingerprintMeta createFromParcel(Parcel in) {
            return new FingerprintMeta(in);
        }

        @Override
        public FingerprintMeta[] newArray(int size) {
            return new FingerprintMeta[size];
        }
    };

    private FingerprintMeta(Parcel in) {
        countNew = in.readLong();
        lastInsert = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(countNew);
        dest.writeLong(lastInsert);
    }

    public FingerprintMeta countNew(int countNew) {
        this.countNew = countNew;
        return this;
    }

    public FingerprintMeta() {}

    public long getCountNew() {
    return countNew;
    }

    public void setCountNew(long countNew) {
    this.countNew = countNew;
    }

    public FingerprintMeta lastInsert(long lastInsert) {
        this.lastInsert = lastInsert;
        return this;
    }

    public long getLastInsert() {
    return lastInsert;
    }

    public void setLastInsert(long lastInsert) {
    this.lastInsert = lastInsert;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        FingerprintMeta fingerprintMeta = (FingerprintMeta) o;
        return Objects.equals(this.countNew, fingerprintMeta.countNew) &&
            Objects.equals(this.lastInsert, fingerprintMeta.lastInsert);
    }

    @Override
    public int hashCode() {
    return Objects.hash(countNew, lastInsert);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class FingerprintMeta {\n");

        sb.append("    countNew: ").append(toIndentedString(countNew)).append("\n");
        sb.append("    lastInsert: ").append(toIndentedString(lastInsert)).append("\n");
        sb.append("}");
        return sb.toString();
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

