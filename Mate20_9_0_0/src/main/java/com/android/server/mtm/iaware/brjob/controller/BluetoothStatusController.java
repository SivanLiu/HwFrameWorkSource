package com.android.server.mtm.iaware.brjob.controller;

import android.bluetooth.BluetoothAdapter;
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

public class BluetoothStatusController extends AwareStateController {
    private static final String CONDITION_NAME = "BluetoothStatus";
    private static final String TAG = "BluetoothStatusController";
    private static BluetoothStatusController mSingleton;
    private static Object sCreationLock = new Object();
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", -1);
                if (BluetoothStatusController.this.DEBUG) {
                    String str = BluetoothStatusController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iaware_brjob bluetooth receiver, state:");
                    stringBuilder.append(state);
                    AwareLog.i(str, stringBuilder.toString());
                }
                if (state == 0) {
                    BluetoothStatusController.this.updateTrackedJobs(AwareJobSchedulerConstants.BLUETOOTH_STATUS_DISCONNECTED);
                } else if (state == 2) {
                    BluetoothStatusController.this.updateTrackedJobs(AwareJobSchedulerConstants.BLUETOOTH_STATUS_CONNECTED);
                }
            }
        }
    };
    private String mBluetoothState = "UNKNOWN";
    @GuardedBy("mLock")
    private final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList();

    public static BluetoothStatusController get(AwareJobSchedulerService jms) {
        BluetoothStatusController bluetoothStatusController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new BluetoothStatusController(jms, jms.getContext(), jms.getLock());
            }
            bluetoothStatusController = mSingleton;
        }
        return bluetoothStatusController;
    }

    private BluetoothStatusController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        if (this.mContext != null) {
            this.mContext.registerReceiverAsUser(this.mBluetoothReceiver, UserHandle.SYSTEM, new IntentFilter("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED"), null, null);
        }
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("BluetoothStatus")) {
            String filterValue = job.getActionFilterValue("BluetoothStatus");
            this.mBluetoothState = getBluetoothState();
            if (this.mBluetoothState.equals(filterValue)) {
                job.setSatisfied("BluetoothStatus", true);
            } else {
                job.setSatisfied("BluetoothStatus", false);
            }
            addJobLocked(this.mTrackedJobs, job);
        }
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("BluetoothStatus")) {
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob stop tracking begin");
            }
            this.mTrackedJobs.remove(job);
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    BluetoothStatusController tracked job num: ");
            stringBuilder.append(this.mTrackedJobs.size());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("        now bluetooth state is ");
            stringBuilder.append(getBluetoothState());
            pw.println(stringBuilder.toString());
        }
    }

    private String getBluetoothState() {
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothState = "UNKNOWN";
        if (blueAdapter == null) {
            return bluetoothState;
        }
        if (blueAdapter.isEnabled()) {
            int connectState = blueAdapter.getConnectionState();
            if (connectState == 2) {
                bluetoothState = AwareJobSchedulerConstants.BLUETOOTH_STATUS_CONNECTED;
            } else if (connectState == 0) {
                bluetoothState = AwareJobSchedulerConstants.BLUETOOTH_STATUS_DISCONNECTED;
            }
        } else {
            bluetoothState = AwareJobSchedulerConstants.BLUETOOTH_STATUS_DISCONNECTED;
        }
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob bluetooth state:");
            stringBuilder.append(bluetoothState);
            AwareLog.i(str, stringBuilder.toString());
        }
        return bluetoothState;
    }

    /* JADX WARNING: Missing block: B:28:0x0072, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateTrackedJobs(String btState) {
        synchronized (this.mLock) {
            boolean changeToSatisfied = false;
            if (this.mBluetoothState.equals(btState)) {
                return;
            }
            this.mBluetoothState = btState;
            List<AwareJobStatus> changedJobs = new ArrayList();
            Iterator it = this.mTrackedJobs.iterator();
            while (it.hasNext()) {
                AwareJobStatus job = (AwareJobStatus) it.next();
                String filterValue = job.getActionFilterValue("BluetoothStatus");
                if (this.mBluetoothState.equals(filterValue) && !job.isSatisfied("BluetoothStatus")) {
                    job.setSatisfied("BluetoothStatus", true);
                    changeToSatisfied = true;
                    changedJobs.add(job);
                } else if (!this.mBluetoothState.equals(filterValue) && job.isSatisfied("BluetoothStatus")) {
                    job.setSatisfied("BluetoothStatus", false);
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
}
