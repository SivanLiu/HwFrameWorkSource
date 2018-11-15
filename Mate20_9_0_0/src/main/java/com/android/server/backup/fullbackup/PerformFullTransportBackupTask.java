package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.util.AndroidException;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
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

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    public void run() {
        /*
        r47 = this;
        r10 = r47;
        r1 = 0;
        r2 = 0;
        r3 = 0;
        r11 = 0;
        r12 = r11;
        r14 = 0;
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = r0.isEnabled();	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        if (r0 == 0) goto L_0x068f;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
    L_0x0011:
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = r0.isProvisioned();	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        if (r0 != 0) goto L_0x001d;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
    L_0x0019:
        r27 = r12;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        goto L_0x0691;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
    L_0x001d:
        r0 = r10.mTransportClient;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r5 = "PFTBT.run()";	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = r0.connect(r5);	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r15 = r0;
        r9 = 1;
        if (r15 != 0) goto L_0x00ad;
    L_0x0029:
        r0 = "PFTBT";	 Catch:{ Exception -> 0x00a8 }
        r5 = "Transport not present; full data backup not performed";	 Catch:{ Exception -> 0x00a8 }
        android.util.Slog.w(r0, r5);	 Catch:{ Exception -> 0x00a8 }
        r12 = -1000; // 0xfffffffffffffc18 float:NaN double:NaN;	 Catch:{ Exception -> 0x00a8 }
        r0 = r10.mMonitor;	 Catch:{ Exception -> 0x00a8 }
        r5 = 15;	 Catch:{ Exception -> 0x00a8 }
        r6 = r10.mCurrentPackage;	 Catch:{ Exception -> 0x00a8 }
        r0 = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r0, r5, r6, r9, r14);	 Catch:{ Exception -> 0x00a8 }
        r10.mMonitor = r0;	 Catch:{ Exception -> 0x00a8 }
        r0 = r10.mCancelAll;
        if (r0 == 0) goto L_0x0044;
    L_0x0042:
        r12 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
    L_0x0044:
        r5 = r12;
        r0 = "PFTBT";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Full backup completed with status: ";
        r6.append(r7);
        r6.append(r5);
        r6 = r6.toString();
        android.util.Slog.i(r0, r6);
        r0 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r0, r5);
        r10.cleanUpPipes(r2);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r0 = r10.mJob;
        if (r0 == 0) goto L_0x0072;
    L_0x006d:
        r0 = r10.mJob;
        r0.finishBackupPass();
    L_0x0072:
        r0 = r10.backupManagerService;
        r6 = r0.getQueueLock();
        monitor-enter(r6);
        r0 = r10.backupManagerService;
        r0.setRunningFullBackupTask(r14);
        monitor-exit(r6);
        r0 = r10.mListener;
        r6 = "PFTBT.run()";
        r0.onFinished(r6);
        r0 = r10.mLatch;
        r0.countDown();
        r0 = r10.mUpdateSchedule;
        if (r0 == 0) goto L_0x0094;
    L_0x008f:
        r0 = r10.backupManagerService;
        r0.scheduleNextFullBackupJob(r3);
    L_0x0094:
        r0 = "PFTBT";
        r6 = "Full data backup pass finished.";
        android.util.Slog.i(r0, r6);
        r0 = r10.backupManagerService;
        r0 = r0.getWakelock();
        r0.release();
        return;
    L_0x00a5:
        r0 = move-exception;
        monitor-exit(r6);
        throw r0;
    L_0x00a8:
        r0 = move-exception;
        r27 = r12;
        goto L_0x0757;
    L_0x00ad:
        r0 = r10.mPackages;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = r0.size();	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r8 = r0;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r0 = new byte[r0];	 Catch:{ Exception -> 0x0754, all -> 0x074e }
        r5 = r0;
        r0 = r11;
    L_0x00ba:
        r6 = r0;
        if (r6 >= r8) goto L_0x0610;
    L_0x00bd:
        r10.mBackupRunner = r14;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = r10.mPackages;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = r0.get(r6);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = (android.content.pm.PackageInfo) r0;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r7 = r0;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = r7.packageName;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r16 = r0;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13.<init>();	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r14 = "Initiating full-data transport backup of ";	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13.append(r14);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r14 = r16;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13.append(r14);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r9 = " token: ";	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13.append(r9);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r9 = r10.mCurrentOpToken;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13.append(r9);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r9 = r13.toString();	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        android.util.Slog.i(r0, r9);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = 2840; // 0xb18 float:3.98E-42 double:1.403E-320;	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        android.util.EventLog.writeEvent(r0, r14);	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r0 = android.os.ParcelFileDescriptor.createPipe();	 Catch:{ Exception -> 0x0609, all -> 0x0601 }
        r13 = r0;
        r0 = r10.mUserInitiated;	 Catch:{ Exception -> 0x05fa, all -> 0x05f4 }
        r9 = r0;	 Catch:{ Exception -> 0x05fa, all -> 0x05f4 }
        r18 = 9223372036854775807; // 0x7fffffffffffffff float:NaN double:NaN;	 Catch:{ Exception -> 0x05fa, all -> 0x05f4 }
        r2 = r10.mCancelLock;	 Catch:{ Exception -> 0x05fa, all -> 0x05f4 }
        monitor-enter(r2);	 Catch:{ Exception -> 0x05fa, all -> 0x05f4 }
        r0 = r10.mCancelAll;	 Catch:{ all -> 0x05d3 }
        if (r0 == 0) goto L_0x0122;
    L_0x0107:
        monitor-exit(r2);	 Catch:{ all -> 0x010e }
        r25 = r3;
        r27 = r12;
        goto L_0x0615;
    L_0x010e:
        r0 = move-exception;
        r23 = r2;
        r25 = r3;
        r41 = r5;
        r29 = r6;
        r5 = r7;
        r20 = r8;
        r17 = r9;
        r27 = r12;
        r12 = r14;
        r9 = r15;
        goto L_0x05e5;
    L_0x0122:
        r0 = r13[r11];	 Catch:{ all -> 0x05d3 }
        r0 = r15.performFullBackup(r7, r0, r9);	 Catch:{ all -> 0x05d3 }
        if (r0 != 0) goto L_0x01f8;
    L_0x012a:
        r11 = r7.packageName;	 Catch:{ all -> 0x01e4 }
        r20 = r3;
        r3 = 1;
        r22 = r15.getBackupQuota(r11, r3);	 Catch:{ all -> 0x01d0 }
        r11 = r6;
        r4 = r7;
        r6 = r22;
        r3 = android.os.ParcelFileDescriptor.createPipe();	 Catch:{ all -> 0x01ba }
        r16 = r3;
        r3 = new com.android.server.backup.fullbackup.PerformFullTransportBackupTask$SinglePackageBackupRunner;	 Catch:{ all -> 0x01a4 }
        r17 = 1;	 Catch:{ all -> 0x01a4 }
        r18 = r16[r17];	 Catch:{ all -> 0x01a4 }
        r1 = r10.mTransportClient;	 Catch:{ all -> 0x01a4 }
        r24 = r8;
        r8 = r10.mBackupRunnerOpToken;	 Catch:{ all -> 0x018c }
        r19 = r15.getTransportFlags();	 Catch:{ all -> 0x018c }
        r22 = r1;
        r1 = r3;
        r23 = r2;
        r2 = r10;
        r27 = r12;
        r25 = r20;
        r12 = r3;
        r3 = r18;
        r28 = r4;
        r29 = r11;
        r11 = r5;
        r5 = r22;
        r20 = r24;
        r30 = r14;
        r14 = r17;
        r17 = r9;
        r9 = r19;
        r1.<init>(r2, r3, r4, r5, r6, r8, r9);	 Catch:{ all -> 0x017e }
        r10.mBackupRunner = r12;	 Catch:{ all -> 0x017e }
        r1 = r16[r14];	 Catch:{ all -> 0x017e }
        r1.close();	 Catch:{ all -> 0x017e }
        r1 = 0;	 Catch:{ all -> 0x017e }
        r16[r14] = r1;	 Catch:{ all -> 0x017e }
        r10.mIsDoingBackup = r14;	 Catch:{ all -> 0x017e }
        r1 = r16;
        goto L_0x020c;
    L_0x017e:
        r0 = move-exception;
        r18 = r6;
        r41 = r11;
        r9 = r15;
        r1 = r16;
        r5 = r28;
        r12 = r30;
        goto L_0x05e5;
    L_0x018c:
        r0 = move-exception;
        r23 = r2;
        r17 = r9;
        r29 = r11;
        r27 = r12;
        r25 = r20;
        r20 = r24;
        r41 = r5;
        r18 = r6;
        r12 = r14;
        r9 = r15;
        r1 = r16;
        r5 = r4;
        goto L_0x05e5;
    L_0x01a4:
        r0 = move-exception;
        r23 = r2;
        r17 = r9;
        r29 = r11;
        r27 = r12;
        r25 = r20;
        r20 = r8;
        r41 = r5;
        r18 = r6;
        r12 = r14;
        r9 = r15;
        r1 = r16;
        goto L_0x01cd;
    L_0x01ba:
        r0 = move-exception;
        r23 = r2;
        r17 = r9;
        r29 = r11;
        r27 = r12;
        r25 = r20;
        r20 = r8;
        r41 = r5;
        r18 = r6;
        r12 = r14;
        r9 = r15;
    L_0x01cd:
        r5 = r4;
        goto L_0x05e5;
    L_0x01d0:
        r0 = move-exception;
        r23 = r2;
        r29 = r6;
        r17 = r9;
        r27 = r12;
        r25 = r20;
        r20 = r8;
        r41 = r5;
        r5 = r7;
        r12 = r14;
        r9 = r15;
        goto L_0x05e5;
    L_0x01e4:
        r0 = move-exception;
        r23 = r2;
        r25 = r3;
        r29 = r6;
        r20 = r8;
        r17 = r9;
        r27 = r12;
        r41 = r5;
        r5 = r7;
        r12 = r14;
        r9 = r15;
        goto L_0x05e5;
    L_0x01f8:
        r23 = r2;
        r25 = r3;
        r11 = r5;
        r29 = r6;
        r28 = r7;
        r20 = r8;
        r17 = r9;
        r27 = r12;
        r30 = r14;
        r14 = 1;
        r6 = r18;
    L_0x020c:
        monitor-exit(r23);	 Catch:{ all -> 0x05c8 }
        if (r0 != 0) goto L_0x03c9;
    L_0x020f:
        r3 = 0;
        r4 = r13[r3];	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4.close();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r13[r3] = r4;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3 = new java.lang.Thread;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = r10.mBackupRunner;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = "package-backup-bridge";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3.<init>(r4, r5);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3.start();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3 = new java.io.FileInputStream;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = r1[r4];	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = r5.getFileDescriptor();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3.<init>(r4);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = new java.io.FileOutputStream;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = r13[r14];	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = r5.getFileDescriptor();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4.<init>(r5);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = r10.mBackupRunner;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r18 = r5.getPreflightResultBlocking();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r31 = r18;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r18 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r33 = r15;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r14 = r31;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = (r14 > r18 ? 1 : (r14 == r18 ? 0 : -1));	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        if (r5 >= 0) goto L_0x0277;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0250:
        r5 = r10.mMonitor;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = r10.mCurrentPackage;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r12 = "android.app.backup.extra.LOG_PREFLIGHT_ERROR";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r34 = r8;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r9 = com.android.server.backup.utils.BackupManagerMonitorUtils.putMonitoringExtra(r8, r12, r14);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = 16;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r12 = 3;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r5, r8, r2, r12, r9);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r10.mMonitor = r2;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = (int) r14;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r39 = r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r36 = r3;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r38 = r4;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r41 = r11;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r12 = r30;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r9 = r33;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3 = r34;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        goto L_0x0337;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0277:
        r34 = r8;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x027b:
        r5 = r3.read(r11);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        if (r5 <= 0) goto L_0x02d4;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0281:
        r8 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4.write(r11, r8, r5);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = r10.mCancelLock;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        monitor-enter(r8);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = r10.mCancelAll;	 Catch:{ all -> 0x02c9 }
        if (r0 != 0) goto L_0x029a;
    L_0x028c:
        r9 = r33;
        r0 = r9.sendBackupData(r5);	 Catch:{ all -> 0x0294 }
        r2 = r0;
        goto L_0x029c;
    L_0x0294:
        r0 = move-exception;
        r36 = r3;
        r12 = r30;
        goto L_0x02d0;
    L_0x029a:
        r9 = r33;
    L_0x029c:
        monitor-exit(r8);	 Catch:{ all -> 0x02c1 }
        r37 = r2;
        r36 = r3;
        r2 = (long) r5;
        r2 = r34 + r2;
        r0 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        if (r0 == 0) goto L_0x02b9;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02a8:
        r0 = (r14 > r18 ? 1 : (r14 == r18 ? 0 : -1));	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        if (r0 <= 0) goto L_0x02b9;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02ac:
        r0 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = new android.app.backup.BackupProgress;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.<init>(r14, r2);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r12 = r30;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnUpdate(r0, r12, r8);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        goto L_0x02bb;
    L_0x02b9:
        r12 = r30;
    L_0x02bb:
        r38 = r4;
        r3 = r2;
        r2 = r37;
        goto L_0x02de;
    L_0x02c1:
        r0 = move-exception;
        r37 = r2;
        r36 = r3;
        r12 = r30;
        goto L_0x02d0;
    L_0x02c9:
        r0 = move-exception;
        r36 = r3;
        r12 = r30;
        r9 = r33;
    L_0x02d0:
        monitor-exit(r8);	 Catch:{ all -> 0x02d2 }
        throw r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02d2:
        r0 = move-exception;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        goto L_0x02d0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02d4:
        r36 = r3;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r12 = r30;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r9 = r33;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r38 = r4;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3 = r34;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02de:
        if (r5 <= 0) goto L_0x02ef;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02e0:
        if (r2 == 0) goto L_0x02e3;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02e2:
        goto L_0x02ef;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02e3:
        r34 = r3;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = r5;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r33 = r9;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r30 = r12;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r3 = r36;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r4 = r38;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        goto L_0x027b;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02ef:
        r0 = -1005; // 0xfffffffffffffc13 float:NaN double:NaN;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        if (r2 != r0) goto L_0x0333;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x02f3:
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.<init>();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r39 = r2;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = "Package hit quota limit in-flight ";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r2);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r12);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = ": ";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r2);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r3);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = " of ";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r2);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r6);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = r8.toString();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        android.util.Slog.w(r0, r2);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = r10.mMonitor;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = 18;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = r10.mCurrentPackage;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r40 = r5;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r41 = r11;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = 0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r11 = 1;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r0, r2, r8, r11, r5);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r10.mMonitor = r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = r10.mBackupRunner;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0.sendQuotaExceeded(r3, r6);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        goto L_0x0337;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0333:
        r39 = r2;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r41 = r11;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0337:
        r0 = r10.mBackupRunner;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r0 = r0.getBackupResultBlocking();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r2 = r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r5 = r10.mCancelLock;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        monitor-enter(r5);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = 0;
        r10.mIsDoingBackup = r8;	 Catch:{ all -> 0x03c0 }
        r0 = r10.mCancelAll;	 Catch:{ all -> 0x03c0 }
        if (r0 != 0) goto L_0x035d;
    L_0x0348:
        if (r2 != 0) goto L_0x035a;
    L_0x034a:
        r0 = r9.finishBackup();	 Catch:{ all -> 0x0353 }
        if (r39 != 0) goto L_0x035d;	 Catch:{ all -> 0x0353 }
    L_0x0350:
        r39 = r0;	 Catch:{ all -> 0x0353 }
    L_0x0352:
        goto L_0x035d;	 Catch:{ all -> 0x0353 }
    L_0x0353:
        r0 = move-exception;	 Catch:{ all -> 0x0353 }
        r46 = r2;	 Catch:{ all -> 0x0353 }
        r44 = r3;	 Catch:{ all -> 0x0353 }
        goto L_0x03c5;	 Catch:{ all -> 0x0353 }
    L_0x035a:
        r9.cancelFullBackup();	 Catch:{ all -> 0x0353 }
    L_0x035d:
        monitor-exit(r5);	 Catch:{ all -> 0x03c0 }
        if (r39 != 0) goto L_0x0364;
    L_0x0360:
        if (r2 == 0) goto L_0x0364;
    L_0x0362:
        r0 = r2;
        goto L_0x0366;
    L_0x0364:
        r0 = r39;
    L_0x0366:
        if (r0 == 0) goto L_0x0386;
    L_0x0368:
        r5 = "PFTBT";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.<init>();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r11 = "Error ";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r11);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r0);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r11 = " backing up ";	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r11);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8.append(r12);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        android.util.Slog.e(r5, r8);	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x0386:
        r18 = r9.requestFullBackupTime();	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
        r42 = r18;
        r5 = "PFTBT";	 Catch:{ Exception -> 0x03b9, all -> 0x03b3 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x03b9, all -> 0x03b3 }
        r8.<init>();	 Catch:{ Exception -> 0x03b9, all -> 0x03b3 }
        r11 = "Transport suggested backoff=";	 Catch:{ Exception -> 0x03b9, all -> 0x03b3 }
        r8.append(r11);	 Catch:{ Exception -> 0x03b9, all -> 0x03b3 }
        r46 = r2;
        r44 = r3;
        r2 = r42;
        r8.append(r2);	 Catch:{ Exception -> 0x03af, all -> 0x03ab }
        r4 = r8.toString();	 Catch:{ Exception -> 0x03af, all -> 0x03ab }
        android.util.Slog.i(r5, r4);	 Catch:{ Exception -> 0x03af, all -> 0x03ab }
        r3 = r2;
        r2 = r0;
        goto L_0x03d1;
    L_0x03ab:
        r0 = move-exception;
        r3 = r2;
        goto L_0x07eb;
    L_0x03af:
        r0 = move-exception;
        r3 = r2;
        goto L_0x05c5;
    L_0x03b3:
        r0 = move-exception;
        r2 = r42;
        r3 = r2;
        goto L_0x07eb;
    L_0x03b9:
        r0 = move-exception;
        r2 = r42;
        r3 = r2;
        r2 = r13;
        goto L_0x0757;
    L_0x03c0:
        r0 = move-exception;
        r46 = r2;
        r44 = r3;
    L_0x03c5:
        monitor-exit(r5);	 Catch:{ all -> 0x03c7 }
        throw r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x03c7:
        r0 = move-exception;
        goto L_0x03c5;
    L_0x03c9:
        r41 = r11;
        r9 = r15;
        r12 = r30;
        r2 = r0;
        r3 = r25;
    L_0x03d1:
        r0 = r10.mUpdateSchedule;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r0 == 0) goto L_0x03de;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x03d5:
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = java.lang.System.currentTimeMillis();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.enqueueFullBackup(r12, r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x03de:
        r0 = -1002; // 0xfffffffffffffc16 float:NaN double:NaN;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r2 != r0) goto L_0x0426;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x03e2:
        r5 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r5, r12, r0);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = "Transport rejected backup of ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5.append(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = ", skipping";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5.append(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5 = r5.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.i(r0, r5);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2841; // 0xb19 float:3.981E-42 double:1.4036E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5 = 2;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5 = new java.lang.Object[r5];	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = 0;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5[r8] = r12;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = "transport rejected";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r11 = 1;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5[r11] = r8;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r5);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.mBackupRunner;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r0 == 0) goto L_0x0422;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0417:
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r5 = r28;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r5.applicationInfo;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.tearDownAgentAndKill(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        goto L_0x056e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0422:
        r5 = r28;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        goto L_0x056e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0426:
        r5 = r28;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r11 = 1;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = -1005; // 0xfffffffffffffc13 float:NaN double:NaN;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r2 != r0) goto L_0x0456;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x042d:
        r8 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r8, r12, r0);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = "Transport quota exceeded for package: ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.i(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2845; // 0xb1d float:3.987E-42 double:1.4056E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r5.applicationInfo;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.tearDownAgentAndKill(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        goto L_0x056e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0456:
        r0 = -1003; // 0xfffffffffffffc15 float:NaN double:NaN;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r2 != r0) goto L_0x0483;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x045a:
        r8 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r8, r12, r0);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = "Application failure for package: ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.w(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2823; // 0xb07 float:3.956E-42 double:1.3947E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r5.applicationInfo;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.tearDownAgentAndKill(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        goto L_0x056e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0483:
        r0 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r2 != r0) goto L_0x04ba;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0487:
        r8 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r8, r12, r0);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = "Backup cancelled. package=";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = ", cancelAll=";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = r10.mCancelAll;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.w(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2846; // 0xb1e float:3.988E-42 double:1.406E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r5.applicationInfo;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.tearDownAgentAndKill(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        goto L_0x056e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x04ba:
        if (r2 == 0) goto L_0x055e;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x04bc:
        r0 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = -1000; // 0xfffffffffffffc18 float:NaN double:NaN;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r0, r12, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r11 = "Transport failed; aborting backup: ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r11);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r2);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.w(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2842; // 0xb1a float:3.982E-42 double:1.404E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = 0;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.Object[r8];	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = -1000; // 0xfffffffffffffc18 float:NaN double:NaN;
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x055a, all -> 0x0555 }
        r11 = r5.applicationInfo;	 Catch:{ Exception -> 0x055a, all -> 0x0555 }
        r0.tearDownAgentAndKill(r11);	 Catch:{ Exception -> 0x055a, all -> 0x0555 }
        r0 = r10.mCancelAll;
        if (r0 == 0) goto L_0x04f0;
    L_0x04ee:
        r8 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
    L_0x04f0:
        r11 = r8;
        r0 = "PFTBT";
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r14 = "Full backup completed with status: ";
        r8.append(r14);
        r8.append(r11);
        r8 = r8.toString();
        android.util.Slog.i(r0, r8);
        r0 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r0, r11);
        r10.cleanUpPipes(r13);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r0 = r10.mJob;
        if (r0 == 0) goto L_0x051e;
    L_0x0519:
        r0 = r10.mJob;
        r0.finishBackupPass();
    L_0x051e:
        r0 = r10.backupManagerService;
        r14 = r0.getQueueLock();
        monitor-enter(r14);
        r0 = r10.backupManagerService;
        r8 = 0;
        r0.setRunningFullBackupTask(r8);
        monitor-exit(r14);
        r0 = r10.mListener;
        r8 = "PFTBT.run()";
        r0.onFinished(r8);
        r0 = r10.mLatch;
        r0.countDown();
        r0 = r10.mUpdateSchedule;
        if (r0 == 0) goto L_0x0541;
    L_0x053c:
        r0 = r10.backupManagerService;
        r0.scheduleNextFullBackupJob(r3);
    L_0x0541:
        r0 = "PFTBT";
        r8 = "Full data backup pass finished.";
        android.util.Slog.i(r0, r8);
        r0 = r10.backupManagerService;
        r0 = r0.getWakelock();
        r0.release();
        return;
    L_0x0552:
        r0 = move-exception;
        monitor-exit(r14);
        throw r0;
    L_0x0555:
        r0 = move-exception;
        r27 = r8;
        goto L_0x07eb;
    L_0x055a:
        r0 = move-exception;
        r27 = r8;
        goto L_0x05c5;
    L_0x055e:
        r0 = r10.mBackupObserver;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = 0;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r0, r12, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = 2843; // 0xb1b float:3.984E-42 double:1.4046E-320;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.EventLog.writeEvent(r0, r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.logBackupComplete(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x056e:
        r10.cleanUpPipes(r13);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r10.cleanUpPipes(r1);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r5.applicationInfo;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        if (r0 == 0) goto L_0x05b2;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
    L_0x0578:
        r0 = "PFTBT";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = "Unbinding agent in ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        android.util.Slog.i(r0, r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.<init>();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r14 = "unbinding ";	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r14);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8.append(r12);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r8 = r8.toString();	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0.addBackupTrace(r8);	 Catch:{ Exception -> 0x05c4, all -> 0x05c1 }
        r0 = r10.backupManagerService;	 Catch:{ RemoteException -> 0x05b1 }
        r0 = r0.getActivityManager();	 Catch:{ RemoteException -> 0x05b1 }
        r8 = r5.applicationInfo;	 Catch:{ RemoteException -> 0x05b1 }
        r0.unbindBackupAgent(r8);	 Catch:{ RemoteException -> 0x05b1 }
        goto L_0x05b2;
    L_0x05b1:
        r0 = move-exception;
    L_0x05b2:
        r0 = r29 + 1;
        r15 = r9;
        r9 = r11;
        r2 = r13;
        r8 = r20;
        r12 = r27;
        r5 = r41;
        r11 = 0;
        r14 = 0;
        goto L_0x00ba;
    L_0x05c1:
        r0 = move-exception;
        goto L_0x07eb;
    L_0x05c4:
        r0 = move-exception;
    L_0x05c5:
        r2 = r13;
        goto L_0x0757;
    L_0x05c8:
        r0 = move-exception;
        r41 = r11;
        r9 = r15;
        r5 = r28;
        r12 = r30;
        r18 = r6;
        goto L_0x05e5;
    L_0x05d3:
        r0 = move-exception;
        r23 = r2;
        r25 = r3;
        r41 = r5;
        r29 = r6;
        r5 = r7;
        r20 = r8;
        r17 = r9;
        r27 = r12;
        r12 = r14;
        r9 = r15;
    L_0x05e5:
        monitor-exit(r23);	 Catch:{ all -> 0x05f2 }
        throw r0;	 Catch:{ Exception -> 0x05ec, all -> 0x05e7 }
    L_0x05e7:
        r0 = move-exception;
        r3 = r25;
        goto L_0x07eb;
    L_0x05ec:
        r0 = move-exception;
        r2 = r13;
        r3 = r25;
        goto L_0x0757;
    L_0x05f2:
        r0 = move-exception;
        goto L_0x05e5;
    L_0x05f4:
        r0 = move-exception;
        r25 = r3;
        r27 = r12;
        goto L_0x0607;
    L_0x05fa:
        r0 = move-exception;
        r25 = r3;
        r27 = r12;
        r2 = r13;
        goto L_0x060e;
    L_0x0601:
        r0 = move-exception;
        r25 = r3;
        r27 = r12;
        r13 = r2;
    L_0x0607:
        goto L_0x07eb;
    L_0x0609:
        r0 = move-exception;
        r25 = r3;
        r27 = r12;
    L_0x060e:
        goto L_0x0757;
    L_0x0610:
        r25 = r3;
        r27 = r12;
        r13 = r2;
    L_0x0615:
        r0 = r10.mCancelAll;
        if (r0 == 0) goto L_0x061c;
    L_0x0619:
        r12 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
        goto L_0x061e;
    L_0x061c:
        r12 = r27;
    L_0x061e:
        r0 = "PFTBT";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Full backup completed with status: ";
        r2.append(r3);
        r2.append(r12);
        r2 = r2.toString();
        android.util.Slog.i(r0, r2);
        r0 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r0, r12);
        r10.cleanUpPipes(r13);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r0 = r10.mJob;
        if (r0 == 0) goto L_0x064b;
    L_0x0646:
        r0 = r10.mJob;
        r0.finishBackupPass();
    L_0x064b:
        r0 = r10.backupManagerService;
        r5 = r0.getQueueLock();
        monitor-enter(r5);
        r0 = r10.backupManagerService;	 Catch:{ all -> 0x0688 }
        r2 = 0;	 Catch:{ all -> 0x0688 }
        r0.setRunningFullBackupTask(r2);	 Catch:{ all -> 0x0688 }
        monitor-exit(r5);	 Catch:{ all -> 0x0688 }
        r0 = r10.mListener;
        r2 = "PFTBT.run()";
        r0.onFinished(r2);
        r0 = r10.mLatch;
        r0.countDown();
        r0 = r10.mUpdateSchedule;
        if (r0 == 0) goto L_0x0671;
    L_0x0669:
        r0 = r10.backupManagerService;
        r6 = r25;
        r0.scheduleNextFullBackupJob(r6);
        goto L_0x0673;
    L_0x0671:
        r6 = r25;
    L_0x0673:
        r0 = "PFTBT";
        r2 = "Full data backup pass finished.";
        android.util.Slog.i(r0, r2);
        r0 = r10.backupManagerService;
        r0 = r0.getWakelock();
        r0.release();
        r3 = r6;
        r5 = r12;
        goto L_0x07e3;
    L_0x0688:
        r0 = move-exception;
        r6 = r25;
    L_0x068b:
        monitor-exit(r5);	 Catch:{ all -> 0x068d }
        throw r0;
    L_0x068d:
        r0 = move-exception;
        goto L_0x068b;
    L_0x068f:
        r27 = r12;
    L_0x0691:
        r0 = "PFTBT";	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.<init>();	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = "full backup requested but enabled=";	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.append(r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = r10.backupManagerService;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = r6.isEnabled();	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.append(r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = " provisioned=";	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.append(r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = r10.backupManagerService;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = r6.isProvisioned();	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.append(r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = "; ignoring";	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5.append(r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r5 = r5.toString();	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        android.util.Slog.i(r0, r5);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r0 = r10.backupManagerService;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r0 = r0.isProvisioned();	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        if (r0 == 0) goto L_0x06cb;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
    L_0x06c8:
        r0 = 13;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        goto L_0x06cd;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
    L_0x06cb:
        r0 = 14;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
    L_0x06cd:
        r5 = r0;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r0 = r10.mMonitor;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = 0;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r7 = 3;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r0 = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r0, r5, r6, r7, r6);	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r10.mMonitor = r0;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r6 = 0;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r10.mUpdateSchedule = r6;	 Catch:{ Exception -> 0x074c, all -> 0x0748 }
        r0 = -2001; // 0xfffffffffffff82f float:NaN double:NaN;
        r6 = r10.mCancelAll;
        if (r6 == 0) goto L_0x06e3;
    L_0x06e1:
        r0 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
    L_0x06e3:
        r6 = r0;
        r0 = "PFTBT";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "Full backup completed with status: ";
        r7.append(r8);
        r7.append(r6);
        r7 = r7.toString();
        android.util.Slog.i(r0, r7);
        r0 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r0, r6);
        r10.cleanUpPipes(r2);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r0 = r10.mJob;
        if (r0 == 0) goto L_0x0711;
    L_0x070c:
        r0 = r10.mJob;
        r0.finishBackupPass();
    L_0x0711:
        r0 = r10.backupManagerService;
        r7 = r0.getQueueLock();
        monitor-enter(r7);
        r0 = r10.backupManagerService;
        r8 = 0;
        r0.setRunningFullBackupTask(r8);
        monitor-exit(r7);
        r0 = r10.mListener;
        r7 = "PFTBT.run()";
        r0.onFinished(r7);
        r0 = r10.mLatch;
        r0.countDown();
        r0 = r10.mUpdateSchedule;
        if (r0 == 0) goto L_0x0734;
    L_0x072f:
        r0 = r10.backupManagerService;
        r0.scheduleNextFullBackupJob(r3);
    L_0x0734:
        r0 = "PFTBT";
        r7 = "Full data backup pass finished.";
        android.util.Slog.i(r0, r7);
        r0 = r10.backupManagerService;
        r0 = r0.getWakelock();
        r0.release();
        return;
    L_0x0745:
        r0 = move-exception;
        monitor-exit(r7);
        throw r0;
    L_0x0748:
        r0 = move-exception;
        r13 = r2;
        goto L_0x07eb;
    L_0x074c:
        r0 = move-exception;
        goto L_0x0757;
    L_0x074e:
        r0 = move-exception;
        r27 = r12;
        r13 = r2;
        goto L_0x07eb;
    L_0x0754:
        r0 = move-exception;
        r27 = r12;
    L_0x0757:
        r12 = -1000; // 0xfffffffffffffc18 float:NaN double:NaN;
        r5 = "PFTBT";	 Catch:{ all -> 0x07e7 }
        r6 = "Exception trying full transport backup";	 Catch:{ all -> 0x07e7 }
        android.util.Slog.w(r5, r6, r0);	 Catch:{ all -> 0x07e7 }
        r5 = r10.mMonitor;	 Catch:{ all -> 0x07e7 }
        r6 = 19;	 Catch:{ all -> 0x07e7 }
        r7 = r10.mCurrentPackage;	 Catch:{ all -> 0x07e7 }
        r8 = "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP";	 Catch:{ all -> 0x07e7 }
        r9 = android.util.Log.getStackTraceString(r0);	 Catch:{ all -> 0x07e7 }
        r11 = 0;	 Catch:{ all -> 0x07e7 }
        r8 = com.android.server.backup.utils.BackupManagerMonitorUtils.putMonitoringExtra(r11, r8, r9);	 Catch:{ all -> 0x07e7 }
        r9 = 3;	 Catch:{ all -> 0x07e7 }
        r5 = com.android.server.backup.utils.BackupManagerMonitorUtils.monitorEvent(r5, r6, r7, r9, r8);	 Catch:{ all -> 0x07e7 }
        r10.mMonitor = r5;	 Catch:{ all -> 0x07e7 }
        r0 = r10.mCancelAll;
        if (r0 == 0) goto L_0x0780;
    L_0x077c:
        r0 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
        r5 = r0;
        goto L_0x0781;
    L_0x0780:
        r5 = r12;
    L_0x0781:
        r0 = "PFTBT";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Full backup completed with status: ";
        r6.append(r7);
        r6.append(r5);
        r6 = r6.toString();
        android.util.Slog.i(r0, r6);
        r0 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r0, r5);
        r10.cleanUpPipes(r2);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r0 = r10.mJob;
        if (r0 == 0) goto L_0x07ae;
    L_0x07a9:
        r0 = r10.mJob;
        r0.finishBackupPass();
    L_0x07ae:
        r0 = r10.backupManagerService;
        r6 = r0.getQueueLock();
        monitor-enter(r6);
        r0 = r10.backupManagerService;
        r7 = 0;
        r0.setRunningFullBackupTask(r7);
        monitor-exit(r6);
        r0 = r10.mListener;
        r6 = "PFTBT.run()";
        r0.onFinished(r6);
        r0 = r10.mLatch;
        r0.countDown();
        r0 = r10.mUpdateSchedule;
        if (r0 == 0) goto L_0x07d1;
    L_0x07cc:
        r0 = r10.backupManagerService;
        r0.scheduleNextFullBackupJob(r3);
    L_0x07d1:
        r0 = "PFTBT";
        r6 = "Full data backup pass finished.";
        android.util.Slog.i(r0, r6);
        r0 = r10.backupManagerService;
        r0 = r0.getWakelock();
        r0.release();
        r13 = r2;
    L_0x07e3:
        return;
    L_0x07e4:
        r0 = move-exception;
        monitor-exit(r6);
        throw r0;
    L_0x07e7:
        r0 = move-exception;
        r13 = r2;
        r27 = r12;
    L_0x07eb:
        r2 = r10.mCancelAll;
        if (r2 == 0) goto L_0x07f1;
    L_0x07ef:
        r27 = -2003; // 0xfffffffffffff82d float:NaN double:NaN;
    L_0x07f1:
        r2 = r27;
        r5 = "PFTBT";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "Full backup completed with status: ";
        r6.append(r7);
        r6.append(r2);
        r6 = r6.toString();
        android.util.Slog.i(r5, r6);
        r5 = r10.mBackupObserver;
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r5, r2);
        r10.cleanUpPipes(r13);
        r10.cleanUpPipes(r1);
        r47.unregisterTask();
        r5 = r10.mJob;
        if (r5 == 0) goto L_0x0820;
    L_0x081b:
        r5 = r10.mJob;
        r5.finishBackupPass();
    L_0x0820:
        r5 = r10.backupManagerService;
        r5 = r5.getQueueLock();
        monitor-enter(r5);
        r6 = r10.backupManagerService;
        r7 = 0;
        r6.setRunningFullBackupTask(r7);
        monitor-exit(r5);
        r5 = r10.mListener;
        r6 = "PFTBT.run()";
        r5.onFinished(r6);
        r5 = r10.mLatch;
        r5.countDown();
        r5 = r10.mUpdateSchedule;
        if (r5 == 0) goto L_0x0843;
    L_0x083e:
        r5 = r10.backupManagerService;
        r5.scheduleNextFullBackupJob(r3);
    L_0x0843:
        r5 = "PFTBT";
        r6 = "Full data backup pass finished.";
        android.util.Slog.i(r5, r6);
        r5 = r10.backupManagerService;
        r5 = r5.getWakelock();
        r5.release();
        throw r0;
    L_0x0854:
        r0 = move-exception;
        monitor-exit(r5);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.fullbackup.PerformFullTransportBackupTask.run():void");
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

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0036 A:{Splitter: B:14:0x002a, ExcHandler: android.os.RemoteException (r1_9 'e' android.util.AndroidException)} */
    /* JADX WARNING: Missing block: B:16:0x0036, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:?, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Error calling cancelFullBackup() on transport: ");
            r3.append(r1);
            android.util.Slog.w(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:20:0x004e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
                    this.mTransportClient.getConnectedTransport("PFTBT.handleCancel()").cancelFullBackup();
                } catch (AndroidException e) {
                }
            }
        }
    }

    public void operationComplete(long result) {
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
