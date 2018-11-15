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
        if (1 == SystemProperties.getInt("ro.logsystem.usertype", 0)) {
            return SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
        }
        return false;
    }

    private static boolean isHwLogSwitchOn() {
        IOException e;
        FileNotFoundException e2;
        Exception e3;
        Throwable th;
        int logSwitch = 0;
        BufferedReader bufferedReader = null;
        try {
            BufferedReader hwLogReader = new BufferedReader(new InputStreamReader(new FileInputStream(HWLOG_SWITCH_PATH), "UTF-8"));
            try {
                String tempString = hwLogReader.readLine();
                if (tempString != null) {
                    logSwitch = Integer.parseInt(tempString);
                }
                Slog.i(TAG, "/dev/hwlog_switch = " + logSwitch);
                if (hwLogReader != null) {
                    try {
                        hwLogReader.close();
                    } catch (IOException e4) {
                        Slog.e(TAG, "hwLogReader close failed", e4);
                    }
                }
                bufferedReader = hwLogReader;
            } catch (FileNotFoundException e5) {
                e2 = e5;
                bufferedReader = hwLogReader;
                Slog.e(TAG, "/dev/hwlog_switch not exist", e2);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e42) {
                        Slog.e(TAG, "hwLogReader close failed", e42);
                    }
                }
                if (1 == logSwitch) {
                    return true;
                }
                return false;
            } catch (IOException e6) {
                e42 = e6;
                bufferedReader = hwLogReader;
                Slog.e(TAG, "logswitch read failed", e42);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e422) {
                        Slog.e(TAG, "hwLogReader close failed", e422);
                    }
                }
                if (1 == logSwitch) {
                    return false;
                }
                return true;
            } catch (Exception e7) {
                e3 = e7;
                bufferedReader = hwLogReader;
                try {
                    Slog.e(TAG, "logswitch read exception", e3);
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e4222) {
                            Slog.e(TAG, "hwLogReader close failed", e4222);
                        }
                    }
                    if (1 == logSwitch) {
                        return true;
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e42222) {
                            Slog.e(TAG, "hwLogReader close failed", e42222);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedReader = hwLogReader;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e8) {
            e2 = e8;
            Slog.e(TAG, "/dev/hwlog_switch not exist", e2);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (1 == logSwitch) {
                return false;
            }
            return true;
        } catch (IOException e9) {
            e42222 = e9;
            Slog.e(TAG, "logswitch read failed", e42222);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (1 == logSwitch) {
                return true;
            }
            return false;
        } catch (Exception e10) {
            e3 = e10;
            Slog.e(TAG, "logswitch read exception", e3);
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (1 == logSwitch) {
                return false;
            }
            return true;
        }
        if (1 == logSwitch) {
            return true;
        }
        return false;
    }

    private static boolean isGmsCoreInstalled(Context context) {
        try {
            if (context.getPackageManager().getPackageInfo(GMS_CORE_PACKAGENAME, 0) == null) {
                return false;
            }
            Slog.i(TAG, "hwLogBuffer: GmsCore installed.");
            return true;
        } catch (NameNotFoundException e) {
            Slog.i(TAG, "hwLogBuffer: GmsCore not installed.");
            return false;
        }
    }

    private static boolean adbEnabled(Context context) {
        return Global.getInt(context.getContentResolver(), "adb_enabled", 0) > 0 || SystemProperties.getInt(PROJECT_MENU_APLOG, 0) == 1;
    }

    private static boolean needCloseLogBuffer(Context context) {
        return (adbEnabled(context) || (isGmsCoreInstalled(context) ^ 1) == 0) ? false : isHwLogSwitchOn() ^ 1;
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
