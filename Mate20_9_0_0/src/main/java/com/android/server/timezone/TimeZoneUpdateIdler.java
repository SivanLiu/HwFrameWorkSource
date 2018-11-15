package com.android.server.timezone;

import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;

public final class TimeZoneUpdateIdler extends JobService {
    private static final String TAG = "timezone.TimeZoneUpdateIdler";
    private static final int TIME_ZONE_UPDATE_IDLE_JOB_ID = 27042305;

    public boolean onStartJob(JobParameters params) {
        RulesManagerService rulesManagerService = (RulesManagerService) LocalServices.getService(RulesManagerService.class);
        Slog.d(TAG, "onStartJob() called");
        rulesManagerService.notifyIdle();
        return false;
    }

    public boolean onStopJob(JobParameters params) {
        boolean reschedule = params.getStopReason() != 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStopJob() called: Reschedule=");
        stringBuilder.append(reschedule);
        Slog.d(str, stringBuilder.toString());
        return reschedule;
    }

    public static void schedule(Context context, long minimumDelayMillis) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        Builder jobInfoBuilder = new Builder(TIME_ZONE_UPDATE_IDLE_JOB_ID, new ComponentName(context, TimeZoneUpdateIdler.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).setMinimumLatency(minimumDelayMillis);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("schedule() called: minimumDelayMillis=");
        stringBuilder.append(minimumDelayMillis);
        Slog.d(str, stringBuilder.toString());
        jobScheduler.schedule(jobInfoBuilder.build());
    }

    public static void unschedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        Slog.d(TAG, "unschedule() called");
        jobScheduler.cancel(TIME_ZONE_UPDATE_IDLE_JOB_ID);
    }
}
