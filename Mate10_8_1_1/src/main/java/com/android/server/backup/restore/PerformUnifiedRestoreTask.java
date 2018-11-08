package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.IBackupAgent.Stub;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.RestoreDescription;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.BackupUtils;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.PackageManagerBackupAgent.Metadata;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class PerformUnifiedRestoreTask implements BackupRestoreTask {
    private static final /* synthetic */ int[] -com-android-server-backup-restore-UnifiedRestoreStateSwitchesValues = null;
    private RefactoredBackupManagerService backupManagerService;
    private List<PackageInfo> mAcceptSet;
    private IBackupAgent mAgent;
    ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private int mCount;
    private PackageInfo mCurrentPackage;
    private boolean mDidLaunch;
    private final int mEphemeralOpToken;
    private boolean mFinished;
    private boolean mIsSystemRestore;
    private IBackupManagerMonitor mMonitor;
    ParcelFileDescriptor mNewState;
    private File mNewStateName;
    private IRestoreObserver mObserver;
    private PackageManagerBackupAgent mPmAgent;
    private int mPmToken;
    private RestoreDescription mRestoreDescription;
    private File mSavedStateName;
    private File mStageName;
    private long mStartRealtime = SystemClock.elapsedRealtime();
    private UnifiedRestoreState mState = UnifiedRestoreState.INITIAL;
    File mStateDir;
    private int mStatus;
    private PackageInfo mTargetPackage;
    private long mToken;
    private IBackupTransport mTransport;
    private byte[] mWidgetData;

    class EngineThread implements Runnable {
        FullRestoreEngine mEngine;
        FileInputStream mEngineStream;

        EngineThread(FullRestoreEngine engine, ParcelFileDescriptor engineSocket) {
            this.mEngine = engine;
            engine.setRunning(true);
            this.mEngineStream = new FileInputStream(engineSocket.getFileDescriptor(), true);
        }

        public boolean isRunning() {
            return this.mEngine.isRunning();
        }

        public int waitForResult() {
            return this.mEngine.waitForResult();
        }

        public void run() {
            while (this.mEngine.isRunning()) {
                try {
                    this.mEngine.restoreOneFile(this.mEngineStream, false, this.mEngine.mBuffer, this.mEngine.mOnlyPackage, this.mEngine.mAllowApks, this.mEngine.mEphemeralOpToken, this.mEngine.mMonitor);
                } finally {
                    IoUtils.closeQuietly(this.mEngineStream);
                }
            }
        }

        public void handleTimeout() {
            IoUtils.closeQuietly(this.mEngineStream);
            this.mEngine.handleTimeout();
        }
    }

    class StreamFeederThread extends RestoreEngine implements Runnable, BackupRestoreTask {
        final String TAG = "StreamFeederThread";
        FullRestoreEngine mEngine;
        ParcelFileDescriptor[] mEnginePipes;
        EngineThread mEngineThread;
        private final int mEphemeralOpToken;
        ParcelFileDescriptor[] mTransportPipes;

        public StreamFeederThread() throws IOException {
            this.mEphemeralOpToken = PerformUnifiedRestoreTask.this.backupManagerService.generateRandomIntegerToken();
            this.mTransportPipes = ParcelFileDescriptor.createPipe();
            this.mEnginePipes = ParcelFileDescriptor.createPipe();
            setRunning(true);
        }

        public void run() {
            PerformUnifiedRestoreTask performUnifiedRestoreTask;
            boolean z;
            UnifiedRestoreState nextState = UnifiedRestoreState.RUNNING_QUEUE;
            int status = 0;
            EventLog.writeEvent(EventLogTags.FULL_RESTORE_PACKAGE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            this.mEngine = new FullRestoreEngine(PerformUnifiedRestoreTask.this.backupManagerService, this, null, PerformUnifiedRestoreTask.this.mMonitor, PerformUnifiedRestoreTask.this.mCurrentPackage, false, false, this.mEphemeralOpToken);
            this.mEngineThread = new EngineThread(this.mEngine, this.mEnginePipes[0]);
            ParcelFileDescriptor eWriteEnd = this.mEnginePipes[1];
            ParcelFileDescriptor tReadEnd = this.mTransportPipes[0];
            ParcelFileDescriptor tWriteEnd = this.mTransportPipes[1];
            int bufferSize = 32768;
            byte[] buffer = new byte[32768];
            FileOutputStream fileOutputStream = new FileOutputStream(eWriteEnd.getFileDescriptor());
            FileInputStream fileInputStream = new FileInputStream(tReadEnd.getFileDescriptor());
            new Thread(this.mEngineThread, "unified-restore-engine").start();
            while (status == 0) {
                try {
                    int result = PerformUnifiedRestoreTask.this.mTransport.getNextFullRestoreDataChunk(tWriteEnd);
                    if (result > 0) {
                        if (result > bufferSize) {
                            bufferSize = result;
                            buffer = new byte[result];
                        }
                        int toCopy = result;
                        while (toCopy > 0) {
                            int n = fileInputStream.read(buffer, 0, toCopy);
                            fileOutputStream.write(buffer, 0, n);
                            toCopy -= n;
                        }
                    } else if (result == -1) {
                        status = 0;
                        break;
                    } else {
                        Slog.e("StreamFeederThread", "Error " + result + " streaming restore for " + PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                        EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                        status = result;
                    }
                } catch (IOException e) {
                    Slog.e("StreamFeederThread", "Unable to route data for restore");
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, "I/O error on pipes"});
                    status = -1003;
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    performUnifiedRestoreTask = PerformUnifiedRestoreTask.this;
                    if (this.mEngine.getAgent() != null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    performUnifiedRestoreTask.mDidLaunch = z;
                    try {
                        PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                    } catch (Exception e2) {
                        Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e2.getMessage());
                        status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                    if (status == -1000) {
                        nextState = UnifiedRestoreState.FINAL;
                    } else {
                        nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    }
                    PerformUnifiedRestoreTask.this.executeNextState(nextState);
                    setRunning(false);
                    return;
                } catch (Exception e22) {
                    Slog.e("StreamFeederThread", "Transport failed during restore: " + e22.getMessage());
                    EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                    status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    performUnifiedRestoreTask = PerformUnifiedRestoreTask.this;
                    if (this.mEngine.getAgent() != null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    performUnifiedRestoreTask.mDidLaunch = z;
                    try {
                        PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                    } catch (Exception e222) {
                        Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e222.getMessage());
                        status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                    if (status == -1000) {
                        nextState = UnifiedRestoreState.FINAL;
                    } else {
                        nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    }
                    PerformUnifiedRestoreTask.this.executeNextState(nextState);
                    setRunning(false);
                    return;
                } catch (Throwable th) {
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                    if (status == 0) {
                        nextState = UnifiedRestoreState.RESTORE_FINISHED;
                        PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                        PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    } else {
                        try {
                            PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                        } catch (Exception e2222) {
                            Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e2222.getMessage());
                            status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        }
                        PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                        if (status == -1000) {
                            nextState = UnifiedRestoreState.FINAL;
                        } else {
                            nextState = UnifiedRestoreState.RUNNING_QUEUE;
                        }
                    }
                    PerformUnifiedRestoreTask.this.executeNextState(nextState);
                    setRunning(false);
                }
            }
            IoUtils.closeQuietly(this.mEnginePipes[1]);
            IoUtils.closeQuietly(this.mTransportPipes[0]);
            IoUtils.closeQuietly(this.mTransportPipes[1]);
            this.mEngineThread.waitForResult();
            IoUtils.closeQuietly(this.mEnginePipes[0]);
            PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
            if (status == 0) {
                nextState = UnifiedRestoreState.RESTORE_FINISHED;
                PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
            } else {
                try {
                    PerformUnifiedRestoreTask.this.mTransport.abortFullRestore();
                } catch (Exception e22222) {
                    Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e22222.getMessage());
                    status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                }
                PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                if (status == -1000) {
                    nextState = UnifiedRestoreState.FINAL;
                } else {
                    nextState = UnifiedRestoreState.RUNNING_QUEUE;
                }
            }
            PerformUnifiedRestoreTask.this.executeNextState(nextState);
            setRunning(false);
        }

        public void execute() {
        }

        public void operationComplete(long result) {
        }

        public void handleCancel(boolean cancelAll) {
            PerformUnifiedRestoreTask.this.backupManagerService.removeOperation(this.mEphemeralOpToken);
            Slog.w("StreamFeederThread", "Full-data restore target timed out; shutting down");
            PerformUnifiedRestoreTask.this.mMonitor = BackupManagerMonitorUtils.monitorEvent(PerformUnifiedRestoreTask.this.mMonitor, 45, PerformUnifiedRestoreTask.this.mCurrentPackage, 2, null);
            this.mEngineThread.handleTimeout();
            IoUtils.closeQuietly(this.mEnginePipes[1]);
            this.mEnginePipes[1] = null;
            IoUtils.closeQuietly(this.mEnginePipes[0]);
            this.mEnginePipes[0] = null;
        }
    }

    private static /* synthetic */ int[] -getcom-android-server-backup-restore-UnifiedRestoreStateSwitchesValues() {
        if (-com-android-server-backup-restore-UnifiedRestoreStateSwitchesValues != null) {
            return -com-android-server-backup-restore-UnifiedRestoreStateSwitchesValues;
        }
        int[] iArr = new int[UnifiedRestoreState.values().length];
        try {
            iArr[UnifiedRestoreState.FINAL.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[UnifiedRestoreState.INITIAL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[UnifiedRestoreState.RESTORE_FINISHED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[UnifiedRestoreState.RESTORE_FULL.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[UnifiedRestoreState.RESTORE_KEYVALUE.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[UnifiedRestoreState.RUNNING_QUEUE.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        -com-android-server-backup-restore-UnifiedRestoreStateSwitchesValues = iArr;
        return iArr;
    }

    public PerformUnifiedRestoreTask(RefactoredBackupManagerService backupManagerService, IBackupTransport transport, IRestoreObserver observer, IBackupManagerMonitor monitor, long restoreSetToken, PackageInfo targetPackage, int pmToken, boolean isFullSystemRestore, String[] filterSet) {
        this.backupManagerService = backupManagerService;
        this.mEphemeralOpToken = backupManagerService.generateRandomIntegerToken();
        this.mTransport = transport;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mToken = restoreSetToken;
        this.mPmToken = pmToken;
        this.mTargetPackage = targetPackage;
        this.mIsSystemRestore = isFullSystemRestore;
        this.mFinished = false;
        this.mDidLaunch = false;
        if (targetPackage != null) {
            this.mAcceptSet = new ArrayList();
            this.mAcceptSet.add(targetPackage);
            return;
        }
        if (filterSet == null) {
            filterSet = packagesToNames(PackageManagerBackupAgent.getStorableApplications(backupManagerService.getPackageManager()));
            Slog.i(RefactoredBackupManagerService.TAG, "Full restore; asking about " + filterSet.length + " apps");
        }
        this.mAcceptSet = new ArrayList(filterSet.length);
        boolean hasSystem = false;
        boolean hasSettings = false;
        for (String packageInfo : filterSet) {
            try {
                PackageInfo info = backupManagerService.getPackageManager().getPackageInfo(packageInfo, 0);
                if ("android".equals(info.packageName)) {
                    hasSystem = true;
                } else if (RefactoredBackupManagerService.SETTINGS_PACKAGE.equals(info.packageName)) {
                    hasSettings = true;
                } else if (AppBackupUtils.appIsEligibleForBackup(info.applicationInfo)) {
                    this.mAcceptSet.add(info);
                }
            } catch (NameNotFoundException e) {
            }
        }
        if (hasSystem) {
            try {
                this.mAcceptSet.add(0, backupManagerService.getPackageManager().getPackageInfo("android", 0));
            } catch (NameNotFoundException e2) {
            }
        }
        if (hasSettings) {
            try {
                this.mAcceptSet.add(backupManagerService.getPackageManager().getPackageInfo(RefactoredBackupManagerService.SETTINGS_PACKAGE, 0));
            } catch (NameNotFoundException e3) {
            }
        }
    }

    private String[] packagesToNames(List<PackageInfo> apps) {
        int N = apps.size();
        String[] names = new String[N];
        for (int i = 0; i < N; i++) {
            names[i] = ((PackageInfo) apps.get(i)).packageName;
        }
        return names;
    }

    public void execute() {
        switch (-getcom-android-server-backup-restore-UnifiedRestoreStateSwitchesValues()[this.mState.ordinal()]) {
            case 1:
                if (this.mFinished) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Duplicate finish");
                } else {
                    finalizeRestore();
                }
                this.mFinished = true;
                return;
            case 2:
                startRestore();
                return;
            case 3:
                restoreFinished();
                return;
            case 4:
                restoreFull();
                return;
            case 5:
                restoreKeyValue();
                return;
            case 6:
                dispatchNextRestore();
                return;
            default:
                return;
        }
    }

    private void startRestore() {
        sendStartRestore(this.mAcceptSet.size());
        if (this.mIsSystemRestore) {
            AppWidgetBackupBridge.restoreStarting(0);
        }
        try {
            this.mStateDir = new File(this.backupManagerService.getBaseStateDir(), this.mTransport.transportDirName());
            PackageInfo pmPackage = new PackageInfo();
            pmPackage.packageName = RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL;
            this.mAcceptSet.add(0, pmPackage);
            this.mStatus = this.mTransport.startRestore(this.mToken, (PackageInfo[]) this.mAcceptSet.toArray(new PackageInfo[0]));
            if (this.mStatus != 0) {
                Slog.e(RefactoredBackupManagerService.TAG, "Transport error " + this.mStatus + "; no restore possible");
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            RestoreDescription desc = this.mTransport.nextRestorePackage();
            if (desc == null) {
                Slog.e(RefactoredBackupManagerService.TAG, "No restore metadata available; halting");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 22, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
            } else if (RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(desc.getPackageName())) {
                this.mCurrentPackage = new PackageInfo();
                this.mCurrentPackage.packageName = RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL;
                this.mPmAgent = new PackageManagerBackupAgent(this.backupManagerService.getPackageManager(), null);
                this.mAgent = Stub.asInterface(this.mPmAgent.onBind());
                initiateOneRestore(this.mCurrentPackage, 0);
                this.backupManagerService.getBackupHandler().removeMessages(18);
                if (!this.mPmAgent.hasMetadata()) {
                    Slog.e(RefactoredBackupManagerService.TAG, "PM agent has no metadata, so not restoring");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 24, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL, "Package manager restore metadata missing"});
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    this.backupManagerService.getBackupHandler().removeMessages(20, this);
                    executeNextState(UnifiedRestoreState.FINAL);
                }
            } else {
                Slog.e(RefactoredBackupManagerService.TAG, "Required package metadata but got " + desc.getPackageName());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 23, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
            }
        } catch (Exception e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to contact transport for restore: " + e.getMessage());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 25, null, 1, null);
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.backupManagerService.getBackupHandler().removeMessages(20, this);
            executeNextState(UnifiedRestoreState.FINAL);
        }
    }

    private void dispatchNextRestore() {
        UnifiedRestoreState nextState = UnifiedRestoreState.FINAL;
        try {
            this.mRestoreDescription = this.mTransport.nextRestorePackage();
            String packageName = this.mRestoreDescription != null ? this.mRestoreDescription.getPackageName() : null;
            if (packageName == null) {
                Slog.e(RefactoredBackupManagerService.TAG, "Failure getting next package name");
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                nextState = UnifiedRestoreState.FINAL;
            } else if (this.mRestoreDescription == RestoreDescription.NO_MORE_PACKAGES) {
                Slog.v(RefactoredBackupManagerService.TAG, "No more packages; finishing restore");
                int millis = (int) (SystemClock.elapsedRealtime() - this.mStartRealtime);
                EventLog.writeEvent(EventLogTags.RESTORE_SUCCESS, new Object[]{Integer.valueOf(this.mCount), Integer.valueOf(millis)});
                executeNextState(UnifiedRestoreState.FINAL);
            } else {
                Slog.i(RefactoredBackupManagerService.TAG, "Next restore package: " + this.mRestoreDescription);
                sendOnRestorePackage(packageName);
                Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
                if (metaInfo == null) {
                    Slog.e(RefactoredBackupManagerService.TAG, "No metadata for " + packageName);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Package metadata missing"});
                    executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                    return;
                }
                try {
                    this.mCurrentPackage = this.backupManagerService.getPackageManager().getPackageInfo(packageName, 64);
                    if (metaInfo.versionCode > this.mCurrentPackage.versionCode) {
                        if ((this.mCurrentPackage.applicationInfo.flags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0) {
                            Slog.w(RefactoredBackupManagerService.TAG, "Package " + packageName + ": " + ("Source version " + metaInfo.versionCode + " > installed version " + this.mCurrentPackage.versionCode));
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_RESTORE_VERSION", (long) metaInfo.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", false));
                            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, message});
                            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                            return;
                        }
                        Slog.v(RefactoredBackupManagerService.TAG, "Source version " + metaInfo.versionCode + " > installed version " + this.mCurrentPackage.versionCode + " but restoreAnyVersion");
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_RESTORE_VERSION", (long) metaInfo.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", true));
                    }
                    this.mWidgetData = null;
                    int type = this.mRestoreDescription.getDataType();
                    if (type == 1) {
                        nextState = UnifiedRestoreState.RESTORE_KEYVALUE;
                    } else if (type == 2) {
                        nextState = UnifiedRestoreState.RESTORE_FULL;
                    } else {
                        Slog.e(RefactoredBackupManagerService.TAG, "Unrecognized restore type " + type);
                        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                        return;
                    }
                    executeNextState(nextState);
                } catch (NameNotFoundException e) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Package not present: " + packageName);
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 26, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Package missing on device"});
                    executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                }
            }
        } catch (Exception e2) {
            Slog.e(RefactoredBackupManagerService.TAG, "Can't get next restore target from transport; halting: " + e2.getMessage());
            EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
            nextState = UnifiedRestoreState.FINAL;
        } finally {
            executeNextState(nextState);
        }
    }

    private void restoreKeyValue() {
        String packageName = this.mCurrentPackage.packageName;
        if (this.mCurrentPackage.applicationInfo.backupAgentName == null || "".equals(this.mCurrentPackage.applicationInfo.backupAgentName)) {
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 28, this.mCurrentPackage, 2, null);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Package has no agent"});
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
        if (BackupUtils.signaturesMatch(metaInfo.sigHashes, this.mCurrentPackage)) {
            this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
            if (this.mAgent == null) {
                Slog.w(RefactoredBackupManagerService.TAG, "Can't find backup agent for " + packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 30, this.mCurrentPackage, 3, null);
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Restore agent missing"});
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                return;
            }
            this.mDidLaunch = true;
            try {
                initiateOneRestore(this.mCurrentPackage, metaInfo.versionCode);
                this.mCount++;
            } catch (Exception e) {
                Slog.e(RefactoredBackupManagerService.TAG, "Error when attempting restore: " + e.toString());
                keyValueAgentErrorCleanup();
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
            return;
        }
        Slog.w(RefactoredBackupManagerService.TAG, "Signature mismatch restoring " + packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 29, this.mCurrentPackage, 3, null);
        EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Signature mismatch"});
        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
    }

    void initiateOneRestore(PackageInfo app, int appVersionCode) {
        String packageName = app.packageName;
        Slog.d(RefactoredBackupManagerService.TAG, "initiateOneRestore packageName=" + packageName);
        this.mBackupDataName = new File(this.backupManagerService.getDataDir(), packageName + ".restore");
        this.mStageName = new File(this.backupManagerService.getDataDir(), packageName + ".stage");
        this.mNewStateName = new File(this.mStateDir, packageName + ".new");
        this.mSavedStateName = new File(this.mStateDir, packageName);
        boolean staging = packageName.equals("android") ^ 1;
        File downloadFile = staging ? this.mStageName : this.mBackupDataName;
        try {
            ParcelFileDescriptor stage = ParcelFileDescriptor.open(downloadFile, 1006632960);
            if (this.mTransport.getRestoreData(stage) != 0) {
                Slog.e(RefactoredBackupManagerService.TAG, "Error getting restore data for " + packageName);
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                stage.close();
                downloadFile.delete();
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            if (staging) {
                stage.close();
                stage = ParcelFileDescriptor.open(downloadFile, 268435456);
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                BackupDataInput in = new BackupDataInput(stage.getFileDescriptor());
                BackupDataOutput out = new BackupDataOutput(this.mBackupData.getFileDescriptor());
                byte[] buffer = new byte[8192];
                while (in.readNextHeader()) {
                    String key = in.getKey();
                    int size = in.getDataSize();
                    if (key.equals(RefactoredBackupManagerService.KEY_WIDGET_STATE)) {
                        Slog.i(RefactoredBackupManagerService.TAG, "Restoring widget state for " + packageName);
                        this.mWidgetData = new byte[size];
                        in.readEntityData(this.mWidgetData, 0, size);
                    } else {
                        if (size > buffer.length) {
                            buffer = new byte[size];
                        }
                        in.readEntityData(buffer, 0, size);
                        out.writeEntityHeader(key, size);
                        out.writeEntityData(buffer, size);
                    }
                }
                this.mBackupData.close();
            }
            stage.close();
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, 60000, this, 1);
            this.mAgent.doRestore(this.mBackupData, appVersionCode, this.mNewState, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to call app for restore: " + packageName, e);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, e.toString()});
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFull() {
        try {
            new Thread(new StreamFeederThread(), "unified-stream-feeder").start();
        } catch (IOException e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to construct pipes for stream restore!");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFinished() {
        try {
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, 30000, this, 1);
            this.mAgent.doRestoreFinished(this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to finalize restore of " + this.mCurrentPackage.packageName);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, e.toString()});
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void finalizeRestore() {
        try {
            this.mTransport.finishRestore();
        } catch (Exception e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Error finishing restore", e);
        }
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e2) {
                Slog.d(RefactoredBackupManagerService.TAG, "Restore observer died at restoreFinished");
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(8);
        if (this.mPmToken > 0) {
            try {
                this.backupManagerService.getPackageManagerBinder().finishPackageInstall(this.mPmToken, this.mDidLaunch);
            } catch (RemoteException e3) {
            }
        } else {
            this.backupManagerService.getBackupHandler().sendEmptyMessageDelayed(8, 60000);
        }
        AppWidgetBackupBridge.restoreFinished(0);
        if (this.mIsSystemRestore && this.mPmAgent != null) {
            this.backupManagerService.setAncestralPackages(this.mPmAgent.getRestoredPackages());
            this.backupManagerService.setAncestralToken(this.mToken);
            this.backupManagerService.writeRestoreTokens();
        }
        Slog.i(RefactoredBackupManagerService.TAG, "Restore complete.");
        synchronized (this.backupManagerService.getPendingRestores()) {
            if (this.backupManagerService.getPendingRestores().size() > 0) {
                Slog.d(RefactoredBackupManagerService.TAG, "Starting next pending restore.");
                this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, (PerformUnifiedRestoreTask) this.backupManagerService.getPendingRestores().remove()));
            } else {
                this.backupManagerService.setRestoreInProgress(false);
            }
        }
        this.backupManagerService.getWakelock().release();
    }

    void keyValueAgentErrorCleanup() {
        this.backupManagerService.clearApplicationDataSynchronous(this.mCurrentPackage.packageName);
        keyValueAgentCleanup();
    }

    void keyValueAgentCleanup() {
        this.mBackupDataName.delete();
        this.mStageName.delete();
        try {
            if (this.mBackupData != null) {
                this.mBackupData.close();
            }
        } catch (IOException e) {
        }
        try {
            if (this.mNewState != null) {
                this.mNewState.close();
            }
        } catch (IOException e2) {
        }
        this.mNewState = null;
        this.mBackupData = null;
        this.mNewStateName.delete();
        if (this.mCurrentPackage.applicationInfo != null) {
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
                boolean killAfterRestore = this.mCurrentPackage.applicationInfo.uid >= 10000 ? this.mRestoreDescription.getDataType() != 2 ? (65536 & this.mCurrentPackage.applicationInfo.flags) != 0 : true : false;
                if (this.mTargetPackage == null && killAfterRestore) {
                    Slog.d(RefactoredBackupManagerService.TAG, "Restore complete, killing host process of " + this.mCurrentPackage.applicationInfo.processName);
                    this.backupManagerService.getActivityManager().killApplicationProcess(this.mCurrentPackage.applicationInfo.processName, this.mCurrentPackage.applicationInfo.uid);
                }
            } catch (RemoteException e3) {
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(18, this);
    }

    public void operationComplete(long unusedResult) {
        UnifiedRestoreState nextState;
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        switch (-getcom-android-server-backup-restore-UnifiedRestoreStateSwitchesValues()[this.mState.ordinal()]) {
            case 2:
                nextState = UnifiedRestoreState.RUNNING_QUEUE;
                break;
            case 3:
                int size = (int) this.mBackupDataName.length();
                EventLog.writeEvent(EventLogTags.RESTORE_PACKAGE, new Object[]{this.mCurrentPackage.packageName, Integer.valueOf(size)});
                keyValueAgentCleanup();
                if (this.mWidgetData != null) {
                    this.backupManagerService.restoreWidgetData(this.mCurrentPackage.packageName, this.mWidgetData);
                }
                nextState = UnifiedRestoreState.RUNNING_QUEUE;
                break;
            case 4:
            case 5:
                nextState = UnifiedRestoreState.RESTORE_FINISHED;
                break;
            default:
                Slog.e(RefactoredBackupManagerService.TAG, "Unexpected restore callback into state " + this.mState);
                keyValueAgentErrorCleanup();
                nextState = UnifiedRestoreState.FINAL;
                break;
        }
        executeNextState(nextState);
    }

    public void handleCancel(boolean cancelAll) {
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        Slog.e(RefactoredBackupManagerService.TAG, "Timeout restoring application " + this.mCurrentPackage.packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 31, this.mCurrentPackage, 2, null);
        EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{this.mCurrentPackage.packageName, "restore timeout"});
        keyValueAgentErrorCleanup();
        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
    }

    void executeNextState(UnifiedRestoreState nextState) {
        this.mState = nextState;
        this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, this));
    }

    void sendStartRestore(int numPackages) {
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreStarting(numPackages);
            } catch (RemoteException e) {
                Slog.w(RefactoredBackupManagerService.TAG, "Restore observer went away: startRestore");
                this.mObserver = null;
            }
        }
    }

    void sendOnRestorePackage(String name) {
        if (this.mObserver != null && this.mObserver != null) {
            try {
                this.mObserver.onUpdate(this.mCount, name);
            } catch (RemoteException e) {
                Slog.d(RefactoredBackupManagerService.TAG, "Restore observer died in onUpdate");
                this.mObserver = null;
            }
        }
    }

    void sendEndRestore() {
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e) {
                Slog.w(RefactoredBackupManagerService.TAG, "Restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }
}
