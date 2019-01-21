package com.android.server.backup.internal;

import android.app.IBackupAgent;
import android.app.IBackupAgent.Stub;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.WorkSource;
import android.system.ErrnoException;
import android.system.Os;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.KeyValueBackupJob;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportUtils;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class PerformBackupTask implements BackupRestoreTask {
    private static final String TAG = "PerformBackupTask";
    private BackupManagerService backupManagerService;
    private IBackupAgent mAgentBinder;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private volatile boolean mCancelAll;
    private final Object mCancelLock = new Object();
    private final int mCurrentOpToken;
    private PackageInfo mCurrentPackage;
    private BackupState mCurrentState;
    private volatile int mEphemeralOpToken;
    private boolean mFinished;
    private final PerformFullTransportBackupTask mFullBackupTask;
    private DataChangedJournal mJournal;
    private final OnTaskFinishedListener mListener;
    private IBackupManagerMonitor mMonitor;
    private ParcelFileDescriptor mNewState;
    private File mNewStateName;
    private final boolean mNonIncremental;
    private IBackupObserver mObserver;
    private ArrayList<BackupRequest> mOriginalQueue;
    private List<String> mPendingFullBackups;
    private ArrayList<BackupRequest> mQueue;
    private ParcelFileDescriptor mSavedState;
    private File mSavedStateName;
    private File mStateDir;
    private int mStatus;
    private final TransportClient mTransportClient;
    private final boolean mUserInitiated;

    public PerformBackupTask(BackupManagerService backupManagerService, TransportClient transportClient, String dirName, ArrayList<BackupRequest> queue, DataChangedJournal journal, IBackupObserver observer, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, List<String> pendingFullBackups, boolean userInitiated, boolean nonIncremental) {
        BackupManagerService backupManagerService2 = backupManagerService;
        this.backupManagerService = backupManagerService2;
        TransportClient transportClient2 = transportClient;
        this.mTransportClient = transportClient2;
        this.mOriginalQueue = queue;
        this.mQueue = new ArrayList();
        this.mJournal = journal;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mListener = listener != null ? listener : OnTaskFinishedListener.NOP;
        this.mPendingFullBackups = pendingFullBackups;
        this.mUserInitiated = userInitiated;
        this.mNonIncremental = nonIncremental;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mStateDir = new File(backupManagerService.getBaseStateDir(), dirName);
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mFinished = false;
        synchronized (backupManagerService.getCurrentOpLock()) {
            if (backupManagerService.isBackupOperationInProgress()) {
                Slog.d(TAG, "Skipping backup since one is already in progress.");
                this.mCancelAll = true;
                this.mFullBackupTask = null;
                this.mCurrentState = BackupState.FINAL;
                backupManagerService2.addBackupTrace("Skipped. Backup already in progress.");
            } else {
                this.mCurrentState = BackupState.INITIAL;
                String[] fullBackups = (String[]) this.mPendingFullBackups.toArray(new String[this.mPendingFullBackups.size()]);
                this.mFullBackupTask = new PerformFullTransportBackupTask(backupManagerService2, transportClient2, null, fullBackups, false, null, new CountDownLatch(1), this.mObserver, this.mMonitor, this.mListener, this.mUserInitiated);
                registerTask();
                backupManagerService2.addBackupTrace("STATE => INITIAL");
            }
        }
    }

    private void registerTask() {
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 2));
        }
    }

    private void unregisterTask() {
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    @GuardedBy("mCancelLock")
    public void execute() {
        synchronized (this.mCancelLock) {
            switch (this.mCurrentState) {
                case INITIAL:
                    beginBackup();
                    break;
                case BACKUP_PM:
                    backupPm();
                    break;
                case RUNNING_QUEUE:
                    invokeNextAgent();
                    break;
                case FINAL:
                    if (!this.mFinished) {
                        finalizeBackup();
                        break;
                    } else {
                        Slog.e(TAG, "Duplicate finish of K/V pass");
                        break;
                    }
                default:
                    break;
            }
        }
    }

    /* JADX WARNING: Missing block: B:33:0x0177, code skipped:
            if (r10.mStatus != 0) goto L_0x0179;
     */
    /* JADX WARNING: Missing block: B:34:0x0179, code skipped:
            r10.backupManagerService.resetBackupState(r10.mStateDir);
            com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r10.mObserver, com.android.server.job.JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            executeNextState(com.android.server.backup.internal.BackupState.FINAL);
     */
    /* JADX WARNING: Missing block: B:40:0x01c7, code skipped:
            if (r10.mStatus == 0) goto L_0x01ca;
     */
    /* JADX WARNING: Missing block: B:41:0x01ca, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void beginBackup() {
        BackupManagerService backupManagerService;
        StringBuilder stringBuilder;
        this.backupManagerService.clearBackupTrace();
        StringBuilder b = new StringBuilder(256);
        b.append("beginBackup: [");
        Iterator it = this.mOriginalQueue.iterator();
        while (it.hasNext()) {
            BackupRequest req = (BackupRequest) it.next();
            b.append(' ');
            b.append(req.packageName);
        }
        b.append(" ]");
        this.backupManagerService.addBackupTrace(b.toString());
        this.mAgentBinder = null;
        this.mStatus = 0;
        if (this.mOriginalQueue.isEmpty() && this.mPendingFullBackups.isEmpty()) {
            Slog.w(TAG, "Backup begun with an empty queue - nothing to do.");
            this.backupManagerService.addBackupTrace("queue empty at begin");
            BackupObserverUtils.sendBackupFinished(this.mObserver, 0);
            executeNextState(BackupState.FINAL);
            return;
        }
        this.mQueue = (ArrayList) this.mOriginalQueue.clone();
        boolean skipPm = this.mNonIncremental;
        for (int i = 0; i < this.mQueue.size(); i++) {
            if (BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(((BackupRequest) this.mQueue.get(i)).packageName)) {
                this.mQueue.remove(i);
                skipPm = false;
                break;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Beginning backup of ");
        stringBuilder2.append(this.mQueue.size());
        stringBuilder2.append(" targets");
        Slog.v(str, stringBuilder2.toString());
        File pmState = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
        BackupManagerService backupManagerService2;
        StringBuilder stringBuilder3;
        try {
            IBackupTransport transport = this.mTransportClient.connectOrThrow("PBT.beginBackup()");
            String transportName = transport.transportDirName();
            EventLog.writeEvent(EventLogTags.BACKUP_START, transportName);
            if (this.mStatus == 0 && pmState.length() <= 0) {
                Slog.i(TAG, "Initializing (wiping) backup state and transport storage");
                BackupManagerService backupManagerService3 = this.backupManagerService;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("initializing transport ");
                stringBuilder4.append(transportName);
                backupManagerService3.addBackupTrace(stringBuilder4.toString());
                this.backupManagerService.resetBackupState(this.mStateDir);
                this.mStatus = transport.initializeDevice();
                backupManagerService3 = this.backupManagerService;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("transport.initializeDevice() == ");
                stringBuilder4.append(this.mStatus);
                backupManagerService3.addBackupTrace(stringBuilder4.toString());
                if (this.mStatus == 0) {
                    EventLog.writeEvent(EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                } else {
                    EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                    Slog.e(TAG, "Transport error in initializeDevice()");
                }
            }
            if (skipPm) {
                Slog.d(TAG, "Skipping backup of package metadata.");
                executeNextState(BackupState.RUNNING_QUEUE);
            } else if (this.mStatus == 0) {
                executeNextState(BackupState.BACKUP_PM);
            }
            backupManagerService2 = this.backupManagerService;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("exiting prelim: ");
            stringBuilder3.append(this.mStatus);
            backupManagerService2.addBackupTrace(stringBuilder3.toString());
        } catch (Exception e) {
            Slog.e(TAG, "Error in backup thread during init", e);
            backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in backup thread during init: ");
            stringBuilder.append(e);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            backupManagerService2 = this.backupManagerService;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("exiting prelim: ");
            stringBuilder3.append(this.mStatus);
            backupManagerService2.addBackupTrace(stringBuilder3.toString());
        } catch (Throwable th) {
            backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exiting prelim: ");
            stringBuilder.append(this.mStatus);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                executeNextState(BackupState.FINAL);
            }
        }
    }

    private void backupPm() {
        BackupManagerService backupManagerService;
        StringBuilder stringBuilder;
        BackupManagerService backupManagerService2;
        StringBuilder stringBuilder2;
        try {
            this.mStatus = invokeAgentForBackup(BackupManagerService.PACKAGE_MANAGER_SENTINEL, Stub.asInterface(this.backupManagerService.makeMetadataAgent().onBind()));
            backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PMBA invoke: ");
            stringBuilder.append(this.mStatus);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            this.backupManagerService.getBackupHandler().removeMessages(17);
            backupManagerService2 = this.backupManagerService;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exiting backupPm: ");
            stringBuilder2.append(this.mStatus);
            backupManagerService2.addBackupTrace(stringBuilder2.toString());
            if (this.mStatus == 0) {
                return;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error in backup thread during pm", e);
            backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in backup thread during pm: ");
            stringBuilder.append(e);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            backupManagerService2 = this.backupManagerService;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exiting backupPm: ");
            stringBuilder2.append(this.mStatus);
            backupManagerService2.addBackupTrace(stringBuilder2.toString());
            if (this.mStatus == 0) {
                return;
            }
        } catch (Throwable th) {
            backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exiting backupPm: ");
            stringBuilder.append(this.mStatus);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, invokeAgentToObserverError(this.mStatus));
                executeNextState(BackupState.FINAL);
            }
            throw th;
        }
        this.backupManagerService.resetBackupState(this.mStateDir);
        BackupObserverUtils.sendBackupFinished(this.mObserver, invokeAgentToObserverError(this.mStatus));
        executeNextState(BackupState.FINAL);
    }

    private int invokeAgentToObserverError(int error) {
        if (error == -1003) {
            return -1003;
        }
        return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
    }

    /* JADX WARNING: Missing block: B:83:0x0295, code skipped:
            if (r11.mStatus == -1004) goto L_0x02e2;
     */
    /* JADX WARNING: Missing block: B:97:0x02e0, code skipped:
            if (r11.mStatus == -1004) goto L_0x02e2;
     */
    /* JADX WARNING: Missing block: B:98:0x02e2, code skipped:
            r11.mStatus = 0;
            com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r11.mObserver, r1.packageName, -2002);
     */
    /* JADX WARNING: Missing block: B:99:0x02ec, code skipped:
            revertAndEndBackup();
            r6 = com.android.server.backup.internal.BackupState.FINAL;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void invokeNextAgent() {
        this.mStatus = 0;
        BackupManagerService backupManagerService = this.backupManagerService;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invoke q=");
        stringBuilder.append(this.mQueue.size());
        backupManagerService.addBackupTrace(stringBuilder.toString());
        if (this.mQueue.isEmpty()) {
            executeNextState(BackupState.FINAL);
            return;
        }
        BackupRequest request = (BackupRequest) this.mQueue.get(0);
        this.mQueue.remove(0);
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("starting key/value backup of ");
        stringBuilder2.append(request);
        Slog.d(str, stringBuilder2.toString());
        BackupManagerService backupManagerService2 = this.backupManagerService;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("launch agent for ");
        stringBuilder2.append(request.packageName);
        backupManagerService2.addBackupTrace(stringBuilder2.toString());
        BackupState nextState;
        BackupState nextState2;
        try {
            PackageManager pm = this.backupManagerService.getPackageManager();
            this.mCurrentPackage = pm.getPackageInfo(request.packageName, 134217728);
            String str2;
            StringBuilder stringBuilder3;
            if (!AppBackupUtils.appIsEligibleForBackup(this.mCurrentPackage.applicationInfo, pm)) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Package ");
                stringBuilder3.append(request.packageName);
                stringBuilder3.append(" no longer supports backup; skipping");
                Slog.i(str2, stringBuilder3.toString());
                this.backupManagerService.addBackupTrace("skipping - not eligible, completion is noop");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus != 0) {
                    nextState = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        this.backupManagerService.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState = BackupState.FINAL;
                        }
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, request.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            } else if (AppBackupUtils.appGetsFullBackup(this.mCurrentPackage)) {
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Package ");
                stringBuilder3.append(request.packageName);
                stringBuilder3.append(" requests full-data rather than key/value; skipping");
                Slog.i(str2, stringBuilder3.toString());
                this.backupManagerService.addBackupTrace("skipping - fullBackupOnly, completion is noop");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus != 0) {
                    nextState = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        this.backupManagerService.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState = BackupState.FINAL;
                        }
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, request.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            } else if (AppBackupUtils.appIsStopped(this.mCurrentPackage.applicationInfo)) {
                this.backupManagerService.addBackupTrace("skipping - stopped");
                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2001);
                executeNextState(BackupState.RUNNING_QUEUE);
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus != 0) {
                    nextState = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        this.backupManagerService.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState = BackupState.FINAL;
                        }
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                    } else if (this.mStatus == -1004) {
                        this.mStatus = 0;
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, request.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            } else {
                IBackupAgent agent = null;
                try {
                    this.backupManagerService.getWakelock().setWorkSource(new WorkSource(this.mCurrentPackage.applicationInfo.uid));
                    agent = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
                    BackupManagerService backupManagerService3 = this.backupManagerService;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("agent bound; a? = ");
                    stringBuilder3.append(agent != null);
                    backupManagerService3.addBackupTrace(stringBuilder3.toString());
                    if (agent != null) {
                        this.mAgentBinder = agent;
                        this.mStatus = invokeAgentForBackup(request.packageName, agent);
                    } else {
                        this.mStatus = -1003;
                    }
                } catch (SecurityException ex) {
                    Slog.d(TAG, "error in bind/backup", ex);
                    this.mStatus = -1003;
                    this.backupManagerService.addBackupTrace("agent SE");
                }
                this.backupManagerService.getWakelock().setWorkSource(null);
                if (this.mStatus != 0) {
                    nextState2 = BackupState.RUNNING_QUEUE;
                    this.mAgentBinder = null;
                    if (this.mStatus == -1003) {
                        this.backupManagerService.dataChangedImpl(request.packageName);
                        this.mStatus = 0;
                        if (this.mQueue.isEmpty()) {
                            nextState2 = BackupState.FINAL;
                        }
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                        executeNextState(nextState2);
                    }
                }
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
            }
        } catch (NameNotFoundException e) {
            Slog.d(TAG, "Package does not exist; skipping");
            this.backupManagerService.addBackupTrace("no such package");
            this.mStatus = -1004;
            this.backupManagerService.getWakelock().setWorkSource(null);
            if (this.mStatus != 0) {
                nextState2 = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(request.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        nextState2 = BackupState.FINAL;
                    }
                }
            }
        } catch (Throwable th) {
            this.backupManagerService.getWakelock().setWorkSource(null);
            if (this.mStatus != 0) {
                nextState = BackupState.RUNNING_QUEUE;
                this.mAgentBinder = null;
                if (this.mStatus == -1003) {
                    this.backupManagerService.dataChangedImpl(request.packageName);
                    this.mStatus = 0;
                    if (this.mQueue.isEmpty()) {
                        nextState = BackupState.FINAL;
                    }
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -1003);
                } else if (this.mStatus == -1004) {
                    this.mStatus = 0;
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, request.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    nextState = BackupState.FINAL;
                }
                executeNextState(nextState);
            } else {
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
            }
            throw th;
        }
    }

    private void finalizeBackup() {
        String str;
        this.backupManagerService.addBackupTrace("finishing");
        Iterator it = this.mQueue.iterator();
        while (it.hasNext()) {
            this.backupManagerService.dataChangedImpl(((BackupRequest) it.next()).packageName);
        }
        if (!(this.mJournal == null || this.mJournal.delete())) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to remove backup journal file ");
            stringBuilder.append(this.mJournal);
            Slog.e(str, stringBuilder.toString());
        }
        str = "PBT.finalizeBackup()";
        if (this.backupManagerService.getCurrentToken() == 0 && this.mStatus == 0) {
            this.backupManagerService.addBackupTrace("success; recording token");
            try {
                this.backupManagerService.setCurrentToken(this.mTransportClient.connectOrThrow(str).getCurrentRestoreSet());
                this.backupManagerService.writeRestoreTokens();
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Transport threw reporting restore set: ");
                stringBuilder2.append(e.getMessage());
                Slog.e(str2, stringBuilder2.toString());
                this.backupManagerService.addBackupTrace("transport threw returning token");
            }
        }
        synchronized (this.backupManagerService.getQueueLock()) {
            this.backupManagerService.setBackupRunning(false);
            if (this.mStatus == JobSchedulerShellCommand.CMD_ERR_NO_JOB) {
                this.backupManagerService.addBackupTrace("init required; rerunning");
                try {
                    this.backupManagerService.getPendingInits().add(this.backupManagerService.getTransportManager().getTransportName(this.mTransportClient.getTransportComponent()));
                } catch (Exception e2) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failed to query transport name for init: ");
                    stringBuilder3.append(e2.getMessage());
                    Slog.w(str3, stringBuilder3.toString());
                }
                clearMetadata();
                this.backupManagerService.backupNow();
            }
        }
        this.backupManagerService.clearBackupTrace();
        unregisterTask();
        if (!this.mCancelAll && this.mStatus == 0 && this.mPendingFullBackups != null && !this.mPendingFullBackups.isEmpty()) {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Starting full backups for: ");
            stringBuilder4.append(this.mPendingFullBackups);
            Slog.d(str4, stringBuilder4.toString());
            this.backupManagerService.getWakelock().acquire();
            new Thread(this.mFullBackupTask, "full-transport-requested").start();
        } else if (this.mCancelAll) {
            this.mListener.onFinished(str);
            if (this.mFullBackupTask != null) {
                this.mFullBackupTask.unregisterTask();
            }
            BackupObserverUtils.sendBackupFinished(this.mObserver, -2003);
        } else {
            this.mListener.onFinished(str);
            this.mFullBackupTask.unregisterTask();
            int i = this.mStatus;
            if (!(i == -1005 || i == 0)) {
                switch (i) {
                    case JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS /*-1002*/:
                        break;
                    case JobSchedulerShellCommand.CMD_ERR_NO_JOB /*-1001*/:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                    default:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                }
            }
            BackupObserverUtils.sendBackupFinished(this.mObserver, 0);
        }
        this.mFinished = true;
        Slog.i(TAG, "K/V backup pass finished.");
        this.backupManagerService.getWakelock().release();
    }

    private void clearMetadata() {
        File pmState = new File(this.mStateDir, BackupManagerService.PACKAGE_MANAGER_SENTINEL);
        if (pmState.exists()) {
            pmState.delete();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x018e  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x018b  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0194  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x019c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int invokeAgentForBackup(String packageName, IBackupAgent agent) {
        Exception e;
        String str;
        BackupManagerService backupManagerService;
        int i;
        Throwable th;
        String str2 = packageName;
        String str3 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invokeAgentForBackup on ");
        stringBuilder.append(str2);
        Slog.d(str3, stringBuilder.toString());
        BackupManagerService backupManagerService2 = this.backupManagerService;
        stringBuilder = new StringBuilder();
        stringBuilder.append("invoking ");
        stringBuilder.append(str2);
        backupManagerService2.addBackupTrace(stringBuilder.toString());
        File blankStateName = new File(this.mStateDir, "blank_state");
        this.mSavedStateName = new File(this.mStateDir, str2);
        File dataDir = this.backupManagerService.getDataDir();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str2);
        stringBuilder2.append(".data");
        this.mBackupDataName = new File(dataDir, stringBuilder2.toString());
        dataDir = this.mStateDir;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str2);
        stringBuilder2.append(".new");
        this.mNewStateName = new File(dataDir, stringBuilder2.toString());
        this.mSavedState = null;
        this.mBackupData = null;
        this.mNewState = null;
        boolean callingAgent = false;
        this.mEphemeralOpToken = this.backupManagerService.generateRandomIntegerToken();
        StringBuilder stringBuilder3;
        try {
            if (str2.equals(BackupManagerService.PACKAGE_MANAGER_SENTINEL)) {
                this.mCurrentPackage = new PackageInfo();
                this.mCurrentPackage.packageName = str2;
            }
            this.mSavedState = ParcelFileDescriptor.open(this.mNonIncremental ? blankStateName : this.mSavedStateName, 402653184);
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
            if (!SELinux.restorecon(this.mBackupDataName)) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("SELinux restorecon failed on ");
                stringBuilder3.append(this.mBackupDataName);
                Slog.e(str3, stringBuilder3.toString());
            }
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
            IBackupTransport transport = this.mTransportClient.connectOrThrow("PBT.invokeAgentForBackup()");
            long quota = transport.getBackupQuota(str2, false);
            try {
                this.backupManagerService.addBackupTrace("setting timeout");
                this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis(), this, 0);
                this.backupManagerService.addBackupTrace("calling agent doBackup()");
                agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, quota, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder(), transport.getTransportFlags());
                if (this.mNonIncremental) {
                    blankStateName.delete();
                }
                this.backupManagerService.addBackupTrace("invoke success");
                return 0;
            } catch (Exception e2) {
                e = e2;
                callingAgent = true;
                try {
                    str = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Error invoking for backup on ");
                    stringBuilder3.append(str2);
                    stringBuilder3.append(". ");
                    stringBuilder3.append(e);
                    Slog.e(str, stringBuilder3.toString());
                    backupManagerService = this.backupManagerService;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("exception: ");
                    stringBuilder3.append(e);
                    backupManagerService.addBackupTrace(stringBuilder3.toString());
                    EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, new Object[]{str2, e.toString()});
                    errorCleanup();
                    if (callingAgent) {
                    }
                    if (this.mNonIncremental) {
                    }
                    return i;
                } catch (Throwable th2) {
                    th = th2;
                    if (this.mNonIncremental) {
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                callingAgent = true;
                if (this.mNonIncremental) {
                    blankStateName.delete();
                }
                throw th;
            }
        } catch (Exception e3) {
            e = e3;
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Error invoking for backup on ");
            stringBuilder3.append(str2);
            stringBuilder3.append(". ");
            stringBuilder3.append(e);
            Slog.e(str, stringBuilder3.toString());
            backupManagerService = this.backupManagerService;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("exception: ");
            stringBuilder3.append(e);
            backupManagerService.addBackupTrace(stringBuilder3.toString());
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, new Object[]{str2, e.toString()});
            errorCleanup();
            if (callingAgent) {
                i = -1003;
            } else {
                i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            if (this.mNonIncremental) {
                blankStateName.delete();
            }
            return i;
        }
    }

    private void failAgent(IBackupAgent agent, String message) {
        try {
            agent.fail(message);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error conveying failure to ");
            stringBuilder.append(this.mCurrentPackage.packageName);
            Slog.w(str, stringBuilder.toString());
        }
    }

    private String SHA1Checksum(byte[] input) {
        try {
            byte[] checksum = MessageDigest.getInstance("SHA-1").digest(input);
            StringBuffer sb = new StringBuffer(checksum.length * 2);
            for (byte toHexString : checksum) {
                sb.append(Integer.toHexString(toHexString));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "Unable to use SHA-1!");
            return "00";
        }
    }

    /* JADX WARNING: Missing block: B:32:0x005e, code skipped:
            $closeResource(r4, r5);
     */
    /* JADX WARNING: Missing block: B:58:0x009b, code skipped:
            $closeResource(r4, r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeWidgetPayloadIfAppropriate(FileDescriptor fd, String pkgName) throws IOException {
        DataInputStream in;
        Throwable th;
        Throwable th2;
        Throwable th3;
        DataOutputStream stateOut;
        Throwable th4;
        byte[] widgetState = AppWidgetBackupBridge.getWidgetState(pkgName, null);
        File file = this.mStateDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkgName);
        stringBuilder.append("_widget");
        File widgetFile = new File(file, stringBuilder.toString());
        boolean priorStateExists = widgetFile.exists();
        if (priorStateExists || widgetState != null) {
            String newChecksum = null;
            if (widgetState != null) {
                newChecksum = SHA1Checksum(widgetState);
                if (priorStateExists) {
                    FileInputStream fin = new FileInputStream(widgetFile);
                    in = new DataInputStream(fin);
                    try {
                        String priorChecksum = in.readUTF();
                        $closeResource(null, in);
                        $closeResource(null, fin);
                        if (Objects.equals(newChecksum, priorChecksum)) {
                            return;
                        }
                    } catch (Throwable th22) {
                        th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                }
            }
            BackupDataOutput out = new BackupDataOutput(fd);
            if (widgetState != null) {
                FileOutputStream fout = new FileOutputStream(widgetFile);
                stateOut = new DataOutputStream(fout);
                try {
                    stateOut.writeUTF(newChecksum);
                    $closeResource(null, stateOut);
                    $closeResource(null, fout);
                    out.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, widgetState.length);
                    out.writeEntityData(widgetState, widgetState.length);
                } catch (Throwable th42) {
                    th3 = th42;
                    th42 = th22;
                    th22 = th3;
                }
            } else {
                out.writeEntityHeader(BackupManagerService.KEY_WIDGET_STATE, -1);
                widgetFile.delete();
            }
            return;
        }
        return;
        $closeResource(th42, stateOut);
        throw th22;
        $closeResource(th22, in);
        throw th;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Missing block: B:33:0x008f, code skipped:
            r7 = r1.mAgentBinder;
            r9 = new java.lang.StringBuilder();
            r9.append("Illegal backup key: ");
            r9.append(r0);
            failAgent(r7, r9.toString());
            r7 = r1.backupManagerService;
            r8 = new java.lang.StringBuilder();
            r8.append("illegal key ");
            r8.append(r0);
            r8.append(" from ");
            r8.append(r3);
            r7.addBackupTrace(r8.toString());
            r8 = new java.lang.Object[r11];
            r8[0] = r3;
            r8[r12] = "bad key";
            android.util.EventLog.writeEvent(com.android.server.EventLogTags.BACKUP_AGENT_FAILURE, r8);
            r1.mMonitor = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r1.mMonitor, 5, r1.mCurrentPackage, 3, com.android.server.backup.utils.BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_ILLEGAL_KEY", r0));
            r1.backupManagerService.getBackupHandler().removeMessages(17);
            com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r1.mObserver, r3, -1003);
            errorCleanup();
     */
    /* JADX WARNING: Missing block: B:34:0x00f7, code skipped:
            if (r14 == null) goto L_0x00fc;
     */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            r14.close();
     */
    /* JADX WARNING: Missing block: B:39:0x00fd, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:43:0x0109, code skipped:
            if (r14 == null) goto L_0x0116;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            r14.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mCancelLock")
    public void operationComplete(long unusedResult) {
        ParcelFileDescriptor backupData;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        synchronized (this.mCancelLock) {
            String pkg;
            if (this.mFinished) {
                Slog.d(TAG, "operationComplete received after task finished.");
            } else if (this.mBackupData == null) {
                pkg = this.mCurrentPackage != null ? this.mCurrentPackage.packageName : "[none]";
                BackupManagerService backupManagerService = this.backupManagerService;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("late opComplete; curPkg = ");
                stringBuilder.append(pkg);
                backupManagerService.addBackupTrace(stringBuilder.toString());
            } else {
                StringBuilder stringBuilder2;
                BackupState nextState;
                String pkgName = this.mCurrentPackage.packageName;
                long filepos = this.mBackupDataName.length();
                FileDescriptor fd = this.mBackupData.getFileDescriptor();
                int i = 2;
                int i2 = 1;
                ParcelFileDescriptor readFd;
                try {
                    if (this.mCurrentPackage.applicationInfo != null && (this.mCurrentPackage.applicationInfo.flags & 1) == 0) {
                        readFd = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                        BackupDataInput in = new BackupDataInput(readFd.getFileDescriptor());
                        while (true) {
                            BackupDataInput in2 = in;
                            if (!in2.readNextHeader()) {
                                break;
                            }
                            pkg = in2.getKey();
                            if (pkg != null && pkg.charAt(0) >= 65280) {
                                break;
                            }
                            in2.skipEntityData();
                            in = in2;
                            i = 2;
                            i2 = 1;
                        }
                    }
                    writeWidgetPayloadIfAppropriate(fd, pkgName);
                } catch (IOException e) {
                    pkg = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unable to save widget state for ");
                    stringBuilder3.append(pkgName);
                    Slog.w(pkg, stringBuilder3.toString());
                    try {
                        Os.ftruncate(fd, filepos);
                    } catch (ErrnoException ee) {
                        ErrnoException errnoException = ee;
                        Slog.w(TAG, "Unable to roll back!");
                    } catch (Throwable th) {
                        backupData = backupData;
                        Throwable backupData2 = th;
                        if (backupData != null) {
                            try {
                                backupData.close();
                            } catch (IOException e2) {
                            }
                        }
                    }
                } catch (Throwable th2) {
                    if (readFd != null) {
                        readFd.close();
                    }
                }
                this.backupManagerService.getBackupHandler().removeMessages(17);
                clearAgentState();
                this.backupManagerService.addBackupTrace("operation complete");
                IBackupTransport transport = this.mTransportClient.connect("PBT.operationComplete()");
                ParcelFileDescriptor backupData3 = null;
                this.mStatus = 0;
                long size = 0;
                StringBuilder stringBuilder4;
                try {
                    TransportUtils.checkTransportNotNull(transport);
                    size = this.mBackupDataName.length();
                    if (size > 0) {
                        boolean isNonIncremental = this.mSavedStateName.length() == 0;
                        if (this.mStatus == 0) {
                            backupData3 = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                            this.backupManagerService.addBackupTrace("sending data to transport");
                            i = this.mUserInitiated;
                            if (isNonIncremental) {
                                i2 = 4;
                            } else {
                                i2 = 2;
                            }
                            this.mStatus = transport.performBackup(this.mCurrentPackage, backupData3, i | i2);
                        }
                        if (isNonIncremental && this.mStatus == -1006) {
                            Slog.w(TAG, "Transport requested non-incremental but already the case, error");
                            this.backupManagerService.addBackupTrace("Transport requested non-incremental but already the case, error");
                            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        }
                        BackupManagerService backupManagerService2 = this.backupManagerService;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("data delivered: ");
                        stringBuilder4.append(this.mStatus);
                        backupManagerService2.addBackupTrace(stringBuilder4.toString());
                        if (this.mStatus == 0) {
                            this.backupManagerService.addBackupTrace("finishing op on transport");
                            this.mStatus = transport.finishBackup();
                            backupManagerService2 = this.backupManagerService;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("finished: ");
                            stringBuilder4.append(this.mStatus);
                            backupManagerService2.addBackupTrace(stringBuilder4.toString());
                        } else if (this.mStatus == JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS) {
                            this.backupManagerService.addBackupTrace("transport rejected package");
                        }
                    } else {
                        this.backupManagerService.addBackupTrace("no data to send");
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 7, this.mCurrentPackage, 3, null);
                    }
                    if (this.mStatus == 0) {
                        this.mBackupDataName.delete();
                        this.mNewStateName.renameTo(this.mSavedStateName);
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, 0);
                        EventLog.writeEvent(EventLogTags.BACKUP_PACKAGE, new Object[]{pkgName, Long.valueOf(size)});
                        this.backupManagerService.logBackupComplete(pkgName);
                    } else if (this.mStatus == JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS) {
                        this.mBackupDataName.delete();
                        this.mNewStateName.delete();
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                        EventLogTags.writeBackupAgentFailure(pkgName, "Transport rejected");
                    } else if (this.mStatus == -1005) {
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, -1005);
                        EventLog.writeEvent(EventLogTags.BACKUP_QUOTA_EXCEEDED, pkgName);
                    } else if (this.mStatus == -1006) {
                        Slog.i(TAG, "Transport lost data, retrying package");
                        BackupManagerService backupManagerService3 = this.backupManagerService;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Transport lost data, retrying package:");
                        stringBuilder2.append(pkgName);
                        backupManagerService3.addBackupTrace(stringBuilder2.toString());
                        BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 51, this.mCurrentPackage, 1, null);
                        this.mBackupDataName.delete();
                        this.mSavedStateName.delete();
                        this.mNewStateName.delete();
                        if (!BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(pkgName)) {
                            this.mQueue.add(0, new BackupRequest(pkgName));
                        }
                    } else {
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                    }
                    if (backupData3 != null) {
                        try {
                            backupData3.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (Exception e4) {
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    String str = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Transport error backing up ");
                    stringBuilder4.append(pkgName);
                    Slog.e(str, stringBuilder4.toString(), e4);
                    EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    if (backupData3 != null) {
                        backupData3.close();
                    }
                }
                if (this.mStatus != 0) {
                    if (this.mStatus != JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS) {
                        if (this.mStatus == -1006) {
                            nextState = BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(pkgName) ? BackupState.BACKUP_PM : BackupState.RUNNING_QUEUE;
                        } else if (this.mStatus == -1005) {
                            if (this.mAgentBinder != null) {
                                try {
                                    TransportUtils.checkTransportNotNull(transport);
                                    this.mAgentBinder.doQuotaExceeded(size, transport.getBackupQuota(this.mCurrentPackage.packageName, 0));
                                } catch (Exception e42) {
                                    backupData = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Unable to notify about quota exceeded: ");
                                    stringBuilder2.append(e42.getMessage());
                                    Slog.e(backupData, stringBuilder2.toString());
                                }
                            }
                            nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
                        } else {
                            revertAndEndBackup();
                            nextState = BackupState.FINAL;
                        }
                        executeNextState(nextState);
                    }
                }
                nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
                executeNextState(nextState);
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0091, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mCancelLock")
    public void handleCancel(boolean cancelAll) {
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        synchronized (this.mCancelLock) {
            if (this.mFinished) {
                return;
            }
            String logPackageName;
            this.mCancelAll = cancelAll;
            if (this.mCurrentPackage != null) {
                logPackageName = this.mCurrentPackage.packageName;
            } else {
                logPackageName = "no_package_yet";
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cancel backing up ");
            stringBuilder.append(logPackageName);
            Slog.i(str, stringBuilder.toString());
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, logPackageName);
            BackupManagerService backupManagerService = this.backupManagerService;
            stringBuilder = new StringBuilder();
            stringBuilder.append("cancel of ");
            stringBuilder.append(logPackageName);
            stringBuilder.append(", cancelAll=");
            stringBuilder.append(cancelAll);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 21, this.mCurrentPackage, 2, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_CANCEL_ALL", this.mCancelAll));
            errorCleanup();
            if (cancelAll) {
                finalizeBackup();
            } else {
                executeNextState(this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE);
                this.backupManagerService.dataChangedImpl(this.mCurrentPackage.packageName);
            }
        }
    }

    private void revertAndEndBackup() {
        long delay;
        this.backupManagerService.addBackupTrace("transport error; reverting");
        try {
            delay = this.mTransportClient.connectOrThrow("PBT.revertAndEndBackup()").requestBackupTime();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to contact transport for recommended backoff: ");
            stringBuilder.append(e.getMessage());
            Slog.w(str, stringBuilder.toString());
            delay = 0;
        }
        KeyValueBackupJob.schedule(this.backupManagerService.getContext(), delay, this.backupManagerService.getConstants());
        Iterator it = this.mOriginalQueue.iterator();
        while (it.hasNext()) {
            this.backupManagerService.dataChangedImpl(((BackupRequest) it.next()).packageName);
        }
    }

    private void errorCleanup() {
        this.mBackupDataName.delete();
        this.mNewStateName.delete();
        clearAgentState();
    }

    private void clearAgentState() {
        try {
            if (this.mSavedState != null) {
                this.mSavedState.close();
            }
        } catch (IOException e) {
        }
        try {
            if (this.mBackupData != null) {
                this.mBackupData.close();
            }
        } catch (IOException e2) {
        }
        try {
            if (this.mNewState != null) {
                this.mNewState.close();
            }
        } catch (IOException e3) {
        }
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            this.backupManagerService.getCurrentOperations().remove(this.mEphemeralOpToken);
            this.mNewState = null;
            this.mBackupData = null;
            this.mSavedState = null;
        }
        if (this.mCurrentPackage.applicationInfo != null) {
            BackupManagerService backupManagerService = this.backupManagerService;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unbinding ");
            stringBuilder.append(this.mCurrentPackage.packageName);
            backupManagerService.addBackupTrace(stringBuilder.toString());
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
            } catch (RemoteException e4) {
            }
        }
    }

    private void executeNextState(BackupState nextState) {
        BackupManagerService backupManagerService = this.backupManagerService;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("executeNextState => ");
        stringBuilder.append(nextState);
        backupManagerService.addBackupTrace(stringBuilder.toString());
        this.mCurrentState = nextState;
        this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this));
    }
}
