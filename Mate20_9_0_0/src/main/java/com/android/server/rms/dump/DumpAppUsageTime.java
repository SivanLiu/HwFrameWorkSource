package com.android.server.rms.dump;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.rms.utils.Utils;
import android.util.Log;
import com.android.server.rms.record.AppUsageTime;
import huawei.com.android.server.policy.stylus.StylusGestureSettings;

public class DumpAppUsageTime {
    private static final String TAG = "RMS.DumpAppUsageTime";
    private static final AppUsageTime mDump = AppUsageTime.self();

    private static final boolean checkInputArgs(Context context, String[] args) {
        if (args.length != 2 || args[1] == null) {
            Log.e(TAG, "please input correct package name for AppUsageTime dump!");
            return false;
        }
        try {
            if (context.getPackageManager().getApplicationInfo(args[1], 0) != null) {
                return true;
            }
            Log.e(TAG, "There is not such an application, please check the package name carefully!!!");
            return false;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getApplicationInfo failed. Please check the package name carefully!!!");
            return false;
        }
    }

    public static final void dumpGetUsageTime(Context context, String[] args) {
        if (checkInputArgs(context, args)) {
            String pkg = args[1];
            long time = mDump.getRealUsageTimeLocked(pkg) / 1000;
            if (Utils.HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pkg ");
                stringBuilder.append(pkg);
                stringBuilder.append(", usageTime is ");
                stringBuilder.append(time);
                stringBuilder.append(StylusGestureSettings.STYLUS_GESTURE_S_SUFFIX);
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    public static final void dumpIsHistoryInstalledApp(Context context, String[] args) {
        if (checkInputArgs(context, args)) {
            String pkg = args[1];
            String str;
            StringBuilder stringBuilder;
            if (mDump.isHistoryInstalledApp(pkg)) {
                if (Utils.HWFLOW) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("pkg ");
                    stringBuilder.append(pkg);
                    stringBuilder.append(" is a history installed application");
                    Log.i(str, stringBuilder.toString());
                }
            } else if (Utils.HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("pkg ");
                stringBuilder.append(pkg);
                stringBuilder.append(" is not a history installed application");
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    public static final void dumpRecordedAppUsageInfo() {
        mDump.dumpInfo(false);
    }
}
