package android.net.metrics;

import android.net.NetworkCapabilities;
import android.system.OsConstants;
import android.util.IntArray;
import android.util.SparseIntArray;
import com.android.internal.util.BitUtils;
import com.android.internal.util.TokenBucket;

public class ConnectStats {
    private static final int EALREADY = OsConstants.EALREADY;
    private static final int EINPROGRESS = OsConstants.EINPROGRESS;
    public int connectBlockingCount = 0;
    public int connectCount = 0;
    public final SparseIntArray errnos = new SparseIntArray();
    public int eventCount = 0;
    public int ipv6ConnectCount = 0;
    public final IntArray latencies = new IntArray();
    public final TokenBucket mLatencyTb;
    public final int mMaxLatencyRecords;
    public final int netId;
    public final long transports;

    public ConnectStats(int netId, long transports, TokenBucket tb, int maxLatencyRecords) {
        this.netId = netId;
        this.transports = transports;
        this.mLatencyTb = tb;
        this.mMaxLatencyRecords = maxLatencyRecords;
    }

    boolean addEvent(int errno, int latencyMs, String ipAddr) {
        this.eventCount++;
        if (isSuccess(errno)) {
            countConnect(errno, ipAddr);
            countLatency(errno, latencyMs);
            return true;
        }
        countError(errno);
        return false;
    }

    private void countConnect(int errno, String ipAddr) {
        this.connectCount++;
        if (!isNonBlocking(errno)) {
            this.connectBlockingCount++;
        }
        if (isIPv6(ipAddr)) {
            this.ipv6ConnectCount++;
        }
    }

    private void countLatency(int errno, int ms) {
        if (!isNonBlocking(errno) && this.mLatencyTb.get() && this.latencies.size() < this.mMaxLatencyRecords) {
            this.latencies.add(ms);
        }
    }

    private void countError(int errno) {
        this.errnos.put(errno, this.errnos.get(errno, 0) + 1);
    }

    private static boolean isSuccess(int errno) {
        return errno == 0 || isNonBlocking(errno);
    }

    static boolean isNonBlocking(int errno) {
        return errno == EINPROGRESS || errno == EALREADY;
    }

    private static boolean isIPv6(String ipAddr) {
        return ipAddr.contains(":");
    }

    public String toString() {
        int t;
        StringBuilder builder = new StringBuilder("ConnectStats(");
        builder.append("netId=");
        builder.append(this.netId);
        builder = builder.append(", ");
        for (int t2 : BitUtils.unpackBits(this.transports)) {
            builder.append(NetworkCapabilities.transportNameOf(t2));
            builder.append(", ");
        }
        builder.append(String.format("%d events, ", new Object[]{Integer.valueOf(this.eventCount)}));
        builder.append(String.format("%d success, ", new Object[]{Integer.valueOf(this.connectCount)}));
        builder.append(String.format("%d blocking, ", new Object[]{Integer.valueOf(this.connectBlockingCount)}));
        builder.append(String.format("%d IPv6 dst", new Object[]{Integer.valueOf(this.ipv6ConnectCount)}));
        for (int i = 0; i < this.errnos.size(); i++) {
            String errno = OsConstants.errnoName(this.errnos.keyAt(i));
            t2 = this.errnos.valueAt(i);
            builder.append(String.format(", %s: %d", new Object[]{errno, Integer.valueOf(t2)}));
        }
        builder.append(")");
        return builder.toString();
    }
}
