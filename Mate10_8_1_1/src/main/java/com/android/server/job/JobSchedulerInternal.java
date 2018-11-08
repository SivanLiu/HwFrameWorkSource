package com.android.server.job;

import android.app.job.JobInfo;
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
            return "FirstLoad: " + this.countAllJobsLoaded + "/" + this.countSystemServerJobsLoaded + "/" + this.countSystemSyncManagerJobsLoaded + " LastSave: " + this.countAllJobsSaved + "/" + this.countSystemServerJobsSaved + "/" + this.countSystemSyncManagerJobsSaved;
        }
    }

    void addBackingUpUid(int i);

    void clearAllBackingUpUids();

    JobStorePersistStats getPersistStats();

    List<JobInfo> getSystemScheduledPendingJobs();

    boolean proxyService(int i, List<String> list);

    void removeBackingUpUid(int i);
}
