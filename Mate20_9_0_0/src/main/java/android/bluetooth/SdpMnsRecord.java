package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SdpMnsRecord implements Parcelable {
    public static final Creator CREATOR = new Creator() {
        public SdpMnsRecord createFromParcel(Parcel in) {
            return new SdpMnsRecord(in);
        }

        public SdpMnsRecord[] newArray(int size) {
            return new SdpMnsRecord[size];
        }
    };
    private final int mL2capPsm;
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;
    private final int mSupportedFeatures;

    public SdpMnsRecord(int l2capPsm, int rfcommChannelNumber, int profileVersion, int supportedFeatures, String serviceName) {
        this.mL2capPsm = l2capPsm;
        this.mRfcommChannelNumber = rfcommChannelNumber;
        this.mSupportedFeatures = supportedFeatures;
        this.mServiceName = serviceName;
        this.mProfileVersion = profileVersion;
    }

    public SdpMnsRecord(Parcel in) {
        this.mRfcommChannelNumber = in.readInt();
        this.mL2capPsm = in.readInt();
        this.mServiceName = in.readString();
        this.mSupportedFeatures = in.readInt();
        this.mProfileVersion = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public int getL2capPsm() {
        return this.mL2capPsm;
    }

    public int getRfcommChannelNumber() {
        return this.mRfcommChannelNumber;
    }

    public int getSupportedFeatures() {
        return this.mSupportedFeatures;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public int getProfileVersion() {
        return this.mProfileVersion;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRfcommChannelNumber);
        dest.writeInt(this.mL2capPsm);
        dest.writeString(this.mServiceName);
        dest.writeInt(this.mSupportedFeatures);
        dest.writeInt(this.mProfileVersion);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String ret = "Bluetooth MNS SDP Record:\n";
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
        if (this.mSupportedFeatures != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("Supported features: ");
            stringBuilder.append(this.mSupportedFeatures);
            stringBuilder.append("\n");
            ret = stringBuilder.toString();
        }
        if (this.mProfileVersion == -1) {
            return ret;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(ret);
        stringBuilder.append("Profile_version: ");
        stringBuilder.append(this.mProfileVersion);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
