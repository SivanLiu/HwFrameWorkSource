package com.android.server.job;

import android.app.job.JobInfo;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.util.List;

public interface JobSchedulerInternal {

    public static class JobStorePersistStats {
        public int countAllJobsLoaded = -1;
        public int countAllJobsSaved = -1;
        public int countSystemServerJobsLoaded = -1;
        public int countSystemServerJobsSaved = -1;
        public int countSystemSyncManagerJobsLoaded = -1;
        public int countSystemSyncManagerJobsSaved = -1;

        public JobStorePersistStats(JobStorePersistStats source) {
            this.countAllJobsLoaded = source.countAllJobsLoaded;
            this.countSystemServerJobsLoaded = source.countSystemServerJobsLoaded;
            this.countSystemSyncManagerJobsLoaded = source.countSystemSyncManagerJobsLoaded;
            this.countAllJobsSaved = source.countAllJobsSaved;
            this.countSystemServerJobsSaved = source.countSystemServerJobsSaved;
            this.countSystemSyncManagerJobsSaved = source.countSystemSyncManagerJobsSaved;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FirstLoad: ");
            stringBuilder.append(this.countAllJobsLoaded);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.countSystemServerJobsLoaded);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.countSystemSyncManagerJobsLoaded);
            stringBuilder.append(" LastSave: ");
            stringBuilder.append(this.countAllJobsSaved);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.countSystemServerJobsSaved);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(this.countSystemSyncManagerJobsSaved);
            return stringBuilder.toString();
        }
    }

    void addBackingUpUid(int i);

    long baseHeartbeatForApp(String str, int i, int i2);

    void cancelJobsForUid(int i, String str);

    void clearAllBackingUpUids();

    long currentHeartbeat();

    JobStorePersistStats getPersistStats();

    List<JobInfo> getSystemScheduledPendingJobs();

    long nextHeartbeatForBucket(int i);

    void noteJobStart(String str, int i);

    boolean proxyService(int i, List<String> list);

    void removeBackingUpUid(int i);

    void reportAppUsage(String str, int i);
}
