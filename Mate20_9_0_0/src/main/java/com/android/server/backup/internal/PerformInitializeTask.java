package com.android.server.backup.internal;

import android.app.backup.IBackupObserver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.transport.TransportClient;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;
import java.util.ArrayList;

public class PerformInitializeTask implements Runnable {
    private final BackupManagerService mBackupManagerService;
    private final File mBaseStateDir;
    private final OnTaskFinishedListener mListener;
    private IBackupObserver mObserver;
    private final String[] mQueue;
    private final TransportManager mTransportManager;

    public PerformInitializeTask(BackupManagerService backupManagerService, String[] transportNames, IBackupObserver observer, OnTaskFinishedListener listener) {
        this(backupManagerService, backupManagerService.getTransportManager(), transportNames, observer, listener, backupManagerService.getBaseStateDir());
    }

    @VisibleForTesting
    PerformInitializeTask(BackupManagerService backupManagerService, TransportManager transportManager, String[] transportNames, IBackupObserver observer, OnTaskFinishedListener listener, File baseStateDir) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = transportManager;
        this.mQueue = transportNames;
        this.mObserver = observer;
        this.mListener = listener;
        this.mBaseStateDir = baseStateDir;
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

    /* JADX WARNING: Removed duplicated region for block: B:53:0x0193 A:{LOOP_END, LOOP:3: B:51:0x018d->B:53:0x0193} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x017c A:{LOOP_END, LOOP:2: B:46:0x0176->B:48:0x017c} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0193 A:{LOOP_END, LOOP:3: B:51:0x018d->B:53:0x0193} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x017c A:{LOOP_END, LOOP:2: B:46:0x0176->B:48:0x017c} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x017c A:{LOOP_END, LOOP:2: B:46:0x0176->B:48:0x017c} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0193 A:{LOOP_END, LOOP:3: B:51:0x018d->B:53:0x0193} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        int result;
        Exception e;
        Throwable th;
        String callerLogString = "PerformInitializeTask.run()";
        ArrayList<TransportClient> transportClientsToDisposeOf = new ArrayList(this.mQueue.length);
        int result2 = 0;
        try {
            String[] strArr = this.mQueue;
            int length = strArr.length;
            result = result2;
            result2 = 0;
            while (result2 < length) {
                try {
                    String[] strArr2;
                    int i;
                    int millis;
                    int i2;
                    String transportName = strArr[result2];
                    TransportClient transportClient = this.mTransportManager.getTransportClient(transportName, callerLogString);
                    String str;
                    StringBuilder stringBuilder;
                    if (transportClient == null) {
                        str = BackupManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Requested init for ");
                        stringBuilder.append(transportName);
                        stringBuilder.append(" but not found");
                        Slog.e(str, stringBuilder.toString());
                        strArr2 = strArr;
                        i = length;
                    } else {
                        transportClientsToDisposeOf.add(transportClient);
                        str = BackupManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Initializing (wiping) backup transport storage: ");
                        stringBuilder.append(transportName);
                        Slog.i(str, stringBuilder.toString());
                        str = this.mTransportManager.getTransportDirName(transportClient.getTransportComponent());
                        EventLog.writeEvent(EventLogTags.BACKUP_START, str);
                        long startRealtime = SystemClock.elapsedRealtime();
                        IBackupTransport transport = transportClient.connectOrThrow(callerLogString);
                        int status = transport.initializeDevice();
                        if (status == 0) {
                            status = transport.finishBackup();
                        }
                        if (status == 0) {
                            Slog.i(BackupManagerService.TAG, "Device init successful");
                            strArr2 = strArr;
                            i = length;
                            millis = (int) (SystemClock.elapsedRealtime() - startRealtime);
                            EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                            this.mBackupManagerService.resetBackupState(new File(this.mBaseStateDir, str));
                            EventLog.writeEvent(EventLogTags.BACKUP_SUCCESS, new Object[]{Integer.valueOf(0), Integer.valueOf(millis)});
                            this.mBackupManagerService.recordInitPending(false, transportName, str);
                            notifyResult(transportName, 0);
                        } else {
                            long delay;
                            String str2;
                            StringBuilder stringBuilder2;
                            int result3;
                            strArr2 = strArr;
                            i = length;
                            Slog.e(BackupManagerService.TAG, "Transport error in initializeDevice()");
                            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                            this.mBackupManagerService.recordInitPending(true, transportName, str);
                            notifyResult(transportName, status);
                            int result4 = status;
                            try {
                                delay = transport.requestBackupTime();
                                str2 = BackupManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                result3 = result4;
                            } catch (Exception e2) {
                                e = e2;
                                result2 = result4;
                                try {
                                    Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
                                    result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                    for (TransportClient transportClient2 : transportClientsToDisposeOf) {
                                        this.mTransportManager.disposeOfTransportClient(transportClient2, callerLogString);
                                    }
                                    notifyFinished(result);
                                    this.mListener.onFinished(callerLogString);
                                } catch (Throwable th2) {
                                    th = th2;
                                    result = result2;
                                    for (TransportClient transportClient3 : transportClientsToDisposeOf) {
                                        this.mTransportManager.disposeOfTransportClient(transportClient3, callerLogString);
                                    }
                                    notifyFinished(result);
                                    this.mListener.onFinished(callerLogString);
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                result = result4;
                                while (r4.hasNext()) {
                                }
                                notifyFinished(result);
                                this.mListener.onFinished(callerLogString);
                                throw th;
                            }
                            try {
                                stringBuilder2.append("Init failed on ");
                                stringBuilder2.append(transportName);
                                stringBuilder2.append(" resched in ");
                                stringBuilder2.append(delay);
                                Slog.w(str2, stringBuilder2.toString());
                                i2 = 0;
                                this.mBackupManagerService.getAlarmManager().set(0, System.currentTimeMillis() + delay, this.mBackupManagerService.getRunInitIntent());
                                result = result3;
                                result2++;
                                millis = i2;
                                strArr = strArr2;
                                length = i;
                            } catch (Exception e3) {
                                e = e3;
                                result2 = result3;
                                Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
                                result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                while (r0.hasNext()) {
                                }
                                notifyFinished(result);
                                this.mListener.onFinished(callerLogString);
                            } catch (Throwable th4) {
                                th = th4;
                                result = result3;
                                while (r4.hasNext()) {
                                }
                                notifyFinished(result);
                                this.mListener.onFinished(callerLogString);
                                throw th;
                            }
                        }
                    }
                    i2 = 0;
                    result2++;
                    millis = i2;
                    strArr = strArr2;
                    length = i;
                } catch (Exception e4) {
                    e = e4;
                    result2 = result;
                    Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
                    result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    while (r0.hasNext()) {
                    }
                    notifyFinished(result);
                    this.mListener.onFinished(callerLogString);
                } catch (Throwable th5) {
                    th = th5;
                    while (r4.hasNext()) {
                    }
                    notifyFinished(result);
                    this.mListener.onFinished(callerLogString);
                    throw th;
                }
            }
            for (TransportClient transportClient22 : transportClientsToDisposeOf) {
                this.mTransportManager.disposeOfTransportClient(transportClient22, callerLogString);
            }
        } catch (Exception e5) {
            e = e5;
            Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
            result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            while (r0.hasNext()) {
            }
            notifyFinished(result);
            this.mListener.onFinished(callerLogString);
        }
        notifyFinished(result);
        this.mListener.onFinished(callerLogString);
    }
}
