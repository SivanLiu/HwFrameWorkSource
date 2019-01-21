package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SdpPseRecord implements Parcelable {
    public static final Creator CREATOR = new Creator() {
        public SdpPseRecord createFromParcel(Parcel in) {
            return new SdpPseRecord(in);
        }

        public SdpPseRecord[] newArray(int size) {
            return new SdpPseRecord[size];
        }
    };
    private final int mL2capPsm;
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;
    private final int mSupportedFeatures;
    private final int mSupportedRepositories;

    public SdpPseRecord(int l2capPsm, int rfcommChannelNumber, int profileVersion, int supportedFeatures, int supportedRepositories, String serviceName) {
        this.mL2capPsm = l2capPsm;
        this.mRfcommChannelNumber = rfcommChannelNumber;
        this.mProfileVersion = profileVersion;
        this.mSupportedFeatures = supportedFeatures;
        this.mSupportedRepositories = supportedRepositories;
        this.mServiceName = serviceName;
    }

    public SdpPseRecord(Parcel in) {
        this.mRfcommChannelNumber = in.readInt();
        this.mL2capPsm = in.readInt();
        this.mProfileVersion = in.readInt();
        this.mSupportedFeatures = in.readInt();
        this.mSupportedRepositories = in.readInt();
        this.mServiceName = in.readString();
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

    public int getSupportedRepositories() {
        return this.mSupportedRepositories;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRfcommChannelNumber);
        dest.writeInt(this.mL2capPsm);
        dest.writeInt(this.mProfileVersion);
        dest.writeInt(this.mSupportedFeatures);
        dest.writeInt(this.mSupportedRepositories);
        dest.writeString(this.mServiceName);
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
        if (this.mProfileVersion != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("profile version: ");
            stringBuilder.append(this.mProfileVersion);
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
        if (this.mSupportedRepositories == -1) {
            return ret;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(ret);
        stringBuilder.append("Supported repositories: ");
        stringBuilder.append(this.mSupportedRepositories);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
