package com.huawei.nearbysdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;

public enum BleScanLevel implements Parcelable {
    VERY_HIGH(3),
    HIGH(2),
    LOW(1),
    VERY_LOW(0);
    
    public static final Creator<BleScanLevel> CREATOR = null;
    private static SparseArray<BleScanLevel> map;
    private final int level;

    static {
        map = new SparseArray();
        BleScanLevel[] values = values();
        int length = values.length;
        int i;
        while (i < length) {
            BleScanLevel scanLevel = values[i];
            map.put(scanLevel.getLevel(), scanLevel);
            i++;
        }
        CREATOR = new Creator<BleScanLevel>() {
            public BleScanLevel createFromParcel(Parcel source) {
                return BleScanLevel.values()[source.readInt()];
            }

            public BleScanLevel[] newArray(int size) {
                return new BleScanLevel[size];
            }
        };
    }

    private BleScanLevel(int level) {
        this.level = level;
    }

    public static BleScanLevel fromLevel(int level) {
        return (BleScanLevel) map.get(level);
    }

    public int getLevel() {
        return this.level;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BleScanLevel{name=");
        stringBuilder.append(name());
        stringBuilder.append(", level=");
        stringBuilder.append(this.level);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
