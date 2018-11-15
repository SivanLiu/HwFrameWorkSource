package com.android.server.pm;

import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.HwServiceExFactory;
import com.android.server.LocalServices;
import com.android.server.PinnerService;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundDexOptService extends JobService implements IHwBackgroundDexOptInner {
    private static final boolean DEBUG = false;
    private static final long IDLE_OPTIMIZATION_PERIOD = TimeUnit.DAYS.toMillis(1);
    private static final int JOB_IDLE_OPTIMIZE = 800;
    private static final int JOB_POST_BOOT_UPDATE = 801;
    private static final int LOW_THRESHOLD_MULTIPLIER_FOR_DOWNGRADE = 2;
    private static final int OPTIMIZE_ABORT_BY_JOB_SCHEDULER = 2;
    private static final int OPTIMIZE_ABORT_NO_SPACE_LEFT = 3;
    private static final int OPTIMIZE_CONTINUE = 1;
    private static final int OPTIMIZE_PROCESSED = 0;
    private static final String TAG = "BackgroundDexOptService";
    private static final long mDowngradeUnusedAppsThresholdInMillis = getDowngradeUnusedAppsThresholdInMillis();
    private static ComponentName sDexoptServiceName = new ComponentName("android", BackgroundDexOptService.class.getName());
    static final ArraySet<String> sFailedPackageNamesPrimary = new ArraySet();
    static final ArraySet<String> sFailedPackageNamesSecondary = new ArraySet();
    private final AtomicBoolean mAbortIdleOptimization = new AtomicBoolean(false);
    private final AtomicBoolean mAbortPostBootUpdate = new AtomicBoolean(false);
    private final File mDataDir = Environment.getDataDirectory();
    private final AtomicBoolean mExitPostBootUpdate = new AtomicBoolean(false);
    IHwBackgroundDexOptServiceEx mHwBDOSEx = null;

    public static void schedule(Context context) {
        if (!isBackgroundDexoptDisabled()) {
            JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
            js.schedule(new Builder(JOB_POST_BOOT_UPDATE, sDexoptServiceName).setMinimumLatency(TimeUnit.MINUTES.toMillis(1)).setOverrideDeadline(TimeUnit.MINUTES.toMillis(1)).build());
            js.schedule(new Builder(JOB_IDLE_OPTIMIZE, sDexoptServiceName).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(IDLE_OPTIMIZATION_PERIOD).build());
        }
    }

    public static void notifyPackageChanged(String packageName) {
        synchronized (sFailedPackageNamesPrimary) {
            sFailedPackageNamesPrimary.remove(packageName);
        }
        synchronized (sFailedPackageNamesSecondary) {
            sFailedPackageNamesSecondary.remove(packageName);
        }
    }

    private int getBatteryLevel() {
        Intent intent = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int level = intent.getIntExtra("level", -1);
        int scale = intent.getIntExtra("scale", -1);
        if (level < 0 || scale <= 0) {
            return 0;
        }
        return (level * 100) / scale;
    }

    private long getLowStorageThreshold(Context context) {
        long lowThreshold = StorageManager.from(context).getStorageLowBytes(this.mDataDir);
        if (lowThreshold == 0) {
            Log.e(TAG, "Invalid low storage threshold");
        }
        return lowThreshold;
    }

    private boolean runPostBootUpdate(JobParameters jobParams, PackageManagerService pm, ArraySet<String> pkgs) {
        if (this.mExitPostBootUpdate.get()) {
            return false;
        }
        final JobParameters jobParameters = jobParams;
        final PackageManagerService packageManagerService = pm;
        final ArraySet<String> arraySet = pkgs;
        new Thread("BackgroundDexOptService_PostBootUpdate") {
            public void run() {
                BackgroundDexOptService.this.postBootUpdate(jobParameters, packageManagerService, arraySet);
            }
        }.start();
        return true;
    }

    private void postBootUpdate(JobParameters jobParams, PackageManagerService pm, ArraySet<String> pkgs) {
        int lowBatteryThreshold = getResources().getInteger(17694804);
        long lowThreshold = getLowStorageThreshold(this);
        this.mAbortPostBootUpdate.set(false);
        ArraySet<String> updatedPackages = new ArraySet();
        for (String pkg : pkgs) {
            if (!this.mAbortPostBootUpdate.get()) {
                if (this.mExitPostBootUpdate.get() || getBatteryLevel() < lowBatteryThreshold) {
                    break;
                }
                long usableSpace = this.mDataDir.getUsableSpace();
                if (usableSpace < lowThreshold) {
                    Log.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
                    break;
                } else if (pm.performDexOptWithStatus(new DexoptOptions(pkg, 1, 4)) == 1) {
                    updatedPackages.add(pkg);
                }
            } else {
                return;
            }
        }
        notifyPinService(updatedPackages);
        jobFinished(jobParams, false);
    }

    private boolean runIdleOptimization(JobParameters jobParams, PackageManagerService pm, ArraySet<String> pkgs) {
        final PackageManagerService packageManagerService = pm;
        final ArraySet<String> arraySet = pkgs;
        final JobParameters jobParameters = jobParams;
        new Thread("BackgroundDexOptService_IdleOptimization") {
            public void run() {
                if (BackgroundDexOptService.this.idleOptimization(packageManagerService, arraySet, BackgroundDexOptService.this) != 2) {
                    Log.w(BackgroundDexOptService.TAG, "Idle optimizations aborted because of space constraints.");
                    BackgroundDexOptService.this.jobFinished(jobParameters, false);
                }
                Log.i(BackgroundDexOptService.TAG, "Performing idle optimizations finished!");
            }
        }.start();
        return true;
    }

    private int idleOptimization(PackageManagerService pm, ArraySet<String> pkgs, Context context) {
        Log.i(TAG, "Performing idle optimizations");
        this.mExitPostBootUpdate.set(true);
        this.mAbortIdleOptimization.set(false);
        if (this.mHwBDOSEx == null) {
            this.mHwBDOSEx = HwServiceExFactory.getHwBackgroundDexOptServiceEx(this, context);
        }
        long lowStorageThreshold = getLowStorageThreshold(context);
        int result = optimizePackages(pm, pkgs, lowStorageThreshold, true, sFailedPackageNamesPrimary);
        if (result == 2) {
            return result;
        }
        if (SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false)) {
            result = reconcileSecondaryDexFiles(pm.getDexManager());
            if (result == 2) {
                return result;
            }
            result = optimizePackages(pm, pkgs, lowStorageThreshold, false, sFailedPackageNamesSecondary);
        }
        return result;
    }

    private int optimizePackages(PackageManagerService pm, ArraySet<String> pkgs, long lowStorageThreshold, boolean is_for_primary_dex, ArraySet<String> failedPackageNames) {
        ArraySet<String> updatedPackages = new ArraySet();
        Set<String> unusedPackages = pm.getUnusedPackages(mDowngradeUnusedAppsThresholdInMillis);
        boolean shouldDowngrade = shouldDowngrade(2 * lowStorageThreshold);
        for (String pkg : pkgs) {
            int abort_code = abortIdleOptimizations(lowStorageThreshold);
            if (abort_code == 2) {
                return abort_code;
            }
            synchronized (failedPackageNames) {
                if (!failedPackageNames.contains(pkg)) {
                    int reason;
                    boolean downgrade;
                    boolean success;
                    if (unusedPackages.contains(pkg) && shouldDowngrade) {
                        if (!is_for_primary_dex || (pm.canHaveOatDir(pkg) ^ 1) == 0) {
                            reason = 5;
                            downgrade = true;
                        } else {
                            pm.deleteOatArtifactsOfPackage(pkg);
                        }
                    } else if (abort_code != 3) {
                        reason = 3;
                        downgrade = false;
                    } else {
                        continue;
                    }
                    synchronized (failedPackageNames) {
                        failedPackageNames.add(pkg);
                    }
                    if (this.mHwBDOSEx != null) {
                        reason = this.mHwBDOSEx.getReason(reason, 3, 7, pkg);
                    }
                    int dexoptFlags = (downgrade ? 32 : 0) | 5;
                    if (is_for_primary_dex) {
                        int result = pm.performDexOptWithStatus(new DexoptOptions(pkg, reason, dexoptFlags));
                        success = result != -1;
                        if (result == 1) {
                            updatedPackages.add(pkg);
                        }
                    } else {
                        success = pm.performDexOpt(new DexoptOptions(pkg, reason, dexoptFlags | 8));
                    }
                    if (success) {
                        synchronized (failedPackageNames) {
                            failedPackageNames.remove(pkg);
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        notifyPinService(updatedPackages);
        return 0;
    }

    private int reconcileSecondaryDexFiles(DexManager dm) {
        for (String p : dm.getAllPackagesWithSecondaryDexFiles()) {
            if (this.mAbortIdleOptimization.get()) {
                return 2;
            }
            dm.reconcileSecondaryDexFiles(p);
        }
        return 0;
    }

    private int abortIdleOptimizations(long lowStorageThreshold) {
        if (this.mAbortIdleOptimization.get()) {
            return 2;
        }
        long usableSpace = this.mDataDir.getUsableSpace();
        if (usableSpace >= lowStorageThreshold) {
            return 1;
        }
        Log.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
        return 3;
    }

    private boolean shouldDowngrade(long lowStorageThresholdForDowngrade) {
        if (this.mDataDir.getUsableSpace() < lowStorageThresholdForDowngrade) {
            return true;
        }
        return false;
    }

    public static boolean runIdleOptimizationsNow(PackageManagerService pm, Context context) {
        if (new BackgroundDexOptService().idleOptimization(pm, pm.getOptimizablePackages(), context) == 0) {
            return true;
        }
        return false;
    }

    public boolean onStartJob(JobParameters params) {
        PackageManagerService pm = (PackageManagerService) ServiceManager.getService(HwBroadcastRadarUtil.KEY_PACKAGE);
        if (pm.isStorageLow()) {
            return false;
        }
        ArraySet<String> pkgs = pm.getOptimizablePackages();
        if (pkgs.isEmpty()) {
            return false;
        }
        boolean result;
        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            result = runPostBootUpdate(params, pm, pkgs);
        } else {
            result = runIdleOptimization(params, pm, pkgs);
        }
        return result;
    }

    public boolean onStopJob(JobParameters params) {
        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            this.mAbortPostBootUpdate.set(true);
        } else {
            this.mAbortIdleOptimization.set(true);
        }
        return false;
    }

    private void notifyPinService(ArraySet<String> updatedPackages) {
        PinnerService pinnerService = (PinnerService) LocalServices.getService(PinnerService.class);
        if (pinnerService != null) {
            Log.i(TAG, "Pinning optimized code " + updatedPackages);
            pinnerService.update(updatedPackages);
        }
    }

    private static long getDowngradeUnusedAppsThresholdInMillis() {
        String sysPropKey = "pm.dexopt.downgrade_after_inactive_days";
        String sysPropValue = SystemProperties.get("pm.dexopt.downgrade_after_inactive_days");
        if (sysPropValue != null && !sysPropValue.isEmpty()) {
            return TimeUnit.DAYS.toMillis(Long.parseLong(sysPropValue));
        }
        Log.w(TAG, "SysProp pm.dexopt.downgrade_after_inactive_days not set");
        return JobStatus.NO_LATEST_RUNTIME;
    }

    private static boolean isBackgroundDexoptDisabled() {
        return SystemProperties.getBoolean("pm.dexopt.disable_bg_dexopt", false);
    }
}
