package com.android.server.gesture;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Flog;
import android.util.Log;
import android.util.MathUtils;
import com.android.server.gesture.GestureNavView.IGestureNavBackAnim;
import java.util.ArrayList;

public class GestureNavBackStrategy extends GestureNavBaseStrategy {
    private static final int EXIT_LOCK_TASK_MODE_CHECK_TIME = 2000;
    private static final int MSG_CHECK_EXIT_LOCK_TASK = 1;
    private boolean mAnimPositionSetup;
    private boolean mAnimProcessedOnce;
    private boolean mAnimProcessing;
    private Handler mBackHandler;
    private int mBackMaxDistance1;
    private int mBackMaxDistance2;
    private IGestureNavBackAnim mGestureNavBackAnim;
    private boolean mInTaskLockMode;

    private final class BackHandler extends Handler {
        public BackHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.i(GestureNavConst.TAG_GESTURE_BACK, "Long press on back gesture, start exit lock task mode.");
                    GestureNavBackStrategy.this.exitLockTaskMode();
                    return;
                default:
                    return;
            }
        }
    }

    public GestureNavBackStrategy(int navId, Context context, Looper looper, IGestureNavBackAnim backAnim) {
        super(navId, context, looper);
        this.mGestureNavBackAnim = backAnim;
        this.mBackHandler = new BackHandler(looper);
    }

    public void updateConfig(int displayWidth, int displayHeight, Rect r) {
        super.updateConfig(displayWidth, displayHeight, r);
        this.mBackMaxDistance1 = GestureNavConst.getBackMaxDistance1(this.mContext);
        this.mBackMaxDistance2 = GestureNavConst.getBackMaxDistance2(this.mContext);
        if (GestureNavConst.DEBUG_ALL) {
            Log.d(GestureNavConst.TAG_GESTURE_BACK, "distance1:" + this.mBackMaxDistance1 + ", distance2:" + this.mBackMaxDistance2);
        }
    }

    protected void onGestureStarted(float rawX, float rawY) {
        boolean z = true;
        super.onGestureStarted(rawX, rawY);
        this.mAnimPositionSetup = false;
        this.mAnimProcessing = false;
        this.mAnimProcessedOnce = true;
        IGestureNavBackAnim iGestureNavBackAnim = this.mGestureNavBackAnim;
        if (this.mNavId != 1) {
            z = false;
        }
        iGestureNavBackAnim.setSide(z);
        this.mInTaskLockMode = GestureUtils.isInLockTaskMode();
    }

    protected void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        startLockTaskCheckState();
        if (!this.mAnimPositionSetup) {
            this.mAnimPositionSetup = true;
            this.mGestureNavBackAnim.setAnimPosition(this.mTouchDownRawY);
        }
    }

    protected void onGestureSlowProcessStarted(ArrayList<Float> pendingMoveDistance) {
        super.onGestureSlowProcessStarted(pendingMoveDistance);
        if (!this.mAnimProcessing) {
            this.mAnimProcessing = true;
        }
        if (pendingMoveDistance != null) {
            int size = pendingMoveDistance.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    notifyAnimProcess(((Float) pendingMoveDistance.get(i)).floatValue());
                }
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_BACK, "interpolate " + size + " pending datas");
                }
            }
        }
    }

    protected void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        super.onGestureSlowProcess(distance, offsetX, offsetY);
        notifyAnimProcess(distance);
    }

    protected void onGestureFailed(int reason, boolean failedInEventEnd) {
        super.onGestureFailed(reason, failedInEventEnd);
        resetLockTaskCheckState();
        if (this.mAnimPositionSetup) {
            Log.d(GestureNavConst.TAG_GESTURE_BACK, "gesture failed, disappear anim");
            this.mGestureNavBackAnim.playDisappearAnim();
        }
        Flog.bdReport(this.mContext, 854, GestureNavConst.reportResultStr(false, this.mNavId));
    }

    protected void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture);
        resetLockTaskCheckState();
        sendKeyEvent(4);
        if (this.mAnimProcessing && (this.mAnimProcessedOnce ^ 1) == 0) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_BACK, "gesture finished, disappear anim");
            }
            this.mGestureNavBackAnim.playDisappearAnim();
        } else if (isFastSlideGesture) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_BACK, "gesture finished, play fast anim, velocity=" + velocity);
            }
            if (!this.mAnimPositionSetup) {
                this.mAnimPositionSetup = true;
                this.mGestureNavBackAnim.setAnimPosition(this.mTouchDownRawY);
            }
            this.mGestureNavBackAnim.playFastSlidingAnim();
        } else {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_BACK, "velocity does not meet the threshold, disappear anim");
            }
            this.mGestureNavBackAnim.playDisappearAnim();
        }
        Flog.bdReport(this.mContext, 854, GestureNavConst.reportResultStr(true, this.mNavId));
    }

    private void notifyAnimProcess(float distance) {
        float process = getBubberbandProcess(distance);
        boolean success = this.mGestureNavBackAnim.setAnimProcess(process);
        if (!this.mAnimProcessedOnce && success) {
            this.mAnimProcessedOnce = true;
        }
        if (GestureNavConst.DEBUG_ALL) {
            Log.d(GestureNavConst.TAG_GESTURE_BACK, "process=" + process + ", distance=" + distance + ", animOnce=" + this.mAnimProcessedOnce);
        }
    }

    private float getBubberbandProcess(float distance) {
        if (distance < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (distance < ((float) this.mBackMaxDistance1)) {
            return (distance / ((float) this.mBackMaxDistance1)) * 0.88f;
        }
        return 0.88f + (MathUtils.constrain((distance - ((float) this.mBackMaxDistance1)) / ((float) (this.mBackMaxDistance2 - this.mBackMaxDistance1)), GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f) * 0.120000005f);
    }

    private void startLockTaskCheckState() {
        if (this.mInTaskLockMode) {
            this.mBackHandler.sendEmptyMessageDelayed(1, 2000);
        }
    }

    private void resetLockTaskCheckState() {
        if (this.mBackHandler.hasMessages(1)) {
            this.mBackHandler.removeMessages(1);
        }
    }

    private void exitLockTaskMode() {
        GestureUtils.exitLockTaskMode();
    }
}
