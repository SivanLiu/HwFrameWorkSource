package com.huawei.msdp.devicestatus;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.huawei.motionservice.common.HuaweiMotionEvent;

public class HwMSDPDeviceStatusEvent implements Parcelable {
    public static final Creator<HwMSDPDeviceStatusEvent> CREATOR = new Creator<HwMSDPDeviceStatusEvent>() {
        public HwMSDPDeviceStatusEvent createFromParcel(Parcel source) {
            String mDeviceStatus = source.readString();
            int mEventType = source.readInt();
            long mTimestampNs = source.readLong();
            Bundle bundle = source.readBundle(getClass().getClassLoader());
            HwMSDPDeviceStatusEvent hwMSDPDeviceStatusEvent = new HwMSDPDeviceStatusEvent(mDeviceStatus, mEventType, mTimestampNs);
            hwMSDPDeviceStatusEvent.setMwMSDPDeviceStatusEventExtras(bundle);
            return hwMSDPDeviceStatusEvent;
        }

        public HwMSDPDeviceStatusEvent[] newArray(int size) {
            return new HwMSDPDeviceStatusEvent[size];
        }
    };
    private String mDeviceStatus;
    private int mEventType;
    private long mTimestampNs;
    private Bundle mwMSDPDeviceStatusEventExtras = null;

    public HwMSDPDeviceStatusEvent(String mDeviceStatus, int mEventType, long mTimestampNs) {
        this.mDeviceStatus = mDeviceStatus;
        this.mEventType = mEventType;
        this.mTimestampNs = mTimestampNs;
    }

    public void setmDeviceStatus(String mDeviceStatus) {
        this.mDeviceStatus = mDeviceStatus;
    }

    public void setmEventType(int mEventType) {
        this.mEventType = mEventType;
    }

    public void setmTimestampNs(long mTimestampNs) {
        this.mTimestampNs = mTimestampNs;
    }

    public String getmDeviceStatus() {
        return this.mDeviceStatus;
    }

    public int getmEventType() {
        return this.mEventType;
    }

    public long getmTimestampNs() {
        return this.mTimestampNs;
    }

    public Bundle getMwMSDPDeviceStatusEventExtras() {
        return this.mwMSDPDeviceStatusEventExtras;
    }

    public void setMwMSDPDeviceStatusEventExtras(Bundle mwMSDPDeviceStatusEventExtras) {
        this.mwMSDPDeviceStatusEventExtras = mwMSDPDeviceStatusEventExtras;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeviceStatus);
        dest.writeInt(this.mEventType);
        dest.writeLong(this.mTimestampNs);
        dest.writeBundle(this.mwMSDPDeviceStatusEventExtras);
    }

    public HuaweiMotionEvent getMotionExtras() {
        if (this.mwMSDPDeviceStatusEventExtras != null) {
            Parcelable parcelable = this.mwMSDPDeviceStatusEventExtras.getParcelable(this.mwMSDPDeviceStatusEventExtras.getString(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE));
            if (parcelable != null && (parcelable instanceof HuaweiMotionEvent)) {
                return (HuaweiMotionEvent) parcelable;
            }
        }
        return null;
    }

    public String toString() {
        return String.format("DeviceStatus='%s',EventType='%s',TimestampNs=%s,MSDPDeviceStatusEventExtras=%s", new Object[]{this.mDeviceStatus, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs), getMotionExtras()});
    }
}
