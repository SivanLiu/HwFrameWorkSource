package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.IBackupAgent.Stub;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.RestoreDescription;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.BackupUtils;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.PackageManagerBackupAgent.Metadata;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class PerformUnifiedRestoreTask implements BackupRestoreTask {
    private BackupManagerService backupManagerService;
    private List<PackageInfo> mAcceptSet;
    private IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private int mCount;
    private PackageInfo mCurrentPackage;
    private boolean mDidLaunch;
    private final int mEphemeralOpToken;
    private boolean mFinished;
    private boolean mIsSystemRestore;
    private final OnTaskFinishedListener mListener;
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
    private final TransportClient mTransportClient;
    private final TransportManager mTransportManager;
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

        /* JADX WARNING: Missing block: B:46:0x01f3, code skipped:
            r12 = r1;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            UnifiedRestoreState nextState;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            UnifiedRestoreState nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
            int status = 0;
            EventLog.writeEvent(EventLogTags.FULL_RESTORE_PACKAGE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            this.mEngine = new FullRestoreEngine(PerformUnifiedRestoreTask.this.backupManagerService, this, null, PerformUnifiedRestoreTask.this.mMonitor, PerformUnifiedRestoreTask.this.mCurrentPackage, false, false, this.mEphemeralOpToken);
            int i = 0;
            this.mEngineThread = new EngineThread(this.mEngine, this.mEnginePipes[0]);
            ParcelFileDescriptor eWriteEnd = this.mEnginePipes[1];
            ParcelFileDescriptor tReadEnd = this.mTransportPipes[0];
            ParcelFileDescriptor tWriteEnd = this.mTransportPipes[1];
            int bufferSize = 32768;
            byte[] buffer = new byte[32768];
            FileOutputStream engineOut = new FileOutputStream(eWriteEnd.getFileDescriptor());
            FileInputStream transportIn = new FileInputStream(tReadEnd.getFileDescriptor());
            new Thread(this.mEngineThread, "unified-restore-engine").start();
            String callerLogString = "PerformUnifiedRestoreTask$StreamFeederThread.run()";
            int toCopy;
            StringBuilder stringBuilder3;
            try {
                IBackupTransport transport = PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow(callerLogString);
                while (status == 0) {
                    int result = transport.getNextFullRestoreDataChunk(tWriteEnd);
                    if (result > 0) {
                        if (result > bufferSize) {
                            bufferSize = result;
                            buffer = new byte[bufferSize];
                        }
                        toCopy = result;
                        while (toCopy > 0) {
                            int n = transportIn.read(buffer, i, toCopy);
                            engineOut.write(buffer, i, n);
                            toCopy -= n;
                        }
                    } else if (result == -1) {
                        status = 0;
                        break;
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Error ");
                        stringBuilder3.append(result);
                        stringBuilder3.append(" streaming restore for ");
                        stringBuilder3.append(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                        Slog.e("StreamFeederThread", stringBuilder3.toString());
                        EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                        status = result;
                    }
                    i = 0;
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
                        PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow(callerLogString).abortFullRestore();
                    } catch (Exception e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Transport threw from abortFullRestore: ");
                        stringBuilder.append(e.getMessage());
                        Slog.e("StreamFeederThread", stringBuilder.toString());
                        status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                    if (status == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                        nextState = UnifiedRestoreState.FINAL;
                    } else {
                        nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    }
                }
            } catch (IOException e2) {
                Slog.e("StreamFeederThread", "Unable to route data for restore");
                EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, "I/O error on pipes"});
                toCopy = -1003;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (-1003 == null) {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                } else {
                    try {
                        PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow(callerLogString).abortFullRestore();
                    } catch (Exception e3) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Transport threw from abortFullRestore: ");
                        stringBuilder2.append(e3.getMessage());
                        Slog.e("StreamFeederThread", stringBuilder2.toString());
                        toCopy = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                    nextState = toCopy == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                }
            } catch (Exception e32) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Transport failed during restore: ");
                stringBuilder.append(e32.getMessage());
                Slog.e("StreamFeederThread", stringBuilder.toString());
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                toCopy = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (-1000 == null) {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                } else {
                    try {
                        PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow(callerLogString).abortFullRestore();
                    } catch (Exception e322) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Transport threw from abortFullRestore: ");
                        stringBuilder2.append(e322.getMessage());
                        Slog.e("StreamFeederThread", stringBuilder2.toString());
                        toCopy = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                    nextState = toCopy == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                }
            } catch (Throwable th) {
                Throwable th2 = th;
                boolean z = true;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask performUnifiedRestoreTask = PerformUnifiedRestoreTask.this;
                if (this.mEngine.getAgent() == null) {
                    z = false;
                }
                performUnifiedRestoreTask.mDidLaunch = z;
                if (null != null) {
                    try {
                        PerformUnifiedRestoreTask.this.mTransportClient.connectOrThrow(callerLogString).abortFullRestore();
                    } catch (Exception e3222) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Transport threw from abortFullRestore: ");
                        stringBuilder3.append(e3222.getMessage());
                        Slog.e("StreamFeederThread", stringBuilder3.toString());
                        status = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    }
                    PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataSynchronous(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, false);
                    if (status == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                        nextState = UnifiedRestoreState.FINAL;
                    } else {
                        nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    }
                } else {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                }
                PerformUnifiedRestoreTask.this.executeNextState(nextState);
                setRunning(false);
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

    public PerformUnifiedRestoreTask(BackupManagerService backupManagerService, TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long restoreSetToken, PackageInfo targetPackage, int pmToken, boolean isFullSystemRestore, String[] filterSet, OnTaskFinishedListener listener) {
        PackageInfo packageInfo = targetPackage;
        this.backupManagerService = backupManagerService;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mEphemeralOpToken = backupManagerService.generateRandomIntegerToken();
        this.mTransportClient = transportClient;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mToken = restoreSetToken;
        this.mPmToken = pmToken;
        this.mTargetPackage = packageInfo;
        this.mIsSystemRestore = isFullSystemRestore;
        this.mFinished = false;
        this.mDidLaunch = false;
        this.mListener = listener;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        String[] strArr;
        if (packageInfo != null) {
            this.mAcceptSet = new ArrayList();
            this.mAcceptSet.add(packageInfo);
            strArr = filterSet;
            return;
        }
        if (filterSet == null) {
            String[] filterSet2 = packagesToNames(PackageManagerBackupAgent.getStorableApplications(backupManagerService.getPackageManager()));
            String str = BackupManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Full restore; asking about ");
            stringBuilder.append(filterSet2.length);
            stringBuilder.append(" apps");
            Slog.i(str, stringBuilder.toString());
            strArr = filterSet2;
        } else {
            strArr = filterSet;
        }
        this.mAcceptSet = new ArrayList(strArr.length);
        boolean hasSettings = false;
        boolean hasSystem = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= strArr.length) {
                break;
            }
            try {
                PackageManager pm = backupManagerService.getPackageManager();
                packageInfo = pm.getPackageInfo(strArr[i2], 0);
                if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageInfo.packageName)) {
                    hasSystem = true;
                } else if (BackupManagerService.SETTINGS_PACKAGE.equals(packageInfo.packageName)) {
                    hasSettings = true;
                } else if (AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, pm)) {
                    this.mAcceptSet.add(packageInfo);
                }
            } catch (NameNotFoundException e) {
            }
            i = i2 + 1;
            packageInfo = targetPackage;
            TransportClient transportClient2 = transportClient;
            IRestoreObserver iRestoreObserver = observer;
        }
        if (hasSystem) {
            try {
                this.mAcceptSet.add(0, backupManagerService.getPackageManager().getPackageInfo(PackageManagerService.PLATFORM_PACKAGE_NAME, 0));
            } catch (NameNotFoundException e2) {
            }
        }
        if (hasSettings) {
            try {
                this.mAcceptSet.add(backupManagerService.getPackageManager().getPackageInfo(BackupManagerService.SETTINGS_PACKAGE, 0));
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
        switch (this.mState) {
            case INITIAL:
                startRestore();
                return;
            case RUNNING_QUEUE:
                dispatchNextRestore();
                return;
            case RESTORE_KEYVALUE:
                restoreKeyValue();
                return;
            case RESTORE_FULL:
                restoreFull();
                return;
            case RESTORE_FINISHED:
                restoreFinished();
                return;
            case FINAL:
                if (this.mFinished) {
                    Slog.e(BackupManagerService.TAG, "Duplicate finish");
                } else {
                    finalizeRestore();
                }
                this.mFinished = true;
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
            this.mStateDir = new File(this.backupManagerService.getBaseStateDir(), this.mTransportManager.getTransportDirName(this.mTransportClient.getTransportComponent()));
            PackageInfo pmPackage = new PackageInfo();
            pmPackage.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
            this.mAcceptSet.add(0, pmPackage);
            PackageInfo[] packages = (PackageInfo[]) this.mAcceptSet.toArray(new PackageInfo[0]);
            IBackupTransport transport = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.startRestore()");
            this.mStatus = transport.startRestore(this.mToken, packages);
            String str;
            if (this.mStatus != 0) {
                str = BackupManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Transport error ");
                stringBuilder.append(this.mStatus);
                stringBuilder.append("; no restore possible");
                Slog.e(str, stringBuilder.toString());
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            RestoreDescription desc = transport.nextRestorePackage();
            if (desc == null) {
                Slog.e(BackupManagerService.TAG, "No restore metadata available; halting");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 22, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
            } else if (BackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(desc.getPackageName())) {
                this.mCurrentPackage = new PackageInfo();
                this.mCurrentPackage.packageName = BackupManagerService.PACKAGE_MANAGER_SENTINEL;
                this.mPmAgent = this.backupManagerService.makeMetadataAgent(null);
                this.mAgent = Stub.asInterface(this.mPmAgent.onBind());
                initiateOneRestore(this.mCurrentPackage, 0);
                this.backupManagerService.getBackupHandler().removeMessages(18);
                if (!this.mPmAgent.hasMetadata()) {
                    Slog.e(BackupManagerService.TAG, "PM agent has no metadata, so not restoring");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 24, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{BackupManagerService.PACKAGE_MANAGER_SENTINEL, "Package manager restore metadata missing"});
                    this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    this.backupManagerService.getBackupHandler().removeMessages(20, this);
                    executeNextState(UnifiedRestoreState.FINAL);
                }
            } else {
                str = BackupManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Required package metadata but got ");
                stringBuilder2.append(desc.getPackageName());
                Slog.e(str, stringBuilder2.toString());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 23, this.mCurrentPackage, 3, null);
                this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                executeNextState(UnifiedRestoreState.FINAL);
            }
        } catch (Exception e) {
            String str2 = BackupManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Unable to contact transport for restore: ");
            stringBuilder3.append(e.getMessage());
            Slog.e(str2, stringBuilder3.toString());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 25, null, 1, null);
            this.mStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.backupManagerService.getBackupHandler().removeMessages(20, this);
            executeNextState(UnifiedRestoreState.FINAL);
        }
    }

    private void dispatchNextRestore() {
        UnifiedRestoreState nextState = UnifiedRestoreState.FINAL;
        String str;
        try {
            this.mRestoreDescription = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.dispatchNextRestore()").nextRestorePackage();
            String pkgName = this.mRestoreDescription != null ? this.mRestoreDescription.getPackageName() : null;
            int millis;
            if (pkgName == null) {
                Slog.e(BackupManagerService.TAG, "Failure getting next package name");
                EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                executeNextState(UnifiedRestoreState.FINAL);
            } else if (this.mRestoreDescription == RestoreDescription.NO_MORE_PACKAGES) {
                Slog.v(BackupManagerService.TAG, "No more packages; finishing restore");
                millis = (int) (SystemClock.elapsedRealtime() - this.mStartRealtime);
                EventLog.writeEvent(EventLogTags.RESTORE_SUCCESS, new Object[]{Integer.valueOf(this.mCount), Integer.valueOf(millis)});
                executeNextState(UnifiedRestoreState.FINAL);
            } else {
                String str2 = BackupManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Next restore package: ");
                stringBuilder.append(this.mRestoreDescription);
                Slog.i(str2, stringBuilder.toString());
                sendOnRestorePackage(pkgName);
                Metadata metaInfo = this.mPmAgent.getRestoredMetadata(pkgName);
                StringBuilder stringBuilder2;
                if (metaInfo == null) {
                    str2 = BackupManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No metadata for ");
                    stringBuilder2.append(pkgName);
                    Slog.e(str2, stringBuilder2.toString());
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{pkgName, "Package metadata missing"});
                    executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                    return;
                }
                try {
                    this.mCurrentPackage = this.backupManagerService.getPackageManager().getPackageInfo(pkgName, 134217728);
                    if (metaInfo.versionCode > this.mCurrentPackage.getLongVersionCode()) {
                        if ((this.mCurrentPackage.applicationInfo.flags & 131072) == 0) {
                            str2 = new StringBuilder();
                            str2.append("Source version ");
                            str2.append(metaInfo.versionCode);
                            str2.append(" > installed version ");
                            str2.append(this.mCurrentPackage.getLongVersionCode());
                            str2 = str2.toString();
                            String str3 = BackupManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Package ");
                            stringBuilder3.append(pkgName);
                            stringBuilder3.append(": ");
                            stringBuilder3.append(str2);
                            Slog.w(str3, stringBuilder3.toString());
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_RESTORE_VERSION", metaInfo.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", false));
                            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{pkgName, str2});
                            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                            return;
                        }
                        str2 = BackupManagerService.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Source version ");
                        stringBuilder4.append(metaInfo.versionCode);
                        stringBuilder4.append(" > installed version ");
                        stringBuilder4.append(this.mCurrentPackage.getLongVersionCode());
                        stringBuilder4.append(" but restoreAnyVersion");
                        Slog.v(str2, stringBuilder4.toString());
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_RESTORE_VERSION", metaInfo.versionCode), "android.app.backup.extra.LOG_RESTORE_ANYWAY", true));
                    }
                    this.mWidgetData = null;
                    millis = this.mRestoreDescription.getDataType();
                    if (millis == 1) {
                        nextState = UnifiedRestoreState.RESTORE_KEYVALUE;
                    } else if (millis == 2) {
                        nextState = UnifiedRestoreState.RESTORE_FULL;
                    } else {
                        str = BackupManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unrecognized restore type ");
                        stringBuilder2.append(millis);
                        Slog.e(str, stringBuilder2.toString());
                        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                        return;
                    }
                    executeNextState(nextState);
                } catch (NameNotFoundException e) {
                    str = BackupManagerService.TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Package not present: ");
                    stringBuilder5.append(pkgName);
                    Slog.e(str, stringBuilder5.toString());
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 26, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{pkgName, "Package missing on device"});
                    executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
                }
            }
        } catch (Exception e2) {
            str = BackupManagerService.TAG;
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("Can't get next restore target from transport; halting: ");
            stringBuilder6.append(e2.getMessage());
            Slog.e(str, stringBuilder6.toString());
            EventLog.writeEvent(EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
            executeNextState(UnifiedRestoreState.FINAL);
        } catch (Throwable th) {
            executeNextState(nextState);
            throw th;
        }
    }

    private void restoreKeyValue() {
        String packageName = this.mCurrentPackage.packageName;
        if (this.mCurrentPackage.applicationInfo.backupAgentName == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(this.mCurrentPackage.applicationInfo.backupAgentName)) {
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 28, this.mCurrentPackage, 2, null);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Package has no agent"});
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
        String str;
        StringBuilder stringBuilder;
        if (BackupUtils.signaturesMatch(metaInfo.sigHashes, this.mCurrentPackage, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class))) {
            this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0);
            if (this.mAgent == null) {
                str = BackupManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't find backup agent for ");
                stringBuilder.append(packageName);
                Slog.w(str, stringBuilder.toString());
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
                String str2 = BackupManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error when attempting restore: ");
                stringBuilder2.append(e.toString());
                Slog.e(str2, stringBuilder2.toString());
                keyValueAgentErrorCleanup();
                executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            }
            return;
        }
        str = BackupManagerService.TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Signature mismatch restoring ");
        stringBuilder.append(packageName);
        Slog.w(str, stringBuilder.toString());
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 29, this.mCurrentPackage, 3, null);
        EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, "Signature mismatch"});
        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
    }

    void initiateOneRestore(PackageInfo app, long appVersionCode) {
        String packageName = app.packageName;
        String str = BackupManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initiateOneRestore packageName=");
        stringBuilder.append(packageName);
        Slog.d(str, stringBuilder.toString());
        File dataDir = this.backupManagerService.getDataDir();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(packageName);
        stringBuilder2.append(".restore");
        this.mBackupDataName = new File(dataDir, stringBuilder2.toString());
        dataDir = this.backupManagerService.getDataDir();
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(packageName);
        stringBuilder2.append(".stage");
        this.mStageName = new File(dataDir, stringBuilder2.toString());
        dataDir = this.mStateDir;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(packageName);
        stringBuilder2.append(".new");
        this.mNewStateName = new File(dataDir, stringBuilder2.toString());
        this.mSavedStateName = new File(this.mStateDir, packageName);
        boolean staging = packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME) ^ 1;
        File downloadFile = staging ? this.mStageName : this.mBackupDataName;
        String str2;
        try {
            IBackupTransport transport = this.mTransportClient.connectOrThrow("PerformUnifiedRestoreTask.initiateOneRestore()");
            ParcelFileDescriptor stage = ParcelFileDescriptor.open(downloadFile, 1006632960);
            if (transport.getRestoreData(stage) != 0) {
                str2 = BackupManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error getting restore data for ");
                stringBuilder3.append(packageName);
                Slog.e(str2, stringBuilder3.toString());
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
                    if (key.equals(BackupManagerService.KEY_WIDGET_STATE)) {
                        String str3 = BackupManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Restoring widget state for ");
                        stringBuilder.append(packageName);
                        Slog.i(str3, stringBuilder.toString());
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
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis(), this, 1);
            this.mAgent.doRestore(this.mBackupData, appVersionCode, this.mNewState, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            str2 = BackupManagerService.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to call app for restore: ");
            stringBuilder2.append(packageName);
            Slog.e(str2, stringBuilder2.toString(), e);
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, e.toString()});
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFull() {
        try {
            new Thread(new StreamFeederThread(), "unified-stream-feeder").start();
        } catch (IOException e) {
            Slog.e(BackupManagerService.TAG, "Unable to construct pipes for stream restore!");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFinished() {
        String str = BackupManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("restoreFinished packageName=");
        stringBuilder.append(this.mCurrentPackage.packageName);
        Slog.d(str, stringBuilder.toString());
        try {
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, this.mAgentTimeoutParameters.getRestoreAgentFinishedTimeoutMillis(), this, 1);
            this.mAgent.doRestoreFinished(this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            String packageName = this.mCurrentPackage.packageName;
            String str2 = BackupManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to finalize restore of ");
            stringBuilder2.append(packageName);
            Slog.e(str2, stringBuilder2.toString());
            EventLog.writeEvent(EventLogTags.RESTORE_AGENT_FAILURE, new Object[]{packageName, e.toString()});
            keyValueAgentErrorCleanup();
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void finalizeRestore() {
        String callerLogString = "PerformUnifiedRestoreTask.finalizeRestore()";
        try {
            this.mTransportClient.connectOrThrow(callerLogString).finishRestore();
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Error finishing restore", e);
        }
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e2) {
                Slog.d(BackupManagerService.TAG, "Restore observer died at restoreFinished");
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(8);
        if (this.mPmToken > 0) {
            try {
                this.backupManagerService.getPackageManagerBinder().finishPackageInstall(this.mPmToken, this.mDidLaunch);
            } catch (RemoteException e3) {
            }
        } else {
            this.backupManagerService.getBackupHandler().sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
        }
        AppWidgetBackupBridge.restoreFinished(0);
        if (this.mIsSystemRestore && this.mPmAgent != null) {
            this.backupManagerService.setAncestralPackages(this.mPmAgent.getRestoredPackages());
            this.backupManagerService.setAncestralToken(this.mToken);
            this.backupManagerService.writeRestoreTokens();
        }
        synchronized (this.backupManagerService.getPendingRestores()) {
            if (this.backupManagerService.getPendingRestores().size() > 0) {
                Slog.d(BackupManagerService.TAG, "Starting next pending restore.");
                this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, (PerformUnifiedRestoreTask) this.backupManagerService.getPendingRestores().remove()));
            } else {
                this.backupManagerService.setRestoreInProgress(false);
            }
        }
        Slog.i(BackupManagerService.TAG, "Restore complete.");
        this.mListener.onFinished(callerLogString);
    }

    void keyValueAgentErrorCleanup() {
        this.backupManagerService.clearApplicationDataSynchronous(this.mCurrentPackage.packageName, false);
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
                boolean killAfterRestore = this.mCurrentPackage.applicationInfo.uid >= 10000 && (this.mRestoreDescription.getDataType() == 2 || (65536 & this.mCurrentPackage.applicationInfo.flags) != 0);
                if (this.mTargetPackage == null && killAfterRestore) {
                    String str = BackupManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Restore complete, killing host process of ");
                    stringBuilder.append(this.mCurrentPackage.applicationInfo.processName);
                    Slog.d(str, stringBuilder.toString());
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
        int i = AnonymousClass1.$SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[this.mState.ordinal()];
        if (i != 1) {
            switch (i) {
                case 3:
                case 4:
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    break;
                case 5:
                    i = (int) this.mBackupDataName.length();
                    EventLog.writeEvent(EventLogTags.RESTORE_PACKAGE, new Object[]{this.mCurrentPackage.packageName, Integer.valueOf(i)});
                    keyValueAgentCleanup();
                    if (this.mWidgetData != null) {
                        this.backupManagerService.restoreWidgetData(this.mCurrentPackage.packageName, this.mWidgetData);
                    }
                    nextState = UnifiedRestoreState.RUNNING_QUEUE;
                    break;
                default:
                    String str = BackupManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected restore callback into state ");
                    stringBuilder.append(this.mState);
                    Slog.e(str, stringBuilder.toString());
                    keyValueAgentErrorCleanup();
                    nextState = UnifiedRestoreState.FINAL;
                    break;
            }
        }
        nextState = UnifiedRestoreState.RUNNING_QUEUE;
        executeNextState(nextState);
    }

    public void handleCancel(boolean cancelAll) {
        this.backupManagerService.removeOperation(this.mEphemeralOpToken);
        String str = BackupManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Timeout restoring application ");
        stringBuilder.append(this.mCurrentPackage.packageName);
        Slog.e(str, stringBuilder.toString());
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
                Slog.w(BackupManagerService.TAG, "Restore observer went away: startRestore");
                this.mObserver = null;
            }
        }
    }

    void sendOnRestorePackage(String name) {
        if (this.mObserver != null) {
            try {
                this.mObserver.onUpdate(this.mCount, name);
            } catch (RemoteException e) {
                Slog.d(BackupManagerService.TAG, "Restore observer died in onUpdate");
                this.mObserver = null;
            }
        }
    }

    void sendEndRestore() {
        if (this.mObserver != null) {
            try {
                this.mObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }
}
