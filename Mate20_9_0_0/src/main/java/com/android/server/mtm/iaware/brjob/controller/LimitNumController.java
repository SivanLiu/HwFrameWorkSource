package com.android.server.mtm.iaware.brjob.controller;

import android.content.Context;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.server.mtm.iaware.brjob.StartupByBroadcastRecorder;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LimitNumController extends AwareStateController {
    private static final String CONDITION_DAY = "LimitNum#Day";
    private static final String CONDITION_HOUR = "LimitNum#Hour";
    private static final long DAY_PERIOD_INTERVAL = 86400000;
    private static final long HOUR_PERIOD_INTERVAL = 3600000;
    private static final String TAG = "LimitNumController";
    private static LimitNumController mSingleton;
    private static Object sCreationLock = new Object();
    @GuardedBy("mLock")
    private final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList();

    public static LimitNumController get(AwareJobSchedulerService jms) {
        LimitNumController limitNumController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new LimitNumController(jms, jms.getContext(), jms.getLock());
            }
            limitNumController = mSingleton;
        }
        return limitNumController;
    }

    private LimitNumController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null) {
            if (job.hasConstraint("LimitNum#Day") || job.hasConstraint("LimitNum#Hour")) {
                String filter;
                long interval;
                String str;
                String condition = "";
                if (job.hasConstraint("LimitNum#Day")) {
                    filter = job.getActionFilterValue("LimitNum#Day");
                    interval = 86400000;
                    condition = "LimitNum#Day";
                } else {
                    filter = job.getActionFilterValue("LimitNum#Hour");
                    interval = 3600000;
                    condition = "LimitNum#Hour";
                }
                if (this.DEBUG) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_brjob maybeStartTrackingJobLocked, ");
                    stringBuilder.append(condition);
                    AwareLog.i(str, stringBuilder.toString());
                }
                if (TextUtils.isEmpty(filter)) {
                    AwareLog.w(TAG, "iaware_brjob limitnum state value is null.");
                    job.setSatisfied(condition, false);
                    return;
                }
                str = job.getReceiverPkg();
                String action = job.getAction();
                if (TextUtils.isEmpty(action)) {
                    AwareLog.w(TAG, "iaware_brjob action is null.");
                    job.setSatisfied(condition, false);
                    return;
                }
                int launchCount = StartupByBroadcastRecorder.getInstance().getStartupCountsByBroadcast(str, action, interval);
                if (this.DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("iaware_brjob StartupCounts: ");
                    stringBuilder2.append(launchCount);
                    stringBuilder2.append("----");
                    stringBuilder2.append(filter);
                    AwareLog.i(str2, stringBuilder2.toString());
                }
                int filterValue = 0;
                try {
                    if (Integer.parseInt(filter) > launchCount) {
                        job.setSatisfied(condition, true);
                    } else {
                        job.setSatisfied(condition, false);
                    }
                    addJobLocked(this.mTrackedJobs, job);
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "iaware_brjob limitnum state value format is error, setShouldRunByError.");
                    job.setSatisfied(condition, false);
                }
            }
        }
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus job) {
        if (job != null) {
            if (job.hasConstraint("LimitNum#Day") || job.hasConstraint("LimitNum#Hour")) {
                if (this.DEBUG) {
                    AwareLog.i(TAG, "iaware_brjob stop tracking begin");
                }
                this.mTrackedJobs.remove(job);
            }
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    LimitNumController tracked job num: ");
            stringBuilder.append(this.mTrackedJobs.size());
            pw.println(stringBuilder.toString());
            StartupByBroadcastRecorder.getInstance().dump(pw);
        }
    }
}
