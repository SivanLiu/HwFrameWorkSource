package android.net.dhcp;

import android.content.Context;
import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.dhcp.DhcpPacket.ParseException;
import android.net.ip.IpClient;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.metrics.IpConnectivityLog;
import android.net.util.InterfaceParams;
import android.net.util.NetworkConstants;
import android.os.Message;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import libcore.io.IoBridge;

public class DhcpClient extends AbsDhcpClient {
    private static final int ARP_TIMEOUT_MS = 2000;
    public static final int CMD_CLEAR_LINKADDRESS = 196615;
    public static final int CMD_CONFIGURE_LINKADDRESS = 196616;
    private static final int CMD_EXPIRE_DHCP = 196714;
    private static final int CMD_FAST_ARP_EXIT = 196715;
    private static final int CMD_FAST_ARP_NOT_EXIT = 196716;
    private static final int CMD_KICK = 196709;
    public static final int CMD_ON_QUIT = 196613;
    public static final int CMD_POST_DHCP_ACTION = 196612;
    public static final int CMD_PRE_DHCP_ACTION = 196611;
    public static final int CMD_PRE_DHCP_ACTION_COMPLETE = 196614;
    private static final int CMD_REBIND_DHCP = 196713;
    private static final int CMD_RECEIVED_PACKET = 196710;
    private static final int CMD_RENEW_DHCP = 196712;
    private static final int CMD_SLOW_ARP_EXIT = 196717;
    private static final int CMD_SLOW_ARP_NOT_EXIT = 196718;
    public static final int CMD_START_DHCP = 196609;
    public static final int CMD_STOP_DHCP = 196610;
    private static final int CMD_TIMEOUT = 196711;
    public static final int CMD_TRY_CACHED_IP = 196618;
    private static final boolean DBG = true;
    private static final int DECLINED_TIMES_MAX = 3;
    public static final int DHCP_FAILURE = 2;
    protected static final int DHCP_RESULTS_RECORD_SIZE = 50;
    public static final int DHCP_SUCCESS = 1;
    private static final int DHCP_TIMEOUT_MS = 18000;
    private static final boolean DO_UNICAST = false;
    public static final int EVENT_LINKADDRESS_CONFIGURED = 196617;
    private static final int FIRST_TIMEOUT_MS = 1000;
    private static final int MAX_TIMEOUT_MS = 128000;
    private static final boolean MSG_DBG = false;
    private static final boolean PACKET_DBG = false;
    private static final int PRIVATE_BASE = 196708;
    private static final int PUBLIC_BASE = 196608;
    static final byte[] REQUESTED_PARAMS = new byte[]{(byte) 1, (byte) 3, (byte) 6, UsbDescriptor.DESCRIPTORTYPE_BOS, (byte) 26, (byte) 28, (byte) 51, (byte) 58, (byte) 59, (byte) 43};
    private static final int SECONDS = 1000;
    private static final boolean STATE_DBG = false;
    private static final String TAG = "DhcpClient";
    public static String mDhcpError = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private static final Class[] sMessageClasses = new Class[]{DhcpClient.class};
    private static final SparseArray<String> sMessageNames = MessageUtils.findMessageNames(sMessageClasses);
    HwArpClient mArpClient;
    private State mConfiguringInterfaceState = new ConfiguringInterfaceState();
    private boolean mConnSavedAP = false;
    private final Context mContext;
    private final StateMachine mController;
    private State mDeclineState = new DeclineState();
    private int mDeclinedTimes = 0;
    private int mDhcpAction = CMD_START_DHCP;
    private State mDhcpBoundState = new DhcpBoundState();
    private State mDhcpHaveLeaseState = new DhcpHaveLeaseState();
    private State mDhcpInitRebootState = new DhcpInitRebootState();
    private State mDhcpInitState = new DhcpInitState();
    private DhcpResults mDhcpLease;
    private long mDhcpLeaseExpiry;
    private int mDhcpOfferCnt = 0;
    private State mDhcpRebindingState = new DhcpRebindingState();
    private State mDhcpRebootingState = new DhcpRebootingState();
    private State mDhcpRenewingState = new DhcpRenewingState();
    private State mDhcpRequestingState = new DhcpRequestingState();
    private DhcpResultsInfoRecord mDhcpResultsInfo = null;
    private State mDhcpSelectingState = new DhcpSelectingState();
    private State mDhcpState = new DhcpState();
    private final WakeupMessage mExpiryAlarm;
    private State mFastArpCheckingState = new FastArpCheckingState();
    private String mFastArpUuidStr = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private byte[] mHwAddr;
    private InterfaceParams mIface;
    private final String mIfaceName;
    private PacketSocketAddress mInterfaceBroadcastAddr;
    private final WakeupMessage mKickAlarm;
    private long mLastBoundExitTime;
    private long mLastInitEnterTime;
    private final IpConnectivityLog mMetricsLog = new IpConnectivityLog();
    private DhcpResults mOffer;
    private FileDescriptor mPacketSock;
    private Inet4Address mPendingDHCPServer = null;
    private LinkAddress mPendingIpAddr = null;
    private String mPendingSSID = null;
    private final Random mRandom;
    private boolean mReadDBDone = false;
    private final WakeupMessage mRebindAlarm;
    private ReceiveThread mReceiveThread;
    private boolean mRegisteredForPreDhcpNotification;
    private final WakeupMessage mRenewAlarm;
    private State mSlowArpCheckingState = new SlowArpCheckingState();
    private String mSlowArpUuidStr = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private State mStoppedState = new StoppedState();
    private final WakeupMessage mTimeoutAlarm;
    private int mTransactionId;
    private long mTransactionStartMillis;
    private FileDescriptor mUdpSock;
    private State mWaitBeforeRenewalState = new WaitBeforeRenewalState(this.mDhcpRenewingState);
    private State mWaitBeforeStartState = new WaitBeforeStartState(this.mDhcpInitState);

    class DhcpState extends State {
        DhcpState() {
        }

        public void enter() {
            DhcpClient.this.clearDhcpState();
            if (DhcpClient.this.initInterface() && DhcpClient.this.initSockets()) {
                DhcpClient.this.mReceiveThread = new ReceiveThread();
                DhcpClient.this.mReceiveThread.start();
                return;
            }
            DhcpClient.mDhcpError = "dhcpclient initialize failed";
            DhcpClient.this.notifyFailure();
            DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
        }

        public void exit() {
            if (DhcpClient.this.mReceiveThread != null) {
                DhcpClient.this.mReceiveThread.halt();
                DhcpClient.this.mReceiveThread = null;
            }
            DhcpClient.this.clearDhcpState();
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            int i = message.what;
            if (i == DhcpClient.CMD_STOP_DHCP) {
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
                return true;
            } else if (i != DhcpClient.CMD_RECEIVED_PACKET) {
                return false;
            } else {
                DhcpClient.this.calcDhcpOfferCnt((DhcpPacket) message.obj);
                DhcpClient.this.sendDhcpOfferPacket(DhcpClient.this.mContext, (DhcpPacket) message.obj);
                return true;
            }
        }
    }

    abstract class LoggingState extends State {
        private long mEnterTimeMs;

        LoggingState() {
        }

        public void enter() {
            this.mEnterTimeMs = SystemClock.elapsedRealtime();
        }

        public void exit() {
            DhcpClient.this.logState(getName(), (int) (SystemClock.elapsedRealtime() - this.mEnterTimeMs));
        }

        private String messageName(int what) {
            return (String) DhcpClient.sMessageNames.get(what, Integer.toString(what));
        }

        private String messageToString(Message message) {
            long now = SystemClock.uptimeMillis();
            StringBuilder b = new StringBuilder(" ");
            TimeUtils.formatDuration(message.getWhen() - now, b);
            b.append(" ");
            b.append(messageName(message.what));
            b.append(" ");
            b.append(message.arg1);
            b.append(" ");
            b.append(message.arg2);
            b.append(" ");
            b.append(message.obj);
            return b.toString();
        }

        public boolean processMessage(Message message) {
            return false;
        }

        public String getName() {
            return getClass().getSimpleName();
        }
    }

    class ReceiveThread extends Thread {
        private final byte[] mPacket = new byte[NetworkConstants.ETHER_MTU];
        private volatile boolean mStopped = false;

        ReceiveThread() {
        }

        public void halt() {
            this.mStopped = true;
            DhcpClient.this.closeSockets();
        }

        public void run() {
            Log.d(DhcpClient.TAG, "Receive thread started");
            while (!this.mStopped) {
                int length = 0;
                try {
                    DhcpClient.this.sendMessage(DhcpClient.CMD_RECEIVED_PACKET, DhcpPacket.decodeFullPacket(this.mPacket, Os.read(DhcpClient.this.mPacketSock, this.mPacket, 0, this.mPacket.length), 0));
                } catch (ErrnoException | IOException e) {
                    if (!this.mStopped) {
                        Log.e(DhcpClient.TAG, "Read error", e);
                        DhcpClient.this.logError(DhcpErrorEvent.RECEIVE_ERROR);
                    }
                } catch (ParseException e2) {
                    String str = DhcpClient.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't parse packet: ");
                    stringBuilder.append(e2.getMessage());
                    Log.e(str, stringBuilder.toString());
                    if (e2.errorCode == DhcpErrorEvent.DHCP_NO_COOKIE) {
                        String data = ParseException.class.getName();
                        EventLog.writeEvent(1397638484, new Object[]{"31850211", Integer.valueOf(-1), data});
                    }
                    DhcpClient.this.logError(e2.errorCode);
                } catch (Exception e3) {
                    Log.e(DhcpClient.TAG, "Failed to parse DHCP packet", e3);
                }
            }
            Log.d(DhcpClient.TAG, "Receive thread stopped");
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public boolean processMessage(Message message) {
            if (message.what != DhcpClient.CMD_START_DHCP) {
                return false;
            }
            DhcpClient.this.mDhcpResultsInfo = DhcpClient.this.getDhcpResultsInfoRecord();
            DhcpClient.this.mDhcpAction = DhcpClient.CMD_START_DHCP;
            if (DhcpClient.this.mDhcpResultsInfo != null && DhcpClient.this.mReadDBDone) {
                try {
                    DhcpClient.this.mPendingIpAddr = new LinkAddress(DhcpClient.this.mDhcpResultsInfo.staIP);
                    DhcpClient.this.mPendingDHCPServer = (Inet4Address) InetAddress.getByName(DhcpClient.this.mDhcpResultsInfo.apDhcpServer.substring(1));
                    DhcpClient.this.mConnSavedAP = true;
                } catch (Exception e) {
                    DhcpClient dhcpClient = DhcpClient.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("get IP&DHCPServer address Exception");
                    stringBuilder.append(e);
                    dhcpClient.logd(stringBuilder.toString());
                }
            }
            if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeStartState);
            } else {
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            }
            return true;
        }
    }

    class ConfiguringInterfaceState extends LoggingState {
        ConfiguringInterfaceState() {
            super();
        }

        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CONFIGURE_LINKADDRESS, DhcpClient.this.mDhcpLease.ipAddress);
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != DhcpClient.EVENT_LINKADDRESS_CONFIGURED) {
                return false;
            }
            Log.d(DhcpClient.TAG, "process EVENT_LINKADDRESS_CONFIGURED, returning to slow arp checking");
            DhcpClient.this.transitionTo(DhcpClient.this.mSlowArpCheckingState);
            return true;
        }
    }

    class DhcpBoundState extends LoggingState {
        DhcpBoundState() {
            super();
        }

        public void enter() {
            super.enter();
            if (!(DhcpClient.this.mDhcpLease.serverAddress == null || DhcpClient.this.connectUdpSock(DhcpClient.this.mDhcpLease.serverAddress))) {
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
            }
            DhcpClient.this.scheduleLeaseTimers();
            logTimeToBoundState();
        }

        public void exit() {
            super.exit();
            DhcpClient.this.mLastBoundExitTime = SystemClock.elapsedRealtime();
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != DhcpClient.CMD_RENEW_DHCP) {
                return false;
            }
            DhcpClient.this.mDhcpAction = DhcpClient.CMD_RENEW_DHCP;
            if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeRenewalState);
            } else {
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRenewingState);
            }
            return true;
        }

        private void logTimeToBoundState() {
            long now = SystemClock.elapsedRealtime();
            if (DhcpClient.this.mLastBoundExitTime > DhcpClient.this.mLastInitEnterTime) {
                DhcpClient.this.logState("RenewingBoundState", (int) (now - DhcpClient.this.mLastBoundExitTime));
            } else {
                DhcpClient.this.logState("InitialBoundState", (int) (now - DhcpClient.this.mLastInitEnterTime));
            }
        }
    }

    class DhcpHaveLeaseState extends LoggingState {
        DhcpHaveLeaseState() {
            super();
        }

        public boolean processMessage(Message message) {
            if (message.what != DhcpClient.CMD_EXPIRE_DHCP) {
                return false;
            }
            Log.d(DhcpClient.TAG, "Lease expired!");
            DhcpClient.this.notifyFailure();
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            return true;
        }

        public void exit() {
            Log.d(DhcpClient.TAG, "DhcpHaveLeaseState exit!");
            DhcpClient.this.mRenewAlarm.cancel();
            DhcpClient.this.mRebindAlarm.cancel();
            DhcpClient.this.mExpiryAlarm.cancel();
            DhcpClient.this.clearDhcpState();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CLEAR_LINKADDRESS);
        }
    }

    class DhcpInitRebootState extends LoggingState {
        DhcpInitRebootState() {
            super();
        }
    }

    class DhcpRebootingState extends LoggingState {
        DhcpRebootingState() {
            super();
        }
    }

    class DhcpSelectingState extends LoggingState {
        DhcpSelectingState() {
            super();
        }
    }

    abstract class PacketRetransmittingState extends LoggingState {
        protected int mTimeout = 0;
        private int mTimer;

        protected abstract void receivePacket(DhcpPacket dhcpPacket);

        protected abstract boolean sendPacket();

        PacketRetransmittingState() {
            super();
        }

        public void enter() {
            super.enter();
            initTimer();
            maybeInitTimeout();
            DhcpClient.this.sendMessage(DhcpClient.CMD_KICK);
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_KICK /*196709*/:
                    sendPacket();
                    scheduleKick();
                    return true;
                case DhcpClient.CMD_RECEIVED_PACKET /*196710*/:
                    receivePacket((DhcpPacket) message.obj);
                    return !(message.obj instanceof DhcpOfferPacket);
                case DhcpClient.CMD_TIMEOUT /*196711*/:
                    timeout();
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            super.exit();
            DhcpClient.this.mKickAlarm.cancel();
            DhcpClient.this.mTimeoutAlarm.cancel();
        }

        protected void timeout() {
        }

        protected void tryCachedIp() {
        }

        protected void initTimer() {
            this.mTimer = 1000;
        }

        protected int jitterTimer(int baseTimer) {
            int maxJitter = baseTimer / 10;
            return baseTimer + (DhcpClient.this.mRandom.nextInt(2 * maxJitter) - maxJitter);
        }

        protected void scheduleKick() {
            DhcpClient.this.mKickAlarm.schedule(SystemClock.elapsedRealtime() + ((long) jitterTimer(this.mTimer)));
            if (this.mTimer >= 6000) {
                tryCachedIp();
            }
            this.mTimer *= 2;
            if (this.mTimer > DhcpClient.MAX_TIMEOUT_MS) {
                this.mTimer = DhcpClient.MAX_TIMEOUT_MS;
            }
        }

        protected void maybeInitTimeout() {
            if (this.mTimeout > 0) {
                DhcpClient.this.mTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeout));
            }
        }
    }

    abstract class WaitBeforeOtherState extends LoggingState {
        protected State mOtherState;

        WaitBeforeOtherState() {
            super();
        }

        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION);
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE) {
                return false;
            }
            DhcpClient.this.transitionTo(this.mOtherState);
            return true;
        }
    }

    class DeclineState extends PacketRetransmittingState {
        protected boolean mRet;

        public DeclineState() {
            super();
            this.mRet = false;
            this.mTimeout = 1000;
            DhcpClient.this.mConnSavedAP = false;
        }

        protected boolean sendPacket() {
            this.mRet = DhcpClient.this.sendDeclinePacket(DhcpPacket.INADDR_ANY, (Inet4Address) DhcpClient.this.mDhcpLease.ipAddress.getAddress(), DhcpClient.this.mDhcpLease.serverAddress, DhcpPacket.INADDR_BROADCAST);
            if (this.mRet) {
                DhcpClient.this.mDeclinedTimes = DhcpClient.this.mDeclinedTimes + 1;
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            }
            return this.mRet;
        }

        protected void receivePacket(DhcpPacket packet) {
        }

        protected void timeout() {
            Log.d(DhcpClient.TAG, "After sending ARP unresponse for a while, go to config IP");
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
        }
    }

    class DhcpInitState extends PacketRetransmittingState {
        public DhcpInitState() {
            super();
        }

        public void enter() {
            super.enter();
            DhcpClient.this.startNewTransaction();
            DhcpClient.this.mLastInitEnterTime = SystemClock.elapsedRealtime();
            DhcpClient.this.mDhcpOfferCnt = 0;
            if (DhcpClient.this.mConnSavedAP && !((IpClient) DhcpClient.this.mController).isDhcpDiscoveryForced()) {
                DhcpClient.this.logd("connect to saved AP, transitionTo mDhcpRequestingState directly");
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRequestingState);
            }
        }

        protected boolean sendPacket() {
            return DhcpClient.this.sendDiscoverPacket();
        }

        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet) && (packet instanceof DhcpOfferPacket)) {
                DhcpClient.this.mOffer = packet.toDhcpResults();
                if (DhcpClient.this.isInvalidIpAddr(DhcpClient.this.mOffer)) {
                    DhcpClient.this.notifyInvalidDhcpOfferRcvd(DhcpClient.this.mContext, DhcpClient.this.mOffer);
                    return;
                }
                if (DhcpClient.this.mOffer != null) {
                    String str = DhcpClient.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Got pending lease: ");
                    stringBuilder.append(DhcpClient.this.mOffer);
                    Log.d(str, stringBuilder.toString());
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRequestingState);
                }
            }
        }

        protected void tryCachedIp() {
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_TRY_CACHED_IP);
        }
    }

    abstract class DhcpReacquiringState extends PacketRetransmittingState {
        protected String mLeaseMsg;

        protected abstract Inet4Address packetDestination();

        DhcpReacquiringState() {
            super();
        }

        public void enter() {
            super.enter();
            DhcpClient.this.startNewTransaction();
        }

        protected boolean sendPacket() {
            return DhcpClient.this.sendRequestPacket((Inet4Address) DhcpClient.this.mDhcpLease.ipAddress.getAddress(), DhcpPacket.INADDR_ANY, null, packetDestination());
        }

        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet)) {
                if (packet instanceof DhcpAckPacket) {
                    DhcpResults results = packet.toDhcpResults();
                    if (results != null) {
                        if (!DhcpClient.this.mDhcpLease.ipAddress.equals(results.ipAddress)) {
                            Log.d(DhcpClient.TAG, "Renewed lease not for our current IP address!");
                            DhcpClient.this.notifyFailure();
                            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                        }
                        DhcpClient.this.setDhcpLeaseExpiry(packet);
                        DhcpClient.this.acceptDhcpResults(results, this.mLeaseMsg);
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                    }
                } else if (packet instanceof DhcpNakPacket) {
                    Log.d(DhcpClient.TAG, "Received NAK, returning to INIT");
                    DhcpClient.this.notifyFailure();
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                }
            }
        }
    }

    class DhcpRequestingState extends PacketRetransmittingState {
        public DhcpRequestingState() {
            super();
            this.mTimeout = 9000;
        }

        protected boolean sendPacket() {
            if (DhcpClient.this.mConnSavedAP) {
                DhcpClient.this.mOffer = new DhcpResults();
                DhcpClient.this.mOffer.ipAddress = DhcpClient.this.mPendingIpAddr;
                DhcpClient.this.mConnSavedAP = false;
                DhcpClient.this.mPendingIpAddr = null;
                DhcpClient.this.mPendingDHCPServer = null;
                DhcpClient.this.mDhcpResultsInfo = null;
            }
            return DhcpClient.this.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) DhcpClient.this.mOffer.ipAddress.getAddress(), DhcpClient.this.mOffer.serverAddress, DhcpPacket.INADDR_BROADCAST);
        }

        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet)) {
                if (packet instanceof DhcpAckPacket) {
                    DhcpResults results = packet.toDhcpResults();
                    if (results != null) {
                        DhcpClient.this.setDhcpLeaseExpiry(packet);
                        DhcpClient.this.acceptDhcpResults(results, "Confirmed");
                        Log.d(DhcpClient.TAG, "Received ACK, returning to fast ARP checking");
                        DhcpClient.this.transitionTo(DhcpClient.this.mFastArpCheckingState);
                    }
                } else if (packet instanceof DhcpNakPacket) {
                    Log.d(DhcpClient.TAG, "Received NAK, returning to INIT");
                    DhcpClient.this.mOffer = null;
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                }
            }
        }

        protected void timeout() {
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
        }
    }

    class FastArpCheckingState extends PacketRetransmittingState {
        public FastArpCheckingState() {
            super();
            this.mTimeout = 2000;
        }

        public void enter() {
            maybeInitTimeout();
            sendPacket();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != DhcpClient.CMD_TIMEOUT) {
                String str;
                StringBuilder stringBuilder;
                switch (i) {
                    case DhcpClient.CMD_FAST_ARP_EXIT /*196715*/:
                        if (DhcpClient.this.mFastArpUuidStr.equals(message.obj)) {
                            DhcpClient.this.transitionTo(DhcpClient.this.mDeclineState);
                            return true;
                        }
                        str = DhcpClient.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("FAST_ARP_EXIT FastArpThreadUuid Error! ArpUuid: ");
                        stringBuilder.append(DhcpClient.this.mFastArpUuidStr);
                        stringBuilder.append("ArpUuidThread: ");
                        stringBuilder.append(message.obj);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    case DhcpClient.CMD_FAST_ARP_NOT_EXIT /*196716*/:
                        if (DhcpClient.this.mFastArpUuidStr.equals(message.obj)) {
                            DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
                            return true;
                        }
                        str = DhcpClient.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("FAST_ARP_NOT_EXIT FastArpThreadUuid Error! ArpUuid: ");
                        stringBuilder.append(DhcpClient.this.mFastArpUuidStr);
                        stringBuilder.append("ArpUuidThread: ");
                        stringBuilder.append(message.obj);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    default:
                        return false;
                }
            }
            timeout();
            return true;
        }

        protected boolean sendPacket() {
            DhcpClient.this.mFastArpUuidStr = UUID.randomUUID().toString();
            if (DhcpClient.this.mDeclinedTimes >= 3) {
                DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
                return true;
            }
            new Thread(new Runnable() {
                final String fastArpThreadUuidStr = DhcpClient.this.mFastArpUuidStr;

                public void run() {
                    try {
                        if (DhcpClient.this.mArpClient.doFastArpTest((Inet4Address) DhcpClient.this.mDhcpLease.ipAddress.getAddress())) {
                            Log.d(DhcpClient.TAG, "Received ARP response, send fast arp exit message");
                            DhcpClient.this.sendMessage(DhcpClient.CMD_FAST_ARP_EXIT, this.fastArpThreadUuidStr);
                            return;
                        }
                        Log.d(DhcpClient.TAG, "Not received ARP response, send fast arp not exit message");
                        DhcpClient.this.sendMessage(DhcpClient.CMD_FAST_ARP_NOT_EXIT, this.fastArpThreadUuidStr);
                    } catch (Exception e) {
                        String str = DhcpClient.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to sendFastArpPacket");
                        stringBuilder.append(e.toString());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }).start();
            return true;
        }

        protected void receivePacket(DhcpPacket packet) {
        }

        protected void timeout() {
            Log.d(DhcpClient.TAG, "Returning to config interface");
            DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
        }

        public void exit() {
            DhcpClient.this.mTimeoutAlarm.cancel();
        }
    }

    class SlowArpCheckingState extends PacketRetransmittingState {
        public SlowArpCheckingState() {
            super();
            this.mTimeout = 4000;
        }

        public void enter() {
            maybeInitTimeout();
            sendPacket();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != DhcpClient.CMD_TIMEOUT) {
                String str;
                StringBuilder stringBuilder;
                switch (i) {
                    case DhcpClient.CMD_SLOW_ARP_EXIT /*196717*/:
                        if (DhcpClient.this.mSlowArpUuidStr.equals(message.obj)) {
                            DhcpClient.this.transitionTo(DhcpClient.this.mDeclineState);
                            return true;
                        }
                        str = DhcpClient.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("SLOW_ARP_EXIT SlowArpThreadUuid Error! ArpUuid: ");
                        stringBuilder.append(DhcpClient.this.mSlowArpUuidStr);
                        stringBuilder.append("ArpUuidThread: ");
                        stringBuilder.append(message.obj);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    case DhcpClient.CMD_SLOW_ARP_NOT_EXIT /*196718*/:
                        if (DhcpClient.this.mSlowArpUuidStr.equals(message.obj)) {
                            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                            return true;
                        }
                        str = DhcpClient.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("SLOW_ARP_NOT_EXIT SlowArpThreadUuid Error! ArpUuid: ");
                        stringBuilder.append(DhcpClient.this.mSlowArpUuidStr);
                        stringBuilder.append("ArpUuidThread: ");
                        stringBuilder.append(message.obj);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    default:
                        return false;
                }
            }
            timeout();
            return true;
        }

        protected boolean sendPacket() {
            DhcpClient.this.mSlowArpUuidStr = UUID.randomUUID().toString();
            if (DhcpClient.this.mDeclinedTimes >= 3) {
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                return true;
            }
            new Thread(new Runnable() {
                final String slowArpThreadUuidStr = DhcpClient.this.mSlowArpUuidStr;

                public void run() {
                    try {
                        if (DhcpClient.this.mArpClient.doSlowArpTest((Inet4Address) DhcpClient.this.mDhcpLease.ipAddress.getAddress())) {
                            Log.d(DhcpClient.TAG, "Received ARP response, send slow arp exit message");
                            DhcpClient.this.sendMessage(DhcpClient.CMD_SLOW_ARP_EXIT, this.slowArpThreadUuidStr);
                            return;
                        }
                        Log.d(DhcpClient.TAG, "Not received ARP response, send slow arp not exit message");
                        DhcpClient.this.sendMessage(DhcpClient.CMD_SLOW_ARP_NOT_EXIT, this.slowArpThreadUuidStr);
                    } catch (Exception e) {
                        String str = DhcpClient.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to sendSlowArpPacket");
                        stringBuilder.append(e.toString());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }).start();
            return true;
        }

        protected void receivePacket(DhcpPacket packet) {
        }

        protected void timeout() {
            Log.d(DhcpClient.TAG, "Returning to bound state");
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
        }

        public void exit() {
            DhcpClient.this.mTimeoutAlarm.cancel();
        }
    }

    class WaitBeforeRenewalState extends WaitBeforeOtherState {
        public WaitBeforeRenewalState(State otherState) {
            super();
            this.mOtherState = otherState;
        }
    }

    class WaitBeforeStartState extends WaitBeforeOtherState {
        public WaitBeforeStartState(State otherState) {
            super();
            this.mOtherState = otherState;
        }
    }

    class DhcpRebindingState extends DhcpReacquiringState {
        public DhcpRebindingState() {
            super();
            this.mLeaseMsg = "Rebound";
        }

        public void enter() {
            super.enter();
            DhcpClient.closeQuietly(DhcpClient.this.mUdpSock);
            if (!DhcpClient.this.initUdpSocket()) {
                Log.e(DhcpClient.TAG, "Failed to recreate UDP socket");
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            }
        }

        protected Inet4Address packetDestination() {
            return DhcpPacket.INADDR_BROADCAST;
        }
    }

    class DhcpRenewingState extends DhcpReacquiringState {
        public DhcpRenewingState() {
            super();
            this.mLeaseMsg = "Renewed";
        }

        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            if (message.what != DhcpClient.CMD_REBIND_DHCP) {
                return false;
            }
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRebindingState);
            return true;
        }

        protected Inet4Address packetDestination() {
            return DhcpClient.this.mDhcpLease.serverAddress != null ? DhcpClient.this.mDhcpLease.serverAddress : DhcpPacket.INADDR_BROADCAST;
        }
    }

    private WakeupMessage makeWakeupMessage(String cmdName, int cmd) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DhcpClient.class.getSimpleName());
        stringBuilder.append(".");
        stringBuilder.append(this.mIfaceName);
        stringBuilder.append(".");
        stringBuilder.append(cmdName);
        return new WakeupMessage(this.mContext, getHandler(), stringBuilder.toString(), cmd);
    }

    protected DhcpClient(Context context, StateMachine controller, String iface) {
        super(TAG, controller.getHandler());
        this.mContext = context;
        this.mController = controller;
        this.mIfaceName = iface;
        addState(this.mStoppedState);
        addState(this.mDhcpState);
        addState(this.mDhcpInitState, this.mDhcpState);
        addState(this.mWaitBeforeStartState, this.mDhcpState);
        addState(this.mDhcpSelectingState, this.mDhcpState);
        addState(this.mDhcpRequestingState, this.mDhcpState);
        addState(this.mDhcpHaveLeaseState, this.mDhcpState);
        addState(this.mFastArpCheckingState, this.mDhcpState);
        addState(this.mDeclineState, this.mDhcpState);
        addState(this.mConfiguringInterfaceState, this.mDhcpState);
        addState(this.mSlowArpCheckingState, this.mDhcpState);
        addState(this.mDhcpBoundState, this.mDhcpHaveLeaseState);
        addState(this.mWaitBeforeRenewalState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRenewingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRebindingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpInitRebootState, this.mDhcpState);
        addState(this.mDhcpRebootingState, this.mDhcpState);
        setInitialState(this.mStoppedState);
        this.mRandom = new Random();
        this.mReadDBDone = true;
        this.mKickAlarm = makeWakeupMessage("KICK", CMD_KICK);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", CMD_TIMEOUT);
        this.mRenewAlarm = makeWakeupMessage("RENEW", CMD_RENEW_DHCP);
        this.mRebindAlarm = makeWakeupMessage("REBIND", CMD_REBIND_DHCP);
        this.mExpiryAlarm = makeWakeupMessage("EXPIRY", CMD_EXPIRE_DHCP);
        this.mArpClient = new HwArpClient(this.mContext);
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = true;
    }

    public static DhcpClient makeDhcpClient(Context context, StateMachine controller, InterfaceParams ifParams) {
        DhcpClient client = new DhcpClient(context, controller, ifParams.name);
        client.mIface = ifParams;
        client.start();
        return client;
    }

    private boolean initInterface() {
        if (this.mIface == null) {
            this.mIface = InterfaceParams.getByName(this.mIfaceName);
        }
        if (this.mIface == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't determine InterfaceParams for ");
            stringBuilder.append(this.mIfaceName);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        this.mHwAddr = this.mIface.macAddr.toByteArray();
        this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.index, DhcpPacket.ETHER_BROADCAST);
        return true;
    }

    private void startNewTransaction() {
        this.mTransactionId = this.mRandom.nextInt();
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    private boolean initSockets() {
        return initPacketSocket() && initUdpSocket();
    }

    private boolean initPacketSocket() {
        try {
            this.mPacketSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IP);
            Os.bind(this.mPacketSock, new PacketSocketAddress((short) OsConstants.ETH_P_IP, this.mIface.index));
            NetworkUtils.attachDhcpFilter(this.mPacketSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating packet socket", e);
            return false;
        }
    }

    private boolean initUdpSocket() {
        int oldTag = TrafficStats.getAndSetThreadStatsTag(-192);
        try {
            this.mUdpSock = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            boolean z = true;
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, z);
            Os.setsockoptIfreq(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, this.mIfaceName);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, z);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 0);
            Os.bind(this.mUdpSock, Inet4Address.ANY, 68);
            NetworkUtils.protectFromVpn(this.mUdpSock);
            return z;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating UDP socket", e);
            return false;
        } finally {
            TrafficStats.setThreadStatsTag(oldTag);
        }
    }

    private boolean connectUdpSock(Inet4Address to) {
        try {
            Os.connect(this.mUdpSock, to, 67);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error connecting UDP socket", e);
            return false;
        }
    }

    private static void closeQuietly(FileDescriptor fd) {
        try {
            IoBridge.closeAndSignalBlockedThreads(fd);
        } catch (IOException e) {
        }
    }

    private void closeSockets() {
        closeQuietly(this.mUdpSock);
        closeQuietly(this.mPacketSock);
    }

    private short getSecs() {
        return (short) ((int) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000));
    }

    private boolean transmitPacket(ByteBuffer buf, String description, int encap, Inet4Address to) {
        String str;
        StringBuilder stringBuilder;
        if (encap == 0) {
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Broadcasting ");
                stringBuilder.append(description);
                Log.d(str, stringBuilder.toString());
                Os.sendto(this.mPacketSock, buf.array(), 0, buf.limit(), 0, this.mInterfaceBroadcastAddr);
            } catch (ErrnoException | IOException e) {
                Log.e(TAG, "Can't send packet: ", e);
                return false;
            }
        } else if (encap == 2 && to.equals(DhcpPacket.INADDR_BROADCAST)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting ");
            stringBuilder.append(description);
            Log.d(str, stringBuilder.toString());
            Os.sendto(this.mUdpSock, buf, 0, to, 67);
        } else {
            Log.d(TAG, String.format("Unicasting %s to %s", new Object[]{description, Os.getpeername(this.mUdpSock)}));
            Os.write(this.mUdpSock, buf);
        }
        return true;
    }

    private boolean sendDiscoverPacket() {
        return transmitPacket(DhcpPacket.buildDiscoverPacket(null, this.mTransactionId, getSecs(), this.mHwAddr, false, REQUESTED_PARAMS), "DHCPDISCOVER", 0, DhcpPacket.INADDR_BROADCAST);
    }

    private boolean sendRequestPacket(Inet4Address clientAddress, Inet4Address requestedAddress, Inet4Address serverAddress, Inet4Address to) {
        int encap = DhcpPacket.INADDR_ANY.equals(clientAddress) ? 0 : 2;
        ByteBuffer packet = DhcpPacket.buildRequestPacket(encap, this.mTransactionId, getSecs(), clientAddress, false, this.mHwAddr, requestedAddress, serverAddress, REQUESTED_PARAMS, null);
        String serverStr = serverAddress != null ? serverAddress.getHostAddress() : null;
        String description = new StringBuilder();
        description.append("DHCPREQUEST ciaddr=");
        description.append(clientAddress.getHostAddress());
        description.append(" request=");
        description.append("xxx.xxx.xxx.xxx");
        description.append(" serverid=");
        description.append(serverStr);
        return transmitPacket(packet, description.toString(), encap, to);
    }

    private boolean sendDeclinePacket(Inet4Address clientAddress, Inet4Address requestedAddress, Inet4Address serverAddress, Inet4Address to) {
        int encap = DhcpPacket.INADDR_ANY.equals(clientAddress) ? 0 : 2;
        ByteBuffer packet = DhcpPacket.buildDeclinePacket(encap, this.mTransactionId, getSecs(), clientAddress, false, this.mHwAddr, requestedAddress, serverAddress, REQUESTED_PARAMS, null);
        String serverStr = serverAddress != null ? serverAddress.getHostAddress() : null;
        String description = new StringBuilder();
        description.append("DHCPDECLINE clientaddr=");
        description.append(clientAddress.getHostAddress());
        description.append(" request=");
        description.append("xxx.xxx.xxx.xxx");
        description.append(" serverid=");
        description.append(serverStr);
        return transmitPacket(packet, description.toString(), encap, to);
    }

    private void scheduleLeaseTimers() {
        if (this.mDhcpLeaseExpiry == 0) {
            Log.d(TAG, "Infinite lease, no timer scheduling needed");
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long remainingDelay = this.mDhcpLeaseExpiry - now;
        long renewDelay = remainingDelay / 2;
        long rebindDelay = (7 * remainingDelay) / 8;
        this.mRenewAlarm.schedule(now + renewDelay);
        this.mRebindAlarm.schedule(now + rebindDelay);
        this.mExpiryAlarm.schedule(now + remainingDelay);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduling renewal in ");
        stringBuilder.append(renewDelay / 1000);
        stringBuilder.append("s");
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduling rebind in ");
        stringBuilder.append(rebindDelay / 1000);
        stringBuilder.append("s");
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduling expiry in ");
        stringBuilder.append(remainingDelay / 1000);
        stringBuilder.append("s");
        Log.d(str, stringBuilder.toString());
    }

    private void notifySuccess() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 1, this.mDhcpAction, new DhcpResults(this.mDhcpLease));
    }

    private void notifyFailure() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 2, this.mDhcpAction, null);
        removeDhcpResultsInfoCache();
    }

    private void acceptDhcpResults(DhcpResults results, String msg) {
        this.mDhcpLease = results;
        try {
            if (this.mDhcpLease.dnsServers.size() == 0) {
                Log.d(TAG, "Add default dns");
                this.mDhcpLease.addDns("8.8.8.8");
                this.mDhcpLease.addDns("8.8.4.4");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        this.mOffer = null;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(" lease: ");
        stringBuilder.append(this.mDhcpLease);
        Log.d(str, stringBuilder.toString());
        notifySuccess();
    }

    private void clearDhcpState() {
        this.mDhcpLease = null;
        this.mDhcpLeaseExpiry = 0;
        this.mOffer = null;
    }

    public void doQuit() {
        Log.d(TAG, "doQuit");
        quit();
    }

    protected void onQuitting() {
        Log.d(TAG, "onQuitting");
        this.mController.sendMessage(CMD_ON_QUIT);
    }

    public boolean isValidPacket(DhcpPacket packet) {
        if (packet.getTransactionId() != this.mTransactionId) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Received packet: ");
        stringBuilder.append(packet);
        Log.d(str, stringBuilder.toString());
        if (Arrays.equals(packet.getClientMac(), this.mHwAddr)) {
            return true;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("MAC addr mismatch: got ");
        stringBuilder.append(HexDump.toHexString(packet.getClientMac()));
        stringBuilder.append(", expected ");
        stringBuilder.append(HexDump.toHexString(packet.getClientMac()));
        Log.d(str, stringBuilder.toString());
        return false;
    }

    public void setDhcpLeaseExpiry(DhcpPacket packet) {
        long leaseTimeMillis = packet.getLeaseTimeMillis();
        long j = 0;
        if (leaseTimeMillis > 0) {
            j = SystemClock.elapsedRealtime() + leaseTimeMillis;
        }
        this.mDhcpLeaseExpiry = j;
    }

    private void logError(int errorCode) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpErrorEvent(errorCode));
    }

    private void logState(String name, int durationMs) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpClientEvent(name, durationMs));
    }

    public static String getDhcpError() {
        return mDhcpError;
    }

    private void calcDhcpOfferCnt(DhcpPacket dhcpPacket) {
        if (dhcpPacket != null && (dhcpPacket instanceof DhcpOfferPacket)) {
            this.mDhcpOfferCnt++;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dhcpoffer count is ");
            stringBuilder.append(this.mDhcpOfferCnt);
            Log.d(str, stringBuilder.toString());
            if (this.mDhcpOfferCnt == 1) {
                DhcpResults results = dhcpPacket.toDhcpResults();
                if (results != null) {
                    updateDhcpResultsInfoCache(results);
                }
            } else if (this.mDhcpOfferCnt >= 2) {
                removeDhcpResultsInfoCache();
                logd("multi gates, not save dhcpResultsInfo");
            }
        }
    }
}
