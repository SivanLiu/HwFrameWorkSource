package com.android.server.utils;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogBufferUtil {
    private static final String GMS_CORE_PACKAGENAME = "com.google.android.gms";
    private static final String HWLOG_SWITCH_PATH = "/dev/hwlog_switch";
    private static final String LOGBUFFER_DISABLE = "sys.logbuffer.disable";
    private static final int LOGSWITCH_STATUS_ON = 1;
    private static final String PROJECT_MENU_APLOG = "persist.sys.huawei.debug.on";
    private static final int PROJECT_MENU_APLOG_ON = 1;
    private static final String TAG = "LogBufferUtil";
    private static final int USER_TYPE_DOMESTIC_COMMERCIAL = 1;

    private static boolean isNologAndLite() {
        return 1 == SystemProperties.getInt("ro.logsystem.usertype", 0) && SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    }

    private static boolean isHwLogSwitchOn() {
        int logSwitch = 1;
        BufferedReader hwLogReader = null;
        try {
            hwLogReader = new BufferedReader(new InputStreamReader(new FileInputStream(HWLOG_SWITCH_PATH), "UTF-8"));
            String readLine = hwLogReader.readLine();
            String tempString = readLine;
            if (readLine != null) {
                logSwitch = Integer.parseInt(tempString);
            }
            readLine = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/dev/hwlog_switch = ");
            stringBuilder.append(logSwitch);
            Slog.i(readLine, stringBuilder.toString());
            try {
                hwLogReader.close();
            } catch (IOException e) {
                Slog.e(TAG, "hwLogReader close failed", e);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "/dev/hwlog_switch not exist", e2);
            if (hwLogReader != null) {
                hwLogReader.close();
            }
        } catch (IOException e3) {
            Slog.e(TAG, "logswitch read failed", e3);
            if (hwLogReader != null) {
                hwLogReader.close();
            }
        } catch (Exception e4) {
            Slog.e(TAG, "logswitch read exception", e4);
            if (hwLogReader != null) {
                hwLogReader.close();
            }
        } catch (Throwable th) {
            if (hwLogReader != null) {
                try {
                    hwLogReader.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "hwLogReader close failed", e5);
                }
            }
        }
        if (1 == logSwitch) {
            return true;
        }
        return false;
    }

    private static boolean isGmsCoreInstalled(Context context) {
        boolean isGmsCoreInstalled = false;
        try {
            if (context.getPackageManager().getPackageInfo(GMS_CORE_PACKAGENAME, 0) == null) {
                return isGmsCoreInstalled;
            }
            Slog.i(TAG, "hwLogBuffer: GmsCore installed.");
            return true;
        } catch (NameNotFoundException e) {
            Slog.i(TAG, "hwLogBuffer: GmsCore not installed.");
            return isGmsCoreInstalled;
        }
    }

    private static boolean adbEnabled(Context context) {
        return Global.getInt(context.getContentResolver(), "adb_enabled", 0) > 0 || SystemProperties.getInt(PROJECT_MENU_APLOG, 0) == 1;
    }

    private static boolean needCloseLogBuffer(Context context) {
        return (adbEnabled(context) || isGmsCoreInstalled(context) || isHwLogSwitchOn()) ? false : true;
    }

    public static void closeLogBufferAsNeed(Context context) {
        if (isNologAndLite()) {
            boolean current = SystemProperties.getBoolean(LOGBUFFER_DISABLE, false);
            if (needCloseLogBuffer(context) != current) {
                SystemProperties.set(LOGBUFFER_DISABLE, current ? "false" : "true");
            }
        }
    }
}
