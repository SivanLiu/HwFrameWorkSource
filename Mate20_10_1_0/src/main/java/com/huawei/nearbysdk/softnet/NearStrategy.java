package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public enum NearStrategy implements Parcelable {
    BLE((byte) 1),
    P2P((byte) 2),
    USB((byte) 4),
    COAP((byte) 8),
    WIFI((byte) 16);
    
    public static final Creator<NearStrategy> CREATOR = new Creator<NearStrategy>() {
        /* class com.huawei.nearbysdk.softnet.NearStrategy.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearStrategy createFromParcel(Parcel source) {
            return NearStrategy.values()[source.readInt()];
        }

        @Override // android.os.Parcelable.Creator
        public NearStrategy[] newArray(int size) {
            return new NearStrategy[size];
        }
    };
    private byte mStrategyValue;

    private NearStrategy(byte strategyValue) {
        this.mStrategyValue = strategyValue;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public byte getStrategyValue() {
        return this.mStrategyValue;
    }
}
