package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession.Stub;
import android.app.backup.RestoreSet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.transport.TransportClient;
import java.util.function.BiFunction;

public class ActiveRestoreSession extends Stub {
    private static final String TAG = "RestoreSession";
    private final BackupManagerService mBackupManagerService;
    boolean mEnded = false;
    private final String mPackageName;
    public RestoreSet[] mRestoreSets = null;
    boolean mTimedOut = false;
    private final TransportManager mTransportManager;
    private final String mTransportName;

    public class EndRestoreRunnable implements Runnable {
        BackupManagerService mBackupManager;
        ActiveRestoreSession mSession;

        public EndRestoreRunnable(BackupManagerService manager, ActiveRestoreSession session) {
            this.mBackupManager = manager;
            this.mSession = session;
        }

        public void run() {
            synchronized (this.mSession) {
                this.mSession.mEnded = true;
            }
            this.mBackupManager.clearRestoreSession(this.mSession);
        }
    }

    public ActiveRestoreSession(BackupManagerService backupManagerService, String packageName, String transportName) {
        this.mBackupManagerService = backupManagerService;
        this.mPackageName = packageName;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportName = transportName;
    }

    public void markTimedOut() {
        this.mTimedOut = true;
    }

    public synchronized int getAvailableRestoreSets(IRestoreObserver observer, IBackupManagerMonitor monitor) {
        synchronized (this) {
            this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreSets");
            if (observer == null) {
                throw new IllegalArgumentException("Observer must not be null");
            } else if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            } else if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            } else {
                long oldId = Binder.clearCallingIdentity();
                try {
                    TransportClient transportClient = this.mTransportManager.getTransportClient(this.mTransportName, "RestoreSession.getAvailableRestoreSets()");
                    if (transportClient == null) {
                        Slog.w(TAG, "Null transport client getting restore sets");
                        Binder.restoreCallingIdentity(oldId);
                        return -1;
                    }
                    this.mBackupManagerService.getBackupHandler().removeMessages(8);
                    WakeLock wakelock = this.mBackupManagerService.getWakelock();
                    wakelock.acquire();
                    OnTaskFinishedListener listener = new -$$Lambda$ActiveRestoreSession$0wzV_GqtA0thM1WxLthNBKD3Ygw(this.mTransportManager, transportClient, wakelock);
                    Handler backupHandler = this.mBackupManagerService.getBackupHandler();
                    RestoreGetSetsParams restoreGetSetsParams = r1;
                    RestoreGetSetsParams restoreGetSetsParams2 = new RestoreGetSetsParams(transportClient, this, observer, monitor, listener);
                    this.mBackupManagerService.getBackupHandler().sendMessage(backupHandler.obtainMessage(6, restoreGetSetsParams));
                    Binder.restoreCallingIdentity(oldId);
                    return 0;
                } catch (Exception e) {
                    try {
                        Slog.e(TAG, "Error in getAvailableRestoreSets", e);
                    } finally {
                        Binder.restoreCallingIdentity(oldId);
                    }
                    return -1;
                }
            }
        }
    }

    static /* synthetic */ void lambda$getAvailableRestoreSets$0(TransportManager transportManager, TransportClient transportClient, WakeLock wakelock, String caller) {
        transportManager.disposeOfTransportClient(transportClient, caller);
        wakelock.release();
    }

    /* JADX WARNING: Missing block: B:42:0x00b2, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int restoreAll(long token, IRestoreObserver observer, IBackupManagerMonitor monitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("restoreAll token=");
        stringBuilder.append(Long.toHexString(token));
        stringBuilder.append(" observer=");
        stringBuilder.append(observer);
        Slog.d(str, stringBuilder.toString());
        StringBuilder stringBuilder2;
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        } else if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        } else if (this.mTransportManager.isTransportRegistered(this.mTransportName)) {
            synchronized (this.mBackupManagerService.getQueueLock()) {
                int i = 0;
                while (i < this.mRestoreSets.length) {
                    if (token == this.mRestoreSets[i].token) {
                        long oldId = Binder.clearCallingIdentity();
                        try {
                            int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(new -$$Lambda$ActiveRestoreSession$iPMdVI7x_J8xmayWzH6Euhd5674(observer, monitor, token), "RestoreSession.restoreAll()");
                        } finally {
                            Binder.restoreCallingIdentity(oldId);
                        }
                    } else {
                        i++;
                    }
                }
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Restore token ");
                stringBuilder2.append(Long.toHexString(token));
                stringBuilder2.append(" not found");
                Slog.w(str, stringBuilder2.toString());
                return -1;
            }
        } else {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Transport ");
            stringBuilder2.append(this.mTransportName);
            stringBuilder2.append(" not registered");
            Slog.e(str, stringBuilder2.toString());
            return -1;
        }
    }

    /* JADX WARNING: Missing block: B:59:0x011e, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int restoreSome(long token, IRestoreObserver observer, IBackupManagerMonitor monitor, String[] packages) {
        String[] strArr = packages;
        synchronized (this) {
            this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
            StringBuilder b = new StringBuilder(128);
            b.append("restoreSome token=");
            b.append(Long.toHexString(token));
            b.append(" observer=");
            b.append(observer.toString());
            b.append(" monitor=");
            if (monitor == null) {
                b.append("null");
            } else {
                b.append(monitor.toString());
            }
            b.append(" packages=");
            int i = 0;
            if (strArr == null) {
                b.append("null");
            } else {
                b.append('{');
                boolean first = true;
                for (String s : strArr) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(", ");
                    }
                    b.append(s);
                }
                b.append('}');
            }
            Slog.d(TAG, b.toString());
            String str;
            StringBuilder stringBuilder;
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            } else if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            } else if (this.mRestoreSets == null) {
                Slog.e(TAG, "Ignoring restoreAll() with no restore set");
                return -1;
            } else if (this.mPackageName != null) {
                Slog.e(TAG, "Ignoring restoreAll() on single-package session");
                return -1;
            } else if (this.mTransportManager.isTransportRegistered(this.mTransportName)) {
                synchronized (this.mBackupManagerService.getQueueLock()) {
                    while (true) {
                        int i2 = i;
                        if (i2 >= this.mRestoreSets.length) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Restore token ");
                            stringBuilder.append(Long.toHexString(token));
                            stringBuilder.append(" not found");
                            Slog.w(str, stringBuilder.toString());
                            return -1;
                        } else if (token == this.mRestoreSets[i2].token) {
                            long oldId = Binder.clearCallingIdentity();
                            try {
                                -$$Lambda$ActiveRestoreSession$amDGbcwA180LGcZKUosvhspMk2E -__lambda_activerestoresession_amdgbcwa180lgczkuosvhspmk2e = new -$$Lambda$ActiveRestoreSession$amDGbcwA180LGcZKUosvhspMk2E(observer, monitor, token, strArr);
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("RestoreSession.restoreSome(");
                                stringBuilder.append(strArr.length);
                                stringBuilder.append(" packages)");
                                int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(-__lambda_activerestoresession_amdgbcwa180lgczkuosvhspmk2e, stringBuilder.toString());
                            } finally {
                                Binder.restoreCallingIdentity(oldId);
                            }
                        } else {
                            i = i2 + 1;
                        }
                    }
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Transport ");
                stringBuilder.append(this.mTransportName);
                stringBuilder.append(" not registered");
                Slog.e(str, stringBuilder.toString());
                return -1;
            }
        }
    }

    static /* synthetic */ RestoreParams lambda$restoreSome$2(IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String[] packages, TransportClient transportClient, OnTaskFinishedListener listener) {
        return RestoreParams.createForRestoreSome(transportClient, observer, monitor, token, packages, packages.length > 1, listener);
    }

    /* JADX WARNING: Missing block: B:43:0x0136, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int restorePackage(String packageName, IRestoreObserver observer, IBackupManagerMonitor monitor) {
        String str = packageName;
        synchronized (this) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restorePackage pkg=");
            stringBuilder.append(str);
            stringBuilder.append(" obs=");
            IRestoreObserver iRestoreObserver = observer;
            stringBuilder.append(iRestoreObserver);
            stringBuilder.append("monitor=");
            IBackupManagerMonitor iBackupManagerMonitor = monitor;
            stringBuilder.append(iBackupManagerMonitor);
            Slog.v(str2, stringBuilder.toString());
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            int i = -1;
            StringBuilder stringBuilder2;
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            } else if (this.mPackageName == null || this.mPackageName.equals(str)) {
                try {
                    PackageInfo app = this.mBackupManagerService.getPackageManager().getPackageInfo(str, 0);
                    if (this.mBackupManagerService.getContext().checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                        if (app.applicationInfo.uid != Binder.getCallingUid()) {
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("restorePackage: bad packageName=");
                            stringBuilder.append(str);
                            stringBuilder.append(" or calling uid=");
                            stringBuilder.append(Binder.getCallingUid());
                            Slog.w(str2, stringBuilder.toString());
                            throw new SecurityException("No permission to restore other packages");
                        }
                    }
                    if (this.mTransportManager.isTransportRegistered(this.mTransportName)) {
                        long oldId = Binder.clearCallingIdentity();
                        try {
                            long availableRestoreToken = this.mBackupManagerService.getAvailableRestoreToken(str);
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("restorePackage pkg=");
                            stringBuilder2.append(str);
                            stringBuilder2.append(" token=");
                            stringBuilder2.append(Long.toHexString(availableRestoreToken));
                            Slog.v(str2, stringBuilder2.toString());
                            if (availableRestoreToken == 0) {
                                Slog.w(TAG, "No data available for this package; not restoring");
                            } else {
                                long token = availableRestoreToken;
                                -$$Lambda$ActiveRestoreSession$tb1mCMujBEuhHsxQ6tX_mYJVCII -__lambda_activerestoresession_tb1mcmujbeuhhsxq6tx_myjvcii = new -$$Lambda$ActiveRestoreSession$tb1mCMujBEuhHsxQ6tX_mYJVCII(iRestoreObserver, iBackupManagerMonitor, availableRestoreToken, app);
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("RestoreSession.restorePackage(");
                                stringBuilder.append(str);
                                stringBuilder.append(")");
                                i = stringBuilder.toString();
                                int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(-__lambda_activerestoresession_tb1mcmujbeuhhsxq6tx_myjvcii, i);
                                Binder.restoreCallingIdentity(oldId);
                                return sendRestoreToHandlerLocked;
                            }
                        } finally {
                            Binder.restoreCallingIdentity(oldId);
                        }
                    } else {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Transport ");
                        stringBuilder2.append(this.mTransportName);
                        stringBuilder2.append(" not registered");
                        Slog.e(str2, stringBuilder2.toString());
                        return -1;
                    }
                } catch (NameNotFoundException e) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Asked to restore nonexistent pkg ");
                    stringBuilder3.append(str);
                    Slog.w(str3, stringBuilder3.toString());
                    return -1;
                }
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Ignoring attempt to restore pkg=");
                stringBuilder2.append(str);
                stringBuilder2.append(" on session for package ");
                stringBuilder2.append(this.mPackageName);
                Slog.e(str2, stringBuilder2.toString());
                return -1;
            }
        }
    }

    public void setRestoreSets(RestoreSet[] restoreSets) {
        this.mRestoreSets = restoreSets;
    }

    private int sendRestoreToHandlerLocked(BiFunction<TransportClient, OnTaskFinishedListener, RestoreParams> restoreParamsBuilder, String callerLogString) {
        TransportClient transportClient = this.mTransportManager.getTransportClient(this.mTransportName, callerLogString);
        if (transportClient == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Transport ");
            stringBuilder.append(this.mTransportName);
            stringBuilder.append(" got unregistered");
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        Handler backupHandler = this.mBackupManagerService.getBackupHandler();
        backupHandler.removeMessages(8);
        WakeLock wakelock = this.mBackupManagerService.getWakelock();
        wakelock.acquire();
        OnTaskFinishedListener listener = new -$$Lambda$ActiveRestoreSession$0QlkHke0fYNRb0nGuyNs6WmyPDM(this.mTransportManager, transportClient, wakelock);
        Message msg = backupHandler.obtainMessage(3);
        msg.obj = restoreParamsBuilder.apply(transportClient, listener);
        backupHandler.sendMessage(msg);
        return 0;
    }

    static /* synthetic */ void lambda$sendRestoreToHandlerLocked$4(TransportManager transportManager, TransportClient transportClient, WakeLock wakelock, String caller) {
        transportManager.disposeOfTransportClient(transportClient, caller);
        wakelock.release();
    }

    public synchronized void endRestoreSession() {
        Slog.d(TAG, "endRestoreSession");
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
        } else if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else {
            this.mBackupManagerService.getBackupHandler().post(new EndRestoreRunnable(this.mBackupManagerService, this));
        }
    }
}
