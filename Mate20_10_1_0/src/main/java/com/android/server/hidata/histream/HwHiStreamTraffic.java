package com.android.server.hidata.histream;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class HwHiStreamTraffic {
    private static HwHiStreamTraffic mHwHiStreamTraffic;
    private Context mContext;
    private NetworkStatsManager mNetworkStatsManager = ((NetworkStatsManager) this.mContext.getSystemService("netstats"));
    private TelephonyManager mTelephonyManager = TelephonyManager.from(this.mContext);

    private HwHiStreamTraffic(Context context) {
        this.mContext = context;
    }

    public static HwHiStreamTraffic createInstance(Context context) {
        if (mHwHiStreamTraffic == null) {
            mHwHiStreamTraffic = new HwHiStreamTraffic(context);
        }
        return mHwHiStreamTraffic;
    }

    public static HwHiStreamTraffic getInstance() {
        return mHwHiStreamTraffic;
    }

    public long getTotalTraffic(long startTime, long endTime, int uid, int network) {
        long[] traffic = getTraffic(startTime, endTime, uid, network);
        if (traffic == null || 2 > traffic.length) {
            return 0;
        }
        return traffic[0] + traffic[1];
    }

    /* JADX DEBUG: Multi-variable search result rejected for r15v2, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r15v0, types: [boolean] */
    /* JADX WARN: Type inference failed for: r15v1 */
    /* JADX WARN: Type inference failed for: r15v3 */
    /* JADX WARN: Type inference failed for: r15v5 */
    /* JADX WARN: Type inference failed for: r15v6 */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0090, code lost:
        if (r14 != null) goto L_0x0092;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0092, code lost:
        r14.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00b2, code lost:
        if (0 != 0) goto L_0x0092;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00b5, code lost:
        return r4;
     */
    public long[] getTraffic(long startTime, long endTime, int uid, int network) {
        ?? r15;
        int i;
        NetworkStats.Bucket summaryBucket;
        long[] traffic = new long[2];
        String imsi = this.mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        NetworkStats mNetworkStats = null;
        if (801 == network && TextUtils.isEmpty(imsi)) {
            return traffic;
        }
        try {
            NetworkStats.Bucket summaryBucket2 = new NetworkStats.Bucket();
            if (800 == network) {
                boolean z = false;
                summaryBucket = summaryBucket2;
                try {
                    mNetworkStats = this.mNetworkStatsManager.querySummary(1, imsi, startTime, endTime);
                    i = z;
                } catch (RemoteException | SecurityException e) {
                    e = e;
                    r15 = z;
                    try {
                        Object[] objArr = new Object[1];
                        objArr[r15] = e.getMessage();
                        HwHiStreamUtils.logD(r15, "getMobileTraffic Exception %{public}s", objArr);
                    } catch (Throwable th) {
                        if (0 != 0) {
                            mNetworkStats.close();
                        }
                        throw th;
                    }
                }
            } else {
                i = 0;
                if (801 == network) {
                    summaryBucket = summaryBucket2;
                    mNetworkStats = this.mNetworkStatsManager.querySummary(0, imsi, startTime, endTime);
                } else {
                    if (0 != 0) {
                        mNetworkStats.close();
                    }
                    return traffic;
                }
            }
            long rxBytes = 0;
            long txBytes = 0;
            if (mNetworkStats != null) {
                do {
                    mNetworkStats.getNextBucket(summaryBucket);
                    if (uid == 0) {
                        rxBytes += summaryBucket.getRxBytes();
                        txBytes += summaryBucket.getTxBytes();
                    } else if (uid > 0 && uid == summaryBucket.getUid()) {
                        rxBytes += summaryBucket.getRxBytes();
                        txBytes += summaryBucket.getTxBytes();
                    }
                } while (mNetworkStats.hasNextBucket());
                traffic[i] = rxBytes;
                traffic[1] = txBytes;
            } else {
                HwHiStreamUtils.logD(i, "mNetworkStats == null", new Object[i]);
            }
        } catch (RemoteException | SecurityException e2) {
            e = e2;
            r15 = 0;
            Object[] objArr2 = new Object[1];
            objArr2[r15] = e.getMessage();
            HwHiStreamUtils.logD(r15, "getMobileTraffic Exception %{public}s", objArr2);
        }
    }
}
