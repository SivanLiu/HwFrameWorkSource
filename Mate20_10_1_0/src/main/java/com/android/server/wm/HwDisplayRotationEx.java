package com.android.server.wm;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.policy.HwGameDockGesture;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.wm.IntelliServiceManager;
import com.huawei.bd.Reporter;
import com.huawei.server.wm.IHwDisplayRotationEx;
import java.util.Calendar;

public class HwDisplayRotationEx implements IHwDisplayRotationEx {
    private static final int DELAY_TIMES = 500;
    private static final int DIRECTION_INIT = 100;
    private static final int MAX_REPORT_TIMES = 50;
    private static final int MSG_REPORT_LOG = 2;
    private static final int MSG_USE_SENSOR = 1;
    private static final int SWING_REPORT_EVENT_ID = 1001;
    private static final String TAG = "HwDisplayRotationEx";
    private int mDate = Calendar.getInstance().get(5);
    DisplayContent mDisplayContent;
    IntelliServiceManager.FaceRotationCallback mFaceRotationCallback = new IntelliServiceManager.FaceRotationCallback() {
        /* class com.android.server.wm.HwDisplayRotationEx.AnonymousClass2 */

        @Override // com.android.server.wm.IntelliServiceManager.FaceRotationCallback
        public void onEvent(int faceRotation) {
            HwDisplayRotationEx.this.mService.updateRotationUnchecked(false, false);
        }
    };
    private HwGameDockGesture mGameDockGesture;
    private GestureNavPolicy mGestureNavPolicy;
    private Handler mHandler = new Handler() {
        /* class com.android.server.wm.HwDisplayRotationEx.AnonymousClass1 */

        public void handleMessage(Message msg) {
            if (msg.what != 2) {
                super.handleMessage(msg);
            } else {
                HwDisplayRotationEx.this.handleReportLog(msg);
            }
        }
    };
    private boolean mIsDefaultDisplay;
    private int mReportCount = 0;
    private int mRotationType = 0;
    /* access modifiers changed from: private */
    public WindowManagerService mService;
    private int mSwingRotation = 100;

    public HwDisplayRotationEx(WindowManagerService service, DisplayContent displayContent, boolean isDefaultDisplay) {
        this.mIsDefaultDisplay = isDefaultDisplay;
        this.mGestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
        this.mGameDockGesture = (HwGameDockGesture) LocalServices.getService(HwGameDockGesture.class);
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    public void setRotation(int rotation) {
        GestureNavPolicy gestureNavPolicy;
        if (this.mIsDefaultDisplay && (gestureNavPolicy = this.mGestureNavPolicy) != null) {
            gestureNavPolicy.onRotationChanged(rotation);
        }
        if (this.mIsDefaultDisplay && this.mGameDockGesture != null && HwGameDockGesture.isGameDockGestureFeatureOn()) {
            this.mGameDockGesture.updateOnRotationChange(rotation);
        }
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            try {
                hwAft.notifyOrientationChange(rotation);
            } catch (RemoteException e) {
                Log.e(TAG, "setRotationLw throw RemoteException");
            }
        }
        Object hwPolicyRelatedObject = this.mService.getPolicy();
        if (hwPolicyRelatedObject != null && (hwPolicyRelatedObject instanceof HwPhoneWindowManager)) {
            ((HwPhoneWindowManager) hwPolicyRelatedObject).setRotationLw(rotation);
        }
    }

    public void setSwingRotation(int rotation) {
        Log.v(TAG, "setSwingRotation " + rotation);
        if (rotation < -2) {
            return;
        }
        if (rotation > 3 && rotation != 100) {
            return;
        }
        if (this.mSwingRotation == -2 && rotation == -1) {
            Slog.i(TAG, "old swing rotation is -2 current is -1 ignore it");
            return;
        }
        this.mSwingRotation = rotation;
        this.mRotationType = 2;
        this.mService.updateRotation(false, false);
        this.mRotationType = 0;
    }

    /* access modifiers changed from: package-private */
    public int swingRotationForSensorRotation(int lastRotation, int sensorRotation) {
        int desireRotation;
        int i = this.mSwingRotation;
        if (i != -2) {
            if (i == -1) {
                desireRotation = lastRotation;
            } else if (i != 100) {
                desireRotation = this.mSwingRotation;
            }
            Slog.i(TAG, "swingRotationForSensor desireRotation " + desireRotation + " swingRotation " + this.mSwingRotation + " sensorRotation " + sensorRotation + " lastRotation " + lastRotation);
            return desireRotation;
        }
        desireRotation = sensorRotation < 0 ? lastRotation : sensorRotation;
        Slog.i(TAG, "swingRotationForSensor desireRotation " + desireRotation + " swingRotation " + this.mSwingRotation + " sensorRotation " + sensorRotation + " lastRotation " + lastRotation);
        return desireRotation;
    }

    public int getSwingRotation(int lastRotation, int sensorRotation) {
        return swingRotationForSensorRotation(lastRotation, sensorRotation);
    }

    public int getRotationType() {
        return this.mRotationType;
    }

    public void setRotationType(int rotationType) {
        this.mRotationType = rotationType;
    }

    public void reportRotation(int rotationType, int oldRotation, int newRotation, String packageName) {
        if (shouldReportLog()) {
            Message msg = Message.obtain();
            msg.what = 2;
            Bundle data = msg.getData();
            data.putInt("rotationType", rotationType);
            data.putInt("oldRotation", oldRotation);
            data.putInt("newRotation", newRotation);
            data.putString("packageName", packageName);
            msg.setData(data);
            this.mHandler.sendMessage(msg);
        }
    }

    private boolean shouldReportLog() {
        if (this.mDisplayContent.getDisplayRotation().getUserRotationMode() == 0) {
            int now = Calendar.getInstance().get(5);
            if (this.mDate != now) {
                this.mReportCount = 0;
                this.mDate = now;
                return true;
            } else if (this.mReportCount < 50) {
                return true;
            } else {
                Log.v(TAG, "had report max times of log " + this.mReportCount);
            }
        } else {
            Log.v(TAG, "had disable sensor no need report");
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void handleReportLog(Message msg) {
        Bundle bundle = msg.getData();
        if (bundle != null) {
            int rotationType = bundle.getInt("rotationType", 0);
            int oldRotation = bundle.getInt("oldRotation", 0);
            int newRotation = bundle.getInt("newRotation", 0);
            String packageName = bundle.getString("packageName", "");
            StringBuilder sb = new StringBuilder();
            if (rotationType == 1) {
                sb.append("{Source:");
                sb.append("sensor");
            } else {
                sb.append("{Source:");
                sb.append("swingFace");
            }
            sb.append("}, {oldRotation:");
            sb.append(oldRotation);
            sb.append("}, {newRotation:");
            sb.append(newRotation);
            sb.append("}, {packageName:");
            sb.append(packageName);
            sb.append("}");
            String content = sb.toString();
            this.mReportCount++;
            Reporter.e(this.mService.mContext, 1001, content);
        }
    }

    public void setSensorRotation(int rotation) {
        IntelliServiceManager.setSensorRotation(rotation);
    }

    public boolean isIntelliServiceEnabled(int orientatin) {
        return IntelliServiceManager.isIntelliServiceEnabled(this.mService.mContext, orientatin, this.mService.mCurrentUserId);
    }

    public void startIntelliService() {
        IntelliServiceManager.getInstance(this.mService.mContext).startIntelliService(this.mFaceRotationCallback);
    }

    public void startIntelliService(final int orientation) {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.wm.HwDisplayRotationEx.AnonymousClass3 */

            public void run() {
                if (HwDisplayRotationEx.this.isIntelliServiceEnabled(orientation)) {
                    IntelliServiceManager.getInstance(HwDisplayRotationEx.this.mService.mContext).startIntelliService(HwDisplayRotationEx.this.mFaceRotationCallback);
                } else {
                    IntelliServiceManager.getInstance(HwDisplayRotationEx.this.mService.mContext).setKeepPortrait(false);
                }
            }
        });
    }

    public int getRotationFromSensorOrFace(int sensor) {
        if (IntelliServiceManager.getInstance(this.mService.mContext).isKeepPortrait()) {
            Slog.d(IntelliServiceManager.TAG, "portraitRotaion:" + 0);
            return 0;
        } else if (IntelliServiceManager.getInstance(this.mService.mContext).getFaceRotaion() != -2) {
            int sensorRotation = IntelliServiceManager.getInstance(this.mService.mContext).getFaceRotaion();
            Slog.d(IntelliServiceManager.TAG, "faceRotation:" + sensorRotation);
            return sensorRotation;
        } else {
            Slog.d(IntelliServiceManager.TAG, "sensor:" + sensor);
            return sensor;
        }
    }
}
