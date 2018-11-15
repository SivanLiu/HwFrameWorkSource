package com.android.server.hidata.histream;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStats.Bucket;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class HwHiStreamTraffic {
    private static HwHiStreamTraffic mHwHiStreamTraffic;
    private Context mContext;
    private NetworkStatsManager mNetworkStatsManager = ((NetworkStatsManager) this.mContext.getSystemService("netstats"));
    private TelephonyManager mTelephonyManager = TelephonyManager.from(this.mContext);
    private Bucket summaryBucket;

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

    public long[] getTraffic(long startTime, long endTime, int uid, int network) {
        int i = uid;
        int i2 = network;
        long[] traffic = new long[2];
        String imsi = this.mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (801 == i2 && TextUtils.isEmpty(imsi)) {
            return traffic;
        }
        try {
            NetworkStats mNetworkStats;
            this.summaryBucket = new Bucket();
            if (800 == i2) {
                mNetworkStats = this.mNetworkStatsManager.querySummary(1, imsi, startTime, endTime);
            } else if (801 != i2) {
                return traffic;
            } else {
                mNetworkStats = this.mNetworkStatsManager.querySummary(0, imsi, startTime, endTime);
            }
            NetworkStats mNetworkStats2 = mNetworkStats;
            long rxBytes = 0;
            long txBytes = 0;
            if (mNetworkStats2 != null) {
                do {
                    mNetworkStats2.getNextBucket(this.summaryBucket);
                    if (i == 0) {
                        rxBytes += this.summaryBucket.getRxBytes();
                        txBytes += this.summaryBucket.getTxBytes();
                    } else if (i > 0 && i == this.summaryBucket.getUid()) {
                        rxBytes += this.summaryBucket.getRxBytes();
                        txBytes += this.summaryBucket.getTxBytes();
                    }
                } while (mNetworkStats2.hasNextBucket());
                traffic[0] = rxBytes;
                traffic[1] = txBytes;
            } else {
                HwHiStreamUtils.logD("mNetworkStats == null");
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMobileTraffic Exception");
            stringBuilder.append(e);
            HwHiStreamUtils.logD(stringBuilder.toString());
        }
        return traffic;
    }
}
