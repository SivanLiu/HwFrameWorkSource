package com.huawei.nearbysdk.closeRange;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

public enum CloseRangeBusinessType implements Parcelable {
    iConnect((byte) 1),
    HWDDMP((byte) 2);
    
    public static final Creator<CloseRangeBusinessType> CREATOR = new Creator<CloseRangeBusinessType>() {
        /* class com.huawei.nearbysdk.closeRange.CloseRangeBusinessType.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public CloseRangeBusinessType createFromParcel(Parcel source) {
            return CloseRangeBusinessType.values()[source.readInt()];
        }

        @Override // android.os.Parcelable.Creator
        public CloseRangeBusinessType[] newArray(int size) {
            return new CloseRangeBusinessType[size];
        }
    };
    private static final SparseArray<CloseRangeBusinessType> lookupMap = new SparseArray<>();
    private byte tag;

    static {
        CloseRangeBusinessType[] values = values();
        for (CloseRangeBusinessType value : values) {
            lookupMap.put(value.getTag(), value);
        }
    }

    private CloseRangeBusinessType(byte tag2) {
        this.tag = tag2;
    }

    public byte getTag() {
        return this.tag;
    }

    public static CloseRangeBusinessType fromTag(byte tag2) {
        return lookupMap.get(tag2);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int describeContents() {
        return 0;
    }
}
