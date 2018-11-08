package com.android.server.job;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobInfo.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.IoThread;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.audio.AudioService;
import com.android.server.content.SyncJobService;
import com.android.server.job.JobSchedulerInternal.JobStorePersistStats;
import com.android.server.job.controllers.JobStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class JobStore {
    private static final boolean DEBUG = false;
    private static final int JOBS_FILE_VERSION = 0;
    private static final int MAX_OPS_BEFORE_WRITE = 1;
    private static final String TAG = "JobStore";
    private static final String XML_TAG_EXTRAS = "extras";
    private static final String XML_TAG_ONEOFF = "one-off";
    private static final String XML_TAG_PARAMS_CONSTRAINTS = "constraints";
    private static final String XML_TAG_PERIODIC = "periodic";
    private static JobStore sSingleton;
    private static final Object sSingletonLock = new Object();
    final Context mContext;
    private int mDirtyOperations;
    private final Handler mIoHandler = IoThread.getHandler();
    final JobSet mJobSet;
    private final AtomicFile mJobsFile;
    final Object mLock;
    private JobStorePersistStats mPersistInfo = new JobStorePersistStats();
    private boolean mRtcGood;
    private final Runnable mWriteRunnable = new Runnable() {
        public void run() {
            long startElapsed = SystemClock.elapsedRealtime();
            final List<JobStatus> storeCopy = new ArrayList();
            synchronized (JobStore.this.mLock) {
                JobStore.this.mJobSet.forEachJob(new JobStatusFunctor() {
                    public void process(JobStatus job) {
                        if (job.isPersisted()) {
                            storeCopy.add(new JobStatus(job));
                        }
                    }
                });
            }
            writeJobsMapImpl(storeCopy);
        }

        private void writeJobsMapImpl(List<JobStatus> jobList) {
            JobStorePersistStats -get1;
            int numJobs = 0;
            int numSystemJobs = 0;
            int numSyncJobs = 0;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(baos, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                out.startTag(null, "job-info");
                out.attribute(null, "version", Integer.toString(0));
                for (int i = 0; i < jobList.size(); i++) {
                    JobStatus jobStatus = (JobStatus) jobList.get(i);
                    out.startTag(null, "job");
                    addAttributesToJobTag(out, jobStatus);
                    writeConstraintsToXml(out, jobStatus);
                    writeExecutionCriteriaToXml(out, jobStatus);
                    writeBundleToXml(jobStatus.getJob().getExtras(), out);
                    out.endTag(null, "job");
                    numJobs++;
                    if (jobStatus.getUid() == 1000) {
                        numSystemJobs++;
                        if (JobStore.isSyncJob(jobStatus)) {
                            numSyncJobs++;
                        }
                    }
                }
                out.endTag(null, "job-info");
                out.endDocument();
                FileOutputStream fos = JobStore.this.mJobsFile.startWrite();
                fos.write(baos.toByteArray());
                JobStore.this.mJobsFile.finishWrite(fos);
                JobStore.this.mDirtyOperations = 0;
                JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = numSystemJobs;
                -get1 = JobStore.this.mPersistInfo;
            } catch (IOException e) {
                JobStore.this.mPersistInfo.countAllJobsSaved = 0;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = 0;
                -get1 = JobStore.this.mPersistInfo;
            } catch (XmlPullParserException e2) {
                JobStore.this.mPersistInfo.countAllJobsSaved = 0;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = 0;
                -get1 = JobStore.this.mPersistInfo;
            } catch (Throwable th) {
                JobStore.this.mPersistInfo.countAllJobsSaved = 0;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = 0;
                JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = 0;
            }
            -get1.countSystemSyncManagerJobsSaved = numSyncJobs;
        }

        private void addAttributesToJobTag(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            out.attribute(null, HwBroadcastRadarUtil.KEY_PACKAGE, jobStatus.getServiceComponent().getPackageName());
            out.attribute(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS, jobStatus.getServiceComponent().getClassName());
            if (jobStatus.getSourcePackageName() != null) {
                out.attribute(null, "sourcePackageName", jobStatus.getSourcePackageName());
            }
            if (jobStatus.getSourceTag() != null) {
                out.attribute(null, "sourceTag", jobStatus.getSourceTag());
            }
            out.attribute(null, "sourceUserId", String.valueOf(jobStatus.getSourceUserId()));
            out.attribute(null, "uid", Integer.toString(jobStatus.getUid()));
            out.attribute(null, "priority", String.valueOf(jobStatus.getPriority()));
            out.attribute(null, "flags", String.valueOf(jobStatus.getFlags()));
            out.attribute(null, "lastSuccessfulRunTime", String.valueOf(jobStatus.getLastSuccessfulRunTime()));
            out.attribute(null, "lastFailedRunTime", String.valueOf(jobStatus.getLastFailedRunTime()));
        }

        private void writeBundleToXml(PersistableBundle extras, XmlSerializer out) throws IOException, XmlPullParserException {
            out.startTag(null, JobStore.XML_TAG_EXTRAS);
            deepCopyBundle(extras, 10).saveToXml(out);
            out.endTag(null, JobStore.XML_TAG_EXTRAS);
        }

        private PersistableBundle deepCopyBundle(PersistableBundle bundle, int maxDepth) {
            if (maxDepth <= 0) {
                return null;
            }
            PersistableBundle copy = (PersistableBundle) bundle.clone();
            for (String key : bundle.keySet()) {
                Object o = copy.get(key);
                if (o instanceof PersistableBundle) {
                    copy.putPersistableBundle(key, deepCopyBundle((PersistableBundle) o, maxDepth - 1));
                }
            }
            return copy;
        }

        private void writeConstraintsToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.startTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
            if (jobStatus.needsAnyConnectivity()) {
                out.attribute(null, "connectivity", Boolean.toString(true));
            }
            if (jobStatus.needsMeteredConnectivity()) {
                out.attribute(null, "metered", Boolean.toString(true));
            }
            if (jobStatus.needsUnmeteredConnectivity()) {
                out.attribute(null, "unmetered", Boolean.toString(true));
            }
            if (jobStatus.needsNonRoamingConnectivity()) {
                out.attribute(null, "not-roaming", Boolean.toString(true));
            }
            if (jobStatus.hasIdleConstraint()) {
                out.attribute(null, "idle", Boolean.toString(true));
            }
            if (jobStatus.hasChargingConstraint()) {
                out.attribute(null, "charging", Boolean.toString(true));
            }
            if (jobStatus.hasBatteryNotLowConstraint()) {
                out.attribute(null, "battery-not-low", Boolean.toString(true));
            }
            out.endTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
        }

        private void writeExecutionCriteriaToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            JobInfo job = jobStatus.getJob();
            if (jobStatus.getJob().isPeriodic()) {
                out.startTag(null, JobStore.XML_TAG_PERIODIC);
                out.attribute(null, "period", Long.toString(job.getIntervalMillis()));
                out.attribute(null, "flex", Long.toString(job.getFlexMillis()));
            } else {
                out.startTag(null, JobStore.XML_TAG_ONEOFF);
            }
            Pair<Long, Long> utcJobTimes = jobStatus.getPersistedUtcTimes();
            long nowRTC = System.currentTimeMillis();
            long nowElapsed = SystemClock.elapsedRealtime();
            if (jobStatus.hasDeadlineConstraint()) {
                long deadlineWallclock;
                if (utcJobTimes == null) {
                    deadlineWallclock = nowRTC + (jobStatus.getLatestRunTimeElapsed() - nowElapsed);
                } else {
                    deadlineWallclock = ((Long) utcJobTimes.second).longValue();
                }
                out.attribute(null, "deadline", Long.toString(deadlineWallclock));
            }
            if (jobStatus.hasTimingDelayConstraint()) {
                long delayWallclock;
                if (utcJobTimes == null) {
                    delayWallclock = nowRTC + (jobStatus.getEarliestRunTime() - nowElapsed);
                } else {
                    delayWallclock = ((Long) utcJobTimes.first).longValue();
                }
                out.attribute(null, "delay", Long.toString(delayWallclock));
            }
            if (!(jobStatus.getJob().getInitialBackoffMillis() == 30000 && jobStatus.getJob().getBackoffPolicy() == 1)) {
                out.attribute(null, "backoff-policy", Integer.toString(job.getBackoffPolicy()));
                out.attribute(null, "initial-backoff", Long.toString(job.getInitialBackoffMillis()));
            }
            if (job.isPeriodic()) {
                out.endTag(null, JobStore.XML_TAG_PERIODIC);
            } else {
                out.endTag(null, JobStore.XML_TAG_ONEOFF);
            }
        }
    };
    private final long mXmlTimestamp;

    public interface JobStatusFunctor {
        void process(JobStatus jobStatus);
    }

    static final class JobSet {
        private SparseArray<ArraySet<JobStatus>> mJobs = new SparseArray();

        public List<JobStatus> getJobsByUid(int uid) {
            ArrayList<JobStatus> matchingJobs = new ArrayList();
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs != null) {
                matchingJobs.addAll(jobs);
            }
            return matchingJobs;
        }

        public List<JobStatus> getJobsByUser(int userId) {
            ArrayList<JobStatus> result = new ArrayList();
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mJobs.keyAt(i)) == userId) {
                    ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.valueAt(i);
                    if (jobs != null) {
                        result.addAll(jobs);
                    }
                }
            }
            return result;
        }

        public boolean add(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs == null) {
                jobs = new ArraySet();
                this.mJobs.put(uid, jobs);
            }
            return jobs.add(job);
        }

        public boolean remove(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            boolean remove = jobs != null ? jobs.remove(job) : false;
            if (remove && jobs.size() == 0) {
                this.mJobs.remove(uid);
            }
            return remove;
        }

        public void removeJobsOfNonUsers(int[] whitelist) {
            for (int jobIndex = this.mJobs.size() - 1; jobIndex >= 0; jobIndex--) {
                if (!ArrayUtils.contains(whitelist, UserHandle.getUserId(this.mJobs.keyAt(jobIndex)))) {
                    this.mJobs.removeAt(jobIndex);
                }
            }
        }

        public boolean contains(JobStatus job) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(job.getUid());
            return jobs != null ? jobs.contains(job) : false;
        }

        public JobStatus get(int uid, int jobId) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = (JobStatus) jobs.valueAt(i);
                    if (job.getJobId() == jobId) {
                        return job;
                    }
                }
            }
            return null;
        }

        public List<JobStatus> getAllJobs() {
            ArrayList<JobStatus> allJobs = new ArrayList(size());
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.valueAt(i);
                if (jobs != null) {
                    for (int j = jobs.size() - 1; j >= 0; j--) {
                        allJobs.add((JobStatus) jobs.valueAt(j));
                    }
                }
            }
            return allJobs;
        }

        public void clear() {
            this.mJobs.clear();
        }

        public int size() {
            int total = 0;
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                total += ((ArraySet) this.mJobs.valueAt(i)).size();
            }
            return total;
        }

        public int countJobsForUid(int uid) {
            int total = 0;
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = (JobStatus) jobs.valueAt(i);
                    if (job.getUid() == job.getSourceUid()) {
                        total++;
                    }
                }
            }
            return total;
        }

        public void forEachJob(JobStatusFunctor functor) {
            for (int uidIndex = this.mJobs.size() - 1; uidIndex >= 0; uidIndex--) {
                ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.valueAt(uidIndex);
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.process((JobStatus) jobs.valueAt(i));
                }
            }
        }

        public void forEachJob(int uid, JobStatusFunctor functor) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.process((JobStatus) jobs.valueAt(i));
                }
            }
        }
    }

    private final class ReadJobMapFromDiskRunnable implements Runnable {
        private final JobSet jobSet;
        private final boolean rtcGood;

        ReadJobMapFromDiskRunnable(JobSet jobSet, boolean rtcIsGood) {
            this.jobSet = jobSet;
            this.rtcGood = rtcIsGood;
        }

        public void run() {
            int numJobs = 0;
            int numSystemJobs = 0;
            int numSyncJobs = 0;
            JobStorePersistStats -get1;
            try {
                FileInputStream fis = JobStore.this.mJobsFile.openRead();
                synchronized (JobStore.this.mLock) {
                    List<JobStatus> jobs = readJobMapImpl(fis, this.rtcGood);
                    if (jobs != null) {
                        long now = SystemClock.elapsedRealtime();
                        IActivityManager am = ActivityManager.getService();
                        for (int i = 0; i < jobs.size(); i++) {
                            JobStatus js = (JobStatus) jobs.get(i);
                            js.prepareLocked(am);
                            js.enqueueTime = now;
                            this.jobSet.add(js);
                            numJobs++;
                            if (js.getUid() == 1000) {
                                numSystemJobs++;
                                if (JobStore.isSyncJob(js)) {
                                    numSyncJobs++;
                                }
                            }
                        }
                    }
                }
                fis.close();
                if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                    JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                    JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                    -get1 = JobStore.this.mPersistInfo;
                    -get1.countSystemSyncManagerJobsLoaded = numSyncJobs;
                }
            } catch (FileNotFoundException e) {
                if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                    JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                    JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                    -get1 = JobStore.this.mPersistInfo;
                }
            } catch (Exception e2) {
                try {
                    Slog.wtf(JobStore.TAG, "Error jobstore xml.", e2);
                    if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                        JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                        -get1 = JobStore.this.mPersistInfo;
                    }
                } catch (Throwable th) {
                    if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                        JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsLoaded = numSyncJobs;
                    }
                }
            }
            Slog.i(JobStore.TAG, "Read " + numJobs + " jobs");
        }

        private List<JobStatus> readJobMapImpl(FileInputStream fis, boolean rtcIsGood) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
                Slog.d(JobStore.TAG, "Start tag: " + parser.getName());
            }
            if (eventType == 1) {
                return null;
            }
            if (!"job-info".equals(parser.getName())) {
                return null;
            }
            List<JobStatus> jobs = new ArrayList();
            try {
                if (Integer.parseInt(parser.getAttributeValue(null, "version")) != 0) {
                    Slog.d(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                    return null;
                }
                eventType = parser.next();
                do {
                    if (eventType == 2) {
                        if ("job".equals(parser.getName())) {
                            JobStatus persistedJob = restoreJobFromXml(rtcIsGood, parser);
                            if (persistedJob != null) {
                                jobs.add(persistedJob);
                            } else {
                                Slog.d(JobStore.TAG, "Error reading job from file.");
                            }
                        }
                    }
                    eventType = parser.next();
                } while (eventType != 1);
                return jobs;
            } catch (NumberFormatException e) {
                Slog.e(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                return null;
            }
        }

        private JobStatus restoreJobFromXml(boolean rtcIsGood, XmlPullParser parser) throws XmlPullParserException, IOException {
            try {
                int eventType;
                boolean equals;
                Builder jobBuilder = buildBuilderFromXml(parser);
                jobBuilder.setPersisted(true);
                int uid = Integer.parseInt(parser.getAttributeValue(null, "uid"));
                String val = parser.getAttributeValue(null, "priority");
                if (val != null) {
                    jobBuilder.setPriority(Integer.parseInt(val));
                }
                val = parser.getAttributeValue(null, "flags");
                if (val != null) {
                    jobBuilder.setFlags(Integer.parseInt(val));
                }
                val = parser.getAttributeValue(null, "sourceUserId");
                int sourceUserId = val == null ? -1 : Integer.parseInt(val);
                val = parser.getAttributeValue(null, "lastSuccessfulRunTime");
                long lastSuccessfulRunTime = val == null ? 0 : Long.parseLong(val);
                val = parser.getAttributeValue(null, "lastFailedRunTime");
                long lastFailedRunTime = val == null ? 0 : Long.parseLong(val);
                String sourcePackageName = parser.getAttributeValue(null, "sourcePackageName");
                String sourceTag = parser.getAttributeValue(null, "sourceTag");
                do {
                    eventType = parser.next();
                } while (eventType == 4);
                if (eventType == 2) {
                    equals = JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(parser.getName());
                } else {
                    equals = false;
                }
                if (!equals) {
                    return null;
                }
                try {
                    buildConstraintsFromXml(jobBuilder, parser);
                    parser.next();
                    do {
                        eventType = parser.next();
                    } while (eventType == 4);
                    if (eventType != 2) {
                        return null;
                    }
                    try {
                        Pair<Long, Long> rtcRuntimes = buildRtcExecutionTimesFromXml(parser);
                        long elapsedNow = SystemClock.elapsedRealtime();
                        Pair<Long, Long> elapsedRuntimes = JobStore.convertRtcBoundsToElapsed(rtcRuntimes, elapsedNow);
                        if (JobStore.XML_TAG_PERIODIC.equals(parser.getName())) {
                            try {
                                long periodMillis = Long.parseLong(parser.getAttributeValue(null, "period"));
                                val = parser.getAttributeValue(null, "flex");
                                long flexMillis = val != null ? Long.valueOf(val).longValue() : periodMillis;
                                jobBuilder.setPeriodic(periodMillis, flexMillis);
                                if (((Long) elapsedRuntimes.second).longValue() > (elapsedNow + periodMillis) + flexMillis) {
                                    long clampedLateRuntimeElapsed = (elapsedNow + flexMillis) + periodMillis;
                                    Slog.w(JobStore.TAG, String.format("Periodic job for uid='%d' persisted run-time is too big [%s, %s]. Clamping to [%s,%s]", new Object[]{Integer.valueOf(uid), DateUtils.formatElapsedTime(((Long) elapsedRuntimes.first).longValue() / 1000), DateUtils.formatElapsedTime(((Long) elapsedRuntimes.second).longValue() / 1000), DateUtils.formatElapsedTime((clampedLateRuntimeElapsed - flexMillis) / 1000), DateUtils.formatElapsedTime(clampedLateRuntimeElapsed / 1000)}));
                                    elapsedRuntimes = Pair.create(Long.valueOf(clampedEarlyRuntimeElapsed), Long.valueOf(clampedLateRuntimeElapsed));
                                }
                            } catch (NumberFormatException e) {
                                Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                return null;
                            }
                        } else if (!JobStore.XML_TAG_ONEOFF.equals(parser.getName())) {
                            return null;
                        } else {
                            try {
                                if (((Long) elapsedRuntimes.first).longValue() != 0) {
                                    jobBuilder.setMinimumLatency(((Long) elapsedRuntimes.first).longValue() - elapsedNow);
                                }
                                if (((Long) elapsedRuntimes.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                    jobBuilder.setOverrideDeadline(((Long) elapsedRuntimes.second).longValue() - elapsedNow);
                                }
                            } catch (NumberFormatException e2) {
                                Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                return null;
                            }
                        }
                        maybeBuildBackoffPolicyFromXml(jobBuilder, parser);
                        parser.nextTag();
                        do {
                            eventType = parser.next();
                        } while (eventType == 4);
                        if (eventType == 2) {
                            equals = JobStore.XML_TAG_EXTRAS.equals(parser.getName());
                        } else {
                            equals = false;
                        }
                        if (!equals) {
                            return null;
                        }
                        Pair pair;
                        PersistableBundle extras = PersistableBundle.restoreFromXml(parser);
                        jobBuilder.setExtras(extras);
                        parser.nextTag();
                        if ("android".equals(sourcePackageName) && extras != null) {
                            if (extras.getBoolean("SyncManagerJob", false)) {
                                sourcePackageName = extras.getString("owningPackage", sourcePackageName);
                            }
                        }
                        JobInfo build = jobBuilder.build();
                        long longValue = ((Long) elapsedRuntimes.first).longValue();
                        long longValue2 = ((Long) elapsedRuntimes.second).longValue();
                        if (rtcIsGood) {
                            pair = null;
                        } else {
                            Pair<Long, Long> pair2 = rtcRuntimes;
                        }
                        return new JobStatus(build, uid, sourcePackageName, sourceUserId, sourceTag, longValue, longValue2, lastSuccessfulRunTime, lastFailedRunTime, pair);
                    } catch (NumberFormatException e3) {
                        return null;
                    }
                } catch (NumberFormatException e4) {
                    Slog.d(JobStore.TAG, "Error reading constraints, skipping.");
                    return null;
                }
            } catch (NumberFormatException e5) {
                Slog.e(JobStore.TAG, "Error parsing job's required fields, skipping");
                return null;
            }
        }

        private Builder buildBuilderFromXml(XmlPullParser parser) throws NumberFormatException {
            return new Builder(Integer.parseInt(parser.getAttributeValue(null, "jobid")), new ComponentName(parser.getAttributeValue(null, HwBroadcastRadarUtil.KEY_PACKAGE), parser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS)));
        }

        private void buildConstraintsFromXml(Builder jobBuilder, XmlPullParser parser) {
            if (parser.getAttributeValue(null, "connectivity") != null) {
                jobBuilder.setRequiredNetworkType(1);
            }
            if (parser.getAttributeValue(null, "metered") != null) {
                jobBuilder.setRequiredNetworkType(4);
            }
            if (parser.getAttributeValue(null, "unmetered") != null) {
                jobBuilder.setRequiredNetworkType(2);
            }
            if (parser.getAttributeValue(null, "not-roaming") != null) {
                jobBuilder.setRequiredNetworkType(3);
            }
            if (parser.getAttributeValue(null, "idle") != null) {
                jobBuilder.setRequiresDeviceIdle(true);
            }
            if (parser.getAttributeValue(null, "charging") != null) {
                jobBuilder.setRequiresCharging(true);
            }
        }

        private void maybeBuildBackoffPolicyFromXml(Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "initial-backoff");
            if (val != null) {
                jobBuilder.setBackoffCriteria(Long.parseLong(val), Integer.parseInt(parser.getAttributeValue(null, "backoff-policy")));
            }
        }

        private Pair<Long, Long> buildRtcExecutionTimesFromXml(XmlPullParser parser) throws NumberFormatException {
            long earliestRunTimeRtc;
            long latestRunTimeRtc;
            String val = parser.getAttributeValue(null, "delay");
            if (val != null) {
                earliestRunTimeRtc = Long.parseLong(val);
            } else {
                earliestRunTimeRtc = 0;
            }
            val = parser.getAttributeValue(null, "deadline");
            if (val != null) {
                latestRunTimeRtc = Long.parseLong(val);
            } else {
                latestRunTimeRtc = JobStatus.NO_LATEST_RUNTIME;
            }
            return Pair.create(Long.valueOf(earliestRunTimeRtc), Long.valueOf(latestRunTimeRtc));
        }

        private Pair<Long, Long> buildExecutionTimesFromXml(XmlPullParser parser) throws NumberFormatException {
            long nowWallclock = System.currentTimeMillis();
            long nowElapsed = SystemClock.elapsedRealtime();
            long earliestRunTimeElapsed = 0;
            long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
            String val = parser.getAttributeValue(null, "deadline");
            if (val != null) {
                latestRunTimeElapsed = nowElapsed + Math.max(Long.parseLong(val) - nowWallclock, 0);
            }
            val = parser.getAttributeValue(null, "delay");
            if (val != null) {
                earliestRunTimeElapsed = nowElapsed + Math.max(Long.parseLong(val) - nowWallclock, 0);
            }
            return Pair.create(Long.valueOf(earliestRunTimeElapsed), Long.valueOf(latestRunTimeElapsed));
        }
    }

    static JobStore initAndGet(JobSchedulerService jobManagerService) {
        JobStore jobStore;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                sSingleton = new JobStore(jobManagerService.getContext(), jobManagerService.getLock(), Environment.getDataDirectory());
            }
            jobStore = sSingleton;
        }
        return jobStore;
    }

    public static JobStore initAndGetForTesting(Context context, File dataDir) {
        JobStore jobStoreUnderTest = new JobStore(context, new Object(), dataDir);
        jobStoreUnderTest.clear();
        return jobStoreUnderTest;
    }

    private JobStore(Context context, Object lock, File dataDir) {
        boolean z = false;
        this.mLock = lock;
        this.mContext = context;
        this.mDirtyOperations = 0;
        File jobDir = new File(new File(dataDir, "system"), "job");
        jobDir.mkdirs();
        this.mJobsFile = new AtomicFile(new File(jobDir, "jobs.xml"));
        this.mJobSet = new JobSet();
        this.mXmlTimestamp = this.mJobsFile.getLastModifiedTime();
        if (System.currentTimeMillis() > this.mXmlTimestamp) {
            z = true;
        }
        this.mRtcGood = z;
        readJobMapFromDisk(this.mJobSet, this.mRtcGood);
    }

    public boolean jobTimesInflatedValid() {
        return this.mRtcGood;
    }

    public boolean clockNowValidToInflate(long now) {
        return now >= this.mXmlTimestamp;
    }

    public void getRtcCorrectedJobsLocked(ArrayList<JobStatus> toAdd, ArrayList<JobStatus> toRemove) {
        forEachJob(new -$Lambda$uHhK2abi5qBUVZxkpfjqb2-WntE(SystemClock.elapsedRealtime(), toAdd, toRemove));
    }

    static /* synthetic */ void lambda$-com_android_server_job_JobStore_6419(long elapsedNow, ArrayList toAdd, ArrayList toRemove, JobStatus job) {
        Pair<Long, Long> utcTimes = job.getPersistedUtcTimes();
        if (utcTimes != null) {
            Pair<Long, Long> elapsedRuntimes = convertRtcBoundsToElapsed(utcTimes, elapsedNow);
            ArrayList arrayList = toAdd;
            arrayList.add(new JobStatus(job, ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue(), 0, job.getLastSuccessfulRunTime(), job.getLastFailedRunTime()));
            toRemove.add(job);
        }
    }

    public boolean add(JobStatus jobStatus) {
        boolean replaced = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return replaced;
    }

    boolean containsJob(JobStatus jobStatus) {
        return this.mJobSet.contains(jobStatus);
    }

    public int size() {
        return this.mJobSet.size();
    }

    public JobStorePersistStats getPersistStats() {
        return this.mPersistInfo;
    }

    public int countJobsForUid(int uid) {
        return this.mJobSet.countJobsForUid(uid);
    }

    public boolean remove(JobStatus jobStatus, boolean writeBack) {
        boolean removed = this.mJobSet.remove(jobStatus);
        if (!removed) {
            return false;
        }
        if (writeBack && jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return removed;
    }

    public void removeJobsOfNonUsers(int[] whitelist) {
        this.mJobSet.removeJobsOfNonUsers(whitelist);
    }

    public void clear() {
        this.mJobSet.clear();
        maybeWriteStatusToDiskAsync();
    }

    public List<JobStatus> getJobsByUser(int userHandle) {
        return this.mJobSet.getJobsByUser(userHandle);
    }

    public List<JobStatus> getJobsByUid(int uid) {
        return this.mJobSet.getJobsByUid(uid);
    }

    public JobStatus getJobByUidAndJobId(int uid, int jobId) {
        return this.mJobSet.get(uid, jobId);
    }

    public void forEachJob(JobStatusFunctor functor) {
        this.mJobSet.forEachJob(functor);
    }

    public void forEachJob(int uid, JobStatusFunctor functor) {
        this.mJobSet.forEachJob(uid, functor);
    }

    private void maybeWriteStatusToDiskAsync() {
        this.mDirtyOperations++;
        if (this.mDirtyOperations >= 1) {
            this.mIoHandler.removeCallbacks(this.mWriteRunnable);
            this.mIoHandler.post(this.mWriteRunnable);
        }
    }

    public void readJobMapFromDisk(JobSet jobSet, boolean rtcGood) {
        new ReadJobMapFromDiskRunnable(jobSet, rtcGood).run();
    }

    private static Pair<Long, Long> convertRtcBoundsToElapsed(Pair<Long, Long> rtcTimes, long nowElapsed) {
        long earliest;
        long latest;
        long nowWallclock = System.currentTimeMillis();
        if (((Long) rtcTimes.first).longValue() > 0) {
            earliest = nowElapsed + Math.max(((Long) rtcTimes.first).longValue() - nowWallclock, 0);
        } else {
            earliest = 0;
        }
        if (((Long) rtcTimes.second).longValue() < JobStatus.NO_LATEST_RUNTIME) {
            latest = nowElapsed + Math.max(((Long) rtcTimes.second).longValue() - nowWallclock, 0);
        } else {
            latest = JobStatus.NO_LATEST_RUNTIME;
        }
        return Pair.create(Long.valueOf(earliest), Long.valueOf(latest));
    }

    private static boolean isSyncJob(JobStatus status) {
        return SyncJobService.class.getName().equals(status.getServiceComponent().getClassName());
    }
}
