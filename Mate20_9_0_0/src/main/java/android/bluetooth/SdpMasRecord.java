package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SdpMasRecord implements Parcelable {
    public static final Creator CREATOR = new Creator() {
        public SdpMasRecord createFromParcel(Parcel in) {
            return new SdpMasRecord(in);
        }

        public SdpRecord[] newArray(int size) {
            return new SdpRecord[size];
        }
    };
    private final int mL2capPsm;
    private final int mMasInstanceId;
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;
    private final int mSupportedFeatures;
    private final int mSupportedMessageTypes;

    public static final class MessageType {
        public static final int EMAIL = 1;
        public static final int MMS = 8;
        public static final int SMS_CDMA = 4;
        public static final int SMS_GSM = 2;
    }

    public SdpMasRecord(int masInstanceId, int l2capPsm, int rfcommChannelNumber, int profileVersion, int supportedFeatures, int supportedMessageTypes, String serviceName) {
        this.mMasInstanceId = masInstanceId;
        this.mL2capPsm = l2capPsm;
        this.mRfcommChannelNumber = rfcommChannelNumber;
        this.mProfileVersion = profileVersion;
        this.mSupportedFeatures = supportedFeatures;
        this.mSupportedMessageTypes = supportedMessageTypes;
        this.mServiceName = serviceName;
    }

    public SdpMasRecord(Parcel in) {
        this.mMasInstanceId = in.readInt();
        this.mL2capPsm = in.readInt();
        this.mRfcommChannelNumber = in.readInt();
        this.mProfileVersion = in.readInt();
        this.mSupportedFeatures = in.readInt();
        this.mSupportedMessageTypes = in.readInt();
        this.mServiceName = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public int getMasInstanceId() {
        return this.mMasInstanceId;
    }

    public int getL2capPsm() {
        return this.mL2capPsm;
    }

    public int getRfcommCannelNumber() {
        return this.mRfcommChannelNumber;
    }

    public int getProfileVersion() {
        return this.mProfileVersion;
    }

    public int getSupportedFeatures() {
        return this.mSupportedFeatures;
    }

    public int getSupportedMessageTypes() {
        return this.mSupportedMessageTypes;
    }

    public boolean msgSupported(int msg) {
        return (this.mSupportedMessageTypes & msg) != 0;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMasInstanceId);
        dest.writeInt(this.mL2capPsm);
        dest.writeInt(this.mRfcommChannelNumber);
        dest.writeInt(this.mProfileVersion);
        dest.writeInt(this.mSupportedFeatures);
        dest.writeInt(this.mSupportedMessageTypes);
        dest.writeString(this.mServiceName);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String ret = "Bluetooth MAS SDP Record:\n";
        if (this.mMasInstanceId != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("Mas Instance Id: ");
            stringBuilder.append(this.mMasInstanceId);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mRfcommChannelNumber != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("RFCOMM Chan Number: ");
            stringBuilder.append(this.mRfcommChannelNumber);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mL2capPsm != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("L2CAP PSM: ");
            stringBuilder.append(this.mL2capPsm);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mServiceName != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("Service Name: ");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mProfileVersion != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("Profile version: ");
            stringBuilder.append(this.mProfileVersion);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mSupportedMessageTypes != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("Supported msg types: ");
            stringBuilder.append(this.mSupportedMessageTypes);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mSupportedFeatures == -1) {
            return ret;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(ret);
        stringBuilder.append("Supported features: ");
        stringBuilder.append(this.mSupportedFeatures);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
