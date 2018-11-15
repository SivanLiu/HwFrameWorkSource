package com.android.server.job;

import android.app.ActivityManager;
import android.app.job.IJobCallback.Stub;
import android.app.job.IJobService;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;

public final class JobServiceContext implements ServiceConnection {
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final boolean DEBUG_STANDBY = JobSchedulerService.DEBUG_STANDBY;
    public static final long EXECUTING_TIMESLICE_MILLIS = 600000;
    private static final int MSG_TIMEOUT = 0;
    public static final int NO_PREFERRED_UID = -1;
    private static final long OP_BIND_TIMEOUT_MILLIS = 18000;
    private static final long OP_TIMEOUT_MILLIS = 8000;
    private static final String TAG = "JobServiceContext";
    static final int VERB_BINDING = 0;
    static final int VERB_EXECUTING = 2;
    static final int VERB_FINISHED = 4;
    static final int VERB_STARTING = 1;
    static final int VERB_STOPPING = 3;
    private static final String[] VERB_STRINGS = new String[]{"VERB_BINDING", "VERB_STARTING", "VERB_EXECUTING", "VERB_STOPPING", "VERB_FINISHED"};
    @GuardedBy("mLock")
    private boolean mAvailable;
    private final IBatteryStats mBatteryStats;
    private final Handler mCallbackHandler;
    private boolean mCancelled;
    private final JobCompletedListener mCompletedListener;
    private final Context mContext;
    private long mExecutionStartTimeElapsed;
    private final JobPackageTracker mJobPackageTracker;
    private final Object mLock;
    private JobParameters mParams;
    private int mPreferredUid;
    private JobCallback mRunningCallback;
    private JobStatus mRunningJob;
    public String mStoppedReason;
    public long mStoppedTime;
    private long mTimeoutElapsed;
    @VisibleForTesting
    int mVerb;
    private WakeLock mWakeLock;
    IJobService service;

    final class JobCallback extends Stub {
        public String mStoppedReason;
        public long mStoppedTime;

        JobCallback() {
        }

        public void acknowledgeStartMessage(int jobId, boolean ongoing) {
            JobServiceContext.this.doAcknowledgeStartMessage(this, jobId, ongoing);
        }

        public void acknowledgeStopMessage(int jobId, boolean reschedule) {
            JobServiceContext.this.doAcknowledgeStopMessage(this, jobId, reschedule);
        }

        public JobWorkItem dequeueWork(int jobId) {
            return JobServiceContext.this.doDequeueWork(this, jobId);
        }

        public boolean completeWork(int jobId, int workId) {
            return JobServiceContext.this.doCompleteWork(this, jobId, workId);
        }

        public void jobFinished(int jobId, boolean reschedule) {
            JobServiceContext.this.doJobFinished(this, jobId, reschedule);
        }
    }

    private class JobServiceHandler extends Handler {
        JobServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            if (message.what != 0) {
                String str = JobServiceContext.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognised message: ");
                stringBuilder.append(message);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            synchronized (JobServiceContext.this.mLock) {
                if (message.obj == JobServiceContext.this.mRunningCallback) {
                    JobServiceContext.this.handleOpTimeoutLocked();
                } else {
                    JobCallback jc = message.obj;
                    StringBuilder sb = new StringBuilder(128);
                    sb.append("Ignoring timeout of no longer active job");
                    if (jc.mStoppedReason != null) {
                        sb.append(", stopped ");
                        TimeUtils.formatDuration(JobSchedulerService.sElapsedRealtimeClock.millis() - jc.mStoppedTime, sb);
                        sb.append(" because: ");
                        sb.append(jc.mStoppedReason);
                    }
                    Slog.w(JobServiceContext.TAG, sb.toString());
                }
            }
        }
    }

    JobServiceContext(JobSchedulerService service, IBatteryStats batteryStats, JobPackageTracker tracker, Looper looper) {
        this(service.getContext(), service.getLock(), batteryStats, tracker, service, looper);
    }

    @VisibleForTesting
    JobServiceContext(Context context, Object lock, IBatteryStats batteryStats, JobPackageTracker tracker, JobCompletedListener completedListener, Looper looper) {
        this.mContext = context;
        this.mLock = lock;
        this.mBatteryStats = batteryStats;
        this.mJobPackageTracker = tracker;
        this.mCallbackHandler = new JobServiceHandler(looper);
        this.mCompletedListener = completedListener;
        this.mAvailable = true;
        this.mVerb = 4;
        this.mPreferredUid = -1;
    }

    boolean executeRunnableJob(JobStatus job) {
        JobStatus jobStatus = job;
        synchronized (this.mLock) {
            if (this.mAvailable) {
                this.mPreferredUid = -1;
                this.mRunningJob = jobStatus;
                this.mRunningCallback = new JobCallback();
                boolean isDeadlineExpired = job.hasDeadlineConstraint() && job.getLatestRunTimeElapsed() < JobSchedulerService.sElapsedRealtimeClock.millis();
                Uri[] triggeredUris = null;
                if (jobStatus.changedUris != null) {
                    triggeredUris = new Uri[jobStatus.changedUris.size()];
                    jobStatus.changedUris.toArray(triggeredUris);
                }
                Uri[] triggeredUris2 = triggeredUris;
                String[] triggeredAuthorities = null;
                if (jobStatus.changedAuthorities != null) {
                    triggeredAuthorities = new String[jobStatus.changedAuthorities.size()];
                    jobStatus.changedAuthorities.toArray(triggeredAuthorities);
                }
                String[] triggeredAuthorities2 = triggeredAuthorities;
                JobInfo ji = job.getJob();
                this.mParams = new JobParameters(this.mRunningCallback, job.getJobId(), ji.getExtras(), ji.getTransientExtras(), ji.getClipData(), ji.getClipGrantFlags(), isDeadlineExpired, triggeredUris2, triggeredAuthorities2, jobStatus.network);
                this.mExecutionStartTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                long whenDeferred = job.getWhenStandbyDeferred();
                if (whenDeferred > 0) {
                    long deferral = this.mExecutionStartTimeElapsed - whenDeferred;
                    EventLog.writeEvent(EventLogTags.JOB_DEFERRED_EXECUTION, deferral);
                    if (DEBUG_STANDBY) {
                        StringBuilder sb = new StringBuilder(128);
                        sb.append("Starting job deferred for standby by ");
                        TimeUtils.formatDuration(deferral, sb);
                        sb.append(" ms : ");
                        sb.append(job.toShortString());
                        Slog.v(TAG, sb.toString());
                    }
                }
                job.clearPersistedUtcTimes();
                this.mVerb = 0;
                scheduleOpTimeOutLocked();
                Intent intent = new Intent().setComponent(job.getServiceComponent());
                intent.addHwFlags(32);
                if (this.mContext.bindServiceAsUser(intent, this, 5, new UserHandle(job.getUserId()))) {
                    this.mJobPackageTracker.noteActive(jobStatus);
                    try {
                        this.mBatteryStats.noteJobStart(job.getBatteryName(), job.getSourceUid());
                    } catch (RemoteException e) {
                    }
                    String jobPackage = job.getSourcePackageName();
                    int jobUserId = job.getSourceUserId();
                    ((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)).setLastJobRunTime(jobPackage, jobUserId, this.mExecutionStartTimeElapsed);
                    ((JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class)).noteJobStart(jobPackage, jobUserId);
                    this.mAvailable = false;
                    this.mStoppedReason = null;
                    this.mStoppedTime = 0;
                    return true;
                }
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(job.getServiceComponent().getShortClassName());
                    stringBuilder.append(" unavailable.");
                    Slog.d(str, stringBuilder.toString());
                }
                if ((32 & intent.getHwFlags()) == 0) {
                    return false;
                }
                this.mRunningJob = null;
                this.mRunningCallback = null;
                this.mParams = null;
                this.mExecutionStartTimeElapsed = 0;
                this.mVerb = 4;
                removeOpTimeOutLocked();
                return false;
            }
            Slog.e(TAG, "Starting new runnable but context is unavailable > Error.");
            return false;
        }
    }

    JobStatus getRunningJobLocked() {
        return this.mRunningJob;
    }

    private String getRunningJobNameLocked() {
        return this.mRunningJob != null ? this.mRunningJob.toShortString() : "<null>";
    }

    @GuardedBy("mLock")
    void cancelExecutingJobLocked(int reason, String debugReason) {
        doCancelLocked(reason, debugReason);
    }

    @GuardedBy("mLock")
    void preemptExecutingJobLocked() {
        doCancelLocked(2, "cancelled due to preemption");
    }

    int getPreferredUid() {
        return this.mPreferredUid;
    }

    void clearPreferredUid() {
        this.mPreferredUid = -1;
    }

    long getExecutionStartTimeElapsed() {
        return this.mExecutionStartTimeElapsed;
    }

    long getTimeoutElapsed() {
        return this.mTimeoutElapsed;
    }

    @GuardedBy("mLock")
    boolean timeoutIfExecutingLocked(String pkgName, int userId, boolean matchJobId, int jobId, String reason) {
        JobStatus executing = getRunningJobLocked();
        if (executing == null || ((userId != -1 && userId != executing.getUserId()) || ((pkgName != null && !pkgName.equals(executing.getSourcePackageName())) || ((matchJobId && jobId != executing.getJobId()) || this.mVerb != 2)))) {
            return false;
        }
        this.mParams.setStopReason(3, reason);
        sendStopMessageLocked("force timeout from shell");
        return true;
    }

    void doJobFinished(JobCallback cb, int jobId, boolean reschedule) {
        doCallback(cb, reschedule, "app called jobFinished");
    }

    void doAcknowledgeStopMessage(JobCallback cb, int jobId, boolean reschedule) {
        doCallback(cb, reschedule, null);
    }

    void doAcknowledgeStartMessage(JobCallback cb, int jobId, boolean ongoing) {
        doCallback(cb, ongoing, "finished start");
    }

    /* JADX WARNING: Missing block: B:15:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARNING: Missing block: B:16:0x0030, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    JobWorkItem doDequeueWork(JobCallback cb, int jobId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                assertCallerLocked(cb);
                if (this.mVerb == 3 || this.mVerb == 4) {
                    Binder.restoreCallingIdentity(ident);
                    return null;
                }
                JobWorkItem work = this.mRunningJob.dequeueWorkLocked();
                if (work == null && !this.mRunningJob.hasExecutingWorkLocked()) {
                    doCallbackLocked(false, "last work dequeued");
                }
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean doCompleteWork(JobCallback cb, int jobId, int workId) {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean completeWorkLocked;
            synchronized (this.mLock) {
                assertCallerLocked(cb);
                completeWorkLocked = this.mRunningJob.completeWorkLocked(ActivityManager.getService(), workId);
            }
            Binder.restoreCallingIdentity(ident);
            return completeWorkLocked;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mLock) {
            JobStatus runningJob = this.mRunningJob;
            if (runningJob == null || !name.equals(runningJob.getServiceComponent())) {
                closeAndCleanupJobLocked(true, "connected for different component");
                return;
            }
            this.service = IJobService.Stub.asInterface(service);
            WakeLock wl = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, runningJob.getTag());
            wl.setWorkSource(deriveWorkSource(runningJob));
            wl.setReferenceCounted(false);
            wl.acquire();
            if (this.mWakeLock != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bound new job ");
                stringBuilder.append(runningJob);
                stringBuilder.append(" but live wakelock ");
                stringBuilder.append(this.mWakeLock);
                stringBuilder.append(" tag=");
                stringBuilder.append(this.mWakeLock.getTag());
                Slog.w(str, stringBuilder.toString());
                this.mWakeLock.release();
            }
            this.mWakeLock = wl;
            doServiceBoundLocked();
        }
    }

    private WorkSource deriveWorkSource(JobStatus runningJob) {
        int jobUid = runningJob.getSourceUid();
        if (!WorkSource.isChainedBatteryAttributionEnabled(this.mContext)) {
            return new WorkSource(jobUid);
        }
        WorkSource workSource = new WorkSource();
        workSource.createWorkChain().addNode(jobUid, null).addNode(1000, JobSchedulerService.TAG);
        return workSource;
    }

    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mLock) {
            closeAndCleanupJobLocked(true, "unexpectedly disconnected");
        }
    }

    private boolean verifyCallerLocked(JobCallback cb) {
        if (this.mRunningCallback == cb) {
            return true;
        }
        if (DEBUG) {
            Slog.d(TAG, "Stale callback received, ignoring.");
        }
        return false;
    }

    private void assertCallerLocked(JobCallback cb) {
        if (!verifyCallerLocked(cb)) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Caller no longer running");
            if (cb.mStoppedReason != null) {
                sb.append(", last stopped ");
                TimeUtils.formatDuration(JobSchedulerService.sElapsedRealtimeClock.millis() - cb.mStoppedTime, sb);
                sb.append(" because: ");
                sb.append(cb.mStoppedReason);
            }
            throw new SecurityException(sb.toString());
        }
    }

    @GuardedBy("mLock")
    void doServiceBoundLocked() {
        removeOpTimeOutLocked();
        handleServiceBoundLocked();
    }

    void doCallback(JobCallback cb, boolean reschedule, String reason) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (verifyCallerLocked(cb)) {
                    doCallbackLocked(reschedule, reason);
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
                Binder.restoreCallingIdentity(ident);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    @GuardedBy("mLock")
    void doCallbackLocked(boolean reschedule, String reason) {
        String str;
        StringBuilder stringBuilder;
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("doCallback of : ");
            stringBuilder.append(this.mRunningJob);
            stringBuilder.append(" v:");
            stringBuilder.append(VERB_STRINGS[this.mVerb]);
            Slog.d(str, stringBuilder.toString());
        }
        removeOpTimeOutLocked();
        if (this.mVerb == 1) {
            handleStartedLocked(reschedule);
        } else if (this.mVerb == 2 || this.mVerb == 3) {
            handleFinishedLocked(reschedule, reason);
        } else if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unrecognised callback: ");
            stringBuilder.append(this.mRunningJob);
            Slog.d(str, stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    void doCancelLocked(int arg1, String debugReason) {
        if (this.mVerb == 4) {
            if (DEBUG) {
                Slog.d(TAG, "Trying to process cancel for torn-down context, ignoring.");
            }
            return;
        }
        this.mParams.setStopReason(arg1, debugReason);
        if (arg1 == 2) {
            int uid;
            if (this.mRunningJob != null) {
                uid = this.mRunningJob.getUid();
            } else {
                uid = -1;
            }
            this.mPreferredUid = uid;
        }
        handleCancelLocked(debugReason);
    }

    @GuardedBy("mLock")
    private void handleServiceBoundLocked() {
        String str;
        StringBuilder stringBuilder;
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleServiceBound for ");
            stringBuilder.append(getRunningJobNameLocked());
            Slog.d(str, stringBuilder.toString());
        }
        StringBuilder stringBuilder2;
        if (this.mVerb != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Sending onStartJob for a job that isn't pending. ");
            stringBuilder.append(VERB_STRINGS[this.mVerb]);
            Slog.e(str, stringBuilder.toString());
            closeAndCleanupJobLocked(false, "started job not pending");
        } else if (this.mCancelled) {
            if (DEBUG) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Job cancelled while waiting for bind to complete. ");
                stringBuilder2.append(this.mRunningJob);
                Slog.d(str, stringBuilder2.toString());
            }
            closeAndCleanupJobLocked(true, "cancelled while waiting for bind");
        } else {
            try {
                this.mVerb = 1;
                scheduleOpTimeOutLocked();
                this.service.startJob(this.mParams);
            } catch (Exception e) {
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error sending onStart message to '");
                stringBuilder2.append(this.mRunningJob.getServiceComponent().getShortClassName());
                stringBuilder2.append("' ");
                Slog.e(str2, stringBuilder2.toString(), e);
            }
        }
    }

    @GuardedBy("mLock")
    private void handleStartedLocked(boolean workOngoing) {
        if (this.mVerb != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Handling started job but job wasn't starting! Was ");
            stringBuilder.append(VERB_STRINGS[this.mVerb]);
            stringBuilder.append(".");
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mVerb = 2;
        if (!workOngoing) {
            handleFinishedLocked(false, "onStartJob returned false");
        } else if (this.mCancelled) {
            if (DEBUG) {
                Slog.d(TAG, "Job cancelled while waiting for onStartJob to complete.");
            }
            handleCancelLocked(null);
        } else {
            scheduleOpTimeOutLocked();
        }
    }

    @GuardedBy("mLock")
    private void handleFinishedLocked(boolean reschedule, String reason) {
        switch (this.mVerb) {
            case 2:
            case 3:
                closeAndCleanupJobLocked(reschedule, reason);
                return;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got an execution complete message for a job that wasn't beingexecuted. Was ");
                stringBuilder.append(VERB_STRINGS[this.mVerb]);
                stringBuilder.append(".");
                Slog.e(str, stringBuilder.toString());
                return;
        }
    }

    @GuardedBy("mLock")
    private void handleCancelLocked(String reason) {
        String str;
        StringBuilder stringBuilder;
        if (JobSchedulerService.DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Handling cancel for: ");
            stringBuilder.append(this.mRunningJob.getJobId());
            stringBuilder.append(" ");
            stringBuilder.append(VERB_STRINGS[this.mVerb]);
            Slog.d(str, stringBuilder.toString());
        }
        switch (this.mVerb) {
            case 0:
            case 1:
                this.mCancelled = true;
                applyStoppedReasonLocked(reason);
                return;
            case 2:
                sendStopMessageLocked(reason);
                return;
            case 3:
                return;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cancelling a job without a valid verb: ");
                stringBuilder.append(this.mVerb);
                Slog.e(str, stringBuilder.toString());
                return;
        }
    }

    @GuardedBy("mLock")
    private void handleOpTimeoutLocked() {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        switch (this.mVerb) {
            case 0:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Time-out while trying to bind ");
                stringBuilder.append(getRunningJobNameLocked());
                stringBuilder.append(", dropping.");
                Slog.w(str, stringBuilder.toString());
                closeAndCleanupJobLocked(false, "timed out while binding");
                return;
            case 1:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No response from client for onStartJob ");
                stringBuilder.append(getRunningJobNameLocked());
                Slog.w(str, stringBuilder.toString());
                closeAndCleanupJobLocked(false, "timed out while starting");
                return;
            case 2:
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Client timed out while executing (no jobFinished received), sending onStop: ");
                stringBuilder2.append(getRunningJobNameLocked());
                Slog.i(str, stringBuilder2.toString());
                this.mParams.setStopReason(3, "client timed out");
                sendStopMessageLocked("timeout while executing");
                return;
            case 3:
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No response from client for onStopJob ");
                stringBuilder2.append(getRunningJobNameLocked());
                Slog.w(str, stringBuilder2.toString());
                closeAndCleanupJobLocked(true, "timed out while stopping");
                return;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Handling timeout for an invalid job state: ");
                stringBuilder.append(getRunningJobNameLocked());
                stringBuilder.append(", dropping.");
                Slog.e(str, stringBuilder.toString());
                closeAndCleanupJobLocked(false, "invalid timeout");
                return;
        }
    }

    @GuardedBy("mLock")
    private void sendStopMessageLocked(String reason) {
        removeOpTimeOutLocked();
        if (this.mVerb != 2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Sending onStopJob for a job that isn't started. ");
            stringBuilder.append(this.mRunningJob);
            Slog.e(str, stringBuilder.toString());
            closeAndCleanupJobLocked(false, reason);
            return;
        }
        try {
            applyStoppedReasonLocked(reason);
            this.mVerb = 3;
            scheduleOpTimeOutLocked();
            this.service.stopJob(this.mParams);
        } catch (RemoteException e) {
            Slog.e(TAG, "Error sending onStopJob to client.", e);
            closeAndCleanupJobLocked(true, "host crashed when trying to stop");
        }
    }

    @GuardedBy("mLock")
    private void closeAndCleanupJobLocked(boolean reschedule, String reason) {
        if (this.mVerb != 4) {
            applyStoppedReasonLocked(reason);
            JobStatus completedJob = this.mRunningJob;
            this.mJobPackageTracker.noteInactive(completedJob, this.mParams.getStopReason(), reason);
            try {
                if (this.mRunningJob != null) {
                    this.mBatteryStats.noteJobFinish(this.mRunningJob.getBatteryName(), this.mRunningJob.getSourceUid(), this.mParams.getStopReason());
                }
            } catch (RemoteException e) {
            }
            if (this.mWakeLock != null) {
                this.mWakeLock.release();
            }
            try {
                this.mContext.unbindService(this);
            } catch (Exception e2) {
                Slog.e(TAG, "Service not bind: JobServiceContext");
            }
            this.mWakeLock = null;
            this.mRunningJob = null;
            this.mRunningCallback = null;
            this.mParams = null;
            this.mVerb = 4;
            this.mCancelled = false;
            this.service = null;
            this.mAvailable = true;
            removeOpTimeOutLocked();
            this.mCompletedListener.onJobCompletedLocked(completedJob, reschedule);
        }
    }

    private void applyStoppedReasonLocked(String reason) {
        if (reason != null && this.mStoppedReason == null) {
            this.mStoppedReason = reason;
            this.mStoppedTime = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (this.mRunningCallback != null) {
                this.mRunningCallback.mStoppedReason = this.mStoppedReason;
                this.mRunningCallback.mStoppedTime = this.mStoppedTime;
            }
        }
    }

    private void scheduleOpTimeOutLocked() {
        long timeoutMillis;
        removeOpTimeOutLocked();
        int i = this.mVerb;
        if (i == 0) {
            timeoutMillis = OP_BIND_TIMEOUT_MILLIS;
        } else if (i != 2) {
            timeoutMillis = OP_TIMEOUT_MILLIS;
        } else {
            timeoutMillis = 600000;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scheduling time out for '");
            stringBuilder.append(this.mRunningJob.getServiceComponent().getShortClassName());
            stringBuilder.append("' jId: ");
            stringBuilder.append(this.mParams.getJobId());
            stringBuilder.append(", in ");
            stringBuilder.append(timeoutMillis / 1000);
            stringBuilder.append(" s");
            Slog.d(str, stringBuilder.toString());
        }
        this.mCallbackHandler.sendMessageDelayed(this.mCallbackHandler.obtainMessage(0, this.mRunningCallback), timeoutMillis);
        this.mTimeoutElapsed = JobSchedulerService.sElapsedRealtimeClock.millis() + timeoutMillis;
    }

    private void removeOpTimeOutLocked() {
        this.mCallbackHandler.removeMessages(0);
    }
}
