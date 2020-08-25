package com.android.server.mtm.iaware.brjob.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SIMStatusController extends AwareStateController {
    private static final String CONDITION_NAME = "SIMStatus";
    private static final String TAG = "SIMStatusController";
    private static SIMStatusController mSingleton;
    private static final Object sCreationLock = new Object();
    private BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        /* class com.android.server.mtm.iaware.brjob.controller.SIMStatusController.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (SIMStatusController.this.isViceCardSimStateBroadcast(intent)) {
                    if (SIMStatusController.this.DEBUG) {
                        AwareLog.i(SIMStatusController.TAG, "iaware_brjob br is vice card sim, not change state");
                    }
                } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED.equals(intent.getAction())) {
                    String state = SIMStatusController.this.getSimStateFromIntent(intent);
                    if (SIMStatusController.this.DEBUG) {
                        AwareLog.i(SIMStatusController.TAG, "iaware_brjob onReceiver sim state:" + state);
                    }
                    if (!SIMStatusController.this.mSimStatus.equals(state)) {
                        synchronized (SIMStatusController.this.mLock) {
                            boolean changeToSatisfied = false;
                            String unused = SIMStatusController.this.mSimStatus = state;
                            List<AwareJobStatus> changedJobs = new ArrayList<>();
                            Iterator it = SIMStatusController.this.mTrackedJobs.iterator();
                            while (it.hasNext()) {
                                AwareJobStatus job = (AwareJobStatus) it.next();
                                String value = job.getActionFilterValue("SIMStatus");
                                if (SIMStatusController.this.matchSimStatus(value) && !job.isSatisfied("SIMStatus")) {
                                    job.setSatisfied("SIMStatus", true);
                                    changeToSatisfied = true;
                                    changedJobs.add(job);
                                } else if (!SIMStatusController.this.matchSimStatus(value) && job.isSatisfied("SIMStatus")) {
                                    job.setSatisfied("SIMStatus", false);
                                }
                            }
                            if (changeToSatisfied) {
                                if (SIMStatusController.this.DEBUG) {
                                    AwareLog.i(SIMStatusController.TAG, "iaware_brjob onControllerStateChanged");
                                }
                                SIMStatusController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                            }
                        }
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public String mSimStatus = "UNKNOWN";
    private TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList<>();

    public static SIMStatusController get(AwareJobSchedulerService jms) {
        SIMStatusController sIMStatusController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new SIMStatusController(jms, jms.getContext(), jms.getLock());
            }
            sIMStatusController = mSingleton;
        }
        return sIMStatusController;
    }

    private SIMStatusController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        if (this.mContext != null) {
            this.mContext.registerReceiverAsUser(this.mSimStateReceiver, UserHandle.SYSTEM, new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED), null, null);
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
    }

    @Override // com.android.server.mtm.iaware.brjob.controller.AwareStateController
    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("SIMStatus")) {
            this.mSimStatus = getSimState();
            String filter = job.getActionFilterValue("SIMStatus");
            if (TextUtils.isEmpty(filter)) {
                AwareLog.w(TAG, "iaware_brjob sim status value is null");
                job.setSatisfied("SIMStatus", false);
                return;
            }
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob start tracking SimState, filter:" + filter + "current: " + this.mSimStatus);
            }
            if (job.getIntent() == null || !isViceCardSimStateBroadcast(job.getIntent())) {
                job.setSatisfied("SIMStatus", matchSimStatus(filter));
                addJobLocked(this.mTrackedJobs, job);
                return;
            }
            job.setSatisfied("SIMStatus", true);
        }
    }

    /* access modifiers changed from: private */
    public boolean matchSimStatus(String filterValue) {
        String[] values;
        for (String str : filterValue.split("[:]")) {
            if (this.mSimStatus.equals(str)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.mtm.iaware.brjob.controller.AwareStateController
    public void maybeStopTrackingJobLocked(AwareJobStatus jobStatus) {
        if (jobStatus != null && jobStatus.hasConstraint("SIMStatus")) {
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob stop tracking begin");
            }
            this.mTrackedJobs.remove(jobStatus);
        }
    }

    @Override // com.android.server.mtm.iaware.brjob.controller.AwareStateController
    public void dump(PrintWriter pw) {
        if (pw != null) {
            pw.println("    SIMStatusController tracked job num: " + this.mTrackedJobs.size());
            pw.println("        now sim state is " + this.mSimStatus);
        }
    }

    private String getSimState() {
        if (this.mTelephonyManager == null) {
            if (this.mContext == null) {
                return "UNKNOWN";
            }
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        int state = this.mTelephonyManager.getSimState();
        if (state == 1) {
            return AwareJobSchedulerConstants.SIM_STATUS_ABSENT;
        }
        if (state == 2 || state == 3 || state == 4) {
            return AwareJobSchedulerConstants.SIM_STATUS_LOCKED;
        }
        if (state != 5) {
            return "UNKNOWN";
        }
        return AwareJobSchedulerConstants.SIM_STATUS_READY;
    }

    /* access modifiers changed from: private */
    public String getSimStateFromIntent(Intent intent) {
        String curSimState = intent.getStringExtra("ss");
        if (curSimState == null) {
            return "UNKNOWN";
        }
        if (this.DEBUG) {
            AwareLog.i(TAG, "iaware_brjob curSimState : " + curSimState);
        }
        if (AwareJobSchedulerConstants.SIM_STATUS_READY.equals(curSimState)) {
            return AwareJobSchedulerConstants.SIM_STATUS_READY;
        }
        if (AwareJobSchedulerConstants.SIM_STATUS_LOCKED.equals(curSimState) || "INTERNAL_LOCKED".equals(curSimState)) {
            return AwareJobSchedulerConstants.SIM_STATUS_LOCKED;
        }
        if (AwareJobSchedulerConstants.SIM_STATUS_ABSENT.equals(curSimState)) {
            return AwareJobSchedulerConstants.SIM_STATUS_ABSENT;
        }
        return "UNKNOWN";
    }

    /* access modifiers changed from: private */
    public boolean isViceCardSimStateBroadcast(Intent intent) {
        if (!SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED.equals(intent.getAction())) {
            return false;
        }
        int subKey = intent.getIntExtra("subscription", -1);
        int defaultSubId = SubscriptionManager.getDefaultSubId();
        if (this.DEBUG) {
            AwareLog.i(TAG, "iaware_brjob is Vice Card sim state Broadcast, subkey : " + subKey + ", defaultSubId : " + defaultSubId);
        }
        if (subKey == -1 || subKey != defaultSubId) {
            return true;
        }
        return false;
    }
}
