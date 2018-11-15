package com.android.server;

import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

public class HwLog {
    private static final String TAG = "Bluetooth_framework";

    public static void v(String tag, String msg) {
        i(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        i(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        i(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        i(tag, msg, tr);
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
        stringBuilder.append(10);
        stringBuilder.append(getStackTraceString(tr));
        Log.i(str, stringBuilder.toString());
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
        stringBuilder.append(10);
        stringBuilder.append(getStackTraceString(tr));
        Log.w(str, stringBuilder.toString());
    }

    public static void w(String tag, Throwable tr) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(":");
        stringBuilder.append(10);
        stringBuilder.append(getStackTraceString(tr));
        Log.w(str, stringBuilder.toString());
    }

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
        stringBuilder.append(10);
        stringBuilder.append(getStackTraceString(tr));
        Log.e(str, stringBuilder.toString());
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        for (Throwable t = tr; t != null; t = t.getCause()) {
            if (t instanceof UnknownHostException) {
                return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
        }
        StringWriter sw = new StringWriter();
        tr.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
