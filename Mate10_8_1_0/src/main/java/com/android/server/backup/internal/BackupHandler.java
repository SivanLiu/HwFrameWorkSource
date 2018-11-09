package com.android.server.backup.internal;

import android.app.backup.RestoreSet;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.fullbackup.PerformAdbBackupTask;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.ActiveRestoreSession.EndRestoreRunnable;
import com.android.server.backup.restore.PerformAdbRestoreTask;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import java.util.ArrayList;
import java.util.Collections;

public class BackupHandler extends Handler {
    public static final int MSG_BACKUP_OPERATION_TIMEOUT = 17;
    public static final int MSG_BACKUP_RESTORE_STEP = 20;
    public static final int MSG_FULL_CONFIRMATION_TIMEOUT = 9;
    public static final int MSG_OP_COMPLETE = 21;
    public static final int MSG_REQUEST_BACKUP = 15;
    public static final int MSG_RESTORE_OPERATION_TIMEOUT = 18;
    public static final int MSG_RESTORE_SESSION_TIMEOUT = 8;
    public static final int MSG_RETRY_CLEAR = 12;
    public static final int MSG_RETRY_INIT = 11;
    public static final int MSG_RUN_ADB_BACKUP = 2;
    public static final int MSG_RUN_ADB_RESTORE = 10;
    public static final int MSG_RUN_BACKUP = 1;
    public static final int MSG_RUN_CLEAR = 4;
    public static final int MSG_RUN_FULL_TRANSPORT_BACKUP = 14;
    public static final int MSG_RUN_GET_RESTORE_SETS = 6;
    public static final int MSG_RUN_RESTORE = 3;
    public static final int MSG_SCHEDULE_BACKUP_PACKAGE = 16;
    public static final int MSG_WIDGET_BROADCAST = 13;
    private RefactoredBackupManagerService backupManagerService;

    public BackupHandler(RefactoredBackupManagerService backupManagerService, Looper looper) {
        super(looper);
        this.backupManagerService = backupManagerService;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                this.backupManagerService.setLastBackupPass(System.currentTimeMillis());
                IBackupTransport transport = this.backupManagerService.getTransportManager().getCurrentTransportBinder();
                if (transport == null) {
                    Slog.v(RefactoredBackupManagerService.TAG, "Backup requested but no transport available");
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.setBackupRunning(false);
                    }
                    this.backupManagerService.getWakelock().release();
                    return;
                }
                ArrayList<BackupRequest> queue = new ArrayList();
                DataChangedJournal oldJournal = this.backupManagerService.getJournal();
                synchronized (this.backupManagerService.getQueueLock()) {
                    if (this.backupManagerService.getPendingBackups().size() > 0) {
                        for (BackupRequest b : this.backupManagerService.getPendingBackups().values()) {
                            queue.add(b);
                        }
                        Slog.v(RefactoredBackupManagerService.TAG, "clearing pending backups");
                        this.backupManagerService.getPendingBackups().clear();
                        this.backupManagerService.setJournal(null);
                    }
                }
                boolean staged = true;
                if (queue.size() > 0) {
                    try {
                        sendMessage(obtainMessage(20, new PerformBackupTask(this.backupManagerService, transport, transport.transportDirName(), queue, oldJournal, null, null, Collections.emptyList(), false, false)));
                    } catch (Throwable e) {
                        Slog.e(RefactoredBackupManagerService.TAG, "Transport became unavailable attempting backup or error initializing backup task", e);
                        staged = false;
                    }
                } else {
                    Slog.v(RefactoredBackupManagerService.TAG, "Backup requested but nothing pending");
                    staged = false;
                }
                if (!staged) {
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.setBackupRunning(false);
                    }
                    this.backupManagerService.getWakelock().release();
                    return;
                }
                return;
            case 2:
                AdbBackupParams params = msg.obj;
                new Thread(new PerformAdbBackupTask(this.backupManagerService, params.fd, params.observer, params.includeApks, params.includeObbs, params.includeShared, params.doWidgets, params.curPassword, params.encryptPassword, params.allApps, params.includeSystem, params.doCompress, params.includeKeyValue, params.packages, params.latch), "adb-backup").start();
                return;
            case 3:
                RestoreParams params2 = msg.obj;
                Slog.d(RefactoredBackupManagerService.TAG, "MSG_RUN_RESTORE observer=" + params2.observer);
                PerformUnifiedRestoreTask task = new PerformUnifiedRestoreTask(this.backupManagerService, params2.transport, params2.observer, params2.monitor, params2.token, params2.pkgInfo, params2.pmToken, params2.isSystemRestore, params2.filterSet);
                synchronized (this.backupManagerService.getPendingRestores()) {
                    if (this.backupManagerService.isRestoreInProgress()) {
                        Slog.d(RefactoredBackupManagerService.TAG, "Restore in progress, queueing.");
                        this.backupManagerService.getPendingRestores().add(task);
                    } else {
                        Slog.d(RefactoredBackupManagerService.TAG, "Starting restore.");
                        this.backupManagerService.setRestoreInProgress(true);
                        sendMessage(obtainMessage(20, task));
                    }
                }
                return;
            case 4:
                ClearParams params3 = msg.obj;
                new PerformClearTask(this.backupManagerService, params3.transport, params3.packageInfo).run();
                return;
            case 6:
                RestoreSet[] restoreSetArr = null;
                RestoreGetSetsParams params4 = msg.obj;
                try {
                    restoreSetArr = params4.transport.getAvailableRestoreSets();
                    synchronized (params4.session) {
                        params4.session.mRestoreSets = restoreSetArr;
                    }
                    if (restoreSetArr == null) {
                        EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    }
                    if (params4.observer != null) {
                        try {
                            params4.observer.restoreSetsAvailable(restoreSetArr);
                        } catch (RemoteException e2) {
                            Slog.e(RefactoredBackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e3) {
                            Slog.e(RefactoredBackupManagerService.TAG, "Restore observer threw: " + e3.getMessage());
                        }
                    }
                    removeMessages(8);
                    sendEmptyMessageDelayed(8, 60000);
                    this.backupManagerService.getWakelock().release();
                    return;
                } catch (Exception e32) {
                    try {
                        Slog.e(RefactoredBackupManagerService.TAG, "Error from transport getting set list: " + e32.getMessage());
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable(restoreSetArr);
                            } catch (RemoteException e4) {
                                Slog.e(RefactoredBackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e322) {
                                Slog.e(RefactoredBackupManagerService.TAG, "Restore observer threw: " + e322.getMessage());
                            }
                        }
                        removeMessages(8);
                        sendEmptyMessageDelayed(8, 60000);
                        this.backupManagerService.getWakelock().release();
                        return;
                    } catch (Throwable th) {
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable(restoreSetArr);
                            } catch (RemoteException e5) {
                                Slog.e(RefactoredBackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e3222) {
                                Slog.e(RefactoredBackupManagerService.TAG, "Restore observer threw: " + e3222.getMessage());
                            }
                        }
                        removeMessages(8);
                        sendEmptyMessageDelayed(8, 60000);
                        this.backupManagerService.getWakelock().release();
                    }
                }
            case 8:
                synchronized (this.backupManagerService) {
                    if (this.backupManagerService.getActiveRestoreSession() != null) {
                        Slog.w(RefactoredBackupManagerService.TAG, "Restore session timed out; aborting");
                        this.backupManagerService.getActiveRestoreSession().markTimedOut();
                        ActiveRestoreSession activeRestoreSession = this.backupManagerService.getActiveRestoreSession();
                        activeRestoreSession.getClass();
                        post(new EndRestoreRunnable(this.backupManagerService, this.backupManagerService.getActiveRestoreSession()));
                    }
                }
                return;
            case 9:
                synchronized (this.backupManagerService.getAdbBackupRestoreConfirmations()) {
                    AdbParams params5 = (AdbParams) this.backupManagerService.getAdbBackupRestoreConfirmations().get(msg.arg1);
                    if (params5 != null) {
                        Slog.i(RefactoredBackupManagerService.TAG, "Full backup/restore timed out waiting for user confirmation");
                        this.backupManagerService.signalAdbBackupRestoreCompletion(params5);
                        this.backupManagerService.getAdbBackupRestoreConfirmations().delete(msg.arg1);
                        if (params5.observer != null) {
                            try {
                                params5.observer.onTimeout();
                            } catch (RemoteException e6) {
                            }
                        }
                    } else {
                        Slog.d(RefactoredBackupManagerService.TAG, "couldn't find params for token " + msg.arg1);
                    }
                }
                return;
            case 10:
                AdbRestoreParams params6 = msg.obj;
                new Thread(new PerformAdbRestoreTask(this.backupManagerService, params6.fd, params6.curPassword, params6.encryptPassword, params6.observer, params6.latch), "adb-restore").start();
                return;
            case 11:
                synchronized (this.backupManagerService.getQueueLock()) {
                    this.backupManagerService.recordInitPendingLocked(msg.arg1 != 0, (String) msg.obj);
                    this.backupManagerService.getAlarmManager().set(0, System.currentTimeMillis(), this.backupManagerService.getRunInitIntent());
                }
                return;
            case 12:
                ClearRetryParams params7 = msg.obj;
                this.backupManagerService.clearBackupData(params7.transportName, params7.packageName);
                return;
            case 13:
                this.backupManagerService.getContext().sendBroadcastAsUser(msg.obj, UserHandle.SYSTEM);
                return;
            case 14:
                new Thread(msg.obj, "transport-backup").start();
                return;
            case 15:
                BackupParams params8 = msg.obj;
                ArrayList<BackupRequest> kvQueue = new ArrayList();
                for (String packageName : params8.kvPackages) {
                    kvQueue.add(new BackupRequest(packageName));
                }
                this.backupManagerService.setBackupRunning(true);
                this.backupManagerService.getWakelock().acquire();
                sendMessage(obtainMessage(20, new PerformBackupTask(this.backupManagerService, params8.transport, params8.dirName, kvQueue, null, params8.observer, params8.monitor, params8.fullPackages, true, params8.nonIncrementalBackup)));
                return;
            case 16:
                this.backupManagerService.dataChangedImpl(msg.obj);
                return;
            case 17:
            case 18:
                Slog.d(RefactoredBackupManagerService.TAG, "Timeout message received for token=" + Integer.toHexString(msg.arg1));
                this.backupManagerService.handleCancel(msg.arg1, false);
                return;
            case 20:
                try {
                    msg.obj.execute();
                    return;
                } catch (ClassCastException e7) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Invalid backup task in flight, obj=" + msg.obj);
                    return;
                }
            case 21:
                try {
                    Pair<BackupRestoreTask, Long> taskWithResult = msg.obj;
                    ((BackupRestoreTask) taskWithResult.first).operationComplete(((Long) taskWithResult.second).longValue());
                    return;
                } catch (ClassCastException e8) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Invalid completion in flight, obj=" + msg.obj);
                    return;
                }
            default:
                return;
        }
    }
}
