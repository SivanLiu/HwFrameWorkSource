package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformFullTransportBackupTask extends FullBackupTask implements BackupRestoreTask {
    private static final String TAG = "PFTBT";
    private BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
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
    private final OnTaskFinishedListener mListener;
    IBackupManagerMonitor mMonitor;
    ArrayList<PackageInfo> mPackages;
    private final TransportClient mTransportClient;
    boolean mUpdateSchedule;
    boolean mUserInitiated;

    class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
        private final int mCurrentOpToken;
        final CountDownLatch mLatch = new CountDownLatch(1);
        final long mQuota;
        final AtomicLong mResult = new AtomicLong(-1003);
        final TransportClient mTransportClient;
        private final int mTransportFlags;

        SinglePackageBackupPreflight(TransportClient transportClient, long quota, int currentOpToken, int transportFlags) {
            this.mTransportClient = transportClient;
            this.mQuota = quota;
            this.mCurrentOpToken = currentOpToken;
            this.mTransportFlags = transportFlags;
        }

        public int preflightFullBackup(PackageInfo pkg, IBackupAgent agent) {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            int result;
            try {
                PerformFullTransportBackupTask.this.backupManagerService.prepareOperationTimeout(this.mCurrentOpToken, fullBackupAgentTimeoutMillis, this, 0);
                PerformFullTransportBackupTask.this.backupManagerService.addBackupTrace("preflighting");
                agent.doMeasureFullBackup(this.mQuota, this.mCurrentOpToken, PerformFullTransportBackupTask.this.backupManagerService.getBackupManagerBinder(), this.mTransportFlags);
                this.mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                long totalSize = this.mResult.get();
                if (totalSize < 0) {
                    return (int) totalSize;
                }
                result = this.mTransportClient.connectOrThrow("PFTBT$SPBP.preflightFullBackup()").checkFullBackupSize(totalSize);
                if (result == -1005) {
                    agent.doQuotaExceeded(totalSize, this.mQuota);
                }
                return result;
            } catch (Exception e) {
                String str = PerformFullTransportBackupTask.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception preflighting ");
                stringBuilder.append(pkg.packageName);
                stringBuilder.append(": ");
                stringBuilder.append(e.getMessage());
                Slog.w(str, stringBuilder.toString());
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
                this.mLatch.await(PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
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
        private final int mTransportFlags;
        final /* synthetic */ PerformFullTransportBackupTask this$0;

        SinglePackageBackupRunner(PerformFullTransportBackupTask this$0, ParcelFileDescriptor output, PackageInfo target, TransportClient transportClient, long quota, int currentOpToken, int transportFlags) throws IOException {
            PerformFullTransportBackupTask performFullTransportBackupTask = this$0;
            this.this$0 = performFullTransportBackupTask;
            this.mOutput = ParcelFileDescriptor.dup(output.getFileDescriptor());
            this.mTarget = target;
            this.mCurrentOpToken = currentOpToken;
            this.mEphemeralToken = performFullTransportBackupTask.backupManagerService.generateRandomIntegerToken();
            this.mPreflight = new SinglePackageBackupPreflight(transportClient, quota, this.mEphemeralToken, transportFlags);
            this.mQuota = quota;
            this.mTransportFlags = transportFlags;
            registerTask();
        }

        void registerTask() {
            synchronized (this.this$0.backupManagerService.getCurrentOpLock()) {
                this.this$0.backupManagerService.getCurrentOperations().put(this.mCurrentOpToken, new Operation(0, this, 0));
            }
        }

        void unregisterTask() {
            synchronized (this.this$0.backupManagerService.getCurrentOpLock()) {
                this.this$0.backupManagerService.getCurrentOperations().remove(this.mCurrentOpToken);
            }
        }

        public void run() {
            this.mEngine = new FullBackupEngine(this.this$0.backupManagerService, new FileOutputStream(this.mOutput.getFileDescriptor()), this.mPreflight, this.mTarget, false, this, this.mQuota, this.mCurrentOpToken, this.mTransportFlags);
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
                    String str = PerformFullTransportBackupTask.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception during full package backup of ");
                    stringBuilder.append(this.mTarget.packageName);
                    Slog.e(str, stringBuilder.toString());
                    this.mOutput.close();
                } finally {
                    unregisterTask();
                    this.mBackupLatch.countDown();
                    try {
                        this.mOutput.close();
                    } catch (IOException e3) {
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
                this.mPreflightLatch.await(this.this$0.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
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
                this.mBackupLatch.await(this.this$0.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), TimeUnit.MILLISECONDS);
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
            String str = PerformFullTransportBackupTask.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Full backup cancel of ");
            stringBuilder.append(this.mTarget.packageName);
            Slog.w(str, stringBuilder.toString());
            this.this$0.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.this$0.mMonitor, 4, this.this$0.mCurrentPackage, 2, null);
            this.mIsCancelled = true;
            this.this$0.backupManagerService.handleCancel(this.mEphemeralToken, cancelAll);
            this.this$0.backupManagerService.tearDownAgentAndKill(this.mTarget.applicationInfo);
            this.mPreflightLatch.countDown();
            this.mBackupLatch.countDown();
            this.this$0.backupManagerService.removeOperation(this.mCurrentOpToken);
        }
    }

    public static PerformFullTransportBackupTask newWithCurrentTransport(BackupManagerService backupManagerService, IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, IBackupManagerMonitor monitor, boolean userInitiated, String caller) {
        TransportManager transportManager = backupManagerService.getTransportManager();
        TransportClient transportClient = transportManager.getCurrentTransportClient(caller);
        return new PerformFullTransportBackupTask(backupManagerService, transportClient, observer, whichPackages, updateSchedule, runningJob, latch, backupObserver, monitor, new -$$Lambda$PerformFullTransportBackupTask$ymLoQLrsEpmGaMrcudrdAgsU1Zk(transportManager, transportClient), userInitiated);
    }

    public PerformFullTransportBackupTask(BackupManagerService backupManagerService, TransportClient transportClient, IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, boolean userInitiated) {
        String[] strArr = whichPackages;
        super(observer);
        this.backupManagerService = backupManagerService;
        this.mTransportClient = transportClient;
        this.mUpdateSchedule = updateSchedule;
        this.mLatch = latch;
        this.mJob = runningJob;
        this.mPackages = new ArrayList(strArr.length);
        this.mBackupObserver = backupObserver;
        this.mMonitor = monitor;
        this.mListener = listener != null ? listener : OnTaskFinishedListener.NOP;
        this.mUserInitiated = userInitiated;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mBackupRunnerOpToken = backupManagerService.generateRandomIntegerToken();
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        if (backupManagerService.isBackupOperationInProgress()) {
            Slog.d(TAG, "Skipping full backup. A backup is already in progress.");
            this.mCancelAll = true;
            return;
        }
        registerTask();
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            String pkg = strArr[i];
            try {
                PackageManager pm = backupManagerService.getPackageManager();
                PackageInfo info = pm.getPackageInfo(pkg, 134217728);
                this.mCurrentPackage = info;
                if (!AppBackupUtils.appIsEligibleForBackup(info.applicationInfo, pm)) {
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Requested package ");
                stringBuilder.append(pkg);
                stringBuilder.append(" not found; ignoring");
                Slog.i(str, stringBuilder.toString());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 12, this.mCurrentPackage, 3, null);
            }
            i++;
            strArr = whichPackages;
            IFullBackupRestoreObserver iFullBackupRestoreObserver = observer;
            BackupManagerService backupManagerService2 = backupManagerService;
            TransportClient transportClient2 = transportClient;
            boolean z = updateSchedule;
        }
    }

    private void registerTask() {
        synchronized (this.backupManagerService.getCurrentOpLock()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("backupmanager pftbt token=");
            stringBuilder.append(Integer.toHexString(this.mCurrentOpToken));
            Slog.d(str, stringBuilder.toString());
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
                try {
                    Slog.wtf(TAG, "Expected cancelAll to be true.");
                } catch (RemoteException | TransportNotAvailableException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error calling cancelFullBackup() on transport: ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                } catch (Throwable th) {
                }
            }
            if (this.mCancelAll) {
                Slog.d(TAG, "Ignoring duplicate cancel call.");
                return;
            }
            this.mCancelAll = true;
            if (this.mIsDoingBackup) {
                this.backupManagerService.handleCancel(this.mBackupRunnerOpToken, cancelAll);
                this.mTransportClient.getConnectedTransport("PFTBT.handleCancel()").cancelFullBackup();
            }
        }
    }

    public void operationComplete(long result) {
    }

    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Removed duplicated region for block: B:335:0x077c  */
    /* JADX WARNING: Removed duplicated region for block: B:336:0x0780  */
    /* JADX WARNING: Removed duplicated region for block: B:339:0x07a9  */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x07b5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:347:0x07cc  */
    /* JADX WARNING: Removed duplicated region for block: B:358:0x07ef  */
    /* JADX WARNING: Removed duplicated region for block: B:361:0x081b  */
    /* JADX WARNING: Removed duplicated region for block: B:364:0x0827 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:369:0x083e  */
    /* JADX WARNING: Missing block: B:113:0x029d, code skipped:
            r37 = r2;
            r36 = r3;
            r2 = r34 + ((long) r5);
     */
    /* JADX WARNING: Missing block: B:116:0x02a6, code skipped:
            if (r10.mBackupObserver == null) goto L_0x02b9;
     */
    /* JADX WARNING: Missing block: B:118:0x02aa, code skipped:
            if (r14 <= 0) goto L_0x02b9;
     */
    /* JADX WARNING: Missing block: B:119:0x02ac, code skipped:
            r12 = r30;
            com.android.server.backup.utils.BackupObserverUtils.sendBackupOnUpdate(r10.mBackupObserver, r12, new android.app.backup.BackupProgress(r14, r2));
     */
    /* JADX WARNING: Missing block: B:120:0x02b9, code skipped:
            r12 = r30;
     */
    /* JADX WARNING: Missing block: B:121:0x02bb, code skipped:
            r38 = r4;
            r3 = r2;
            r2 = r37;
     */
    /* JADX WARNING: Missing block: B:158:0x035e, code skipped:
            if (r39 != 0) goto L_0x0364;
     */
    /* JADX WARNING: Missing block: B:159:0x0360, code skipped:
            if (r2 == 0) goto L_0x0364;
     */
    /* JADX WARNING: Missing block: B:160:0x0362, code skipped:
            r0 = r2;
     */
    /* JADX WARNING: Missing block: B:161:0x0364, code skipped:
            r0 = r39;
     */
    /* JADX WARNING: Missing block: B:162:0x0366, code skipped:
            if (r0 == 0) goto L_0x0386;
     */
    /* JADX WARNING: Missing block: B:164:?, code skipped:
            r5 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("Error ");
            r8.append(r0);
            r8.append(" backing up ");
            r8.append(r12);
            android.util.Slog.e(r5, r8.toString());
     */
    /* JADX WARNING: Missing block: B:166:0x038a, code skipped:
            r42 = r9.requestFullBackupTime();
     */
    /* JADX WARNING: Missing block: B:168:?, code skipped:
            r5 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("Transport suggested backoff=");
     */
    /* JADX WARNING: Missing block: B:169:0x0398, code skipped:
            r46 = r2;
            r44 = r3;
            r2 = r42;
     */
    /* JADX WARNING: Missing block: B:171:?, code skipped:
            r8.append(r2);
            android.util.Slog.i(r5, r8.toString());
     */
    /* JADX WARNING: Missing block: B:172:0x03a8, code skipped:
            r3 = r2;
            r2 = r0;
     */
    /* JADX WARNING: Missing block: B:173:0x03ab, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:174:0x03ac, code skipped:
            r3 = r2;
     */
    /* JADX WARNING: Missing block: B:175:0x03af, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:176:0x03b0, code skipped:
            r3 = r2;
     */
    /* JADX WARNING: Missing block: B:177:0x03b3, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:178:0x03b4, code skipped:
            r3 = r42;
     */
    /* JADX WARNING: Missing block: B:179:0x03b9, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:180:0x03ba, code skipped:
            r3 = r42;
            r2 = r13;
     */
    /* JADX WARNING: Missing block: B:210:0x04bc, code skipped:
            com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r10.mBackupObserver, r12, com.android.server.job.JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            r0 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("Transport failed; aborting backup: ");
            r8.append(r2);
            android.util.Slog.w(r0, r8.toString());
            android.util.EventLog.writeEvent(com.android.server.EventLogTags.FULL_BACKUP_TRANSPORT_FAILURE, new java.lang.Object[0]);
     */
    /* JADX WARNING: Missing block: B:211:0x04e1, code skipped:
            r8 = com.android.server.job.JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
     */
    /* JADX WARNING: Missing block: B:213:?, code skipped:
            r10.backupManagerService.tearDownAgentAndKill(r5.applicationInfo);
     */
    /* JADX WARNING: Missing block: B:215:0x04ec, code skipped:
            if (r10.mCancelAll == false) goto L_0x04f0;
     */
    /* JADX WARNING: Missing block: B:216:0x04ee, code skipped:
            r8 = -2003;
     */
    /* JADX WARNING: Missing block: B:217:0x04f0, code skipped:
            r11 = r8;
            r0 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("Full backup completed with status: ");
            r8.append(r11);
            android.util.Slog.i(r0, r8.toString());
            com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r10.mBackupObserver, r11);
            cleanUpPipes(r13);
            cleanUpPipes(r1);
            unregisterTask();
     */
    /* JADX WARNING: Missing block: B:218:0x0517, code skipped:
            if (r10.mJob == null) goto L_0x051e;
     */
    /* JADX WARNING: Missing block: B:219:0x0519, code skipped:
            r10.mJob.finishBackupPass();
     */
    /* JADX WARNING: Missing block: B:220:0x051e, code skipped:
            r14 = r10.backupManagerService.getQueueLock();
     */
    /* JADX WARNING: Missing block: B:221:0x0524, code skipped:
            monitor-enter(r14);
     */
    /* JADX WARNING: Missing block: B:223:?, code skipped:
            r10.backupManagerService.setRunningFullBackupTask(null);
     */
    /* JADX WARNING: Missing block: B:224:0x052b, code skipped:
            monitor-exit(r14);
     */
    /* JADX WARNING: Missing block: B:225:0x052c, code skipped:
            r10.mListener.onFinished("PFTBT.run()");
            r10.mLatch.countDown();
     */
    /* JADX WARNING: Missing block: B:226:0x053a, code skipped:
            if (r10.mUpdateSchedule == false) goto L_0x0541;
     */
    /* JADX WARNING: Missing block: B:227:0x053c, code skipped:
            r10.backupManagerService.scheduleNextFullBackupJob(r3);
     */
    /* JADX WARNING: Missing block: B:228:0x0541, code skipped:
            android.util.Slog.i(TAG, "Full data backup pass finished.");
            r10.backupManagerService.getWakelock().release();
     */
    /* JADX WARNING: Missing block: B:229:0x0551, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:234:0x0555, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:235:0x0556, code skipped:
            r27 = com.android.server.job.JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
     */
    /* JADX WARNING: Missing block: B:236:0x055a, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:237:0x055b, code skipped:
            r27 = com.android.server.job.JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        int backupRunStatus;
        Exception e;
        ParcelFileDescriptor[] parcelFileDescriptorArr;
        Throwable th;
        int backupRunStatus2;
        String str;
        long backoff;
        Object obj;
        Object obj2;
        boolean z;
        int flags;
        long backoff2;
        byte[] buffer;
        PackageInfo currentPackage;
        int i;
        ParcelFileDescriptor[] enginePipes;
        byte[] bArr;
        long j;
        PackageInfo currentPackage2;
        String packageName;
        int i2;
        long j2;
        ParcelFileDescriptor[] enginePipes2 = null;
        ParcelFileDescriptor[] transportPipes = null;
        long backoff3 = 0;
        int buffer2 = 0;
        int backupRunStatus3 = 0;
        SinglePackageBackupRunner packageName2 = null;
        int backupRunStatus4;
        String str2;
        StringBuilder stringBuilder;
        try {
            int i3;
            StringBuilder stringBuilder2;
            if (!this.backupManagerService.isEnabled()) {
                backupRunStatus = backupRunStatus3;
            } else if (this.backupManagerService.isProvisioned()) {
                IBackupTransport transport = this.mTransportClient.connect("PFTBT.run()");
                if (transport == null) {
                    try {
                        Slog.w(TAG, "Transport not present; full data backup not performed");
                        backupRunStatus3 = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 15, this.mCurrentPackage, 1, null);
                        if (this.mCancelAll) {
                            backupRunStatus3 = -2003;
                        }
                        backupRunStatus4 = backupRunStatus3;
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Full backup completed with status: ");
                        stringBuilder.append(backupRunStatus4);
                        Slog.i(str2, stringBuilder.toString());
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                        cleanUpPipes(null);
                        cleanUpPipes(null);
                        unregisterTask();
                        if (this.mJob != null) {
                            this.mJob.finishBackupPass();
                        }
                        synchronized (this.backupManagerService.getQueueLock()) {
                            this.backupManagerService.setRunningFullBackupTask(null);
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            this.backupManagerService.scheduleNextFullBackupJob(0);
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.backupManagerService.getWakelock().release();
                        return;
                    } catch (Exception e2) {
                        e = e2;
                        backupRunStatus = backupRunStatus3;
                        try {
                            Slog.w(TAG, "Exception trying full transport backup", e);
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                            if (this.mCancelAll) {
                            }
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Full backup completed with status: ");
                            stringBuilder.append(backupRunStatus4);
                            Slog.i(str2, stringBuilder.toString());
                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                            cleanUpPipes(transportPipes);
                            cleanUpPipes(enginePipes2);
                            unregisterTask();
                            if (this.mJob != null) {
                            }
                            synchronized (this.backupManagerService.getQueueLock()) {
                            }
                            this.mListener.onFinished("PFTBT.run()");
                            this.mLatch.countDown();
                            if (this.mUpdateSchedule) {
                            }
                            Slog.i(TAG, "Full data backup pass finished.");
                            this.backupManagerService.getWakelock().release();
                            parcelFileDescriptorArr = transportPipes;
                        } catch (Throwable th2) {
                            th = th2;
                            parcelFileDescriptorArr = transportPipes;
                            backupRunStatus = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                            if (this.mCancelAll) {
                                backupRunStatus = -2003;
                            }
                            backupRunStatus2 = backupRunStatus;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Full backup completed with status: ");
                            stringBuilder.append(backupRunStatus2);
                            Slog.i(str, stringBuilder.toString());
                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                            cleanUpPipes(parcelFileDescriptorArr);
                            cleanUpPipes(enginePipes2);
                            unregisterTask();
                            if (this.mJob != null) {
                                this.mJob.finishBackupPass();
                            }
                            synchronized (this.backupManagerService.getQueueLock()) {
                                this.backupManagerService.setRunningFullBackupTask(null);
                            }
                            this.mListener.onFinished("PFTBT.run()");
                            this.mLatch.countDown();
                            if (this.mUpdateSchedule) {
                                this.backupManagerService.scheduleNextFullBackupJob(backoff3);
                            }
                            Slog.i(TAG, "Full data backup pass finished.");
                            this.backupManagerService.getWakelock().release();
                            throw th;
                        }
                    }
                }
                long backoff4;
                BackupManagerService backupManagerService;
                int N = this.mPackages.size();
                byte[] buffer3 = new byte[8192];
                i3 = 0;
                while (true) {
                    backoff4 = i3;
                    if (backoff4 >= N) {
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        parcelFileDescriptorArr = transportPipes;
                        break;
                    }
                    PackageInfo currentPackage3;
                    String packageName3;
                    try {
                        this.mBackupRunner = packageName2;
                        currentPackage3 = (PackageInfo) this.mPackages.get(backoff4);
                        String packageName4 = currentPackage3.packageName;
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Initiating full-data transport backup of ");
                        packageName3 = packageName4;
                        stringBuilder3.append(packageName3);
                        stringBuilder3.append(" token: ");
                        stringBuilder3.append(this.mCurrentOpToken);
                        Slog.i(str2, stringBuilder3.toString());
                        EventLog.writeEvent(EventLogTags.FULL_BACKUP_PACKAGE, packageName3);
                        parcelFileDescriptorArr = ParcelFileDescriptor.createPipe();
                    } catch (Exception e3) {
                        e = e3;
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        Slog.w(TAG, "Exception trying full transport backup", e);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                        backupRunStatus4 = this.mCancelAll ? -2003 : JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Full backup completed with status: ");
                        stringBuilder.append(backupRunStatus4);
                        Slog.i(str2, stringBuilder.toString());
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                        cleanUpPipes(transportPipes);
                        cleanUpPipes(enginePipes2);
                        unregisterTask();
                        if (this.mJob != null) {
                            this.mJob.finishBackupPass();
                        }
                        synchronized (this.backupManagerService.getQueueLock()) {
                            this.backupManagerService.setRunningFullBackupTask(null);
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            this.backupManagerService.scheduleNextFullBackupJob(backoff3);
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.backupManagerService.getWakelock().release();
                        parcelFileDescriptorArr = transportPipes;
                    } catch (Throwable th3) {
                        th = th3;
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        parcelFileDescriptorArr = transportPipes;
                        if (this.mCancelAll) {
                        }
                        backupRunStatus2 = backupRunStatus;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Full backup completed with status: ");
                        stringBuilder.append(backupRunStatus2);
                        Slog.i(str, stringBuilder.toString());
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                        cleanUpPipes(parcelFileDescriptorArr);
                        cleanUpPipes(enginePipes2);
                        unregisterTask();
                        if (this.mJob != null) {
                        }
                        synchronized (this.backupManagerService.getQueueLock()) {
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.backupManagerService.getWakelock().release();
                        throw th;
                    }
                    try {
                        boolean flags2 = this.mUserInitiated;
                        Object obj3 = this.mCancelLock;
                        synchronized (obj3) {
                            byte[] bArr2;
                            int i4;
                            try {
                                if (this.mCancelAll) {
                                    try {
                                        break;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        obj = obj3;
                                        backoff = backoff3;
                                        bArr2 = buffer3;
                                        obj2 = backoff4;
                                        buffer3 = currentPackage3;
                                        i4 = N;
                                        z = flags2;
                                        backupRunStatus = backupRunStatus3;
                                        backupRunStatus3 = packageName3;
                                        flags = transport;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        }
                                        throw th;
                                    }
                                } else {
                                    int i5;
                                    int packageName5;
                                    IBackupTransport iBackupTransport;
                                    PackageInfo packageInfo;
                                    String str3;
                                    i3 = transport.performFullBackup(currentPackage3, parcelFileDescriptorArr[buffer2], flags2);
                                    if (i3 == 0) {
                                        int N2;
                                        try {
                                            backoff2 = backoff3;
                                            try {
                                                buffer = backoff4;
                                                currentPackage = currentPackage3;
                                                i = transport.getBackupQuota(currentPackage3.packageName, 1);
                                                try {
                                                    enginePipes = ParcelFileDescriptor.createPipe();
                                                    try {
                                                        N2 = N;
                                                    } catch (Throwable th6) {
                                                        th = th6;
                                                        obj = obj3;
                                                        z = flags2;
                                                        bArr = buffer;
                                                        backupRunStatus = backupRunStatus3;
                                                        backoff = backoff2;
                                                        backoff2 = N;
                                                        bArr2 = buffer3;
                                                        j = i;
                                                        backupRunStatus3 = packageName3;
                                                        flags = transport;
                                                        enginePipes2 = enginePipes;
                                                        buffer3 = currentPackage;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    obj = obj3;
                                                    z = flags2;
                                                    bArr = buffer;
                                                    backupRunStatus = backupRunStatus3;
                                                    backoff = backoff2;
                                                    backoff2 = N;
                                                    j = i;
                                                    backupRunStatus3 = packageName3;
                                                    flags = transport;
                                                    buffer3 = currentPackage;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            } catch (Throwable th8) {
                                                th = th8;
                                                obj = obj3;
                                                obj2 = backoff4;
                                                z = flags2;
                                                backupRunStatus = backupRunStatus3;
                                                backoff = backoff2;
                                                bArr2 = buffer3;
                                                buffer3 = currentPackage3;
                                                backupRunStatus3 = packageName3;
                                                flags = transport;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        } catch (Throwable th9) {
                                            th = th9;
                                            obj = obj3;
                                            backoff = backoff3;
                                            obj2 = backoff4;
                                            i4 = N;
                                            z = flags2;
                                            backupRunStatus = backupRunStatus3;
                                            bArr2 = buffer3;
                                            buffer3 = currentPackage3;
                                            backupRunStatus3 = packageName3;
                                            flags = transport;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                        try {
                                            SinglePackageBackupRunner singlePackageBackupRunner = singlePackageBackupRunner;
                                            obj = obj3;
                                            backupRunStatus = backupRunStatus3;
                                            backoff = backoff2;
                                            SinglePackageBackupRunner singlePackageBackupRunner2 = singlePackageBackupRunner;
                                            currentPackage2 = currentPackage;
                                            i5 = buffer;
                                            buffer = buffer3;
                                            i4 = N2;
                                            packageName = packageName3;
                                            packageName5 = 1;
                                            try {
                                                singlePackageBackupRunner = new SinglePackageBackupRunner(this, enginePipes[1], currentPackage, this.mTransportClient, i, this.mBackupRunnerOpToken, transport.getTransportFlags());
                                                this.mBackupRunner = singlePackageBackupRunner2;
                                                enginePipes[packageName5].close();
                                                enginePipes[packageName5] = null;
                                                this.mIsDoingBackup = packageName5;
                                                enginePipes2 = enginePipes;
                                            } catch (Throwable th10) {
                                                th = th10;
                                                j = i;
                                                bArr2 = buffer;
                                                iBackupTransport = transport;
                                                enginePipes2 = enginePipes;
                                                packageInfo = currentPackage2;
                                                str3 = packageName;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        } catch (Throwable th11) {
                                            th = th11;
                                            obj = obj3;
                                            z = flags2;
                                            bArr = buffer;
                                            backupRunStatus = backupRunStatus3;
                                            backoff = backoff2;
                                            backoff2 = N2;
                                            bArr2 = buffer3;
                                            j = i;
                                            backupRunStatus3 = packageName3;
                                            flags = transport;
                                            enginePipes2 = enginePipes;
                                            buffer3 = currentPackage;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    } else {
                                        obj = obj3;
                                        backoff = backoff3;
                                        buffer = buffer3;
                                        i5 = backoff4;
                                        currentPackage2 = currentPackage3;
                                        i4 = N;
                                        z = flags2;
                                        backupRunStatus = backupRunStatus3;
                                        packageName = packageName3;
                                        packageName5 = 1;
                                        i = JobStatus.NO_LATEST_RUNTIME;
                                    }
                                    try {
                                        StringBuilder stringBuilder4;
                                        if (i3 == 0) {
                                            try {
                                                int backupPackageStatus;
                                                parcelFileDescriptorArr[0].close();
                                                parcelFileDescriptorArr[0] = null;
                                                new Thread(this.mBackupRunner, "package-backup-bridge").start();
                                                FileInputStream in = new FileInputStream(enginePipes2[0].getFileDescriptor());
                                                FileOutputStream out = new FileOutputStream(parcelFileDescriptorArr[packageName5].getFileDescriptor());
                                                IBackupTransport transport2 = transport;
                                                long preflightResult = this.mBackupRunner.getPreflightResultBlocking();
                                                long totalRead;
                                                FileInputStream fileInputStream;
                                                FileOutputStream fileOutputStream;
                                                if (preflightResult < 0) {
                                                    totalRead = 0;
                                                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 16, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) 0, "android.app.backup.extra.LOG_PREFLIGHT_ERROR", preflightResult));
                                                    backupPackageStatus = (int) preflightResult;
                                                    fileInputStream = in;
                                                    fileOutputStream = out;
                                                    bArr2 = buffer;
                                                    str3 = packageName;
                                                    iBackupTransport = transport2;
                                                    backoff3 = totalRead;
                                                } else {
                                                    totalRead = 0;
                                                    backupRunStatus2 = i3;
                                                    while (true) {
                                                        backupRunStatus4 = in.read(buffer);
                                                        if (backupRunStatus4 > 0) {
                                                            out.write(buffer, 0, backupRunStatus4);
                                                            synchronized (this.mCancelLock) {
                                                                try {
                                                                    if (this.mCancelAll) {
                                                                        iBackupTransport = transport2;
                                                                    } else {
                                                                        iBackupTransport = transport2;
                                                                        try {
                                                                            backupRunStatus2 = iBackupTransport.sendBackupData(backupRunStatus4);
                                                                        } catch (Throwable th12) {
                                                                            th = th12;
                                                                            fileInputStream = in;
                                                                            str3 = packageName;
                                                                            while (true) {
                                                                                try {
                                                                                    break;
                                                                                } catch (Throwable th13) {
                                                                                    th = th13;
                                                                                }
                                                                            }
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    try {
                                                                    } catch (Throwable th14) {
                                                                        th = th14;
                                                                        int i6 = backupRunStatus2;
                                                                        fileInputStream = in;
                                                                        str3 = packageName;
                                                                        while (true) {
                                                                            break;
                                                                        }
                                                                        throw th;
                                                                    }
                                                                } catch (Throwable th15) {
                                                                    th = th15;
                                                                    fileInputStream = in;
                                                                    str3 = packageName;
                                                                    iBackupTransport = transport2;
                                                                    while (true) {
                                                                        break;
                                                                    }
                                                                    throw th;
                                                                }
                                                            }
                                                        }
                                                        fileInputStream = in;
                                                        str3 = packageName;
                                                        iBackupTransport = transport2;
                                                        fileOutputStream = out;
                                                        backoff3 = totalRead;
                                                        if (backupRunStatus4 <= 0) {
                                                            break;
                                                        } else if (backupRunStatus2 != 0) {
                                                            break;
                                                        } else {
                                                            totalRead = backoff3;
                                                            i3 = backupRunStatus4;
                                                            transport2 = iBackupTransport;
                                                            packageName = str3;
                                                            in = fileInputStream;
                                                            out = fileOutputStream;
                                                        }
                                                    }
                                                    if (backupRunStatus2 == -1005) {
                                                        str2 = TAG;
                                                        stringBuilder4 = new StringBuilder();
                                                        backupPackageStatus = backupRunStatus2;
                                                        stringBuilder4.append("Package hit quota limit in-flight ");
                                                        stringBuilder4.append(str3);
                                                        stringBuilder4.append(": ");
                                                        stringBuilder4.append(backoff3);
                                                        stringBuilder4.append(" of ");
                                                        stringBuilder4.append(i);
                                                        Slog.w(str2, stringBuilder4.toString());
                                                        bArr2 = buffer;
                                                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 18, this.mCurrentPackage, 1, 0);
                                                        this.mBackupRunner.sendQuotaExceeded(backoff3, i);
                                                    } else {
                                                        backupPackageStatus = backupRunStatus2;
                                                        bArr2 = buffer;
                                                    }
                                                }
                                                backupRunStatus2 = this.mBackupRunner.getBackupResultBlocking();
                                                synchronized (this.mCancelLock) {
                                                    try {
                                                        this.mIsDoingBackup = false;
                                                        if (!this.mCancelAll) {
                                                            if (backupRunStatus2 == 0) {
                                                                try {
                                                                    i3 = iBackupTransport.finishBackup();
                                                                    if (backupPackageStatus == 0) {
                                                                        backupPackageStatus = i3;
                                                                    }
                                                                } catch (Throwable th16) {
                                                                    th = th16;
                                                                    i2 = backupRunStatus2;
                                                                    j2 = backoff3;
                                                                    while (true) {
                                                                        try {
                                                                            break;
                                                                        } catch (Throwable th17) {
                                                                            th = th17;
                                                                        }
                                                                    }
                                                                    throw th;
                                                                }
                                                            } else {
                                                                iBackupTransport.cancelFullBackup();
                                                            }
                                                        }
                                                    } catch (Throwable th18) {
                                                        th = th18;
                                                        i2 = backupRunStatus2;
                                                        j2 = backoff3;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } catch (Exception e4) {
                                                e = e4;
                                                transportPipes = parcelFileDescriptorArr;
                                                backoff3 = backoff;
                                            } catch (Throwable th19) {
                                                th = th19;
                                                backoff3 = backoff;
                                                if (this.mCancelAll) {
                                                }
                                                backupRunStatus2 = backupRunStatus;
                                                str = TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Full backup completed with status: ");
                                                stringBuilder.append(backupRunStatus2);
                                                Slog.i(str, stringBuilder.toString());
                                                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                                                cleanUpPipes(parcelFileDescriptorArr);
                                                cleanUpPipes(enginePipes2);
                                                unregisterTask();
                                                if (this.mJob != null) {
                                                }
                                                synchronized (this.backupManagerService.getQueueLock()) {
                                                }
                                                this.mListener.onFinished("PFTBT.run()");
                                                this.mLatch.countDown();
                                                if (this.mUpdateSchedule) {
                                                }
                                                Slog.i(TAG, "Full data backup pass finished.");
                                                this.backupManagerService.getWakelock().release();
                                                throw th;
                                            }
                                        }
                                        bArr2 = buffer;
                                        iBackupTransport = transport;
                                        str3 = packageName;
                                        backupRunStatus2 = i3;
                                        backoff3 = backoff;
                                        try {
                                            if (this.mUpdateSchedule) {
                                                this.backupManagerService.enqueueFullBackup(str3, System.currentTimeMillis());
                                            }
                                            if (backupRunStatus2 == JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS) {
                                                BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str3, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                                                str2 = TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Transport rejected backup of ");
                                                stringBuilder2.append(str3);
                                                stringBuilder2.append(", skipping");
                                                Slog.i(str2, stringBuilder2.toString());
                                                Object[] objArr = new Object[2];
                                                objArr[0] = str3;
                                                buffer2 = 1;
                                                objArr[1] = "transport rejected";
                                                EventLog.writeEvent(EventLogTags.FULL_BACKUP_AGENT_FAILURE, objArr);
                                                if (this.mBackupRunner != null) {
                                                    packageInfo = currentPackage2;
                                                    this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                } else {
                                                    packageInfo = currentPackage2;
                                                }
                                            } else {
                                                packageInfo = currentPackage2;
                                                buffer2 = 1;
                                                if (backupRunStatus2 == -1005) {
                                                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str3, -1005);
                                                    str2 = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("Transport quota exceeded for package: ");
                                                    stringBuilder4.append(str3);
                                                    Slog.i(str2, stringBuilder4.toString());
                                                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_QUOTA_EXCEEDED, str3);
                                                    this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                } else if (backupRunStatus2 == -1003) {
                                                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str3, -1003);
                                                    str2 = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("Application failure for package: ");
                                                    stringBuilder4.append(str3);
                                                    Slog.w(str2, stringBuilder4.toString());
                                                    EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, str3);
                                                    this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                } else if (backupRunStatus2 == -2003) {
                                                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str3, -2003);
                                                    str2 = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("Backup cancelled. package=");
                                                    stringBuilder4.append(str3);
                                                    stringBuilder4.append(", cancelAll=");
                                                    stringBuilder4.append(this.mCancelAll);
                                                    Slog.w(str2, stringBuilder4.toString());
                                                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_CANCELLED, str3);
                                                    this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
                                                } else if (backupRunStatus2 != 0) {
                                                    break;
                                                } else {
                                                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, str3, 0);
                                                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_SUCCESS, str3);
                                                    this.backupManagerService.logBackupComplete(str3);
                                                }
                                            }
                                            cleanUpPipes(parcelFileDescriptorArr);
                                            cleanUpPipes(enginePipes2);
                                            if (packageInfo.applicationInfo != null) {
                                                str2 = TAG;
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("Unbinding agent in ");
                                                stringBuilder4.append(str3);
                                                Slog.i(str2, stringBuilder4.toString());
                                                backupManagerService = this.backupManagerService;
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("unbinding ");
                                                stringBuilder4.append(str3);
                                                backupManagerService.addBackupTrace(stringBuilder4.toString());
                                                try {
                                                    this.backupManagerService.getActivityManager().unbindBackupAgent(packageInfo.applicationInfo);
                                                } catch (RemoteException e5) {
                                                }
                                            }
                                            i3 = i5 + 1;
                                            transport = iBackupTransport;
                                            iBackupTransport = buffer2;
                                            transportPipes = parcelFileDescriptorArr;
                                            N = i4;
                                            backupRunStatus3 = backupRunStatus;
                                            buffer3 = bArr2;
                                            buffer2 = 0;
                                            packageName2 = null;
                                        } catch (Exception e6) {
                                            e = e6;
                                            transportPipes = parcelFileDescriptorArr;
                                            Slog.w(TAG, "Exception trying full transport backup", e);
                                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                                            if (this.mCancelAll) {
                                            }
                                            str2 = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Full backup completed with status: ");
                                            stringBuilder.append(backupRunStatus4);
                                            Slog.i(str2, stringBuilder.toString());
                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                            cleanUpPipes(transportPipes);
                                            cleanUpPipes(enginePipes2);
                                            unregisterTask();
                                            if (this.mJob != null) {
                                            }
                                            synchronized (this.backupManagerService.getQueueLock()) {
                                            }
                                            this.mListener.onFinished("PFTBT.run()");
                                            this.mLatch.countDown();
                                            if (this.mUpdateSchedule) {
                                            }
                                            Slog.i(TAG, "Full data backup pass finished.");
                                            this.backupManagerService.getWakelock().release();
                                            parcelFileDescriptorArr = transportPipes;
                                        } catch (Throwable th20) {
                                            th = th20;
                                            if (this.mCancelAll) {
                                            }
                                            backupRunStatus2 = backupRunStatus;
                                            str = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Full backup completed with status: ");
                                            stringBuilder.append(backupRunStatus2);
                                            Slog.i(str, stringBuilder.toString());
                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                                            cleanUpPipes(parcelFileDescriptorArr);
                                            cleanUpPipes(enginePipes2);
                                            unregisterTask();
                                            if (this.mJob != null) {
                                            }
                                            synchronized (this.backupManagerService.getQueueLock()) {
                                            }
                                            this.mListener.onFinished("PFTBT.run()");
                                            this.mLatch.countDown();
                                            if (this.mUpdateSchedule) {
                                            }
                                            Slog.i(TAG, "Full data backup pass finished.");
                                            this.backupManagerService.getWakelock().release();
                                            throw th;
                                        }
                                    } catch (Throwable th21) {
                                        th = th21;
                                        bArr2 = buffer;
                                        flags = transport;
                                        buffer3 = currentPackage2;
                                        backupRunStatus3 = packageName;
                                        j = i;
                                        while (true) {
                                            break;
                                        }
                                        throw th;
                                    }
                                }
                            } catch (Throwable th22) {
                                th = th22;
                                obj = obj3;
                                backoff = backoff3;
                                bArr2 = buffer3;
                                obj2 = backoff4;
                                i4 = N;
                                z = flags2;
                                backupRunStatus = backupRunStatus3;
                                backupRunStatus3 = packageName3;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        break;
                    } catch (Exception e7) {
                        e = e7;
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        transportPipes = parcelFileDescriptorArr;
                        Slog.w(TAG, "Exception trying full transport backup", e);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                        if (this.mCancelAll) {
                        }
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Full backup completed with status: ");
                        stringBuilder.append(backupRunStatus4);
                        Slog.i(str2, stringBuilder.toString());
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                        cleanUpPipes(transportPipes);
                        cleanUpPipes(enginePipes2);
                        unregisterTask();
                        if (this.mJob != null) {
                        }
                        synchronized (this.backupManagerService.getQueueLock()) {
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.backupManagerService.getWakelock().release();
                        parcelFileDescriptorArr = transportPipes;
                    } catch (Throwable th23) {
                        th = th23;
                        backoff = backoff3;
                        backupRunStatus = backupRunStatus3;
                        if (this.mCancelAll) {
                        }
                        backupRunStatus2 = backupRunStatus;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Full backup completed with status: ");
                        stringBuilder.append(backupRunStatus2);
                        Slog.i(str, stringBuilder.toString());
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                        cleanUpPipes(parcelFileDescriptorArr);
                        cleanUpPipes(enginePipes2);
                        unregisterTask();
                        if (this.mJob != null) {
                        }
                        synchronized (this.backupManagerService.getQueueLock()) {
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.backupManagerService.getWakelock().release();
                        throw th;
                    }
                }
                if (this.mCancelAll) {
                    backupRunStatus3 = -2003;
                } else {
                    backupRunStatus3 = backupRunStatus;
                }
                str2 = TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Full backup completed with status: ");
                stringBuilder5.append(backupRunStatus3);
                Slog.i(str2, stringBuilder5.toString());
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus3);
                cleanUpPipes(parcelFileDescriptorArr);
                cleanUpPipes(enginePipes2);
                unregisterTask();
                if (this.mJob != null) {
                    this.mJob.finishBackupPass();
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                    try {
                        this.backupManagerService.setRunningFullBackupTask(null);
                    } finally {
                        backoff4 = backoff;
                        while (true) {
                        }
                    }
                }
                this.mListener.onFinished("PFTBT.run()");
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                    backupManagerService = this.backupManagerService;
                } else {
                    backoff4 = backoff;
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
                backoff3 = backoff4;
                backupRunStatus4 = backupRunStatus3;
            } else {
                backupRunStatus = backupRunStatus3;
            }
            try {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("full backup requested but enabled=");
                stringBuilder2.append(this.backupManagerService.isEnabled());
                stringBuilder2.append(" provisioned=");
                stringBuilder2.append(this.backupManagerService.isProvisioned());
                stringBuilder2.append("; ignoring");
                Slog.i(str2, stringBuilder2.toString());
                if (this.backupManagerService.isProvisioned()) {
                    i3 = 13;
                } else {
                    i3 = 14;
                }
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, i3, null, 3, null);
                this.mUpdateSchedule = false;
                i3 = -2001;
                if (this.mCancelAll) {
                    i3 = -2003;
                }
                i = i3;
                str2 = TAG;
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("Full backup completed with status: ");
                stringBuilder6.append(i);
                Slog.i(str2, stringBuilder6.toString());
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, i);
                cleanUpPipes(null);
                cleanUpPipes(null);
                unregisterTask();
                if (this.mJob != null) {
                    this.mJob.finishBackupPass();
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                    this.backupManagerService.setRunningFullBackupTask(null);
                }
                this.mListener.onFinished("PFTBT.run()");
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                    this.backupManagerService.scheduleNextFullBackupJob(0);
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
            } catch (Exception e8) {
                e = e8;
                Slog.w(TAG, "Exception trying full transport backup", e);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
                if (this.mCancelAll) {
                }
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Full backup completed with status: ");
                stringBuilder.append(backupRunStatus4);
                Slog.i(str2, stringBuilder.toString());
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                cleanUpPipes(transportPipes);
                cleanUpPipes(enginePipes2);
                unregisterTask();
                if (this.mJob != null) {
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                }
                this.mListener.onFinished("PFTBT.run()");
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
                parcelFileDescriptorArr = transportPipes;
            } catch (Throwable th24) {
                th = th24;
                parcelFileDescriptorArr = null;
                if (this.mCancelAll) {
                }
                backupRunStatus2 = backupRunStatus;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Full backup completed with status: ");
                stringBuilder.append(backupRunStatus2);
                Slog.i(str, stringBuilder.toString());
                BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
                cleanUpPipes(parcelFileDescriptorArr);
                cleanUpPipes(enginePipes2);
                unregisterTask();
                if (this.mJob != null) {
                }
                synchronized (this.backupManagerService.getQueueLock()) {
                }
                this.mListener.onFinished("PFTBT.run()");
                this.mLatch.countDown();
                if (this.mUpdateSchedule) {
                }
                Slog.i(TAG, "Full data backup pass finished.");
                this.backupManagerService.getWakelock().release();
                throw th;
            }
        } catch (Exception e9) {
            e = e9;
            backupRunStatus = backupRunStatus3;
            Slog.w(TAG, "Exception trying full transport backup", e);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
            if (this.mCancelAll) {
            }
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Full backup completed with status: ");
            stringBuilder.append(backupRunStatus4);
            Slog.i(str2, stringBuilder.toString());
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
            cleanUpPipes(transportPipes);
            cleanUpPipes(enginePipes2);
            unregisterTask();
            if (this.mJob != null) {
            }
            synchronized (this.backupManagerService.getQueueLock()) {
            }
            this.mListener.onFinished("PFTBT.run()");
            this.mLatch.countDown();
            if (this.mUpdateSchedule) {
            }
            Slog.i(TAG, "Full data backup pass finished.");
            this.backupManagerService.getWakelock().release();
            parcelFileDescriptorArr = transportPipes;
        } catch (Throwable th25) {
            th = th25;
            backupRunStatus = backupRunStatus3;
            parcelFileDescriptorArr = null;
            if (this.mCancelAll) {
            }
            backupRunStatus2 = backupRunStatus;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Full backup completed with status: ");
            stringBuilder.append(backupRunStatus2);
            Slog.i(str, stringBuilder.toString());
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus2);
            cleanUpPipes(parcelFileDescriptorArr);
            cleanUpPipes(enginePipes2);
            unregisterTask();
            if (this.mJob != null) {
            }
            synchronized (this.backupManagerService.getQueueLock()) {
            }
            this.mListener.onFinished("PFTBT.run()");
            this.mLatch.countDown();
            if (this.mUpdateSchedule) {
            }
            Slog.i(TAG, "Full data backup pass finished.");
            this.backupManagerService.getWakelock().release();
            throw th;
        }
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
