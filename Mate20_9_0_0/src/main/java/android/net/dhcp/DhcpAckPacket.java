package android.net.dhcp;

import android.net.util.NetworkConstants;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpAckPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpAckPacket(int transId, short secs, boolean broadcast, Inet4Address serverAddress, Inet4Address clientIp, Inet4Address yourIp, byte[] clientMac) {
        super(transId, secs, clientIp, yourIp, serverAddress, INADDR_ANY, clientMac, broadcast);
        this.mBroadcast = broadcast;
        this.mSrcIp = serverAddress;
    }

    public String toString() {
        String s = super.toString();
        String dnsServers = " DNS servers: ";
        for (Inet4Address dnsServer : this.mDnsServers) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dnsServers);
            stringBuilder.append(dnsServer.toString());
            stringBuilder.append(" ");
            dnsServers = stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(s);
        stringBuilder2.append(" ACK: your new IP ");
        stringBuilder2.append("xxx.xxx.xxx.xxx");
        stringBuilder2.append(", netmask ");
        stringBuilder2.append(this.mSubnetMask);
        stringBuilder2.append(", gateways ");
        stringBuilder2.append(this.mGateways);
        stringBuilder2.append(dnsServers);
        stringBuilder2.append(", lease time ");
        stringBuilder2.append(this.mLeaseTime);
        return stringBuilder2.toString();
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    void finishPacket(ByteBuffer buffer) {
        DhcpPacket.addTlv(buffer, (byte) 53, (byte) 5);
        DhcpPacket.addTlv(buffer, (byte) 54, this.mServerIdentifier);
        DhcpPacket.addTlv(buffer, (byte) 51, this.mLeaseTime);
        if (this.mLeaseTime != null) {
            DhcpPacket.addTlv(buffer, (byte) 58, Integer.valueOf(this.mLeaseTime.intValue() / 2));
        }
        DhcpPacket.addTlv(buffer, (byte) 1, this.mSubnetMask);
        DhcpPacket.addTlv(buffer, (byte) 3, this.mGateways);
        DhcpPacket.addTlv(buffer, (byte) UsbDescriptor.DESCRIPTORTYPE_BOS, this.mDomainName);
        DhcpPacket.addTlv(buffer, (byte) 28, this.mBroadcastAddress);
        DhcpPacket.addTlv(buffer, (byte) 6, this.mDnsServers);
        DhcpPacket.addTlvEnd(buffer);
    }

    private static final int getInt(Integer v) {
        if (v == null) {
            return 0;
        }
        return v.intValue();
    }
}
