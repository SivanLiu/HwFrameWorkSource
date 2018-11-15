package com.android.server;

import android.util.LogWriter;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.Watchdog.HandlerChecker;
import dalvik.system.AnnotatedStackTraceElement;
import dalvik.system.VMStack;
import java.io.PrintWriter;
import java.util.List;

class WatchdogDiagnostics {
    WatchdogDiagnostics() {
    }

    private static String getBlockedOnString(Object blockedOn) {
        return String.format("- waiting to lock <0x%08x> (a %s)", new Object[]{Integer.valueOf(System.identityHashCode(blockedOn)), blockedOn.getClass().getName()});
    }

    private static String getLockedString(Object heldLock) {
        return String.format("- locked <0x%08x> (a %s)", new Object[]{Integer.valueOf(System.identityHashCode(heldLock)), heldLock.getClass().getName()});
    }

    @VisibleForTesting
    public static boolean printAnnotatedStack(Thread thread, PrintWriter out) {
        AnnotatedStackTraceElement[] stack = VMStack.getAnnotatedThreadStackTrace(thread);
        if (stack == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(thread.getName());
        stringBuilder.append(" annotated stack trace:");
        out.println(stringBuilder.toString());
        for (AnnotatedStackTraceElement element : stack) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("    at ");
            stringBuilder2.append(element.getStackTraceElement());
            out.println(stringBuilder2.toString());
            if (element.getBlockedOn() != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    ");
                stringBuilder2.append(getBlockedOnString(element.getBlockedOn()));
                out.println(stringBuilder2.toString());
            }
            if (element.getHeldLocks() != null) {
                for (Object held : element.getHeldLocks()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("    ");
                    stringBuilder3.append(getLockedString(held));
                    out.println(stringBuilder3.toString());
                }
            }
        }
        return true;
    }

    public static void diagnoseCheckers(List<HandlerChecker> blockedCheckers) {
        PrintWriter out = new PrintWriter(new LogWriter(5, "Watchdog", 3), true);
        for (int i = 0; i < blockedCheckers.size(); i++) {
            Thread blockedThread = ((HandlerChecker) blockedCheckers.get(i)).getThread();
            if (!printAnnotatedStack(blockedThread, out)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(blockedThread.getName());
                stringBuilder.append(" stack trace:");
                Slog.w("Watchdog", stringBuilder.toString());
                for (StackTraceElement element : blockedThread.getStackTrace()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    at ");
                    stringBuilder2.append(element);
                    Slog.w("Watchdog", stringBuilder2.toString());
                }
            }
        }
    }
}
