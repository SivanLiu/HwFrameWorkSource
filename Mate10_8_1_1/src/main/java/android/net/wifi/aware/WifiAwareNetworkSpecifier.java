package android.net.wifi.aware;

import android.net.NetworkSpecifier;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;
import java.util.Objects;

public final class WifiAwareNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    public static final Creator<WifiAwareNetworkSpecifier> CREATOR = new Creator<WifiAwareNetworkSpecifier>() {
        public WifiAwareNetworkSpecifier createFromParcel(Parcel in) {
            return new WifiAwareNetworkSpecifier(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.createByteArray(), in.createByteArray(), in.readString(), in.readInt());
        }

        public WifiAwareNetworkSpecifier[] newArray(int size) {
            return new WifiAwareNetworkSpecifier[size];
        }
    };
    public static final int NETWORK_SPECIFIER_TYPE_IB = 0;
    public static final int NETWORK_SPECIFIER_TYPE_IB_ANY_PEER = 1;
    public static final int NETWORK_SPECIFIER_TYPE_MAX_VALID = 3;
    public static final int NETWORK_SPECIFIER_TYPE_OOB = 2;
    public static final int NETWORK_SPECIFIER_TYPE_OOB_ANY_PEER = 3;
    public final int clientId;
    public final String passphrase;
    public final int peerId;
    public final byte[] peerMac;
    public final byte[] pmk;
    public final int requestorUid;
    public final int role;
    public final int sessionId;
    public final int type;

    public WifiAwareNetworkSpecifier(int type, int role, int clientId, int sessionId, int peerId, byte[] peerMac, byte[] pmk, String passphrase, int requestorUid) {
        this.type = type;
        this.role = role;
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.peerId = peerId;
        this.peerMac = peerMac;
        this.pmk = pmk;
        this.passphrase = passphrase;
        this.requestorUid = requestorUid;
    }

    public boolean isOutOfBand() {
        return this.type == 2 || this.type == 3;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeInt(this.role);
        dest.writeInt(this.clientId);
        dest.writeInt(this.sessionId);
        dest.writeInt(this.peerId);
        dest.writeByteArray(this.peerMac);
        dest.writeByteArray(this.pmk);
        dest.writeString(this.passphrase);
        dest.writeInt(this.requestorUid);
    }

    public boolean satisfiedBy(NetworkSpecifier other) {
        if (other instanceof WifiAwareAgentNetworkSpecifier) {
            return ((WifiAwareAgentNetworkSpecifier) other).satisfiesAwareNetworkSpecifier(this);
        }
        return equals(other);
    }

    public int hashCode() {
        return ((((((((((((((((this.type + 527) * 31) + this.role) * 31) + this.clientId) * 31) + this.sessionId) * 31) + this.peerId) * 31) + Arrays.hashCode(this.peerMac)) * 31) + Arrays.hashCode(this.pmk)) * 31) + Objects.hashCode(this.passphrase)) * 31) + this.requestorUid;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiAwareNetworkSpecifier)) {
            return false;
        }
        WifiAwareNetworkSpecifier lhs = (WifiAwareNetworkSpecifier) obj;
        if (this.type != lhs.type || this.role != lhs.role || this.clientId != lhs.clientId || this.sessionId != lhs.sessionId || this.peerId != lhs.peerId || !Arrays.equals(this.peerMac, lhs.peerMac) || !Arrays.equals(this.pmk, lhs.pmk) || !Objects.equals(this.passphrase, lhs.passphrase)) {
            z = false;
        } else if (this.requestorUid != lhs.requestorUid) {
            z = false;
        }
        return z;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder("WifiAwareNetworkSpecifier [");
        StringBuilder append = sb.append("type=").append(this.type).append(", role=").append(this.role).append(", clientId=").append(this.clientId).append(", sessionId=").append(this.sessionId).append(", peerId=").append(this.peerId).append(", peerMac=");
        if (this.peerMac == null) {
            str = "<null>";
        } else {
            str = "<non-null>";
        }
        append = append.append(str).append(", pmk=");
        if (this.pmk == null) {
            str = "<null>";
        } else {
            str = "<non-null>";
        }
        append = append.append(str).append(", passphrase=");
        if (this.passphrase == null) {
            str = "<null>";
        } else {
            str = "<non-null>";
        }
        append.append(str).append(", requestorUid=").append(this.requestorUid).append("]");
        return sb.toString();
    }

    public void assertValidFromUid(int requestorUid) {
        if (this.requestorUid != requestorUid) {
            throw new SecurityException("mismatched UIDs");
        }
    }
}
