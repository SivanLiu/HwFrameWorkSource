package com.huawei.nearbysdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;

public enum BleAdvLevel implements Parcelable {
    VERY_HIGH(3),
    HIGH(2),
    LOW(1),
    VERY_LOW(0);
    
    public static final Creator<BleAdvLevel> CREATOR = null;
    private static SparseArray<BleAdvLevel> map;
    private final int level;

    static {
        map = new SparseArray();
        BleAdvLevel[] values = values();
        int length = values.length;
        int i;
        while (i < length) {
            BleAdvLevel advLevel = values[i];
            map.put(advLevel.getLevel(), advLevel);
            i++;
        }
        CREATOR = new Creator<BleAdvLevel>() {
            public BleAdvLevel createFromParcel(Parcel source) {
                return BleAdvLevel.values()[source.readInt()];
            }

            public BleAdvLevel[] newArray(int size) {
                return new BleAdvLevel[size];
            }
        };
    }

    private BleAdvLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    public static BleAdvLevel fromLevel(int level) {
        return (BleAdvLevel) map.get(level);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BleAdvLevel{name=");
        stringBuilder.append(name());
        stringBuilder.append(", level=");
        stringBuilder.append(this.level);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
