package com.huawei.nearbysdk;

import android.util.Log;
import java.lang.reflect.Field;

public class HwLog {
    private static final int BYTE_MASK = 255;
    private static final int BYTE_TO_HEX_HIGHER_OFFSET = 4;
    private static final int BYTE_TO_HEX_LOWER_MASK = 15;
    private static final String TAG = "nearby";
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static boolean sHwDetailLog;
    private static boolean sHwInfo;
    private static boolean sHwModuleDebug;

    public static void e(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString());
    }

    public static void e(String tag, String msg, Throwable tr) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString(), tr);
    }

    public static void w(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString());
    }

    public static void w(String tag, String msg, Throwable tr) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString(), tr);
    }

    public static void i(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.i(str, stringBuilder.toString());
    }

    public static void i(String tag, String msg, Throwable tr) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.i(str, stringBuilder.toString(), tr);
    }

    public static void d(String tag, String msg) {
        if (sHwDetailLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(":");
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (sHwDetailLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(":");
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString(), tr);
        }
    }

    public static void s(String tag, String msg) {
        if (sHwDetailLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(":");
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void s(String tag, String msg, Throwable tr) {
        if (sHwDetailLog) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(":");
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString(), tr);
        }
    }

    static {
        String str;
        StringBuilder stringBuilder;
        try {
            boolean z;
            String str2;
            StringBuilder stringBuilder2;
            Class<Log> logClass = Log.class;
            Field field_HwModuleLog = logClass.getField("HWModuleLog");
            sHwInfo = logClass.getField("HWINFO").getBoolean(null);
            sHwModuleDebug = field_HwModuleLog.getBoolean(null);
            if (!sHwInfo) {
                if (!sHwModuleDebug || !Log.isLoggable(TAG, 4)) {
                    z = false;
                    sHwInfo = z;
                    sHwDetailLog = sHwInfo;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sHwDetailLog:");
                    stringBuilder2.append(sHwDetailLog);
                    stringBuilder2.append(" HwModuleDebug:");
                    stringBuilder2.append(sHwModuleDebug);
                    e(str2, stringBuilder2.toString());
                }
            }
            z = true;
            sHwInfo = z;
            sHwDetailLog = sHwInfo;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sHwDetailLog:");
            stringBuilder2.append(sHwDetailLog);
            stringBuilder2.append(" HwModuleDebug:");
            stringBuilder2.append(sHwModuleDebug);
            e(str2, stringBuilder2.toString());
        } catch (IllegalArgumentException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error:getLogField--IllegalArgumentException");
            stringBuilder.append(e.getMessage());
            e(str, stringBuilder.toString());
        } catch (IllegalAccessException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error:getLogField--IllegalAccessException");
            stringBuilder.append(e2.getMessage());
            e(str, stringBuilder.toString());
        } catch (NoSuchFieldException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error:getLogField--NoSuchFieldException");
            stringBuilder.append(e3.getMessage());
            e(str, stringBuilder.toString());
        }
    }

    public static void logByteArray(String tag, byte[] data) {
        logByteArray(tag, null, data);
    }

    public static void logByteArray(String tag, String what, byte[] data) {
        if (sHwDetailLog && data != null) {
            StringBuilder dataStr = new StringBuilder();
            if (what != null) {
                dataStr.append(what);
            }
            dataStr.append(" len: ");
            dataStr.append(data.length);
            dataStr.append(" ## ");
            for (byte aData : data) {
                int v = aData & 255;
                dataStr.append(" 0x");
                dataStr.append(hexArray[v >>> 4]);
                dataStr.append(hexArray[v & 15]);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(":");
            stringBuilder.append(dataStr);
            Log.d(str, stringBuilder.toString());
        }
    }
}
