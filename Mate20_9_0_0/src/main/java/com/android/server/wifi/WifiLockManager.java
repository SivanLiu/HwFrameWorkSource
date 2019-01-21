package com.android.server.wifi;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class WifiLockManager {
    private static final String TAG = "WifiLockManager";
    private final IBatteryStats mBatteryStats;
    private final Context mContext;
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;
    private boolean mVerboseLoggingEnabled = false;
    private final List<WifiLock> mWifiLocks = new ArrayList();

    private class WifiLock implements DeathRecipient {
        IBinder mBinder;
        int mMode;
        String mTag;
        int mUid = Binder.getCallingUid();
        WorkSource mWorkSource;

        WifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
            this.mTag = tag;
            this.mBinder = binder;
            this.mMode = lockMode;
            this.mWorkSource = ws;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        protected WorkSource getWorkSource() {
            return this.mWorkSource;
        }

        protected int getUid() {
            return this.mUid;
        }

        protected IBinder getBinder() {
            return this.mBinder;
        }

        public void binderDied() {
            WifiLockManager.this.releaseLock(this.mBinder);
        }

        public void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiLock{");
            stringBuilder.append(this.mTag);
            stringBuilder.append(" type=");
            stringBuilder.append(this.mMode);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" workSource=");
            stringBuilder.append(this.mWorkSource);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    WifiLockManager(Context context, IBatteryStats batteryStats) {
        this.mContext = context;
        this.mBatteryStats = batteryStats;
    }

    public boolean acquireWifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        if (isValidLockMode(lockMode)) {
            if (ws == null || ws.isEmpty()) {
                ws = new WorkSource(Binder.getCallingUid());
            } else {
                this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
            }
            return addLock(new WifiLock(lockMode, tag, binder, ws));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lockMode =");
        stringBuilder.append(lockMode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean releaseWifiLock(IBinder binder) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        return releaseLock(binder);
    }

    public synchronized int getStrongestLockMode() {
        if (this.mWifiLocks.isEmpty()) {
            return 0;
        }
        if (this.mFullHighPerfLocksAcquired > this.mFullHighPerfLocksReleased) {
            return 3;
        }
        if (this.mFullLocksAcquired > this.mFullLocksReleased) {
            return 1;
        }
        return 2;
    }

    public synchronized WorkSource createMergedWorkSource() {
        WorkSource mergedWS;
        mergedWS = new WorkSource();
        for (WifiLock lock : this.mWifiLocks) {
            mergedWS.add(lock.getWorkSource());
        }
        return mergedWS;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002c A:{Catch:{ all -> 0x0060 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateWifiLockWorkSource(IBinder binder, WorkSource ws) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
        WifiLock wl = findLockByBinder(binder);
        if (wl != null) {
            WorkSource newWorkSource;
            long ident;
            if (ws != null) {
                if (!ws.isEmpty()) {
                    newWorkSource = new WorkSource(ws);
                    if (this.mVerboseLoggingEnabled) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("updateWifiLockWakeSource: ");
                        stringBuilder.append(wl);
                        stringBuilder.append(", newWorkSource=");
                        stringBuilder.append(newWorkSource);
                        Slog.d(str, stringBuilder.toString());
                    }
                    ident = Binder.clearCallingIdentity();
                    this.mBatteryStats.noteFullWifiLockAcquiredFromSource(newWorkSource);
                    this.mBatteryStats.noteFullWifiLockReleasedFromSource(wl.mWorkSource);
                    wl.mWorkSource = newWorkSource;
                    Binder.restoreCallingIdentity(ident);
                }
            }
            newWorkSource = new WorkSource(Binder.getCallingUid());
            if (this.mVerboseLoggingEnabled) {
            }
            ident = Binder.clearCallingIdentity();
            try {
                this.mBatteryStats.noteFullWifiLockAcquiredFromSource(newWorkSource);
                this.mBatteryStats.noteFullWifiLockReleasedFromSource(wl.mWorkSource);
                wl.mWorkSource = newWorkSource;
                Binder.restoreCallingIdentity(ident);
            } catch (RemoteException e) {
                try {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RemoteException in noteFullWifiLockReleasedFromSource or noteFullWifiLockAcquiredFromSource: ");
                    stringBuilder2.append(e.getMessage());
                    Slog.e(str2, stringBuilder2.toString());
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        } else {
            throw new IllegalArgumentException("Wifi lock not active");
        }
    }

    public boolean clearWifiLocks() {
        return clearWifiLocksLocked();
    }

    private synchronized boolean clearWifiLocksLocked() {
        String EXCEPT = "WiFiDirectFT";
        List<WifiLock> copyList = new ArrayList();
        copyList.addAll(this.mWifiLocks);
        for (WifiLock l : copyList) {
            if (EXCEPT.equals(l.mTag)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("don't release module: ");
                stringBuilder.append(EXCEPT);
                Slog.d(str, stringBuilder.toString());
            } else if (!releaseWifiLock(l.mBinder)) {
                Slog.d(TAG, "releaseWifiLock failed , don't send CMD_LACKS_CHANGED");
            }
        }
        if (this.mWifiLocks.size() != 0) {
            Slog.d(TAG, "mWifiLocks.size() != 0, don't send CMD_LOCKS_CHANGED");
            return false;
        }
        Slog.e(TAG, "CMD_LOCKS_CHANGED is waived!!!");
        return true;
    }

    private static boolean isValidLockMode(int lockMode) {
        if (lockMode == 1 || lockMode == 2 || lockMode == 3) {
            return true;
        }
        return false;
    }

    private synchronized boolean addLock(WifiLock lock) {
        boolean lockAdded;
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addLock: ");
            stringBuilder.append(lock);
            Slog.d(str, stringBuilder.toString());
        }
        if (findLockByBinder(lock.getBinder()) != null) {
            if (this.mVerboseLoggingEnabled) {
                Slog.d(TAG, "attempted to add a lock when already holding one");
            }
            return false;
        }
        this.mWifiLocks.add(lock);
        lockAdded = false;
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockAcquiredFromSource(lock.mWorkSource);
            switch (lock.mMode) {
                case 1:
                    this.mFullLocksAcquired++;
                    break;
                case 2:
                    this.mScanLocksAcquired++;
                    break;
                case 3:
                    this.mFullHighPerfLocksAcquired++;
                    break;
                default:
                    break;
            }
            lockAdded = true;
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e) {
            try {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RemoteException in addLock : ");
                stringBuilder2.append(e.getMessage());
                Slog.e(str2, stringBuilder2.toString());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return lockAdded;
    }

    private synchronized WifiLock removeLock(IBinder binder) {
        WifiLock lock;
        lock = findLockByBinder(binder);
        if (lock != null) {
            this.mWifiLocks.remove(lock);
            lock.unlinkDeathRecipient();
        }
        return lock;
    }

    private synchronized boolean releaseLock(IBinder binder) {
        WifiLock wifiLock = removeLock(binder);
        if (wifiLock == null) {
            return false;
        }
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("releaseLock: ");
            stringBuilder.append(wifiLock);
            Slog.d(str, stringBuilder.toString());
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLock.mWorkSource);
            switch (wifiLock.mMode) {
                case 1:
                    this.mFullLocksReleased++;
                    break;
                case 2:
                    this.mScanLocksReleased++;
                    break;
                case 3:
                    this.mFullHighPerfLocksReleased++;
                    break;
                default:
                    break;
            }
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e) {
            try {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RemoteException occurs: ");
                stringBuilder2.append(e.getMessage());
                Slog.e(str2, stringBuilder2.toString());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return true;
    }

    private synchronized WifiLock findLockByBinder(IBinder binder) {
        for (WifiLock lock : this.mWifiLocks) {
            if (lock.getBinder() == binder) {
                return lock;
            }
        }
        return null;
    }

    protected void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Locks acquired: ");
        stringBuilder.append(this.mFullLocksAcquired);
        stringBuilder.append(" full, ");
        stringBuilder.append(this.mFullHighPerfLocksAcquired);
        stringBuilder.append(" full high perf, ");
        stringBuilder.append(this.mScanLocksAcquired);
        stringBuilder.append(" scan");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Locks released: ");
        stringBuilder.append(this.mFullLocksReleased);
        stringBuilder.append(" full, ");
        stringBuilder.append(this.mFullHighPerfLocksReleased);
        stringBuilder.append(" full high perf, ");
        stringBuilder.append(this.mScanLocksReleased);
        stringBuilder.append(" scan");
        pw.println(stringBuilder.toString());
        pw.println();
        pw.println("Locks held:");
        for (WifiLock lock : this.mWifiLocks) {
            pw.print("    ");
            pw.println(lock);
        }
    }

    protected void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }
}
