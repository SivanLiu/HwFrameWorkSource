package android.net.dhcp;

import android.net.util.NetworkConstants;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpOfferPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpOfferPacket(int transId, short secs, boolean broadcast, Inet4Address serverAddress, Inet4Address clientIp, Inet4Address yourIp, byte[] clientMac) {
        super(transId, secs, clientIp, yourIp, INADDR_ANY, INADDR_ANY, clientMac, broadcast);
        this.mSrcIp = serverAddress;
    }

    public String toString() {
        String s = super.toString();
        String dnsServers = ", DNS servers: ";
        if (this.mDnsServers != null) {
            for (Inet4Address dnsServer : this.mDnsServers) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(dnsServers);
                stringBuilder.append(dnsServer);
                stringBuilder.append(" ");
                dnsServers = stringBuilder.toString();
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(s);
        stringBuilder2.append(" OFFER, ip ");
        stringBuilder2.append("xxx.xxx.xxx.xxx");
        stringBuilder2.append(", mask ");
        stringBuilder2.append(this.mSubnetMask);
        stringBuilder2.append(dnsServers);
        stringBuilder2.append(", gateways ");
        stringBuilder2.append(this.mGateways);
        stringBuilder2.append(" lease time ");
        stringBuilder2.append(this.mLeaseTime);
        stringBuilder2.append(", domain ");
        stringBuilder2.append(this.mDomainName);
        return stringBuilder2.toString();
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(encap, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    void finishPacket(ByteBuffer buffer) {
        DhcpPacket.addTlv(buffer, (byte) 53, (byte) 2);
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
}
