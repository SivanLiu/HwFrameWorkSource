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
import com.android.server.devicepolicy.HwLog;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class NetworkDiagnostics {
    private static final String TAG = "NetworkDiagnostics";
    private static final InetAddress TEST_DNS4 = NetworkUtils.numericToInetAddress("8.8.8.8");
    private static final InetAddress TEST_DNS6 = NetworkUtils.numericToInetAddress("2001:4860:4860::8888");
    /* access modifiers changed from: private */
    public final CountDownLatch mCountDownLatch;
    /* access modifiers changed from: private */
    public final long mDeadlineTime;
    private final String mDescription;
    private final Map<InetAddress, Measurement> mDnsUdpChecks = new HashMap();
    private final Map<Pair<InetAddress, InetAddress>, Measurement> mExplicitSourceIcmpChecks = new HashMap();
    private final Map<InetAddress, Measurement> mIcmpChecks = new HashMap();
    /* access modifiers changed from: private */
    public final Integer mInterfaceIndex;
    private final LinkProperties mLinkProperties;
    /* access modifiers changed from: private */
    public final Network mNetwork;
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

    /* access modifiers changed from: private */
    public static final long now() {
        return SystemClock.elapsedRealtime();
    }

    public class Measurement {
        private static final String FAILED = "FAILED";
        private static final String SUCCEEDED = "SUCCEEDED";
        String description = "";
        long finishTime;
        String result = "";
        long startTime;
        private boolean succeeded;
        Thread thread;

        public Measurement() {
        }

        public boolean checkSucceeded() {
            return this.succeeded;
        }

        /* access modifiers changed from: package-private */
        public void recordSuccess(String msg) {
            maybeFixupTimes();
            this.succeeded = true;
            this.result = "SUCCEEDED: " + msg;
            if (NetworkDiagnostics.this.mCountDownLatch != null) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
            }
        }

        /* access modifiers changed from: package-private */
        public void recordFailure(String msg) {
            maybeFixupTimes();
            this.succeeded = false;
            this.result = "FAILED: " + msg;
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
            return this.description + ": " + this.result + " (" + (this.finishTime - this.startTime) + "ms)";
        }
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
        if (this.mLinkProperties.hasGlobalIpv6Address() || this.mLinkProperties.hasIpv6DefaultRoute()) {
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
        this.mDescription = "ifaces{" + TextUtils.join(",", this.mLinkProperties.getAllInterfaceNames()) + "} index{" + this.mInterfaceIndex + "} network{" + this.mNetwork + "} nethandle{" + this.mNetwork.getNetworkHandle() + "}";
    }

    private static Integer getInterfaceIndex(String ifname) {
        try {
            return Integer.valueOf(NetworkInterface.getByName(ifname).getIndex());
        } catch (NullPointerException | SocketException e) {
            return null;
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
                Pair<InetAddress, InetAddress> srcTarget = new Pair<>(source, target);
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
        return this.mIcmpChecks.size() + this.mExplicitSourceIcmpChecks.size() + this.mDnsUdpChecks.size();
    }

    private void startMeasurements() {
        for (Measurement measurement : this.mIcmpChecks.values()) {
            measurement.thread.start();
        }
        for (Measurement measurement2 : this.mExplicitSourceIcmpChecks.values()) {
            measurement2.thread.start();
        }
        for (Measurement measurement3 : this.mDnsUdpChecks.values()) {
            measurement3.thread.start();
        }
    }

    public void waitForMeasurements() {
        try {
            this.mCountDownLatch.await(this.mDeadlineTime - now(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    public List<Measurement> getMeasurements() {
        ArrayList<Measurement> measurements = new ArrayList<>(totalMeasurementCount());
        for (Map.Entry<InetAddress, Measurement> entry : this.mIcmpChecks.entrySet()) {
            if (entry.getKey() instanceof Inet4Address) {
                measurements.add(entry.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry2 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry2.getKey().first instanceof Inet4Address) {
                measurements.add(entry2.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry3 : this.mDnsUdpChecks.entrySet()) {
            if (entry3.getKey() instanceof Inet4Address) {
                measurements.add(entry3.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry4 : this.mIcmpChecks.entrySet()) {
            if (entry4.getKey() instanceof Inet6Address) {
                measurements.add(entry4.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry5 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry5.getKey().first instanceof Inet6Address) {
                measurements.add(entry5.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry6 : this.mDnsUdpChecks.entrySet()) {
            if (entry6.getKey() instanceof Inet6Address) {
                measurements.add(entry6.getValue());
            }
        }
        return measurements;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("NetworkDiagnostics:" + this.mDescription);
        long unfinished = this.mCountDownLatch.getCount();
        if (unfinished > 0) {
            pw.println("WARNING: countdown wait incomplete: " + unfinished + " unfinished measurements");
        }
        pw.increaseIndent();
        for (Measurement m : getMeasurements()) {
            String prefix = m.checkSucceeded() ? "." : "F";
            pw.println(prefix + "  " + m.toString());
        }
        pw.decreaseIndent();
    }

    private class SimpleSocketCheck implements Closeable {
        protected final int mAddressFamily;
        protected FileDescriptor mFileDescriptor;
        protected final Measurement mMeasurement;
        protected SocketAddress mSocketAddress;
        protected final InetAddress mSource;
        protected final InetAddress mTarget;

        /* JADX WARNING: Removed duplicated region for block: B:13:0x0037  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0039  */
        protected SimpleSocketCheck(InetAddress source, InetAddress target, Measurement measurement) {
            Inet6Address targetWithScopeId;
            this.mMeasurement = measurement;
            if (target instanceof Inet6Address) {
                if (target.isLinkLocalAddress() && NetworkDiagnostics.this.mInterfaceIndex != null) {
                    try {
                        targetWithScopeId = Inet6Address.getByAddress((String) null, target.getAddress(), NetworkDiagnostics.this.mInterfaceIndex.intValue());
                    } catch (UnknownHostException e) {
                        this.mMeasurement.recordFailure(e.toString());
                    }
                    this.mTarget = targetWithScopeId == null ? targetWithScopeId : target;
                    this.mAddressFamily = OsConstants.AF_INET6;
                }
                targetWithScopeId = null;
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

        /* JADX INFO: finally extract failed */
        /* access modifiers changed from: protected */
        public void setupSocket(int sockType, int protocol, long writeTimeout, long readTimeout, int dstPort) throws ErrnoException, IOException {
            int oldTag = TrafficStats.getAndSetThreadStatsTag(-127);
            try {
                this.mFileDescriptor = Os.socket(this.mAddressFamily, sockType, protocol);
                TrafficStats.setThreadStatsTag(oldTag);
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(writeTimeout));
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(readTimeout));
                NetworkDiagnostics.this.mNetwork.bindSocket(this.mFileDescriptor);
                InetAddress inetAddress = this.mSource;
                if (inetAddress != null) {
                    Os.bind(this.mFileDescriptor, inetAddress, 0);
                }
                Os.connect(this.mFileDescriptor, this.mTarget, dstPort);
                this.mSocketAddress = Os.getsockname(this.mFileDescriptor);
            } catch (Throwable th) {
                TrafficStats.setThreadStatsTag(oldTag);
                throw th;
            }
        }

        /* access modifiers changed from: protected */
        public String getSocketAddressString() {
            InetSocketAddress inetSockAddr = (InetSocketAddress) this.mSocketAddress;
            InetAddress localAddr = inetSockAddr.getAddress();
            return String.format(localAddr instanceof Inet6Address ? "[%s]:%d" : "%s:%d", localAddr.getHostAddress(), Integer.valueOf(inetSockAddr.getPort()));
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            IoUtils.closeQuietly(this.mFileDescriptor);
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
            StringBuilder sb = new StringBuilder();
            Measurement measurement2 = this.mMeasurement;
            sb.append(measurement2.description);
            sb.append(" dst{");
            sb.append(this.mTarget.getHostAddress());
            sb.append("}");
            measurement2.description = sb.toString();
        }

        public IcmpCheck(NetworkDiagnostics networkDiagnostics, InetAddress target, Measurement measurement) {
            this(null, target, measurement);
        }

        public void run() {
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, this.mProtocol, 100, 300, 0);
                StringBuilder sb = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                sb.append(measurement.description);
                sb.append(" src{");
                sb.append("xxx.xxx.xxx.xxx:xxx");
                sb.append("}");
                measurement.description = sb.toString();
                byte[] icmpPacket = {(byte) this.mIcmpType, 0, 0, 0, 0, 0, 0, 0};
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
                            measurement2.recordSuccess("1/" + count);
                            break;
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    Measurement measurement3 = this.mMeasurement;
                    measurement3.recordFailure("0/" + count);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
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
                this.mQueryType = RR_TYPE_AAAA;
            } else {
                this.mQueryType = 1;
            }
            Measurement measurement2 = this.mMeasurement;
            measurement2.description = "DNS UDP dst{" + this.mTarget.getHostAddress() + "}";
        }

        public void run() {
            String rcodeStr;
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP, 100, 500, 53);
                StringBuilder sb = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                sb.append(measurement.description);
                sb.append(" src{");
                sb.append("xxx.xxx.xxx.xxx:xxx");
                sb.append("}");
                measurement.description = sb.toString();
                String sixRandomDigits = String.valueOf(this.mRandom.nextInt(900000) + 100000);
                StringBuilder sb2 = new StringBuilder();
                Measurement measurement2 = this.mMeasurement;
                sb2.append(measurement2.description);
                sb2.append(" qtype{");
                sb2.append(this.mQueryType);
                sb2.append("} qname{");
                sb2.append(sixRandomDigits);
                sb2.append("-android-ds.metric.gstatic.com}");
                measurement2.description = sb2.toString();
                byte[] dnsPacket = getDnsQueryPacket(sixRandomDigits);
                int count = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                while (true) {
                    if (NetworkDiagnostics.now() >= NetworkDiagnostics.this.mDeadlineTime - 1000) {
                        break;
                    }
                    count++;
                    try {
                        Os.write(this.mFileDescriptor, dnsPacket, 0, dnsPacket.length);
                        try {
                            ByteBuffer reply = ByteBuffer.allocate(512);
                            Os.read(this.mFileDescriptor, reply);
                            if (reply.limit() > 3) {
                                rcodeStr = HwLog.PREFIX + responseCodeStr(reply.get(3) & UsbDescriptor.DESCRIPTORTYPE_BOS);
                            } else {
                                rcodeStr = "";
                            }
                            this.mMeasurement.recordSuccess("1/" + count + rcodeStr);
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    this.mMeasurement.recordFailure("0/" + count);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
        }

        private byte[] getDnsQueryPacket(String sixRandomDigits) {
            byte[] rnd = sixRandomDigits.getBytes(StandardCharsets.US_ASCII);
            byte[] bArr = new byte[54];
            bArr[0] = (byte) this.mRandom.nextInt();
            bArr[1] = (byte) this.mRandom.nextInt();
            bArr[2] = 1;
            bArr[3] = 0;
            bArr[4] = 0;
            bArr[5] = 1;
            bArr[6] = 0;
            bArr[7] = 0;
            bArr[8] = 0;
            bArr[9] = 0;
            bArr[10] = 0;
            bArr[11] = 0;
            bArr[12] = 17;
            bArr[13] = rnd[0];
            bArr[14] = rnd[1];
            bArr[15] = rnd[2];
            bArr[16] = rnd[3];
            bArr[17] = rnd[4];
            bArr[18] = rnd[5];
            bArr[19] = 45;
            bArr[20] = 97;
            bArr[21] = 110;
            bArr[22] = 100;
            bArr[23] = 114;
            bArr[24] = 111;
            bArr[25] = 105;
            bArr[26] = 100;
            bArr[27] = 45;
            bArr[RR_TYPE_AAAA] = 100;
            bArr[29] = 115;
            bArr[30] = 6;
            bArr[31] = 109;
            bArr[32] = 101;
            bArr[33] = 116;
            bArr[34] = 114;
            bArr[35] = 105;
            bArr[36] = 99;
            bArr[37] = 7;
            bArr[38] = 103;
            bArr[39] = 115;
            bArr[40] = 116;
            bArr[41] = 97;
            bArr[42] = 116;
            bArr[43] = 105;
            bArr[44] = 99;
            bArr[45] = 3;
            bArr[46] = 99;
            bArr[47] = 111;
            bArr[48] = 109;
            bArr[49] = 0;
            bArr[50] = 0;
            bArr[51] = (byte) this.mQueryType;
            bArr[52] = 0;
            bArr[53] = 1;
            return bArr;
        }
    }
}
