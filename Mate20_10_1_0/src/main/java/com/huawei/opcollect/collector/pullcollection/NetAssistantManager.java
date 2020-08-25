package com.huawei.opcollect.collector.pullcollection;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.huawei.android.os.ServiceManagerEx;
import com.huawei.netassistant.service.INetAssistantService;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;

public class NetAssistantManager {
    private static final long INVALID_DATA = -1;
    private static final Object LOCK = new Object();
    private static final String TAG = "NetAssistantManager";
    private static NetAssistantManager instance = null;
    private INetAssistantService mNetAssistantService = null;
    private NetworkStatsManager mNetworkStatsManager;

    private NetAssistantManager(Context context) {
        if (context != null) {
            this.mNetworkStatsManager = (NetworkStatsManager) context.getSystemService("netstats");
        }
    }

    public static NetAssistantManager getInstance(Context context) {
        NetAssistantManager netAssistantManager;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new NetAssistantManager(context);
            }
            netAssistantManager = instance;
        }
        return netAssistantManager;
    }

    private void checkNotificationBinder() {
        if (this.mNetAssistantService == null) {
            try {
                IBinder binder = ServiceManagerEx.getService("com.huawei.netassistant.service.netassistantservice");
                if (binder != null) {
                    this.mNetAssistantService = INetAssistantService.Stub.asInterface(binder);
                    OPCollectLog.i(TAG, "mNetAssistantService=" + this.mNetAssistantService);
                }
            } catch (RuntimeException e) {
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
        try {
            NetworkStats.Bucket bucket = querySummaryForDevice(1, null, start, end);
            if (bucket == null) {
                return INVALID_DATA;
            }
            return bucket.getRxBytes() + bucket.getTxBytes();
        } catch (RemoteException e) {
            OPCollectLog.i(TAG, "RemoteException error in get wifi bytes");
            return INVALID_DATA;
        } catch (SecurityException e2) {
            OPCollectLog.i(TAG, "error in get wifi bytes");
            return INVALID_DATA;
        }
    }

    private long getMobileBytes(String imsi, long start, long end) {
        try {
            NetworkStats.Bucket bucket = querySummaryForDevice(0, imsi, start, end);
            if (bucket == null) {
                return INVALID_DATA;
            }
            return bucket.getRxBytes() + bucket.getTxBytes();
        } catch (RemoteException e) {
            OPCollectLog.i(TAG, "RemoteException in get mobile bytes");
            return INVALID_DATA;
        } catch (SecurityException e2) {
            OPCollectLog.i(TAG, "error in get mobile bytes");
            return INVALID_DATA;
        }
    }

    private NetworkStats.Bucket querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime) throws SecurityException, RemoteException {
        if (this.mNetworkStatsManager != null) {
            return this.mNetworkStatsManager.querySummaryForDevice(networkType, subscriberId, startTime, endTime);
        }
        OPCollectLog.e(TAG, "mNetworkStatsManager is null.");
        return null;
    }
}
