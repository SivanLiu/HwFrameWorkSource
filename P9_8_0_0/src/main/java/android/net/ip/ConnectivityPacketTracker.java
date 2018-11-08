package android.net.ip;

import android.net.NetworkUtils;
import android.net.util.BlockingSocketReader;
import android.net.util.ConnectivityPacketSummary;
import android.os.Handler;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.LocalLog;
import android.util.Log;
import java.io.FileDescriptor;
import java.net.NetworkInterface;
import libcore.util.HexEncoding;

public class ConnectivityPacketTracker {
    private static final boolean DBG = false;
    private static final String MARK_START = "--- START ---";
    private static final String MARK_STOP = "--- STOP ---";
    private static final String TAG = ConnectivityPacketTracker.class.getSimpleName();
    private final Handler mHandler;
    private final LocalLog mLog;
    private final BlockingSocketReader mPacketListener;
    private final String mTag;

    private final class PacketListener extends BlockingSocketReader {
        private final byte[] mHwAddr;
        private final int mIfIndex;

        PacketListener(int ifindex, byte[] hwaddr, int mtu) {
            super(mtu);
            this.mIfIndex = ifindex;
            this.mHwAddr = hwaddr;
        }

        protected FileDescriptor createSocket() {
            FileDescriptor fileDescriptor = null;
            try {
                fileDescriptor = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, 0);
                NetworkUtils.attachControlPacketFilter(fileDescriptor, OsConstants.ARPHRD_ETHER);
                Os.bind(fileDescriptor, new PacketSocketAddress((short) OsConstants.ETH_P_ALL, this.mIfIndex));
                return fileDescriptor;
            } catch (Exception e) {
                logError("Failed to create packet tracking socket: ", e);
                BlockingSocketReader.closeSocket(fileDescriptor);
                return null;
            }
        }

        protected void handlePacket(byte[] recvbuf, int length) {
            String summary = ConnectivityPacketSummary.summarize(this.mHwAddr, recvbuf, length);
            if (summary != null) {
                addLogEntry(summary + "\n[" + new String(HexEncoding.encode(recvbuf, 0, length)) + "]");
            }
        }

        protected void logError(String msg, Exception e) {
            Log.e(ConnectivityPacketTracker.this.mTag, msg, e);
            addLogEntry(msg + e);
        }

        private void addLogEntry(String entry) {
            ConnectivityPacketTracker.this.mHandler.post(new -$Lambda$yDnD85pUPxzgrjWolXWWPPki110(this, entry));
        }

        /* synthetic */ void lambda$-android_net_ip_ConnectivityPacketTracker$PacketListener_4824(String entry) {
            ConnectivityPacketTracker.this.mLog.log(entry);
        }
    }

    public ConnectivityPacketTracker(NetworkInterface netif, LocalLog log) {
        try {
            String ifname = netif.getName();
            int ifindex = netif.getIndex();
            byte[] hwaddr = netif.getHardwareAddress();
            int mtu = netif.getMTU();
            this.mTag = TAG + "." + ifname;
            this.mHandler = new Handler();
            this.mLog = log;
            this.mPacketListener = new PacketListener(ifindex, hwaddr, mtu);
        } catch (Exception e) {
            throw new IllegalArgumentException("bad network interface", e);
        }
    }

    public void start() {
        this.mLog.log(MARK_START);
        this.mPacketListener.start();
    }

    public void stop() {
        this.mPacketListener.stop();
        this.mLog.log(MARK_STOP);
    }
}
