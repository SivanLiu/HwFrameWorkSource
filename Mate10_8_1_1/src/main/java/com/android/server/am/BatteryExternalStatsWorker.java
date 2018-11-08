package com.android.server.am;

import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.IWifiManager;
import android.net.wifi.IWifiManager.Stub;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SynchronousResultReceiver;
import android.os.SynchronousResultReceiver.Result;
import android.os.SystemClock;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.util.IntArray;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BatteryStatsImpl.ExternalStatsSync;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import libcore.util.EmptyArray;

class BatteryExternalStatsWorker implements ExternalStatsSync {
    private static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final long MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS = 750;
    private static final String TAG = "BatteryExternalStatsWorker";
    private final Context mContext;
    @GuardedBy("this")
    private Future<?> mCurrentFuture = null;
    @GuardedBy("this")
    private String mCurrentReason = null;
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor(-$Lambda$pTkujrAbcljW_zZtzXt4TxsgOZU.$INST$0);
    @GuardedBy("mWorkerLock")
    private WifiActivityEnergyInfo mLastInfo = new WifiActivityEnergyInfo(0, 0, 0, new long[]{0}, 0, 0, 0);
    private final BatteryStatsImpl mStats;
    private final Runnable mSyncTask = new Runnable() {
        public void run() {
            synchronized (BatteryExternalStatsWorker.this) {
                int updateFlags = BatteryExternalStatsWorker.this.mUpdateFlags;
                String reason = BatteryExternalStatsWorker.this.mCurrentReason;
                int[] uidsToRemove = BatteryExternalStatsWorker.this.mUidsToRemove.size() > 0 ? BatteryExternalStatsWorker.this.mUidsToRemove.toArray() : EmptyArray.INT;
                BatteryExternalStatsWorker.this.mUpdateFlags = 0;
                BatteryExternalStatsWorker.this.mCurrentReason = null;
                BatteryExternalStatsWorker.this.mUidsToRemove.clear();
                BatteryExternalStatsWorker.this.mCurrentFuture = null;
            }
            synchronized (BatteryExternalStatsWorker.this.mWorkerLock) {
                BatteryExternalStatsWorker.this.updateExternalStatsLocked(reason, updateFlags);
            }
            synchronized (BatteryExternalStatsWorker.this.mStats) {
                for (int uid : uidsToRemove) {
                    BatteryExternalStatsWorker.this.mStats.removeIsolatedUidLocked(uid);
                }
            }
        }
    };
    @GuardedBy("mWorkerLock")
    private TelephonyManager mTelephony = null;
    @GuardedBy("this")
    private final IntArray mUidsToRemove = new IntArray();
    @GuardedBy("this")
    private int mUpdateFlags = 0;
    @GuardedBy("mWorkerLock")
    private IWifiManager mWifiManager = null;
    private final Object mWorkerLock = new Object();
    private final Runnable mWriteTask = new Runnable() {
        public void run() {
            synchronized (BatteryExternalStatsWorker.this.mStats) {
                BatteryExternalStatsWorker.this.mStats.writeAsyncLocked();
            }
        }
    };

    static /* synthetic */ Thread lambda$-com_android_server_am_BatteryExternalStatsWorker_2654(Runnable r) {
        Thread t = new Thread(r, "batterystats-worker");
        t.setPriority(5);
        return t;
    }

    BatteryExternalStatsWorker(Context context, BatteryStatsImpl stats) {
        this.mContext = context;
        this.mStats = stats;
    }

    public synchronized Future<?> scheduleSync(String reason, int flags) {
        return scheduleSyncLocked(reason, flags);
    }

    public synchronized Future<?> scheduleCpuSyncDueToRemovedUid(int uid) {
        this.mUidsToRemove.add(uid);
        return scheduleSyncLocked("remove-uid", 1);
    }

    public synchronized Future<?> scheduleWrite() {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        scheduleSyncLocked("write", 31);
        return this.mExecutorService.submit(this.mWriteTask);
    }

    public synchronized void scheduleRunnable(Runnable runnable) {
        if (!this.mExecutorService.isShutdown()) {
            this.mExecutorService.submit(runnable);
        }
    }

    public void shutdown() {
        this.mExecutorService.shutdownNow();
    }

    private Future<?> scheduleSyncLocked(String reason, int flags) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (this.mCurrentFuture == null) {
            this.mUpdateFlags = flags;
            this.mCurrentReason = reason;
            this.mCurrentFuture = this.mExecutorService.submit(this.mSyncTask);
        }
        this.mUpdateFlags |= flags;
        return this.mCurrentFuture;
    }

    private void updateExternalStatsLocked(String reason, int updateFlags) {
        SynchronousResultReceiver synchronousResultReceiver = null;
        SynchronousResultReceiver bluetoothReceiver = null;
        SynchronousResultReceiver modemReceiver = null;
        if ((updateFlags & 2) != 0) {
            if (this.mWifiManager == null) {
                this.mWifiManager = Stub.asInterface(ServiceManager.getService("wifi"));
            }
            if (this.mWifiManager != null) {
                try {
                    SynchronousResultReceiver synchronousResultReceiver2 = new SynchronousResultReceiver("wifi");
                    try {
                        this.mWifiManager.requestActivityInfo(synchronousResultReceiver2);
                        synchronousResultReceiver = synchronousResultReceiver2;
                    } catch (RemoteException e) {
                        synchronousResultReceiver = synchronousResultReceiver2;
                    }
                } catch (RemoteException e2) {
                }
            }
        }
        if ((updateFlags & 8) != 0) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                bluetoothReceiver = new SynchronousResultReceiver("bluetooth");
                adapter.requestControllerActivityEnergyInfo(bluetoothReceiver);
            }
        }
        if ((updateFlags & 4) != 0) {
            if (this.mTelephony == null) {
                this.mTelephony = TelephonyManager.from(this.mContext);
            }
            if (this.mTelephony != null) {
                modemReceiver = new SynchronousResultReceiver("telephony");
                this.mTelephony.requestModemActivityInfo(modemReceiver);
            }
        }
        WifiActivityEnergyInfo wifiInfo = (WifiActivityEnergyInfo) awaitControllerInfo(synchronousResultReceiver);
        BluetoothActivityEnergyInfo bluetoothInfo = (BluetoothActivityEnergyInfo) awaitControllerInfo(bluetoothReceiver);
        ModemActivityInfo modemInfo = (ModemActivityInfo) awaitControllerInfo(modemReceiver);
        synchronized (this.mStats) {
            this.mStats.addHistoryEventLocked(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis(), 14, reason, 0);
            if ((updateFlags & 1) != 0) {
                this.mStats.updateCpuTimeLocked(true);
                this.mStats.updateKernelWakelocksLocked();
                this.mStats.updateKernelMemoryBandwidthLocked();
            }
            if ((updateFlags & 16) != 0) {
                this.mStats.updateRpmStatsLocked();
            }
            if (bluetoothInfo != null) {
                if (bluetoothInfo.isValid()) {
                    this.mStats.updateBluetoothStateLocked(bluetoothInfo);
                } else {
                    Slog.e(TAG, "bluetooth info is invalid: " + bluetoothInfo);
                }
            }
        }
        if (wifiInfo != null) {
            if (wifiInfo.isValid()) {
                this.mStats.updateWifiState(extractDeltaLocked(wifiInfo));
            } else {
                Slog.e(TAG, "wifi info is invalid: " + wifiInfo);
            }
        }
        if (modemInfo == null) {
            return;
        }
        if (modemInfo.isValid()) {
            this.mStats.updateMobileRadioState(modemInfo);
        } else {
            Slog.e(TAG, "modem info is invalid: " + modemInfo);
        }
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver receiver) {
        if (receiver == null) {
            return null;
        }
        try {
            Result result = receiver.awaitResult(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (result.bundle != null) {
                result.bundle.setDefusable(true);
                T data = result.bundle.getParcelable("controller_activity");
                if (data != null) {
                    return data;
                }
            }
            Slog.e(TAG, "no controller energy info supplied for " + receiver.getName());
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + receiver.getName() + " stats");
        }
        return null;
    }

    private WifiActivityEnergyInfo extractDeltaLocked(WifiActivityEnergyInfo latest) {
        long timePeriodMs = latest.mTimestamp - this.mLastInfo.mTimestamp;
        long lastIdleMs = this.mLastInfo.mControllerIdleTimeMs;
        long lastTxMs = this.mLastInfo.mControllerTxTimeMs;
        long lastRxMs = this.mLastInfo.mControllerRxTimeMs;
        long lastEnergy = this.mLastInfo.mControllerEnergyUsed;
        WifiActivityEnergyInfo delta = this.mLastInfo;
        delta.mTimestamp = latest.getTimeStamp();
        delta.mStackState = latest.getStackState();
        long txTimeMs = latest.mControllerTxTimeMs - lastTxMs;
        long rxTimeMs = latest.mControllerRxTimeMs - lastRxMs;
        long idleTimeMs = latest.mControllerIdleTimeMs - lastIdleMs;
        if (txTimeMs < 0 || rxTimeMs < 0) {
            delta.mControllerEnergyUsed = latest.mControllerEnergyUsed;
            delta.mControllerRxTimeMs = latest.mControllerRxTimeMs;
            delta.mControllerTxTimeMs = latest.mControllerTxTimeMs;
            delta.mControllerIdleTimeMs = latest.mControllerIdleTimeMs;
            Slog.v(TAG, "WiFi energy data was reset, new WiFi energy data is " + delta);
        } else {
            long maxExpectedIdleTimeMs;
            long totalActiveTimeMs = txTimeMs + rxTimeMs;
            if (totalActiveTimeMs > timePeriodMs) {
                maxExpectedIdleTimeMs = 0;
                if (totalActiveTimeMs > MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS + timePeriodMs) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Total Active time ");
                    TimeUtils.formatDuration(totalActiveTimeMs, sb);
                    sb.append(" is longer than sample period ");
                    TimeUtils.formatDuration(timePeriodMs, sb);
                    sb.append(".\n");
                    sb.append("Previous WiFi snapshot: ").append("idle=");
                    TimeUtils.formatDuration(lastIdleMs, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(lastRxMs, sb);
                    sb.append(" tx=");
                    TimeUtils.formatDuration(lastTxMs, sb);
                    sb.append(" e=").append(lastEnergy);
                    sb.append("\n");
                    sb.append("Current WiFi snapshot: ").append("idle=");
                    TimeUtils.formatDuration(latest.mControllerIdleTimeMs, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(latest.mControllerRxTimeMs, sb);
                    sb.append(" tx=");
                    TimeUtils.formatDuration(latest.mControllerTxTimeMs, sb);
                    sb.append(" e=").append(latest.mControllerEnergyUsed);
                    Slog.wtf(TAG, sb.toString());
                }
            } else {
                maxExpectedIdleTimeMs = timePeriodMs - totalActiveTimeMs;
            }
            delta.mControllerTxTimeMs = txTimeMs;
            delta.mControllerRxTimeMs = rxTimeMs;
            delta.mControllerIdleTimeMs = Math.min(maxExpectedIdleTimeMs, Math.max(0, idleTimeMs));
            delta.mControllerEnergyUsed = Math.max(0, latest.mControllerEnergyUsed - lastEnergy);
        }
        this.mLastInfo = latest;
        return delta;
    }
}
