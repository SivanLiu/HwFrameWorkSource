package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class MetaStatusPolicy extends AManagedObject {
    public static final Creator<MetaStatusPolicy> CREATOR = new Creator<MetaStatusPolicy>() {
        public MetaStatusPolicy createFromParcel(Parcel in) {
            return new MetaStatusPolicy(in);
        }

        public MetaStatusPolicy[] newArray(int size) {
            return new MetaStatusPolicy[size];
        }
    };
    private String mAirplaneMode;
    private String mBatteryState;
    private String mBluetoothState;
    private String mEyeComfortSwitch;
    private String mHeadsetState;
    private String mHiBoardSwitch;
    private Integer mId;
    private String mNfcSwitch;
    private String mNoDisturb;
    private String mPhoneState;
    private String mPowerSaving;
    private String mPowerSupply;
    private Integer mReservedInt;
    private String mReservedText;
    private String mRoamingState;
    private String mScreenState;
    private String mSilentMode;
    private String mStudentState;
    private String mWifiState;

    public MetaStatusPolicy(Cursor cursor) {
        Integer num;
        Integer num2 = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        if (cursor.isNull(1)) {
            num = null;
        } else {
            num = Integer.valueOf(cursor.getInt(1));
        }
        this.mId = num;
        this.mPhoneState = cursor.getString(2);
        this.mRoamingState = cursor.getString(3);
        this.mBatteryState = cursor.getString(4);
        this.mPowerSaving = cursor.getString(5);
        this.mPowerSupply = cursor.getString(6);
        this.mScreenState = cursor.getString(7);
        this.mHeadsetState = cursor.getString(8);
        this.mBluetoothState = cursor.getString(9);
        this.mWifiState = cursor.getString(10);
        this.mStudentState = cursor.getString(11);
        this.mAirplaneMode = cursor.getString(12);
        this.mNoDisturb = cursor.getString(13);
        this.mSilentMode = cursor.getString(14);
        this.mNfcSwitch = cursor.getString(15);
        this.mEyeComfortSwitch = cursor.getString(16);
        this.mHiBoardSwitch = cursor.getString(17);
        if (!cursor.isNull(18)) {
            num2 = Integer.valueOf(cursor.getInt(18));
        }
        this.mReservedInt = num2;
        this.mReservedText = cursor.getString(19);
    }

    public MetaStatusPolicy(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mPhoneState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mRoamingState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mBatteryState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mPowerSaving = in.readByte() == (byte) 0 ? null : in.readString();
        this.mPowerSupply = in.readByte() == (byte) 0 ? null : in.readString();
        this.mScreenState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mHeadsetState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mBluetoothState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mWifiState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mStudentState = in.readByte() == (byte) 0 ? null : in.readString();
        this.mAirplaneMode = in.readByte() == (byte) 0 ? null : in.readString();
        this.mNoDisturb = in.readByte() == (byte) 0 ? null : in.readString();
        this.mSilentMode = in.readByte() == (byte) 0 ? null : in.readString();
        this.mNfcSwitch = in.readByte() == (byte) 0 ? null : in.readString();
        this.mEyeComfortSwitch = in.readByte() == (byte) 0 ? null : in.readString();
        this.mHiBoardSwitch = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaStatusPolicy(Integer mId, String mPhoneState, String mRoamingState, String mBatteryState, String mPowerSaving, String mPowerSupply, String mScreenState, String mHeadsetState, String mBluetoothState, String mWifiState, String mStudentState, String mAirplaneMode, String mNoDisturb, String mSilentMode, String mNfcSwitch, String mEyeComfortSwitch, String mHiBoardSwitch, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mPhoneState = mPhoneState;
        this.mRoamingState = mRoamingState;
        this.mBatteryState = mBatteryState;
        this.mPowerSaving = mPowerSaving;
        this.mPowerSupply = mPowerSupply;
        this.mScreenState = mScreenState;
        this.mHeadsetState = mHeadsetState;
        this.mBluetoothState = mBluetoothState;
        this.mWifiState = mWifiState;
        this.mStudentState = mStudentState;
        this.mAirplaneMode = mAirplaneMode;
        this.mNoDisturb = mNoDisturb;
        this.mSilentMode = mSilentMode;
        this.mNfcSwitch = mNfcSwitch;
        this.mEyeComfortSwitch = mEyeComfortSwitch;
        this.mHiBoardSwitch = mHiBoardSwitch;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId) {
        this.mId = mId;
        setValue();
    }

    public String getMPhoneState() {
        return this.mPhoneState;
    }

    public void setMPhoneState(String mPhoneState) {
        this.mPhoneState = mPhoneState;
        setValue();
    }

    public String getMRoamingState() {
        return this.mRoamingState;
    }

    public void setMRoamingState(String mRoamingState) {
        this.mRoamingState = mRoamingState;
        setValue();
    }

    public String getMBatteryState() {
        return this.mBatteryState;
    }

    public void setMBatteryState(String mBatteryState) {
        this.mBatteryState = mBatteryState;
        setValue();
    }

    public String getMPowerSaving() {
        return this.mPowerSaving;
    }

    public void setMPowerSaving(String mPowerSaving) {
        this.mPowerSaving = mPowerSaving;
        setValue();
    }

    public String getMPowerSupply() {
        return this.mPowerSupply;
    }

    public void setMPowerSupply(String mPowerSupply) {
        this.mPowerSupply = mPowerSupply;
        setValue();
    }

    public String getMScreenState() {
        return this.mScreenState;
    }

    public void setMScreenState(String mScreenState) {
        this.mScreenState = mScreenState;
        setValue();
    }

    public String getMHeadsetState() {
        return this.mHeadsetState;
    }

    public void setMHeadsetState(String mHeadsetState) {
        this.mHeadsetState = mHeadsetState;
        setValue();
    }

    public String getMBluetoothState() {
        return this.mBluetoothState;
    }

    public void setMBluetoothState(String mBluetoothState) {
        this.mBluetoothState = mBluetoothState;
        setValue();
    }

    public String getMWifiState() {
        return this.mWifiState;
    }

    public void setMWifiState(String mWifiState) {
        this.mWifiState = mWifiState;
        setValue();
    }

    public String getMStudentState() {
        return this.mStudentState;
    }

    public void setMStudentState(String mStudentState) {
        this.mStudentState = mStudentState;
        setValue();
    }

    public String getMAirplaneMode() {
        return this.mAirplaneMode;
    }

    public void setMAirplaneMode(String mAirplaneMode) {
        this.mAirplaneMode = mAirplaneMode;
        setValue();
    }

    public String getMNoDisturb() {
        return this.mNoDisturb;
    }

    public void setMNoDisturb(String mNoDisturb) {
        this.mNoDisturb = mNoDisturb;
        setValue();
    }

    public String getMSilentMode() {
        return this.mSilentMode;
    }

    public void setMSilentMode(String mSilentMode) {
        this.mSilentMode = mSilentMode;
        setValue();
    }

    public String getMNfcSwitch() {
        return this.mNfcSwitch;
    }

    public void setMNfcSwitch(String mNfcSwitch) {
        this.mNfcSwitch = mNfcSwitch;
        setValue();
    }

    public String getMEyeComfortSwitch() {
        return this.mEyeComfortSwitch;
    }

    public void setMEyeComfortSwitch(String mEyeComfortSwitch) {
        this.mEyeComfortSwitch = mEyeComfortSwitch;
        setValue();
    }

    public String getMHiBoardSwitch() {
        return this.mHiBoardSwitch;
    }

    public void setMHiBoardSwitch(String mHiBoardSwitch) {
        this.mHiBoardSwitch = mHiBoardSwitch;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt) {
        this.mReservedInt = mReservedInt;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText) {
        this.mReservedText = mReservedText;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mPhoneState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPhoneState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRoamingState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mRoamingState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBatteryState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBatteryState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mPowerSaving != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPowerSaving);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mPowerSupply != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPowerSupply);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mScreenState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mScreenState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHeadsetState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHeadsetState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBluetoothState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBluetoothState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWifiState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mWifiState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mStudentState != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mStudentState);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAirplaneMode != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mAirplaneMode);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mNoDisturb != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mNoDisturb);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSilentMode != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSilentMode);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mNfcSwitch != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mNfcSwitch);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEyeComfortSwitch != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEyeComfortSwitch);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHiBoardSwitch != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHiBoardSwitch);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<MetaStatusPolicy> getHelper() {
        return MetaStatusPolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaStatusPolicy";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaStatusPolicy { mId: ").append(this.mId);
        sb.append(", mPhoneState: ").append(this.mPhoneState);
        sb.append(", mRoamingState: ").append(this.mRoamingState);
        sb.append(", mBatteryState: ").append(this.mBatteryState);
        sb.append(", mPowerSaving: ").append(this.mPowerSaving);
        sb.append(", mPowerSupply: ").append(this.mPowerSupply);
        sb.append(", mScreenState: ").append(this.mScreenState);
        sb.append(", mHeadsetState: ").append(this.mHeadsetState);
        sb.append(", mBluetoothState: ").append(this.mBluetoothState);
        sb.append(", mWifiState: ").append(this.mWifiState);
        sb.append(", mStudentState: ").append(this.mStudentState);
        sb.append(", mAirplaneMode: ").append(this.mAirplaneMode);
        sb.append(", mNoDisturb: ").append(this.mNoDisturb);
        sb.append(", mSilentMode: ").append(this.mSilentMode);
        sb.append(", mNfcSwitch: ").append(this.mNfcSwitch);
        sb.append(", mEyeComfortSwitch: ").append(this.mEyeComfortSwitch);
        sb.append(", mHiBoardSwitch: ").append(this.mHiBoardSwitch);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
