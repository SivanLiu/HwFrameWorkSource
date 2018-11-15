package android.net.netlink;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RtNetlinkNeighborMessage extends NetlinkMessage {
    public static final short NDA_CACHEINFO = (short) 3;
    public static final short NDA_DST = (short) 1;
    public static final short NDA_IFINDEX = (short) 8;
    public static final short NDA_LLADDR = (short) 2;
    public static final short NDA_MASTER = (short) 9;
    public static final short NDA_PORT = (short) 6;
    public static final short NDA_PROBES = (short) 4;
    public static final short NDA_UNSPEC = (short) 0;
    public static final short NDA_VLAN = (short) 5;
    public static final short NDA_VNI = (short) 7;
    private StructNdaCacheInfo mCacheInfo = null;
    private InetAddress mDestination = null;
    private byte[] mLinkLayerAddr = null;
    private StructNdMsg mNdmsg = null;
    private int mNumProbes = 0;

    private static StructNlAttr findNextAttrOfType(short attrType, ByteBuffer byteBuffer) {
        while (byteBuffer != null && byteBuffer.remaining() > 0) {
            StructNlAttr nlAttr = StructNlAttr.peek(byteBuffer);
            if (nlAttr == null) {
                break;
            } else if (nlAttr.nla_type == attrType) {
                return StructNlAttr.parse(byteBuffer);
            } else {
                if (byteBuffer.remaining() < nlAttr.getAlignedLength()) {
                    break;
                }
                byteBuffer.position(byteBuffer.position() + nlAttr.getAlignedLength());
            }
        }
        return null;
    }

    public static RtNetlinkNeighborMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        RtNetlinkNeighborMessage neighMsg = new RtNetlinkNeighborMessage(header);
        neighMsg.mNdmsg = StructNdMsg.parse(byteBuffer);
        if (neighMsg.mNdmsg == null) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mDestination = nlAttr.getValueAsInetAddress();
        }
        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType((short) 2, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mLinkLayerAddr = nlAttr.nla_value;
        }
        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType((short) 4, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mNumProbes = nlAttr.getValueAsInt(0);
        }
        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType((short) 3, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mCacheInfo = StructNdaCacheInfo.parse(nlAttr.getValueAsByteBuffer());
        }
        int kAdditionalSpace = NetlinkConstants.alignedLengthOf(neighMsg.mHeader.nlmsg_len - 28);
        if (byteBuffer.remaining() < kAdditionalSpace) {
            byteBuffer.position(byteBuffer.limit());
        } else {
            byteBuffer.position(baseOffset + kAdditionalSpace);
        }
        return neighMsg;
    }

    public static byte[] newGetNeighborsRequest(int seqNo) {
        byte[] bytes = new byte[28];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_len = 28;
        nlmsghdr.nlmsg_type = (short) 30;
        nlmsghdr.nlmsg_flags = (short) 769;
        nlmsghdr.nlmsg_seq = seqNo;
        nlmsghdr.pack(byteBuffer);
        new StructNdMsg().pack(byteBuffer);
        return bytes;
    }

    public static byte[] newNewNeighborMessage(int seqNo, InetAddress ip, short nudState, int ifIndex, byte[] llAddr) {
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_type = (short) 28;
        nlmsghdr.nlmsg_flags = (short) 261;
        nlmsghdr.nlmsg_seq = seqNo;
        RtNetlinkNeighborMessage msg = new RtNetlinkNeighborMessage(nlmsghdr);
        msg.mNdmsg = new StructNdMsg();
        msg.mNdmsg.ndm_family = (byte) (ip instanceof Inet6Address ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        msg.mNdmsg.ndm_ifindex = ifIndex;
        msg.mNdmsg.ndm_state = nudState;
        msg.mDestination = ip;
        msg.mLinkLayerAddr = llAddr;
        byte[] bytes = new byte[msg.getRequiredSpace()];
        nlmsghdr.nlmsg_len = bytes.length;
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        msg.pack(byteBuffer);
        return bytes;
    }

    private RtNetlinkNeighborMessage(StructNlMsgHdr header) {
        super(header);
    }

    public StructNdMsg getNdHeader() {
        return this.mNdmsg;
    }

    public InetAddress getDestination() {
        return this.mDestination;
    }

    public byte[] getLinkLayerAddress() {
        return this.mLinkLayerAddr;
    }

    public int getProbes() {
        return this.mNumProbes;
    }

    public StructNdaCacheInfo getCacheInfo() {
        return this.mCacheInfo;
    }

    public int getRequiredSpace() {
        int spaceRequired = 28;
        if (this.mDestination != null) {
            spaceRequired = 28 + NetlinkConstants.alignedLengthOf(this.mDestination.getAddress().length + 4);
        }
        if (this.mLinkLayerAddr != null) {
            return spaceRequired + NetlinkConstants.alignedLengthOf(4 + this.mLinkLayerAddr.length);
        }
        return spaceRequired;
    }

    private static void packNlAttr(short nlType, byte[] nlValue, ByteBuffer byteBuffer) {
        StructNlAttr nlAttr = new StructNlAttr();
        nlAttr.nla_type = nlType;
        nlAttr.nla_value = nlValue;
        nlAttr.nla_len = (short) (4 + nlAttr.nla_value.length);
        nlAttr.pack(byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mNdmsg.pack(byteBuffer);
        if (this.mDestination != null) {
            packNlAttr((short) 1, this.mDestination.getAddress(), byteBuffer);
        }
        if (this.mLinkLayerAddr != null) {
            packNlAttr((short) 2, this.mLinkLayerAddr, byteBuffer);
        }
    }

    public String toString() {
        String ipLiteral = this.mDestination == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mDestination.getHostAddress();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RtNetlinkNeighborMessage{ nlmsghdr{");
        stringBuilder.append(this.mHeader == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mHeader.toString());
        stringBuilder.append("}, ndmsg{");
        stringBuilder.append(this.mNdmsg == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mNdmsg.toString());
        stringBuilder.append("}, destination{");
        stringBuilder.append(ipLiteral);
        stringBuilder.append("} linklayeraddr{");
        stringBuilder.append(NetlinkConstants.hexify(this.mLinkLayerAddr));
        stringBuilder.append("} probes{");
        stringBuilder.append(this.mNumProbes);
        stringBuilder.append("} cacheinfo{");
        stringBuilder.append(this.mCacheInfo == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mCacheInfo.toString());
        stringBuilder.append("} }");
        return stringBuilder.toString();
    }
}
