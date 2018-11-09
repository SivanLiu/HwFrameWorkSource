package com.android.server.backup.internal;

import android.app.IBackupAgent;
import android.app.IBackupAgent.Stub;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.content.pm.PackageInfo;
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
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.KeyValueBackupJob;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class PerformBackupTask implements BackupRestoreTask {
    private static final /* synthetic */ int[] -com-android-server-backup-internal-BackupStateSwitchesValues = null;
    private static final String TAG = "PerformBackupTask";
    private RefactoredBackupManagerService backupManagerService;
    IBackupAgent mAgentBinder;
    ParcelFileDescriptor mBackupData;
    File mBackupDataName;
    private volatile boolean mCancelAll;
    private final Object mCancelLock = new Object();
    private final int mCurrentOpToken;
    PackageInfo mCurrentPackage;
    BackupState mCurrentState;
    private volatile int mEphemeralOpToken;
    boolean mFinished;
    private final PerformFullTransportBackupTask mFullBackupTask;
    DataChangedJournal mJournal;
    IBackupManagerMonitor mMonitor;
    ParcelFileDescriptor mNewState;
    File mNewStateName;
    final boolean mNonIncremental;
    IBackupObserver mObserver;
    ArrayList<BackupRequest> mOriginalQueue;
    List<String> mPendingFullBackups;
    ArrayList<BackupRequest> mQueue;
    ParcelFileDescriptor mSavedState;
    File mSavedStateName;
    File mStateDir;
    int mStatus;
    IBackupTransport mTransport;
    final boolean mUserInitiated;

    private static /* synthetic */ int[] -getcom-android-server-backup-internal-BackupStateSwitchesValues() {
        if (-com-android-server-backup-internal-BackupStateSwitchesValues != null) {
            return -com-android-server-backup-internal-BackupStateSwitchesValues;
        }
        int[] iArr = new int[BackupState.values().length];
        try {
            iArr[BackupState.FINAL.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[BackupState.INITIAL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[BackupState.RUNNING_QUEUE.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        -com-android-server-backup-internal-BackupStateSwitchesValues = iArr;
        return iArr;
    }

    public PerformBackupTask(RefactoredBackupManagerService backupManagerService, IBackupTransport transport, String dirName, ArrayList<BackupRequest> queue, DataChangedJournal journal, IBackupObserver observer, IBackupManagerMonitor monitor, List<String> pendingFullBackups, boolean userInitiated, boolean nonIncremental) {
        this.backupManagerService = backupManagerService;
        this.mTransport = transport;
        this.mOriginalQueue = queue;
        this.mQueue = new ArrayList();
        this.mJournal = journal;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mPendingFullBackups = pendingFullBackups;
        this.mUserInitiated = userInitiated;
        this.mNonIncremental = nonIncremental;
        this.mStateDir = new File(backupManagerService.getBaseStateDir(), dirName);
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mFinished = false;
        synchronized (backupManagerService.getCurrentOpLock()) {
            if (backupManagerService.isBackupOperationInProgress()) {
                Slog.d(TAG, "Skipping backup since one is already in progress.");
                this.mCancelAll = true;
                this.mFullBackupTask = null;
                this.mCurrentState = BackupState.FINAL;
                backupManagerService.addBackupTrace("Skipped. Backup already in progress.");
            } else {
                this.mCurrentState = BackupState.INITIAL;
                RefactoredBackupManagerService refactoredBackupManagerService = backupManagerService;
                this.mFullBackupTask = new PerformFullTransportBackupTask(refactoredBackupManagerService, null, (String[]) this.mPendingFullBackups.toArray(new String[this.mPendingFullBackups.size()]), false, null, new CountDownLatch(1), this.mObserver, this.mMonitor, this.mUserInitiated);
                registerTask();
                backupManagerService.addBackupTrace("STATE => INITIAL");
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
            switch (-getcom-android-server-backup-internal-BackupStateSwitchesValues()[this.mCurrentState.ordinal()]) {
                case 1:
                    if (this.mFinished) {
                        Slog.e(TAG, "Duplicate finish");
                    } else {
                        finalizeBackup();
                    }
                    this.mFinished = true;
                    break;
                case 2:
                    beginBackup();
                    break;
                case 3:
                    invokeNextAgent();
                    break;
            }
        }
    }

    void beginBackup() {
        this.backupManagerService.clearBackupTrace();
        StringBuilder b = new StringBuilder(256);
        b.append("beginBackup: [");
        for (BackupRequest req : this.mOriginalQueue) {
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
            if (RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(((BackupRequest) this.mQueue.get(i)).packageName)) {
                this.mQueue.remove(i);
                skipPm = false;
                break;
            }
        }
        Slog.v(TAG, "Beginning backup of " + this.mQueue.size() + " targets");
        File pmState = new File(this.mStateDir, RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL);
        try {
            String transportName = this.mTransport.transportDirName();
            EventLog.writeEvent(EventLogTags.BACKUP_START, transportName);
            if (this.mStatus == 0 && pmState.length() <= 0) {
                Slog.i(TAG, "Initializing (wiping) backup state and transport storage");
                this.backupManagerService.addBackupTrace("initializing transport " + transportName);
                this.backupManagerService.resetBackupState(this.mStateDir);
                this.mStatus = this.mTransport.initializeDevice();
                this.backupManagerService.addBackupTrace("transport.initializeDevice() == " + this.mStatus);
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
                this.mStatus = invokeAgentForBackup(RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL, Stub.asInterface(new PackageManagerBackupAgent(this.backupManagerService.getPackageManager()).onBind()), this.mTransport);
                this.backupManagerService.addBackupTrace("PMBA invoke: " + this.mStatus);
                this.backupManagerService.getBackupHandler().removeMessages(17);
            }
            if (this.mStatus == JobSchedulerShellCommand.CMD_ERR_NO_JOB) {
                EventLog.writeEvent(EventLogTags.BACKUP_RESET, this.mTransport.transportDirName());
            }
            this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                executeNextState(BackupState.FINAL);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error in backup thread", e);
            this.backupManagerService.addBackupTrace("Exception in backup thread: " + e);
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                executeNextState(BackupState.FINAL);
            }
        } catch (Throwable th) {
            this.backupManagerService.addBackupTrace("exiting prelim: " + this.mStatus);
            if (this.mStatus != 0) {
                this.backupManagerService.resetBackupState(this.mStateDir);
                BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                executeNextState(BackupState.FINAL);
            }
        }
    }

    void invokeNextAgent() {
        this.mStatus = 0;
        this.backupManagerService.addBackupTrace("invoke q=" + this.mQueue.size());
        if (this.mQueue.isEmpty()) {
            executeNextState(BackupState.FINAL);
            return;
        }
        BackupRequest request = (BackupRequest) this.mQueue.get(0);
        this.mQueue.remove(0);
        Slog.d(TAG, "starting key/value backup of " + request);
        this.backupManagerService.addBackupTrace("launch agent for " + request.packageName);
        BackupState nextState;
        try {
            this.mCurrentPackage = this.backupManagerService.getPackageManager().getPackageInfo(request.packageName, 64);
            if (!AppBackupUtils.appIsEligibleForBackup(this.mCurrentPackage.applicationInfo)) {
                Slog.i(TAG, "Package " + request.packageName + " no longer supports backup; skipping");
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
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            } else if (AppBackupUtils.appGetsFullBackup(this.mCurrentPackage)) {
                Slog.i(TAG, "Package " + request.packageName + " requests full-data rather than key/value; skipping");
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
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
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
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            } else {
                try {
                    boolean z;
                    this.backupManagerService.getWakelock().setWorkSource(new WorkSource(this.mCurrentPackage.applicationInfo.uid));
                    IBackupAgent agent = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
                    RefactoredBackupManagerService refactoredBackupManagerService = this.backupManagerService;
                    StringBuilder append = new StringBuilder().append("agent bound; a? = ");
                    if (agent != null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    refactoredBackupManagerService.addBackupTrace(append.append(z).toString());
                    if (agent != null) {
                        this.mAgentBinder = agent;
                        this.mStatus = invokeAgentForBackup(request.packageName, agent, this.mTransport);
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
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                    } else {
                        revertAndEndBackup();
                        nextState = BackupState.FINAL;
                    }
                    executeNextState(nextState);
                } else {
                    this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
                }
            }
        } catch (NameNotFoundException e) {
            Slog.d(TAG, "Package does not exist; skipping");
            this.backupManagerService.addBackupTrace("no such package");
            this.mStatus = -1004;
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
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    nextState = BackupState.FINAL;
                }
                executeNextState(nextState);
            } else {
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
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
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, this.mCurrentPackage.packageName, -2002);
                } else {
                    revertAndEndBackup();
                    nextState = BackupState.FINAL;
                }
                executeNextState(nextState);
            } else {
                this.backupManagerService.addBackupTrace("expecting completion/timeout callback");
            }
        }
    }

    void finalizeBackup() {
        this.backupManagerService.addBackupTrace("finishing");
        for (BackupRequest req : this.mQueue) {
            this.backupManagerService.dataChangedImpl(req.packageName);
        }
        if (!(this.mJournal == null || (this.mJournal.delete() ^ 1) == 0)) {
            Slog.e(TAG, "Unable to remove backup journal file " + this.mJournal);
        }
        if (this.backupManagerService.getCurrentToken() == 0 && this.mStatus == 0) {
            this.backupManagerService.addBackupTrace("success; recording token");
            try {
                this.backupManagerService.setCurrentToken(this.mTransport.getCurrentRestoreSet());
                this.backupManagerService.writeRestoreTokens();
            } catch (Exception e) {
                Slog.e(TAG, "Transport threw reporting restore set: " + e.getMessage());
                this.backupManagerService.addBackupTrace("transport threw returning token");
            }
        }
        synchronized (this.backupManagerService.getQueueLock()) {
            this.backupManagerService.setBackupRunning(false);
            if (this.mStatus == JobSchedulerShellCommand.CMD_ERR_NO_JOB) {
                this.backupManagerService.addBackupTrace("init required; rerunning");
                try {
                    String name = this.backupManagerService.getTransportManager().getTransportName(this.mTransport);
                    if (name != null) {
                        this.backupManagerService.getPendingInits().add(name);
                    } else {
                        Slog.w(TAG, "Couldn't find name of transport " + this.mTransport + " for init");
                    }
                } catch (Exception e2) {
                    Slog.w(TAG, "Failed to query transport name for init: " + e2.getMessage());
                }
                clearMetadata();
                this.backupManagerService.backupNow();
            }
        }
        this.backupManagerService.clearBackupTrace();
        unregisterTask();
        if (this.mCancelAll || this.mStatus != 0 || this.mPendingFullBackups == null || (this.mPendingFullBackups.isEmpty() ^ 1) == 0) {
            if (!this.mCancelAll) {
                this.mFullBackupTask.unregisterTask();
                switch (this.mStatus) {
                    case JobSchedulerShellCommand.CMD_ERR_NO_JOB /*-1001*/:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                    case 0:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, 0);
                        break;
                    default:
                        BackupObserverUtils.sendBackupFinished(this.mObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        break;
                }
            }
            if (this.mFullBackupTask != null) {
                this.mFullBackupTask.unregisterTask();
            }
            BackupObserverUtils.sendBackupFinished(this.mObserver, -2003);
        } else {
            Slog.d(TAG, "Starting full backups for: " + this.mPendingFullBackups);
            this.backupManagerService.getWakelock().acquire();
            new Thread(this.mFullBackupTask, "full-transport-requested").start();
        }
        Slog.i(TAG, "K/V backup pass finished.");
        this.backupManagerService.getWakelock().release();
    }

    void clearMetadata() {
        File pmState = new File(this.mStateDir, RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL);
        if (pmState.exists()) {
            pmState.delete();
        }
    }

    int invokeAgentForBackup(String packageName, IBackupAgent agent, IBackupTransport transport) {
        Slog.d(TAG, "invokeAgentForBackup on " + packageName);
        this.backupManagerService.addBackupTrace("invoking " + packageName);
        File blankStateName = new File(this.mStateDir, "blank_state");
        this.mSavedStateName = new File(this.mStateDir, packageName);
        this.mBackupDataName = new File(this.backupManagerService.getDataDir(), packageName + ".data");
        this.mNewStateName = new File(this.mStateDir, packageName + ".new");
        this.mSavedState = null;
        this.mBackupData = null;
        this.mNewState = null;
        this.mEphemeralOpToken = this.backupManagerService.generateRandomIntegerToken();
        try {
            if (packageName.equals(RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL)) {
                this.mCurrentPackage = new PackageInfo();
                this.mCurrentPackage.packageName = packageName;
            }
            this.mSavedState = ParcelFileDescriptor.open(this.mNonIncremental ? blankStateName : this.mSavedStateName, 402653184);
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
            if (!SELinux.restorecon(this.mBackupDataName)) {
                Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
            }
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
            long quota = this.mTransport.getBackupQuota(packageName, false);
            this.backupManagerService.addBackupTrace("setting timeout");
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, 30000, this, 0);
            this.backupManagerService.addBackupTrace("calling agent doBackup()");
            agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, quota, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
            if (this.mNonIncremental) {
                blankStateName.delete();
            }
            this.backupManagerService.addBackupTrace("invoke success");
            return 0;
        } catch (Exception e) {
            int i;
            Slog.e(TAG, "Error invoking for backup on " + packageName + ". " + e);
            this.backupManagerService.addBackupTrace("exception: " + e);
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, new Object[]{packageName, e.toString()});
            errorCleanup();
            if (false) {
                i = -1003;
            } else {
                i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            if (this.mNonIncremental) {
                blankStateName.delete();
            }
            return i;
        } catch (Throwable th) {
            if (this.mNonIncremental) {
                blankStateName.delete();
            }
        }
    }

    public void failAgent(IBackupAgent agent, String message) {
        try {
            agent.fail(message);
        } catch (Exception e) {
            Slog.w(TAG, "Error conveying failure to " + this.mCurrentPackage.packageName);
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

    private void writeWidgetPayloadIfAppropriate(FileDescriptor fd, String pkgName) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3;
        byte[] widgetState = AppWidgetBackupBridge.getWidgetState(pkgName, 0);
        File widgetFile = new File(this.mStateDir, pkgName + "_widget");
        boolean priorStateExists = widgetFile.exists();
        if (priorStateExists || widgetState != null) {
            Throwable th4;
            String str = null;
            if (widgetState != null) {
                str = SHA1Checksum(widgetState);
                if (priorStateExists) {
                    th4 = null;
                    FileInputStream fileInputStream = null;
                    DataInputStream dataInputStream = null;
                    try {
                        FileInputStream fin = new FileInputStream(widgetFile);
                        try {
                            DataInputStream in = new DataInputStream(fin);
                            try {
                                String priorChecksum = in.readUTF();
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (Throwable th5) {
                                        th4 = th5;
                                    }
                                }
                                if (fin != null) {
                                    try {
                                        fin.close();
                                    } catch (Throwable th6) {
                                        th = th6;
                                        if (th4 != null) {
                                            if (th4 != th) {
                                                th4.addSuppressed(th);
                                                th = th4;
                                            }
                                        }
                                    }
                                }
                                th = th4;
                                if (th != null) {
                                    throw th;
                                } else if (Objects.equals(str, priorChecksum)) {
                                    return;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                dataInputStream = in;
                                fileInputStream = fin;
                                if (dataInputStream != null) {
                                    try {
                                        dataInputStream.close();
                                    } catch (Throwable th8) {
                                        th3 = th8;
                                        if (th4 != null) {
                                            if (th4 != th3) {
                                                th4.addSuppressed(th3);
                                                th3 = th4;
                                            }
                                        }
                                    }
                                }
                                th3 = th4;
                                if (fileInputStream != null) {
                                    try {
                                        fileInputStream.close();
                                    } catch (Throwable th9) {
                                        th4 = th9;
                                        if (th3 != null) {
                                            if (th3 != th4) {
                                                th3.addSuppressed(th4);
                                                th4 = th3;
                                            }
                                        }
                                    }
                                }
                                th4 = th3;
                                if (th4 != null) {
                                    throw th4;
                                }
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            fileInputStream = fin;
                            if (dataInputStream != null) {
                                dataInputStream.close();
                            }
                            th3 = th4;
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                            th4 = th3;
                            if (th4 != null) {
                                throw th;
                            }
                            throw th4;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        if (dataInputStream != null) {
                            dataInputStream.close();
                        }
                        th3 = th4;
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        th4 = th3;
                        if (th4 != null) {
                            throw th4;
                        }
                        throw th;
                    }
                }
            }
            BackupDataOutput out = new BackupDataOutput(fd);
            if (widgetState != null) {
                th4 = null;
                FileOutputStream fileOutputStream = null;
                DataOutputStream dataOutputStream = null;
                try {
                    FileOutputStream fout = new FileOutputStream(widgetFile);
                    try {
                        DataOutputStream stateOut = new DataOutputStream(fout);
                        try {
                            stateOut.writeUTF(str);
                            if (stateOut != null) {
                                try {
                                    stateOut.close();
                                } catch (Throwable th12) {
                                    th4 = th12;
                                }
                            }
                            if (fout != null) {
                                try {
                                    fout.close();
                                } catch (Throwable th13) {
                                    th = th13;
                                    if (th4 != null) {
                                        if (th4 != th) {
                                            th4.addSuppressed(th);
                                            th = th4;
                                        }
                                    }
                                }
                            }
                            th = th4;
                            if (th != null) {
                                throw th;
                            }
                            out.writeEntityHeader(RefactoredBackupManagerService.KEY_WIDGET_STATE, widgetState.length);
                            out.writeEntityData(widgetState, widgetState.length);
                        } catch (Throwable th14) {
                            th = th14;
                            dataOutputStream = stateOut;
                            fileOutputStream = fout;
                            if (dataOutputStream != null) {
                                try {
                                    dataOutputStream.close();
                                } catch (Throwable th15) {
                                    th3 = th15;
                                    if (th4 != null) {
                                        if (th4 != th3) {
                                            th4.addSuppressed(th3);
                                            th3 = th4;
                                        }
                                    }
                                }
                            }
                            th3 = th4;
                            if (fileOutputStream != null) {
                                try {
                                    fileOutputStream.close();
                                } catch (Throwable th16) {
                                    th4 = th16;
                                    if (th3 != null) {
                                        if (th3 != th4) {
                                            th3.addSuppressed(th4);
                                            th4 = th3;
                                        }
                                    }
                                }
                            }
                            th4 = th3;
                            if (th4 != null) {
                                throw th4;
                            }
                            throw th;
                        }
                    } catch (Throwable th17) {
                        th = th17;
                        fileOutputStream = fout;
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
                        th3 = th4;
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        th4 = th3;
                        if (th4 != null) {
                            throw th;
                        }
                        throw th4;
                    }
                } catch (Throwable th18) {
                    th = th18;
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    th3 = th4;
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    th4 = th3;
                    if (th4 != null) {
                        throw th4;
                    }
                    throw th;
                }
            }
            out.writeEntityHeader(RefactoredBackupManagerService.KEY_WIDGET_STATE, -1);
            widgetFile.delete();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mCancelLock")
    public void operationComplete(long unusedResult) {
        ParcelFileDescriptor readFd;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        synchronized (this.mCancelLock) {
            if (this.mFinished) {
                Slog.d(TAG, "operationComplete received after task finished.");
                return;
            } else if (this.mBackupData == null) {
                this.backupManagerService.addBackupTrace("late opComplete; curPkg = " + (this.mCurrentPackage != null ? this.mCurrentPackage.packageName : "[none]"));
                return;
            } else {
                BackupState nextState;
                String pkgName = this.mCurrentPackage.packageName;
                long filepos = this.mBackupDataName.length();
                FileDescriptor fd = this.mBackupData.getFileDescriptor();
                try {
                    if (this.mCurrentPackage.applicationInfo != null && (this.mCurrentPackage.applicationInfo.flags & 1) == 0) {
                        readFd = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                        BackupDataInput in = new BackupDataInput(readFd.getFileDescriptor());
                        while (in.readNextHeader()) {
                            String key = in.getKey();
                            if (key == null || key.charAt(0) < '＀') {
                                in.skipEntityData();
                            } else {
                                failAgent(this.mAgentBinder, "Illegal backup key: " + key);
                                this.backupManagerService.addBackupTrace("illegal key " + key + " from " + pkgName);
                                EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, new Object[]{pkgName, "bad key"});
                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 5, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_ILLEGAL_KEY", key));
                                this.backupManagerService.getBackupHandler().removeMessages(17);
                                BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, -1003);
                                errorCleanup();
                                if (readFd != null) {
                                    readFd.close();
                                }
                            }
                        }
                        if (readFd != null) {
                            readFd.close();
                        }
                    }
                    writeWidgetPayloadIfAppropriate(fd, pkgName);
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to save widget state for " + pkgName);
                    try {
                        Os.ftruncate(fd, filepos);
                    } catch (ErrnoException e2) {
                        Slog.w(TAG, "Unable to roll back!");
                    }
                } catch (Throwable th) {
                    if (readFd != null) {
                        readFd.close();
                    }
                }
                this.backupManagerService.getBackupHandler().removeMessages(17);
                clearAgentState();
                this.backupManagerService.addBackupTrace("operation complete");
                ParcelFileDescriptor parcelFileDescriptor = null;
                this.mStatus = 0;
                long j = 0;
                try {
                    j = this.mBackupDataName.length();
                    if (j > 0) {
                        if (this.mStatus == 0) {
                            parcelFileDescriptor = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
                            this.backupManagerService.addBackupTrace("sending data to transport");
                            this.mStatus = this.mTransport.performBackup(this.mCurrentPackage, parcelFileDescriptor, this.mUserInitiated ? 1 : 0);
                        }
                        this.backupManagerService.addBackupTrace("data delivered: " + this.mStatus);
                        if (this.mStatus == 0) {
                            this.backupManagerService.addBackupTrace("finishing op on transport");
                            this.mStatus = this.mTransport.finishBackup();
                            this.backupManagerService.addBackupTrace("finished: " + this.mStatus);
                        } else if (this.mStatus == -1002) {
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
                        EventLog.writeEvent(EventLogTags.BACKUP_PACKAGE, new Object[]{pkgName, Long.valueOf(j)});
                        this.backupManagerService.logBackupComplete(pkgName);
                    } else if (this.mStatus == -1002) {
                        this.mBackupDataName.delete();
                        this.mNewStateName.delete();
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                        EventLogTags.writeBackupAgentFailure(pkgName, "Transport rejected");
                    } else if (this.mStatus == -1005) {
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, -1005);
                        EventLog.writeEvent(EventLogTags.BACKUP_QUOTA_EXCEEDED, pkgName);
                    } else {
                        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                        EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                    }
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (Exception e4) {
                    BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, pkgName, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                    Slog.e(TAG, "Transport error backing up " + pkgName, e4);
                    EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_FAILURE, pkgName);
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e5) {
                        }
                    }
                } catch (Throwable th2) {
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e6) {
                        }
                    }
                }
                if (this.mStatus == 0 || this.mStatus == -1002) {
                    nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
                } else if (this.mStatus == -1005) {
                    if (this.mAgentBinder != null) {
                        try {
                            this.mAgentBinder.doQuotaExceeded(j, this.mTransport.getBackupQuota(this.mCurrentPackage.packageName, false));
                        } catch (Exception e42) {
                            Slog.e(TAG, "Unable to notify about quota exceeded: " + e42.getMessage());
                        }
                    }
                    nextState = this.mQueue.isEmpty() ? BackupState.FINAL : BackupState.RUNNING_QUEUE;
                } else {
                    revertAndEndBackup();
                    nextState = BackupState.FINAL;
                }
                executeNextState(nextState);
                return;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
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
            Slog.i(TAG, "Cancel backing up " + logPackageName);
            EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, logPackageName);
            this.backupManagerService.addBackupTrace("cancel of " + logPackageName + ", cancelAll=" + cancelAll);
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

    void revertAndEndBackup() {
        long delay;
        this.backupManagerService.addBackupTrace("transport error; reverting");
        try {
            delay = this.mTransport.requestBackupTime();
        } catch (Exception e) {
            Slog.w(TAG, "Unable to contact transport for recommended backoff: " + e.getMessage());
            delay = 0;
        }
        KeyValueBackupJob.schedule(this.backupManagerService.getContext(), delay);
        for (BackupRequest request : this.mOriginalQueue) {
            this.backupManagerService.dataChangedImpl(request.packageName);
        }
    }

    void errorCleanup() {
        this.mBackupDataName.delete();
        this.mNewStateName.delete();
        clearAgentState();
    }

    void clearAgentState() {
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
            this.backupManagerService.addBackupTrace("unbinding " + this.mCurrentPackage.packageName);
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
            } catch (RemoteException e4) {
            }
        }
    }

    void executeNextState(BackupState nextState) {
        this.backupManagerService.addBackupTrace("executeNextState => " + nextState);
        this.mCurrentState = nextState;
        this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this));
    }
}
