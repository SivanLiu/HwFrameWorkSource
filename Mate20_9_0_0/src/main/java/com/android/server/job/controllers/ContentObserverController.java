package com.android.server.job.controllers;

import android.app.job.JobInfo.TriggerContentUri;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public final class ContentObserverController extends StateController {
    private static final boolean DEBUG;
    private static final int MAX_URIS_REPORTED = 50;
    private static final String TAG = "JobScheduler.ContentObserver";
    private static final int URIS_URGENT_THRESHOLD = 40;
    final Handler mHandler = new Handler(this.mContext.getMainLooper());
    final SparseArray<ArrayMap<TriggerContentUri, ObserverInstance>> mObservers = new SparseArray();
    private final ArraySet<JobStatus> mTrackedTasks = new ArraySet();

    final class JobInstance {
        ArraySet<String> mChangedAuthorities;
        ArraySet<Uri> mChangedUris;
        final Runnable mExecuteRunner;
        final JobStatus mJobStatus;
        final ArrayList<ObserverInstance> mMyObservers = new ArrayList();
        final Runnable mTimeoutRunner;
        boolean mTriggerPending;

        JobInstance(JobStatus jobStatus) {
            this.mJobStatus = jobStatus;
            this.mExecuteRunner = new TriggerRunnable(this);
            this.mTimeoutRunner = new TriggerRunnable(this);
            TriggerContentUri[] uris = jobStatus.getJob().getTriggerContentUris();
            int sourceUserId = jobStatus.getSourceUserId();
            ArrayMap<TriggerContentUri, ObserverInstance> observersOfUser = (ArrayMap) ContentObserverController.this.mObservers.get(sourceUserId);
            if (observersOfUser == null) {
                observersOfUser = new ArrayMap();
                ContentObserverController.this.mObservers.put(sourceUserId, observersOfUser);
            }
            if (uris != null) {
                for (TriggerContentUri uri : uris) {
                    ObserverInstance obs = (ObserverInstance) observersOfUser.get(uri);
                    boolean andDescendants = true;
                    String str;
                    StringBuilder stringBuilder;
                    if (obs == null) {
                        obs = new ObserverInstance(ContentObserverController.this.mHandler, uri, jobStatus.getSourceUserId());
                        observersOfUser.put(uri, obs);
                        if ((uri.getFlags() & 1) == 0) {
                            andDescendants = false;
                        }
                        if (ContentObserverController.DEBUG) {
                            str = ContentObserverController.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("New observer ");
                            stringBuilder.append(obs);
                            stringBuilder.append(" for ");
                            stringBuilder.append(uri.getUri());
                            stringBuilder.append(" andDescendants=");
                            stringBuilder.append(andDescendants);
                            stringBuilder.append(" sourceUserId=");
                            stringBuilder.append(sourceUserId);
                            Slog.v(str, stringBuilder.toString());
                        }
                        ContentObserverController.this.mContext.getContentResolver().registerContentObserver(uri.getUri(), andDescendants, obs, sourceUserId);
                    } else if (ContentObserverController.DEBUG) {
                        if ((uri.getFlags() & 1) == 0) {
                            andDescendants = false;
                        }
                        str = ContentObserverController.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Reusing existing observer ");
                        stringBuilder.append(obs);
                        stringBuilder.append(" for ");
                        stringBuilder.append(uri.getUri());
                        stringBuilder.append(" andDescendants=");
                        stringBuilder.append(andDescendants);
                        Slog.v(str, stringBuilder.toString());
                    }
                    obs.mJobs.add(this);
                    this.mMyObservers.add(obs);
                }
            }
        }

        void trigger() {
            boolean reportChange = false;
            synchronized (ContentObserverController.this.mLock) {
                if (this.mTriggerPending) {
                    if (this.mJobStatus.setContentTriggerConstraintSatisfied(true)) {
                        reportChange = true;
                    }
                    unscheduleLocked();
                }
            }
            if (reportChange) {
                ContentObserverController.this.mStateChangedListener.onControllerStateChanged();
            }
        }

        void scheduleLocked() {
            if (!this.mTriggerPending) {
                this.mTriggerPending = true;
                ContentObserverController.this.mHandler.postDelayed(this.mTimeoutRunner, this.mJobStatus.getTriggerContentMaxDelay());
            }
            ContentObserverController.this.mHandler.removeCallbacks(this.mExecuteRunner);
            if (this.mChangedUris.size() >= 40) {
                ContentObserverController.this.mHandler.post(this.mExecuteRunner);
            } else {
                ContentObserverController.this.mHandler.postDelayed(this.mExecuteRunner, this.mJobStatus.getTriggerContentUpdateDelay());
            }
        }

        void unscheduleLocked() {
            if (this.mTriggerPending) {
                ContentObserverController.this.mHandler.removeCallbacks(this.mExecuteRunner);
                ContentObserverController.this.mHandler.removeCallbacks(this.mTimeoutRunner);
                this.mTriggerPending = false;
            }
        }

        void detachLocked() {
            int N = this.mMyObservers.size();
            for (int i = 0; i < N; i++) {
                ObserverInstance obs = (ObserverInstance) this.mMyObservers.get(i);
                obs.mJobs.remove(this);
                if (obs.mJobs.size() == 0) {
                    if (ContentObserverController.DEBUG) {
                        String str = ContentObserverController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unregistering observer ");
                        stringBuilder.append(obs);
                        stringBuilder.append(" for ");
                        stringBuilder.append(obs.mUri.getUri());
                        Slog.i(str, stringBuilder.toString());
                    }
                    ContentObserverController.this.mContext.getContentResolver().unregisterContentObserver(obs);
                    ArrayMap<TriggerContentUri, ObserverInstance> observerOfUser = (ArrayMap) ContentObserverController.this.mObservers.get(obs.mUserId);
                    if (observerOfUser != null) {
                        observerOfUser.remove(obs.mUri);
                    }
                }
            }
        }
    }

    final class ObserverInstance extends ContentObserver {
        final ArraySet<JobInstance> mJobs = new ArraySet();
        final TriggerContentUri mUri;
        final int mUserId;

        public ObserverInstance(Handler handler, TriggerContentUri uri, int userId) {
            super(handler);
            this.mUri = uri;
            this.mUserId = userId;
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (ContentObserverController.DEBUG) {
                String str = ContentObserverController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onChange(self=");
                stringBuilder.append(selfChange);
                stringBuilder.append(") for ");
                stringBuilder.append(uri);
                stringBuilder.append(" when mUri=");
                stringBuilder.append(this.mUri);
                stringBuilder.append(" mUserId=");
                stringBuilder.append(this.mUserId);
                Slog.i(str, stringBuilder.toString());
            }
            synchronized (ContentObserverController.this.mLock) {
                int N = this.mJobs.size();
                for (int i = 0; i < N; i++) {
                    JobInstance inst = (JobInstance) this.mJobs.valueAt(i);
                    if (inst.mChangedUris == null) {
                        inst.mChangedUris = new ArraySet();
                    }
                    if (inst.mChangedUris.size() < 50) {
                        inst.mChangedUris.add(uri);
                    }
                    if (inst.mChangedAuthorities == null) {
                        inst.mChangedAuthorities = new ArraySet();
                    }
                    inst.mChangedAuthorities.add(uri.getAuthority());
                    inst.scheduleLocked();
                }
            }
        }
    }

    static final class TriggerRunnable implements Runnable {
        final JobInstance mInstance;

        TriggerRunnable(JobInstance instance) {
            this.mInstance = instance;
        }

        public void run() {
            this.mInstance.trigger();
        }
    }

    static {
        boolean z = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
        DEBUG = z;
    }

    public ContentObserverController(JobSchedulerService service) {
        super(service);
    }

    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (taskStatus.hasContentTriggerConstraint()) {
            if (taskStatus.contentObserverJobInstance == null) {
                taskStatus.contentObserverJobInstance = new JobInstance(taskStatus);
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Tracking content-trigger job ");
                stringBuilder.append(taskStatus);
                Slog.i(str, stringBuilder.toString());
            }
            this.mTrackedTasks.add(taskStatus);
            taskStatus.setTrackingController(4);
            boolean havePendingUris = false;
            if (taskStatus.contentObserverJobInstance.mChangedAuthorities != null) {
                havePendingUris = true;
            }
            if (taskStatus.changedAuthorities != null) {
                havePendingUris = true;
                if (taskStatus.contentObserverJobInstance.mChangedAuthorities == null) {
                    taskStatus.contentObserverJobInstance.mChangedAuthorities = new ArraySet();
                }
                Iterator it = taskStatus.changedAuthorities.iterator();
                while (it.hasNext()) {
                    taskStatus.contentObserverJobInstance.mChangedAuthorities.add((String) it.next());
                }
                if (taskStatus.changedUris != null) {
                    if (taskStatus.contentObserverJobInstance.mChangedUris == null) {
                        taskStatus.contentObserverJobInstance.mChangedUris = new ArraySet();
                    }
                    it = taskStatus.changedUris.iterator();
                    while (it.hasNext()) {
                        taskStatus.contentObserverJobInstance.mChangedUris.add((Uri) it.next());
                    }
                }
                taskStatus.changedAuthorities = null;
                taskStatus.changedUris = null;
            }
            taskStatus.changedAuthorities = null;
            taskStatus.changedUris = null;
            taskStatus.setContentTriggerConstraintSatisfied(havePendingUris);
        }
        if (lastJob != null && lastJob.contentObserverJobInstance != null) {
            lastJob.contentObserverJobInstance.detachLocked();
            lastJob.contentObserverJobInstance = null;
        }
    }

    public void prepareForExecutionLocked(JobStatus taskStatus) {
        if (taskStatus.hasContentTriggerConstraint() && taskStatus.contentObserverJobInstance != null) {
            taskStatus.changedUris = taskStatus.contentObserverJobInstance.mChangedUris;
            taskStatus.changedAuthorities = taskStatus.contentObserverJobInstance.mChangedAuthorities;
            taskStatus.contentObserverJobInstance.mChangedUris = null;
            taskStatus.contentObserverJobInstance.mChangedAuthorities = null;
        }
    }

    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (taskStatus.clearTrackingController(4)) {
            this.mTrackedTasks.remove(taskStatus);
            if (taskStatus.contentObserverJobInstance != null) {
                taskStatus.contentObserverJobInstance.unscheduleLocked();
                if (incomingJob == null) {
                    taskStatus.contentObserverJobInstance.detachLocked();
                    taskStatus.contentObserverJobInstance = null;
                } else if (!(taskStatus.contentObserverJobInstance == null || taskStatus.contentObserverJobInstance.mChangedAuthorities == null)) {
                    if (incomingJob.contentObserverJobInstance == null) {
                        incomingJob.contentObserverJobInstance = new JobInstance(incomingJob);
                    }
                    incomingJob.contentObserverJobInstance.mChangedAuthorities = taskStatus.contentObserverJobInstance.mChangedAuthorities;
                    incomingJob.contentObserverJobInstance.mChangedUris = taskStatus.contentObserverJobInstance.mChangedUris;
                    taskStatus.contentObserverJobInstance.mChangedAuthorities = null;
                    taskStatus.contentObserverJobInstance.mChangedUris = null;
                }
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No longer tracking job ");
                stringBuilder.append(taskStatus);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    public void rescheduleForFailureLocked(JobStatus newJob, JobStatus failureToReschedule) {
        if (failureToReschedule.hasContentTriggerConstraint() && newJob.hasContentTriggerConstraint()) {
            newJob.changedAuthorities = failureToReschedule.changedAuthorities;
            newJob.changedUris = failureToReschedule.changedUris;
        }
    }

    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        int i;
        PrintWriter printWriter = pw;
        Predicate predicate2 = predicate;
        for (i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = (JobStatus) this.mTrackedTasks.valueAt(i);
            if (predicate2.test(js)) {
                printWriter.print("#");
                js.printUniqueId(printWriter);
                printWriter.print(" from ");
                UserHandle.formatUid(printWriter, js.getSourceUid());
                pw.println();
            }
        }
        pw.println();
        i = this.mObservers.size();
        if (i > 0) {
            printWriter.println("Observers:");
            pw.increaseIndent();
            int userIdx = 0;
            while (userIdx < i) {
                Predicate<JobStatus> predicate3;
                ArrayMap<TriggerContentUri, ObserverInstance> observersOfUser = (ArrayMap) this.mObservers.get(this.mObservers.keyAt(userIdx));
                int numbOfObserversPerUser = observersOfUser.size();
                int observerIdx = 0;
                while (observerIdx < numbOfObserversPerUser) {
                    ObserverInstance obs = (ObserverInstance) observersOfUser.valueAt(observerIdx);
                    int M = obs.mJobs.size();
                    boolean shouldDump = false;
                    for (int j = 0; j < M; j++) {
                        if (predicate2.test(((JobInstance) obs.mJobs.valueAt(j)).mJobStatus)) {
                            shouldDump = true;
                            break;
                        }
                    }
                    if (shouldDump) {
                        TriggerContentUri trigger = (TriggerContentUri) observersOfUser.keyAt(observerIdx);
                        printWriter.print(trigger.getUri());
                        printWriter.print(" 0x");
                        printWriter.print(Integer.toHexString(trigger.getFlags()));
                        printWriter.print(" (");
                        printWriter.print(System.identityHashCode(obs));
                        printWriter.println("):");
                        pw.increaseIndent();
                        printWriter.println("Jobs:");
                        pw.increaseIndent();
                        int j2 = 0;
                        while (j2 < M) {
                            JobInstance inst = (JobInstance) obs.mJobs.valueAt(j2);
                            printWriter.print("#");
                            inst.mJobStatus.printUniqueId(printWriter);
                            printWriter.print(" from ");
                            UserHandle.formatUid(printWriter, inst.mJobStatus.getSourceUid());
                            if (inst.mChangedAuthorities != null) {
                                int k;
                                printWriter.println(":");
                                pw.increaseIndent();
                                if (inst.mTriggerPending) {
                                    printWriter.print("Trigger pending: update=");
                                    TimeUtils.formatDuration(inst.mJobStatus.getTriggerContentUpdateDelay(), printWriter);
                                    printWriter.print(", max=");
                                    TimeUtils.formatDuration(inst.mJobStatus.getTriggerContentMaxDelay(), printWriter);
                                    pw.println();
                                }
                                printWriter.println("Changed Authorities:");
                                for (k = 0; k < inst.mChangedAuthorities.size(); k++) {
                                    printWriter.println((String) inst.mChangedAuthorities.valueAt(k));
                                }
                                if (inst.mChangedUris != null) {
                                    printWriter.println("          Changed URIs:");
                                    for (k = 0; k < inst.mChangedUris.size(); k++) {
                                        printWriter.println(inst.mChangedUris.valueAt(k));
                                    }
                                }
                                pw.decreaseIndent();
                            } else {
                                pw.println();
                            }
                            j2++;
                            predicate3 = predicate;
                        }
                        pw.decreaseIndent();
                        pw.decreaseIndent();
                    }
                    observerIdx++;
                    predicate3 = predicate;
                }
                userIdx++;
                predicate3 = predicate;
            }
            pw.decreaseIndent();
        }
    }

    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        int i;
        int n;
        long token;
        ContentObserverController contentObserverController = this;
        ProtoOutputStream protoOutputStream = proto;
        Predicate predicate2 = predicate;
        long token2 = proto.start(fieldId);
        long mToken = protoOutputStream.start(1146756268036L);
        for (i = 0; i < contentObserverController.mTrackedTasks.size(); i++) {
            JobStatus js = (JobStatus) contentObserverController.mTrackedTasks.valueAt(i);
            if (predicate2.test(js)) {
                long jsToken = protoOutputStream.start(2246267895809L);
                js.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, js.getSourceUid());
                protoOutputStream.end(jsToken);
            }
        }
        i = contentObserverController.mObservers.size();
        int userIdx = 0;
        while (userIdx < i) {
            int userId;
            int numbOfObserversPerUser;
            long mToken2;
            ArrayMap<TriggerContentUri, ObserverInstance> observersOfUser;
            Predicate<JobStatus> predicate3;
            n = i;
            long oToken = protoOutputStream.start(2246267895810L);
            int userId2 = contentObserverController.mObservers.keyAt(userIdx);
            protoOutputStream.write(1120986464257L, userId2);
            ArrayMap<TriggerContentUri, ObserverInstance> observersOfUser2 = (ArrayMap) contentObserverController.mObservers.get(userId2);
            int numbOfObserversPerUser2 = observersOfUser2.size();
            int observerIdx = 0;
            while (observerIdx < numbOfObserversPerUser2) {
                long oToken2;
                ObserverInstance obs = (ObserverInstance) observersOfUser2.valueAt(observerIdx);
                int m = obs.mJobs.size();
                boolean shouldDump = false;
                int j = 0;
                while (true) {
                    int j2 = j;
                    if (j2 >= m) {
                        userId = userId2;
                        numbOfObserversPerUser = numbOfObserversPerUser2;
                        break;
                    }
                    userId = userId2;
                    numbOfObserversPerUser = numbOfObserversPerUser2;
                    if (predicate2.test(((JobInstance) obs.mJobs.valueAt(j2)).mJobStatus)) {
                        shouldDump = true;
                        break;
                    }
                    j = j2 + 1;
                    userId2 = userId;
                    numbOfObserversPerUser2 = numbOfObserversPerUser;
                }
                if (shouldDump) {
                    ObserverInstance obs2;
                    int m2;
                    Uri u;
                    token = token2;
                    mToken2 = mToken;
                    long tToken = protoOutputStream.start(2246267895810L);
                    TriggerContentUri trigger = (TriggerContentUri) observersOfUser2.keyAt(observerIdx);
                    Uri u2 = trigger.getUri();
                    if (u2 != null) {
                        protoOutputStream.write(1138166333441L, u2.toString());
                    }
                    observersOfUser = observersOfUser2;
                    protoOutputStream.write(1120986464258L, trigger.getFlags());
                    int j3 = 0;
                    while (j3 < m) {
                        long jToken = protoOutputStream.start(2246267895811L);
                        JobInstance token3 = (JobInstance) obs.mJobs.valueAt(j3);
                        obs2 = obs;
                        m2 = m;
                        token3.mJobStatus.writeToShortProto(protoOutputStream, 1);
                        protoOutputStream.write(1120986464258L, token3.mJobStatus.getSourceUid());
                        if (token3.mChangedAuthorities == null) {
                            protoOutputStream.end(jToken);
                            oToken2 = oToken;
                        } else {
                            int k;
                            if (token3.mTriggerPending) {
                                u = u2;
                                oToken2 = oToken;
                                protoOutputStream.write(1112396529667L, token3.mJobStatus.getTriggerContentUpdateDelay());
                                protoOutputStream.write(1112396529668L, token3.mJobStatus.getTriggerContentMaxDelay());
                            } else {
                                u = u2;
                                oToken2 = oToken;
                            }
                            for (k = 0; k < token3.mChangedAuthorities.size(); k++) {
                                protoOutputStream.write(2237677961221L, (String) token3.mChangedAuthorities.valueAt(k));
                            }
                            if (token3.mChangedUris != null) {
                                k = 0;
                                while (k < token3.mChangedUris.size()) {
                                    Uri u3 = (Uri) token3.mChangedUris.valueAt(k);
                                    if (u3 != null) {
                                        protoOutputStream.write(2237677961222L, u3.toString());
                                    }
                                    k++;
                                    u = u3;
                                }
                            }
                            protoOutputStream.end(jToken);
                            u2 = u;
                        }
                        j3++;
                        obs = obs2;
                        m = m2;
                        oToken = oToken2;
                    }
                    u = u2;
                    oToken2 = oToken;
                    obs2 = obs;
                    m2 = m;
                    protoOutputStream.end(tToken);
                } else {
                    token = token2;
                    mToken2 = mToken;
                    oToken2 = oToken;
                    observersOfUser = observersOfUser2;
                }
                observerIdx++;
                userId2 = userId;
                numbOfObserversPerUser2 = numbOfObserversPerUser;
                token2 = token;
                mToken = mToken2;
                observersOfUser2 = observersOfUser;
                oToken = oToken2;
                predicate3 = predicate;
            }
            token = token2;
            mToken2 = mToken;
            userId = userId2;
            observersOfUser = observersOfUser2;
            numbOfObserversPerUser = numbOfObserversPerUser2;
            protoOutputStream.end(oToken);
            userIdx++;
            i = n;
            token2 = token;
            contentObserverController = this;
            predicate3 = predicate;
        }
        token = token2;
        n = i;
        protoOutputStream.end(mToken);
        protoOutputStream.end(token);
    }
}
