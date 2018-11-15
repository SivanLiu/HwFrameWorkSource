package com.huawei.nearbysdk.closeRange;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;

public enum CloseRangeBusinessType implements Parcelable {
    iConnect((byte) 1);
    
    public static final Creator<CloseRangeBusinessType> CREATOR = null;
    private static final SparseArray<CloseRangeBusinessType> lookupMap = null;
    private byte tag;

    static {
        lookupMap = new SparseArray();
        CloseRangeBusinessType[] values = values();
        int length = values.length;
        int i;
        while (i < length) {
            CloseRangeBusinessType value = values[i];
            lookupMap.put(value.getTag(), value);
            i++;
        }
        CREATOR = new Creator<CloseRangeBusinessType>() {
            public CloseRangeBusinessType createFromParcel(Parcel source) {
                return CloseRangeBusinessType.values()[source.readInt()];
            }

            public CloseRangeBusinessType[] newArray(int size) {
                return new CloseRangeBusinessType[size];
            }
        };
    }

    private CloseRangeBusinessType(byte tag) {
        this.tag = tag;
    }

    public byte getTag() {
        return this.tag;
    }

    public static CloseRangeBusinessType fromTag(byte tag) {
        return (CloseRangeBusinessType) lookupMap.get(tag);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int describeContents() {
        return 0;
    }
}
