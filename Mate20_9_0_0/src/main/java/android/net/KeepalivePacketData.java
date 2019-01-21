package android.net;

import android.bluetooth.BluetoothHidDevice;
import android.net.ConnectivityManager.PacketKeepalive;
import android.net.util.IpUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.system.OsConstants;
import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class KeepalivePacketData implements Parcelable {
    public static final Creator<KeepalivePacketData> CREATOR = new Creator<KeepalivePacketData>() {
        public KeepalivePacketData createFromParcel(Parcel in) {
            return new KeepalivePacketData(in, null);
        }

        public KeepalivePacketData[] newArray(int size) {
            return new KeepalivePacketData[size];
        }
    };
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final String TAG = "KeepalivePacketData";
    private static final int UDP_HEADER_LENGTH = 8;
    public final InetAddress dstAddress;
    public final int dstPort;
    private final byte[] mPacket;
    public final InetAddress srcAddress;
    public final int srcPort;

    public static class InvalidPacketException extends Exception {
        public final int error;

        public InvalidPacketException(int error) {
            this.error = error;
        }
    }

    /* synthetic */ KeepalivePacketData(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    protected KeepalivePacketData(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort, byte[] data) throws InvalidPacketException {
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.mPacket = data;
        if (srcAddress == null || dstAddress == null || !srcAddress.getClass().getName().equals(dstAddress.getClass().getName())) {
            Log.e(TAG, "Invalid or mismatched InetAddresses in KeepalivePacketData");
            throw new InvalidPacketException(-21);
        } else if (!IpUtils.isValidUdpOrTcpPort(srcPort) || !IpUtils.isValidUdpOrTcpPort(dstPort)) {
            Log.e(TAG, "Invalid ports in KeepalivePacketData");
            throw new InvalidPacketException(-22);
        }
    }

    public byte[] getPacket() {
        return (byte[]) this.mPacket.clone();
    }

    public static KeepalivePacketData nattKeepalivePacket(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort) throws InvalidPacketException {
        if (!(srcAddress instanceof Inet4Address) || !(dstAddress instanceof Inet4Address)) {
            throw new InvalidPacketException(-21);
        } else if (dstPort == PacketKeepalive.NATT_PORT) {
            ByteBuffer buf = ByteBuffer.allocate(29);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort((short) 17664);
            buf.putShort((short) 29);
            buf.putInt(0);
            buf.put(BluetoothHidDevice.SUBCLASS1_KEYBOARD);
            buf.put((byte) OsConstants.IPPROTO_UDP);
            int ipChecksumOffset = buf.position();
            buf.putShort((short) 0);
            buf.put(srcAddress.getAddress());
            buf.put(dstAddress.getAddress());
            buf.putShort((short) srcPort);
            buf.putShort((short) dstPort);
            buf.putShort((short) (29 - 20));
            int udpChecksumOffset = buf.position();
            buf.putShort((short) 0);
            buf.put((byte) -1);
            buf.putShort(ipChecksumOffset, IpUtils.ipChecksum(buf, 0));
            buf.putShort(udpChecksumOffset, IpUtils.udpChecksum(buf, 0, 20));
            return new KeepalivePacketData(srcAddress, srcPort, dstAddress, dstPort, buf.array());
        } else {
            throw new InvalidPacketException(-22);
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.srcAddress.getHostAddress());
        out.writeString(this.dstAddress.getHostAddress());
        out.writeInt(this.srcPort);
        out.writeInt(this.dstPort);
        out.writeByteArray(this.mPacket);
    }

    private KeepalivePacketData(Parcel in) {
        this.srcAddress = NetworkUtils.numericToInetAddress(in.readString());
        this.dstAddress = NetworkUtils.numericToInetAddress(in.readString());
        this.srcPort = in.readInt();
        this.dstPort = in.readInt();
        this.mPacket = in.createByteArray();
    }
}
