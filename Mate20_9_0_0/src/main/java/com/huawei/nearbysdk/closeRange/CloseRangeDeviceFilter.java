package com.huawei.nearbysdk.closeRange;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class CloseRangeDeviceFilter implements Parcelable {
    public static final Creator<CloseRangeDeviceFilter> CREATOR = new Creator<CloseRangeDeviceFilter>() {
        public CloseRangeDeviceFilter createFromParcel(Parcel source) {
            return new CloseRangeDeviceFilter((CloseRangeBusinessType) source.readParcelable(CloseRangeDeviceFilter.class.getClassLoader()), (CloseRangeDevice) source.readParcelable(CloseRangeDevice.class.getClassLoader()), null);
        }

        public CloseRangeDeviceFilter[] newArray(int size) {
            return new CloseRangeDeviceFilter[size];
        }
    };
    private CloseRangeBusinessType businessType;
    private CloseRangeDevice device;

    /* synthetic */ CloseRangeDeviceFilter(CloseRangeBusinessType x0, CloseRangeDevice x1, AnonymousClass1 x2) {
        this(x0, x1);
    }

    private CloseRangeDeviceFilter(CloseRangeBusinessType businessType, CloseRangeDevice device) {
        this.businessType = businessType;
        this.device = device;
    }

    public static CloseRangeDeviceFilter buildFilter(CloseRangeBusinessType businessType, String deviceMAC) {
        return new CloseRangeDeviceFilter(businessType, new CloseRangeDevice(deviceMAC));
    }

    public CloseRangeDevice getDevice() {
        return this.device;
    }

    public String getDeviceMAC() {
        return this.device.getMAC();
    }

    public CloseRangeBusinessType getBusinessType() {
        return this.businessType;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.businessType, 0);
        dest.writeParcelable(this.device, 0);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (!(o instanceof CloseRangeDeviceFilter)) {
            return false;
        }
        CloseRangeDeviceFilter that = (CloseRangeDeviceFilter) o;
        if (!(getBusinessType().equals(that.getBusinessType()) && getDeviceMAC().equals(that.getDeviceMAC()))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return getBusinessType().getTag();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CloseRangeDeviceFilter{businessType=");
        stringBuilder.append(this.businessType);
        stringBuilder.append(", device=");
        stringBuilder.append(this.device);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
