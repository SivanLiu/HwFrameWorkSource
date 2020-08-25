package com.android.systemui.shared.recents.utilities;

import android.os.Trace;
import com.android.server.security.permissionmanager.util.PermissionType;

public class AppTrace {
    public static void start(String key, int cookie) {
        Trace.asyncTraceBegin(PermissionType.RECEIVE_SMS, key, cookie);
    }

    public static void start(String key) {
        Trace.asyncTraceBegin(PermissionType.RECEIVE_SMS, key, 0);
    }

    public static void end(String key) {
        Trace.asyncTraceEnd(PermissionType.RECEIVE_SMS, key, 0);
    }

    public static void end(String key, int cookie) {
        Trace.asyncTraceEnd(PermissionType.RECEIVE_SMS, key, cookie);
    }

    public static void beginSection(String key) {
        Trace.beginSection(key);
    }

    public static void endSection() {
        Trace.endSection();
    }

    public static void count(String name, int count) {
        Trace.traceCounter(PermissionType.RECEIVE_SMS, name, count);
    }
}
