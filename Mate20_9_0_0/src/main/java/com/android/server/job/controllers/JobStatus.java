package com.android.server.job.controllers;

import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobInfo.TriggerContentUri;
import android.app.job.JobWorkItem;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.net.Network;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.format.Time;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.job.GrantedUriPermissions;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobSchedulerService;
import com.android.server.lights.LightsManager;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public final class JobStatus {
    static final int CONSTRAINTS_OF_INTEREST = -1811939057;
    static final int CONSTRAINT_BACKGROUND_NOT_RESTRICTED = 4194304;
    static final int CONSTRAINT_BATTERY_NOT_LOW = 2;
    static final int CONSTRAINT_CHARGING = 1;
    static final int CONSTRAINT_CONNECTIVITY = 268435456;
    static final int CONSTRAINT_CONTENT_TRIGGER = 67108864;
    static final int CONSTRAINT_DEADLINE = 1073741824;
    static final int CONSTRAINT_DEVICE_NOT_DOZING = 33554432;
    static final int CONSTRAINT_IDLE = 4;
    static final int CONSTRAINT_STORAGE_NOT_LOW = 8;
    static final int CONSTRAINT_TIMING_DELAY = Integer.MIN_VALUE;
    static final boolean DEBUG = JobSchedulerService.DEBUG;
    static final boolean DEBUG_PREPARE = true;
    public static final long DEFAULT_TRIGGER_MAX_DELAY = 120000;
    public static final long DEFAULT_TRIGGER_UPDATE_DELAY = 10000;
    static final int HW_CONSTRAINT_BATTERY_LEV_JOB_ALLOWED = 256;
    public static final int INTERNAL_FLAG_HAS_FOREGROUND_EXEMPTION = 1;
    public static final long MIN_TRIGGER_MAX_DELAY = 1000;
    public static final long MIN_TRIGGER_UPDATE_DELAY = 500;
    public static final long NO_EARLIEST_RUNTIME = 0;
    public static final long NO_LATEST_RUNTIME = Long.MAX_VALUE;
    public static final int OVERRIDE_FULL = 2;
    public static final int OVERRIDE_SOFT = 1;
    static final int SOFT_OVERRIDE_CONSTRAINTS = -2147483633;
    static final String TAG = "JobSchedulerService";
    public static final int TRACKING_BATTERY = 1;
    public static final int TRACKING_CONNECTIVITY = 2;
    public static final int TRACKING_CONTENT = 4;
    public static final int TRACKING_IDLE = 8;
    public static final int TRACKING_STORAGE = 16;
    public static final int TRACKING_TIME = 32;
    private final long baseHeartbeat;
    final String batteryName;
    final int callingUid;
    public ArraySet<String> changedAuthorities;
    public ArraySet<Uri> changedUris;
    JobInstance contentObserverJobInstance;
    public boolean dozeWhitelisted;
    private final long earliestRunTimeElapsedMillis;
    public long enqueueTime;
    public ArrayList<JobWorkItem> executingWork;
    final JobInfo job;
    public int lastEvaluatedPriority;
    private final long latestRunTimeElapsedMillis;
    private int mInternalFlags;
    private long mLastFailedRunTime;
    private long mLastSuccessfulRunTime;
    private Pair<Long, Long> mPersistedUtcTimes;
    public long madeActive;
    public long madePending;
    public Network network;
    public int nextPendingWorkId;
    private final int numFailures;
    public int overrideState;
    public ArrayList<JobWorkItem> pendingWork;
    private boolean prepared;
    final int requiredConstraints;
    int satisfiedConstraints;
    final String sourcePackageName;
    final String sourceTag;
    final int sourceUid;
    final int sourceUserId;
    private int standbyBucket;
    final String tag;
    final int targetSdkVersion;
    private long totalNetworkBytes;
    private int trackingControllers;
    public boolean uidActive;
    private Throwable unpreparedPoint;
    private GrantedUriPermissions uriPerms;
    private long whenStandbyDeferred;

    public int getServiceToken() {
        return this.callingUid;
    }

    private JobStatus(JobInfo job, int callingUid, int targetSdkVersion, String sourcePackageName, int sourceUserId, int standbyBucket, long heartbeat, String tag, int numFailures, long earliestRunTimeElapsedMillis, long latestRunTimeElapsedMillis, long lastSuccessfulRunTime, long lastFailedRunTime, int internalFlags) {
        StringBuilder stringBuilder;
        String stringBuilder2;
        int i = callingUid;
        String str = sourcePackageName;
        int i2 = sourceUserId;
        long j = earliestRunTimeElapsedMillis;
        long j2 = latestRunTimeElapsedMillis;
        this.unpreparedPoint = null;
        this.satisfiedConstraints = 0;
        this.nextPendingWorkId = 1;
        this.overrideState = 0;
        this.totalNetworkBytes = -1;
        this.job = job;
        this.callingUid = i;
        this.targetSdkVersion = targetSdkVersion;
        this.standbyBucket = standbyBucket;
        this.baseHeartbeat = heartbeat;
        int tempSourceUid = -1;
        if (!(i2 == -1 || str == null)) {
            try {
                tempSourceUid = AppGlobals.getPackageManager().getPackageUid(str, 0, i2);
            } catch (RemoteException e) {
            }
        }
        if (tempSourceUid == -1) {
            this.sourceUid = i;
            this.sourceUserId = UserHandle.getUserId(callingUid);
            this.sourcePackageName = job.getService().getPackageName();
            this.sourceTag = null;
            String str2 = tag;
        } else {
            this.sourceUid = tempSourceUid;
            this.sourceUserId = i2;
            this.sourcePackageName = str;
            this.sourceTag = tag;
        }
        if (this.sourceTag != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.sourceTag);
            stringBuilder.append(":");
            stringBuilder.append(job.getService().getPackageName());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = job.getService().flattenToShortString();
        }
        this.batteryName = stringBuilder2;
        stringBuilder = new StringBuilder();
        stringBuilder.append("*job*/");
        stringBuilder.append(this.batteryName);
        this.tag = stringBuilder.toString();
        this.earliestRunTimeElapsedMillis = j;
        this.latestRunTimeElapsedMillis = j2;
        this.numFailures = numFailures;
        int requiredConstraints = job.getConstraintFlags();
        if (job.getRequiredNetwork() != null) {
            requiredConstraints |= CONSTRAINT_CONNECTIVITY;
        }
        if (j != 0) {
            requiredConstraints |= Integer.MIN_VALUE;
        }
        if (j2 != NO_LATEST_RUNTIME) {
            requiredConstraints |= CONSTRAINT_DEADLINE;
        }
        if (job.getTriggerContentUris() != null) {
            requiredConstraints |= CONSTRAINT_CONTENT_TRIGGER;
        }
        this.requiredConstraints = requiredConstraints;
        this.mLastSuccessfulRunTime = lastSuccessfulRunTime;
        this.mLastFailedRunTime = lastFailedRunTime;
        this.mInternalFlags = internalFlags;
        updateEstimatedNetworkBytesLocked();
        if (job.getRequiredNetwork() != null) {
            job.getRequiredNetwork().networkCapabilities.setSingleUid(this.sourceUid);
            return;
        }
    }

    public JobStatus(JobStatus jobStatus) {
        JobStatus jobStatus2 = jobStatus;
        this(jobStatus.getJob(), jobStatus.getUid(), jobStatus2.targetSdkVersion, jobStatus.getSourcePackageName(), jobStatus.getSourceUserId(), jobStatus.getStandbyBucket(), jobStatus.getBaseHeartbeat(), jobStatus.getSourceTag(), jobStatus.getNumFailures(), jobStatus.getEarliestRunTime(), jobStatus.getLatestRunTimeElapsed(), jobStatus.getLastSuccessfulRunTime(), jobStatus.getLastFailedRunTime(), jobStatus.getInternalFlags());
        this.mPersistedUtcTimes = jobStatus2.mPersistedUtcTimes;
        if (jobStatus2.mPersistedUtcTimes != null && DEBUG) {
            Slog.i(TAG, "Cloning job with persisted run times", new RuntimeException("here"));
        }
    }

    public JobStatus(JobInfo job, int callingUid, String sourcePkgName, int sourceUserId, int standbyBucket, long baseHeartbeat, String sourceTag, long earliestRunTimeElapsedMillis, long latestRunTimeElapsedMillis, long lastSuccessfulRunTime, long lastFailedRunTime, Pair<Long, Long> persistedExecutionTimesUTC, int innerFlags) {
        Pair<Long, Long> pair = persistedExecutionTimesUTC;
        this(job, callingUid, resolveTargetSdkVersion(job), sourcePkgName, sourceUserId, standbyBucket, baseHeartbeat, sourceTag, 0, earliestRunTimeElapsedMillis, latestRunTimeElapsedMillis, lastSuccessfulRunTime, lastFailedRunTime, innerFlags);
        this.mPersistedUtcTimes = pair;
        if (pair != null && DEBUG) {
            Slog.i(TAG, "+ restored job with RTC times because of bad boot clock");
        }
    }

    public JobStatus(JobStatus rescheduling, long newBaseHeartbeat, long newEarliestRuntimeElapsedMillis, long newLatestRuntimeElapsedMillis, int backoffAttempt, long lastSuccessfulRunTime, long lastFailedRunTime) {
        JobStatus jobStatus = rescheduling;
        this(jobStatus.job, rescheduling.getUid(), resolveTargetSdkVersion(jobStatus.job), rescheduling.getSourcePackageName(), rescheduling.getSourceUserId(), rescheduling.getStandbyBucket(), newBaseHeartbeat, rescheduling.getSourceTag(), backoffAttempt, newEarliestRuntimeElapsedMillis, newLatestRuntimeElapsedMillis, lastSuccessfulRunTime, lastFailedRunTime, rescheduling.getInternalFlags());
    }

    public static JobStatus createFromJobInfo(JobInfo job, int callingUid, String sourcePkg, int sourceUserId, String tag) {
        long latestRunTimeElapsedMillis;
        long earliestRunTimeElapsedMillis;
        long currentHeartbeat;
        int i = sourceUserId;
        long elapsedNow = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (job.isPeriodic()) {
            long latestRunTimeElapsedMillis2 = job.getIntervalMillis() + elapsedNow;
            latestRunTimeElapsedMillis = latestRunTimeElapsedMillis2;
            earliestRunTimeElapsedMillis = latestRunTimeElapsedMillis2 - job.getFlexMillis();
        } else {
            earliestRunTimeElapsedMillis = job.hasEarlyConstraint() ? job.getMinLatencyMillis() + elapsedNow : 0;
            latestRunTimeElapsedMillis = job.hasLateConstraint() ? job.getMaxExecutionDelayMillis() + elapsedNow : NO_LATEST_RUNTIME;
        }
        String jobPackage = sourcePkg != null ? sourcePkg : job.getService().getPackageName();
        int standbyBucket = JobSchedulerService.standbyBucketForPackage(jobPackage, i, elapsedNow);
        JobSchedulerInternal js = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        if (js != null) {
            currentHeartbeat = js.baseHeartbeatForApp(jobPackage, i, standbyBucket);
        } else {
            currentHeartbeat = 0;
        }
        return new JobStatus(job, callingUid, resolveTargetSdkVersion(job), sourcePkg, i, standbyBucket, currentHeartbeat, tag, 0, earliestRunTimeElapsedMillis, latestRunTimeElapsedMillis, 0, 0, 0);
    }

    public void enqueueWorkLocked(IActivityManager am, JobWorkItem work) {
        if (this.pendingWork == null) {
            this.pendingWork = new ArrayList();
        }
        work.setWorkId(this.nextPendingWorkId);
        this.nextPendingWorkId++;
        if (work.getIntent() != null && GrantedUriPermissions.checkGrantFlags(work.getIntent().getFlags())) {
            work.setGrants(GrantedUriPermissions.createFromIntent(am, work.getIntent(), this.sourceUid, this.sourcePackageName, this.sourceUserId, toShortString()));
        }
        this.pendingWork.add(work);
        updateEstimatedNetworkBytesLocked();
    }

    public JobWorkItem dequeueWorkLocked() {
        if (this.pendingWork == null || this.pendingWork.size() <= 0) {
            return null;
        }
        JobWorkItem work = (JobWorkItem) this.pendingWork.remove(0);
        if (work != null) {
            if (this.executingWork == null) {
                this.executingWork = new ArrayList();
            }
            this.executingWork.add(work);
            work.bumpDeliveryCount();
        }
        updateEstimatedNetworkBytesLocked();
        return work;
    }

    public boolean hasWorkLocked() {
        return (this.pendingWork != null && this.pendingWork.size() > 0) || hasExecutingWorkLocked();
    }

    public boolean hasExecutingWorkLocked() {
        return this.executingWork != null && this.executingWork.size() > 0;
    }

    private static void ungrantWorkItem(IActivityManager am, JobWorkItem work) {
        if (work.getGrants() != null) {
            ((GrantedUriPermissions) work.getGrants()).revoke(am);
        }
    }

    public boolean completeWorkLocked(IActivityManager am, int workId) {
        if (this.executingWork != null) {
            int N = this.executingWork.size();
            for (int i = 0; i < N; i++) {
                JobWorkItem work = (JobWorkItem) this.executingWork.get(i);
                if (work.getWorkId() == workId) {
                    this.executingWork.remove(i);
                    ungrantWorkItem(am, work);
                    return true;
                }
            }
        }
        return false;
    }

    private static void ungrantWorkList(IActivityManager am, ArrayList<JobWorkItem> list) {
        if (list != null) {
            int N = list.size();
            for (int i = 0; i < N; i++) {
                ungrantWorkItem(am, (JobWorkItem) list.get(i));
            }
        }
    }

    public void stopTrackingJobLocked(IActivityManager am, JobStatus incomingJob) {
        if (incomingJob != null) {
            if (this.executingWork != null && this.executingWork.size() > 0) {
                incomingJob.pendingWork = this.executingWork;
            }
            if (incomingJob.pendingWork == null) {
                incomingJob.pendingWork = this.pendingWork;
            } else if (this.pendingWork != null && this.pendingWork.size() > 0) {
                incomingJob.pendingWork.addAll(this.pendingWork);
            }
            this.pendingWork = null;
            this.executingWork = null;
            incomingJob.nextPendingWorkId = this.nextPendingWorkId;
            incomingJob.updateEstimatedNetworkBytesLocked();
        } else {
            ungrantWorkList(am, this.pendingWork);
            this.pendingWork = null;
            ungrantWorkList(am, this.executingWork);
            this.executingWork = null;
        }
        updateEstimatedNetworkBytesLocked();
    }

    public void prepareLocked(IActivityManager am) {
        if (this.prepared) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Already prepared: ");
            stringBuilder.append(this);
            Slog.wtf(str, stringBuilder.toString());
            return;
        }
        this.prepared = true;
        this.unpreparedPoint = null;
        ClipData clip = this.job.getClipData();
        if (clip != null) {
            this.uriPerms = GrantedUriPermissions.createFromClip(am, clip, this.sourceUid, this.sourcePackageName, this.sourceUserId, this.job.getClipGrantFlags(), toShortString());
        }
    }

    public void unprepareLocked(IActivityManager am) {
        if (this.prepared) {
            this.prepared = false;
            this.unpreparedPoint = new Throwable().fillInStackTrace();
            if (this.uriPerms != null) {
                this.uriPerms.revoke(am);
                this.uriPerms = null;
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hasn't been prepared: ");
        stringBuilder.append(this);
        Slog.wtf(str, stringBuilder.toString());
        if (this.unpreparedPoint != null) {
            Slog.e(TAG, "Was already unprepared at ", this.unpreparedPoint);
        }
    }

    public boolean isPreparedLocked() {
        return this.prepared;
    }

    public JobInfo getJob() {
        return this.job;
    }

    public int getJobId() {
        return this.job.getId();
    }

    public int getTargetSdkVersion() {
        return this.targetSdkVersion;
    }

    public void printUniqueId(PrintWriter pw) {
        UserHandle.formatUid(pw, this.callingUid);
        pw.print(SliceAuthority.DELIMITER);
        pw.print(this.job.getId());
    }

    public int getNumFailures() {
        return this.numFailures;
    }

    public ComponentName getServiceComponent() {
        return this.job.getService();
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public int getSourceUid() {
        return this.sourceUid;
    }

    public int getSourceUserId() {
        return this.sourceUserId;
    }

    public int getUserId() {
        return UserHandle.getUserId(this.callingUid);
    }

    public int getStandbyBucket() {
        return this.standbyBucket;
    }

    public long getBaseHeartbeat() {
        return this.baseHeartbeat;
    }

    public void setStandbyBucket(int newBucket) {
        this.standbyBucket = newBucket;
    }

    public long getWhenStandbyDeferred() {
        return this.whenStandbyDeferred;
    }

    public void setWhenStandbyDeferred(long now) {
        this.whenStandbyDeferred = now;
    }

    public String getSourceTag() {
        return this.sourceTag;
    }

    public int getUid() {
        return this.callingUid;
    }

    public String getBatteryName() {
        return this.batteryName;
    }

    public String getTag() {
        return this.tag;
    }

    public int getPriority() {
        return this.job.getPriority();
    }

    public int getFlags() {
        return this.job.getFlags();
    }

    public int getInternalFlags() {
        return this.mInternalFlags;
    }

    public void addInternalFlags(int flags) {
        this.mInternalFlags |= flags;
    }

    /* JADX WARNING: Missing block: B:11:0x002a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void maybeAddForegroundExemption(Predicate<Integer> uidForegroundChecker) {
        if (!this.job.hasEarlyConstraint() && !this.job.hasLateConstraint() && (this.mInternalFlags & 1) == 0 && uidForegroundChecker.test(Integer.valueOf(getSourceUid()))) {
            addInternalFlags(1);
        }
    }

    private void updateEstimatedNetworkBytesLocked() {
        this.totalNetworkBytes = computeEstimatedNetworkBytesLocked();
    }

    private long computeEstimatedNetworkBytesLocked() {
        long networkBytes = this.job.getEstimatedNetworkBytes();
        if (networkBytes == -1) {
            return -1;
        }
        long totalNetworkBytes = 0 + networkBytes;
        if (this.pendingWork != null) {
            for (int i = 0; i < this.pendingWork.size(); i++) {
                networkBytes = ((JobWorkItem) this.pendingWork.get(i)).getEstimatedNetworkBytes();
                if (networkBytes == -1) {
                    return -1;
                }
                totalNetworkBytes += networkBytes;
            }
        }
        return totalNetworkBytes;
    }

    public long getEstimatedNetworkBytes() {
        return this.totalNetworkBytes;
    }

    public boolean hasConnectivityConstraint() {
        return (this.requiredConstraints & CONSTRAINT_CONNECTIVITY) != 0;
    }

    public boolean hasChargingConstraint() {
        return (this.requiredConstraints & 1) != 0;
    }

    public boolean hasBatteryNotLowConstraint() {
        return (this.requiredConstraints & 2) != 0;
    }

    public boolean hasHwBatteryLevJobAllowedConstraint() {
        return (this.requiredConstraints & 256) != 0;
    }

    public boolean hasPowerConstraint() {
        return (this.requiredConstraints & LightsManager.LIGHT_ID_MANUALCUSTOMBACKLIGHT) != 0;
    }

    public boolean hasStorageNotLowConstraint() {
        return (this.requiredConstraints & 8) != 0;
    }

    public boolean hasTimingDelayConstraint() {
        return (this.requiredConstraints & Integer.MIN_VALUE) != 0;
    }

    public boolean hasDeadlineConstraint() {
        return (this.requiredConstraints & CONSTRAINT_DEADLINE) != 0;
    }

    public boolean hasIdleConstraint() {
        return (this.requiredConstraints & 4) != 0;
    }

    public boolean hasContentTriggerConstraint() {
        return (this.requiredConstraints & CONSTRAINT_CONTENT_TRIGGER) != 0;
    }

    public long getTriggerContentUpdateDelay() {
        long time = this.job.getTriggerContentUpdateDelay();
        if (time < 0) {
            return DEFAULT_TRIGGER_UPDATE_DELAY;
        }
        return Math.max(time, 500);
    }

    public long getTriggerContentMaxDelay() {
        long time = this.job.getTriggerContentMaxDelay();
        if (time < 0) {
            return DEFAULT_TRIGGER_MAX_DELAY;
        }
        return Math.max(time, 1000);
    }

    public boolean isPersisted() {
        return this.job.isPersisted();
    }

    public long getEarliestRunTime() {
        return this.earliestRunTimeElapsedMillis;
    }

    public long getLatestRunTimeElapsed() {
        return this.latestRunTimeElapsedMillis;
    }

    public float getFractionRunTime() {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        float f = 1.0f;
        if (this.earliestRunTimeElapsedMillis == 0 && this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            return 1.0f;
        }
        if (this.earliestRunTimeElapsedMillis == 0) {
            if (now < this.latestRunTimeElapsedMillis) {
                f = 0.0f;
            }
            return f;
        } else if (this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            if (now < this.earliestRunTimeElapsedMillis) {
                f = 0.0f;
            }
            return f;
        } else if (now <= this.earliestRunTimeElapsedMillis) {
            return 0.0f;
        } else {
            if (now >= this.latestRunTimeElapsedMillis) {
                return 1.0f;
            }
            return ((float) (now - this.earliestRunTimeElapsedMillis)) / ((float) (this.latestRunTimeElapsedMillis - this.earliestRunTimeElapsedMillis));
        }
    }

    public Pair<Long, Long> getPersistedUtcTimes() {
        return this.mPersistedUtcTimes;
    }

    public void clearPersistedUtcTimes() {
        this.mPersistedUtcTimes = null;
    }

    boolean setChargingConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(1, state);
    }

    boolean setBatteryNotLowConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(2, state);
    }

    boolean setHwBatteryLevJobAllowedConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(256, state);
    }

    boolean setStorageNotLowConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(8, state);
    }

    boolean setTimingDelayConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(Integer.MIN_VALUE, state);
    }

    boolean setDeadlineConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(CONSTRAINT_DEADLINE, state);
    }

    boolean setIdleConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(4, state);
    }

    boolean setConnectivityConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(CONSTRAINT_CONNECTIVITY, state);
    }

    boolean setContentTriggerConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(CONSTRAINT_CONTENT_TRIGGER, state);
    }

    boolean setDeviceNotDozingConstraintSatisfied(boolean state, boolean whitelisted) {
        this.dozeWhitelisted = whitelisted;
        return setConstraintSatisfied(33554432, state);
    }

    boolean setBackgroundNotRestrictedConstraintSatisfied(boolean state) {
        return setConstraintSatisfied(4194304, state);
    }

    boolean setUidActive(boolean newActiveState) {
        if (newActiveState == this.uidActive) {
            return false;
        }
        this.uidActive = newActiveState;
        return true;
    }

    boolean setConstraintSatisfied(int constraint, boolean state) {
        int i = 0;
        if (((this.satisfiedConstraints & constraint) != 0) == state) {
            return false;
        }
        int i2 = this.satisfiedConstraints & (~constraint);
        if (state) {
            i = constraint;
        }
        this.satisfiedConstraints = i | i2;
        return true;
    }

    boolean isConstraintSatisfied(int constraint) {
        return (this.satisfiedConstraints & constraint) != 0;
    }

    boolean clearTrackingController(int which) {
        if ((this.trackingControllers & which) == 0) {
            return false;
        }
        this.trackingControllers &= ~which;
        return true;
    }

    void setTrackingController(int which) {
        this.trackingControllers |= which;
    }

    public long getLastSuccessfulRunTime() {
        return this.mLastSuccessfulRunTime;
    }

    public long getLastFailedRunTime() {
        return this.mLastFailedRunTime;
    }

    public boolean isReady() {
        boolean deadlineSatisfied = (this.job.isPeriodic() || !hasDeadlineConstraint() || (this.satisfiedConstraints & CONSTRAINT_DEADLINE) == 0) ? false : true;
        boolean notDozing = ((this.satisfiedConstraints & 33554432) == 0 && (this.job.getFlags() & 1) == 0) ? false : true;
        boolean notRestrictedInBg = (this.satisfiedConstraints & 4194304) != 0;
        if ((isConstraintsSatisfied() || deadlineSatisfied) && notDozing && notRestrictedInBg) {
            return true;
        }
        return false;
    }

    public boolean isConstraintsSatisfied() {
        boolean z = true;
        if (this.overrideState == 2) {
            return true;
        }
        int req = this.requiredConstraints & CONSTRAINTS_OF_INTEREST;
        int sat = CONSTRAINTS_OF_INTEREST & this.satisfiedConstraints;
        if (this.overrideState == 1) {
            sat |= this.requiredConstraints & SOFT_OVERRIDE_CONSTRAINTS;
        }
        if ((sat & req) != req) {
            z = false;
        }
        return z;
    }

    public boolean matches(int uid, int jobId) {
        return this.job.getId() == jobId && this.callingUid == uid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("JobStatus{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        sb.append(" u=");
        sb.append(getUserId());
        sb.append(" s=");
        sb.append(getSourceUid());
        if (!(this.earliestRunTimeElapsedMillis == 0 && this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME)) {
            long now = JobSchedulerService.sElapsedRealtimeClock.millis();
            sb.append(" TIME=");
            StringBuilder stringBuilder = sb;
            long j = now;
            formatRunTime(stringBuilder, this.earliestRunTimeElapsedMillis, 0, j);
            sb.append(":");
            formatRunTime(stringBuilder, this.latestRunTimeElapsedMillis, (long) NO_LATEST_RUNTIME, j);
        }
        if (this.job.getRequiredNetwork() != null) {
            sb.append(" NET");
        }
        if (this.job.isRequireCharging()) {
            sb.append(" CHARGING");
        }
        if (this.job.isRequireBatteryNotLow()) {
            sb.append(" BATNOTLOW");
        }
        if (this.job.isRequireStorageNotLow()) {
            sb.append(" STORENOTLOW");
        }
        if (this.job.isRequireDeviceIdle()) {
            sb.append(" IDLE");
        }
        if (this.job.isPeriodic()) {
            sb.append(" PERIODIC");
        }
        if (this.job.isPersisted()) {
            sb.append(" PERSISTED");
        }
        if ((this.satisfiedConstraints & 33554432) == 0) {
            sb.append(" WAIT:DEV_NOT_DOZING");
        }
        if (this.job.getTriggerContentUris() != null) {
            sb.append(" URIS=");
            sb.append(Arrays.toString(this.job.getTriggerContentUris()));
        }
        if (this.numFailures != 0) {
            sb.append(" failures=");
            sb.append(this.numFailures);
        }
        if (isReady()) {
            sb.append(" READY");
        }
        sb.append("}");
        return sb.toString();
    }

    private void formatRunTime(PrintWriter pw, long runtime, long defaultValue, long now) {
        if (runtime == defaultValue) {
            pw.print("none");
        } else {
            TimeUtils.formatDuration(runtime - now, pw);
        }
    }

    private void formatRunTime(StringBuilder sb, long runtime, long defaultValue, long now) {
        if (runtime == defaultValue) {
            sb.append("none");
        } else {
            TimeUtils.formatDuration(runtime - now, sb);
        }
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        return sb.toString();
    }

    public String toShortStringExceptUniqueId() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.batteryName);
        return sb.toString();
    }

    public void writeToShortProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.callingUid);
        proto.write(1120986464258L, this.job.getId());
        proto.write(1138166333443L, this.batteryName);
        proto.end(token);
    }

    void dumpConstraints(PrintWriter pw, int constraints) {
        if ((constraints & 1) != 0) {
            pw.print(" CHARGING");
        }
        if ((constraints & 2) != 0) {
            pw.print(" BATTERY_NOT_LOW");
        }
        if ((constraints & 8) != 0) {
            pw.print(" STORAGE_NOT_LOW");
        }
        if ((Integer.MIN_VALUE & constraints) != 0) {
            pw.print(" TIMING_DELAY");
        }
        if ((CONSTRAINT_DEADLINE & constraints) != 0) {
            pw.print(" DEADLINE");
        }
        if ((constraints & 4) != 0) {
            pw.print(" IDLE");
        }
        if ((CONSTRAINT_CONNECTIVITY & constraints) != 0) {
            pw.print(" CONNECTIVITY");
        }
        if ((CONSTRAINT_CONTENT_TRIGGER & constraints) != 0) {
            pw.print(" CONTENT_TRIGGER");
        }
        if ((33554432 & constraints) != 0) {
            pw.print(" DEVICE_NOT_DOZING");
        }
        if ((4194304 & constraints) != 0) {
            pw.print(" BACKGROUND_NOT_RESTRICTED");
        }
        if (constraints != 0) {
            pw.print(" [0x");
            pw.print(Integer.toHexString(constraints));
            pw.print("]");
        }
    }

    void dumpConstraints(ProtoOutputStream proto, long fieldId, int constraints) {
        if ((constraints & 1) != 0) {
            proto.write(fieldId, 1);
        }
        if ((constraints & 2) != 0) {
            proto.write(fieldId, 2);
        }
        if ((constraints & 8) != 0) {
            proto.write(fieldId, 3);
        }
        if ((Integer.MIN_VALUE & constraints) != 0) {
            proto.write(fieldId, 4);
        }
        if ((CONSTRAINT_DEADLINE & constraints) != 0) {
            proto.write(fieldId, 5);
        }
        if ((constraints & 4) != 0) {
            proto.write(fieldId, 6);
        }
        if ((CONSTRAINT_CONNECTIVITY & constraints) != 0) {
            proto.write(fieldId, 7);
        }
        if ((CONSTRAINT_CONTENT_TRIGGER & constraints) != 0) {
            proto.write(fieldId, 8);
        }
        if ((33554432 & constraints) != 0) {
            proto.write(fieldId, 9);
        }
    }

    private void dumpJobWorkItem(PrintWriter pw, String prefix, JobWorkItem work, int index) {
        pw.print(prefix);
        pw.print("  #");
        pw.print(index);
        pw.print(": #");
        pw.print(work.getWorkId());
        pw.print(" ");
        pw.print(work.getDeliveryCount());
        pw.print("x ");
        pw.println(work.getIntent());
        if (work.getGrants() != null) {
            pw.print(prefix);
            pw.println("  URI grants:");
            GrantedUriPermissions grantedUriPermissions = (GrantedUriPermissions) work.getGrants();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("    ");
            grantedUriPermissions.dump(pw, stringBuilder.toString());
        }
    }

    private void dumpJobWorkItem(ProtoOutputStream proto, long fieldId, JobWorkItem work) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, work.getWorkId());
        proto.write(1120986464258L, work.getDeliveryCount());
        if (work.getIntent() != null) {
            work.getIntent().writeToProto(proto, 1146756268035L);
        }
        Object grants = work.getGrants();
        if (grants != null) {
            ((GrantedUriPermissions) grants).dump(proto, 1146756268036L);
        }
        proto.end(token);
    }

    private String bucketName(int bucket) {
        switch (bucket) {
            case 0:
                return "ACTIVE";
            case 1:
                return "WORKING_SET";
            case 2:
                return "FREQUENT";
            case 3:
                return "RARE";
            case 4:
                return "NEVER";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown: ");
                stringBuilder.append(bucket);
                return stringBuilder.toString();
        }
    }

    private static int resolveTargetSdkVersion(JobInfo job) {
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageTargetSdkVersion(job.getService().getPackageName());
    }

    public void dump(PrintWriter pw, String prefix, boolean full, long elapsedRealtimeMillis) {
        int i;
        PrintWriter printWriter = pw;
        String str = prefix;
        long j = elapsedRealtimeMillis;
        pw.print(prefix);
        UserHandle.formatUid(printWriter, this.callingUid);
        printWriter.print(" tag=");
        printWriter.println(this.tag);
        pw.print(prefix);
        printWriter.print("Source: uid=");
        UserHandle.formatUid(printWriter, getSourceUid());
        printWriter.print(" user=");
        printWriter.print(getSourceUserId());
        printWriter.print(" pkg=");
        printWriter.println(getSourcePackageName());
        int i2 = 0;
        if (full) {
            pw.print(prefix);
            printWriter.println("JobInfo:");
            pw.print(prefix);
            printWriter.print("  Service: ");
            printWriter.println(this.job.getService().flattenToShortString());
            if (this.job.isPeriodic()) {
                pw.print(prefix);
                printWriter.print("  PERIODIC: interval=");
                TimeUtils.formatDuration(this.job.getIntervalMillis(), printWriter);
                printWriter.print(" flex=");
                TimeUtils.formatDuration(this.job.getFlexMillis(), printWriter);
                pw.println();
            }
            if (this.job.isPersisted()) {
                pw.print(prefix);
                printWriter.println("  PERSISTED");
            }
            if (this.job.getPriority() != 0) {
                pw.print(prefix);
                printWriter.print("  Priority: ");
                printWriter.println(this.job.getPriority());
            }
            if (this.job.getFlags() != 0) {
                pw.print(prefix);
                printWriter.print("  Flags: ");
                printWriter.println(Integer.toHexString(this.job.getFlags()));
            }
            if (getInternalFlags() != 0) {
                pw.print(prefix);
                printWriter.print("  Internal flags: ");
                printWriter.print(Integer.toHexString(getInternalFlags()));
                if ((getInternalFlags() & 1) != 0) {
                    printWriter.print(" HAS_FOREGROUND_EXEMPTION");
                }
                pw.println();
            }
            pw.print(prefix);
            printWriter.print("  Requires: charging=");
            printWriter.print(this.job.isRequireCharging());
            printWriter.print(" batteryNotLow=");
            printWriter.print(this.job.isRequireBatteryNotLow());
            printWriter.print(" deviceIdle=");
            printWriter.println(this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                pw.print(prefix);
                printWriter.println("  Trigger content URIs:");
                for (TriggerContentUri trig : this.job.getTriggerContentUris()) {
                    pw.print(prefix);
                    printWriter.print("    ");
                    printWriter.print(Integer.toHexString(trig.getFlags()));
                    printWriter.print(' ');
                    printWriter.println(trig.getUri());
                }
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    pw.print(prefix);
                    printWriter.print("  Trigger update delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentUpdateDelay(), printWriter);
                    pw.println();
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    pw.print(prefix);
                    printWriter.print("  Trigger max delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentMaxDelay(), printWriter);
                    pw.println();
                }
            }
            if (!(this.job.getExtras() == null || this.job.getExtras().maybeIsEmpty())) {
                PersistableBundle temp = this.job.getExtras();
                if (temp.get("accountName") != null) {
                    temp.putString("accountName", "XXXXXXXXX");
                }
                pw.print(prefix);
                printWriter.print("  Extras: ");
                printWriter.println(temp.toShortString());
            }
            if (!(this.job.getTransientExtras() == null || this.job.getTransientExtras().maybeIsEmpty())) {
                pw.print(prefix);
                printWriter.print("  Transient extras: ");
                printWriter.println(this.job.getTransientExtras().toShortString());
            }
            if (this.job.getClipData() != null) {
                pw.print(prefix);
                printWriter.print("  Clip data: ");
                StringBuilder b = new StringBuilder(128);
                this.job.getClipData().toShortString(b);
                printWriter.println(b);
            }
            if (this.uriPerms != null) {
                pw.print(prefix);
                printWriter.println("  Granted URI permissions:");
                GrantedUriPermissions grantedUriPermissions = this.uriPerms;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append("  ");
                grantedUriPermissions.dump(printWriter, stringBuilder.toString());
            }
            if (this.job.getRequiredNetwork() != null) {
                pw.print(prefix);
                printWriter.print("  Network type: ");
                printWriter.println(this.job.getRequiredNetwork());
            }
            if (this.totalNetworkBytes != -1) {
                pw.print(prefix);
                printWriter.print("  Network bytes: ");
                printWriter.println(this.totalNetworkBytes);
            }
            if (this.job.getMinLatencyMillis() != 0) {
                pw.print(prefix);
                printWriter.print("  Minimum latency: ");
                TimeUtils.formatDuration(this.job.getMinLatencyMillis(), printWriter);
                pw.println();
            }
            if (this.job.getMaxExecutionDelayMillis() != 0) {
                pw.print(prefix);
                printWriter.print("  Max execution delay: ");
                TimeUtils.formatDuration(this.job.getMaxExecutionDelayMillis(), printWriter);
                pw.println();
            }
            pw.print(prefix);
            printWriter.print("  Backoff: policy=");
            printWriter.print(this.job.getBackoffPolicy());
            printWriter.print(" initial=");
            TimeUtils.formatDuration(this.job.getInitialBackoffMillis(), printWriter);
            pw.println();
            if (this.job.hasEarlyConstraint()) {
                pw.print(prefix);
                printWriter.println("  Has early constraint");
            }
            if (this.job.hasLateConstraint()) {
                pw.print(prefix);
                printWriter.println("  Has late constraint");
            }
        }
        pw.print(prefix);
        printWriter.print("Required constraints:");
        dumpConstraints(printWriter, this.requiredConstraints);
        pw.println();
        if (full) {
            pw.print(prefix);
            printWriter.print("Satisfied constraints:");
            dumpConstraints(printWriter, this.satisfiedConstraints);
            pw.println();
            pw.print(prefix);
            printWriter.print("Unsatisfied constraints:");
            dumpConstraints(printWriter, this.requiredConstraints & (~this.satisfiedConstraints));
            pw.println();
            if (this.dozeWhitelisted) {
                pw.print(prefix);
                printWriter.println("Doze whitelisted: true");
            }
            if (this.uidActive) {
                pw.print(prefix);
                printWriter.println("Uid: active");
            }
        }
        if (this.trackingControllers != 0) {
            pw.print(prefix);
            printWriter.print("Tracking:");
            if ((this.trackingControllers & 1) != 0) {
                printWriter.print(" BATTERY");
            }
            if ((this.trackingControllers & 2) != 0) {
                printWriter.print(" CONNECTIVITY");
            }
            if ((this.trackingControllers & 4) != 0) {
                printWriter.print(" CONTENT");
            }
            if ((this.trackingControllers & 8) != 0) {
                printWriter.print(" IDLE");
            }
            if ((this.trackingControllers & 16) != 0) {
                printWriter.print(" STORAGE");
            }
            if ((32 & this.trackingControllers) != 0) {
                printWriter.print(" TIME");
            }
            pw.println();
        }
        if (this.changedAuthorities != null) {
            pw.print(prefix);
            printWriter.println("Changed authorities:");
            for (i = 0; i < this.changedAuthorities.size(); i++) {
                pw.print(prefix);
                printWriter.print("  ");
                printWriter.println((String) this.changedAuthorities.valueAt(i));
            }
            if (this.changedUris != null) {
                pw.print(prefix);
                printWriter.println("Changed URIs:");
                for (i = 0; i < this.changedUris.size(); i++) {
                    pw.print(prefix);
                    printWriter.print("  ");
                    printWriter.println(this.changedUris.valueAt(i));
                }
            }
        }
        if (this.network != null) {
            pw.print(prefix);
            printWriter.print("Network: ");
            printWriter.println(this.network);
        }
        if (this.pendingWork != null && this.pendingWork.size() > 0) {
            pw.print(prefix);
            printWriter.println("Pending work:");
            for (i = 0; i < this.pendingWork.size(); i++) {
                dumpJobWorkItem(printWriter, str, (JobWorkItem) this.pendingWork.get(i), i);
            }
        }
        if (this.executingWork != null && this.executingWork.size() > 0) {
            pw.print(prefix);
            printWriter.println("Executing work:");
            while (true) {
                i = i2;
                if (i >= this.executingWork.size()) {
                    break;
                }
                dumpJobWorkItem(printWriter, str, (JobWorkItem) this.executingWork.get(i), i);
                i2 = i + 1;
            }
        }
        pw.print(prefix);
        printWriter.print("Standby bucket: ");
        printWriter.println(bucketName(this.standbyBucket));
        if (this.standbyBucket > 0) {
            pw.print(prefix);
            printWriter.print("Base heartbeat: ");
            printWriter.println(this.baseHeartbeat);
        }
        if (this.whenStandbyDeferred != 0) {
            pw.print(prefix);
            printWriter.print("  Deferred since: ");
            TimeUtils.formatDuration(this.whenStandbyDeferred, j, printWriter);
            pw.println();
        }
        pw.print(prefix);
        printWriter.print("Enqueue time: ");
        TimeUtils.formatDuration(this.enqueueTime, j, printWriter);
        pw.println();
        pw.print(prefix);
        printWriter.print("Run time: earliest=");
        PrintWriter printWriter2 = printWriter;
        long j2 = j;
        formatRunTime(printWriter2, this.earliestRunTimeElapsedMillis, 0, j2);
        printWriter.print(", latest=");
        formatRunTime(printWriter2, this.latestRunTimeElapsedMillis, (long) NO_LATEST_RUNTIME, j2);
        pw.println();
        if (this.numFailures != 0) {
            pw.print(prefix);
            printWriter.print("Num failures: ");
            printWriter.println(this.numFailures);
        }
        Time t = new Time();
        String format = "%Y-%m-%d %H:%M:%S";
        if (this.mLastSuccessfulRunTime != 0) {
            pw.print(prefix);
            printWriter.print("Last successful run: ");
            t.set(this.mLastSuccessfulRunTime);
            printWriter.println(t.format("%Y-%m-%d %H:%M:%S"));
        }
        if (this.mLastFailedRunTime != 0) {
            pw.print(prefix);
            printWriter.print("Last failed run: ");
            t.set(this.mLastFailedRunTime);
            printWriter.println(t.format("%Y-%m-%d %H:%M:%S"));
        }
    }

    public void dump(ProtoOutputStream proto, long fieldId, boolean full, long elapsedRealtimeMillis) {
        int i;
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        protoOutputStream.write(1120986464257L, this.callingUid);
        protoOutputStream.write(1138166333442L, this.tag);
        protoOutputStream.write(1120986464259L, getSourceUid());
        protoOutputStream.write(1120986464260L, getSourceUserId());
        protoOutputStream.write(1138166333445L, getSourcePackageName());
        protoOutputStream.write(1112396529688L, getInternalFlags());
        if (full) {
            long jiToken = protoOutputStream.start(1146756268038L);
            this.job.getService().writeToProto(protoOutputStream, 1146756268033L);
            protoOutputStream.write(1133871366146L, this.job.isPeriodic());
            protoOutputStream.write(1112396529667L, this.job.getIntervalMillis());
            protoOutputStream.write(1112396529668L, this.job.getFlexMillis());
            protoOutputStream.write(1133871366149L, this.job.isPersisted());
            protoOutputStream.write(1172526071814L, this.job.getPriority());
            protoOutputStream.write(1120986464263L, this.job.getFlags());
            protoOutputStream.write(1133871366152L, this.job.isRequireCharging());
            protoOutputStream.write(1133871366153L, this.job.isRequireBatteryNotLow());
            protoOutputStream.write(1133871366154L, this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                for (TriggerContentUri trig : this.job.getTriggerContentUris()) {
                    long tcuToken = protoOutputStream.start(2246267895819L);
                    protoOutputStream.write(1120986464257L, trig.getFlags());
                    Uri u = trig.getUri();
                    if (u != null) {
                        TriggerContentUri triggerContentUri = trig;
                        Uri uri = u;
                        protoOutputStream.write(2, u.toString());
                    }
                    protoOutputStream.end(tcuToken);
                }
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    protoOutputStream.write(1112396529676L, this.job.getTriggerContentUpdateDelay());
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    protoOutputStream.write(1112396529677L, this.job.getTriggerContentMaxDelay());
                }
            }
            if (!(this.job.getExtras() == null || this.job.getExtras().maybeIsEmpty())) {
                this.job.getExtras().writeToProto(protoOutputStream, 1146756268046L);
            }
            if (!(this.job.getTransientExtras() == null || this.job.getTransientExtras().maybeIsEmpty())) {
                this.job.getTransientExtras().writeToProto(protoOutputStream, 1146756268047L);
            }
            if (this.job.getClipData() != null) {
                this.job.getClipData().writeToProto(protoOutputStream, 1146756268048L);
            }
            if (this.uriPerms != null) {
                this.uriPerms.dump(protoOutputStream, 1146756268049L);
            }
            if (this.job.getRequiredNetwork() != null) {
                this.job.getRequiredNetwork().writeToProto(protoOutputStream, 1146756268050L);
            }
            if (this.totalNetworkBytes != -1) {
                protoOutputStream.write(1112396529683L, this.totalNetworkBytes);
            }
            protoOutputStream.write(1112396529684L, this.job.getMinLatencyMillis());
            protoOutputStream.write(1112396529685L, this.job.getMaxExecutionDelayMillis());
            long bpToken = protoOutputStream.start(1146756268054L);
            protoOutputStream.write(1159641169921L, this.job.getBackoffPolicy());
            protoOutputStream.write(1112396529666L, this.job.getInitialBackoffMillis());
            protoOutputStream.end(bpToken);
            protoOutputStream.write(1133871366167L, this.job.hasEarlyConstraint());
            protoOutputStream.write(1133871366168L, this.job.hasLateConstraint());
            protoOutputStream.end(jiToken);
        }
        dumpConstraints(protoOutputStream, 2259152797703L, this.requiredConstraints);
        if (full) {
            dumpConstraints(protoOutputStream, 2259152797704L, this.satisfiedConstraints);
            dumpConstraints(protoOutputStream, 2259152797705L, this.requiredConstraints & (~this.satisfiedConstraints));
            protoOutputStream.write(1133871366154L, this.dozeWhitelisted);
        }
        if ((this.trackingControllers & 1) != 0) {
            protoOutputStream.write(2259152797707L, 0);
        }
        if ((this.trackingControllers & 2) != 0) {
            protoOutputStream.write(2259152797707L, 1);
        }
        if ((this.trackingControllers & 4) != 0) {
            protoOutputStream.write(2259152797707L, 2);
        }
        if ((this.trackingControllers & 8) != 0) {
            protoOutputStream.write(2259152797707L, 3);
        }
        if ((this.trackingControllers & 16) != 0) {
            protoOutputStream.write(2259152797707L, 4);
        }
        if ((this.trackingControllers & 32) != 0) {
            protoOutputStream.write(2259152797707L, 5);
        }
        if (this.changedAuthorities != null) {
            for (i = 0; i < this.changedAuthorities.size(); i++) {
                protoOutputStream.write(2237677961228L, (String) this.changedAuthorities.valueAt(i));
            }
        }
        if (this.changedUris != null) {
            for (i = 0; i < this.changedUris.size(); i++) {
                protoOutputStream.write(2237677961229L, ((Uri) this.changedUris.valueAt(i)).toString());
            }
        }
        if (this.network != null) {
            this.network.writeToProto(protoOutputStream, 1146756268046L);
        }
        if (this.pendingWork != null && this.pendingWork.size() > 0) {
            for (i = 0; i < this.pendingWork.size(); i++) {
                dumpJobWorkItem(protoOutputStream, 2246267895823L, (JobWorkItem) this.pendingWork.get(i));
            }
        }
        if (this.executingWork != null && this.executingWork.size() > 0) {
            for (i = 0; i < this.executingWork.size(); i++) {
                dumpJobWorkItem(protoOutputStream, 2246267895824L, (JobWorkItem) this.executingWork.get(i));
            }
        }
        protoOutputStream.write(1159641169937L, this.standbyBucket);
        protoOutputStream.write(1112396529682L, elapsedRealtimeMillis - this.enqueueTime);
        if (this.earliestRunTimeElapsedMillis == 0) {
            protoOutputStream.write(1176821039123L, 0);
        } else {
            protoOutputStream.write(1176821039123L, this.earliestRunTimeElapsedMillis - elapsedRealtimeMillis);
        }
        if (this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            protoOutputStream.write(1176821039124L, 0);
        } else {
            protoOutputStream.write(1176821039124L, this.latestRunTimeElapsedMillis - elapsedRealtimeMillis);
        }
        protoOutputStream.write(1120986464277L, this.numFailures);
        protoOutputStream.write(1112396529686L, this.mLastSuccessfulRunTime);
        protoOutputStream.write(1112396529687L, this.mLastFailedRunTime);
        protoOutputStream.end(token);
    }
}
