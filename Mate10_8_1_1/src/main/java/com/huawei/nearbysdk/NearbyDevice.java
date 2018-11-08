package com.huawei.nearbysdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.huawei.nearbysdk.NearbyConfig.BusinessTypeEnum;
import java.io.Serializable;

public final class NearbyDevice implements Parcelable, Serializable {
    public static final Creator<NearbyDevice> CREATOR = new Creator<NearbyDevice>() {
        public NearbyDevice createFromParcel(Parcel source) {
            return new NearbyDevice(source.readString(), source.readString(), source.readString(), source.readString(), source.readInt(), NearbySDKUtils.getEnumFromInt(source.readInt()), source.readInt() == 1, source.readString(), source.readInt() == 1, source.readString(), source.readString(), source.readInt(), source.readString(), source.readString(), source.readInt(), source.readInt() == 1);
        }

        public NearbyDevice[] newArray(int size) {
            return new NearbyDevice[size];
        }
    };
    static final String TAG = "for outer NearbyDevice";
    private String mApMac;
    private boolean mAvailability;
    private String mBluetoothMac;
    private String mBtName;
    private int mBusinessId;
    private BusinessTypeEnum mBusinessType;
    private String mHuaweiIdName;
    private boolean mIsNeedClose;
    private String mLocalIp;
    private String mRemoteIp;
    private boolean mSameHwAccount;
    private String mSummary;
    private int mWifiBand;
    private int mWifiPort;
    private String mWifiPwd;
    private String mWifiSsid;

    public boolean isSameHwAccount() {
        return this.mSameHwAccount;
    }

    public void setSameHwAccount(boolean sameHwAccount) {
        this.mSameHwAccount = sameHwAccount;
    }

    public String getSummary() {
        return this.mSummary;
    }

    public String getBluetoothMac() {
        return this.mBluetoothMac;
    }

    public String getHuaweiIdName() {
        return this.mHuaweiIdName;
    }

    public String getBtName() {
        return this.mBtName;
    }

    public int getBusinessId() {
        return this.mBusinessId;
    }

    public BusinessTypeEnum getBusinessType() {
        return this.mBusinessType;
    }

    public boolean getAvailability() {
        return this.mAvailability;
    }

    public String getApMac() {
        return this.mApMac;
    }

    public String getWifiSsid() {
        return this.mWifiSsid;
    }

    public String getWifiPwd() {
        return this.mWifiPwd;
    }

    public int getWifiBand() {
        return this.mWifiBand;
    }

    public String getRemoteIp() {
        return this.mRemoteIp;
    }

    public String getLocalIp() {
        return this.mLocalIp;
    }

    public int getWifiPort() {
        return this.mWifiPort;
    }

    public boolean getNeedClose() {
        return this.mIsNeedClose;
    }

    public void setNeedClose(boolean needClose) {
        this.mIsNeedClose = needClose;
    }

    public NearbyDevice(String summary, String bluetoothMac, String huaweiIdName, String btName, int businessId, BusinessTypeEnum businessType, boolean availability, String apMac, boolean sameHwAccount) {
        this(summary, bluetoothMac, huaweiIdName, btName, businessId, businessType, availability, apMac, sameHwAccount, null, null, -1, null, null, 0, false);
    }

    public NearbyDevice(String summary, String bluetoothMac, String huaweiIdName, String btName, int businessId, BusinessTypeEnum businessType, boolean availability, String apMac, boolean sameHwAccount, String wifiSsid, String wifiPwd, int wifiBand, String remoteIp, String localIp, int wifiPort, boolean isNeedClose) {
        this.mBusinessType = BusinessTypeEnum.AllType;
        this.mSummary = summary;
        this.mBluetoothMac = bluetoothMac;
        this.mHuaweiIdName = huaweiIdName;
        this.mBtName = btName;
        this.mBusinessId = businessId;
        this.mBusinessType = businessType;
        this.mAvailability = availability;
        this.mApMac = apMac;
        this.mSameHwAccount = sameHwAccount;
        this.mWifiSsid = wifiSsid;
        this.mWifiPwd = wifiPwd;
        this.mWifiBand = wifiBand;
        this.mRemoteIp = remoteIp;
        this.mLocalIp = localIp;
        this.mWifiPort = wifiPort;
        this.mIsNeedClose = isNeedClose;
    }

    public NearbyDevice(String wifiSsid, String wifiPwd, int wifiBand, String wifiIp, int wifiPort) {
        this.mBusinessType = BusinessTypeEnum.AllType;
        this.mWifiSsid = wifiSsid;
        this.mWifiPwd = wifiPwd;
        this.mWifiBand = wifiBand;
        this.mRemoteIp = wifiIp;
        this.mWifiPort = wifiPort;
    }

    private static boolean equals(Object a, Object b) {
        if (a != b) {
            return a != null ? a.equals(b) : false;
        } else {
            return true;
        }
    }

    public boolean equals(Object anObject) {
        boolean z = false;
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof NearbyDevice)) {
            return false;
        }
        NearbyDevice anDevice = (NearbyDevice) anObject;
        if (this.mSummary != null || anDevice.mSummary != null) {
            return equals(this.mSummary, anDevice.mSummary);
        }
        if (equals(this.mWifiSsid, anDevice.mWifiSsid) && equals(this.mWifiPwd, anDevice.mWifiPwd)) {
            z = equals(Integer.valueOf(this.mWifiBand), Integer.valueOf(anDevice.mWifiBand));
        }
        return z;
    }

    public int hashCode() {
        if (this.mSummary == null) {
            return 0;
        }
        return this.mSummary.hashCode();
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "NearbyDevice:{Summary:" + this.mSummary + ";BusinessId:" + this.mBusinessId + ";BusinessType:" + this.mBusinessType.toNumber() + ";Availability:" + this.mAvailability + "}";
    }

    public void writeToParcel(Parcel dest, int flag) {
        int i;
        int i2 = 1;
        dest.writeString(this.mSummary);
        dest.writeString(this.mBluetoothMac);
        dest.writeString(this.mHuaweiIdName);
        dest.writeString(this.mBtName);
        dest.writeInt(this.mBusinessId);
        dest.writeInt(this.mBusinessType.toNumber());
        dest.writeInt(this.mAvailability ? 1 : 0);
        dest.writeString(this.mApMac);
        if (this.mSameHwAccount) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeString(this.mWifiSsid);
        dest.writeString(this.mWifiPwd);
        dest.writeInt(this.mWifiBand);
        dest.writeString(this.mRemoteIp);
        dest.writeString(this.mLocalIp);
        dest.writeInt(this.mWifiPort);
        if (!this.mIsNeedClose) {
            i2 = 0;
        }
        dest.writeInt(i2);
    }
}
