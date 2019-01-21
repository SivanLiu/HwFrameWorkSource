package com.android.server.job;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobInfo.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkRequest;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.BitUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.audio.AudioService;
import com.android.server.content.SyncJobService;
import com.android.server.job.JobSchedulerInternal.JobStorePersistStats;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class JobStore {
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
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
            long startElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            List<JobStatus> storeCopy = new ArrayList();
            synchronized (JobStore.this.mLock) {
                JobStore.this.mJobSet.forEachJob(null, new -$$Lambda$JobStore$1$Wgepg1oHZp0-Q01q1baIVZKWujU(storeCopy));
            }
            writeJobsMapImpl(storeCopy);
            if (JobStore.DEBUG) {
                String str = JobStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Finished writing, took ");
                stringBuilder.append(JobSchedulerService.sElapsedRealtimeClock.millis() - startElapsed);
                stringBuilder.append("ms");
                Slog.v(str, stringBuilder.toString());
            }
        }

        static /* synthetic */ void lambda$run$0(List storeCopy, JobStatus job) {
            if (job.isPersisted()) {
                storeCopy.add(new JobStatus(job));
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:47:0x0125 A:{Catch:{ IOException -> 0x011c, XmlPullParserException -> 0x010b, all -> 0x0104, all -> 0x0146 }} */
        /* JADX WARNING: Removed duplicated region for block: B:42:0x0114 A:{Catch:{ IOException -> 0x011c, XmlPullParserException -> 0x010b, all -> 0x0104, all -> 0x0146 }} */
        /* JADX WARNING: Removed duplicated region for block: B:47:0x0125 A:{Catch:{ IOException -> 0x011c, XmlPullParserException -> 0x010b, all -> 0x0104, all -> 0x0146 }} */
        /* JADX WARNING: Removed duplicated region for block: B:42:0x0114 A:{Catch:{ IOException -> 0x011c, XmlPullParserException -> 0x010b, all -> 0x0104, all -> 0x0146 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void writeJobsMapImpl(List<JobStatus> jobList) {
            IOException e;
            XmlPullParserException e2;
            Throwable th;
            List<JobStatus> list;
            int numJobs = 0;
            int numJobs2 = 0;
            int numSystemJobs = 0;
            int numSyncJobs;
            try {
                long startTime = SystemClock.uptimeMillis();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(baos, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                out.startTag(null, "job-info");
                out.attribute(null, "version", Integer.toString(0));
                numSyncJobs = numSystemJobs;
                numSystemJobs = 0;
                numJobs2 = 0;
                numJobs = 0;
                while (numJobs < jobList.size()) {
                    try {
                        try {
                            JobStatus jobStatus = (JobStatus) jobList.get(numJobs);
                            if (JobStore.DEBUG) {
                                String str = JobStore.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Saving job ");
                                stringBuilder.append(jobStatus.getJobId());
                                Slog.d(str, stringBuilder.toString());
                            }
                            out.startTag(null, "job");
                            addAttributesToJobTag(out, jobStatus);
                            writeConstraintsToXml(out, jobStatus);
                            writeExecutionCriteriaToXml(out, jobStatus);
                            writeBundleToXml(jobStatus.getJob().getExtras(), out);
                            out.endTag(null, "job");
                            numJobs2++;
                            if (jobStatus.getUid() == 1000) {
                                numSystemJobs++;
                                if (JobStore.isSyncJob(jobStatus)) {
                                    numSyncJobs++;
                                }
                            }
                            numJobs++;
                        } catch (IOException e3) {
                            e = e3;
                            numJobs = numJobs2;
                            numJobs2 = numSystemJobs;
                            numSystemJobs = numSyncJobs;
                            if (JobStore.DEBUG) {
                            }
                            JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
                        } catch (XmlPullParserException e4) {
                            e2 = e4;
                            numJobs = numJobs2;
                            numJobs2 = numSystemJobs;
                            numSystemJobs = numSyncJobs;
                            if (JobStore.DEBUG) {
                            }
                            JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
                        } catch (Throwable th2) {
                            th = th2;
                            JobStore.this.mPersistInfo.countAllJobsSaved = numJobs2;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = numSystemJobs;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSyncJobs;
                            throw th;
                        }
                    } catch (IOException e5) {
                        e = e5;
                        list = jobList;
                        numJobs = numJobs2;
                        numJobs2 = numSystemJobs;
                        numSystemJobs = numSyncJobs;
                        if (JobStore.DEBUG) {
                        }
                        JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
                    } catch (XmlPullParserException e6) {
                        e2 = e6;
                        list = jobList;
                        numJobs = numJobs2;
                        numJobs2 = numSystemJobs;
                        numSystemJobs = numSyncJobs;
                        if (JobStore.DEBUG) {
                        }
                        JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
                    } catch (Throwable th3) {
                        th = th3;
                        list = jobList;
                        JobStore.this.mPersistInfo.countAllJobsSaved = numJobs2;
                        JobStore.this.mPersistInfo.countSystemServerJobsSaved = numSystemJobs;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSyncJobs;
                        throw th;
                    }
                }
                list = jobList;
                out.endTag(null, "job-info");
                out.endDocument();
                FileOutputStream fos = JobStore.this.mJobsFile.startWrite(startTime);
                fos.write(baos.toByteArray());
                JobStore.this.mJobsFile.finishWrite(fos);
                JobStore.this.mDirtyOperations = 0;
                JobStore.this.mPersistInfo.countAllJobsSaved = numJobs2;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = numSystemJobs;
                JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSyncJobs;
                numJobs = numJobs2;
            } catch (IOException e7) {
                e = e7;
                list = jobList;
                if (JobStore.DEBUG) {
                    Slog.v(JobStore.TAG, "Error writing out job data.", e);
                }
                JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
            } catch (XmlPullParserException e8) {
                e2 = e8;
                list = jobList;
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "Error persisting bundle.", e2);
                }
                JobStore.this.mPersistInfo.countAllJobsSaved = numJobs;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = numJobs2;
                JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSystemJobs;
            } catch (Throwable th4) {
                th = th4;
                numSyncJobs = numSystemJobs;
                numSystemJobs = numJobs2;
                numJobs2 = numJobs;
                JobStore.this.mPersistInfo.countAllJobsSaved = numJobs2;
                JobStore.this.mPersistInfo.countSystemServerJobsSaved = numSystemJobs;
                JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = numSyncJobs;
                throw th;
            }
        }

        private void addAttributesToJobTag(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            out.attribute(null, "package", jobStatus.getServiceComponent().getPackageName());
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
            if (jobStatus.getInternalFlags() != 0) {
                out.attribute(null, "internalFlags", String.valueOf(jobStatus.getInternalFlags()));
            }
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
            if (jobStatus.hasConnectivityConstraint()) {
                NetworkRequest network = jobStatus.getJob().getRequiredNetwork();
                out.attribute(null, "net-capabilities", Long.toString(BitUtils.packBits(network.networkCapabilities.getCapabilities())));
                out.attribute(null, "net-unwanted-capabilities", Long.toString(BitUtils.packBits(network.networkCapabilities.getUnwantedCapabilities())));
                out.attribute(null, "net-transport-types", Long.toString(BitUtils.packBits(network.networkCapabilities.getTransportTypes())));
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
            long deadlineWallclock;
            JobInfo job = jobStatus.getJob();
            if (jobStatus.getJob().isPeriodic()) {
                out.startTag(null, JobStore.XML_TAG_PERIODIC);
                out.attribute(null, "period", Long.toString(job.getIntervalMillis()));
                out.attribute(null, "flex", Long.toString(job.getFlexMillis()));
            } else {
                out.startTag(null, JobStore.XML_TAG_ONEOFF);
            }
            Pair<Long, Long> utcJobTimes = jobStatus.getPersistedUtcTimes();
            if (JobStore.DEBUG && utcJobTimes != null) {
                String str = JobStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("storing original UTC timestamps for ");
                stringBuilder.append(jobStatus);
                Slog.i(str, stringBuilder.toString());
            }
            long nowRTC = JobSchedulerService.sSystemClock.millis();
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobStatus.hasDeadlineConstraint()) {
                if (utcJobTimes == null) {
                    deadlineWallclock = (jobStatus.getLatestRunTimeElapsed() - nowElapsed) + nowRTC;
                } else {
                    deadlineWallclock = ((Long) utcJobTimes.second).longValue();
                }
                out.attribute(null, "deadline", Long.toString(deadlineWallclock));
            }
            if (jobStatus.hasTimingDelayConstraint()) {
                if (utcJobTimes == null) {
                    deadlineWallclock = (jobStatus.getEarliestRunTime() - nowElapsed) + nowRTC;
                } else {
                    deadlineWallclock = ((Long) utcJobTimes.first).longValue();
                }
                out.attribute(null, "delay", Long.toString(deadlineWallclock));
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

    static final class JobSet {
        @VisibleForTesting
        final SparseArray<ArraySet<JobStatus>> mJobs = new SparseArray();
        @VisibleForTesting
        final SparseArray<ArraySet<JobStatus>> mJobsPerSourceUid = new SparseArray();

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
            for (int i = this.mJobsPerSourceUid.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mJobsPerSourceUid.keyAt(i)) == userId) {
                    ArraySet<JobStatus> jobs = (ArraySet) this.mJobsPerSourceUid.valueAt(i);
                    if (jobs != null) {
                        result.addAll(jobs);
                    }
                }
            }
            return result;
        }

        public boolean add(JobStatus job) {
            int uid = job.getUid();
            int sourceUid = job.getSourceUid();
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            if (jobs == null) {
                jobs = new ArraySet();
                this.mJobs.put(uid, jobs);
            }
            ArraySet<JobStatus> jobsForSourceUid = (ArraySet) this.mJobsPerSourceUid.get(sourceUid);
            if (jobsForSourceUid == null) {
                jobsForSourceUid = new ArraySet();
                this.mJobsPerSourceUid.put(sourceUid, jobsForSourceUid);
            }
            boolean added = jobs.add(job);
            boolean addedInSource = jobsForSourceUid.add(job);
            if (added != addedInSource) {
                String str = JobStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mJobs and mJobsPerSourceUid mismatch; caller= ");
                stringBuilder.append(added);
                stringBuilder.append(" source= ");
                stringBuilder.append(addedInSource);
                Slog.wtf(str, stringBuilder.toString());
            }
            return added || addedInSource;
        }

        public boolean remove(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(uid);
            int sourceUid = job.getSourceUid();
            ArraySet<JobStatus> jobsForSourceUid = (ArraySet) this.mJobsPerSourceUid.get(sourceUid);
            boolean didRemove = jobs != null && jobs.remove(job);
            boolean sourceRemove = jobsForSourceUid != null && jobsForSourceUid.remove(job);
            if (didRemove != sourceRemove) {
                String str = JobStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Job presence mismatch; caller=");
                stringBuilder.append(didRemove);
                stringBuilder.append(" source=");
                stringBuilder.append(sourceRemove);
                Slog.wtf(str, stringBuilder.toString());
            }
            if (!didRemove && !sourceRemove) {
                return false;
            }
            if (jobs != null && jobs.size() == 0) {
                this.mJobs.remove(uid);
            }
            if (jobsForSourceUid != null && jobsForSourceUid.size() == 0) {
                this.mJobsPerSourceUid.remove(sourceUid);
            }
            return true;
        }

        public void removeJobsOfNonUsers(int[] whitelist) {
            removeAll(new -$$Lambda$JobStore$JobSet$D9839QVHHu4X-hnxouyIMkP5NWA(whitelist).or(new -$$Lambda$JobStore$JobSet$id1Y3Yh8Y9sEb-njlNCUNay6U9k(whitelist)));
        }

        private void removeAll(Predicate<JobStatus> predicate) {
            int jobSetIndex;
            ArraySet<JobStatus> jobs;
            int jobIndex;
            for (jobSetIndex = this.mJobs.size() - 1; jobSetIndex >= 0; jobSetIndex--) {
                jobs = (ArraySet) this.mJobs.valueAt(jobSetIndex);
                for (jobIndex = jobs.size() - 1; jobIndex >= 0; jobIndex--) {
                    if (predicate.test((JobStatus) jobs.valueAt(jobIndex))) {
                        jobs.removeAt(jobIndex);
                    }
                }
                if (jobs.size() == 0) {
                    this.mJobs.removeAt(jobSetIndex);
                }
            }
            for (jobSetIndex = this.mJobsPerSourceUid.size() - 1; jobSetIndex >= 0; jobSetIndex--) {
                jobs = (ArraySet) this.mJobsPerSourceUid.valueAt(jobSetIndex);
                for (jobIndex = jobs.size() - 1; jobIndex >= 0; jobIndex--) {
                    if (predicate.test((JobStatus) jobs.valueAt(jobIndex))) {
                        jobs.removeAt(jobIndex);
                    }
                }
                if (jobs.size() == 0) {
                    this.mJobsPerSourceUid.removeAt(jobSetIndex);
                }
            }
        }

        public boolean contains(JobStatus job) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(job.getUid());
            return jobs != null && jobs.contains(job);
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
            this.mJobsPerSourceUid.clear();
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

        public void forEachJob(Predicate<JobStatus> filterPredicate, Consumer<JobStatus> functor) {
            for (int uidIndex = this.mJobs.size() - 1; uidIndex >= 0; uidIndex--) {
                ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.valueAt(uidIndex);
                if (jobs != null) {
                    for (int i = jobs.size() - 1; i >= 0; i--) {
                        JobStatus jobStatus = (JobStatus) jobs.valueAt(i);
                        if (filterPredicate == null || filterPredicate.test(jobStatus)) {
                            functor.accept(jobStatus);
                        }
                    }
                }
            }
        }

        public void forEachJob(int callingUid, Consumer<JobStatus> functor) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobs.get(callingUid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.accept((JobStatus) jobs.valueAt(i));
                }
            }
        }

        public void forEachJobForSourceUid(int sourceUid, Consumer<JobStatus> functor) {
            ArraySet<JobStatus> jobs = (ArraySet) this.mJobsPerSourceUid.get(sourceUid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.accept((JobStatus) jobs.valueAt(i));
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

        /* JADX WARNING: Missing block: B:21:0x0060, code skipped:
            if (com.android.server.job.JobStore.access$400(r13.this$0).countAllJobsLoaded < 0) goto L_0x0062;
     */
        /* JADX WARNING: Missing block: B:22:0x0062, code skipped:
            com.android.server.job.JobStore.access$400(r13.this$0).countAllJobsLoaded = r0;
            com.android.server.job.JobStore.access$400(r13.this$0).countSystemServerJobsLoaded = r1;
            com.android.server.job.JobStore.access$400(r13.this$0).countSystemSyncManagerJobsLoaded = r3;
     */
        /* JADX WARNING: Missing block: B:33:0x0090, code skipped:
            if (com.android.server.job.JobStore.access$400(r13.this$0).countAllJobsLoaded >= 0) goto L_0x00ac;
     */
        /* JADX WARNING: Missing block: B:40:0x00a9, code skipped:
            if (com.android.server.job.JobStore.access$400(r13.this$0).countAllJobsLoaded >= 0) goto L_0x00ac;
     */
        /* JADX WARNING: Missing block: B:41:0x00ac, code skipped:
            r2 = com.android.server.job.JobStore.TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Read ");
            r4.append(r0);
            r4.append(" jobs");
            android.util.Slog.i(r2, r4.toString());
     */
        /* JADX WARNING: Missing block: B:42:0x00c7, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            int numJobs = 0;
            int numSystemJobs = 0;
            int i = 0;
            int numSyncJobs = 0;
            try {
                FileInputStream fis = JobStore.this.mJobsFile.openRead();
                synchronized (JobStore.this.mLock) {
                    List<JobStatus> jobs = readJobMapImpl(fis, this.rtcGood);
                    if (jobs != null) {
                        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
                        IActivityManager am = ActivityManager.getService();
                        while (i < jobs.size()) {
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
                            i++;
                        }
                    }
                }
                fis.close();
            } catch (FileNotFoundException e) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "Could not find jobs file, probably there was nothing to load.");
                }
            } catch (IOException | XmlPullParserException e2) {
                try {
                    Slog.wtf(JobStore.TAG, "Error jobstore xml.", e2);
                } catch (Throwable th) {
                    if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                        JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsLoaded = numSyncJobs;
                    }
                }
            }
        }

        private List<JobStatus> readJobMapImpl(FileInputStream fis, boolean rtcIsGood) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
                String str = JobStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Start tag: ");
                stringBuilder.append(parser.getName());
                Slog.d(str, stringBuilder.toString());
            }
            if (eventType == 1) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "No persisted jobs.");
                }
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
                        String tagName = parser.getName();
                        if ("job".equals(tagName)) {
                            JobStatus persistedJob = restoreJobFromXml(rtcIsGood, parser);
                            if (persistedJob != null) {
                                if (JobStore.DEBUG) {
                                    String str2 = JobStore.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Read out ");
                                    stringBuilder2.append(persistedJob);
                                    Slog.d(str2, stringBuilder2.toString());
                                }
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
            Pair<Long, Long> pair;
            long j;
            NumberFormatException numberFormatException;
            XmlPullParser xmlPullParser = parser;
            int internalFlags = 0;
            JobStatus jobStatus = null;
            try {
                Builder jobBuilder = buildBuilderFromXml(xmlPullParser);
                jobBuilder.setPersisted(true);
                int uid = Integer.parseInt(xmlPullParser.getAttributeValue(null, "uid"));
                String val = xmlPullParser.getAttributeValue(null, "priority");
                if (val != null) {
                    jobBuilder.setPriority(Integer.parseInt(val));
                }
                val = xmlPullParser.getAttributeValue(null, "flags");
                if (val != null) {
                    jobBuilder.setFlags(Integer.parseInt(val));
                }
                val = xmlPullParser.getAttributeValue(null, "internalFlags");
                if (val != null) {
                    internalFlags = Integer.parseInt(val);
                }
                int i;
                try {
                    int eventType;
                    val = xmlPullParser.getAttributeValue(null, "sourceUserId");
                    int sourceUserId = val == null ? -1 : Integer.parseInt(val);
                    val = xmlPullParser.getAttributeValue(null, "lastSuccessfulRunTime");
                    long lastSuccessfulRunTime = val == null ? 0 : Long.parseLong(val);
                    val = xmlPullParser.getAttributeValue(null, "lastFailedRunTime");
                    long lastFailedRunTime = val == null ? 0 : Long.parseLong(val);
                    val = xmlPullParser.getAttributeValue(null, "sourcePackageName");
                    String sourceTag = xmlPullParser.getAttributeValue(null, "sourceTag");
                    while (true) {
                        eventType = parser.next();
                        if (eventType != 4) {
                            break;
                        }
                        jobStatus = null;
                    }
                    Builder builder;
                    int i2;
                    if (eventType != 2) {
                        builder = jobBuilder;
                        internalFlags = uid;
                        i2 = sourceUserId;
                        uid = jobStatus;
                    } else if (JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(parser.getName())) {
                        try {
                            int i3;
                            buildConstraintsFromXml(jobBuilder, xmlPullParser);
                            parser.next();
                            while (true) {
                                eventType = parser.next();
                                if (eventType != 4) {
                                    break;
                                }
                                i = internalFlags;
                                internalFlags = uid;
                                i3 = eventType;
                                uid = jobStatus;
                                uid = internalFlags;
                                internalFlags = i;
                            }
                            if (eventType != 2) {
                                return jobStatus;
                            }
                            try {
                                long clampedLateRuntimeElapsed;
                                Pair<Long, Long> elapsedRuntimes;
                                Pair<Long, Long> rtcRuntimes = buildRtcExecutionTimesFromXml(xmlPullParser);
                                int sourceUserId2 = sourceUserId;
                                long elapsedNow = JobSchedulerService.sElapsedRealtimeClock.millis();
                                Pair<Long, Long> elapsedRuntimes2 = JobStore.convertRtcBoundsToElapsed(rtcRuntimes, elapsedNow);
                                Builder builder2;
                                if (JobStore.XML_TAG_PERIODIC.equals(parser.getName())) {
                                    try {
                                        long longValue;
                                        long periodMillis = Long.parseLong(xmlPullParser.getAttributeValue(jobStatus, "period"));
                                        String val2 = xmlPullParser.getAttributeValue(jobStatus, "flex");
                                        if (val2 != null) {
                                            try {
                                                longValue = Long.valueOf(val2).longValue();
                                            } catch (NumberFormatException e) {
                                                i = internalFlags;
                                                builder2 = jobBuilder;
                                                internalFlags = uid;
                                                pair = rtcRuntimes;
                                                i3 = eventType;
                                            }
                                        } else {
                                            longValue = periodMillis;
                                        }
                                        int uid2 = uid;
                                        long periodMillis2 = periodMillis;
                                        long flexMillis = longValue;
                                        try {
                                            jobBuilder.setPeriodic(periodMillis2, flexMillis);
                                            if (((Long) elapsedRuntimes2.second).longValue() > (elapsedNow + periodMillis2) + flexMillis) {
                                                String str;
                                                String str2;
                                                Object[] objArr;
                                                builder2 = jobBuilder;
                                                clampedLateRuntimeElapsed = (elapsedNow + flexMillis) + periodMillis2;
                                                periodMillis2 = clampedLateRuntimeElapsed - flexMillis;
                                                try {
                                                    str = JobStore.TAG;
                                                    str2 = "Periodic job for uid='%d' persisted run-time is too big [%s, %s]. Clamping to [%s,%s]";
                                                    objArr = new Object[5];
                                                    i = internalFlags;
                                                    internalFlags = uid2;
                                                    try {
                                                        objArr[0] = Integer.valueOf(internalFlags);
                                                        pair = rtcRuntimes;
                                                    } catch (NumberFormatException e2) {
                                                        pair = rtcRuntimes;
                                                        i3 = eventType;
                                                        Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                                        return null;
                                                    }
                                                } catch (NumberFormatException e3) {
                                                    i = internalFlags;
                                                    pair = rtcRuntimes;
                                                    i3 = eventType;
                                                    internalFlags = uid2;
                                                    Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                                    return null;
                                                }
                                                try {
                                                    try {
                                                        objArr[1] = DateUtils.formatElapsedTime(((Long) elapsedRuntimes2.first).longValue() / 1000);
                                                        objArr[2] = DateUtils.formatElapsedTime(((Long) elapsedRuntimes2.second).longValue() / 1000);
                                                        objArr[3] = DateUtils.formatElapsedTime(periodMillis2 / 1000);
                                                        objArr[4] = DateUtils.formatElapsedTime(clampedLateRuntimeElapsed / 1000);
                                                        Slog.w(str, String.format(str2, objArr));
                                                        elapsedRuntimes2 = Pair.create(Long.valueOf(periodMillis2), Long.valueOf(clampedLateRuntimeElapsed));
                                                    } catch (NumberFormatException e4) {
                                                        Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                                        return null;
                                                    }
                                                } catch (NumberFormatException e5) {
                                                    i3 = eventType;
                                                    Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                                    return null;
                                                }
                                            }
                                            i = internalFlags;
                                            builder2 = jobBuilder;
                                            pair = rtcRuntimes;
                                            i3 = eventType;
                                            internalFlags = uid2;
                                            elapsedRuntimes = elapsedRuntimes2;
                                            builder = builder2;
                                            clampedLateRuntimeElapsed = 0;
                                        } catch (NumberFormatException e6) {
                                            i = internalFlags;
                                            builder2 = jobBuilder;
                                            pair = rtcRuntimes;
                                            i3 = eventType;
                                            internalFlags = uid2;
                                            Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                            return null;
                                        }
                                    } catch (NumberFormatException e7) {
                                        i = internalFlags;
                                        builder2 = jobBuilder;
                                        internalFlags = uid;
                                        pair = rtcRuntimes;
                                        i3 = eventType;
                                        Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                        return null;
                                    }
                                }
                                i = internalFlags;
                                builder2 = jobBuilder;
                                internalFlags = uid;
                                pair = rtcRuntimes;
                                i3 = eventType;
                                if (JobStore.XML_TAG_ONEOFF.equals(parser.getName())) {
                                    try {
                                        clampedLateRuntimeElapsed = 0;
                                        if (((Long) elapsedRuntimes2.first).longValue() != 0) {
                                            try {
                                                builder = builder2;
                                                try {
                                                    builder.setMinimumLatency(((Long) elapsedRuntimes2.first).longValue() - elapsedNow);
                                                } catch (NumberFormatException e8) {
                                                    j = elapsedNow;
                                                    i2 = sourceUserId2;
                                                    sourceUserId2 = pair;
                                                }
                                            } catch (NumberFormatException e9) {
                                                builder = builder2;
                                                j = elapsedNow;
                                                i2 = sourceUserId2;
                                                sourceUserId2 = pair;
                                                Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                                return null;
                                            }
                                        }
                                        builder = builder2;
                                        try {
                                            if (((Long) elapsedRuntimes2.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                                builder.setOverrideDeadline(((Long) elapsedRuntimes2.second).longValue() - elapsedNow);
                                            }
                                            elapsedRuntimes = elapsedRuntimes2;
                                        } catch (NumberFormatException e10) {
                                            j = elapsedNow;
                                            i2 = sourceUserId2;
                                            sourceUserId2 = pair;
                                            Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                            return null;
                                        }
                                    } catch (NumberFormatException e11) {
                                        j = elapsedNow;
                                        i2 = sourceUserId2;
                                        builder = builder2;
                                        sourceUserId2 = pair;
                                        Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                        return null;
                                    }
                                }
                                i2 = sourceUserId2;
                                builder = builder2;
                                if (JobStore.DEBUG) {
                                    String str3 = JobStore.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Invalid parameter tag, skipping - ");
                                    stringBuilder.append(parser.getName());
                                    Slog.d(str3, stringBuilder.toString());
                                }
                                return null;
                                maybeBuildBackoffPolicyFromXml(builder, xmlPullParser);
                                parser.nextTag();
                                while (true) {
                                    uid = parser.next();
                                    if (uid != 4) {
                                        break;
                                    }
                                    i3 = uid;
                                }
                                if (uid != 2) {
                                    i2 = sourceUserId2;
                                    sourceUserId2 = pair;
                                } else if (JobStore.XML_TAG_EXTRAS.equals(parser.getName())) {
                                    PersistableBundle extras = PersistableBundle.restoreFromXml(parser);
                                    builder.setExtras(extras);
                                    parser.nextTag();
                                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(val) && extras != null && extras.getBoolean("SyncManagerJob", false)) {
                                        val = extras.getString("owningPackage", val);
                                        if (JobStore.DEBUG) {
                                            String str4 = JobStore.TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Fixing up sync job source package name from 'android' to '");
                                            stringBuilder2.append(val);
                                            stringBuilder2.append("'");
                                            Slog.i(str4, stringBuilder2.toString());
                                        }
                                    }
                                    String sourcePackageName = val;
                                    JobSchedulerInternal service = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
                                    int sourceUserId3 = sourceUserId2;
                                    int appBucket = JobSchedulerService.standbyBucketForPackage(sourcePackageName, sourceUserId3, elapsedNow);
                                    if (service != null) {
                                        clampedLateRuntimeElapsed = service.currentHeartbeat();
                                    }
                                    int sourceUserId4 = sourceUserId3;
                                    sourceUserId2 = pair;
                                    return new JobStatus(builder.build(), internalFlags, sourcePackageName, sourceUserId4, appBucket, clampedLateRuntimeElapsed, sourceTag, ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue(), lastSuccessfulRunTime, lastFailedRunTime, rtcIsGood ? null : pair, i);
                                } else {
                                    j = elapsedNow;
                                    i2 = sourceUserId2;
                                    sourceUserId2 = pair;
                                }
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Error reading extras, skipping.");
                                }
                                return null;
                            } catch (NumberFormatException e12) {
                                i = internalFlags;
                                builder = jobBuilder;
                                internalFlags = uid;
                                i3 = eventType;
                                i2 = sourceUserId;
                                numberFormatException = e12;
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Error parsing execution time parameters, skipping.");
                                }
                                return null;
                            }
                        } catch (NumberFormatException e122) {
                            i = internalFlags;
                            builder = jobBuilder;
                            internalFlags = uid;
                            i2 = sourceUserId;
                            JobStatus jobStatus2 = jobStatus;
                            numberFormatException = e122;
                            Slog.d(JobStore.TAG, "Error reading constraints, skipping.");
                            return jobStatus2;
                        }
                    } else {
                        i = internalFlags;
                        builder = jobBuilder;
                        internalFlags = uid;
                        i2 = sourceUserId;
                        uid = jobStatus;
                    }
                    return uid;
                } catch (NumberFormatException e13) {
                    i = internalFlags;
                    Slog.e(JobStore.TAG, "Error parsing job's required fields, skipping");
                    return null;
                }
            } catch (NumberFormatException e14) {
                Slog.e(JobStore.TAG, "Error parsing job's required fields, skipping");
                return null;
            }
        }

        private Builder buildBuilderFromXml(XmlPullParser parser) throws NumberFormatException {
            return new Builder(Integer.parseInt(parser.getAttributeValue(null, "jobid")), new ComponentName(parser.getAttributeValue(null, "package"), parser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS)));
        }

        private void buildConstraintsFromXml(Builder jobBuilder, XmlPullParser parser) {
            String netCapabilities = parser.getAttributeValue(null, "net-capabilities");
            String netUnwantedCapabilities = parser.getAttributeValue(null, "net-unwanted-capabilities");
            String netTransportTypes = parser.getAttributeValue(null, "net-transport-types");
            if (netCapabilities == null || netTransportTypes == null) {
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
            } else {
                long unwantedCapabilities;
                NetworkRequest request = new NetworkRequest.Builder().build();
                if (netUnwantedCapabilities != null) {
                    unwantedCapabilities = Long.parseLong(netUnwantedCapabilities);
                } else {
                    unwantedCapabilities = BitUtils.packBits(request.networkCapabilities.getUnwantedCapabilities());
                }
                request.networkCapabilities.setCapabilities(BitUtils.unpackBits(Long.parseLong(netCapabilities)), BitUtils.unpackBits(unwantedCapabilities));
                request.networkCapabilities.setTransportTypes(BitUtils.unpackBits(Long.parseLong(netTransportTypes)));
                jobBuilder.setRequiredNetwork(request);
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
            long earliestRunTimeElapsed;
            long earliestRunTimeElapsed2;
            XmlPullParser xmlPullParser = parser;
            long nowWallclock = JobSchedulerService.sSystemClock.millis();
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
            String val = xmlPullParser.getAttributeValue(null, "deadline");
            if (val != null) {
                earliestRunTimeElapsed = 0;
                latestRunTimeElapsed = nowElapsed + Math.max(Long.parseLong(val) - nowWallclock, 0);
            } else {
                earliestRunTimeElapsed = 0;
            }
            String val2 = xmlPullParser.getAttributeValue(null, "delay");
            if (val2 != null) {
                earliestRunTimeElapsed2 = nowElapsed + Math.max(Long.parseLong(val2) - nowWallclock, 0);
            } else {
                earliestRunTimeElapsed2 = earliestRunTimeElapsed;
            }
            return Pair.create(Long.valueOf(earliestRunTimeElapsed2), Long.valueOf(latestRunTimeElapsed));
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

    @VisibleForTesting
    public static JobStore initAndGetForTesting(Context context, File dataDir) {
        JobStore jobStoreUnderTest = new JobStore(context, new Object(), dataDir);
        jobStoreUnderTest.clear();
        return jobStoreUnderTest;
    }

    private JobStore(Context context, Object lock, File dataDir) {
        this.mLock = lock;
        this.mContext = context;
        boolean z = false;
        this.mDirtyOperations = 0;
        File jobDir = new File(new File(dataDir, "system"), "job");
        jobDir.mkdirs();
        this.mJobsFile = new AtomicFile(new File(jobDir, "jobs.xml"), "jobs");
        this.mJobSet = new JobSet();
        this.mXmlTimestamp = this.mJobsFile.getLastModifiedTime();
        if (JobSchedulerService.sSystemClock.millis() > this.mXmlTimestamp) {
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
        forEachJob(new -$$Lambda$JobStore$apkqpwp0Wau6LvC-MCNG2GidMkM(JobSchedulerService.sElapsedRealtimeClock.millis(), toAdd, toRemove));
    }

    static /* synthetic */ void lambda$getRtcCorrectedJobsLocked$0(long elapsedNow, ArrayList toAdd, ArrayList toRemove, JobStatus job) {
        Pair<Long, Long> utcTimes = job.getPersistedUtcTimes();
        if (utcTimes != null) {
            Pair<Long, Long> elapsedRuntimes = convertRtcBoundsToElapsed(utcTimes, elapsedNow);
            JobStatus jobStatus = r4;
            JobStatus jobStatus2 = new JobStatus(job, job.getBaseHeartbeat(), ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue(), 0, job.getLastSuccessfulRunTime(), job.getLastFailedRunTime());
            toAdd.add(jobStatus);
            toRemove.add(job);
            return;
        }
        long j = elapsedNow;
        ArrayList arrayList = toAdd;
        Pair<Long, Long> pair = utcTimes;
    }

    public boolean add(JobStatus jobStatus) {
        boolean replaced = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Added job status to store: ");
            stringBuilder.append(jobStatus);
            Slog.d(str, stringBuilder.toString());
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
        if (removed) {
            if (writeBack && jobStatus.isPersisted()) {
                maybeWriteStatusToDiskAsync();
            }
            return removed;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't remove job: didn't exist: ");
            stringBuilder.append(jobStatus);
            Slog.d(str, stringBuilder.toString());
        }
        return false;
    }

    public void removeJobsOfNonUsers(int[] whitelist) {
        this.mJobSet.removeJobsOfNonUsers(whitelist);
    }

    @VisibleForTesting
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

    public void forEachJob(Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob(null, (Consumer) functor);
    }

    public void forEachJob(Predicate<JobStatus> filterPredicate, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob((Predicate) filterPredicate, (Consumer) functor);
    }

    public void forEachJob(int uid, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob(uid, (Consumer) functor);
    }

    public void forEachJobForSourceUid(int sourceUid, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJobForSourceUid(sourceUid, functor);
    }

    private void maybeWriteStatusToDiskAsync() {
        this.mDirtyOperations++;
        if (this.mDirtyOperations >= 1) {
            if (DEBUG) {
                Slog.v(TAG, "Writing jobs to disk.");
            }
            this.mIoHandler.removeCallbacks(this.mWriteRunnable);
            this.mIoHandler.post(this.mWriteRunnable);
        }
    }

    @VisibleForTesting
    public void readJobMapFromDisk(JobSet jobSet, boolean rtcGood) {
        new ReadJobMapFromDiskRunnable(jobSet, rtcGood).run();
    }

    private static Pair<Long, Long> convertRtcBoundsToElapsed(Pair<Long, Long> rtcTimes, long nowElapsed) {
        long earliest;
        long nowWallclock = JobSchedulerService.sSystemClock.millis();
        if (((Long) rtcTimes.first).longValue() > 0) {
            earliest = Math.max(((Long) rtcTimes.first).longValue() - nowWallclock, 0) + nowElapsed;
        } else {
            earliest = 0;
        }
        long longValue = ((Long) rtcTimes.second).longValue();
        long j = JobStatus.NO_LATEST_RUNTIME;
        if (longValue < JobStatus.NO_LATEST_RUNTIME) {
            j = nowElapsed + Math.max(((Long) rtcTimes.second).longValue() - nowWallclock, 0);
        }
        return Pair.create(Long.valueOf(earliest), Long.valueOf(j));
    }

    private static boolean isSyncJob(JobStatus status) {
        return SyncJobService.class.getName().equals(status.getServiceComponent().getClassName());
    }
}
