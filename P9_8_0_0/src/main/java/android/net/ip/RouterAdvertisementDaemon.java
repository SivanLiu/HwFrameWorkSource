package android.net.ip;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.util.NetworkConstants;
import android.system.Os;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoBridge;

public class RouterAdvertisementDaemon {
    private static final byte[] ALL_NODES = new byte[]{(byte) -1, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1};
    private static final int DAY_IN_SECONDS = 86400;
    private static final int DEFAULT_LIFETIME = 3600;
    private static final byte ICMPV6_ND_ROUTER_ADVERT = asByte(NetworkConstants.ICMPV6_ROUTER_ADVERTISEMENT);
    private static final byte ICMPV6_ND_ROUTER_SOLICIT = asByte(NetworkConstants.ICMPV6_ROUTER_SOLICITATION);
    private static final int MAX_RTR_ADV_INTERVAL_SEC = 600;
    private static final int MAX_URGENT_RTR_ADVERTISEMENTS = 5;
    private static final int MIN_DELAY_BETWEEN_RAS_SEC = 3;
    private static final int MIN_RA_HEADER_SIZE = 16;
    private static final int MIN_RTR_ADV_INTERVAL_SEC = 300;
    private static final String TAG = RouterAdvertisementDaemon.class.getSimpleName();
    private final InetSocketAddress mAllNodes;
    @GuardedBy("mLock")
    private final DeprecatedInfoTracker mDeprecatedInfoTracker;
    private final byte[] mHwAddr;
    private final int mIfIndex;
    private final String mIfName;
    private final Object mLock = new Object();
    private volatile MulticastTransmitter mMulticastTransmitter;
    @GuardedBy("mLock")
    private final byte[] mRA = new byte[NetworkConstants.IPV6_MIN_MTU];
    @GuardedBy("mLock")
    private int mRaLength;
    @GuardedBy("mLock")
    private RaParams mRaParams;
    private volatile FileDescriptor mSocket;
    private volatile UnicastResponder mUnicastResponder;

    private static class DeprecatedInfoTracker {
        private final HashMap<Inet6Address, Integer> mDnses;
        private final HashMap<IpPrefix, Integer> mPrefixes;

        private DeprecatedInfoTracker() {
            this.mPrefixes = new HashMap();
            this.mDnses = new HashMap();
        }

        Set<IpPrefix> getPrefixes() {
            return this.mPrefixes.keySet();
        }

        void putPrefixes(Set<IpPrefix> prefixes) {
            for (IpPrefix ipp : prefixes) {
                this.mPrefixes.put(ipp, Integer.valueOf(5));
            }
        }

        void removePrefixes(Set<IpPrefix> prefixes) {
            for (IpPrefix ipp : prefixes) {
                this.mPrefixes.remove(ipp);
            }
        }

        Set<Inet6Address> getDnses() {
            return this.mDnses.keySet();
        }

        void putDnses(Set<Inet6Address> dnses) {
            for (Inet6Address dns : dnses) {
                this.mDnses.put(dns, Integer.valueOf(5));
            }
        }

        void removeDnses(Set<Inet6Address> dnses) {
            for (Inet6Address dns : dnses) {
                this.mDnses.remove(dns);
            }
        }

        boolean isEmpty() {
            return this.mPrefixes.isEmpty() ? this.mDnses.isEmpty() : false;
        }

        private boolean decrementCounters() {
            return decrementCounter(this.mPrefixes) | decrementCounter(this.mDnses);
        }

        private <T> boolean decrementCounter(HashMap<T, Integer> map) {
            boolean removed = false;
            Iterator<Entry<T, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<T, Integer> kv = (Entry) it.next();
                if (((Integer) kv.getValue()).intValue() == 0) {
                    it.remove();
                    removed = true;
                } else {
                    kv.setValue(Integer.valueOf(((Integer) kv.getValue()).intValue() - 1));
                }
            }
            return removed;
        }
    }

    private final class MulticastTransmitter extends Thread {
        private final Random mRandom;
        private final AtomicInteger mUrgentAnnouncements;

        private MulticastTransmitter() {
            this.mRandom = new Random();
            this.mUrgentAnnouncements = new AtomicInteger(0);
        }

        public void run() {
            while (RouterAdvertisementDaemon.this.isSocketValid()) {
                try {
                    Thread.sleep(getNextMulticastTransmitDelayMs());
                } catch (InterruptedException e) {
                }
                RouterAdvertisementDaemon.this.maybeSendRA(RouterAdvertisementDaemon.this.mAllNodes);
                synchronized (RouterAdvertisementDaemon.this.mLock) {
                    if (RouterAdvertisementDaemon.this.mDeprecatedInfoTracker.decrementCounters()) {
                        RouterAdvertisementDaemon.this.assembleRaLocked();
                    }
                }
            }
        }

        public void hup() {
            this.mUrgentAnnouncements.set(4);
            interrupt();
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int getNextMulticastTransmitDelaySec() {
            synchronized (RouterAdvertisementDaemon.this.mLock) {
                if (RouterAdvertisementDaemon.this.mRaLength < 16) {
                    return RouterAdvertisementDaemon.DAY_IN_SECONDS;
                }
                boolean deprecationInProgress = RouterAdvertisementDaemon.this.mDeprecatedInfoTracker.isEmpty() ^ 1;
            }
        }

        private long getNextMulticastTransmitDelayMs() {
            return ((long) getNextMulticastTransmitDelaySec()) * 1000;
        }
    }

    public static class RaParams {
        public HashSet<Inet6Address> dnses;
        public boolean hasDefaultRoute;
        public int mtu;
        public HashSet<IpPrefix> prefixes;

        public RaParams() {
            this.hasDefaultRoute = false;
            this.mtu = NetworkConstants.IPV6_MIN_MTU;
            this.prefixes = new HashSet();
            this.dnses = new HashSet();
        }

        public RaParams(RaParams other) {
            this.hasDefaultRoute = other.hasDefaultRoute;
            this.mtu = other.mtu;
            this.prefixes = (HashSet) other.prefixes.clone();
            this.dnses = (HashSet) other.dnses.clone();
        }

        public static RaParams getDeprecatedRaParams(RaParams oldRa, RaParams newRa) {
            RaParams newlyDeprecated = new RaParams();
            if (oldRa != null) {
                for (IpPrefix ipp : oldRa.prefixes) {
                    if (newRa == null || (newRa.prefixes.contains(ipp) ^ 1) != 0) {
                        newlyDeprecated.prefixes.add(ipp);
                    }
                }
                for (Inet6Address dns : oldRa.dnses) {
                    if (newRa == null || (newRa.dnses.contains(dns) ^ 1) != 0) {
                        newlyDeprecated.dnses.add(dns);
                    }
                }
            }
            return newlyDeprecated;
        }
    }

    private final class UnicastResponder extends Thread {
        private final byte[] mSolication;
        private final InetSocketAddress solicitor;

        private UnicastResponder() {
            this.solicitor = new InetSocketAddress();
            this.mSolication = new byte[NetworkConstants.IPV6_MIN_MTU];
        }

        public void run() {
            while (RouterAdvertisementDaemon.this.isSocketValid()) {
                try {
                    if (Os.recvfrom(RouterAdvertisementDaemon.this.mSocket, this.mSolication, 0, this.mSolication.length, 0, this.solicitor) >= 1 && this.mSolication[0] == RouterAdvertisementDaemon.ICMPV6_ND_ROUTER_SOLICIT) {
                        RouterAdvertisementDaemon.this.maybeSendRA(this.solicitor);
                    }
                } catch (Exception e) {
                    if (RouterAdvertisementDaemon.this.isSocketValid()) {
                        Log.e(RouterAdvertisementDaemon.TAG, "recvfrom error: " + e);
                    }
                }
            }
        }
    }

    private boolean createSocket() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:12:?
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.modifyBlocksTree(BlockProcessor.java:248)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r8 = this;
        r0 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r3 = -248; // 0xffffffffffffff08 float:NaN double:NaN;
        r2 = android.net.TrafficStats.getAndSetThreadStatsTag(r3);
        r3 = android.system.OsConstants.AF_INET6;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = android.system.OsConstants.SOCK_RAW;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r5 = android.system.OsConstants.IPPROTO_ICMPV6;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = android.system.Os.socket(r3, r4, r5);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r8.mSocket = r3;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = r8.mSocket;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = android.system.OsConstants.SOL_SOCKET;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r5 = android.system.OsConstants.SO_SNDTIMEO;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r6 = 300; // 0x12c float:4.2E-43 double:1.48E-321;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r6 = android.system.StructTimeval.fromMillis(r6);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.system.Os.setsockoptTimeval(r3, r4, r5, r6);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = r8.mSocket;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = android.system.OsConstants.SOL_SOCKET;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r5 = android.system.OsConstants.SO_BINDTODEVICE;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r6 = r8.mIfName;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.system.Os.setsockoptIfreq(r3, r4, r5, r6);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = r8.mSocket;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.net.NetworkUtils.protectFromVpn(r3);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = r8.mSocket;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = r8.mIfIndex;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.net.NetworkUtils.setupRaSocket(r3, r4);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.net.TrafficStats.setThreadStatsTag(r2);
        r3 = 1;
        return r3;
    L_0x003f:
        r1 = move-exception;
        r3 = TAG;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = new java.lang.StringBuilder;	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4.<init>();	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r5 = "Failed to create RA daemon socket: ";	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = r4.append(r5);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = r4.append(r1);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r4 = r4.toString();	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        android.util.Log.e(r3, r4);	 Catch:{ ErrnoException -> 0x003f, ErrnoException -> 0x003f, all -> 0x005e }
        r3 = 0;
        android.net.TrafficStats.setThreadStatsTag(r2);
        return r3;
    L_0x005e:
        r3 = move-exception;
        android.net.TrafficStats.setThreadStatsTag(r2);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.net.ip.RouterAdvertisementDaemon.createSocket():boolean");
    }

    public RouterAdvertisementDaemon(String ifname, int ifindex, byte[] hwaddr) {
        this.mIfName = ifname;
        this.mIfIndex = ifindex;
        this.mHwAddr = hwaddr;
        this.mAllNodes = new InetSocketAddress(getAllNodesForScopeId(this.mIfIndex), 0);
        this.mDeprecatedInfoTracker = new DeprecatedInfoTracker();
    }

    public void buildNewRa(RaParams deprecatedParams, RaParams newParams) {
        synchronized (this.mLock) {
            if (deprecatedParams != null) {
                this.mDeprecatedInfoTracker.putPrefixes(deprecatedParams.prefixes);
                this.mDeprecatedInfoTracker.putDnses(deprecatedParams.dnses);
            }
            if (newParams != null) {
                this.mDeprecatedInfoTracker.removePrefixes(newParams.prefixes);
                this.mDeprecatedInfoTracker.removeDnses(newParams.dnses);
            }
            this.mRaParams = newParams;
            assembleRaLocked();
        }
        maybeNotifyMulticastTransmitter();
    }

    public boolean start() {
        if (!createSocket()) {
            return false;
        }
        this.mMulticastTransmitter = new MulticastTransmitter();
        this.mMulticastTransmitter.start();
        this.mUnicastResponder = new UnicastResponder();
        this.mUnicastResponder.start();
        return true;
    }

    public void stop() {
        closeSocket();
        this.mMulticastTransmitter = null;
        this.mUnicastResponder = null;
    }

    private void assembleRaLocked() {
        ByteBuffer ra = ByteBuffer.wrap(this.mRA);
        ra.order(ByteOrder.BIG_ENDIAN);
        boolean shouldSendRA = false;
        try {
            boolean z;
            if (this.mRaParams != null) {
                z = this.mRaParams.hasDefaultRoute;
            } else {
                z = false;
            }
            putHeader(ra, z);
            putSlla(ra, this.mHwAddr);
            this.mRaLength = ra.position();
            if (this.mRaParams != null) {
                putMtu(ra, this.mRaParams.mtu);
                this.mRaLength = ra.position();
                for (IpPrefix ipp : this.mRaParams.prefixes) {
                    putPio(ra, ipp, DEFAULT_LIFETIME, DEFAULT_LIFETIME);
                    this.mRaLength = ra.position();
                    shouldSendRA = true;
                }
                if (this.mRaParams.dnses.size() > 0) {
                    putRdnss(ra, this.mRaParams.dnses, DEFAULT_LIFETIME);
                    this.mRaLength = ra.position();
                    shouldSendRA = true;
                }
            }
            for (IpPrefix ipp2 : this.mDeprecatedInfoTracker.getPrefixes()) {
                putPio(ra, ipp2, 0, 0);
                this.mRaLength = ra.position();
                shouldSendRA = true;
            }
            Set<Inet6Address> deprecatedDnses = this.mDeprecatedInfoTracker.getDnses();
            if (!deprecatedDnses.isEmpty()) {
                putRdnss(ra, deprecatedDnses, 0);
                this.mRaLength = ra.position();
                shouldSendRA = true;
            }
        } catch (BufferOverflowException e) {
            Log.e(TAG, "Could not construct new RA: " + e);
        }
        if (!shouldSendRA) {
            this.mRaLength = 0;
        }
    }

    private void maybeNotifyMulticastTransmitter() {
        MulticastTransmitter m = this.mMulticastTransmitter;
        if (m != null) {
            m.hup();
        }
    }

    private static Inet6Address getAllNodesForScopeId(int scopeId) {
        try {
            return Inet6Address.getByAddress("ff02::1", ALL_NODES, scopeId);
        } catch (UnknownHostException uhe) {
            Log.wtf(TAG, "Failed to construct ff02::1 InetAddress: " + uhe);
            return null;
        }
    }

    private static byte asByte(int value) {
        return (byte) value;
    }

    private static short asShort(int value) {
        return (short) value;
    }

    private static void putHeader(ByteBuffer ra, boolean hasDefaultRoute) {
        byte asByte;
        short asShort;
        ByteBuffer put = ra.put(ICMPV6_ND_ROUTER_ADVERT).put(asByte(0)).putShort(asShort(0)).put((byte) 64);
        if (hasDefaultRoute) {
            asByte = asByte(8);
        } else {
            asByte = asByte(0);
        }
        put = put.put(asByte);
        if (hasDefaultRoute) {
            asShort = asShort(DEFAULT_LIFETIME);
        } else {
            asShort = asShort(0);
        }
        put.putShort(asShort).putInt(0).putInt(0);
    }

    private static void putSlla(ByteBuffer ra, byte[] slla) {
        if (slla != null && slla.length == 6) {
            ra.put((byte) 1).put((byte) 1).put(slla);
        }
    }

    private static void putExpandedFlagsOption(ByteBuffer ra) {
        ra.put((byte) 26).put((byte) 1).putShort(asShort(0)).putInt(0);
    }

    private static void putMtu(ByteBuffer ra, int mtu) {
        ByteBuffer putShort = ra.put((byte) 5).put((byte) 1).putShort(asShort(0));
        if (mtu < NetworkConstants.IPV6_MIN_MTU) {
            mtu = NetworkConstants.IPV6_MIN_MTU;
        }
        putShort.putInt(mtu);
    }

    private static void putPio(ByteBuffer ra, IpPrefix ipp, int validTime, int preferredTime) {
        int prefixLength = ipp.getPrefixLength();
        if (prefixLength == 64) {
            if (validTime < 0) {
                validTime = 0;
            }
            if (preferredTime < 0) {
                preferredTime = 0;
            }
            if (preferredTime > validTime) {
                preferredTime = validTime;
            }
            ra.put((byte) 3).put((byte) 4).put(asByte(prefixLength)).put(asByte(192)).putInt(validTime).putInt(preferredTime).putInt(0).put(ipp.getAddress().getAddress());
        }
    }

    private static void putRio(ByteBuffer ra, IpPrefix ipp) {
        int prefixLength = ipp.getPrefixLength();
        if (prefixLength <= 64) {
            int i = prefixLength == 0 ? 1 : prefixLength <= 8 ? 2 : 3;
            byte RIO_NUM_8OCTETS = asByte(i);
            byte[] addr = ipp.getAddress().getAddress();
            ra.put((byte) 24).put(RIO_NUM_8OCTETS).put(asByte(prefixLength)).put(asByte(24)).putInt(DEFAULT_LIFETIME);
            if (prefixLength > 0) {
                ra.put(addr, 0, prefixLength <= 64 ? 8 : 16);
            }
        }
    }

    private static void putRdnss(ByteBuffer ra, Set<Inet6Address> dnses, int lifetime) {
        HashSet<Inet6Address> filteredDnses = new HashSet();
        for (Inet6Address dns : dnses) {
            if (new LinkAddress(dns, 64).isGlobalPreferred()) {
                filteredDnses.add(dns);
            }
        }
        if (!filteredDnses.isEmpty()) {
            ra.put((byte) 25).put(asByte((dnses.size() * 2) + 1)).putShort(asShort(0)).putInt(lifetime);
            for (Inet6Address dns2 : filteredDnses) {
                ra.put(dns2.getAddress());
            }
        }
    }

    private void closeSocket() {
        if (this.mSocket != null) {
            try {
                IoBridge.closeAndSignalBlockedThreads(this.mSocket);
            } catch (IOException e) {
            }
        }
        this.mSocket = null;
    }

    private boolean isSocketValid() {
        FileDescriptor s = this.mSocket;
        return s != null ? s.valid() : false;
    }

    private boolean isSuitableDestination(InetSocketAddress dest) {
        boolean z = true;
        if (this.mAllNodes.equals(dest)) {
            return true;
        }
        InetAddress destip = dest.getAddress();
        if (!(destip instanceof Inet6Address) || !destip.isLinkLocalAddress()) {
            z = false;
        } else if (((Inet6Address) destip).getScopeId() != this.mIfIndex) {
            z = false;
        }
        return z;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void maybeSendRA(InetSocketAddress dest) {
        if (dest == null || (isSuitableDestination(dest) ^ 1) != 0) {
            SocketAddress dest2 = this.mAllNodes;
        }
        try {
            synchronized (this.mLock) {
                if (this.mRaLength < 16) {
                    return;
                }
                Os.sendto(this.mSocket, this.mRA, 0, this.mRaLength, 0, dest2);
            }
        } catch (Exception e) {
            if (isSocketValid()) {
                Log.e(TAG, "sendto error: " + e);
            }
        }
    }
}
