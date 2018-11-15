package com.android.server.backup.internal;

import android.app.backup.IBackupObserver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.EventLogTags;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;

public class PerformInitializeTask implements Runnable {
    private RefactoredBackupManagerService backupManagerService;
    IBackupObserver mObserver;
    String[] mQueue;

    public PerformInitializeTask(RefactoredBackupManagerService backupManagerService, String[] transportNames, IBackupObserver observer) {
        this.backupManagerService = backupManagerService;
        this.mQueue = transportNames;
        this.mObserver = observer;
    }

    private void notifyResult(String target, int status) {
        try {
            if (this.mObserver != null) {
                this.mObserver.onResult(target, status);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    private void notifyFinished(int status) {
        try {
            if (this.mObserver != null) {
                this.mObserver.backupFinished(status);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    public void run() {
        int result = 0;
        for (String transportName : this.mQueue) {
            IBackupTransport transport = this.backupManagerService.getTransportManager().getTransportBinder(transportName);
            if (transport == null) {
                Slog.e(RefactoredBackupManagerService.TAG, "Requested init for " + transportName + " but not found");
            } else {
                Slog.i(RefactoredBackupManagerService.TAG, "Initializing (wiping) backup transport storage: " + transportName);
                EventLog.writeEvent(EventLogTags.BACKUP_START, transport.transportDirName());
                long startRealtime = SystemClock.elapsedRealtime();
                int status = transport.initializeDevice();
                if (status == 0) {
                    status = transport.finishBackup();
                }
                if (status == 0) {
                    Slog.i(RefactoredBackupManagerService.TAG, "Device init successful");
                    int millis = (int) (SystemClock.elapsedRealtime() - startRealtime);
                    EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                    this.backupManagerService.resetBackupState(new File(this.backupManagerService.getBaseStateDir(), transport.transportDirName()));
                    EventLog.writeEvent(EventLogTags.BACKUP_SUCCESS, new Object[]{Integer.valueOf(0), Integer.valueOf(millis)});
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.recordInitPendingLocked(false, transportName);
                        try {
                        } catch (Exception e) {
                            Slog.e(RefactoredBackupManagerService.TAG, "Unexpected error performing init", e);
                            notifyFinished(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                            this.backupManagerService.getWakelock().release();
                            return;
                        } catch (Throwable th) {
                            notifyFinished(result);
                            this.backupManagerService.getWakelock().release();
                        }
                    }
                    notifyResult(transportName, 0);
                } else {
                    Slog.e(RefactoredBackupManagerService.TAG, "Transport error in initializeDevice()");
                    EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.recordInitPendingLocked(true, transportName);
                    }
                    notifyResult(transportName, status);
                    result = status;
                    long delay = transport.requestBackupTime();
                    Slog.w(RefactoredBackupManagerService.TAG, "Init failed on " + transportName + " resched in " + delay);
                    this.backupManagerService.getAlarmManager().set(0, System.currentTimeMillis() + delay, this.backupManagerService.getRunInitIntent());
                }
            }
        }
        notifyFinished(result);
        this.backupManagerService.getWakelock().release();
    }
}
