package com.android.server.input;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.IMonitor;
import android.util.Log;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import java.util.ArrayList;
import java.util.List;

public class DMDUtils {
    private static final int DMD_DEFAULT_RET = -1;
    private static final int DMD_DELAY = 100;
    public static final int DMD_GO_LAUNCHER_ERROR = 912001014;
    public static final int DMD_GO_RECENT_ERROOR = 912001015;
    /* access modifiers changed from: private */
    public static final Object DMD_LOCK = new Object();
    public static final int DMD_NO_ERROR = 0;
    private static final String DMD_STRING_DEFAULT_VALUE = "Default";
    private static final String DMD_WORKING_THREAD = "DMD_woring_thread";
    private static final int MAX_TRY = 10;
    /* access modifiers changed from: private */
    public static final String TAG = DMDUtils.class.getSimpleName();
    /* access modifiers changed from: private */
    public static int currentDmdType = 0;
    /* access modifiers changed from: private */
    public static boolean isDmdWorking = false;
    private Runnable dmdWorking = new Runnable() {
        /* class com.android.server.input.DMDUtils.AnonymousClass1 */
        int ret = -1;
        int time = 0;

        public void run() {
            while (true) {
                int i = this.time;
                if (i >= 10) {
                    break;
                }
                this.time = i + 1;
                this.ret = DMDUtils.this.checkCurrentStatus();
                if (this.ret == 0) {
                    break;
                } else if (this.time < 10) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(DMDUtils.TAG, "sleep error");
                    }
                } else {
                    this.ret = DMDUtils.currentDmdType;
                }
            }
            DMDUtils.this.sendDmdLogToService(this.ret);
            this.time = 0;
            this.ret = -1;
            synchronized (DMDUtils.DMD_LOCK) {
                boolean unused = DMDUtils.isDmdWorking = false;
            }
        }
    };
    private ActivityManager mActivityManager = null;
    private Context mContext;

    public DMDUtils(Context context) {
        this.mContext = context;
        this.mActivityManager = (ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
    }

    public boolean isTopTask(int stackId) {
        List<ActivityManager.RunningTaskInfo> tasks;
        ActivityManager.RunningTaskInfo topTask;
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager != null && (tasks = activityManager.getRunningTasks(1)) != null && !tasks.isEmpty() && (topTask = tasks.get(0)) != null && topTask.stackId == stackId && !getHomes().contains(topTask.topActivity.getPackageName())) {
            return true;
        }
        return false;
    }

    private boolean isTopTaskHome() {
        List<ActivityManager.RunningTaskInfo> tasks;
        ActivityManager.RunningTaskInfo topTask;
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager == null || (tasks = activityManager.getRunningTasks(1)) == null || tasks.isEmpty() || (topTask = tasks.get(0)) == null) {
            return false;
        }
        return getHomes().contains(topTask.topActivity.getPackageName());
    }

    public List<String> getHomes() {
        List<String> packages = new ArrayList<>();
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 65536)) {
            packages.add(info.activityInfo.packageName);
        }
        return packages;
    }

    public boolean doDMDDetect(int type) {
        boolean isDetect = false;
        synchronized (DMD_LOCK) {
            if (!isDmdWorking) {
                currentDmdType = type;
                isDmdWorking = true;
                Thread thread = new Thread(this.dmdWorking);
                thread.setName(DMD_WORKING_THREAD);
                thread.start();
                isDetect = true;
            }
        }
        return isDetect;
    }

    /* access modifiers changed from: private */
    public int checkCurrentStatus() {
        if (currentDmdType == 912001014 && isTopTaskHome()) {
            return 0;
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public void sendDmdLogToService(int errorType) {
        if (errorType != 0) {
            String str = TAG;
            Log.e(str, "DMD ERROR: " + errorType);
            switch (errorType) {
                case DMD_GO_LAUNCHER_ERROR /*{ENCODED_INT: 912001014}*/:
                    IMonitor.EventStream launcherErrorStream = IMonitor.openEventStream(errorType);
                    launcherErrorStream.setParam(0, 6);
                    launcherErrorStream.setParam(1, DMD_STRING_DEFAULT_VALUE);
                    launcherErrorStream.setParam(2, DMD_STRING_DEFAULT_VALUE);
                    launcherErrorStream.setParam(3, DMD_STRING_DEFAULT_VALUE);
                    IMonitor.sendEvent(launcherErrorStream);
                    IMonitor.closeEventStream(launcherErrorStream);
                    return;
                case DMD_GO_RECENT_ERROOR /*{ENCODED_INT: 912001015}*/:
                    IMonitor.EventStream recentErrorStream = IMonitor.openEventStream(errorType);
                    recentErrorStream.setParam(0, 6);
                    recentErrorStream.setParam(1, DMD_STRING_DEFAULT_VALUE);
                    recentErrorStream.setParam(2, DMD_STRING_DEFAULT_VALUE);
                    recentErrorStream.setParam(3, DMD_STRING_DEFAULT_VALUE);
                    IMonitor.sendEvent(recentErrorStream);
                    IMonitor.closeEventStream(recentErrorStream);
                    return;
                default:
                    return;
            }
        }
    }
}
