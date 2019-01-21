package android.net.ip;

import android.net.MacAddress;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkErrorMessage;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkNeighborMessage;
import android.net.netlink.StructNdMsg;
import android.net.util.PacketReader;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.util.BitUtils;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;
import libcore.io.IoUtils;

public class IpNeighborMonitor extends PacketReader {
    private static final boolean DBG = false;
    private static final String TAG = IpNeighborMonitor.class.getSimpleName();
    private static final boolean VDBG = false;
    private final NeighborEventConsumer mConsumer;
    private final SharedLog mLog;

    public static class NeighborEvent {
        final long elapsedMs;
        final int ifindex;
        final InetAddress ip;
        final MacAddress macAddr;
        final short msgType;
        final short nudState;

        public NeighborEvent(long elapsedMs, short msgType, int ifindex, InetAddress ip, short nudState, MacAddress macAddr) {
            this.elapsedMs = elapsedMs;
            this.msgType = msgType;
            this.ifindex = ifindex;
            this.ip = ip;
            this.nudState = nudState;
            this.macAddr = macAddr;
        }

        boolean isConnected() {
            return this.msgType != (short) 29 && StructNdMsg.isNudStateConnected(this.nudState);
        }

        boolean isValid() {
            return this.msgType != (short) 29 && StructNdMsg.isNudStateValid(this.nudState);
        }

        public String toString() {
            StringJoiner j = new StringJoiner(",", "NeighborEvent{", "}");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("@");
            stringBuilder.append(this.elapsedMs);
            StringJoiner add = j.add(stringBuilder.toString()).add(NetlinkConstants.stringForNlMsgType(this.msgType));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("if=");
            stringBuilder2.append(this.ifindex);
            add = add.add(stringBuilder2.toString()).add(this.ip.getHostAddress()).add(StructNdMsg.stringForNudState(this.nudState));
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[");
            stringBuilder2.append(this.macAddr);
            stringBuilder2.append("]");
            return add.add(stringBuilder2.toString()).toString();
        }
    }

    public interface NeighborEventConsumer {
        void accept(NeighborEvent neighborEvent);
    }

    public static int startKernelNeighborProbe(int ifIndex, InetAddress ip) {
        String msgSnippet = new StringBuilder();
        msgSnippet.append("probing ip=");
        msgSnippet.append(ip.getHostAddress());
        msgSnippet.append("%");
        msgSnippet.append(ifIndex);
        msgSnippet = msgSnippet.toString();
        try {
            NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_ROUTE, RtNetlinkNeighborMessage.newNewNeighborMessage(1, ip, (short) 16, ifIndex, null));
            return 0;
        } catch (ErrnoException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error ");
            stringBuilder.append(msgSnippet);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -e.errno;
        }
    }

    public IpNeighborMonitor(Handler h, SharedLog log, NeighborEventConsumer cb) {
        super(h, 8192);
        this.mLog = log.forSubComponent(TAG);
        this.mConsumer = cb != null ? cb : -$$Lambda$IpNeighborMonitor$4TdKAwtCtq9Ri1cSdW1mKm0JycM.INSTANCE;
    }

    static /* synthetic */ void lambda$new$0(NeighborEvent event) {
    }

    protected FileDescriptor createFd() {
        FileDescriptor fd = null;
        try {
            fd = NetlinkSocket.forProto(OsConstants.NETLINK_ROUTE);
            Os.bind(fd, new NetlinkSocketAddress(0, OsConstants.RTMGRP_NEIGH));
            Os.connect(fd, new NetlinkSocketAddress(0, 0));
            return fd;
        } catch (ErrnoException | SocketException e) {
            logError("Failed to create rtnetlink socket", e);
            IoUtils.closeQuietly(fd);
            return null;
        }
    }

    protected void handlePacket(byte[] recvbuf, int length) {
        long whenMs = SystemClock.elapsedRealtime();
        ByteBuffer byteBuffer = ByteBuffer.wrap(recvbuf, null, length);
        byteBuffer.order(ByteOrder.nativeOrder());
        parseNetlinkMessageBuffer(byteBuffer, whenMs);
    }

    private void parseNetlinkMessageBuffer(ByteBuffer byteBuffer, long whenMs) {
        while (byteBuffer.remaining() > 0) {
            int position = byteBuffer.position();
            NetlinkMessage nlMsg = NetlinkMessage.parse(byteBuffer);
            if (nlMsg == null || nlMsg.getHeader() == null) {
                byteBuffer.position(position);
                SharedLog sharedLog = this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unparsable netlink msg: ");
                stringBuilder.append(NetlinkConstants.hexify(byteBuffer));
                sharedLog.e(stringBuilder.toString());
                return;
            }
            int srcPortId = nlMsg.getHeader().nlmsg_pid;
            SharedLog sharedLog2;
            StringBuilder stringBuilder2;
            if (srcPortId != 0) {
                sharedLog2 = this.mLog;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("non-kernel source portId: ");
                stringBuilder2.append(BitUtils.uint32(srcPortId));
                sharedLog2.e(stringBuilder2.toString());
                return;
            } else if (nlMsg instanceof NetlinkErrorMessage) {
                sharedLog2 = this.mLog;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("netlink error: ");
                stringBuilder2.append(nlMsg);
                sharedLog2.e(stringBuilder2.toString());
            } else if (nlMsg instanceof RtNetlinkNeighborMessage) {
                evaluateRtNetlinkNeighborMessage((RtNetlinkNeighborMessage) nlMsg, whenMs);
            } else {
                sharedLog2 = this.mLog;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("non-rtnetlink neighbor msg: ");
                stringBuilder2.append(nlMsg);
                sharedLog2.i(stringBuilder2.toString());
            }
        }
    }

    private void evaluateRtNetlinkNeighborMessage(RtNetlinkNeighborMessage neighMsg, long whenMs) {
        short msgType = neighMsg.getHeader().nlmsg_type;
        StructNdMsg ndMsg = neighMsg.getNdHeader();
        if (ndMsg == null) {
            this.mLog.e("RtNetlinkNeighborMessage without ND message header!");
            return;
        }
        short s;
        int ifindex = ndMsg.ndm_ifindex;
        InetAddress destination = neighMsg.getDestination();
        if (msgType == (short) 29) {
            s = (short) 0;
        } else {
            s = ndMsg.ndm_state;
        }
        this.mConsumer.accept(new NeighborEvent(whenMs, msgType, ifindex, destination, s, getMacAddress(neighMsg.getLinkLayerAddress())));
    }

    private static MacAddress getMacAddress(byte[] linkLayerAddress) {
        if (linkLayerAddress != null) {
            try {
                return MacAddress.fromBytes(linkLayerAddress);
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse link-layer address: ");
                stringBuilder.append(NetlinkConstants.hexify(linkLayerAddress));
                Log.e(str, stringBuilder.toString());
            }
        }
        return null;
    }
}
