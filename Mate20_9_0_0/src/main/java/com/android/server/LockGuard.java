package com.android.server;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LockGuard {
    public static final int INDEX_ACTIVITY = 6;
    public static final int INDEX_APP_OPS = 0;
    public static final int INDEX_DPMS = 7;
    public static final int INDEX_PACKAGES = 3;
    public static final int INDEX_POWER = 1;
    public static final int INDEX_STORAGE = 4;
    public static final int INDEX_USER = 2;
    public static final int INDEX_WINDOW = 5;
    private static final String TAG = "LockGuard";
    private static ArrayMap<Object, LockInfo> sKnown = new ArrayMap(0, true);
    private static Object[] sKnownFixed = new Object[8];

    private static class LockInfo {
        public ArraySet<Object> children;
        public boolean doWtf;
        public String label;

        private LockInfo() {
            this.children = new ArraySet(0, true);
        }
    }

    private static LockInfo findOrCreateLockInfo(Object lock) {
        LockInfo info = (LockInfo) sKnown.get(lock);
        if (info != null) {
            return info;
        }
        info = new LockInfo();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(lock)));
        stringBuilder.append(" [");
        stringBuilder.append(new Throwable().getStackTrace()[2].toString());
        stringBuilder.append("]");
        info.label = stringBuilder.toString();
        sKnown.put(lock, info);
        return info;
    }

    public static Object guard(Object lock) {
        if (lock == null || Thread.holdsLock(lock)) {
            return lock;
        }
        int i;
        LockInfo info = findOrCreateLockInfo(lock);
        int i2 = 0;
        boolean triggered = false;
        for (i = 0; i < info.children.size(); i++) {
            Object child = info.children.valueAt(i);
            if (child != null && Thread.holdsLock(child)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling thread ");
                stringBuilder.append(Thread.currentThread().getName());
                stringBuilder.append(" is holding ");
                stringBuilder.append(lockToString(child));
                stringBuilder.append(" while trying to acquire ");
                stringBuilder.append(lockToString(lock));
                doLog(lock, stringBuilder.toString());
                triggered = true;
            }
        }
        if (!triggered) {
            while (true) {
                i = i2;
                if (i >= sKnown.size()) {
                    break;
                }
                Object test = sKnown.keyAt(i);
                if (!(test == null || test == lock || !Thread.holdsLock(test))) {
                    ((LockInfo) sKnown.valueAt(i)).children.add(lock);
                }
                i2 = i + 1;
            }
        }
        return lock;
    }

    public static void guard(int index) {
        for (int i = 0; i < index; i++) {
            Object lock = sKnownFixed[i];
            if (lock != null && Thread.holdsLock(lock)) {
                Object targetMayBeNull = sKnownFixed[index];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling thread ");
                stringBuilder.append(Thread.currentThread().getName());
                stringBuilder.append(" is holding ");
                stringBuilder.append(lockToString(i));
                stringBuilder.append(" while trying to acquire ");
                stringBuilder.append(lockToString(index));
                doLog(targetMayBeNull, stringBuilder.toString());
            }
        }
    }

    private static void doLog(Object lock, String message) {
        if (lock == null || !findOrCreateLockInfo(lock).doWtf) {
            Slog.w(TAG, message, new Throwable());
        } else {
            new Thread(new -$$Lambda$LockGuard$C107ImDhsfBAwlfWxZPBoVXIl_4(new RuntimeException(message))).start();
        }
    }

    public static Object installLock(Object lock, String label) {
        findOrCreateLockInfo(lock).label = label;
        return lock;
    }

    public static Object installLock(Object lock, int index) {
        return installLock(lock, index, false);
    }

    public static Object installLock(Object lock, int index, boolean doWtf) {
        sKnownFixed[index] = lock;
        LockInfo info = findOrCreateLockInfo(lock);
        info.doWtf = doWtf;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lock-");
        stringBuilder.append(lockToString(index));
        info.label = stringBuilder.toString();
        return lock;
    }

    public static Object installNewLock(int index) {
        return installNewLock(index, false);
    }

    public static Object installNewLock(int index, boolean doWtf) {
        Object lock = new Object();
        installLock(lock, index, doWtf);
        return lock;
    }

    private static String lockToString(Object lock) {
        LockInfo info = (LockInfo) sKnown.get(lock);
        if (info != null && !TextUtils.isEmpty(info.label)) {
            return info.label;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(lock)));
        return stringBuilder.toString();
    }

    private static String lockToString(int index) {
        switch (index) {
            case 0:
                return "APP_OPS";
            case 1:
                return "POWER";
            case 2:
                return "USER";
            case 3:
                return "PACKAGES";
            case 4:
                return "STORAGE";
            case 5:
                return "WINDOW";
            case 6:
                return "ACTIVITY";
            case 7:
                return "DPMS";
            default:
                return Integer.toString(index);
        }
    }

    public static void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        for (int i = 0; i < sKnown.size(); i++) {
            Object lock = sKnown.keyAt(i);
            LockInfo info = (LockInfo) sKnown.valueAt(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Lock ");
            stringBuilder.append(lockToString(lock));
            stringBuilder.append(":");
            pw.println(stringBuilder.toString());
            for (int j = 0; j < info.children.size(); j++) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Child ");
                stringBuilder2.append(lockToString(info.children.valueAt(j)));
                pw.println(stringBuilder2.toString());
            }
            pw.println();
        }
    }
}
