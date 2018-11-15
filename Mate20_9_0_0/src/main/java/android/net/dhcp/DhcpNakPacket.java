package android.net.dhcp;

import android.net.util.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpNakPacket extends DhcpPacket {
    DhcpNakPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac) {
        super(transId, secs, INADDR_ANY, INADDR_ANY, nextIp, relayIp, clientMac, false);
    }

    public String toString() {
        String s = super.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append(" NAK, reason ");
        stringBuilder.append(this.mMessage == null ? "(none)" : this.mMessage);
        return stringBuilder.toString();
    }

    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        Inet4Address destIp = this.mClientIp;
        fillInPacket(encap, destIp, this.mYourIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    void finishPacket(ByteBuffer buffer) {
        DhcpPacket.addTlv(buffer, (byte) 53, (byte) 6);
        DhcpPacket.addTlv(buffer, (byte) 54, this.mServerIdentifier);
        DhcpPacket.addTlv(buffer, (byte) 56, this.mMessage);
        DhcpPacket.addTlvEnd(buffer);
    }
}
