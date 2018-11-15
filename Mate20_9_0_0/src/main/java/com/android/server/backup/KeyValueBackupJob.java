package com.android.server.backup;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import com.android.server.pm.PackageManagerService;

public class KeyValueBackupJob extends JobService {
    private static final int JOB_ID = 20537;
    private static final long MAX_DEFERRAL = 86400000;
    private static final String TAG = "KeyValueBackupJob";
    private static ComponentName sKeyValueJobService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, KeyValueBackupJob.class.getName());
    private static long sNextScheduled = 0;
    private static boolean sScheduled = false;

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        */
    public static void schedule(android.content.Context r15, long r16, com.android.server.backup.BackupManagerConstants r18) {
        /*
        r1 = com.android.server.backup.KeyValueBackupJob.class;
        monitor-enter(r1);
        r0 = sScheduled;	 Catch:{ all -> 0x0097 }
        if (r0 == 0) goto L_0x0009;	 Catch:{ all -> 0x0097 }
    L_0x0007:
        monitor-exit(r1);	 Catch:{ all -> 0x0097 }
        return;	 Catch:{ all -> 0x0097 }
    L_0x0009:
        monitor-enter(r18);	 Catch:{ all -> 0x0097 }
        r2 = r18.getKeyValueBackupIntervalMilliseconds();	 Catch:{ all -> 0x008f, all -> 0x0093 }
        r4 = r18.getKeyValueBackupFuzzMilliseconds();	 Catch:{ all -> 0x008f, all -> 0x0093 }
        r0 = r18.getKeyValueBackupRequiredNetworkType();	 Catch:{ all -> 0x008f, all -> 0x0093 }
        r6 = r18.getKeyValueBackupRequireCharging();	 Catch:{ all -> 0x008f, all -> 0x0093 }
        monitor-exit(r18);	 Catch:{ all -> 0x008f, all -> 0x0093 }
        r7 = 0;
        r7 = (r16 > r7 ? 1 : (r16 == r7 ? 0 : -1));
        if (r7 > 0) goto L_0x002f;
    L_0x0021:
        r7 = new java.util.Random;	 Catch:{ all -> 0x0097 }
        r7.<init>();	 Catch:{ all -> 0x0097 }
        r8 = (int) r4;	 Catch:{ all -> 0x0097 }
        r7 = r7.nextInt(r8);	 Catch:{ all -> 0x0097 }
        r7 = (long) r7;
        r7 = r7 + r2;
        r9 = r7;
        goto L_0x0031;
    L_0x002f:
        r9 = r16;
    L_0x0031:
        r7 = "KeyValueBackupJob";	 Catch:{ all -> 0x008c }
        r8 = new java.lang.StringBuilder;	 Catch:{ all -> 0x008c }
        r8.<init>();	 Catch:{ all -> 0x008c }
        r11 = "Scheduling k/v pass in ";	 Catch:{ all -> 0x008c }
        r8.append(r11);	 Catch:{ all -> 0x008c }
        r11 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;	 Catch:{ all -> 0x008c }
        r11 = r9 / r11;	 Catch:{ all -> 0x008c }
        r13 = 60;	 Catch:{ all -> 0x008c }
        r11 = r11 / r13;	 Catch:{ all -> 0x008c }
        r8.append(r11);	 Catch:{ all -> 0x008c }
        r11 = " minutes";	 Catch:{ all -> 0x008c }
        r8.append(r11);	 Catch:{ all -> 0x008c }
        r8 = r8.toString();	 Catch:{ all -> 0x008c }
        android.util.Slog.v(r7, r8);	 Catch:{ all -> 0x008c }
        r7 = new android.app.job.JobInfo$Builder;	 Catch:{ all -> 0x008c }
        r8 = 20537; // 0x5039 float:2.8778E-41 double:1.01466E-319;	 Catch:{ all -> 0x008c }
        r11 = sKeyValueJobService;	 Catch:{ all -> 0x008c }
        r7.<init>(r8, r11);	 Catch:{ all -> 0x008c }
        r7 = r7.setMinimumLatency(r9);	 Catch:{ all -> 0x008c }
        r7 = r7.setRequiredNetworkType(r0);	 Catch:{ all -> 0x008c }
        r7 = r7.setRequiresCharging(r6);	 Catch:{ all -> 0x008c }
        r11 = 86400000; // 0x5265c00 float:7.82218E-36 double:4.2687272E-316;	 Catch:{ all -> 0x008c }
        r7 = r7.setOverrideDeadline(r11);	 Catch:{ all -> 0x008c }
        r8 = "jobscheduler";	 Catch:{ all -> 0x008c }
        r11 = r15;
        r8 = r11.getSystemService(r8);	 Catch:{ all -> 0x009d }
        r8 = (android.app.job.JobScheduler) r8;	 Catch:{ all -> 0x009d }
        r12 = r7.build();	 Catch:{ all -> 0x009d }
        r8.schedule(r12);	 Catch:{ all -> 0x009d }
        r12 = java.lang.System.currentTimeMillis();	 Catch:{ all -> 0x009d }
        r12 = r12 + r9;	 Catch:{ all -> 0x009d }
        sNextScheduled = r12;	 Catch:{ all -> 0x009d }
        r12 = 1;	 Catch:{ all -> 0x009d }
        sScheduled = r12;	 Catch:{ all -> 0x009d }
        monitor-exit(r1);	 Catch:{ all -> 0x009d }
        return;
    L_0x008c:
        r0 = move-exception;
        r11 = r15;
        goto L_0x009b;
    L_0x008f:
        r0 = move-exception;
        r11 = r15;
    L_0x0091:
        monitor-exit(r18);	 Catch:{ all -> 0x0095 }
        throw r0;	 Catch:{ all -> 0x008f, all -> 0x0093 }
    L_0x0093:
        r0 = move-exception;
        goto L_0x0099;
    L_0x0095:
        r0 = move-exception;
        goto L_0x0091;
    L_0x0097:
        r0 = move-exception;
        r11 = r15;
    L_0x0099:
        r9 = r16;
    L_0x009b:
        monitor-exit(r1);	 Catch:{ all -> 0x009d }
        throw r0;
    L_0x009d:
        r0 = move-exception;
        goto L_0x009b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.KeyValueBackupJob.schedule(android.content.Context, long, com.android.server.backup.BackupManagerConstants):void");
    }

    public static void schedule(Context ctx, BackupManagerConstants constants) {
        schedule(ctx, 0, constants);
    }

    public static void cancel(Context ctx) {
        synchronized (KeyValueBackupJob.class) {
            ((JobScheduler) ctx.getSystemService("jobscheduler")).cancel(JOB_ID);
            sNextScheduled = 0;
            sScheduled = false;
        }
    }

    public static long nextScheduled() {
        long j;
        synchronized (KeyValueBackupJob.class) {
            j = sNextScheduled;
        }
        return j;
    }

    public boolean onStartJob(JobParameters params) {
        synchronized (KeyValueBackupJob.class) {
            sNextScheduled = 0;
            sScheduled = false;
        }
        try {
            BackupManagerService.getInstance().backupNow();
        } catch (RemoteException e) {
        }
        return false;
    }

    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
