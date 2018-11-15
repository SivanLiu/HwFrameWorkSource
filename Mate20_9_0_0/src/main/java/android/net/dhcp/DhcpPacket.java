package android.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.metrics.DhcpErrorEvent;
import android.net.util.NetworkConstants;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.WindowManagerService.H;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DhcpPacket {
    protected static final byte CLIENT_ID_ETHER = (byte) 1;
    protected static final byte DHCP_BOOTREPLY = (byte) 2;
    protected static final byte DHCP_BOOTREQUEST = (byte) 1;
    protected static final byte DHCP_BROADCAST_ADDRESS = (byte) 28;
    static final short DHCP_CLIENT = (short) 68;
    protected static final byte DHCP_CLIENT_IDENTIFIER = (byte) 61;
    protected static final byte DHCP_DNS_SERVER = (byte) 6;
    protected static final byte DHCP_DOMAIN_NAME = (byte) 15;
    protected static final byte DHCP_HOST_NAME = (byte) 12;
    protected static final byte DHCP_LEASE_TIME = (byte) 51;
    private static final int DHCP_MAGIC_COOKIE = 1669485411;
    protected static final byte DHCP_MAX_MESSAGE_SIZE = (byte) 57;
    protected static final byte DHCP_MESSAGE = (byte) 56;
    protected static final byte DHCP_MESSAGE_TYPE = (byte) 53;
    protected static final byte DHCP_MESSAGE_TYPE_ACK = (byte) 5;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = (byte) 4;
    protected static final byte DHCP_MESSAGE_TYPE_DISCOVER = (byte) 1;
    protected static final byte DHCP_MESSAGE_TYPE_INFORM = (byte) 8;
    protected static final byte DHCP_MESSAGE_TYPE_NAK = (byte) 6;
    protected static final byte DHCP_MESSAGE_TYPE_OFFER = (byte) 2;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = (byte) 3;
    protected static final byte DHCP_MTU = (byte) 26;
    protected static final byte DHCP_OPTION_END = (byte) -1;
    protected static final byte DHCP_OPTION_PAD = (byte) 0;
    protected static final byte DHCP_PARAMETER_LIST = (byte) 55;
    protected static final byte DHCP_REBINDING_TIME = (byte) 59;
    protected static final byte DHCP_RENEWAL_TIME = (byte) 58;
    protected static final byte DHCP_REQUESTED_IP = (byte) 50;
    protected static final byte DHCP_ROUTER = (byte) 3;
    static final short DHCP_SERVER = (short) 67;
    protected static final byte DHCP_SERVER_IDENTIFIER = (byte) 54;
    protected static final byte DHCP_SUBNET_MASK = (byte) 1;
    protected static final byte DHCP_VENDOR_CLASS_ID = (byte) 60;
    protected static final byte DHCP_VENDOR_INFO = (byte) 43;
    public static final int ENCAP_BOOTP = 2;
    public static final int ENCAP_L2 = 0;
    public static final int ENCAP_L3 = 1;
    public static final byte[] ETHER_BROADCAST = new byte[]{DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END};
    public static final int HWADDR_LEN = 16;
    public static final Inet4Address INADDR_ANY = ((Inet4Address) Inet4Address.ANY);
    public static final Inet4Address INADDR_BROADCAST = ((Inet4Address) Inet4Address.ALL);
    public static final int INFINITE_LEASE = -1;
    private static final short IP_FLAGS_OFFSET = (short) 16384;
    private static final byte IP_TOS_LOWDELAY = (byte) 16;
    private static final byte IP_TTL = (byte) 64;
    private static final byte IP_TYPE_UDP = (byte) 17;
    private static final byte IP_VERSION_HEADER_LEN = (byte) 69;
    protected static final int MAX_LENGTH = 1500;
    private static final int MAX_MTU = 1500;
    public static final int MAX_OPTION_LEN = 255;
    public static final int MINIMUM_LEASE = 60;
    private static final int MIN_MTU = 1280;
    public static final int MIN_PACKET_LENGTH_BOOTP = 236;
    public static final int MIN_PACKET_LENGTH_L2 = 278;
    public static final int MIN_PACKET_LENGTH_L3 = 264;
    protected static final String TAG = "DhcpPacket";
    static String testOverrideHostname = null;
    static String testOverrideVendorId = null;
    protected boolean mBroadcast;
    protected Inet4Address mBroadcastAddress;
    protected final Inet4Address mClientIp;
    protected final byte[] mClientMac;
    protected List<Inet4Address> mDnsServers;
    protected String mDomainName;
    protected List<Inet4Address> mGateways;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected Short mMaxMessageSize;
    protected String mMessage;
    protected Short mMtu;
    private final Inet4Address mNextIp;
    private final Inet4Address mRelayIp;
    protected Inet4Address mRequestedIp;
    protected byte[] mRequestedParams;
    protected final short mSecs;
    protected Inet4Address mServerIdentifier;
    protected Inet4Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final int mTransId;
    protected String mVendorId;
    protected String mVendorInfo;
    protected final Inet4Address mYourIp;

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int errorCode, String msg, Object... args) {
            super(String.format(msg, args));
            this.errorCode = errorCode;
        }
    }

    public abstract ByteBuffer buildPacket(int i, short s, short s2);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac, boolean broadcast) {
        this.mTransId = transId;
        this.mSecs = secs;
        this.mClientIp = clientIp;
        this.mYourIp = yourIp;
        this.mNextIp = nextIp;
        this.mRelayIp = relayIp;
        this.mClientMac = clientMac;
        this.mBroadcast = broadcast;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public byte[] getClientId() {
        byte[] clientId = new byte[(this.mClientMac.length + 1)];
        clientId[0] = (byte) 1;
        System.arraycopy(this.mClientMac, 0, clientId, 1, this.mClientMac.length);
        return clientId;
    }

    protected void fillInPacket(int encap, Inet4Address destIp, Inet4Address srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode, boolean broadcast) {
        int i = encap;
        ByteBuffer byteBuffer = buf;
        byte[] destIpArray = destIp.getAddress();
        byte[] srcIpArray = srcIp.getAddress();
        int ipHeaderOffset = 0;
        int ipLengthOffset = 0;
        int ipChecksumOffset = 0;
        int endIpHeader = 0;
        int udpHeaderOffset = 0;
        int udpLengthOffset = 0;
        int udpChecksumOffset = 0;
        buf.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        if (i == 0) {
            byteBuffer.put(ETHER_BROADCAST);
            byteBuffer.put(this.mClientMac);
            byteBuffer.putShort((short) OsConstants.ETH_P_IP);
        }
        if (i <= 1) {
            ipHeaderOffset = buf.position();
            byteBuffer.put(IP_VERSION_HEADER_LEN);
            byteBuffer.put((byte) 16);
            ipLengthOffset = buf.position();
            byteBuffer.putShort((short) 0);
            byteBuffer.putShort((short) 0);
            byteBuffer.putShort(IP_FLAGS_OFFSET);
            byteBuffer.put(IP_TTL);
            byteBuffer.put(IP_TYPE_UDP);
            ipChecksumOffset = buf.position();
            byteBuffer.putShort((short) 0);
            byteBuffer.put(srcIpArray);
            byteBuffer.put(destIpArray);
            endIpHeader = buf.position();
            udpHeaderOffset = buf.position();
            byteBuffer.putShort(srcUdp);
            byteBuffer.putShort(destUdp);
            udpLengthOffset = buf.position();
            byteBuffer.putShort((short) 0);
            udpChecksumOffset = buf.position();
            byteBuffer.putShort((short) 0);
        } else {
            short s = destUdp;
            short s2 = srcUdp;
        }
        buf.put(requestCode);
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte) this.mClientMac.length);
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.mTransId);
        byteBuffer.putShort(this.mSecs);
        if (broadcast) {
            byteBuffer.putShort(Short.MIN_VALUE);
        } else {
            byteBuffer.putShort((short) 0);
        }
        byteBuffer.put(this.mClientIp.getAddress());
        byteBuffer.put(this.mYourIp.getAddress());
        byteBuffer.put(this.mNextIp.getAddress());
        byteBuffer.put(this.mRelayIp.getAddress());
        byteBuffer.put(this.mClientMac);
        byteBuffer.position(((buf.position() + (16 - this.mClientMac.length)) + 64) + 128);
        byteBuffer.putInt(DHCP_MAGIC_COOKIE);
        finishPacket(byteBuffer);
        if ((buf.position() & 1) == 1) {
            byteBuffer.put((byte) 0);
        }
        if (i <= 1) {
            short udpLen = (short) (buf.position() - udpHeaderOffset);
            byteBuffer.putShort(udpLengthOffset, udpLen);
            byteBuffer.putShort(udpChecksumOffset, (short) checksum(byteBuffer, (((((0 + intAbs(byteBuffer.getShort(ipChecksumOffset + 2))) + intAbs(byteBuffer.getShort(ipChecksumOffset + 4))) + intAbs(byteBuffer.getShort(ipChecksumOffset + 6))) + intAbs(byteBuffer.getShort(ipChecksumOffset + 8))) + 17) + udpLen, udpHeaderOffset, buf.position()));
            byteBuffer.putShort(ipLengthOffset, (short) (buf.position() - ipHeaderOffset));
            byteBuffer.putShort(ipChecksumOffset, (short) checksum(byteBuffer, 0, ipHeaderOffset, endIpHeader));
        }
    }

    private static int intAbs(short v) {
        return NetworkConstants.ARP_HWTYPE_RESERVED_HI & v;
    }

    private int checksum(ByteBuffer buf, int seed, int start, int end) {
        int sum = seed;
        int bufPosition = buf.position();
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();
        buf.position(bufPosition);
        short[] shortArray = new short[((end - start) / 2)];
        shortBuf.get(shortArray);
        for (short s : shortArray) {
            sum += intAbs(s);
        }
        start += shortArray.length * 2;
        if (end != start) {
            short b = (short) buf.get(start);
            if (b < (short) 0) {
                b = (short) (b + 256);
            }
            sum += b * 256;
        }
        int sum2 = ((sum >> 16) & NetworkConstants.ARP_HWTYPE_RESERVED_HI) + (sum & NetworkConstants.ARP_HWTYPE_RESERVED_HI);
        return intAbs((short) (~((((sum2 >> 16) & NetworkConstants.ARP_HWTYPE_RESERVED_HI) + sum2) & NetworkConstants.ARP_HWTYPE_RESERVED_HI)));
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte value) {
        buf.put(type);
        buf.put((byte) 1);
        buf.put(value);
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length <= 255) {
            buf.put(type);
            buf.put((byte) payload.length);
            buf.put(payload);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DHCP option too long: ");
        stringBuilder.append(payload.length);
        stringBuilder.append(" vs. ");
        stringBuilder.append(255);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected static void addTlv(ByteBuffer buf, byte type, Inet4Address addr) {
        if (addr != null) {
            addTlv(buf, type, addr.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, List<Inet4Address> addrs) {
        if (addrs != null && addrs.size() != 0) {
            int optionLen = 4 * addrs.size();
            if (optionLen <= 255) {
                buf.put(type);
                buf.put((byte) optionLen);
                for (Inet4Address addr : addrs) {
                    buf.put(addr.getAddress());
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DHCP option too long: ");
            stringBuilder.append(optionLen);
            stringBuilder.append(" vs. ");
            stringBuilder.append(255);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Short value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 2);
            buf.putShort(value.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Integer value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 4);
            buf.putInt(value.intValue());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, String str) {
        try {
            addTlv(buf, type, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("String is not US-ASCII: ");
            stringBuilder.append(str);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected static void addTlvEnd(ByteBuffer buf) {
        buf.put(DHCP_OPTION_END);
    }

    private String getVendorId() {
        if (testOverrideVendorId != null) {
            return testOverrideVendorId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HUAWEI:android:");
        stringBuilder.append(SystemProperties.get("ro.product.board"));
        return stringBuilder.toString();
    }

    private String getHostname() {
        if (testOverrideHostname != null) {
            return testOverrideHostname;
        }
        return SystemProperties.get("net.hostname");
    }

    protected void addCommonClientTlvs(ByteBuffer buf) {
        addTlv(buf, (byte) DHCP_MAX_MESSAGE_SIZE, Short.valueOf((short) 1500));
        addTlv(buf, (byte) DHCP_VENDOR_CLASS_ID, getVendorId());
        String hn = getHostname();
        if (!TextUtils.isEmpty(hn)) {
            addTlv(buf, (byte) 12, hn);
        }
    }

    public static String macToString(byte[] mac) {
        String macAddr = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (int i = 0; i < mac.length; i++) {
            StringBuilder stringBuilder;
            String hexString = new StringBuilder();
            hexString.append("0");
            hexString.append(Integer.toHexString(mac[i]));
            hexString = hexString.toString();
            if (i % 2 == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(macAddr);
                stringBuilder.append(hexString.substring(hexString.length() - 2));
                macAddr = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(macAddr);
                stringBuilder.append("xx");
                macAddr = stringBuilder.toString();
            }
            if (i != mac.length - 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(macAddr);
                stringBuilder.append(":");
                macAddr = stringBuilder.toString();
            }
        }
        return macAddr;
    }

    public String toString() {
        return macToString(this.mClientMac);
    }

    private static Inet4Address readIpAddress(ByteBuffer packet) {
        byte[] ipAddr = new byte[4];
        packet.get(ipAddr);
        try {
            return (Inet4Address) Inet4Address.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer buf, int byteCount, boolean nullOk) {
        byte[] bytes = new byte[byteCount];
        buf.get(bytes);
        int length = bytes.length;
        if (!nullOk) {
            length = 0;
            while (length < bytes.length && bytes[length] != (byte) 0) {
                length++;
            }
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    }

    private static boolean isPacketToOrFromClient(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == (short) 68 || udpDstPort == (short) 68;
    }

    private static boolean isPacketServerToServer(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == DHCP_SERVER && udpDstPort == DHCP_SERVER;
    }

    /* JADX WARNING: Removed duplicated region for block: B:202:0x03e7 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x03de  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    static DhcpPacket decodeFullPacket(ByteBuffer packet, int pktType) throws ParseException {
        Inet4Address netMask;
        String message;
        String vendorId;
        String vendorId2;
        byte[] expectedParams;
        byte ipTypeAndLength;
        int i;
        BufferUnderflowException e;
        String domainName;
        byte dhcpType;
        Integer num;
        String str;
        byte[] bArr;
        ByteBuffer byteBuffer = packet;
        int i2 = pktType;
        List<Inet4Address> dnsServers = new ArrayList();
        List<Inet4Address> gateways = new ArrayList();
        Inet4Address ipDst = null;
        Inet4Address ipSrc = null;
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        Inet4Address serverIdentifier = null;
        Object[] objArr;
        if (i2 != 0) {
            netMask = null;
            message = null;
            vendorId = null;
            vendorId2 = true;
        } else if (packet.remaining() >= MIN_PACKET_LENGTH_L2) {
            byte[] l2dst = new byte[6];
            netMask = null;
            Inet4Address netMask2 = new byte[6];
            byteBuffer.get(l2dst);
            byteBuffer.get(netMask2);
            short l2type = packet.getShort();
            if (l2type == OsConstants.ETH_P_IP) {
                message = null;
                vendorId = null;
                vendorId2 = true;
            } else {
                byte[] l2src = netMask2;
                netMask2 = DhcpErrorEvent.L2_WRONG_ETH_TYPE;
                message = null;
                objArr = new Object[2];
                objArr[0] = Short.valueOf(l2type);
                objArr[1] = Integer.valueOf(OsConstants.ETH_P_IP);
                throw new ParseException(netMask2, "Unexpected L2 type 0x%04x, expected 0x%04x", objArr);
            }
        } else {
            netMask = null;
            message = null;
            int i3 = DhcpErrorEvent.L2_TOO_SHORT;
            objArr = new Object[2];
            objArr[0] = Integer.valueOf(packet.remaining());
            vendorId = null;
            objArr[1] = Integer.valueOf(MIN_PACKET_LENGTH_L2);
            throw new ParseException(i3, "L2 packet too short, %d < %d", objArr);
        }
        String vendorInfo;
        if (i2 > vendorId2) {
            vendorInfo = null;
            expectedParams = null;
        } else if (packet.remaining() >= MIN_PACKET_LENGTH_L3) {
            ipTypeAndLength = packet.get();
            if (((ipTypeAndLength & 240) >> 4) == 4) {
                byte ipDiffServicesField = packet.get();
                short ipTotalLength = packet.getShort();
                short ipIdentification = packet.getShort();
                byte ipFlags = packet.get();
                byte ipFragOffset = packet.get();
                byte ipTTL = packet.get();
                byte ipProto = packet.get();
                short ipChksm = packet.getShort();
                ipSrc = readIpAddress(packet);
                ipDst = readIpAddress(packet);
                byte ipTypeAndLength2;
                if (ipProto == IP_TYPE_UDP) {
                    ipDiffServicesField = (ipTypeAndLength & 15) - 5;
                    byte i4 = (byte) 0;
                    while (true) {
                        ipTypeAndLength2 = ipTypeAndLength;
                        ipTypeAndLength = i4;
                        if (ipTypeAndLength >= ipDiffServicesField) {
                            break;
                        }
                        packet.getInt();
                        i4 = ipTypeAndLength + 1;
                        ipTypeAndLength = ipTypeAndLength2;
                    }
                    ipTypeAndLength = packet.getShort();
                    short udpDstPort = packet.getShort();
                    short udpLen = packet.getShort();
                    short udpChkSum = packet.getShort();
                    if (isPacketToOrFromClient(ipTypeAndLength, udpDstPort)) {
                        vendorInfo = null;
                        expectedParams = null;
                    } else if (isPacketServerToServer(ipTypeAndLength, udpDstPort)) {
                        vendorInfo = null;
                        expectedParams = null;
                    } else {
                        vendorInfo = null;
                        i = DhcpErrorEvent.L4_WRONG_PORT;
                        expectedParams = null;
                        Object[] objArr2 = new Object[2];
                        objArr2[0] = Short.valueOf(ipTypeAndLength);
                        byte udpSrcPort = ipTypeAndLength;
                        objArr2[(byte) 1] = Short.valueOf(udpDstPort);
                        throw new ParseException(i, "Unexpected UDP ports %d->%d", objArr2);
                    }
                }
                ipTypeAndLength2 = ipTypeAndLength;
                short s = ipTotalLength;
                vendorInfo = null;
                expectedParams = null;
                throw new ParseException(DhcpErrorEvent.L4_NOT_UDP, "Protocol not UDP: %d", Byte.valueOf(ipProto));
            }
            vendorInfo = null;
            expectedParams = null;
            throw new ParseException(DhcpErrorEvent.L3_NOT_IPV4, "Invalid IP version %d", Integer.valueOf((ipTypeAndLength & 240) >> 4));
        } else {
            vendorInfo = null;
            expectedParams = null;
            throw new ParseException(DhcpErrorEvent.L3_TOO_SHORT, "L3 packet too short, %d < %d", Integer.valueOf(packet.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_L3));
        }
        List<Inet4Address> list;
        Inet4Address inet4Address;
        if (i2 > 2 || packet.remaining() < MIN_PACKET_LENGTH_BOOTP) {
            list = gateways;
            inet4Address = ipDst;
            throw new ParseException(DhcpErrorEvent.BOOTP_TOO_SHORT, "Invalid type or BOOTP packet too short, %d < %d", Integer.valueOf(packet.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_BOOTP));
        }
        ipTypeAndLength = packet.get();
        byte hwType = packet.get();
        int addrLen = packet.get() & 255;
        byte hops = packet.get();
        i = packet.getInt();
        short secs = packet.getShort();
        boolean broadcast = (packet.getShort() & 32768) != 0;
        byte[] ipv4addr = new byte[4];
        List<Inet4Address> list2;
        byte b;
        try {
            String vendorInfo2;
            byteBuffer.get(ipv4addr);
            Inet4Address clientIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            byteBuffer.get(ipv4addr);
            Inet4Address yourIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            byteBuffer.get(ipv4addr);
            Inet4Address nextIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            byteBuffer.get(ipv4addr);
            Inet4Address relayIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            if (addrLen > 16) {
                addrLen = ETHER_BROADCAST.length;
            }
            if (addrLen > 16) {
                addrLen = ETHER_BROADCAST.length;
            }
            ipTypeAndLength = new byte[addrLen];
            byteBuffer.get(ipTypeAndLength);
            byteBuffer.position(packet.position() + (16 - addrLen));
            String vendorInfo3 = readAsciiString(byteBuffer, 64, (byte) 0);
            if (vendorInfo3.isEmpty()) {
                vendorInfo2 = vendorInfo3;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hostname:");
                stringBuilder.append(vendorInfo3);
                vendorInfo2 = stringBuilder.toString();
            }
            byteBuffer.position(packet.position() + 128);
            boolean hops2;
            if (packet.remaining() >= 4) {
                try {
                    if (packet.getInt() == DHCP_MAGIC_COOKIE) {
                        byte[] expectedParams2;
                        Inet4Address requestedIp;
                        Short mtu;
                        String vendorInfo4 = vendorInfo2;
                        ipv4addr = null;
                        Short maxMessageSize = null;
                        Integer leaseTime = null;
                        Integer T1 = null;
                        Integer T2 = null;
                        hwType = DHCP_OPTION_END;
                        Inet4Address serverIdentifier2 = serverIdentifier;
                        Inet4Address netMask3 = netMask;
                        String domainName2 = message;
                        String vendorId3 = vendorId;
                        byte[] expectedParams3 = expectedParams;
                        boolean notFinishedOptions = true;
                        ipDst = null;
                        Short mtu2 = null;
                        Inet4Address requestedIp2 = null;
                        Inet4Address requestedIp3 = null;
                        while (true) {
                            Short mtu3 = notFinishedOptions;
                            expectedParams2 = expectedParams3;
                            requestedIp = requestedIp3;
                            if (packet.position() >= packet.limit() || mtu3 == null) {
                                mtu = mtu2;
                            } else {
                                hops = packet.get();
                                if (hops == DHCP_OPTION_END) {
                                    notFinishedOptions = false;
                                } else if (hops == (byte) 0) {
                                    notFinishedOptions = mtu3;
                                } else {
                                    try {
                                        int optionLen = packet.get() & 255;
                                        int expectedLen = 0;
                                        mtu = mtu2;
                                        if (hops != (byte) 1) {
                                            if (hops == (byte) 3) {
                                                mtu2 = null;
                                                while (mtu2 < optionLen) {
                                                    gateways.add(readIpAddress(packet));
                                                    mtu2 += 4;
                                                }
                                            } else if (hops == (byte) 6) {
                                                mtu2 = null;
                                                while (mtu2 < optionLen) {
                                                    dnsServers.add(readIpAddress(packet));
                                                    mtu2 += 4;
                                                }
                                            } else if (hops == (byte) 12) {
                                                expectedLen = optionLen;
                                                Object ipDst2 = readAsciiString(byteBuffer, optionLen, null);
                                            } else if (hops == (byte) 15) {
                                                expectedLen = optionLen;
                                                ipv4addr = readAsciiString(byteBuffer, optionLen, null);
                                            } else if (hops == DHCP_MTU) {
                                                expectedLen = 2;
                                                mtu = Short.valueOf(packet.getShort());
                                            } else if (hops == DHCP_BROADCAST_ADDRESS) {
                                                Object requestedIp4 = readIpAddress(packet);
                                                expectedLen = 4;
                                            } else if (hops != DHCP_VENDOR_INFO) {
                                                switch (hops) {
                                                    case HdmiCecKeycode.CEC_KEYCODE_PREVIOUS_CHANNEL /*50*/:
                                                        expectedLen = 4;
                                                        Object requestedIp5 = readIpAddress(packet);
                                                        break;
                                                    case (byte) 51:
                                                        Number leaseTime2 = Integer.valueOf(packet.getInt());
                                                        expectedLen = 4;
                                                        break;
                                                    default:
                                                        switch (hops) {
                                                            case (byte) 53:
                                                                hwType = packet.get();
                                                                expectedLen = 1;
                                                                break;
                                                            case (byte) 54:
                                                                Object serverIdentifier3 = readIpAddress(packet);
                                                                expectedLen = 4;
                                                                break;
                                                            case (byte) 55:
                                                                mtu2 = new byte[optionLen];
                                                                try {
                                                                    byteBuffer.get(mtu2);
                                                                    expectedLen = optionLen;
                                                                    expectedParams2 = mtu2;
                                                                    break;
                                                                } catch (BufferUnderflowException e2) {
                                                                    e = e2;
                                                                    Short sh = mtu2;
                                                                    break;
                                                                }
                                                            case (byte) 56:
                                                                expectedLen = optionLen;
                                                                domainName2 = readAsciiString(byteBuffer, optionLen, null);
                                                                break;
                                                            case H.NOTIFY_KEYGUARD_TRUSTED_CHANGED /*57*/:
                                                                expectedLen = 2;
                                                                maxMessageSize = Short.valueOf(packet.getShort());
                                                                break;
                                                            case H.SET_HAS_OVERLAY_UI /*58*/:
                                                                expectedLen = 4;
                                                                Number T12 = Integer.valueOf(packet.getInt());
                                                                break;
                                                            case H.SET_RUNNING_REMOTE_ANIMATION /*59*/:
                                                                expectedLen = 4;
                                                                Number T22 = Integer.valueOf(packet.getInt());
                                                                break;
                                                            case (byte) 60:
                                                                expectedLen = optionLen;
                                                                vendorId3 = readAsciiString(byteBuffer, optionLen, true);
                                                                break;
                                                            case H.RECOMPUTE_FOCUS /*61*/:
                                                                byteBuffer.get(new byte[optionLen]);
                                                                expectedLen = optionLen;
                                                                break;
                                                            default:
                                                                mtu2 = null;
                                                                while (mtu2 < optionLen) {
                                                                    expectedLen++;
                                                                    try {
                                                                        packet.get();
                                                                        mtu2++;
                                                                    } catch (BufferUnderflowException e3) {
                                                                        e = e3;
                                                                        domainName = ipv4addr;
                                                                        throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, hops), "BufferUnderflowException", new Object[null]);
                                                                    }
                                                                }
                                                                break;
                                                        }
                                                }
                                            } else {
                                                expectedLen = optionLen;
                                                vendorInfo4 = readAsciiString(byteBuffer, optionLen, true);
                                            }
                                            requestedIp3 = requestedIp;
                                            if (mtu2 != optionLen) {
                                                notFinishedOptions = mtu3;
                                                mtu2 = mtu;
                                                expectedParams3 = expectedParams2;
                                            } else {
                                                String domainName3 = ipv4addr;
                                                try {
                                                    dhcpType = hwType;
                                                } catch (BufferUnderflowException e4) {
                                                    e = e4;
                                                    dhcpType = hwType;
                                                    num = leaseTime;
                                                    str = domainName2;
                                                    requestedIp = requestedIp3;
                                                    ipv4addr = domainName3;
                                                    domainName = ipv4addr;
                                                    throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, hops), "BufferUnderflowException", new Object[null]);
                                                }
                                                try {
                                                    num = leaseTime;
                                                    try {
                                                        try {
                                                            throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.DHCP_INVALID_OPTION_LENGTH, hops), "Invalid length %d for option %d, expected %d", new Object[]{Integer.valueOf(optionLen), Byte.valueOf(hops), Integer.valueOf(mtu2)});
                                                        } catch (BufferUnderflowException e5) {
                                                            e = e5;
                                                            requestedIp = requestedIp3;
                                                            ipv4addr = domainName3;
                                                            hwType = dhcpType;
                                                            leaseTime = num;
                                                            domainName = ipv4addr;
                                                            throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, hops), "BufferUnderflowException", new Object[null]);
                                                        }
                                                    } catch (BufferUnderflowException e6) {
                                                        e = e6;
                                                        str = domainName2;
                                                        requestedIp = requestedIp3;
                                                        ipv4addr = domainName3;
                                                        hwType = dhcpType;
                                                        leaseTime = num;
                                                        domainName = ipv4addr;
                                                        throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, hops), "BufferUnderflowException", new Object[null]);
                                                    }
                                                } catch (BufferUnderflowException e7) {
                                                    e = e7;
                                                    num = leaseTime;
                                                    str = domainName2;
                                                    requestedIp = requestedIp3;
                                                    ipv4addr = domainName3;
                                                    hwType = dhcpType;
                                                    domainName = ipv4addr;
                                                    throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, hops), "BufferUnderflowException", new Object[null]);
                                                }
                                            }
                                        }
                                        Object netMask4 = readIpAddress(packet);
                                        expectedLen = 4;
                                        requestedIp3 = requestedIp;
                                        mtu2 = expectedLen;
                                        if (mtu2 != optionLen) {
                                        }
                                    } catch (BufferUnderflowException e8) {
                                        e = e8;
                                        mtu = mtu2;
                                    }
                                }
                                requestedIp3 = requestedIp;
                                expectedParams3 = expectedParams2;
                            }
                        }
                        mtu = mtu2;
                        String domainName4;
                        byte[] expectedParams4;
                        Inet4Address serverIdentifier4;
                        Inet4Address netMask5;
                        Short maxMessageSize2;
                        Integer T13;
                        Integer T23;
                        String vendorId4;
                        if (hwType != DHCP_OPTION_END) {
                            DhcpPacket newPacket;
                            DhcpPacket dhcpRequestPacket;
                            if (hwType != (byte) 8) {
                                DhcpPacket dhcpOfferPacket;
                                switch (hwType) {
                                    case (byte) 1:
                                        newPacket = new DhcpDiscoverPacket(i, secs, ipTypeAndLength, broadcast);
                                        break;
                                    case (byte) 2:
                                        dhcpOfferPacket = new DhcpOfferPacket(i, secs, broadcast, ipSrc, clientIp, yourIp, ipTypeAndLength);
                                        break;
                                    case (byte) 3:
                                        dhcpRequestPacket = new DhcpRequestPacket(i, secs, clientIp, ipTypeAndLength, broadcast);
                                        break;
                                    case (byte) 4:
                                        dhcpRequestPacket = new DhcpDeclinePacket(i, secs, clientIp, yourIp, nextIp, relayIp, ipTypeAndLength);
                                        break;
                                    case (byte) 5:
                                        dhcpOfferPacket = new DhcpAckPacket(i, secs, broadcast, ipSrc, clientIp, yourIp, ipTypeAndLength);
                                        break;
                                    case (byte) 6:
                                        DhcpPacket dhcpNakPacket = new DhcpNakPacket(i, secs, clientIp, yourIp, nextIp, relayIp, ipTypeAndLength);
                                        break;
                                    default:
                                        throw new ParseException(DhcpErrorEvent.DHCP_UNKNOWN_MSG_TYPE, "Unimplemented DHCP type %d", Byte.valueOf(hwType));
                                }
                            } else {
                                dhcpRequestPacket = new DhcpInformPacket(i, secs, clientIp, yourIp, nextIp, relayIp, ipTypeAndLength);
                            }
                            newPacket.mBroadcastAddress = requestedIp2;
                            newPacket.mDnsServers = dnsServers;
                            newPacket.mDomainName = ipv4addr;
                            newPacket.mGateways = gateways;
                            newPacket.mHostName = ipDst;
                            newPacket.mLeaseTime = leaseTime;
                            newPacket.mMessage = domainName2;
                            newPacket.mMtu = mtu;
                            newPacket.mRequestedIp = requestedIp;
                            domainName4 = ipv4addr;
                            ipv4addr = expectedParams2;
                            newPacket.mRequestedParams = ipv4addr;
                            expectedParams4 = ipv4addr;
                            ipv4addr = serverIdentifier2;
                            newPacket.mServerIdentifier = ipv4addr;
                            serverIdentifier4 = ipv4addr;
                            ipv4addr = netMask3;
                            newPacket.mSubnetMask = ipv4addr;
                            netMask5 = ipv4addr;
                            ipv4addr = maxMessageSize;
                            newPacket.mMaxMessageSize = ipv4addr;
                            maxMessageSize2 = ipv4addr;
                            ipv4addr = T1;
                            newPacket.mT1 = ipv4addr;
                            T13 = ipv4addr;
                            ipv4addr = T2;
                            newPacket.mT2 = ipv4addr;
                            T23 = ipv4addr;
                            ipv4addr = vendorId3;
                            newPacket.mVendorId = ipv4addr;
                            vendorId4 = ipv4addr;
                            newPacket.mVendorInfo = vendorInfo4;
                            return newPacket;
                        }
                        domainName4 = ipv4addr;
                        hops2 = broadcast;
                        vendorId4 = vendorId3;
                        netMask5 = netMask3;
                        maxMessageSize2 = maxMessageSize;
                        serverIdentifier4 = serverIdentifier2;
                        T13 = T1;
                        T23 = T2;
                        expectedParams4 = expectedParams2;
                        requestedIp3 = requestedIp;
                        mtu2 = mtu;
                        throw new ParseException(DhcpErrorEvent.DHCP_NO_MSG_TYPE, "No DHCP message type option", new Object[null]);
                    }
                    list2 = dnsServers;
                    list = gateways;
                    b = hops;
                    inet4Address = ipDst;
                    hops2 = broadcast;
                    try {
                        throw new ParseException(DhcpErrorEvent.DHCP_BAD_MAGIC_COOKIE, "Bad magic cookie 0x%08x, should be 0x%08x", Integer.valueOf(packet.getInt()), Integer.valueOf(DHCP_MAGIC_COOKIE));
                    } catch (BufferUnderflowException e9) {
                        throw new ParseException(DhcpErrorEvent.BUFFER_UNDERFLOW, "BufferUnderflowException", new Object[0]);
                    }
                } catch (BufferUnderflowException e10) {
                    bArr = ipv4addr;
                    list2 = dnsServers;
                    list = gateways;
                    b = hops;
                    inet4Address = ipDst;
                    hops2 = broadcast;
                    throw new ParseException(DhcpErrorEvent.BUFFER_UNDERFLOW, "BufferUnderflowException", new Object[0]);
                }
            }
            list2 = dnsServers;
            list = gateways;
            b = hops;
            inet4Address = ipDst;
            hops2 = broadcast;
            throw new ParseException(DhcpErrorEvent.DHCP_NO_COOKIE, "not a DHCP message", new Object[0]);
        } catch (UnknownHostException e11) {
            list2 = dnsServers;
            list = gateways;
            byte b2 = ipTypeAndLength;
            byte b3 = hwType;
            b = hops;
            inet4Address = ipDst;
            throw new ParseException(DhcpErrorEvent.L3_INVALID_IP, "Invalid IPv4 address: %s", Arrays.toString(bArr));
        }
    }

    public static DhcpPacket decodeFullPacket(byte[] packet, int length, int pktType) throws ParseException {
        try {
            return decodeFullPacket(ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN), pktType);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e2) {
            throw new ParseException(DhcpErrorEvent.PARSING_ERROR, e2.getMessage(), new Object[0]);
        }
    }

    public DhcpResults toDhcpResults() {
        int prefixLength;
        Inet4Address ipAddress = this.mYourIp;
        if (ipAddress.equals(Inet4Address.ANY)) {
            ipAddress = this.mClientIp;
            if (ipAddress.equals(Inet4Address.ANY)) {
                return null;
            }
        }
        if (this.mSubnetMask != null) {
            try {
                prefixLength = NetworkUtils.netmaskToPrefixLength(this.mSubnetMask);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        prefixLength = NetworkUtils.getImplicitNetmask(ipAddress);
        DhcpResults results = new DhcpResults();
        try {
            results.ipAddress = new LinkAddress(ipAddress, prefixLength);
            int i = 0;
            if (this.mGateways.size() > 0) {
                results.gateway = (InetAddress) this.mGateways.get(0);
            }
            results.dnsServers.addAll(this.mDnsServers);
            results.domains = this.mDomainName;
            results.serverAddress = this.mServerIdentifier;
            results.vendorInfo = this.mVendorInfo;
            results.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            if (this.mMtu != null && (short) 1280 <= this.mMtu.shortValue() && this.mMtu.shortValue() <= (short) 1500) {
                i = this.mMtu.shortValue();
            }
            results.mtu = i;
            return results;
        } catch (IllegalArgumentException e2) {
            return null;
        }
    }

    public long getLeaseTimeMillis() {
        if (this.mLeaseTime == null || this.mLeaseTime.intValue() == -1) {
            return 0;
        }
        if (this.mLeaseTime.intValue() < 0 || this.mLeaseTime.intValue() >= 60) {
            return (((long) this.mLeaseTime.intValue()) & 4294967295L) * 1000;
        }
        return 60000;
    }

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, short secs, byte[] clientMac, boolean broadcast, byte[] expectedParams) {
        DhcpPacket pkt = new DhcpDiscoverPacket(transactionId, secs, clientMac, broadcast);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }

    public static ByteBuffer buildOfferPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpOfferPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mSubnetMask = netMask;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpAckPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mSubnetMask = netMask;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int encap, int transactionId, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac) {
        DhcpPacket pkt = new DhcpNakPacket(transactionId, (short) 0, clientIpAddr, serverIpAddr, serverIpAddr, serverIpAddr, mac);
        pkt.mMessage = "requested address not available";
        pkt.mRequestedIp = clientIpAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int encap, int transactionId, short secs, Inet4Address clientIp, boolean broadcast, byte[] clientMac, Inet4Address requestedIpAddress, Inet4Address serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpRequestPacket(transactionId, secs, clientIp, clientMac, broadcast);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }

    public static ByteBuffer buildDeclinePacket(int encap, int transactionId, short secs, Inet4Address clientIp, boolean broadcast, byte[] clientMac, Inet4Address requestedIpAddress, Inet4Address serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpDeclinePacket(transactionId, secs, INADDR_ANY, INADDR_ANY, INADDR_ANY, INADDR_ANY, clientMac);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }
}
