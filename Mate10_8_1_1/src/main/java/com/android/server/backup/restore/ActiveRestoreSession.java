package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession.Stub;
import android.app.backup.RestoreSet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Message;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;

public class ActiveRestoreSession extends Stub {
    private static final String TAG = "RestoreSession";
    private RefactoredBackupManagerService backupManagerService;
    boolean mEnded = false;
    private String mPackageName;
    public RestoreSet[] mRestoreSets = null;
    private IBackupTransport mRestoreTransport = null;
    boolean mTimedOut = false;

    public class EndRestoreRunnable implements Runnable {
        RefactoredBackupManagerService mBackupManager;
        ActiveRestoreSession mSession;

        public EndRestoreRunnable(RefactoredBackupManagerService manager, ActiveRestoreSession session) {
            this.mBackupManager = manager;
            this.mSession = session;
        }

        public void run() {
            synchronized (this.mSession) {
                this.mSession.mRestoreTransport = null;
                this.mSession.mEnded = true;
            }
            this.mBackupManager.clearRestoreSession(this.mSession);
        }
    }

    public ActiveRestoreSession(RefactoredBackupManagerService backupManagerService, String packageName, String transport) {
        this.backupManagerService = backupManagerService;
        this.mPackageName = packageName;
        this.mRestoreTransport = backupManagerService.getTransportManager().getTransportBinder(transport);
    }

    public void markTimedOut() {
        this.mTimedOut = true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int getAvailableRestoreSets(IRestoreObserver observer, IBackupManagerMonitor monitor) {
        this.backupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreSets");
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
                if (this.mRestoreTransport == null) {
                    Slog.w(TAG, "Null transport getting restore sets");
                } else {
                    this.backupManagerService.getBackupHandler().removeMessages(8);
                    this.backupManagerService.getWakelock().acquire();
                    this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(6, new RestoreGetSetsParams(this.mRestoreTransport, this, observer, monitor)));
                    Binder.restoreCallingIdentity(oldId);
                    return 0;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error in getAvailableRestoreSets", e);
                return -1;
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }
    }

    public synchronized int restoreAll(long token, IRestoreObserver observer, IBackupManagerMonitor monitor) {
        this.backupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        Slog.d(TAG, "restoreAll token=" + Long.toHexString(token) + " observer=" + observer);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mRestoreTransport == null || this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        } else if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        } else {
            try {
                String dirName = this.mRestoreTransport.transportDirName();
                synchronized (this.backupManagerService.getQueueLock()) {
                    for (RestoreSet restoreSet : this.mRestoreSets) {
                        if (token == restoreSet.token) {
                            this.backupManagerService.getBackupHandler().removeMessages(8);
                            long oldId = Binder.clearCallingIdentity();
                            this.backupManagerService.getWakelock().acquire();
                            Message msg = this.backupManagerService.getBackupHandler().obtainMessage(3);
                            msg.obj = new RestoreParams(this.mRestoreTransport, dirName, observer, monitor, token);
                            this.backupManagerService.getBackupHandler().sendMessage(msg);
                            Binder.restoreCallingIdentity(oldId);
                            return 0;
                        }
                    }
                    Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                    return -1;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get transport dir for restore: " + e.getMessage());
                return -1;
            }
        }
    }

    public synchronized int restoreSome(long token, IRestoreObserver observer, IBackupManagerMonitor monitor, String[] packages) {
        this.backupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
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
        if (packages == null) {
            b.append("null");
        } else {
            b.append('{');
            boolean first = true;
            for (String s : packages) {
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
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mRestoreTransport == null || this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        } else if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        } else {
            try {
                String dirName = this.mRestoreTransport.transportDirName();
                synchronized (this.backupManagerService.getQueueLock()) {
                    for (RestoreSet restoreSet : this.mRestoreSets) {
                        if (token == restoreSet.token) {
                            this.backupManagerService.getBackupHandler().removeMessages(8);
                            long oldId = Binder.clearCallingIdentity();
                            this.backupManagerService.getWakelock().acquire();
                            Message msg = this.backupManagerService.getBackupHandler().obtainMessage(3);
                            msg.obj = new RestoreParams(this.mRestoreTransport, dirName, observer, monitor, token, packages, packages.length > 1);
                            this.backupManagerService.getBackupHandler().sendMessage(msg);
                            Binder.restoreCallingIdentity(oldId);
                            return 0;
                        }
                    }
                    Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                    return -1;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get transport name for restoreSome: " + e.getMessage());
                return -1;
            }
        }
    }

    public synchronized int restorePackage(String packageName, IRestoreObserver observer, IBackupManagerMonitor monitor) {
        Slog.v(TAG, "restorePackage pkg=" + packageName + " obs=" + observer + "monitor=" + monitor);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mPackageName == null || this.mPackageName.equals(packageName)) {
            try {
                PackageInfo app = this.backupManagerService.getPackageManager().getPackageInfo(packageName, 0);
                if (this.backupManagerService.getContext().checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) != -1 || app.applicationInfo.uid == Binder.getCallingUid()) {
                    long oldId = Binder.clearCallingIdentity();
                    try {
                        long token = this.backupManagerService.getAvailableRestoreToken(packageName);
                        Slog.v(TAG, "restorePackage pkg=" + packageName + " token=" + Long.toHexString(token));
                        if (token == 0) {
                            Slog.w(TAG, "No data available for this package; not restoring");
                            return -1;
                        }
                        String dirName = this.mRestoreTransport.transportDirName();
                        this.backupManagerService.getBackupHandler().removeMessages(8);
                        this.backupManagerService.getWakelock().acquire();
                        Message msg = this.backupManagerService.getBackupHandler().obtainMessage(3);
                        msg.obj = new RestoreParams(this.mRestoreTransport, dirName, observer, monitor, token, app);
                        this.backupManagerService.getBackupHandler().sendMessage(msg);
                        return 0;
                    } catch (Exception e) {
                        Slog.e(TAG, "Unable to get transport dir for restorePackage: " + e.getMessage());
                        return -1;
                    } finally {
                        Binder.restoreCallingIdentity(oldId);
                    }
                } else {
                    Slog.w(TAG, "restorePackage: bad packageName=" + packageName + " or calling uid=" + Binder.getCallingUid());
                    throw new SecurityException("No permission to restore other packages");
                }
            } catch (NameNotFoundException e2) {
                Slog.w(TAG, "Asked to restore nonexistent pkg " + packageName);
                return -1;
            }
        } else {
            Slog.e(TAG, "Ignoring attempt to restore pkg=" + packageName + " on session for package " + this.mPackageName);
            return -1;
        }
    }

    public synchronized void endRestoreSession() {
        Slog.d(TAG, "endRestoreSession");
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
        } else if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else {
            this.backupManagerService.getBackupHandler().post(new EndRestoreRunnable(this.backupManagerService, this));
        }
    }
}
