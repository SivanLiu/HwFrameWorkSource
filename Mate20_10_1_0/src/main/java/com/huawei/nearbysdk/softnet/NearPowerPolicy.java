package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public enum NearPowerPolicy implements Parcelable {
    High(1),
    Middle(2),
    Low(3),
    Very_Low(4);
    
    public static final Creator<NearPowerPolicy> CREATOR = new Creator<NearPowerPolicy>() {
        /* class com.huawei.nearbysdk.softnet.NearPowerPolicy.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearPowerPolicy createFromParcel(Parcel source) {
            return NearPowerPolicy.values()[source.readInt()];
        }

        @Override // android.os.Parcelable.Creator
        public NearPowerPolicy[] newArray(int size) {
            return new NearPowerPolicy[size];
        }
    };
    private int mPowerPolicyValue;

    private NearPowerPolicy(int powerPolicyValue) {
        this.mPowerPolicyValue = powerPolicyValue;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public int getPowerPolicyValue() {
        return this.mPowerPolicyValue;
    }
}
