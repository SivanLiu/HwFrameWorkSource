package com.android.server.backup.internal;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IRestoreObserver;
import android.app.backup.RestoreSet;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.TransportManager;
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
import com.android.server.backup.transport.TransportClient;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

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
    private final BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;

    public BackupHandler(BackupManagerService backupManagerService, Looper looper) {
        super(looper);
        this.backupManagerService = backupManagerService;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    /* JADX WARNING: Missing block: B:144:0x043d, code skipped:
            r21 = true;
     */
    /* JADX WARNING: Missing block: B:145:0x0443, code skipped:
            if (r14.size() <= 0) goto L_0x0488;
     */
    /* JADX WARNING: Missing block: B:148:0x0459, code skipped:
            r7 = r7;
            r23 = r14;
     */
    /* JADX WARNING: Missing block: B:150:?, code skipped:
            sendMessage(obtainMessage(20, new com.android.server.backup.internal.PerformBackupTask(r1.backupManagerService, r4, r5.transportDirName(), r14, r19, null, null, new com.android.server.backup.internal.-$$Lambda$BackupHandler$TJcRazGYTaUxjeiX6mPLlipfZUI(r3, r4), java.util.Collections.emptyList(), false, false)));
     */
    /* JADX WARNING: Missing block: B:151:0x0479, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:152:0x047b, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:153:0x047c, code skipped:
            r23 = r14;
     */
    /* JADX WARNING: Missing block: B:154:0x047e, code skipped:
            android.util.Slog.e(com.android.server.backup.BackupManagerService.TAG, "Transport became unavailable attempting backup or error initializing backup task", r0);
            r21 = false;
     */
    /* JADX WARNING: Missing block: B:155:0x0488, code skipped:
            r23 = r14;
            android.util.Slog.v(com.android.server.backup.BackupManagerService.TAG, "Backup requested but nothing pending");
            r21 = false;
     */
    /* JADX WARNING: Missing block: B:156:0x0493, code skipped:
            if (r21 == false) goto L_0x0495;
     */
    /* JADX WARNING: Missing block: B:157:0x0495, code skipped:
            r3.disposeOfTransportClient(r4, r2);
     */
    /* JADX WARNING: Missing block: B:158:0x049e, code skipped:
            monitor-enter(r1.backupManagerService.getQueueLock());
     */
    /* JADX WARNING: Missing block: B:160:?, code skipped:
            r1.backupManagerService.setBackupRunning(false);
     */
    /* JADX WARNING: Missing block: B:162:0x04a5, code skipped:
            r1.backupManagerService.getWakelock().release();
     */
    /* JADX WARNING: Missing block: B:200:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:201:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        Throwable th;
        ArrayList<BackupRequest> arrayList;
        Exception e;
        String str;
        StringBuilder stringBuilder;
        String str2;
        StringBuilder stringBuilder2;
        Message message = msg;
        TransportManager transportManager = this.backupManagerService.getTransportManager();
        TransportManager transportManager2;
        String str3;
        switch (message.what) {
            case 1:
                IBackupTransport transport;
                transportManager2 = transportManager;
                this.backupManagerService.setLastBackupPass(System.currentTimeMillis());
                String callerLogString = "BH/MSG_RUN_BACKUP";
                transportManager = transportManager2;
                TransportClient transportClient = transportManager.getCurrentTransportClient(callerLogString);
                if (transportClient != null) {
                    transport = transportClient.connect(callerLogString);
                } else {
                    transport = null;
                }
                if (transport == null) {
                    if (transportClient != null) {
                        transportManager.disposeOfTransportClient(transportClient, callerLogString);
                    }
                    Slog.v(BackupManagerService.TAG, "Backup requested but no transport available");
                    synchronized (this.backupManagerService.getQueueLock()) {
                        this.backupManagerService.setBackupRunning(false);
                    }
                    this.backupManagerService.getWakelock().release();
                    return;
                }
                ArrayList<BackupRequest> queue = new ArrayList();
                DataChangedJournal oldJournal = this.backupManagerService.getJournal();
                synchronized (this.backupManagerService.getQueueLock()) {
                    try {
                        if (this.backupManagerService.getPendingBackups().size() > 0) {
                            try {
                                for (BackupRequest b : this.backupManagerService.getPendingBackups().values()) {
                                    queue.add(b);
                                }
                                Slog.v(BackupManagerService.TAG, "clearing pending backups");
                                this.backupManagerService.getPendingBackups().clear();
                                this.backupManagerService.setJournal(null);
                            } catch (Throwable th2) {
                                th = th2;
                                arrayList = queue;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                                throw th;
                            }
                        }
                        break;
                    } catch (Throwable th4) {
                        th = th4;
                        arrayList = queue;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            case 2:
                AdbBackupParams params = message.obj;
                transportManager2 = transportManager;
                new Thread(new PerformAdbBackupTask(this.backupManagerService, params.fd, params.observer, params.includeApks, params.includeObbs, params.includeShared, params.doWidgets, params.curPassword, params.encryptPassword, params.allApps, params.includeSystem, params.doCompress, params.includeKeyValue, params.packages, params.latch), "adb-backup").start();
                transportManager = transportManager2;
                return;
            case 3:
                RestoreParams params2 = message.obj;
                str3 = BackupManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("MSG_RUN_RESTORE observer=");
                stringBuilder3.append(params2.observer);
                Slog.d(str3, stringBuilder3.toString());
                BackupManagerService backupManagerService = this.backupManagerService;
                TransportClient transportClient2 = params2.transportClient;
                IRestoreObserver iRestoreObserver = params2.observer;
                IBackupManagerMonitor iBackupManagerMonitor = params2.monitor;
                long j = params2.token;
                PackageInfo packageInfo = params2.packageInfo;
                int i = params2.pmToken;
                int i2 = i;
                PerformUnifiedRestoreTask task = new PerformUnifiedRestoreTask(backupManagerService, transportClient2, iRestoreObserver, iBackupManagerMonitor, j, packageInfo, i2, params2.isSystemRestore, params2.filterSet, params2.listener);
                synchronized (this.backupManagerService.getPendingRestores()) {
                    if (this.backupManagerService.isRestoreInProgress()) {
                        Slog.d(BackupManagerService.TAG, "Restore in progress, queueing.");
                        this.backupManagerService.getPendingRestores().add(task);
                    } else {
                        Slog.d(BackupManagerService.TAG, "Starting restore.");
                        this.backupManagerService.setRestoreInProgress(true);
                        sendMessage(obtainMessage(20, task));
                    }
                }
                return;
            case 4:
                ClearParams params3 = message.obj;
                new PerformClearTask(this.backupManagerService, params3.transportClient, params3.packageInfo, params3.listener).run();
                return;
            case 6:
                RestoreSet[] sets = null;
                RestoreGetSetsParams params4 = message.obj;
                String callerLogString2 = "BH/MSG_RUN_GET_RESTORE_SETS";
                try {
                    sets = params4.transportClient.connectOrThrow(callerLogString2).getAvailableRestoreSets();
                    synchronized (params4.session) {
                        params4.session.setRestoreSets(sets);
                    }
                    if (sets == null) {
                        EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    }
                    if (params4.observer != null) {
                        try {
                            params4.observer.restoreSetsAvailable(sets);
                        } catch (RemoteException e2) {
                            Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e3) {
                            e = e3;
                            str = BackupManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Restore observer threw: ");
                            stringBuilder.append(e.getMessage());
                            Slog.e(str, stringBuilder.toString());
                        }
                    }
                } catch (Exception e4) {
                    try {
                        str = BackupManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error from transport getting set list: ");
                        stringBuilder.append(e4.getMessage());
                        Slog.e(str, stringBuilder.toString());
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable(sets);
                            } catch (RemoteException e5) {
                                Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e6) {
                                e4 = e6;
                                str = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Restore observer threw: ");
                                stringBuilder.append(e4.getMessage());
                                Slog.e(str, stringBuilder.toString());
                            }
                        }
                    } catch (Throwable th5) {
                        RestoreSet[] sets2 = sets;
                        sets = th5;
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable(sets2);
                            } catch (RemoteException e7) {
                                Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e42) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Restore observer threw: ");
                                stringBuilder.append(e42.getMessage());
                                Slog.e(BackupManagerService.TAG, stringBuilder.toString());
                            }
                        }
                        removeMessages(8);
                        sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
                        params4.listener.onFinished(callerLogString2);
                    }
                }
                removeMessages(8);
                sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
                params4.listener.onFinished(callerLogString2);
                return;
            case 8:
                synchronized (this.backupManagerService) {
                    if (this.backupManagerService.getActiveRestoreSession() != null) {
                        Slog.w(BackupManagerService.TAG, "Restore session timed out; aborting");
                        this.backupManagerService.getActiveRestoreSession().markTimedOut();
                        ActiveRestoreSession activeRestoreSession = this.backupManagerService.getActiveRestoreSession();
                        Objects.requireNonNull(activeRestoreSession);
                        post(new EndRestoreRunnable(this.backupManagerService, this.backupManagerService.getActiveRestoreSession()));
                    }
                }
                return;
            case 9:
                synchronized (this.backupManagerService.getAdbBackupRestoreConfirmations()) {
                    AdbParams params5 = (AdbParams) this.backupManagerService.getAdbBackupRestoreConfirmations().get(message.arg1);
                    if (params5 != null) {
                        Slog.i(BackupManagerService.TAG, "Full backup/restore timed out waiting for user confirmation");
                        this.backupManagerService.signalAdbBackupRestoreCompletion(params5);
                        this.backupManagerService.getAdbBackupRestoreConfirmations().delete(message.arg1);
                        if (params5.observer != null) {
                            try {
                                params5.observer.onTimeout();
                            } catch (RemoteException e8) {
                            }
                        }
                    } else {
                        str3 = BackupManagerService.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("couldn't find params for token ");
                        stringBuilder4.append(message.arg1);
                        Slog.d(str3, stringBuilder4.toString());
                    }
                }
                return;
            case 10:
                AdbRestoreParams params6 = message.obj;
                new Thread(new PerformAdbRestoreTask(this.backupManagerService, params6.fd, params6.curPassword, params6.encryptPassword, params6.observer, params6.latch), "adb-restore").start();
                return;
            case 12:
                ClearRetryParams params7 = message.obj;
                this.backupManagerService.clearBackupData(params7.transportName, params7.packageName);
                return;
            case 13:
                this.backupManagerService.getContext().sendBroadcastAsUser(message.obj, UserHandle.SYSTEM);
                return;
            case 14:
                new Thread(message.obj, "transport-backup").start();
                return;
            case 15:
                BackupParams params8 = message.obj;
                ArrayList<BackupRequest> kvQueue = new ArrayList();
                Iterator it = params8.kvPackages.iterator();
                while (it.hasNext()) {
                    kvQueue.add(new BackupRequest((String) it.next()));
                }
                this.backupManagerService.setBackupRunning(true);
                this.backupManagerService.getWakelock().acquire();
                BackupManagerService backupManagerService2 = this.backupManagerService;
                TransportClient transportClient3 = params8.transportClient;
                String str4 = params8.dirName;
                IBackupObserver iBackupObserver = params8.observer;
                IBackupManagerMonitor iBackupManagerMonitor2 = params8.monitor;
                OnTaskFinishedListener onTaskFinishedListener = params8.listener;
                ArrayList arrayList2 = params8.fullPackages;
                sendMessage(obtainMessage(20, new PerformBackupTask(backupManagerService2, transportClient3, str4, kvQueue, null, iBackupObserver, iBackupManagerMonitor2, onTaskFinishedListener, arrayList2, true, params8.nonIncrementalBackup)));
                return;
            case 16:
                this.backupManagerService.dataChangedImpl(message.obj);
                return;
            case 17:
            case 18:
                str3 = BackupManagerService.TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Timeout message received for token=");
                stringBuilder5.append(Integer.toHexString(message.arg1));
                Slog.d(str3, stringBuilder5.toString());
                this.backupManagerService.handleCancel(message.arg1, false);
                return;
            case 20:
                try {
                    message.obj.execute();
                    return;
                } catch (ClassCastException e9) {
                    str2 = BackupManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid backup/restore task in flight, obj=");
                    stringBuilder2.append(message.obj);
                    Slog.e(str2, stringBuilder2.toString());
                    return;
                }
            case MSG_OP_COMPLETE /*21*/:
                try {
                    Pair<BackupRestoreTask, Long> taskWithResult = message.obj;
                    ((BackupRestoreTask) taskWithResult.first).operationComplete(((Long) taskWithResult.second).longValue());
                    return;
                } catch (ClassCastException e10) {
                    str2 = BackupManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid completion in flight, obj=");
                    stringBuilder2.append(message.obj);
                    Slog.e(str2, stringBuilder2.toString());
                    return;
                }
            default:
                return;
        }
    }
}
