package com.android.server.mtm.iaware.brjob.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BarStatusController extends AwareStateController {
    private static final String CONDITION_NAME = "BarStatus";
    private static final String EXTRA_VISIBLE = "visible";
    private static final String INTENT_STATUSBAR_VISIBLE_CHANGE = "com.android.systemui.statusbar.visible.change";
    private static final String TAG = "BarStatusController";
    private static BarStatusController mSingleton;
    private static Object sCreationLock = new Object();
    private BroadcastReceiver mBarStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (BarStatusController.INTENT_STATUSBAR_VISIBLE_CHANGE.equals(intent.getAction())) {
                    BarStatusController.this.updateBarVisible(intent);
                    synchronized (BarStatusController.this.mLock) {
                        boolean changeToSatisfied = false;
                        List<AwareJobStatus> changedJobs = new ArrayList();
                        Iterator it = BarStatusController.this.mTrackedJobs.iterator();
                        while (it.hasNext()) {
                            AwareJobStatus job = (AwareJobStatus) it.next();
                            String value = job.getActionFilterValue("BarStatus");
                            if (BarStatusController.this.mIsVisible.equals(value) && !job.isSatisfied("BarStatus")) {
                                job.setSatisfied("BarStatus", true);
                                changeToSatisfied = true;
                                changedJobs.add(job);
                            } else if (!BarStatusController.this.mIsVisible.equals(value) && job.isSatisfied("BarStatus")) {
                                job.setSatisfied("BarStatus", false);
                            }
                        }
                        if (changeToSatisfied) {
                            if (BarStatusController.this.DEBUG) {
                                AwareLog.i(BarStatusController.TAG, "iaware_brjob onControllerStateChanged");
                            }
                            BarStatusController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                        }
                    }
                }
            }
        }
    };
    private String mIsVisible = AwareJobSchedulerConstants.BAR_STATUS_OFF;
    @GuardedBy("mLock")
    private final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList();

    public static BarStatusController get(AwareJobSchedulerService jms) {
        BarStatusController barStatusController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new BarStatusController(jms, jms.getContext(), jms.getLock());
            }
            barStatusController = mSingleton;
        }
        return barStatusController;
    }

    private BarStatusController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        if (this.mContext != null) {
            this.mContext.registerReceiverAsUser(this.mBarStatusReceiver, UserHandle.SYSTEM, new IntentFilter(INTENT_STATUSBAR_VISIBLE_CHANGE), null, null);
        }
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("BarStatus")) {
            String value = job.getActionFilterValue("BarStatus");
            String action = job.getAction();
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob start tracking, action:");
                stringBuilder.append(action);
                stringBuilder.append(", value: ");
                stringBuilder.append(value);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (INTENT_STATUSBAR_VISIBLE_CHANGE.equals(action)) {
                updateBarVisible(job.getIntent());
            }
            if (this.mIsVisible.equals(value)) {
                job.setSatisfied("BarStatus", true);
            } else {
                job.setSatisfied("BarStatus", false);
            }
            addJobLocked(this.mTrackedJobs, job);
        }
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("BarStatus")) {
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob stop tracking begin");
            }
            this.mTrackedJobs.remove(job);
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    BarStatusController tracked job num: ");
            stringBuilder.append(this.mTrackedJobs.size());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("        now bar status is: ");
            stringBuilder.append(this.mIsVisible);
            pw.println(stringBuilder.toString());
        }
    }

    private void updateBarVisible(Intent intent) {
        if (intent != null) {
            this.mIsVisible = Boolean.parseBoolean(intent.getStringExtra(EXTRA_VISIBLE)) ? AwareJobSchedulerConstants.BAR_STATUS_ON : AwareJobSchedulerConstants.BAR_STATUS_OFF;
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob bar status changed, now is: ");
                stringBuilder.append(this.mIsVisible);
                AwareLog.i(str, stringBuilder.toString());
            }
        }
    }
}
