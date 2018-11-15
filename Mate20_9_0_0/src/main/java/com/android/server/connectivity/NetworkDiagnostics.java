package com.android.server.connectivity;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class NetworkDiagnostics {
    private static final String TAG = "NetworkDiagnostics";
    private static final InetAddress TEST_DNS4 = NetworkUtils.numericToInetAddress("8.8.8.8");
    private static final InetAddress TEST_DNS6 = NetworkUtils.numericToInetAddress("2001:4860:4860::8888");
    private final CountDownLatch mCountDownLatch;
    private final long mDeadlineTime;
    private final String mDescription;
    private final Map<InetAddress, Measurement> mDnsUdpChecks = new HashMap();
    private final Map<Pair<InetAddress, InetAddress>, Measurement> mExplicitSourceIcmpChecks = new HashMap();
    private final Map<InetAddress, Measurement> mIcmpChecks = new HashMap();
    private final Integer mInterfaceIndex;
    private final LinkProperties mLinkProperties;
    private final Network mNetwork;
    private final long mStartTime;
    private final long mTimeoutMs;

    public enum DnsResponseCode {
        NOERROR,
        FORMERR,
        SERVFAIL,
        NXDOMAIN,
        NOTIMP,
        REFUSED
    }

    public class Measurement {
        private static final String FAILED = "FAILED";
        private static final String SUCCEEDED = "SUCCEEDED";
        String description = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        long finishTime;
        String result = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        long startTime;
        private boolean succeeded;
        Thread thread;

        public boolean checkSucceeded() {
            return this.succeeded;
        }

        void recordSuccess(String msg) {
            maybeFixupTimes();
            this.succeeded = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUCCEEDED: ");
            stringBuilder.append(msg);
            this.result = stringBuilder.toString();
            if (NetworkDiagnostics.this.mCountDownLatch != null) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
            }
        }

        void recordFailure(String msg) {
            maybeFixupTimes();
            this.succeeded = false;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FAILED: ");
            stringBuilder.append(msg);
            this.result = stringBuilder.toString();
            if (NetworkDiagnostics.this.mCountDownLatch != null) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
            }
        }

        private void maybeFixupTimes() {
            if (this.finishTime == 0) {
                this.finishTime = NetworkDiagnostics.now();
            }
            if (this.startTime == 0) {
                this.startTime = this.finishTime;
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.description);
            stringBuilder.append(": ");
            stringBuilder.append(this.result);
            stringBuilder.append(" (");
            stringBuilder.append(this.finishTime - this.startTime);
            stringBuilder.append("ms)");
            return stringBuilder.toString();
        }
    }

    private class SimpleSocketCheck implements Closeable {
        protected final int mAddressFamily;
        protected FileDescriptor mFileDescriptor;
        protected final Measurement mMeasurement;
        protected SocketAddress mSocketAddress;
        protected final InetAddress mSource;
        protected final InetAddress mTarget;

        /* JADX WARNING: Removed duplicated region for block: B:14:0x0039  */
        /* JADX WARNING: Removed duplicated region for block: B:13:0x0037  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected SimpleSocketCheck(InetAddress source, InetAddress target, Measurement measurement) {
            this.mMeasurement = measurement;
            if (target instanceof Inet6Address) {
                InetAddress targetWithScopeId;
                if (target.isLinkLocalAddress() && NetworkDiagnostics.this.mInterfaceIndex != null) {
                    try {
                        targetWithScopeId = Inet6Address.getByAddress(null, target.getAddress(), NetworkDiagnostics.this.mInterfaceIndex.intValue());
                    } catch (UnknownHostException e) {
                        this.mMeasurement.recordFailure(e.toString());
                    }
                    this.mTarget = targetWithScopeId == null ? targetWithScopeId : target;
                    this.mAddressFamily = OsConstants.AF_INET6;
                }
                targetWithScopeId = null;
                if (targetWithScopeId == null) {
                }
                this.mTarget = targetWithScopeId == null ? targetWithScopeId : target;
                this.mAddressFamily = OsConstants.AF_INET6;
            } else {
                this.mTarget = target;
                this.mAddressFamily = OsConstants.AF_INET;
            }
            this.mSource = source;
        }

        protected SimpleSocketCheck(NetworkDiagnostics networkDiagnostics, InetAddress target, Measurement measurement) {
            this(null, target, measurement);
        }

        protected void setupSocket(int sockType, int protocol, long writeTimeout, long readTimeout, int dstPort) throws ErrnoException, IOException {
            int oldTag = TrafficStats.getAndSetThreadStatsTag(-190);
            try {
                this.mFileDescriptor = Os.socket(this.mAddressFamily, sockType, protocol);
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(writeTimeout));
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(readTimeout));
                NetworkDiagnostics.this.mNetwork.bindSocket(this.mFileDescriptor);
                if (this.mSource != null) {
                    Os.bind(this.mFileDescriptor, this.mSource, 0);
                }
                Os.connect(this.mFileDescriptor, this.mTarget, dstPort);
                this.mSocketAddress = Os.getsockname(this.mFileDescriptor);
            } finally {
                TrafficStats.setThreadStatsTag(oldTag);
            }
        }

        protected String getSocketAddressString() {
            return String.format(this.mSocketAddress.getAddress() instanceof Inet6Address ? "[%s]:%d" : "%s:%d", new Object[]{this.mSocketAddress.getAddress().getHostAddress(), Integer.valueOf(this.mSocketAddress.getPort())});
        }

        public void close() {
            IoUtils.closeQuietly(this.mFileDescriptor);
        }
    }

    private class DnsUdpCheck extends SimpleSocketCheck implements Runnable {
        private static final int PACKET_BUFSIZE = 512;
        private static final int RR_TYPE_A = 1;
        private static final int RR_TYPE_AAAA = 28;
        private static final int TIMEOUT_RECV = 500;
        private static final int TIMEOUT_SEND = 100;
        private final int mQueryType;
        private final Random mRandom = new Random();

        private String responseCodeStr(int rcode) {
            try {
                return DnsResponseCode.values()[rcode].toString();
            } catch (IndexOutOfBoundsException e) {
                return String.valueOf(rcode);
            }
        }

        public DnsUdpCheck(InetAddress target, Measurement measurement) {
            super(NetworkDiagnostics.this, target, measurement);
            if (this.mAddressFamily == OsConstants.AF_INET6) {
                this.mQueryType = 28;
            } else {
                this.mQueryType = 1;
            }
            Measurement measurement2 = this.mMeasurement;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DNS UDP dst{");
            stringBuilder.append(this.mTarget.getHostAddress());
            stringBuilder.append("}");
            measurement2.description = stringBuilder.toString();
        }

        /* JADX WARNING: Removed duplicated region for block: B:25:0x0108  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0122 A:{Splitter: B:4:0x0014, ExcHandler: android.system.ErrnoException (r0_6 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x00f5 A:{Splitter: B:10:0x00a1, ExcHandler: android.system.ErrnoException (r6_13 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x00f3 A:{Splitter: B:13:0x00ab, ExcHandler: android.system.ErrnoException (e android.system.ErrnoException)} */
        /* JADX WARNING: Missing block: B:21:0x00f5, code:
            r6 = move-exception;
     */
        /* JADX WARNING: Missing block: B:22:0x00f6, code:
            r12.mMeasurement.recordFailure(r6.toString());
     */
        /* JADX WARNING: Missing block: B:28:0x0122, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:29:0x0123, code:
            r12.mMeasurement.recordFailure(r0.toString());
     */
        /* JADX WARNING: Missing block: B:30:0x012c, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP, 100, 500, 53);
                StringBuilder stringBuilder = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                stringBuilder.append(measurement.description);
                stringBuilder.append(" src{");
                stringBuilder.append("xxx.xxx.xxx.xxx:xxx");
                stringBuilder.append("}");
                measurement.description = stringBuilder.toString();
                String sixRandomDigits = String.valueOf(this.mRandom.nextInt(900000) + 100000);
                StringBuilder stringBuilder2 = new StringBuilder();
                Measurement measurement2 = this.mMeasurement;
                stringBuilder2.append(measurement2.description);
                stringBuilder2.append(" qtype{");
                stringBuilder2.append(this.mQueryType);
                stringBuilder2.append("} qname{");
                stringBuilder2.append(sixRandomDigits);
                stringBuilder2.append("-android-ds.metric.gstatic.com}");
                measurement2.description = stringBuilder2.toString();
                byte[] dnsPacket = getDnsQueryPacket(sixRandomDigits);
                int count = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 1000) {
                    count++;
                    try {
                        Os.write(this.mFileDescriptor, dnsPacket, 0, dnsPacket.length);
                        try {
                            String rcodeStr;
                            ByteBuffer reply = ByteBuffer.allocate(512);
                            Os.read(this.mFileDescriptor, reply);
                            if (reply.limit() > 3) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(" ");
                                stringBuilder3.append(responseCodeStr(reply.get(3) & 15));
                                rcodeStr = stringBuilder3.toString();
                            } else {
                                rcodeStr = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                            }
                            Measurement measurement3 = this.mMeasurement;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("1/");
                            stringBuilder4.append(count);
                            stringBuilder4.append(rcodeStr);
                            measurement3.recordSuccess(stringBuilder4.toString());
                        } catch (ErrnoException e) {
                        }
                    } catch (Exception e2) {
                    }
                    if (this.mMeasurement.finishTime == 0) {
                        Measurement measurement4 = this.mMeasurement;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("0/");
                        stringBuilder5.append(count);
                        measurement4.recordFailure(stringBuilder5.toString());
                    }
                    close();
                }
                if (this.mMeasurement.finishTime == 0) {
                }
                close();
            } catch (Exception e3) {
            }
        }

        private byte[] getDnsQueryPacket(String sixRandomDigits) {
            byte[] rnd = sixRandomDigits.getBytes(StandardCharsets.US_ASCII);
            return new byte[]{(byte) this.mRandom.nextInt(), (byte) this.mRandom.nextInt(), (byte) 1, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 17, rnd[0], rnd[1], rnd[2], rnd[3], rnd[4], rnd[5], (byte) 45, (byte) 97, (byte) 110, (byte) 100, (byte) 114, (byte) 111, (byte) 105, (byte) 100, (byte) 45, (byte) 100, (byte) 115, (byte) 6, (byte) 109, (byte) 101, (byte) 116, (byte) 114, (byte) 105, (byte) 99, (byte) 7, (byte) 103, (byte) 115, (byte) 116, (byte) 97, (byte) 116, (byte) 105, (byte) 99, (byte) 3, (byte) 99, (byte) 111, (byte) 109, (byte) 0, (byte) 0, (byte) this.mQueryType, (byte) 0, (byte) 1};
        }
    }

    private class IcmpCheck extends SimpleSocketCheck implements Runnable {
        private static final int PACKET_BUFSIZE = 512;
        private static final int TIMEOUT_RECV = 300;
        private static final int TIMEOUT_SEND = 100;
        private final int mIcmpType;
        private final int mProtocol;

        public IcmpCheck(InetAddress source, InetAddress target, Measurement measurement) {
            super(source, target, measurement);
            if (this.mAddressFamily == OsConstants.AF_INET6) {
                this.mProtocol = OsConstants.IPPROTO_ICMPV6;
                this.mIcmpType = 128;
                this.mMeasurement.description = "ICMPv6";
            } else {
                this.mProtocol = OsConstants.IPPROTO_ICMP;
                this.mIcmpType = 8;
                this.mMeasurement.description = "ICMPv4";
            }
            StringBuilder stringBuilder = new StringBuilder();
            Measurement measurement2 = this.mMeasurement;
            stringBuilder.append(measurement2.description);
            stringBuilder.append(" dst{");
            stringBuilder.append(this.mTarget.getHostAddress());
            stringBuilder.append("}");
            measurement2.description = stringBuilder.toString();
        }

        public IcmpCheck(NetworkDiagnostics networkDiagnostics, InetAddress target, Measurement measurement) {
            this(null, target, measurement);
        }

        /* JADX WARNING: Removed duplicated region for block: B:24:0x00dd A:{Splitter: B:4:0x0014, ExcHandler: android.system.ErrnoException (r0_6 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x00b0 A:{Splitter: B:10:0x0085, ExcHandler: android.system.ErrnoException (r4_5 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x00ae A:{Splitter: B:13:0x008e, ExcHandler: android.system.ErrnoException (e android.system.ErrnoException)} */
        /* JADX WARNING: Missing block: B:17:0x00b0, code:
            r4 = move-exception;
     */
        /* JADX WARNING: Missing block: B:18:0x00b1, code:
            r13.mMeasurement.recordFailure(r4.toString());
     */
        /* JADX WARNING: Missing block: B:24:0x00dd, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:25:0x00de, code:
            r13.mMeasurement.recordFailure(r0.toString());
     */
        /* JADX WARNING: Missing block: B:26:0x00e7, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, this.mProtocol, 100, 300, 0);
                StringBuilder stringBuilder = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                stringBuilder.append(measurement.description);
                stringBuilder.append(" src{");
                stringBuilder.append("xxx.xxx.xxx.xxx:xxx");
                stringBuilder.append("}");
                measurement.description = stringBuilder.toString();
                byte[] icmpPacket = new byte[]{(byte) this.mIcmpType, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
                int count = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 400) {
                    count++;
                    icmpPacket[icmpPacket.length - 1] = (byte) count;
                    try {
                        Os.write(this.mFileDescriptor, icmpPacket, 0, icmpPacket.length);
                        try {
                            Os.read(this.mFileDescriptor, ByteBuffer.allocate(512));
                            Measurement measurement2 = this.mMeasurement;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("1/");
                            stringBuilder2.append(count);
                            measurement2.recordSuccess(stringBuilder2.toString());
                            break;
                        } catch (ErrnoException e) {
                        }
                    } catch (Exception e2) {
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    Measurement measurement3 = this.mMeasurement;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("0/");
                    stringBuilder3.append(count);
                    measurement3.recordFailure(stringBuilder3.toString());
                }
                close();
            } catch (Exception e3) {
            }
        }
    }

    private static final long now() {
        return SystemClock.elapsedRealtime();
    }

    public NetworkDiagnostics(Network network, LinkProperties lp, long timeoutMs) {
        this.mNetwork = network;
        this.mLinkProperties = lp;
        this.mInterfaceIndex = getInterfaceIndex(this.mLinkProperties.getInterfaceName());
        this.mTimeoutMs = timeoutMs;
        this.mStartTime = now();
        this.mDeadlineTime = this.mStartTime + this.mTimeoutMs;
        if (this.mLinkProperties.isReachable(TEST_DNS4)) {
            this.mLinkProperties.addDnsServer(TEST_DNS4);
        }
        if (this.mLinkProperties.hasGlobalIPv6Address() || this.mLinkProperties.hasIPv6DefaultRoute()) {
            this.mLinkProperties.addDnsServer(TEST_DNS6);
        }
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route.hasGateway()) {
                InetAddress gateway = route.getGateway();
                prepareIcmpMeasurement(gateway);
                if (route.isIPv6Default()) {
                    prepareExplicitSourceIcmpMeasurements(gateway);
                }
            }
        }
        for (InetAddress nameserver : this.mLinkProperties.getDnsServers()) {
            prepareIcmpMeasurement(nameserver);
            prepareDnsMeasurement(nameserver);
        }
        this.mCountDownLatch = new CountDownLatch(totalMeasurementCount());
        startMeasurements();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ifaces{");
        stringBuilder.append(TextUtils.join(",", this.mLinkProperties.getAllInterfaceNames()));
        stringBuilder.append("} index{");
        stringBuilder.append(this.mInterfaceIndex);
        stringBuilder.append("} network{");
        stringBuilder.append(this.mNetwork);
        stringBuilder.append("} nethandle{");
        stringBuilder.append(this.mNetwork.getNetworkHandle());
        stringBuilder.append("}");
        this.mDescription = stringBuilder.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x000d A:{Splitter: B:0:0x0000, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Missing block: B:5:0x000f, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Integer getInterfaceIndex(String ifname) {
        try {
            return Integer.valueOf(NetworkInterface.getByName(ifname).getIndex());
        } catch (NullPointerException e) {
        }
    }

    private void prepareIcmpMeasurement(InetAddress target) {
        if (!this.mIcmpChecks.containsKey(target)) {
            Measurement measurement = new Measurement();
            measurement.thread = new Thread(new IcmpCheck(this, target, measurement));
            this.mIcmpChecks.put(target, measurement);
        }
    }

    private void prepareExplicitSourceIcmpMeasurements(InetAddress target) {
        for (LinkAddress l : this.mLinkProperties.getLinkAddresses()) {
            InetAddress source = l.getAddress();
            if ((source instanceof Inet6Address) && l.isGlobalPreferred()) {
                Pair<InetAddress, InetAddress> srcTarget = new Pair(source, target);
                if (!this.mExplicitSourceIcmpChecks.containsKey(srcTarget)) {
                    Measurement measurement = new Measurement();
                    measurement.thread = new Thread(new IcmpCheck(source, target, measurement));
                    this.mExplicitSourceIcmpChecks.put(srcTarget, measurement);
                }
            }
        }
    }

    private void prepareDnsMeasurement(InetAddress target) {
        if (!this.mDnsUdpChecks.containsKey(target)) {
            Measurement measurement = new Measurement();
            measurement.thread = new Thread(new DnsUdpCheck(target, measurement));
            this.mDnsUdpChecks.put(target, measurement);
        }
    }

    private int totalMeasurementCount() {
        return (this.mIcmpChecks.size() + this.mExplicitSourceIcmpChecks.size()) + this.mDnsUdpChecks.size();
    }

    private void startMeasurements() {
        for (Measurement measurement : this.mIcmpChecks.values()) {
            measurement.thread.start();
        }
        for (Measurement measurement2 : this.mExplicitSourceIcmpChecks.values()) {
            measurement2.thread.start();
        }
        for (Measurement measurement22 : this.mDnsUdpChecks.values()) {
            measurement22.thread.start();
        }
    }

    public void waitForMeasurements() {
        try {
            this.mCountDownLatch.await(this.mDeadlineTime - now(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    public List<Measurement> getMeasurements() {
        ArrayList<Measurement> measurements = new ArrayList(totalMeasurementCount());
        for (Entry<InetAddress, Measurement> entry : this.mIcmpChecks.entrySet()) {
            if (entry.getKey() instanceof Inet4Address) {
                measurements.add((Measurement) entry.getValue());
            }
        }
        for (Entry<Pair<InetAddress, InetAddress>, Measurement> entry2 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (((Pair) entry2.getKey()).first instanceof Inet4Address) {
                measurements.add((Measurement) entry2.getValue());
            }
        }
        for (Entry<InetAddress, Measurement> entry3 : this.mDnsUdpChecks.entrySet()) {
            if (entry3.getKey() instanceof Inet4Address) {
                measurements.add((Measurement) entry3.getValue());
            }
        }
        for (Entry<InetAddress, Measurement> entry32 : this.mIcmpChecks.entrySet()) {
            if (entry32.getKey() instanceof Inet6Address) {
                measurements.add((Measurement) entry32.getValue());
            }
        }
        for (Entry<Pair<InetAddress, InetAddress>, Measurement> entry22 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (((Pair) entry22.getKey()).first instanceof Inet6Address) {
                measurements.add((Measurement) entry22.getValue());
            }
        }
        for (Entry<InetAddress, Measurement> entry322 : this.mDnsUdpChecks.entrySet()) {
            if (entry322.getKey() instanceof Inet6Address) {
                measurements.add((Measurement) entry322.getValue());
            }
        }
        return measurements;
    }

    public void dump(IndentingPrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetworkDiagnostics:");
        stringBuilder.append(this.mDescription);
        pw.println(stringBuilder.toString());
        long unfinished = this.mCountDownLatch.getCount();
        if (unfinished > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("WARNING: countdown wait incomplete: ");
            stringBuilder2.append(unfinished);
            stringBuilder2.append(" unfinished measurements");
            pw.println(stringBuilder2.toString());
        }
        pw.increaseIndent();
        for (Measurement m : getMeasurements()) {
            String prefix = m.checkSucceeded() ? "." : "F";
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("  ");
            stringBuilder3.append(m.toString());
            pw.println(stringBuilder3.toString());
        }
        pw.decreaseIndent();
    }
}
