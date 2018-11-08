package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public final class NetworkScanRequest implements Parcelable {
    public static final Creator<NetworkScanRequest> CREATOR = new Creator<NetworkScanRequest>() {
        public NetworkScanRequest createFromParcel(Parcel in) {
            return new NetworkScanRequest(in);
        }

        public NetworkScanRequest[] newArray(int size) {
            return new NetworkScanRequest[size];
        }
    };
    public static final int MAX_BANDS = 8;
    public static final int MAX_CHANNELS = 32;
    public static final int MAX_RADIO_ACCESS_NETWORKS = 8;
    public static final int SCAN_TYPE_ONE_SHOT = 0;
    public static final int SCAN_TYPE_PERIODIC = 1;
    public int scanType;
    public RadioAccessSpecifier[] specifiers;

    public NetworkScanRequest(int scanType, RadioAccessSpecifier[] specifiers) {
        this.scanType = scanType;
        this.specifiers = specifiers;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.scanType);
        dest.writeParcelableArray(this.specifiers, flags);
    }

    private NetworkScanRequest(Parcel in) {
        this.scanType = in.readInt();
        this.specifiers = (RadioAccessSpecifier[]) in.readParcelableArray(Object.class.getClassLoader(), RadioAccessSpecifier.class);
    }

    public boolean equals(Object o) {
        boolean z = false;
        try {
            NetworkScanRequest nsr = (NetworkScanRequest) o;
            if (o == null) {
                return false;
            }
            if (this.scanType == nsr.scanType) {
                z = Arrays.equals(this.specifiers, nsr.specifiers);
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return (this.scanType * 31) + (Arrays.hashCode(this.specifiers) * 37);
    }
}
