package android.telephony.euicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class EuiccInfo implements Parcelable {
    public static final Creator<EuiccInfo> CREATOR = new Creator<EuiccInfo>() {
        public EuiccInfo createFromParcel(Parcel in) {
            return new EuiccInfo(in, null);
        }

        public EuiccInfo[] newArray(int size) {
            return new EuiccInfo[size];
        }
    };
    private final String osVersion;

    /* synthetic */ EuiccInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public String getOsVersion() {
        return this.osVersion;
    }

    public EuiccInfo(String osVersion) {
        this.osVersion = osVersion;
    }

    private EuiccInfo(Parcel in) {
        this.osVersion = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.osVersion);
    }

    public int describeContents() {
        return 0;
    }
}
