package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class NetworkMisc implements Parcelable {
    public static final Creator<NetworkMisc> CREATOR = new Creator<NetworkMisc>() {
        public NetworkMisc createFromParcel(Parcel in) {
            NetworkMisc networkMisc = new NetworkMisc();
            boolean z = false;
            networkMisc.allowBypass = in.readInt() != 0;
            networkMisc.explicitlySelected = in.readInt() != 0;
            networkMisc.acceptUnvalidated = in.readInt() != 0;
            networkMisc.subscriberId = in.readString();
            if (in.readInt() != 0) {
                z = true;
            }
            networkMisc.provisioningNotificationDisabled = z;
            networkMisc.wifiApType = in.readInt();
            networkMisc.connectToCellularAndWLAN = in.readInt();
            return networkMisc;
        }

        public NetworkMisc[] newArray(int size) {
            return new NetworkMisc[size];
        }
    };
    public boolean acceptUnvalidated;
    public boolean allowBypass;
    public int connectToCellularAndWLAN;
    public boolean explicitlySelected;
    public boolean provisioningNotificationDisabled;
    public String subscriberId;
    public int wifiApType;

    public NetworkMisc(NetworkMisc nm) {
        if (nm != null) {
            this.allowBypass = nm.allowBypass;
            this.explicitlySelected = nm.explicitlySelected;
            this.acceptUnvalidated = nm.acceptUnvalidated;
            this.subscriberId = nm.subscriberId;
            this.provisioningNotificationDisabled = nm.provisioningNotificationDisabled;
            this.wifiApType = nm.wifiApType;
            this.connectToCellularAndWLAN = nm.connectToCellularAndWLAN;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.allowBypass);
        out.writeInt(this.explicitlySelected);
        out.writeInt(this.acceptUnvalidated);
        out.writeString(this.subscriberId);
        out.writeInt(this.provisioningNotificationDisabled);
        out.writeInt(this.wifiApType);
        out.writeInt(this.connectToCellularAndWLAN);
    }
}
