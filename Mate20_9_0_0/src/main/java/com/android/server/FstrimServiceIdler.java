package com.android.server;

import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.am.ProcessList;
import com.android.server.pm.PackageManagerService;
import java.util.Calendar;

public class FstrimServiceIdler extends JobService {
    private static int FSTRIM_JOB_ID = 909;
    private static int HOUR_IN_MILLIS = ProcessList.PSS_MAX_INTERVAL;
    private static int IMMED_FSTRIM_JOB_ID = 919;
    private static int MINUTE_IN_MILLIS = 60000;
    private static int PRE_FSTRIM_JOB_ID = 809;
    private static int PRE_IMMED_FSTRIM_JOB_ID = 819;
    private static final String TAG = "FstrimServiceIdler";
    private static int mFstrimJobId = FSTRIM_JOB_ID;
    private static boolean mFstrimPending = false;
    private static boolean mScreenOn = true;
    private static ComponentName sIdleService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, FstrimServiceIdler.class.getName());
    private Runnable mFinishCallback = new Runnable() {
        public void run() {
            Slog.i(FstrimServiceIdler.TAG, "Got fstrim service completion callback");
            synchronized (FstrimServiceIdler.this.mFinishCallback) {
                if (FstrimServiceIdler.this.mStarted) {
                    FstrimServiceIdler.this.jobFinished(FstrimServiceIdler.this.mJobParams, false);
                    FstrimServiceIdler.this.mStarted = false;
                }
            }
            synchronized (FstrimServiceIdler.sIdleService) {
                FstrimServiceIdler.mFstrimPending = false;
            }
            FstrimServiceIdler.schedulePreFstrim(FstrimServiceIdler.this);
        }
    };
    private JobParameters mJobParams;
    private boolean mStarted;

    /* JADX WARNING: Missing block: B:56:0x00b4, code skipped:
            if (PRE_FSTRIM_JOB_ID != r0) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:57:0x00b6, code skipped:
            mFstrimJobId = FSTRIM_JOB_ID;
     */
    /* JADX WARNING: Missing block: B:58:0x00bb, code skipped:
            mFstrimJobId = IMMED_FSTRIM_JOB_ID;
     */
    /* JADX WARNING: Missing block: B:60:0x00c1, code skipped:
            if (mScreenOn != false) goto L_0x00c6;
     */
    /* JADX WARNING: Missing block: B:61:0x00c3, code skipped:
            scheduleFstrim(r8);
     */
    /* JADX WARNING: Missing block: B:62:0x00c6, code skipped:
            jobFinished(r8.mJobParams, false);
     */
    /* JADX WARNING: Missing block: B:63:0x00cb, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onStartJob(JobParameters params) {
        this.mJobParams = params;
        int jobId = params.getJobId();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduled Job ");
        stringBuilder.append(jobId);
        Slog.i(str, stringBuilder.toString());
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        boolean z = true;
        if (PRE_FSTRIM_JOB_ID == jobId || PRE_IMMED_FSTRIM_JOB_ID == jobId) {
            synchronized (sIdleService) {
                if (mFstrimPending) {
                    return false;
                }
                mFstrimPending = true;
            }
        } else if (FSTRIM_JOB_ID != jobId && IMMED_FSTRIM_JOB_ID != jobId) {
            return false;
        } else {
            StorageManagerService ms = StorageManagerService.sSelf;
            if (ms != null) {
                String str2;
                StringBuilder stringBuilder2;
                if (ms.lastMaintenance() + ((long) (HOUR_IN_MILLIS * 1)) > System.currentTimeMillis()) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("last maintenance was done in an hour, cancel Job ");
                    stringBuilder2.append(jobId);
                    Slog.i(str2, stringBuilder2.toString());
                    synchronized (sIdleService) {
                        mFstrimPending = false;
                    }
                    schedulePreFstrim(this);
                    return false;
                } else if (mScreenOn) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Screen is on, igore this schedule Job ");
                    stringBuilder2.append(jobId);
                    Slog.i(str2, stringBuilder2.toString());
                    return false;
                } else {
                    synchronized (this.mFinishCallback) {
                        this.mStarted = true;
                    }
                    ms.runIdleMaint(this.mFinishCallback);
                }
            }
            if (ms == null) {
                z = false;
            }
            return z;
        }
    }

    public boolean onStopJob(JobParameters params) {
        int jobId = params.getJobId();
        if (FSTRIM_JOB_ID == jobId || IMMED_FSTRIM_JOB_ID == jobId) {
            synchronized (this.mFinishCallback) {
                this.mStarted = false;
            }
        }
        return false;
    }

    public static void setScreenOn(boolean isOn) {
        mScreenOn = isOn;
    }

    public static void scheduleFstrim(Context context) {
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        if (mFstrimPending && tm != null) {
            Builder builder = new Builder(mFstrimJobId, sIdleService);
            builder.setRequiresCharging(true);
            builder.setMinimumLatency((long) (30 * MINUTE_IN_MILLIS));
            tm.schedule(builder.build());
        }
    }

    public static void cancelFstrim(Context context) {
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        if (mFstrimPending && tm != null) {
            tm.cancel(mFstrimJobId);
        }
    }

    public static void schedulePreFstrim(Context context) {
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        long timeToNight = midnight().getTimeInMillis() - System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current time is ");
        stringBuilder.append(System.currentTimeMillis());
        Slog.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("timeToNight is ");
        stringBuilder.append(timeToNight);
        Slog.i(str, stringBuilder.toString());
        if (tm != null) {
            StorageManagerService ms = StorageManagerService.sSelf;
            Builder builder = new Builder(PRE_FSTRIM_JOB_ID, sIdleService);
            builder.setRequiresCharging(true);
            builder.setMinimumLatency(timeToNight);
            tm.schedule(builder.build());
            if (ms.lastMaintenance() + ((long) (24 * HOUR_IN_MILLIS)) <= System.currentTimeMillis()) {
                builder = new Builder(PRE_IMMED_FSTRIM_JOB_ID, sIdleService);
                builder.setRequiresCharging(true);
                tm.schedule(builder.build());
                return;
            }
            tm.cancel(PRE_IMMED_FSTRIM_JOB_ID);
        }
    }

    private static Calendar midnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (calendar.get(11) >= 23) {
            calendar.add(5, 1);
        }
        calendar.set(11, 23);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }
}
