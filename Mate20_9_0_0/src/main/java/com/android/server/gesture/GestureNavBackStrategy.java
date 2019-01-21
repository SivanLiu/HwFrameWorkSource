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
    private static final int MSG_CHECK_HAPTICS_VIBRATOR = 1;
    private boolean mAnimPositionSetup;
    private boolean mAnimProcessedOnce;
    private boolean mAnimProcessing;
    private Handler mBackHandler;
    private int mBackMaxDistance1;
    private int mBackMaxDistance2;
    private IGestureNavBackAnim mGestureNavBackAnim;

    private final class BackHandler extends Handler {
        public BackHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                GestureUtils.performHapticFeedbackIfNeed(GestureNavBackStrategy.this.mContext);
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
            String str = GestureNavConst.TAG_GESTURE_BACK;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("distance1:");
            stringBuilder.append(this.mBackMaxDistance1);
            stringBuilder.append(", distance2:");
            stringBuilder.append(this.mBackMaxDistance2);
            Log.d(str, stringBuilder.toString());
        }
    }

    protected void onGestureStarted(float rawX, float rawY) {
        super.onGestureStarted(rawX, rawY);
        boolean z = false;
        this.mAnimPositionSetup = false;
        this.mAnimProcessing = false;
        this.mAnimProcessedOnce = true;
        IGestureNavBackAnim iGestureNavBackAnim = this.mGestureNavBackAnim;
        if (this.mNavId == 1) {
            z = true;
        }
        iGestureNavBackAnim.setSide(z);
    }

    protected void onGestureReallyStarted() {
        super.onGestureReallyStarted();
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
            int size2 = size;
            if (size > 0) {
                for (size = 0; size < size2; size++) {
                    notifyAnimProcess(((Float) pendingMoveDistance.get(size)).floatValue());
                }
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavConst.TAG_GESTURE_BACK;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("interpolate ");
                    stringBuilder.append(size2);
                    stringBuilder.append(" pending datas");
                    Log.d(str, stringBuilder.toString());
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
        if (this.mAnimPositionSetup) {
            Log.d(GestureNavConst.TAG_GESTURE_BACK, "gesture failed, disappear anim");
            this.mGestureNavBackAnim.playDisappearAnim();
        }
        Flog.bdReport(this.mContext, 854, GestureNavConst.reportResultStr(false, this.mNavId));
    }

    protected void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture);
        checkHwHapticsVibrator();
        sendKeyEvent(4);
        if (this.mAnimProcessing && this.mAnimProcessedOnce) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_BACK, "gesture finished, disappear anim");
            }
            this.mGestureNavBackAnim.playDisappearAnim();
        } else if (isFastSlideGesture) {
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_BACK;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("gesture finished, play fast anim, velocity=");
                stringBuilder.append(velocity);
                Log.d(str, stringBuilder.toString());
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
            String str = GestureNavConst.TAG_GESTURE_BACK;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process=");
            stringBuilder.append(process);
            stringBuilder.append(", distance=");
            stringBuilder.append(distance);
            stringBuilder.append(", animOnce=");
            stringBuilder.append(this.mAnimProcessedOnce);
            Log.d(str, stringBuilder.toString());
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

    private void checkHwHapticsVibrator() {
        if (!this.mBackHandler.hasMessages(1)) {
            this.mBackHandler.sendEmptyMessage(1);
        }
    }
}
