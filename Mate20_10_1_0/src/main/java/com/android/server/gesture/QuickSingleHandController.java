package com.android.server.gesture;

import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Flog;
import android.util.Log;
import android.view.MotionEvent;
import com.android.server.LocalServices;
import com.android.server.policy.SlideTouchEvent;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;

public class QuickSingleHandController extends QuickStartupStub {
    private HwFoldScreenManagerInternal mFsmInternal;
    private boolean mIsFoldableScreen = false;
    private SlideTouchEvent mSlideTouchEvent;

    public QuickSingleHandController(Context context, Looper looper) {
        super(context);
        this.mSlideTouchEvent = new SlideTouchEvent(context);
        this.mIsFoldableScreen = !SystemProperties.get("ro.config.hw_fold_disp").isEmpty();
        if (this.mIsFoldableScreen) {
            this.mFsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        }
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public boolean isPreConditionNotReady(boolean isOnLeft) {
        if (!this.mDeviceStateController.isKeyguardLocked() && !checkSkipFoldableScreen()) {
            return false;
        }
        if (!GestureNavConst.DEBUG) {
            return true;
        }
        Log.i(GestureNavConst.TAG_GESTURE_QSH, "not ready as keygaurd locked or not fold main screen");
        return true;
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void updateSettings() {
        super.updateSettings();
        this.mSlideTouchEvent.updateSettings();
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void setGestureResultAtUp(boolean isSuccess, int failedReason) {
        this.mSlideTouchEvent.setGestureResultAtUp(isSuccess);
        Flog.bdReport(this.mContext, 855, GestureNavConst.reportResultStr(isSuccess, failedReason));
    }

    @Override // com.android.server.gesture.QuickStartupStub
    public void handleTouchEvent(MotionEvent event) {
        this.mSlideTouchEvent.handleTouchEvent(event);
    }

    public boolean isSingleHandEnableAndAvailable() {
        return this.mSlideTouchEvent.isSingleHandEnableAndAvailable();
    }

    public boolean isBeginFailedAsExceedDegree() {
        return this.mSlideTouchEvent.isBeginFailedAsExceedDegree();
    }

    public void interrupt() {
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSH, "single hand event is interrupted");
        }
    }

    private boolean checkSkipFoldableScreen() {
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal;
        if (!this.mIsFoldableScreen || (hwFoldScreenManagerInternal = this.mFsmInternal) == null) {
            return false;
        }
        int mode = hwFoldScreenManagerInternal.getDisplayMode();
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_QSH, "display mode is " + mode);
        }
        if (mode != 2) {
            return true;
        }
        return false;
    }
}
