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
import com.android.server.stats.StatsCompanionService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import libcore.util.EmptyArray;

class BatteryExternalStatsWorker implements ExternalStatsSync {
    private static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final long MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS = 750;
    private static final String TAG = "BatteryExternalStatsWorker";
    @GuardedBy("this")
    private Future<?> mBatteryLevelSync;
    private final Context mContext;
    @GuardedBy("this")
    private Future<?> mCurrentFuture = null;
    @GuardedBy("this")
    private String mCurrentReason = null;
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor(-$$Lambda$BatteryExternalStatsWorker$y4b5S_CLdUbDV0ejaQDagLXGZRg.INSTANCE);
    @GuardedBy("this")
    private long mLastCollectionTimeStamp;
    @GuardedBy("mWorkerLock")
    private WifiActivityEnergyInfo mLastInfo = new WifiActivityEnergyInfo(0, 0, 0, new long[]{0}, 0, 0, 0, 0);
    @GuardedBy("this")
    private boolean mOnBattery;
    @GuardedBy("this")
    private boolean mOnBatteryScreenOff;
    private final BatteryStatsImpl mStats;
    private final Runnable mSyncTask = new Runnable() {
        public void run() {
            int updateFlags;
            String reason;
            int[] uidsToRemove;
            boolean onBattery;
            boolean onBatteryScreenOff;
            boolean useLatestStates;
            int i;
            synchronized (BatteryExternalStatsWorker.this) {
                updateFlags = BatteryExternalStatsWorker.this.mUpdateFlags;
                reason = BatteryExternalStatsWorker.this.mCurrentReason;
                uidsToRemove = BatteryExternalStatsWorker.this.mUidsToRemove.size() > 0 ? BatteryExternalStatsWorker.this.mUidsToRemove.toArray() : EmptyArray.INT;
                onBattery = BatteryExternalStatsWorker.this.mOnBattery;
                onBatteryScreenOff = BatteryExternalStatsWorker.this.mOnBatteryScreenOff;
                useLatestStates = BatteryExternalStatsWorker.this.mUseLatestStates;
                i = 0;
                BatteryExternalStatsWorker.this.mUpdateFlags = 0;
                BatteryExternalStatsWorker.this.mCurrentReason = null;
                BatteryExternalStatsWorker.this.mUidsToRemove.clear();
                BatteryExternalStatsWorker.this.mCurrentFuture = null;
                BatteryExternalStatsWorker.this.mUseLatestStates = true;
                if ((updateFlags & 31) != 0) {
                    BatteryExternalStatsWorker.this.cancelSyncDueToBatteryLevelChangeLocked();
                }
                if ((updateFlags & 1) != 0) {
                    BatteryExternalStatsWorker.this.cancelCpuSyncDueToWakelockChange();
                }
            }
            try {
                synchronized (BatteryExternalStatsWorker.this.mWorkerLock) {
                    BatteryExternalStatsWorker.this.updateExternalStatsLocked(reason, updateFlags, onBattery, onBatteryScreenOff, useLatestStates);
                }
                if ((updateFlags & 1) != 0) {
                    BatteryExternalStatsWorker.this.mStats.copyFromAllUidsCpuTimes();
                }
                synchronized (BatteryExternalStatsWorker.this.mStats) {
                    int length = uidsToRemove.length;
                    while (i < length) {
                        BatteryExternalStatsWorker.this.mStats.removeIsolatedUidLocked(uidsToRemove[i]);
                        i++;
                    }
                    BatteryExternalStatsWorker.this.mStats.clearPendingRemovedUids();
                }
            } catch (Exception e) {
                Slog.wtf(BatteryExternalStatsWorker.TAG, "Error updating external stats: ", e);
            }
            synchronized (BatteryExternalStatsWorker.this) {
                BatteryExternalStatsWorker.this.mLastCollectionTimeStamp = SystemClock.elapsedRealtime();
            }
        }
    };
    @GuardedBy("mWorkerLock")
    private TelephonyManager mTelephony = null;
    @GuardedBy("this")
    private final IntArray mUidsToRemove = new IntArray();
    @GuardedBy("this")
    private int mUpdateFlags = 0;
    @GuardedBy("this")
    private boolean mUseLatestStates = true;
    @GuardedBy("this")
    private Future<?> mWakelockChangesUpdate;
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

    static /* synthetic */ Thread lambda$new$0(Runnable r) {
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

    public synchronized Future<?> scheduleCpuSyncDueToSettingChange() {
        return scheduleSyncLocked("setting-change", 1);
    }

    /* JADX WARNING: Missing block: B:8:0x000f, code:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:11:0x0016, code:
            if (r5.mExecutorService.isShutdown() != false) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:12:0x0018, code:
            r0 = r5.mExecutorService.schedule(com.android.internal.util.function.pooled.PooledLambda.obtainRunnable(com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$cC4f0pNQX9_D9f8AXLmKk2sArGY.INSTANCE, r5.mStats, java.lang.Boolean.valueOf(r6), java.lang.Boolean.valueOf(r7)).recycleOnUse(), r8, java.util.concurrent.TimeUnit.MILLISECONDS);
     */
    /* JADX WARNING: Missing block: B:13:0x0034, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:14:0x0035, code:
            return r0;
     */
    /* JADX WARNING: Missing block: B:15:0x0036, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Future<?> scheduleReadProcStateCpuTimes(boolean onBattery, boolean onBatteryScreenOff, long delayMillis) {
        synchronized (this.mStats) {
            if (!this.mStats.trackPerProcStateCpuTimes()) {
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000f, code:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:11:0x0016, code:
            if (r5.mExecutorService.isShutdown() != false) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:12:0x0018, code:
            r0 = r5.mExecutorService.submit(com.android.internal.util.function.pooled.PooledLambda.obtainRunnable(com.android.server.am.-$$Lambda$BatteryExternalStatsWorker$7toxTvZDSEytL0rCkoEfGilPDWM.INSTANCE, r5.mStats, java.lang.Boolean.valueOf(r6), java.lang.Boolean.valueOf(r7)).recycleOnUse());
     */
    /* JADX WARNING: Missing block: B:13:0x0032, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:14:0x0033, code:
            return r0;
     */
    /* JADX WARNING: Missing block: B:15:0x0034, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:16:0x0035, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Future<?> scheduleCopyFromAllUidsCpuTimes(boolean onBattery, boolean onBatteryScreenOff) {
        synchronized (this.mStats) {
            if (!this.mStats.trackPerProcStateCpuTimes()) {
                return null;
            }
        }
    }

    public Future<?> scheduleCpuSyncDueToScreenStateChange(boolean onBattery, boolean onBatteryScreenOff) {
        Future<?> scheduleSyncLocked;
        synchronized (this) {
            if (this.mCurrentFuture == null || (this.mUpdateFlags & 1) == 0) {
                this.mOnBattery = onBattery;
                this.mOnBatteryScreenOff = onBatteryScreenOff;
                this.mUseLatestStates = false;
            }
            scheduleSyncLocked = scheduleSyncLocked("screen-state", 1);
        }
        return scheduleSyncLocked;
    }

    public Future<?> scheduleCpuSyncDueToWakelockChange(long delayMillis) {
        Future<?> future;
        synchronized (this) {
            this.mWakelockChangesUpdate = scheduleDelayedSyncLocked(this.mWakelockChangesUpdate, new -$$Lambda$BatteryExternalStatsWorker$PpNEY15dspg9oLlkg1OsyjrPTqw(this), delayMillis);
            future = this.mWakelockChangesUpdate;
        }
        return future;
    }

    public static /* synthetic */ void lambda$scheduleCpuSyncDueToWakelockChange$2(BatteryExternalStatsWorker batteryExternalStatsWorker) {
        batteryExternalStatsWorker.scheduleSync("wakelock-change", 1);
        batteryExternalStatsWorker.scheduleRunnable(new -$$Lambda$BatteryExternalStatsWorker$Nx17DLnpsjeC2juW1TuPEAogLvE(batteryExternalStatsWorker));
    }

    public void cancelCpuSyncDueToWakelockChange() {
        synchronized (this) {
            if (this.mWakelockChangesUpdate != null) {
                this.mWakelockChangesUpdate.cancel(false);
                this.mWakelockChangesUpdate = null;
            }
        }
    }

    public Future<?> scheduleSyncDueToBatteryLevelChange(long delayMillis) {
        Future<?> future;
        synchronized (this) {
            this.mBatteryLevelSync = scheduleDelayedSyncLocked(this.mBatteryLevelSync, new -$$Lambda$BatteryExternalStatsWorker$eNtlYRY6yBjSWzaL4STPjcGEduM(this), delayMillis);
            future = this.mBatteryLevelSync;
        }
        return future;
    }

    @GuardedBy("this")
    private void cancelSyncDueToBatteryLevelChangeLocked() {
        if (this.mBatteryLevelSync != null) {
            this.mBatteryLevelSync.cancel(false);
            this.mBatteryLevelSync = null;
        }
    }

    @GuardedBy("this")
    private Future<?> scheduleDelayedSyncLocked(Future<?> lastScheduledSync, Runnable syncRunnable, long delayMillis) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (lastScheduledSync != null) {
            if (delayMillis != 0) {
                return lastScheduledSync;
            }
            lastScheduledSync.cancel(false);
        }
        return this.mExecutorService.schedule(syncRunnable, delayMillis, TimeUnit.MILLISECONDS);
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

    @GuardedBy("this")
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

    long getLastCollectionTimeStamp() {
        long j;
        synchronized (this) {
            j = this.mLastCollectionTimeStamp;
        }
        return j;
    }

    /* JADX WARNING: Missing block: B:51:0x00f2, code:
            if (r5 == null) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:53:0x00f8, code:
            if (r5.isValid() == false) goto L_0x0104;
     */
    /* JADX WARNING: Missing block: B:54:0x00fa, code:
            r1.mStats.updateWifiState(extractDeltaLocked(r5));
     */
    /* JADX WARNING: Missing block: B:55:0x0104, code:
            r0 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("wifi info is invalid: ");
            r8.append(r5);
            android.util.Slog.w(r0, r8.toString());
     */
    /* JADX WARNING: Missing block: B:56:0x011b, code:
            if (r7 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:58:0x0121, code:
            if (r7.isValid() == false) goto L_0x0129;
     */
    /* JADX WARNING: Missing block: B:59:0x0123, code:
            r1.mStats.updateMobileRadioState(r7);
     */
    /* JADX WARNING: Missing block: B:60:0x0129, code:
            r0 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("modem info is invalid: ");
            r8.append(r7);
            android.util.Slog.w(r0, r8.toString());
     */
    /* JADX WARNING: Missing block: B:68:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:69:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:70:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mWorkerLock")
    private void updateExternalStatsLocked(String reason, int updateFlags, boolean onBattery, boolean onBatteryScreenOff, boolean useLatestStates) {
        Throwable th;
        SynchronousResultReceiver wifiReceiver = null;
        SynchronousResultReceiver bluetoothReceiver = null;
        SynchronousResultReceiver modemReceiver = null;
        if ((updateFlags & 2) != 0) {
            if (this.mWifiManager == null) {
                this.mWifiManager = Stub.asInterface(ServiceManager.getService("wifi"));
            }
            if (this.mWifiManager != null) {
                try {
                    wifiReceiver = new SynchronousResultReceiver("wifi");
                    this.mWifiManager.requestActivityInfo(wifiReceiver);
                } catch (RemoteException e) {
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
        WifiActivityEnergyInfo wifiInfo = (WifiActivityEnergyInfo) awaitControllerInfo(wifiReceiver);
        BluetoothActivityEnergyInfo bluetoothInfo = (BluetoothActivityEnergyInfo) awaitControllerInfo(bluetoothReceiver);
        ModemActivityInfo modemInfo = (ModemActivityInfo) awaitControllerInfo(modemReceiver);
        synchronized (this.mStats) {
            boolean onBattery2;
            try {
                this.mStats.addHistoryEventLocked(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis(), 14, reason, 0);
                boolean onBatteryScreenOff2;
                if ((updateFlags & 1) != 0) {
                    if (useLatestStates) {
                        onBattery2 = this.mStats.isOnBatteryLocked();
                        try {
                            onBatteryScreenOff2 = this.mStats.isOnBatteryScreenOffLocked();
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    onBattery2 = onBattery;
                    onBatteryScreenOff2 = onBatteryScreenOff;
                    try {
                        this.mStats.updateCpuTimeLocked(onBattery2, onBatteryScreenOff2);
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
                onBatteryScreenOff2 = onBatteryScreenOff;
                if ((updateFlags & 31) != 0) {
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
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("bluetooth info is invalid: ");
                        stringBuilder.append(bluetoothInfo);
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                onBattery2 = onBattery;
                throw th;
            }
        }
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver receiver) {
        if (receiver == null) {
            return null;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            Result result = receiver.awaitResult(2000);
            if (result.bundle != null) {
                result.bundle.setDefusable(true);
                T data = result.bundle.getParcelable(StatsCompanionService.RESULT_RECEIVER_CONTROLLER_KEY);
                if (data != null) {
                    return data;
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("no controller energy info supplied for ");
            stringBuilder.append(receiver.getName());
            Slog.e(str, stringBuilder.toString());
        } catch (TimeoutException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("timeout reading ");
            stringBuilder.append(receiver.getName());
            stringBuilder.append(" stats");
            Slog.w(str, stringBuilder.toString());
        }
        return null;
    }

    @GuardedBy("mWorkerLock")
    private WifiActivityEnergyInfo extractDeltaLocked(WifiActivityEnergyInfo latest) {
        WifiActivityEnergyInfo delta;
        WifiActivityEnergyInfo wifiActivityEnergyInfo = latest;
        long timePeriodMs = wifiActivityEnergyInfo.mTimestamp - this.mLastInfo.mTimestamp;
        long lastScanMs = this.mLastInfo.mControllerScanTimeMs;
        long lastIdleMs = this.mLastInfo.mControllerIdleTimeMs;
        long lastTxMs = this.mLastInfo.mControllerTxTimeMs;
        long lastRxMs = this.mLastInfo.mControllerRxTimeMs;
        long lastEnergy = this.mLastInfo.mControllerEnergyUsed;
        WifiActivityEnergyInfo delta2 = this.mLastInfo;
        long lastEnergy2 = lastEnergy;
        delta2.mTimestamp = latest.getTimeStamp();
        delta2.mStackState = latest.getStackState();
        lastEnergy = wifiActivityEnergyInfo.mControllerTxTimeMs - lastTxMs;
        WifiActivityEnergyInfo delta3 = delta2;
        long lastEnergy3 = lastEnergy2;
        long rxTimeMs = wifiActivityEnergyInfo.mControllerRxTimeMs - lastRxMs;
        long lastTxMs2 = lastTxMs;
        long idleTimeMs = wifiActivityEnergyInfo.mControllerIdleTimeMs - lastIdleMs;
        lastTxMs = wifiActivityEnergyInfo.mControllerScanTimeMs - lastScanMs;
        long j;
        long j2;
        long j3;
        long j4;
        if (lastEnergy < 0 || rxTimeMs < 0) {
            j = lastIdleMs;
            j2 = lastTxMs;
            j3 = lastRxMs;
            delta = delta3;
            j4 = lastTxMs2;
        } else if (lastTxMs < 0) {
            long j5 = timePeriodMs;
            j = lastIdleMs;
            j2 = lastTxMs;
            j3 = lastRxMs;
            delta = delta3;
            lastIdleMs = lastEnergy3;
            j4 = lastTxMs2;
            timePeriodMs = idleTimeMs;
        } else {
            long maxExpectedIdleTimeMs;
            long scanTimeMs;
            lastScanMs = lastEnergy + rxTimeMs;
            if (lastScanMs > timePeriodMs) {
                maxExpectedIdleTimeMs = 0;
                if (lastScanMs > timePeriodMs + MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS) {
                    StringBuilder sb = new StringBuilder();
                    scanTimeMs = lastTxMs;
                    sb.append("Total Active time ");
                    TimeUtils.formatDuration(lastScanMs, sb);
                    sb.append(" is longer than sample period ");
                    TimeUtils.formatDuration(timePeriodMs, sb);
                    sb.append(".\n");
                    sb.append("Previous WiFi snapshot: ");
                    sb.append("idle=");
                    TimeUtils.formatDuration(lastIdleMs, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(lastRxMs, sb);
                    sb.append(" tx=");
                    lastTxMs = lastTxMs2;
                    TimeUtils.formatDuration(lastTxMs, sb);
                    sb.append(" e=");
                    lastIdleMs = lastEnergy3;
                    sb.append(lastIdleMs);
                    sb.append("\n");
                    sb.append("Current WiFi snapshot: ");
                    sb.append("idle=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerIdleTimeMs, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerRxTimeMs, sb);
                    sb.append(" tx=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerTxTimeMs, sb);
                    sb.append(" e=");
                    sb.append(wifiActivityEnergyInfo.mControllerEnergyUsed);
                    Slog.wtf(TAG, sb.toString());
                } else {
                    scanTimeMs = lastTxMs;
                    lastIdleMs = lastEnergy3;
                    j4 = lastTxMs2;
                }
            } else {
                scanTimeMs = lastTxMs;
                lastIdleMs = lastEnergy3;
                j4 = lastTxMs2;
                maxExpectedIdleTimeMs = timePeriodMs - lastScanMs;
            }
            lastTxMs = maxExpectedIdleTimeMs;
            delta = delta3;
            delta.mControllerTxTimeMs = lastEnergy;
            delta.mControllerRxTimeMs = rxTimeMs;
            timePeriodMs = scanTimeMs;
            delta.mControllerScanTimeMs = timePeriodMs;
            j2 = timePeriodMs;
            delta.mControllerIdleTimeMs = Math.min(lastTxMs, Math.max(0, idleTimeMs));
            delta.mControllerEnergyUsed = Math.max(0, wifiActivityEnergyInfo.mControllerEnergyUsed - lastIdleMs);
            this.mLastInfo = wifiActivityEnergyInfo;
            return delta;
        }
        delta.mControllerEnergyUsed = wifiActivityEnergyInfo.mControllerEnergyUsed;
        delta.mControllerRxTimeMs = wifiActivityEnergyInfo.mControllerRxTimeMs;
        delta.mControllerTxTimeMs = wifiActivityEnergyInfo.mControllerTxTimeMs;
        delta.mControllerIdleTimeMs = wifiActivityEnergyInfo.mControllerIdleTimeMs;
        delta.mControllerScanTimeMs = wifiActivityEnergyInfo.mControllerScanTimeMs;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WiFi energy data was reset, new WiFi energy data is ");
        stringBuilder.append(delta);
        Slog.v(str, stringBuilder.toString());
        this.mLastInfo = wifiActivityEnergyInfo;
        return delta;
    }
}
