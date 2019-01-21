package com.android.server.content;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import com.android.internal.annotations.GuardedBy;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;

public class SyncJobService extends JobService {
    public static final String EXTRA_MESSENGER = "messenger";
    private static final String TAG = "SyncManager";
    @GuardedBy("mLock")
    private final SparseArray<JobParameters> mJobParamsMap = new SparseArray();
    @GuardedBy("mLock")
    private final SparseLongArray mJobStartUptimes = new SparseLongArray();
    private final Object mLock = new Object();
    private final SyncLogger mLogger = SyncLogger.getInstance();
    private Messenger mMessenger;
    @GuardedBy("mLock")
    private final SparseBooleanArray mStartedSyncs = new SparseBooleanArray();

    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mMessenger = (Messenger) intent.getParcelableExtra(EXTRA_MESSENGER);
        Message m = Message.obtain();
        m.what = 7;
        m.obj = this;
        sendMessage(m);
        return 2;
    }

    private void sendMessage(Message message) {
        if (this.mMessenger == null) {
            Slog.e("SyncManager", "Messenger not initialized.");
            return;
        }
        try {
            this.mMessenger.send(message);
        } catch (RemoteException e) {
            Slog.e("SyncManager", e.toString());
        }
    }

    public boolean onStartJob(JobParameters params) {
        this.mLogger.purgeOldLogs();
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        synchronized (this.mLock) {
            int jobId = params.getJobId();
            this.mJobParamsMap.put(jobId, params);
            this.mStartedSyncs.delete(jobId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SyncJobService->onStartJob:put job to map, jobid:");
            stringBuilder.append(params.getJobId());
            Slog.i("SyncManager", stringBuilder.toString());
            this.mJobStartUptimes.put(jobId, SystemClock.uptimeMillis());
        }
        Message m = Message.obtain();
        m.what = 10;
        SyncOperation op = SyncOperation.maybeCreateFromJobExtras(params.getExtras());
        this.mLogger.log("onStartJob() jobid=", Integer.valueOf(params.getJobId()), " op=", op);
        StringBuilder stringBuilder2;
        if (op == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Got invalid job ");
            stringBuilder2.append(params.getJobId());
            Slog.e("SyncManager", stringBuilder2.toString());
            return false;
        }
        if (isLoggable) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Got start job message ");
            stringBuilder2.append(op.target);
            Slog.v("SyncManager", stringBuilder2.toString());
        }
        m.obj = op;
        sendMessage(m);
        return true;
    }

    public boolean onStopJob(JobParameters params) {
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStopJob called ");
            stringBuilder.append(params.getJobId());
            stringBuilder.append(", reason: ");
            stringBuilder.append(params.getStopReason());
            Slog.v("SyncManager", stringBuilder.toString());
        }
        boolean readyToSync = SyncManager.readyToSync();
        SyncLogger syncLogger = this.mLogger;
        r4 = new Object[4];
        int i = 1;
        r4[1] = this.mLogger.jobParametersToString(params);
        r4[2] = " readyToSync=";
        r4[3] = Boolean.valueOf(readyToSync);
        syncLogger.log(r4);
        synchronized (this.mLock) {
            int jobId = params.getJobId();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SyncJobService->onStopJob: remove job to map, jobid:");
            stringBuilder2.append(params.getJobId());
            Slog.i("SyncManager", stringBuilder2.toString());
            this.mJobParamsMap.remove(jobId);
            long startUptime = this.mJobStartUptimes.get(jobId);
            long nowUptime = SystemClock.uptimeMillis();
            long runtime = nowUptime - startUptime;
            StringBuilder stringBuilder3;
            if (startUptime == 0) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Job ");
                stringBuilder3.append(jobId);
                stringBuilder3.append(" start uptime not found:  params=");
                stringBuilder3.append(jobParametersToString(params));
                wtf(stringBuilder3.toString());
            } else if (runtime > 60000) {
                if (readyToSync && !this.mStartedSyncs.get(jobId)) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Job ");
                    stringBuilder3.append(jobId);
                    stringBuilder3.append(" didn't start:  startUptime=");
                    stringBuilder3.append(startUptime);
                    stringBuilder3.append(" nowUptime=");
                    stringBuilder3.append(nowUptime);
                    stringBuilder3.append(" params=");
                    stringBuilder3.append(jobParametersToString(params));
                    wtf(stringBuilder3.toString());
                }
            }
            this.mStartedSyncs.delete(jobId);
            this.mJobStartUptimes.delete(jobId);
        }
        Message m = Message.obtain();
        m.what = 11;
        m.obj = SyncOperation.maybeCreateFromJobExtras(params.getExtras());
        if (m.obj == null) {
            return false;
        }
        m.arg1 = params.getStopReason() != 0 ? 1 : 0;
        if (params.getStopReason() != 3) {
            i = 0;
        }
        m.arg2 = i;
        sendMessage(m);
        return false;
    }

    public void callJobFinished(int jobId, boolean needsReschedule, String why) {
        synchronized (this.mLock) {
            JobParameters params = (JobParameters) this.mJobParamsMap.get(jobId);
            this.mLogger.log("callJobFinished()", " jobid=", Integer.valueOf(jobId), " needsReschedule=", Boolean.valueOf(needsReschedule), " ", this.mLogger.jobParametersToString(params), " why=", why);
            StringBuilder stringBuilder;
            if (params != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("SyncJobService->callJobFinished: remove job to map, jobid:");
                stringBuilder.append(jobId);
                Slog.i("SyncManager", stringBuilder.toString());
                jobFinished(params, needsReschedule);
                this.mJobParamsMap.remove(jobId);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Job params not found for ");
                stringBuilder.append(String.valueOf(jobId));
                Slog.e("SyncManager", stringBuilder.toString());
            }
        }
    }

    public void markSyncStarted(int jobId) {
        synchronized (this.mLock) {
            this.mStartedSyncs.put(jobId, true);
        }
    }

    public static String jobParametersToString(JobParameters params) {
        if (params == null) {
            return "job:null";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("job:#");
        stringBuilder.append(params.getJobId());
        stringBuilder.append(":sr=[");
        stringBuilder.append(params.getStopReason());
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(params.getDebugStopReason());
        stringBuilder.append("]:");
        stringBuilder.append(SyncOperation.maybeCreateFromJobExtras(params.getExtras()));
        return stringBuilder.toString();
    }

    private void wtf(String message) {
        this.mLogger.log(message);
        Slog.wtf("SyncManager", message);
    }
}
