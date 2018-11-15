package com.android.server.notification;

import android.app.NotificationManager.Policy;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ZenLog {
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final String[] MSGS = new String[SIZE];
    private static final int SIZE = (Build.IS_DEBUGGABLE ? 100 : 20);
    private static final String TAG = "ZenLog";
    private static final long[] TIMES = new long[SIZE];
    private static final int[] TYPES = new int[SIZE];
    private static final int TYPE_ALLOW_DISABLE = 2;
    private static final int TYPE_CONFIG = 11;
    private static final int TYPE_DISABLE_EFFECTS = 13;
    private static final int TYPE_DOWNTIME = 5;
    private static final int TYPE_EXIT_CONDITION = 8;
    private static final int TYPE_INTERCEPTED = 1;
    private static final int TYPE_LISTENER_HINTS_CHANGED = 15;
    private static final int TYPE_NOT_INTERCEPTED = 12;
    private static final int TYPE_SET_NOTIFICATION_POLICY = 16;
    private static final int TYPE_SET_RINGER_MODE_EXTERNAL = 3;
    private static final int TYPE_SET_RINGER_MODE_INTERNAL = 4;
    private static final int TYPE_SET_ZEN_MODE = 6;
    private static final int TYPE_SUBSCRIBE = 9;
    private static final int TYPE_SUPPRESSOR_CHANGED = 14;
    private static final int TYPE_UNSUBSCRIBE = 10;
    private static final int TYPE_UPDATE_ZEN_MODE = 7;
    private static int sNext;
    private static int sSize;

    public static void traceIntercepted(NotificationRecord record, String reason) {
        if (record == null || !record.isIntercepted()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(record.getKey());
            stringBuilder.append(",");
            stringBuilder.append(reason);
            append(1, stringBuilder.toString());
        }
    }

    public static void traceNotIntercepted(NotificationRecord record, String reason) {
        if (record == null || !record.isUpdate) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(record.getKey());
            stringBuilder.append(",");
            stringBuilder.append(reason);
            append(12, stringBuilder.toString());
        }
    }

    public static void traceSetRingerModeExternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeInternalIn, int ringerModeInternalOut) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(caller);
        stringBuilder.append(",e:");
        stringBuilder.append(ringerModeToString(ringerModeOld));
        stringBuilder.append("->");
        stringBuilder.append(ringerModeToString(ringerModeNew));
        stringBuilder.append(",i:");
        stringBuilder.append(ringerModeToString(ringerModeInternalIn));
        stringBuilder.append("->");
        stringBuilder.append(ringerModeToString(ringerModeInternalOut));
        append(3, stringBuilder.toString());
    }

    public static void traceSetRingerModeInternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeExternalIn, int ringerModeExternalOut) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(caller);
        stringBuilder.append(",i:");
        stringBuilder.append(ringerModeToString(ringerModeOld));
        stringBuilder.append("->");
        stringBuilder.append(ringerModeToString(ringerModeNew));
        stringBuilder.append(",e:");
        stringBuilder.append(ringerModeToString(ringerModeExternalIn));
        stringBuilder.append("->");
        stringBuilder.append(ringerModeToString(ringerModeExternalOut));
        append(4, stringBuilder.toString());
    }

    public static void traceDowntimeAutotrigger(String result) {
        append(5, result);
    }

    public static void traceSetZenMode(int zenMode, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(zenModeToString(zenMode));
        stringBuilder.append(",");
        stringBuilder.append(reason);
        append(6, stringBuilder.toString());
    }

    public static void traceUpdateZenMode(int fromMode, int toMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(zenModeToString(fromMode));
        stringBuilder.append(" -> ");
        stringBuilder.append(zenModeToString(toMode));
        append(7, stringBuilder.toString());
    }

    public static void traceExitCondition(Condition c, ComponentName component, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);
        stringBuilder.append(",");
        stringBuilder.append(componentToString(component));
        stringBuilder.append(",");
        stringBuilder.append(reason);
        append(8, stringBuilder.toString());
    }

    public static void traceSetNotificationPolicy(String pkg, int targetSdk, Policy policy) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pkg=");
        stringBuilder.append(pkg);
        stringBuilder.append(" targetSdk=");
        stringBuilder.append(targetSdk);
        stringBuilder.append(" NotificationPolicy=");
        stringBuilder.append(policy.toString());
        append(16, stringBuilder.toString());
    }

    public static void traceSubscribe(Uri uri, IConditionProvider provider, RemoteException e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uri);
        stringBuilder.append(",");
        stringBuilder.append(subscribeResult(provider, e));
        append(9, stringBuilder.toString());
    }

    public static void traceUnsubscribe(Uri uri, IConditionProvider provider, RemoteException e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uri);
        stringBuilder.append(",");
        stringBuilder.append(subscribeResult(provider, e));
        append(10, stringBuilder.toString());
    }

    public static void traceConfig(String reason, ZenModeConfig oldConfig, ZenModeConfig newConfig) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(reason);
        stringBuilder.append(",");
        stringBuilder.append(newConfig != null ? newConfig.toString() : null);
        stringBuilder.append(",");
        stringBuilder.append(ZenModeConfig.diff(oldConfig, newConfig));
        append(11, stringBuilder.toString());
    }

    public static void traceDisableEffects(NotificationRecord record, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(record.getKey());
        stringBuilder.append(",");
        stringBuilder.append(reason);
        append(13, stringBuilder.toString());
    }

    public static void traceEffectsSuppressorChanged(List<ComponentName> oldSuppressors, List<ComponentName> newSuppressors, long suppressedEffects) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("suppressed effects:");
        stringBuilder.append(suppressedEffects);
        stringBuilder.append(",");
        stringBuilder.append(componentListToString(oldSuppressors));
        stringBuilder.append("->");
        stringBuilder.append(componentListToString(newSuppressors));
        append(14, stringBuilder.toString());
    }

    public static void traceListenerHintsChanged(int oldHints, int newHints, int listenerCount) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hintsToString(oldHints));
        stringBuilder.append("->");
        stringBuilder.append(hintsToString(newHints));
        stringBuilder.append(",listeners=");
        stringBuilder.append(listenerCount);
        append(15, stringBuilder.toString());
    }

    private static String subscribeResult(IConditionProvider provider, RemoteException e) {
        if (provider == null) {
            return "no provider";
        }
        return e != null ? e.getMessage() : "ok";
    }

    private static String typeToString(int type) {
        switch (type) {
            case 1:
                return "intercepted";
            case 2:
                return "allow_disable";
            case 3:
                return "set_ringer_mode_external";
            case 4:
                return "set_ringer_mode_internal";
            case 5:
                return "downtime";
            case 6:
                return "set_zen_mode";
            case 7:
                return "update_zen_mode";
            case 8:
                return "exit_condition";
            case 9:
                return "subscribe";
            case 10:
                return "unsubscribe";
            case 11:
                return "config";
            case 12:
                return "not_intercepted";
            case 13:
                return "disable_effects";
            case 14:
                return "suppressor_changed";
            case 15:
                return "listener_hints_changed";
            case 16:
                return "set_notification_policy";
            default:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String ringerModeToString(int ringerMode) {
        switch (ringerMode) {
            case 0:
                return "silent";
            case 1:
                return "vibrate";
            case 2:
                return "normal";
            default:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String zenModeToString(int zenMode) {
        switch (zenMode) {
            case 0:
                return "off";
            case 1:
                return "important_interruptions";
            case 2:
                return "no_interruptions";
            case 3:
                return "alarms";
            default:
                return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    private static String hintsToString(int hints) {
        if (hints == 4) {
            return "disable_call_effects";
        }
        switch (hints) {
            case 0:
                return "none";
            case 1:
                return "disable_effects";
            case 2:
                return "disable_notification_effects";
            default:
                return Integer.toString(hints);
        }
    }

    private static String componentToString(ComponentName component) {
        return component != null ? component.toShortString() : null;
    }

    private static String componentListToString(List<ComponentName> components) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(componentToString((ComponentName) components.get(i)));
        }
        return stringBuilder.toString();
    }

    private static void append(int type, String msg) {
        synchronized (MSGS) {
            TIMES[sNext] = System.currentTimeMillis();
            TYPES[sNext] = type;
            MSGS[sNext] = msg;
            sNext = (sNext + 1) % SIZE;
            if (sSize < SIZE) {
                sSize++;
            }
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(typeToString(type));
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public static void dump(PrintWriter pw, String prefix) {
        synchronized (MSGS) {
            int start = ((sNext - sSize) + SIZE) % SIZE;
            for (int i = 0; i < sSize; i++) {
                int j = (start + i) % SIZE;
                pw.print(prefix);
                pw.print(FORMAT.format(new Date(TIMES[j])));
                pw.print(' ');
                pw.print(typeToString(TYPES[j]));
                pw.print(": ");
                pw.println(MSGS[j]);
            }
        }
    }
}
