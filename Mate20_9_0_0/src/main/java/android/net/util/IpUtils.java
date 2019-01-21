package android.net.util;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class IpUtils {
    private static int intAbs(short v) {
        return 65535 & v;
    }

    private static int checksum(ByteBuffer buf, int seed, int start, int end) {
        int i;
        int sum = seed;
        int bufPosition = buf.position();
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();
        buf.position(bufPosition);
        int numShorts = (end - start) / 2;
        for (i = 0; i < numShorts; i++) {
            sum += intAbs(shortBuf.get(i));
        }
        start += numShorts * 2;
        if (end != start) {
            short b = (short) buf.get(start);
            if (b < (short) 0) {
                b = (short) (b + 256);
            }
            sum += b * 256;
        }
        i = ((sum >> 16) & 65535) + (sum & 65535);
        return intAbs((short) (~((((i >> 16) & 65535) + i) & 65535)));
    }

    private static int pseudoChecksumIPv4(ByteBuffer buf, int headerOffset, int protocol, int transportLen) {
        return ((((protocol + transportLen) + intAbs(buf.getShort(headerOffset + 12))) + intAbs(buf.getShort(headerOffset + 14))) + intAbs(buf.getShort(headerOffset + 16))) + intAbs(buf.getShort(headerOffset + 18));
    }

    private static int pseudoChecksumIPv6(ByteBuffer buf, int headerOffset, int protocol, int transportLen) {
        int partial = protocol + transportLen;
        for (int offset = 8; offset < 40; offset += 2) {
            partial += intAbs(buf.getShort(headerOffset + offset));
        }
        return partial;
    }

    private static byte ipversion(ByteBuffer buf, int headerOffset) {
        return (byte) ((buf.get(headerOffset) & -16) >> 4);
    }

    public static short ipChecksum(ByteBuffer buf, int headerOffset) {
        return (short) checksum(buf, 0, headerOffset, (((byte) (buf.get(headerOffset) & 15)) * 4) + headerOffset);
    }

    private static short transportChecksum(ByteBuffer buf, int protocol, int ipOffset, int transportOffset, int transportLen) {
        if (transportLen >= 0) {
            int sum;
            byte ver = ipversion(buf, ipOffset);
            if (ver == (byte) 4) {
                sum = pseudoChecksumIPv4(buf, ipOffset, protocol, transportLen);
            } else if (ver == (byte) 6) {
                sum = pseudoChecksumIPv6(buf, ipOffset, protocol, transportLen);
            } else {
                throw new UnsupportedOperationException("Checksum must be IPv4 or IPv6");
            }
            sum = checksum(buf, sum, transportOffset, transportOffset + transportLen);
            if (protocol == OsConstants.IPPROTO_UDP && sum == 0) {
                sum = -1;
            }
            return (short) sum;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transport length < 0: ");
        stringBuilder.append(transportLen);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static short udpChecksum(ByteBuffer buf, int ipOffset, int transportOffset) {
        return transportChecksum(buf, OsConstants.IPPROTO_UDP, ipOffset, transportOffset, intAbs(buf.getShort(transportOffset + 4)));
    }

    public static short tcpChecksum(ByteBuffer buf, int ipOffset, int transportOffset, int transportLen) {
        return transportChecksum(buf, OsConstants.IPPROTO_TCP, ipOffset, transportOffset, transportLen);
    }

    public static String addressAndPortToString(InetAddress address, int port) {
        return String.format(address instanceof Inet6Address ? "[%s]:%d" : "%s:%d", new Object[]{address.getHostAddress(), Integer.valueOf(port)});
    }

    public static boolean isValidUdpOrTcpPort(int port) {
        return port > 0 && port < 65536;
    }
}
