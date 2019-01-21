package com.android.internal.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class VpnInfo implements Parcelable {
    public static final Creator<VpnInfo> CREATOR = new Creator<VpnInfo>() {
        public VpnInfo createFromParcel(Parcel source) {
            VpnInfo info = new VpnInfo();
            info.ownerUid = source.readInt();
            info.vpnIface = source.readString();
            info.primaryUnderlyingIface = source.readString();
            return info;
        }

        public VpnInfo[] newArray(int size) {
            return new VpnInfo[size];
        }
    };
    public int ownerUid;
    public String primaryUnderlyingIface;
    public String vpnIface;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VpnInfo{ownerUid=");
        stringBuilder.append(this.ownerUid);
        stringBuilder.append(", vpnIface='");
        stringBuilder.append(this.vpnIface);
        stringBuilder.append('\'');
        stringBuilder.append(", primaryUnderlyingIface='");
        stringBuilder.append(this.primaryUnderlyingIface);
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.ownerUid);
        dest.writeString(this.vpnIface);
        dest.writeString(this.primaryUnderlyingIface);
    }
}
