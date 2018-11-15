package com.android.server.gesture;

import android.content.Context;
import android.os.Looper;
import android.util.Flog;
import android.util.Log;
import android.view.MotionEvent;
import com.android.server.policy.SlideTouchEvent;

public class QuickSingleHandController extends QuickStartupStub {
    private SlideTouchEvent mSlideTouchEvent;

    public QuickSingleHandController(Context context, Looper looper) {
        super(context);
        this.mSlideTouchEvent = new SlideTouchEvent(context);
    }

    public boolean isPreConditionNotReady(boolean onLeft) {
        if (this.mDeviceStateController.isNavBarAtBottom() && !this.mDeviceStateController.isKeyguardLocked()) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSH;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("nav bar not in bottom or keygaurdLocked, bottom=");
            stringBuilder.append(this.mDeviceStateController.isNavBarAtBottom());
            Log.d(str, stringBuilder.toString());
        }
        Flog.bdReport(this.mContext, 855, GestureNavConst.REPORT_FAILURE);
        return true;
    }

    public void updateSettings() {
        super.updateSettings();
        this.mSlideTouchEvent.updateSettings();
    }

    public void setGestureResultAtUp(boolean success) {
        this.mSlideTouchEvent.setGestureResultAtUp(success);
        if (success) {
            Flog.bdReport(this.mContext, 855, GestureNavConst.REPORT_SUCCESS);
        } else {
            Flog.bdReport(this.mContext, 855, GestureNavConst.REPORT_FAILURE);
        }
    }

    public void handleTouchEvent(MotionEvent event) {
        this.mSlideTouchEvent.handleTouchEvent(event);
    }
}
