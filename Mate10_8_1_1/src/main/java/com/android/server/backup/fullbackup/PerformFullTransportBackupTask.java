package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformFullTransportBackupTask extends FullBackupTask implements BackupRestoreTask {
    private static final String TAG = "PFTBT";
    private RefactoredBackupManagerService backupManagerService;
    IBackupObserver mBackupObserver;
    SinglePackageBackupRunner mBackupRunner;
    private final int mBackupRunnerOpToken;
    private volatile boolean mCancelAll;
    private final Object mCancelLock = new Object();
    private final int mCurrentOpToken;
    PackageInfo mCurrentPackage;
    private volatile boolean mIsDoingBackup;
    FullBackupJob mJob;
    CountDownLatch mLatch;
    IBackupManagerMonitor mMonitor;
    ArrayList<PackageInfo> mPackages;
    private volatile IBackupTransport mTransport;
    boolean mUpdateSchedule;
    boolean mUserInitiated;

    class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
        private final int mCurrentOpToken;
        final CountDownLatch mLatch = new CountDownLatch(1);
        final long mQuota;
        final AtomicLong mResult = new AtomicLong(-1003);
        final IBackupTransport mTransport;

        SinglePackageBackupPreflight(IBackupTransport transport, long quota, int currentOpToken) {
            this.mTransport = transport;
            this.mQuota = quota;
            this.mCurrentOpToken = currentOpToken;
        }

        public int preflightFullBackup(PackageInfo pkg, IBackupAgent agent) {
            int result;
            try {
                PerformFullTransportBackupTask.this.backupManagerService.prepareOperationTimeout(this.mCurrentOpToken, RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, this, 0);
                PerformFullTransportBackupTask.this.backupManagerService.addBackupTrace("preflighting");
                agent.doMeasureFullBackup(this.mQuota, this.mCurrentOpToken, PerformFullTransportBackupTask.this.backupManagerService.getBackupManagerBinder());
                this.mLatch.await(RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, TimeUnit.MILLISECONDS);
                long totalSize = this.mResult.get();
                if (totalSize < 0) {
                    return (int) totalSize;
                }
                result = this.mTransport.checkFullBackupSize(totalSize);
                if (result == -1005) {
                    agent.doQuotaExceeded(totalSize, this.mQuota);
                }
                return result;
            } catch (Exception e) {
                Slog.w(PerformFullTransportBackupTask.TAG, "Exception preflighting " + pkg.packageName + ": " + e.getMessage());
                result = -1003;
            }
        }

        public void execute() {
        }

        public void operationComplete(long result) {
            this.mResult.set(result);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }

        public void handleCancel(boolean cancelAll) {
            this.mResult.set(-1003);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }

        public long getExpectedSizeOrErrorCode() {
            try {
                this.mLatch.await(RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, TimeUnit.MILLISECONDS);
                return this.mResult.get();
            } catch (InterruptedException e) {
                return -1;
            }
        }
    }

    class SinglePackageBackupRunner implements Runnable, BackupRestoreTask {
        final CountDownLatch mBackupLatch = new CountDownLatch(1);
        private volatile int mBackupResult = -1003;
        private final int mCurrentOpToken;
        private FullBackupEngine mEngine;
        private final int mEphemeralToken;
        private volatile boolean mIsCancelled;
        final ParcelFileDescriptor mOutput;
        final SinglePackageBackupPreflight mPreflight;
        final CountDownLatch mPreflightLatch = new CountDownLatch(1);
        private volatile int mPreflightResult = -1003;
        private final long mQuota;
        final PackageInfo mTarget;

        SinglePackageBackupRunner(ParcelFileDescriptor output, PackageInfo target, IBackupTransport transport, long quota, int currentOpToken) throws IOException {
            this.mOutput = ParcelFileDescriptor.dup(output.getFileDescriptor());
            this.mTarget = target;
            this.mCurrentOpToken = currentOpToken;
            this.mEphemeralToken = PerformFullTransportBackupTask.this.backupManagerService.generateRandomIntegerToken();
            this.mPreflight = new SinglePackageBackupPreflight(transport, quota, this.mEphemeralToken);
            this.mQuota = quota;
            registerTask();
        }

        void registerTask() {
            synchronized (PerformFullTransportBackupTask.this.backupManagerService.getCurrentOpLock()) {
                PerformFullTransportBackupTask.this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 0));
            }
        }

        void unregisterTask() {
            synchronized (PerformFullTransportBackupTask.this.backupManagerService.getCurrentOpLock()) {
                PerformFullTransportBackupTask.this.backupManagerService.getCurrentOperations().remove(this.mCurrentOpToken);
            }
        }

        public void run() {
            this.mEngine = new FullBackupEngine(PerformFullTransportBackupTask.this.backupManagerService, new FileOutputStream(this.mOutput.getFileDescriptor()), this.mPreflight, this.mTarget, false, this, this.mQuota, this.mCurrentOpToken);
            try {
                if (!this.mIsCancelled) {
                    this.mPreflightResult = this.mEngine.preflightCheck();
                }
                this.mPreflightLatch.countDown();
                if (this.mPreflightResult == 0 && !this.mIsCancelled) {
                    this.mBackupResult = this.mEngine.backupOnePackage();
                }
                unregisterTask();
                this.mBackupLatch.countDown();
                try {
                    this.mOutput.close();
                } catch (IOException e) {
                    Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                }
            } catch (Exception e2) {
                try {
                    Slog.e(PerformFullTransportBackupTask.TAG, "Exception during full package backup of " + this.mTarget.packageName);
                    try {
                        this.mOutput.close();
                    } catch (IOException e3) {
                        Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                    }
                } finally {
                    unregisterTask();
                    this.mBackupLatch.countDown();
                    try {
                        this.mOutput.close();
                    } catch (IOException e4) {
                        Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                    }
                }
            } catch (Throwable th) {
                this.mPreflightLatch.countDown();
            }
        }

        public void sendQuotaExceeded(long backupDataBytes, long quotaBytes) {
            this.mEngine.sendQuotaExceeded(backupDataBytes, quotaBytes);
        }

        long getPreflightResultBlocking() {
            try {
                this.mPreflightLatch.await(RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003;
                }
                if (this.mPreflightResult == 0) {
                    return this.mPreflight.getExpectedSizeOrErrorCode();
                }
                return (long) this.mPreflightResult;
            } catch (InterruptedException e) {
                return -1003;
            }
        }

        int getBackupResultBlocking() {
            try {
                this.mBackupLatch.await(RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003;
                }
                return this.mBackupResult;
            } catch (InterruptedException e) {
                return -1003;
            }
        }

        public void execute() {
        }

        public void operationComplete(long result) {
        }

        public void handleCancel(boolean cancelAll) {
            Slog.w(PerformFullTransportBackupTask.TAG, "Full backup cancel of " + this.mTarget.packageName);
            PerformFullTransportBackupTask.this.mMonitor = BackupManagerMonitorUtils.monitorEvent(PerformFullTransportBackupTask.this.mMonitor, 4, PerformFullTransportBackupTask.this.mCurrentPackage, 2, null);
            this.mIsCancelled = true;
            PerformFullTransportBackupTask.this.backupManagerService.handleCancel(this.mEphemeralToken, cancelAll);
            PerformFullTransportBackupTask.this.backupManagerService.tearDownAgentAndKill(this.mTarget.applicationInfo);
            this.mPreflightLatch.countDown();
            this.mBackupLatch.countDown();
            PerformFullTransportBackupTask.this.backupManagerService.removeOperation(this.mCurrentOpToken);
        }
    }

    public PerformFullTransportBackupTask(RefactoredBackupManagerService backupManagerService, IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, IBackupManagerMonitor monitor, boolean userInitiated) {
        super(observer);
        this.backupManagerService = backupManagerService;
        this.mUpdateSchedule = updateSchedule;
        this.mLatch = latch;
        this.mJob = runningJob;
        this.mPackages = new ArrayList(whichPackages.length);
        this.mBackupObserver = backupObserver;
        this.mMonitor = monitor;
        this.mUserInitiated = userInitiated;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mBackupRunnerOpToken = backupManagerService.generateRandomIntegerToken();
        if (backupManagerService.isBackupOperationInProgress()) {
            Slog.d(TAG, "Skipping full backup. A backup is already in progress.");
            this.mCancelAll = true;
            return;
        }
        registerTask();
        for (String pkg : whichPackages) {
            try {
                PackageInfo info = backupManagerService.getPackageManager().getPackageInfo(pkg, 64);
                this.mCurrentPackage = info;
                if (!AppBackupUtils.appIsEligibleForBackup(info.applicationInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 9, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else if (!AppBackupUtils.appGetsFullBackup(info)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 10, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else if (AppBackupUtils.appIsStopped(info.applicationInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 11, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else {
                    this.mPackages.add(info);
                }
            } catch (NameNotFoundException e) {
                Slog.i(TAG, "Requested package " + pkg + " not found; ignoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 12, this.mCurrentPackage, 3, null);
            }
        }
    }

    private void registerTask() {
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            Slog.d(TAG, "backupmanager pftbt token=" + Integer.toHexString(this.mCurrentOpToken));
            this.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 2));
        }
    }

    public void unregisterTask() {
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    public void execute() {
    }

    public void handleCancel(boolean cancelAll) {
        synchronized (this.mCancelLock) {
            if (!cancelAll) {
                Slog.wtf(TAG, "Expected cancelAll to be true.");
            }
            if (this.mCancelAll) {
                Slog.d(TAG, "Ignoring duplicate cancel call.");
                return;
            }
            this.mCancelAll = true;
            if (this.mIsDoingBackup) {
                this.backupManagerService.handleCancel(this.mBackupRunnerOpToken, cancelAll);
                try {
                    this.mTransport.cancelFullBackup();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Error calling cancelFullBackup() on transport: " + e);
                }
            }
        }
    }

    public void operationComplete(long result) {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        ParcelFileDescriptor[] parcelFileDescriptorArr = null;
        ParcelFileDescriptor[] parcelFileDescriptorArr2 = null;
        long backoff = 0;
        int backupRunStatus = 0;
        int i;
        if (this.backupManagerService.isEnabled() && (this.backupManagerService.isProvisioned() ^ 1) == 0) {
            this.mTransport = this.backupManagerService.getTransportManager().getCurrentTransportBinder();
            if (this.mTransport == null) {
                Slog.w(TAG, "Transport not present; full data backup not performed");
                backupRunStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 15, this.mCurrentPackage, 1, null);
                if (this.mCancelAll) {
                    backupRunStatus = -2003;
                }
                Slog.i(TAG, "Full backup completed with status: " + backupRunStatus);
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus);
                cleanUpPipes(null);
                cleanUpPipes(null);
                unregisterTask();
                if (this.mJob != null) {
                    this.mJob.finishBackupPass();
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                    this.backupManagerService.setRunningFullBackupTask(null);
                }
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                    this.backupManagerService.scheduleNextFullBackupJob(0);
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
                return;
            }
            int N = this.mPackages.size();
            byte[] buffer = new byte[8192];
            i = 0;
            while (i < N) {
                PackageInfo currentPackage = (PackageInfo) this.mPackages.get(i);
                String packageName = currentPackage.packageName;
                Slog.i(TAG, "Initiating full-data transport backup of " + packageName + " token: " + this.mCurrentOpToken);
                EventLog.writeEvent(EventLogTags.FULL_BACKUP_PACKAGE, packageName);
                parcelFileDescriptorArr2 = ParcelFileDescriptor.createPipe();
                int flags = this.mUserInitiated ? 1 : 0;
                long quota = JobStatus.NO_LATEST_RUNTIME;
                synchronized (this.mCancelLock) {
                    if (!this.mCancelAll) {
                        int backupPackageStatus = this.mTransport.performFullBackup(currentPackage, parcelFileDescriptorArr2[0], flags);
                        if (backupPackageStatus == 0) {
                            quota = this.mTransport.getBackupQuota(currentPackage.packageName, true);
                            parcelFileDescriptorArr = ParcelFileDescriptor.createPipe();
                            this.mBackupRunner = new SinglePackageBackupRunner(parcelFileDescriptorArr[1], currentPackage, this.mTransport, quota, this.mBackupRunnerOpToken);
                            parcelFileDescriptorArr[1].close();
                            parcelFileDescriptorArr[1] = null;
                            this.mIsDoingBackup = true;
                        }
                    }
                }
            }
            if (this.mCancelAll) {
                backupRunStatus = -2003;
            }
            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus);
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus);
            cleanUpPipes(parcelFileDescriptorArr2);
            cleanUpPipes(parcelFileDescriptorArr);
            unregisterTask();
            if (this.mJob != null) {
                this.mJob.finishBackupPass();
            }
            synchronized (this.backupManagerService.getQueueLock()) {
                this.backupManagerService.setRunningFullBackupTask(null);
            }
            this.mLatch.countDown();
            if (this.mUpdateSchedule) {
                this.backupManagerService.scheduleNextFullBackupJob(backoff);
            }
            Slog.i(TAG, "Full data backup pass finished.");
            this.backupManagerService.getWakelock().release();
            return;
        }
        int monitoringEvent;
        Slog.i(TAG, "full backup requested but enabled=" + this.backupManagerService.isEnabled() + " provisioned=" + this.backupManagerService.isProvisioned() + "; ignoring");
        if (this.backupManagerService.isProvisioned()) {
            monitoringEvent = 13;
        } else {
            monitoringEvent = 14;
        }
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, monitoringEvent, null, 3, null);
        this.mUpdateSchedule = false;
        backupRunStatus = -2001;
        if (this.mCancelAll) {
            backupRunStatus = -2003;
        }
        Slog.i(TAG, "Full backup completed with status: " + backupRunStatus);
        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus);
        cleanUpPipes(null);
        cleanUpPipes(null);
        unregisterTask();
        if (this.mJob != null) {
            this.mJob.finishBackupPass();
        }
        synchronized (this.backupManagerService.getQueueLock()) {
            this.backupManagerService.setRunningFullBackupTask(null);
        }
        this.mLatch.countDown();
        if (this.mUpdateSchedule) {
            this.backupManagerService.scheduleNextFullBackupJob(0);
        }
        Slog.i(TAG, "Full data backup pass finished.");
        this.backupManagerService.getWakelock().release();
        return;
        i++;
    }

    void cleanUpPipes(ParcelFileDescriptor[] pipes) {
        if (pipes != null) {
            ParcelFileDescriptor fd;
            if (pipes[0] != null) {
                fd = pipes[0];
                pipes[0] = null;
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
            if (pipes[1] != null) {
                fd = pipes[1];
                pipes[1] = null;
                try {
                    fd.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
        }
    }
}
