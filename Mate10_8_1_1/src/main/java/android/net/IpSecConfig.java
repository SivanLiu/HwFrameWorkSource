package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpSecConfig implements Parcelable {
    public static final Creator<IpSecConfig> CREATOR = new Creator<IpSecConfig>() {
        public IpSecConfig createFromParcel(Parcel in) {
            return new IpSecConfig(in);
        }

        public IpSecConfig[] newArray(int size) {
            return new IpSecConfig[size];
        }
    };
    private static final String TAG = "IpSecConfig";
    int encapLocalPortResourceId;
    int encapRemotePort;
    int encapType;
    Flow[] flow;
    InetAddress localAddress;
    int mode;
    int nattKeepaliveInterval;
    Network network;
    InetAddress remoteAddress;

    public static class Flow {
        IpSecAlgorithm authentication;
        IpSecAlgorithm encryption;
        int spiResourceId;
    }

    public int getMode() {
        return this.mode;
    }

    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public int getSpiResourceId(int direction) {
        return this.flow[direction].spiResourceId;
    }

    public InetAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    public IpSecAlgorithm getEncryption(int direction) {
        return this.flow[direction].encryption;
    }

    public IpSecAlgorithm getAuthentication(int direction) {
        return this.flow[direction].authentication;
    }

    public Network getNetwork() {
        return this.network;
    }

    public int getEncapType() {
        return this.encapType;
    }

    public int getEncapLocalResourceId() {
        return this.encapLocalPortResourceId;
    }

    public int getEncapRemotePort() {
        return this.encapRemotePort;
    }

    public int getNattKeepaliveInterval() {
        return this.nattKeepaliveInterval;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        String str = null;
        out.writeString(this.localAddress != null ? this.localAddress.getHostAddress() : null);
        if (this.remoteAddress != null) {
            str = this.remoteAddress.getHostAddress();
        }
        out.writeString(str);
        out.writeParcelable(this.network, flags);
        out.writeInt(this.flow[0].spiResourceId);
        out.writeParcelable(this.flow[0].encryption, flags);
        out.writeParcelable(this.flow[0].authentication, flags);
        out.writeInt(this.flow[1].spiResourceId);
        out.writeParcelable(this.flow[1].encryption, flags);
        out.writeParcelable(this.flow[1].authentication, flags);
        out.writeInt(this.encapType);
        out.writeInt(this.encapLocalPortResourceId);
        out.writeInt(this.encapRemotePort);
    }

    IpSecConfig() {
        this.flow = new Flow[]{new Flow(), new Flow()};
    }

    private static InetAddress readInetAddressFromParcel(Parcel in) {
        String addrString = in.readString();
        if (addrString == null) {
            return null;
        }
        try {
            return InetAddress.getByName(addrString);
        } catch (UnknownHostException e) {
            Log.wtf(TAG, "Invalid IpAddress " + addrString);
            return null;
        }
    }

    private IpSecConfig(Parcel in) {
        this.flow = new Flow[]{new Flow(), new Flow()};
        this.localAddress = readInetAddressFromParcel(in);
        this.remoteAddress = readInetAddressFromParcel(in);
        this.network = (Network) in.readParcelable(Network.class.getClassLoader());
        this.flow[0].spiResourceId = in.readInt();
        this.flow[0].encryption = (IpSecAlgorithm) in.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.flow[0].authentication = (IpSecAlgorithm) in.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.flow[1].spiResourceId = in.readInt();
        this.flow[1].encryption = (IpSecAlgorithm) in.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.flow[1].authentication = (IpSecAlgorithm) in.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.encapType = in.readInt();
        this.encapLocalPortResourceId = in.readInt();
        this.encapRemotePort = in.readInt();
    }
}
