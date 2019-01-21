package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SdpSapsRecord implements Parcelable {
    public static final Creator CREATOR = new Creator() {
        public SdpSapsRecord createFromParcel(Parcel in) {
            return new SdpSapsRecord(in);
        }

        public SdpRecord[] newArray(int size) {
            return new SdpRecord[size];
        }
    };
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;

    public SdpSapsRecord(int rfcommChannelNumber, int profileVersion, String serviceName) {
        this.mRfcommChannelNumber = rfcommChannelNumber;
        this.mProfileVersion = profileVersion;
        this.mServiceName = serviceName;
    }

    public SdpSapsRecord(Parcel in) {
        this.mRfcommChannelNumber = in.readInt();
        this.mProfileVersion = in.readInt();
        this.mServiceName = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public int getRfcommCannelNumber() {
        return this.mRfcommChannelNumber;
    }

    public int getProfileVersion() {
        return this.mProfileVersion;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRfcommChannelNumber);
        dest.writeInt(this.mProfileVersion);
        dest.writeString(this.mServiceName);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String ret = "Bluetooth MAS SDP Record:\n";
        if (this.mRfcommChannelNumber != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("RFCOMM Chan Number: ");
            stringBuilder.append(this.mRfcommChannelNumber);
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
        if (this.mProfileVersion == -1) {
            return ret;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(ret);
        stringBuilder.append("Profile version: ");
        stringBuilder.append(this.mProfileVersion);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
