package com.huawei.opcollect.collector.pullcollection;

import android.app.usage.NetworkStats.Bucket;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.huawei.netassistant.service.INetAssistantService;
import com.huawei.netassistant.service.INetAssistantService.Stub;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;

public class NetAssistantManager {
    private static final long INVALID_DATA = -1;
    private static final String TAG = "HwUserCollection";
    private static NetAssistantManager sInstance = null;
    private INetAssistantService mNetAssistantService = null;
    private NetworkStatsManager mNetworkStatsManager;

    public static synchronized NetAssistantManager getInstance(Context context) {
        NetAssistantManager netAssistantManager;
        synchronized (NetAssistantManager.class) {
            if (sInstance == null) {
                sInstance = new NetAssistantManager(context);
            }
            netAssistantManager = sInstance;
        }
        return netAssistantManager;
    }

    private NetAssistantManager(Context context) {
        if (context != null && VERSION.SDK_INT >= 23) {
            this.mNetworkStatsManager = (NetworkStatsManager) context.getSystemService("netstats");
        }
    }

    private void checkNotificationBinder() {
        if (this.mNetAssistantService == null) {
            try {
                IBinder b = ServiceManager.getService("com.huawei.netassistant.service.netassistantservice");
                if (b != null) {
                    this.mNetAssistantService = Stub.asInterface(b);
                    OPCollectLog.i(TAG, "mNetAssistantService=" + this.mNetAssistantService);
                }
            } catch (Exception e) {
                OPCollectLog.i(TAG, "getService exception: " + e.getMessage());
            }
        }
    }

    public long getTodayMobileTotalBytes(String imsi) {
        return getMobileBytes(imsi, OPCollectUtils.getDayStartTimeMills(), OPCollectUtils.getCurrentTimeMills());
    }

    public long getMobileLeftBytes(String imsi) throws RemoteException {
        checkNotificationBinder();
        if (this.mNetAssistantService != null) {
            return this.mNetAssistantService.getMonthlyTotalBytes(imsi) - this.mNetAssistantService.getMonthMobileTotalBytes(imsi);
        }
        OPCollectLog.e(TAG, "client proxy is null");
        return INVALID_DATA;
    }

    public long getTodayWifiTotalBytes() {
        return getWifiBytes(OPCollectUtils.getDayStartTimeMills(), OPCollectUtils.getCurrentTimeMills());
    }

    private long getWifiBytes(long start, long end) {
        if (VERSION.SDK_INT < 23) {
            return INVALID_DATA;
        }
        try {
            Bucket bucket = querySummaryForDevice(1, null, start, end);
            if (bucket == null) {
                return INVALID_DATA;
            }
            return bucket.getRxBytes() + bucket.getTxBytes();
        } catch (RemoteException e) {
            OPCollectLog.i(TAG, "RemoteException error in get wifi bytes");
            return INVALID_DATA;
        } catch (Exception e2) {
            OPCollectLog.i(TAG, "error in get wifi bytes");
            return INVALID_DATA;
        }
    }

    private long getMobileBytes(String imsi, long start, long end) {
        if (VERSION.SDK_INT < 23) {
            return INVALID_DATA;
        }
        try {
            Bucket bucket = querySummaryForDevice(0, imsi, start, end);
            if (bucket == null) {
                return INVALID_DATA;
            }
            return bucket.getRxBytes() + bucket.getTxBytes();
        } catch (RemoteException e) {
            OPCollectLog.i(TAG, "RemoteException in get mobile bytes");
            return INVALID_DATA;
        } catch (Exception e2) {
            OPCollectLog.i(TAG, "error in get mobile bytes");
            return INVALID_DATA;
        }
    }

    private Bucket querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime) throws SecurityException, RemoteException {
        if (this.mNetworkStatsManager == null) {
            OPCollectLog.e(TAG, "mNetworkStatsManager is null.");
            return null;
        } else if (VERSION.SDK_INT < 23) {
            return null;
        } else {
            return this.mNetworkStatsManager.querySummaryForDevice(networkType, subscriberId, startTime, endTime);
        }
    }
}
