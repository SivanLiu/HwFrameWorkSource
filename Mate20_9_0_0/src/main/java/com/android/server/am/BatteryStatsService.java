package com.android.server.am;

import android.app.ActivityManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.util.NetworkConstants;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.BatteryStats.Uid;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFormatException;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiBatteryStats;
import android.os.health.HealthStatsParceler;
import android.os.health.HealthStatsWriter;
import android.os.health.UidHealthStats;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.HwLog;
import android.util.Slog;
import android.util.StatsLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.internal.os.AtomicFile;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BatteryStatsImpl.PlatformIdleStateCallback;
import com.android.internal.os.BatteryStatsImpl.UserInfoProvider;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.RpmStats;
import com.android.internal.util.DumpUtils;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.utils.PriorityDump;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public final class BatteryStatsService extends Stub implements LowPowerModeListener, PlatformIdleStateCallback {
    static final boolean DBG = false;
    private static final int MAX_LOW_POWER_STATS_SIZE = 2048;
    static final String TAG = "BatteryStatsService";
    private static final int USER_TYPE_CHINA_BETA = 3;
    private static final int USER_TYPE_OVERSEA_BETA = 5;
    private static IBatteryStats sService;
    private ActivityManagerService mActivityManagerService;
    private final Context mContext;
    private CharsetDecoder mDecoderStat = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
    private boolean mIsPowerInfoStatus = false;
    private IHwPowerInfoService mPowerInfoService;
    final BatteryStatsImpl mStats;
    private final UserInfoProvider mUserManagerUserInfoProvider;
    private CharBuffer mUtf16BufferStat = CharBuffer.allocate(2048);
    private ByteBuffer mUtf8BufferStat = ByteBuffer.allocateDirect(2048);
    private final BatteryExternalStatsWorker mWorker;
    private boolean misBetaUser = false;

    private final class LocalService extends BatteryStatsInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(BatteryStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public String[] getWifiIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getWifiIfaces().clone();
        }

        public String[] getMobileIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getMobileIfaces().clone();
        }

        public void noteJobsDeferred(int uid, int numDeferred, long sinceLast) {
            BatteryStatsService.this.noteJobsDeferred(uid, numDeferred, sinceLast);
        }
    }

    final class WakeupReasonThread extends Thread {
        private static final int MAX_REASON_SIZE = 512;
        private CharsetDecoder mDecoder;
        private CharBuffer mUtf16Buffer;
        private ByteBuffer mUtf8Buffer;

        WakeupReasonThread() {
            super("BatteryStats_wakeupReason");
        }

        public void run() {
            Process.setThreadPriority(-2);
            this.mDecoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
            this.mUtf8Buffer = ByteBuffer.allocateDirect(512);
            this.mUtf16Buffer = CharBuffer.allocate(512);
            while (true) {
                try {
                    String waitWakeup = waitWakeup();
                    String reason = waitWakeup;
                    if (waitWakeup != null) {
                        synchronized (BatteryStatsService.this.mStats) {
                            BatteryStatsService.this.mStats.noteWakeupReasonLocked(reason);
                            HwServiceFactory.reportSysWakeUp(reason);
                        }
                        if (BatteryStatsService.this.misBetaUser && BatteryStatsService.this.mPowerInfoService != null) {
                            synchronized (BatteryStatsService.this.mPowerInfoService) {
                                BatteryStatsService.this.mPowerInfoService.notePowerInfoWakeupReason(reason);
                            }
                        }
                    } else {
                        return;
                    }
                } catch (RuntimeException e) {
                    Slog.e(BatteryStatsService.TAG, "Failure reading wakeup reasons", e);
                    return;
                }
            }
            while (true) {
            }
        }

        private String waitWakeup() {
            this.mUtf8Buffer.clear();
            this.mUtf16Buffer.clear();
            this.mDecoder.reset();
            int bytesWritten = BatteryStatsService.nativeWaitWakeup(this.mUtf8Buffer);
            if (bytesWritten < 0) {
                return null;
            }
            if (bytesWritten == 0) {
                return Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            this.mUtf8Buffer.limit(bytesWritten);
            this.mDecoder.decode(this.mUtf8Buffer, this.mUtf16Buffer, true);
            this.mUtf16Buffer.flip();
            return this.mUtf16Buffer.toString();
        }
    }

    private native void getLowPowerStats(RpmStats rpmStats);

    private native int getPlatformLowPowerStats(ByteBuffer byteBuffer);

    private native int getSubsystemLowPowerStats(ByteBuffer byteBuffer);

    private static native int nativeWaitWakeup(ByteBuffer byteBuffer);

    public void fillLowPowerStats(RpmStats rpmStats) {
        getLowPowerStats(rpmStats);
    }

    public String getPlatformLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int bytesWritten = getPlatformLowPowerStats(this.mUtf8BufferStat);
        if (bytesWritten < 0) {
            return null;
        }
        if (bytesWritten == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(bytesWritten);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    public String getSubsystemLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int bytesWritten = getSubsystemLowPowerStats(this.mUtf8BufferStat);
        if (bytesWritten < 0) {
            return null;
        }
        if (bytesWritten == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(bytesWritten);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    BatteryStatsService(Context context, File systemDir, Handler handler) {
        this.mContext = context;
        this.mUserManagerUserInfoProvider = new UserInfoProvider() {
            private UserManagerInternal umi;

            public int[] getUserIds() {
                if (this.umi == null) {
                    this.umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
                }
                return this.umi != null ? this.umi.getUserIds() : null;
            }
        };
        this.mStats = new BatteryStatsImpl(systemDir, handler, this, this.mUserManagerUserInfoProvider);
        this.mWorker = new BatteryExternalStatsWorker(context, this.mStats);
        this.mStats.setExternalStatsSyncLocked(this.mWorker);
        this.mStats.setRadioScanningTimeoutLocked(((long) this.mContext.getResources().getInteger(17694851)) * 1000);
        this.mStats.setPowerProfileLocked(new PowerProfile(context));
    }

    public void publish() {
        LocalServices.addService(BatteryStatsInternal.class, new LocalService(this, null));
        ServiceManager.addService("batterystats", asBinder());
    }

    public void systemServicesReady() {
        this.mStats.systemServicesReady(this.mContext);
    }

    private static void awaitUninterruptibly(Future<?> future) {
        while (true) {
            try {
                future.get();
                return;
            } catch (ExecutionException e) {
                return;
            } catch (InterruptedException e2) {
            }
        }
    }

    private void syncStats(String reason, int flags) {
        awaitUninterruptibly(this.mWorker.scheduleSync(reason, flags));
    }

    public void initPowerManagement() {
        PowerManagerInternal powerMgr = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        powerMgr.registerLowPowerModeObserver(this);
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(powerMgr.getLowPowerState(9).batterySaverEnabled);
        }
        new WakeupReasonThread().start();
    }

    public void shutdown() {
        Slog.w("BatteryStats", "Writing battery stats before shutdown...");
        syncStats("shutdown", 31);
        synchronized (this.mStats) {
            this.mStats.shutdownLocked();
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteShutdown();
            }
        }
        this.mWorker.shutdown();
    }

    public static IBatteryStats getService() {
        if (sService != null) {
            return sService;
        }
        sService = asInterface(ServiceManager.getService("batterystats"));
        return sService;
    }

    public int getServiceType() {
        return 9;
    }

    public void onLowPowerModeChanged(PowerSaveState result) {
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(result.batterySaverEnabled);
        }
    }

    public BatteryStatsImpl getActiveStatistics() {
        return this.mStats;
    }

    public void scheduleWriteToDisk() {
        this.mWorker.scheduleWrite();
    }

    void removeUid(int uid) {
        synchronized (this.mStats) {
            this.mStats.removeUidStatsLocked(uid);
        }
    }

    void onCleanupUser(int userId) {
        synchronized (this.mStats) {
            this.mStats.onCleanupUserLocked(userId);
        }
    }

    void onUserRemoved(int userId) {
        synchronized (this.mStats) {
            this.mStats.onUserRemovedLocked(userId);
        }
    }

    void addIsolatedUid(int isolatedUid, int appUid) {
        synchronized (this.mStats) {
            this.mStats.addIsolatedUidLocked(isolatedUid, appUid);
        }
    }

    void removeIsolatedUid(int isolatedUid, int appUid) {
        synchronized (this.mStats) {
            this.mStats.scheduleRemoveIsolatedUidLocked(isolatedUid, appUid);
        }
    }

    void noteProcessStart(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessStartLocked(name, uid);
            StatsLog.write(28, uid, name, 1);
        }
    }

    void noteProcessCrash(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessCrashLocked(name, uid);
            StatsLog.write(28, uid, name, 2);
        }
    }

    void noteProcessAnr(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessAnrLocked(name, uid);
        }
    }

    void noteProcessFinish(String name, int uid) {
        synchronized (this.mStats) {
            this.mStats.noteProcessFinishLocked(name, uid);
            StatsLog.write(28, uid, name, 0);
        }
    }

    void noteUidProcessState(int uid, int state) {
        synchronized (this.mStats) {
            StatsLog.write(27, uid, ActivityManager.processStateAmToProto(state));
            try {
                this.mStats.noteUidProcessStateLocked(uid, state);
            } catch (RejectedExecutionException e) {
                Slog.w(TAG, "noteUidProcessStateLocked Failed.", e);
            }
        }
    }

    public byte[] getStatistics() {
        this.mContext.enforceCallingPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        return data;
    }

    public ParcelFileDescriptor getStatisticsStream() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        Parcel out = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(out, 0);
        }
        byte[] data = out.marshall();
        out.recycle();
        try {
            return ParcelFileDescriptor.fromData(data, "battery-stats");
        } catch (IOException e) {
            Slog.w(TAG, "Unable to create shared memory", e);
            return null;
        }
    }

    public boolean isCharging() {
        boolean isCharging;
        synchronized (this.mStats) {
            isCharging = this.mStats.isCharging();
        }
        return isCharging;
    }

    public long computeBatteryTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public long computeChargeTimeRemaining() {
        long j;
        synchronized (this.mStats) {
            long time = this.mStats.computeChargeTimeRemaining(SystemClock.elapsedRealtime());
            j = time >= 0 ? time / 1000 : time;
        }
        return j;
    }

    public void noteEvent(int code, String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteEventLocked(code, name, uid);
        }
        if (this.misBetaUser && code == 32771 && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoTopApp(name, uid);
            }
        }
    }

    public void noteSyncStart(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncStartLocked(name, uid);
            StatsLog.write_non_chained(7, uid, null, name, 1);
        }
    }

    public void noteSyncFinish(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncFinishLocked(name, uid);
            StatsLog.write_non_chained(7, uid, null, name, 0);
        }
    }

    public void noteJobStart(String name, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobStartLocked(name, uid);
            StatsLog.write_non_chained(8, uid, null, name, 1, -1);
        }
    }

    public void noteJobFinish(String name, int uid, int stopReason) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobFinishLocked(name, uid, stopReason);
            StatsLog.write_non_chained(8, uid, null, name, 0, stopReason);
        }
    }

    void noteJobsDeferred(int uid, int numDeferred, long sinceLast) {
        synchronized (this.mStats) {
            this.mStats.noteJobsDeferredLocked(uid, numDeferred, sinceLast);
        }
    }

    public void noteWakupAlarm(String name, int uid, WorkSource workSource, String tag) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakupAlarmLocked(name, uid, workSource, tag);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoStartAlarm(name, uid);
            }
        }
    }

    public void noteAlarmStart(String name, WorkSource workSource, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmStartLocked(name, workSource, uid);
        }
    }

    public void noteAlarmFinish(String name, WorkSource workSource, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmFinishLocked(name, workSource, uid);
        }
    }

    /* JADX WARNING: Missing block: B:28:0x004d, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void noteStartWakelock(int uid, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        int i;
        String str;
        Throwable th;
        enforceCallingPermission();
        IHwPowerInfoService iHwPowerInfoService = this.mStats;
        synchronized (iHwPowerInfoService) {
            try {
                i = this.mStats;
                str = uid;
                i.noteStartWakeLocked(str, pid, null, name, historyName, type, unimportantForLogging, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
            } finally {
                i = pid;
                str = name;
                while (true) {
                }
            }
        }
        if (!this.misBetaUser || this.mPowerInfoService == null) {
            i = pid;
            str = name;
            return;
        }
        iHwPowerInfoService = this.mPowerInfoService;
        synchronized (iHwPowerInfoService) {
            try {
                IHwPowerInfoService th2 = this.mPowerInfoService;
                th2.notePowerInfoAcquireWakeLock(str, i);
                return;
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    /* JADX WARNING: Missing block: B:28:0x004a, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void noteStopWakelock(int uid, int pid, String name, String historyName, int type) {
        int i;
        String str;
        Throwable th;
        enforceCallingPermission();
        IHwPowerInfoService iHwPowerInfoService = this.mStats;
        synchronized (iHwPowerInfoService) {
            try {
                i = this.mStats;
                str = uid;
                i.noteStopWakeLocked(str, pid, null, name, historyName, type, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
            } finally {
                i = pid;
                str = name;
                while (true) {
                }
            }
        }
        if (!this.misBetaUser || this.mPowerInfoService == null) {
            i = pid;
            str = name;
            return;
        }
        iHwPowerInfoService = this.mPowerInfoService;
        synchronized (iHwPowerInfoService) {
            try {
                IHwPowerInfoService th2 = this.mPowerInfoService;
                th2.notePowerInfoReleaseWakeLock(str, i);
                return;
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    public void noteStartWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartWakeFromSourceLocked(ws, pid, name, historyName, type, unimportantForLogging);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoAcquireWakeLock(name, pid);
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x005e, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void noteChangeWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) {
        int i;
        String str;
        int i2;
        String str2;
        Throwable th;
        enforceCallingPermission();
        IHwPowerInfoService iHwPowerInfoService = this.mStats;
        synchronized (iHwPowerInfoService) {
            try {
                i = this.mStats;
                str = ws;
                i2 = pid;
                str2 = name;
                i.noteChangeWakelockFromSourceLocked(str, i2, str2, historyName, type, newWs, newPid, newName, newHistoryName, newType, newUnimportantForLogging);
            } finally {
                i = pid;
                str = name;
                i2 = newPid;
                str2 = newName;
                while (true) {
                }
            }
        }
        if (!this.misBetaUser || this.mPowerInfoService == null) {
            i = pid;
            str = name;
            i2 = newPid;
            str2 = newName;
            return;
        }
        iHwPowerInfoService = this.mPowerInfoService;
        synchronized (iHwPowerInfoService) {
            try {
                IHwPowerInfoService th2 = this.mPowerInfoService;
                th2.notePowerInfoChangeWakeLock(str, i, str2, i2);
                return;
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    public void noteStopWakelockFromSource(WorkSource ws, int pid, String name, String historyName, int type) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopWakeFromSourceLocked(ws, pid, name, historyName, type);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoReleaseWakeLock(name, pid);
            }
        }
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStart(name, historyName, uid);
        }
    }

    public void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStartFromSource(name, historyName, workSource);
        }
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinish(name, historyName, uid);
        }
    }

    public void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinishFromSource(name, historyName, workSource);
        }
    }

    public void noteStartSensor(int uid, int sensor) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartSensorLocked(uid, sensor);
            StatsLog.write_non_chained(5, uid, null, sensor, 1);
        }
    }

    public void noteStopSensor(int uid, int sensor) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopSensorLocked(uid, sensor);
            StatsLog.write_non_chained(5, uid, null, sensor, 0);
        }
    }

    public void noteVibratorOn(int uid, long durationMillis) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOnLocked(uid, durationMillis);
        }
        HwServiceFactory.reportVibratorToIAware(uid);
    }

    public void noteVibratorOff(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOffLocked(uid);
        }
    }

    public void noteGpsChanged(WorkSource oldWs, WorkSource newWs) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteGpsChangedLocked(oldWs, newWs);
        }
    }

    public void noteGpsSignalQuality(int signalLevel) {
        synchronized (this.mStats) {
            this.mStats.noteGpsSignalQualityLocked(signalLevel);
        }
    }

    public void noteScreenState(int state) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(29, state);
            this.mStats.noteScreenStateLocked(state);
        }
        boolean z = 3 == SystemProperties.getInt("ro.logsystem.usertype", 0) || 5 == SystemProperties.getInt("ro.logsystem.usertype", 0) || SystemProperties.getBoolean("persist.sys.huawei.debug.on", false) || SystemProperties.getBoolean("hwlog.remotedebug", false);
        this.misBetaUser = z;
        if (this.misBetaUser && !this.mIsPowerInfoStatus) {
            Slog.i(TAG, "getHwPowerInfoService instance");
            this.mPowerInfoService = HwServiceFactory.getHwPowerInfoService(this.mContext, true);
            if (this.mPowerInfoService != null) {
                this.mIsPowerInfoStatus = true;
            }
        } else if (!this.misBetaUser && this.mIsPowerInfoStatus) {
            this.mIsPowerInfoStatus = false;
            Slog.i(TAG, "getHwPowerInfoService uninstance");
            this.mPowerInfoService = HwServiceFactory.getHwPowerInfoService(this.mContext, false);
        }
    }

    public void noteScreenBrightness(int brightness) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(9, brightness);
            this.mStats.noteScreenBrightnessLocked(brightness);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoBrightness(brightness);
            }
        }
    }

    public void noteUserActivity(int uid, int event) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteUserActivityLocked(uid, event);
        }
    }

    public void noteWakeUp(String reason, int reasonUid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakeUpLocked(reason, reasonUid);
        }
    }

    public void noteInteractive(boolean interactive) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteInteractiveLocked(interactive);
        }
    }

    public void noteConnectivityChanged(int type, String extra) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteConnectivityChangedLocked(type, extra);
        }
    }

    public void noteMobileRadioPowerState(int powerState, long timestampNs, int uid) {
        boolean update;
        enforceCallingPermission();
        synchronized (this.mStats) {
            update = this.mStats.noteMobileRadioPowerStateLocked(powerState, timestampNs, uid);
        }
        if (update) {
            this.mWorker.scheduleSync("modem-data", 4);
        }
    }

    public void notePhoneOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOnLocked();
        }
    }

    public void notePhoneOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOffLocked();
        }
    }

    public void notePhoneSignalStrength(SignalStrength signalStrength) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneSignalStrengthLocked(signalStrength);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoSignalStrength(signalStrength.getLevel());
            }
        }
    }

    public void notePhoneDataConnectionState(int dataType, boolean hasData) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneDataConnectionStateLocked(dataType, hasData);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoConnectionState(dataType, hasData);
            }
        }
    }

    public void notePhoneState(int state) {
        enforceCallingPermission();
        int simState = TelephonyManager.getDefault().getSimState();
        synchronized (this.mStats) {
            this.mStats.notePhoneStateLocked(state, simState);
        }
    }

    public void noteWifiOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOnLocked();
        }
    }

    public void noteWifiOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOffLocked();
        }
    }

    public void noteStartAudio(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOnLocked(uid);
            StatsLog.write_non_chained(23, uid, null, 1);
        }
    }

    public void noteStopAudio(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOffLocked(uid);
            StatsLog.write_non_chained(23, uid, null, 0);
        }
    }

    public void noteStartVideo(int uid) {
        LogPower.push(NetworkConstants.ICMPV6_NEIGHBOR_ADVERTISEMENT, Integer.toString(Binder.getCallingUid()));
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOnLocked(uid);
            StatsLog.write_non_chained(24, uid, null, 1);
        }
    }

    public void noteStopVideo(int uid) {
        LogPower.push(137, Integer.toString(Binder.getCallingUid()));
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOffLocked(uid);
            StatsLog.write_non_chained(24, uid, null, 0);
        }
    }

    public void noteResetAudio() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetAudioLocked();
            StatsLog.write_non_chained(23, -1, null, 2);
        }
    }

    public void noteResetVideo() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetVideoLocked();
            StatsLog.write_non_chained(24, -1, null, 2);
        }
    }

    public void noteFlashlightOn(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOnLocked(uid);
            StatsLog.write_non_chained(26, uid, null, 1);
        }
    }

    public void noteFlashlightOff(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOffLocked(uid);
            StatsLog.write_non_chained(26, uid, null, 0);
        }
    }

    public void noteStartCamera(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOnLocked(uid);
            StatsLog.write_non_chained(25, uid, null, 1);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteStartCamera();
            }
        }
        this.mActivityManagerService.reportCamera(uid, 1);
        LogPower.push(204, null, String.valueOf(uid), String.valueOf(1));
    }

    public void noteStopCamera(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOffLocked(uid);
            StatsLog.write_non_chained(25, uid, null, 0);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.noteStopCamera();
            }
        }
        this.mActivityManagerService.reportCamera(uid, 0);
        LogPower.push(205, null, String.valueOf(uid), String.valueOf(1));
    }

    public void noteResetCamera() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetCameraLocked();
            StatsLog.write_non_chained(25, -1, null, 2);
        }
    }

    public void noteResetFlashlight() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetFlashlightLocked();
            StatsLog.write_non_chained(26, -1, null, 2);
        }
    }

    public void noteWifiRadioPowerState(int powerState, long tsNanos, int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            if (this.mStats.isOnBattery()) {
                String type;
                BatteryExternalStatsWorker batteryExternalStatsWorker;
                StringBuilder stringBuilder;
                if (powerState != 3) {
                    if (powerState != 2) {
                        type = "inactive";
                        batteryExternalStatsWorker = this.mWorker;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("wifi-data: ");
                        stringBuilder.append(type);
                        batteryExternalStatsWorker.scheduleSync(stringBuilder.toString(), 2);
                    }
                }
                type = "active";
                batteryExternalStatsWorker = this.mWorker;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wifi-data: ");
                stringBuilder.append(type);
                batteryExternalStatsWorker.scheduleSync(stringBuilder.toString(), 2);
            }
            this.mStats.noteWifiRadioPowerState(powerState, tsNanos, uid);
        }
    }

    public void noteWifiRunning(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningLocked(ws);
        }
    }

    public void noteWifiRunningChanged(WorkSource oldWs, WorkSource newWs) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningChangedLocked(oldWs, newWs);
        }
    }

    public void noteWifiStopped(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStoppedLocked(ws);
        }
    }

    public void noteWifiState(int wifiState, String accessPoint) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStateLocked(wifiState, accessPoint);
        }
    }

    public void noteWifiSupplicantStateChanged(int supplState, boolean failedAuth) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiSupplicantStateChangedLocked(supplState, failedAuth);
        }
        if (this.misBetaUser && this.mPowerInfoService != null) {
            synchronized (this.mPowerInfoService) {
                this.mPowerInfoService.notePowerInfoWifiState(supplState);
            }
        }
    }

    public void noteWifiRssiChanged(int newRssi) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRssiChangedLocked(newRssi);
        }
    }

    public void noteFullWifiLockAcquired(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredLocked(uid);
        }
    }

    public void noteFullWifiLockReleased(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedLocked(uid);
        }
    }

    public void noteWifiScanStarted(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedLocked(uid);
        }
    }

    public void noteWifiScanStopped(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedLocked(uid);
        }
    }

    public void noteWifiMulticastEnabled(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastEnabledLocked(uid);
        }
    }

    public void noteWifiMulticastDisabled(int uid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastDisabledLocked(uid);
        }
    }

    public void noteFullWifiLockAcquiredFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredFromSourceLocked(ws);
        }
    }

    public void noteFullWifiLockReleasedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedFromSourceLocked(ws);
        }
    }

    public void noteWifiScanStartedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            for (int i = 0; i < ws.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uid=");
                stringBuilder.append(ws.get(i));
                HwLog.dubaie("DUBAI_TAG_WIFI_SCAN_STARTED", stringBuilder.toString());
            }
            this.mStats.noteWifiScanStartedFromSourceLocked(ws);
        }
    }

    public void noteWifiScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedFromSourceLocked(ws);
        }
    }

    public void noteWifiBatchedScanStartedFromSource(WorkSource ws, int csph) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStartedFromSourceLocked(ws, csph);
        }
    }

    public void noteWifiBatchedScanStoppedFromSource(WorkSource ws) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStoppedFromSourceLocked(ws);
        }
    }

    public void noteNetworkInterfaceType(String iface, int networkType) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteNetworkInterfaceTypeLocked(iface, networkType);
        }
    }

    public void noteNetworkStatsEnabled() {
        enforceCallingPermission();
        this.mWorker.scheduleSync("network-stats-enabled", 6);
    }

    public void noteDeviceIdleMode(int mode, String activeReason, int activeUid) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteDeviceIdleModeLocked(mode, activeReason, activeUid);
        }
    }

    public void notePackageInstalled(String pkgName, long versionCode) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageInstalledLocked(pkgName, versionCode);
        }
    }

    public void notePackageUninstalled(String pkgName) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageUninstalledLocked(pkgName);
        }
    }

    public void noteBleScanStarted(WorkSource ws, boolean isUnoptimized) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStartedFromSourceLocked(ws, isUnoptimized);
        }
    }

    public void noteBleScanStopped(WorkSource ws, boolean isUnoptimized) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStoppedFromSourceLocked(ws, isUnoptimized);
        }
    }

    public void noteResetBleScan() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetBluetoothScanLocked();
        }
    }

    public void noteBleScanResults(WorkSource ws, int numNewResults) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanResultsFromSourceLocked(ws, numNewResults);
        }
    }

    public void noteWifiControllerActivity(WifiActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid wifi data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mStats.updateWifiState(info);
    }

    public void noteBluetoothControllerActivity(BluetoothActivityEnergyInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid bluetooth data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mStats) {
            this.mStats.updateBluetoothStateLocked(info);
        }
    }

    public void noteModemControllerActivity(ModemActivityInfo info) {
        enforceCallingPermission();
        if (info == null || !info.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid modem data given: ");
            stringBuilder.append(info);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mStats.updateMobileRadioState(info);
    }

    public boolean isOnBattery() {
        return this.mStats.isOnBattery();
    }

    public void setBatteryState(int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        enforceCallingPermission();
        this.mWorker.scheduleRunnable(new -$$Lambda$BatteryStatsService$ZxbqtJ7ozYmzYFkkNV3m_QRd0Sk(this, plugType, status, health, level, temp, volt, chargeUAh, chargeFullUAh));
    }

    /* JADX WARNING: Missing block: B:27:0x004b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$setBatteryState$1(BatteryStatsService batteryStatsService, int plugType, int status, int health, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        Throwable th;
        BatteryStatsService batteryStatsService2 = batteryStatsService;
        synchronized (batteryStatsService2.mStats) {
            int i;
            int i2;
            try {
                if (batteryStatsService2.mStats.isOnBattery() == BatteryStatsImpl.isOnBattery(plugType, status)) {
                    batteryStatsService2.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh);
                    if (!batteryStatsService2.misBetaUser || batteryStatsService2.mPowerInfoService == null) {
                        i = plugType;
                        i2 = level;
                    } else {
                        synchronized (batteryStatsService2.mPowerInfoService) {
                            try {
                                batteryStatsService2.mPowerInfoService.notePowerInfoBatteryState(plugType, level);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                }
                i = plugType;
                i2 = level;
                batteryStatsService2.mWorker.scheduleSync("battery-state", 31);
                batteryStatsService2.mWorker.scheduleRunnable(new -$$Lambda$BatteryStatsService$rRONgIFHr4sujxPESRmo9P5RJ6w(batteryStatsService2, status, health, i, i2, temp, volt, chargeUAh, chargeFullUAh));
            } catch (Throwable th3) {
                th = th3;
                i = plugType;
                i2 = level;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:25:0x0037, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$setBatteryState$0(BatteryStatsService batteryStatsService, int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        Throwable th;
        BatteryStatsService batteryStatsService2 = batteryStatsService;
        synchronized (batteryStatsService2.mStats) {
            int i;
            int i2;
            try {
                batteryStatsService2.mStats.setBatteryStateLocked(status, health, plugType, level, temp, volt, chargeUAh, chargeFullUAh);
                if (!batteryStatsService2.misBetaUser || batteryStatsService2.mPowerInfoService == null) {
                    i = plugType;
                    i2 = level;
                } else {
                    synchronized (batteryStatsService2.mPowerInfoService) {
                        try {
                            batteryStatsService2.mPowerInfoService.notePowerInfoBatteryState(plugType, level);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public long getAwakeTimeBattery() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimeBattery();
    }

    public long getAwakeTimePlugged() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimePlugged();
    }

    public void enforceCallingPermission() {
        if (Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    private void dumpHelp(PrintWriter pw) {
        pw.println("Battery stats (batterystats) dump options:");
        pw.println("  [--checkin] [--proto] [--history] [--history-start] [--charged] [-c]");
        pw.println("  [--daily] [--reset] [--write] [--new-daily] [--read-daily] [-h] [<package.name>]");
        pw.println("  --checkin: generate output for a checkin report; will write (and clear) the");
        pw.println("             last old completed stats when they had been reset.");
        pw.println("  -c: write the current stats in checkin format.");
        pw.println("  --proto: write the current aggregate stats (without history) in proto format.");
        pw.println("  --history: show only history data.");
        pw.println("  --history-start <num>: show only history data starting at given time offset.");
        pw.println("  --charged: only output data since last charged.");
        pw.println("  --daily: only output full daily data.");
        pw.println("  --reset: reset the stats, clearing all current data.");
        pw.println("  --write: force write current collected stats to disk.");
        pw.println("  --new-daily: immediately create and write new daily stats record.");
        pw.println("  --read-daily: read-load last written daily stats.");
        pw.println("  --settings: dump the settings key/values related to batterystats");
        pw.println("  --cpu: dump cpu stats for debugging purpose");
        pw.println("  <package.name>: optional name of package to filter output by.");
        pw.println("  -h: print this help text.");
        pw.println("Battery stats (batterystats) commands:");
        pw.println("  enable|disable <option>");
        pw.println("    Enable or disable a running option.  Option state is not saved across boots.");
        pw.println("    Options are:");
        pw.println("      full-history: include additional detailed events in battery history:");
        pw.println("          wake_lock_in, alarms and proc events");
        pw.println("      no-auto-reset: don't automatically reset stats when unplugged");
        pw.println("      pretend-screen-off: pretend the screen is off, even if screen state changes");
    }

    private void dumpSettings(PrintWriter pw) {
        synchronized (this.mStats) {
            this.mStats.dumpConstantsLocked(pw);
        }
    }

    private void dumpCpuStats(PrintWriter pw) {
        synchronized (this.mStats) {
            this.mStats.dumpCpuStatsLocked(pw);
        }
    }

    private int doEnableOrDisable(PrintWriter pw, int i, String[] args, boolean enable) {
        i++;
        StringBuilder stringBuilder;
        if (i >= args.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Missing option argument for ");
            stringBuilder.append(enable ? "--enable" : "--disable");
            pw.println(stringBuilder.toString());
            dumpHelp(pw);
            return -1;
        }
        if ("full-wake-history".equals(args[i]) || "full-history".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setRecordAllHistoryLocked(enable);
            }
        } else if ("no-auto-reset".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setNoAutoReset(enable);
            }
        } else if ("pretend-screen-off".equals(args[i])) {
            synchronized (this.mStats) {
                this.mStats.setPretendScreenOff(enable);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown enable/disable option: ");
            stringBuilder.append(args[i]);
            pw.println(stringBuilder.toString());
            dumpHelp(pw);
            return -1;
        }
        return i;
    }

    /* JADX WARNING: Removed duplicated region for block: B:224:0x03b6 A:{SYNTHETIC, Splitter:B:224:0x03b6} */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x03b6 A:{SYNTHETIC, Splitter:B:224:0x03b6} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x02df A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x02df A:{SYNTHETIC} */
    /* JADX WARNING: Missing block: B:41:0x00b4, code skipped:
            r3 = r19;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        long ident;
        Exception e;
        String str;
        StringBuilder stringBuilder;
        boolean z;
        long j;
        Throwable th;
        StringBuilder stringBuilder2;
        BatteryStatsService batteryStatsService = this;
        PrintWriter printWriter = pw;
        String[] strArr = args;
        if (DumpUtils.checkDumpAndUsageStatsPermission(batteryStatsService.mContext, TAG, printWriter)) {
            long historyStart;
            int useCheckinFormat;
            boolean useCheckinFormat2;
            boolean toProto;
            boolean isRealCheckin;
            long historyStart2;
            boolean writeData;
            int reqUid;
            boolean noOutput;
            boolean noOutput2 = false;
            int reqUid2 = -1;
            if (strArr != null) {
                long historyStart3 = -1;
                historyStart = 0;
                boolean writeData2 = false;
                boolean isRealCheckin2 = false;
                boolean toProto2 = false;
                useCheckinFormat = false;
                int i = 0;
                loop0:
                while (true) {
                    int i2 = i;
                    if (i2 >= strArr.length) {
                        useCheckinFormat2 = toProto2;
                        toProto = isRealCheckin2;
                        isRealCheckin = writeData2;
                        historyStart2 = historyStart3;
                        writeData = historyStart;
                        reqUid = reqUid2;
                        noOutput = noOutput2;
                        break loop0;
                    }
                    String arg = strArr[i2];
                    if ("--checkin".equals(arg)) {
                        toProto2 = true;
                        writeData2 = true;
                    } else if ("--history".equals(arg)) {
                        useCheckinFormat |= 8;
                    } else if ("--history-start".equals(arg)) {
                        useCheckinFormat |= 8;
                        i2++;
                        if (i2 >= strArr.length) {
                            printWriter.println("Missing time argument for --history-since");
                            batteryStatsService.dumpHelp(printWriter);
                            return;
                        }
                        historyStart3 = Long.parseLong(strArr[i2]);
                        historyStart = 1;
                    } else if ("-c".equals(arg)) {
                        toProto2 = true;
                        useCheckinFormat |= 16;
                    } else if (PriorityDump.PROTO_ARG.equals(arg)) {
                        isRealCheckin2 = true;
                    } else if ("--charged".equals(arg)) {
                        useCheckinFormat |= 2;
                    } else if ("--daily".equals(arg)) {
                        useCheckinFormat |= 4;
                    } else if ("--reset".equals(arg)) {
                        synchronized (batteryStatsService.mStats) {
                            try {
                                batteryStatsService.mStats.resetAllStatsCmdLocked();
                                printWriter.println("Battery stats reset.");
                                noOutput2 = true;
                            } finally {
                                useCheckinFormat2 = toProto2;
                                while (true) {
                                }
                                i = 1 + i2;
                            }
                        }
                        BatteryExternalStatsWorker batteryExternalStatsWorker = batteryStatsService.mWorker;
                        String str2 = "dump";
                    } else {
                        useCheckinFormat2 = toProto2;
                        if ("--write".equals(arg)) {
                            batteryStatsService.syncStats("dump", true);
                            synchronized (batteryStatsService.mStats) {
                                batteryStatsService.mStats.writeSyncLocked();
                                printWriter.println("Battery stats written.");
                                noOutput2 = true;
                            }
                        } else if ("--new-daily".equals(arg)) {
                            synchronized (batteryStatsService.mStats) {
                                batteryStatsService.mStats.recordDailyStatsLocked();
                                printWriter.println("New daily stats written.");
                                noOutput2 = true;
                            }
                        } else if ("--read-daily".equals(arg)) {
                            synchronized (batteryStatsService.mStats) {
                                batteryStatsService.mStats.readDailyStatsLocked();
                                printWriter.println("Last daily stats read.");
                                noOutput2 = true;
                            }
                        } else if ("--enable".equals(arg) || "enable".equals(arg)) {
                            i = batteryStatsService.doEnableOrDisable(printWriter, i2, strArr, 1);
                            if (i >= 0) {
                                toProto2 = new StringBuilder();
                                toProto2.append("Enabled: ");
                                toProto2.append(strArr[i]);
                                printWriter.println(toProto2.toString());
                                return;
                            }
                            return;
                        } else if ("--disable".equals(arg) || "disable".equals(arg)) {
                            i = batteryStatsService.doEnableOrDisable(printWriter, i2, strArr, 0);
                            if (i >= 0) {
                                toProto2 = new StringBuilder();
                                toProto2.append("Disabled: ");
                                toProto2.append(strArr[i]);
                                printWriter.println(toProto2.toString());
                                return;
                            }
                            return;
                        } else if ("-h".equals(arg)) {
                            batteryStatsService.dumpHelp(printWriter);
                            return;
                        } else if ("--settings".equals(arg)) {
                            batteryStatsService.dumpSettings(printWriter);
                            return;
                        } else if ("--cpu".equals(arg)) {
                            batteryStatsService.dumpCpuStats(printWriter);
                            return;
                        } else if ("-a".equals(arg)) {
                            useCheckinFormat |= 32;
                        } else if (arg.length() <= 0 || !arg.charAt(0)) {
                            try {
                                reqUid2 = batteryStatsService.mContext.getPackageManager().getPackageUidAsUser(arg, UserHandle.getCallingUserId());
                            } catch (NameNotFoundException e2) {
                                toProto2 = new StringBuilder();
                                toProto2.append("Unknown package: ");
                                toProto2.append(arg);
                                printWriter.println(toProto2.toString());
                                batteryStatsService.dumpHelp(printWriter);
                                return;
                            }
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unknown option: ");
                            stringBuilder3.append(arg);
                            printWriter.println(stringBuilder3.toString());
                            batteryStatsService.dumpHelp(printWriter);
                            return;
                        }
                    }
                    i = 1 + i2;
                }
            } else {
                useCheckinFormat2 = false;
                toProto = false;
                isRealCheckin = false;
                writeData = false;
                historyStart2 = -1;
                reqUid = -1;
                useCheckinFormat = 0;
                noOutput = false;
            }
            if (!noOutput) {
                historyStart = Binder.clearCallingIdentity();
                try {
                    int flags;
                    if (BatteryStatsHelper.checkWifiOnly(batteryStatsService.mContext)) {
                        useCheckinFormat |= 64;
                    }
                    batteryStatsService.syncStats("dump", 31);
                    Binder.restoreCallingIdentity(historyStart);
                    if (reqUid < 0 || (useCheckinFormat & 10) != 0) {
                        flags = useCheckinFormat;
                    } else {
                        flags = (useCheckinFormat | 2) & -17;
                    }
                    byte[] raw;
                    if (toProto) {
                        List<ApplicationInfo> apps = batteryStatsService.mContext.getPackageManager().getInstalledApplications(4325376);
                        if (isRealCheckin) {
                            synchronized (batteryStatsService.mStats.mCheckinFile) {
                                try {
                                    if (batteryStatsService.mStats.mCheckinFile.exists()) {
                                        try {
                                            raw = batteryStatsService.mStats.mCheckinFile.readFully();
                                            if (raw != null) {
                                                Parcel in = Parcel.obtain();
                                                in.unmarshall(raw, 0, raw.length);
                                                in.setDataPosition(0);
                                                ident = historyStart;
                                                try {
                                                    BatteryStatsImpl checkinStats = new BatteryStatsImpl(null, batteryStatsService.mStats.mHandler, null, batteryStatsService.mUserManagerUserInfoProvider);
                                                    checkinStats.readSummaryFromParcel(in);
                                                    in.recycle();
                                                    checkinStats.dumpProtoLocked(batteryStatsService.mContext, fd, apps, flags, historyStart2);
                                                    batteryStatsService.mStats.mCheckinFile.delete();
                                                    return;
                                                } catch (ParcelFormatException | IOException e3) {
                                                    e = e3;
                                                    str = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Failure reading checkin file ");
                                                    stringBuilder.append(batteryStatsService.mStats.mCheckinFile.getBaseFile());
                                                    Slog.w(str, stringBuilder.toString(), e);
                                                    synchronized (batteryStatsService.mStats) {
                                                    }
                                                    z = noOutput;
                                                    j = ident;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            }
                                            ident = historyStart;
                                        } catch (ParcelFormatException | IOException e4) {
                                            e = e4;
                                            ident = historyStart;
                                            str = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Failure reading checkin file ");
                                            stringBuilder.append(batteryStatsService.mStats.mCheckinFile.getBaseFile());
                                            Slog.w(str, stringBuilder.toString(), e);
                                            synchronized (batteryStatsService.mStats) {
                                            }
                                            z = noOutput;
                                            j = ident;
                                        }
                                    }
                                    ident = historyStart;
                                } catch (Throwable th3) {
                                    th = th3;
                                    ident = historyStart;
                                    throw th;
                                }
                            }
                        }
                        ident = historyStart;
                        synchronized (batteryStatsService.mStats) {
                            batteryStatsService.mStats.dumpProtoLocked(batteryStatsService.mContext, fd, apps, flags, historyStart2);
                            if (writeData) {
                                batteryStatsService.mStats.writeAsyncLocked();
                            }
                        }
                        z = noOutput;
                        j = ident;
                    } else {
                        ident = historyStart;
                        BatteryStatsImpl batteryStatsImpl;
                        BatteryStatsImpl batteryStatsImpl2;
                        if (useCheckinFormat2) {
                            List<ApplicationInfo> apps2 = batteryStatsService.mContext.getPackageManager().getInstalledApplications(4325376);
                            if (isRealCheckin) {
                                AtomicFile atomicFile = batteryStatsService.mStats.mCheckinFile;
                                synchronized (atomicFile) {
                                    AtomicFile atomicFile2;
                                    try {
                                        if (batteryStatsService.mStats.mCheckinFile.exists()) {
                                            try {
                                                raw = batteryStatsService.mStats.mCheckinFile.readFully();
                                                if (raw != null) {
                                                    Parcel in2 = Parcel.obtain();
                                                    in2.unmarshall(raw, 0, raw.length);
                                                    in2.setDataPosition(0);
                                                    BatteryStatsImpl checkinStats2 = new BatteryStatsImpl(null, batteryStatsService.mStats.mHandler, null, batteryStatsService.mUserManagerUserInfoProvider);
                                                    checkinStats2.readSummaryFromParcel(in2);
                                                    in2.recycle();
                                                    batteryStatsImpl = checkinStats2;
                                                    atomicFile2 = atomicFile;
                                                    try {
                                                        checkinStats2.dumpCheckinLocked(batteryStatsService.mContext, printWriter, apps2, flags, historyStart2);
                                                        batteryStatsService.mStats.mCheckinFile.delete();
                                                        return;
                                                    } catch (ParcelFormatException | IOException e5) {
                                                        e = e5;
                                                        str = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Failure reading checkin file ");
                                                        stringBuilder2.append(batteryStatsService.mStats.mCheckinFile.getBaseFile());
                                                        Slog.w(str, stringBuilder2.toString(), e);
                                                        batteryStatsImpl2 = batteryStatsService.mStats;
                                                        synchronized (batteryStatsImpl2) {
                                                        }
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        throw th;
                                                    }
                                                }
                                                atomicFile2 = atomicFile;
                                                z = noOutput;
                                                j = ident;
                                            } catch (ParcelFormatException | IOException e6) {
                                                e = e6;
                                                atomicFile2 = atomicFile;
                                                z = noOutput;
                                                j = ident;
                                                str = TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Failure reading checkin file ");
                                                stringBuilder2.append(batteryStatsService.mStats.mCheckinFile.getBaseFile());
                                                Slog.w(str, stringBuilder2.toString(), e);
                                                batteryStatsImpl2 = batteryStatsService.mStats;
                                                synchronized (batteryStatsImpl2) {
                                                }
                                            }
                                        } else {
                                            atomicFile2 = atomicFile;
                                            z = noOutput;
                                            j = ident;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        atomicFile2 = atomicFile;
                                        z = noOutput;
                                        j = ident;
                                        throw th;
                                    }
                                }
                            }
                            j = ident;
                            batteryStatsImpl2 = batteryStatsService.mStats;
                            synchronized (batteryStatsImpl2) {
                                try {
                                    batteryStatsImpl = batteryStatsImpl2;
                                    batteryStatsService.mStats.dumpCheckinLocked(batteryStatsService.mContext, printWriter, apps2, flags, historyStart2);
                                    if (writeData) {
                                        batteryStatsService.mStats.writeAsyncLocked();
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    throw th;
                                }
                            }
                        }
                        j = ident;
                        batteryStatsImpl2 = batteryStatsService.mStats;
                        synchronized (batteryStatsImpl2) {
                            try {
                                batteryStatsImpl = batteryStatsImpl2;
                                batteryStatsService.mStats.dumpLocked(batteryStatsService.mContext, printWriter, flags, reqUid, historyStart2);
                                if (writeData) {
                                    batteryStatsService.mStats.writeAsyncLocked();
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                throw th;
                            }
                        }
                    }
                } catch (Throwable th8) {
                    z = noOutput;
                    Binder.restoreCallingIdentity(historyStart);
                }
            }
        }
    }

    public CellularBatteryStats getCellularBatteryStats() {
        CellularBatteryStats cellularBatteryStats;
        synchronized (this.mStats) {
            cellularBatteryStats = this.mStats.getCellularBatteryStats();
        }
        return cellularBatteryStats;
    }

    public WifiBatteryStats getWifiBatteryStats() {
        WifiBatteryStats wifiBatteryStats;
        synchronized (this.mStats) {
            wifiBatteryStats = this.mStats.getWifiBatteryStats();
        }
        return wifiBatteryStats;
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats gpsBatteryStats;
        synchronized (this.mStats) {
            gpsBatteryStats = this.mStats.getGpsBatteryStats();
        }
        return gpsBatteryStats;
    }

    public HealthStatsParceler takeUidSnapshot(int requestUid) {
        if (requestUid != Binder.getCallingUid()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            HealthStatsParceler healthStatsForUidLocked;
            if (shouldCollectExternalStats()) {
                syncStats("get-health-stats-for-uids", 31);
            }
            synchronized (this.mStats) {
                healthStatsForUidLocked = getHealthStatsForUidLocked(requestUid);
            }
            Binder.restoreCallingIdentity(ident);
            return healthStatsForUidLocked;
        } catch (Exception ex) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Crashed while writing for takeUidSnapshot(");
                stringBuilder.append(requestUid);
                stringBuilder.append(")");
                Slog.w(str, stringBuilder.toString(), ex);
                throw ex;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public HealthStatsParceler[] takeUidSnapshots(int[] requestUids) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.BATTERYSTATS_TAKEUIDSNAPSHOTS);
        if (!onlyCaller(requestUids)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            HealthStatsParceler[] results;
            if (shouldCollectExternalStats()) {
                syncStats("get-health-stats-for-uids", 31);
            }
            synchronized (this.mStats) {
                int N = requestUids.length;
                results = new HealthStatsParceler[N];
                for (int i = 0; i < N; i++) {
                    results[i] = getHealthStatsForUidLocked(requestUids[i]);
                }
            }
            Binder.restoreCallingIdentity(ident);
            return results;
        } catch (Exception ex) {
            try {
                throw ex;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private boolean shouldCollectExternalStats() {
        return SystemClock.elapsedRealtime() - this.mWorker.getLastCollectionTimeStamp() > this.mStats.getExternalStatsCollectionRateLimitMs();
    }

    private static boolean onlyCaller(int[] requestUids) {
        int caller = Binder.getCallingUid();
        for (int i : requestUids) {
            if (i != caller) {
                return false;
            }
        }
        return true;
    }

    HealthStatsParceler getHealthStatsForUidLocked(int requestUid) {
        HealthStatsBatteryStatsWriter writer = new HealthStatsBatteryStatsWriter();
        HealthStatsWriter uidWriter = new HealthStatsWriter(UidHealthStats.CONSTANTS);
        Uid uid = (Uid) this.mStats.getUidStats().get(requestUid);
        if (uid != null) {
            writer.writeUid(uidWriter, this.mStats, uid);
        }
        return new HealthStatsParceler(uidWriter);
    }

    protected void setActivityService(ActivityManagerService ams) {
        this.mActivityManagerService = ams;
    }
}
