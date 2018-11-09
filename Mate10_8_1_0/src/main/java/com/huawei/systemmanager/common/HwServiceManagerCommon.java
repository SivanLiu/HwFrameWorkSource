package com.huawei.systemmanager.common;

import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AppOpsManager;

public class HwServiceManagerCommon {
    public static final int OP_WRITE_SMS = 15;

    public static int getRunningAppProcessInfoFlag(RunningAppProcessInfo info) {
        return info.flags;
    }

    public static int getRecentTaskInfoUserId(RecentTaskInfo info) {
        return info.userId;
    }

    public static long getRecentTaskInfoLastActiveTime(RecentTaskInfo info) {
        return info.lastActiveTime;
    }

    public static long getSecondaryServerThreshold(MemoryInfo info) {
        return info.secondaryServerThreshold;
    }

    public static void setMode(AppOpsManager manager, int code, int uid, String packageName, int mode) {
        if (manager != null) {
            manager.setMode(code, uid, packageName, mode);
        }
    }

    public static void setUidMode(AppOpsManager opsManager, String appOp, int uid, int mode) {
        if (opsManager != null) {
            opsManager.setUidMode(appOp, uid, mode);
        }
    }
}
