package com.huawei.nearbysdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public enum BleAdvLevel implements Parcelable {
    VERY_HIGH(3),
    HIGH(2),
    LOW(1),
    VERY_LOW(0);
    
    public static final Creator<BleAdvLevel> CREATOR = new Creator<BleAdvLevel>() {
        /* class com.huawei.nearbysdk.BleAdvLevel.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public BleAdvLevel createFromParcel(Parcel source) {
            return BleAdvLevel.values()[source.readInt()];
        }

        @Override // android.os.Parcelable.Creator
        public BleAdvLevel[] newArray(int size) {
            return new BleAdvLevel[size];
        }
    };
    private static SparseArray<BleAdvLevel> map = new SparseArray<>();
    private final int level;

    static {
        BleAdvLevel[] values = values();
        for (BleAdvLevel advLevel : values) {
            map.put(advLevel.getLevel(), advLevel);
        }
    }

    private BleAdvLevel(int level2) {
        this.level = level2;
    }

    public int getLevel() {
        return this.level;
    }

    public static BleAdvLevel fromLevel(int level2) {
        return map.get(level2);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "BleAdvLevel{name=" + name() + ", level=" + this.level + '}';
    }
}
