package android.net.netlink;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConntrackMessage extends NetlinkMessage {
    public static final short CTA_IP_V4_DST = (short) 2;
    public static final short CTA_IP_V4_SRC = (short) 1;
    public static final short CTA_PROTO_DST_PORT = (short) 3;
    public static final short CTA_PROTO_NUM = (short) 1;
    public static final short CTA_PROTO_SRC_PORT = (short) 2;
    public static final short CTA_TIMEOUT = (short) 7;
    public static final short CTA_TUPLE_IP = (short) 1;
    public static final short CTA_TUPLE_ORIG = (short) 1;
    public static final short CTA_TUPLE_PROTO = (short) 2;
    public static final short CTA_TUPLE_REPLY = (short) 2;
    public static final short IPCTNL_MSG_CT_NEW = (short) 0;
    public static final short NFNL_SUBSYS_CTNETLINK = (short) 1;
    public static final int STRUCT_SIZE = 20;
    protected StructNfGenMsg mNfGenMsg = new StructNfGenMsg((byte) OsConstants.AF_INET);

    public static byte[] newIPv4TimeoutUpdateRequest(int proto, Inet4Address src, int sport, Inet4Address dst, int dport, int timeoutSec) {
        r7 = new StructNlAttr[2];
        r7[0] = new StructNlAttr((short) 1, new StructNlAttr((short) 1, (InetAddress) src), new StructNlAttr((short) 2, (InetAddress) dst));
        r7[1] = new StructNlAttr((short) 2, new StructNlAttr((short) 1, (byte) proto), new StructNlAttr((short) 2, (short) sport, ByteOrder.BIG_ENDIAN), new StructNlAttr((short) 3, (short) dport, ByteOrder.BIG_ENDIAN));
        StructNlAttr ctaTupleOrig = new StructNlAttr((short) 1, r7);
        StructNlAttr ctaTimeout = new StructNlAttr((short) 7, timeoutSec, ByteOrder.BIG_ENDIAN);
        byte[] bytes = new byte[((ctaTupleOrig.getAlignedLength() + ctaTimeout.getAlignedLength()) + 20)];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        ConntrackMessage ctmsg = new ConntrackMessage();
        ctmsg.mHeader.nlmsg_len = bytes.length;
        ctmsg.mHeader.nlmsg_type = (short) 256;
        ctmsg.mHeader.nlmsg_flags = (short) 261;
        ctmsg.mHeader.nlmsg_seq = 1;
        ctmsg.pack(byteBuffer);
        ctaTupleOrig.pack(byteBuffer);
        ctaTimeout.pack(byteBuffer);
        return bytes;
    }

    private ConntrackMessage() {
        super(new StructNlMsgHdr());
    }

    public void pack(ByteBuffer byteBuffer) {
        this.mHeader.pack(byteBuffer);
        this.mNfGenMsg.pack(byteBuffer);
    }
}
