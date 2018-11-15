package com.android.server.mtm.iaware.brjob.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.annotations.GuardedBy;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServicesStatusController extends AwareStateController {
    private static final String CONDITION_NAME = "ServicesStatus";
    private static final String TAG = "ServicesStatusController";
    private static ServicesStatusController mSingleton;
    private static Object sCreationLock = new Object();
    private boolean mIsRoaming = false;
    private BroadcastReceiver mServcicesStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (ServicesStatusController.this.isViceCardServicesStatusBroadcast(intent)) {
                    if (ServicesStatusController.this.DEBUG) {
                        AwareLog.i(ServicesStatusController.TAG, "iaware_brjob br is vice card service, not change state");
                    }
                    return;
                }
                if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
                    boolean changed = false;
                    ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                    if (!(serviceState.getRoaming() == ServicesStatusController.this.mIsRoaming && serviceState.getState() == ServicesStatusController.this.mServicesState)) {
                        changed = true;
                    }
                    ServicesStatusController.this.mIsRoaming = serviceState.getRoaming();
                    ServicesStatusController.this.mServicesState = serviceState.getState();
                    if (changed) {
                        if (ServicesStatusController.this.DEBUG) {
                            String str = ServicesStatusController.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("iaware_brjob service changed, state: ");
                            stringBuilder.append(ServicesStatusController.this.mServicesState);
                            stringBuilder.append(", isRoaming: ");
                            stringBuilder.append(ServicesStatusController.this.mIsRoaming);
                            AwareLog.i(str, stringBuilder.toString());
                        }
                        ServicesStatusController.this.updateJobs();
                    }
                }
            }
        }
    };
    private int mServicesState = 1;
    private TelephonyManager mTelephonyManager;
    @GuardedBy("mLock")
    private final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList();

    public static ServicesStatusController get(AwareJobSchedulerService jms) {
        ServicesStatusController servicesStatusController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new ServicesStatusController(jms, jms.getContext(), jms.getLock());
            }
            servicesStatusController = mSingleton;
        }
        return servicesStatusController;
    }

    private ServicesStatusController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        if (this.mContext != null) {
            this.mContext.registerReceiverAsUser(this.mServcicesStatusReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.SERVICE_STATE"), null, null);
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("ServicesStatus")) {
            String value = job.getActionFilterValue("ServicesStatus");
            String action = job.getAction();
            Intent intent = job.getIntent();
            String str;
            StringBuilder stringBuilder;
            if (action == null || intent == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob action: ");
                stringBuilder.append(action == null ? "null" : action);
                stringBuilder.append(", intent: ");
                stringBuilder.append(intent == null ? "null" : intent);
                AwareLog.w(str, stringBuilder.toString());
                job.setSatisfied("ServicesStatus", false);
            } else if (isViceCardServicesStatusBroadcast(intent)) {
                job.setSatisfied("ServicesStatus", true);
            } else {
                resetServicesState();
                if (this.DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_brjob start tracking, action:");
                    stringBuilder.append(action);
                    stringBuilder.append(", value: ");
                    stringBuilder.append(value);
                    stringBuilder.append(", is roaming:");
                    stringBuilder.append(this.mIsRoaming);
                    stringBuilder.append(", service state: ");
                    stringBuilder.append(this.mServicesState);
                    AwareLog.i(str, stringBuilder.toString());
                }
                checkSatisfiedLocked(job, value);
                addJobLocked(this.mTrackedJobs, job);
            }
        }
    }

    private boolean checkSatisfiedLocked(AwareJobStatus job, String filterValue) {
        boolean changeToSatisfied = false;
        String[] values = filterValue.split("[:]");
        boolean match = false;
        for (int i = 0; i < values.length; i++) {
            if (AwareJobSchedulerConstants.SERVICES_STATUS_ROAMING.equals(values[i])) {
                match = this.mIsRoaming;
            } else {
                boolean z = true;
                if (AwareJobSchedulerConstants.SERVICES_STATUS_CONNECTED.equals(values[i])) {
                    if (this.mServicesState != 0) {
                        z = false;
                    }
                    match = z;
                } else if (AwareJobSchedulerConstants.SERVICES_STATUS_DISCONNECTED.equals(values[i])) {
                    if (this.mServicesState == 0) {
                        z = false;
                    }
                    match = z;
                }
            }
            if (match) {
                break;
            }
        }
        if (match && !job.isSatisfied("ServicesStatus")) {
            changeToSatisfied = true;
        }
        job.setSatisfied("ServicesStatus", match);
        return changeToSatisfied;
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("ServicesStatus")) {
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob stop tracking begin");
            }
            this.mTrackedJobs.remove(job);
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    ServicesStatusController tracked job num: ");
            stringBuilder.append(this.mTrackedJobs.size());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("        now services state: ");
            stringBuilder.append(this.mServicesState);
            stringBuilder.append(", isRoaming: ");
            stringBuilder.append(this.mIsRoaming);
            pw.println(stringBuilder.toString());
        }
    }

    private void updateJobs() {
        synchronized (this.mLock) {
            boolean changeToSatisfied = false;
            List<AwareJobStatus> changedJobs = new ArrayList();
            Iterator it = this.mTrackedJobs.iterator();
            while (it.hasNext()) {
                AwareJobStatus job = (AwareJobStatus) it.next();
                changeToSatisfied = checkSatisfiedLocked(job, job.getActionFilterValue("ServicesStatus"));
                if (changeToSatisfied) {
                    changedJobs.add(job);
                }
            }
            if (changeToSatisfied) {
                if (this.DEBUG) {
                    AwareLog.i(TAG, "iaware_brjob onControllerStateChanged");
                }
                this.mStateChangedListener.onControllerStateChanged(changedJobs);
            }
        }
    }

    private void resetServicesState() {
        ServiceState state = this.mTelephonyManager != null ? this.mTelephonyManager.getServiceState() : null;
        if (state != null) {
            this.mIsRoaming = state.getRoaming();
            this.mServicesState = state.getState();
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob resetServicesState mServicesState: ");
                stringBuilder.append(this.mServicesState);
                stringBuilder.append(", mIsRoaming : ");
                stringBuilder.append(this.mIsRoaming);
                AwareLog.i(str, stringBuilder.toString());
            }
        }
    }

    private boolean isViceCardServicesStatusBroadcast(Intent intent) {
        if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
            int subKey = intent.getIntExtra("subscription", -1);
            int defaultSubId = SubscriptionManager.getDefaultSubId();
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob is Vice Card ServicesStatus Broadcast, subkey : ");
                stringBuilder.append(subKey);
                stringBuilder.append(", defaultSubId : ");
                stringBuilder.append(defaultSubId);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (subKey == -1 || subKey != defaultSubId) {
                return true;
            }
        }
        return false;
    }
}
