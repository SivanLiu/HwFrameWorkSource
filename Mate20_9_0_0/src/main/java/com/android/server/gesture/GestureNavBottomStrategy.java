package com.android.server.gesture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManagerInternal;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.wm.HwGestureNavWhiteConfig;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GestureNavBottomStrategy extends GestureNavBaseStrategy {
    private static final int MSG_HIDE_INPUTMETHOD_IF_NEED = 1;
    private static final int STARTUP_TARGET_HIVISION = 4;
    private static final int STARTUP_TARGET_NONE = 0;
    private static final int STARTUP_TARGET_QUICK_STEP = 3;
    private static final int STARTUP_TARGET_SINGLE_HAND = 1;
    private static final int STARTUP_TARGET_VOICE_ASSIST = 2;
    private Handler mBottomHandler;
    private boolean mFirstAftTriggered;
    private final Runnable mGoHomeRunnable = new Runnable() {
        public void run() {
            GestureNavBottomStrategy.this.sendKeyEvent(3);
        }
    };
    private boolean mInTaskLockMode;
    private InputMethodManagerInternal mInputMethodManagerInternal;
    private long mLastAftGestureTime;
    private boolean mPreConditionNotReady;
    private QuickSingleHandController mQuickSingleHandController;
    private QuickSlideOutController mQuickSlideOutController;
    private QuickStepController mQuickStepController;
    private boolean mShouldCheckAftForThisGesture;
    private int mStartupTarget = 0;

    private final class BottomHandler extends Handler {
        public BottomHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                GestureNavBottomStrategy.this.hideCurrentInputMethod();
            }
        }
    }

    public GestureNavBottomStrategy(int navId, Context context, Looper looper) {
        super(navId, context, looper);
        this.mBottomHandler = new BottomHandler(looper);
    }

    private void notifyStart() {
        this.mQuickStepController = new QuickStepController(this.mContext, this.mLooper);
        this.mQuickSingleHandController = new QuickSingleHandController(this.mContext, this.mLooper);
        this.mQuickSlideOutController = new QuickSlideOutController(this.mContext, this.mLooper);
    }

    private void notifyStop() {
        this.mQuickStepController = null;
        this.mQuickSingleHandController = null;
        this.mQuickSlideOutController = null;
    }

    private boolean isSidleOutEnabled() {
        return this.mQuickSlideOutController != null && this.mQuickSlideOutController.isSlideOutEnabled();
    }

    private boolean isSupportMultiTouch() {
        return this.mQuickStepController != null && this.mQuickStepController.isSupportMultiTouch();
    }

    private int checkStartupTarget(int rawX, int rawY) {
        if (this.mKeyguardShowing) {
            return 3;
        }
        int width = getRegion().width();
        int singleHandWidth = (int) (GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO * ((float) getRegion().height()));
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_BOTTOM;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkStartupTarget width=");
            stringBuilder.append(width);
            stringBuilder.append(", rawX=");
            stringBuilder.append(rawX);
            stringBuilder.append(", rawY=");
            stringBuilder.append(rawY);
            stringBuilder.append(", singleHandWidth=");
            stringBuilder.append(singleHandWidth);
            Log.d(str, stringBuilder.toString());
        }
        if (rawX <= singleHandWidth || rawX >= width - singleHandWidth) {
            return 1;
        }
        if (!isSidleOutEnabled() || this.mInTaskLockMode) {
            return 3;
        }
        if (((float) rawX) < ((float) width) * 0.19999999f) {
            return 2;
        }
        if (((float) rawX) >= ((float) width) * 0.19999999f && ((float) rawX) <= ((float) width) - (((float) width) * 0.19999999f)) {
            return 3;
        }
        if (((float) rawX) > ((float) width) - (((float) width) * 0.19999999f)) {
            return 4;
        }
        return 0;
    }

    public void updateDeviceState(boolean keyguardShowing) {
        super.updateDeviceState(keyguardShowing);
        if (this.mQuickStepController != null) {
            this.mQuickStepController.updateDeviceState(keyguardShowing);
        }
    }

    public void onNavCreate(GestureNavView navView) {
        notifyStart();
        this.mQuickStepController.onNavCreate(navView);
        this.mQuickSlideOutController.onNavCreate(navView);
        this.mQuickSingleHandController.onNavCreate(navView);
    }

    public void onNavUpdate() {
        this.mQuickStepController.onNavUpdate();
        this.mQuickSlideOutController.onNavUpdate();
        this.mQuickSingleHandController.onNavUpdate();
    }

    public void onNavDestroy() {
        this.mQuickStepController.onNavDestroy();
        this.mQuickSlideOutController.onNavDestroy();
        this.mQuickSingleHandController.onNavDestroy();
        notifyStop();
    }

    protected boolean shouldDropMultiTouch() {
        if (isSupportMultiTouch()) {
            return false;
        }
        return true;
    }

    protected int moveOutAngleThreshold() {
        if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
            return 45;
        }
        return super.moveOutAngleThreshold();
    }

    protected void onGestureStarted(float rawX, float rawY) {
        super.onGestureStarted(rawX, rawY);
        this.mInTaskLockMode = GestureUtils.isInLockTaskMode();
        this.mPreConditionNotReady = false;
        this.mStartupTarget = checkStartupTarget((int) rawX, (int) rawY);
        boolean z = true;
        if (this.mStartupTarget == 1) {
            this.mShouldCheckAftForThisGesture = false;
        } else {
            if (!(this.mInTaskLockMode || shouldCheckAftForThisGesture())) {
                z = false;
            }
            this.mShouldCheckAftForThisGesture = z;
        }
        long diffTime = 0;
        if (!this.mShouldCheckAftForThisGesture) {
            this.mFirstAftTriggered = false;
        } else if (this.mFirstAftTriggered) {
            long uptimeMillis = SystemClock.uptimeMillis() - this.mLastAftGestureTime;
            diffTime = uptimeMillis;
            if (uptimeMillis > 2000) {
                this.mFirstAftTriggered = false;
            }
        }
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_BOTTOM;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("StartupTarget=");
            stringBuilder.append(this.mStartupTarget);
            stringBuilder.append(", checkAft=");
            stringBuilder.append(this.mShouldCheckAftForThisGesture);
            stringBuilder.append(", firstAftTriggered=");
            stringBuilder.append(this.mFirstAftTriggered);
            stringBuilder.append(", diffTime=");
            stringBuilder.append(diffTime);
            Log.d(str, stringBuilder.toString());
        }
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mInTaskLockMode) {
            if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
                this.mQuickSlideOutController.onGestureStarted();
            } else if (this.mStartupTarget == 3) {
                this.mQuickStepController.onGestureStarted();
            }
        }
    }

    protected void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mInTaskLockMode) {
            this.mBottomHandler.sendEmptyMessage(1);
            if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
                this.mQuickSlideOutController.onGestureReallyStarted();
            } else if (this.mStartupTarget == 3) {
                this.mQuickStepController.onGestureReallyStarted();
            }
            return;
        }
        showReTryToast();
    }

    protected void onGestureSlowProcessStarted(ArrayList<Float> pendingMoveDistance) {
        super.onGestureSlowProcessStarted(pendingMoveDistance);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mInTaskLockMode) {
            if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
                this.mQuickSlideOutController.onGestureSlowProcessStarted();
            } else if (this.mStartupTarget == 3) {
                this.mQuickStepController.onGestureSlowProcessStarted();
            }
        }
    }

    protected void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        super.onGestureSlowProcess(distance, offsetX, offsetY);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || !this.mInTaskLockMode) {
        }
    }

    protected void onGestureFailed(int reason, boolean failedInEventEnd) {
        super.onGestureFailed(reason, failedInEventEnd);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mInTaskLockMode) {
            if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
                this.mQuickSlideOutController.onGestureFailed();
            } else if (this.mStartupTarget == 3) {
                this.mQuickStepController.onGestureFailed();
            }
        }
    }

    protected void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mInTaskLockMode) {
            if (this.mStartupTarget == 2 || this.mStartupTarget == 4) {
                this.mQuickSlideOutController.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, null);
            } else if (this.mStartupTarget == 3) {
                this.mQuickStepController.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, this.mGoHomeRunnable);
            }
        }
    }

    protected void onGestureEnd(int action) {
        super.onGestureEnd(action);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        boolean firstAftChecking = this.mShouldCheckAftForThisGesture && !this.mFirstAftTriggered;
        if (firstAftChecking) {
            if (this.mGestureEnd && !this.mGestureFailed) {
                this.mFirstAftTriggered = true;
                this.mLastAftGestureTime = SystemClock.uptimeMillis();
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavConst.TAG_GESTURE_BOTTOM;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("gesture end, mLastAftGestureTime=");
                    stringBuilder.append(this.mLastAftGestureTime);
                    Log.d(str, stringBuilder.toString());
                }
            }
            if (!this.mInTaskLockMode) {
                return result;
            }
        }
        handleTouchEvent(event);
        if (!firstAftChecking && this.mGestureEnd && !this.mGestureFailed && this.mFirstAftTriggered) {
            this.mFirstAftTriggered = false;
            if (this.mInTaskLockMode) {
                exitLockTaskMode();
            }
        }
        return result;
    }

    private void handleTouchEvent(MotionEvent event) {
        switch (this.mStartupTarget) {
            case 1:
                handleSingleHand(event);
                return;
            case 2:
            case 4:
                handleQuickSlideOut(event);
                return;
            case 3:
                handleQuickStep(event);
                return;
            default:
                return;
        }
    }

    private void handleSingleHand(MotionEvent event) {
        if (!this.mPreConditionNotReady) {
            int actionMasked = event.getActionMasked();
            if (actionMasked != 3) {
                switch (actionMasked) {
                    case 0:
                        setUseProxyAngleStrategy(true);
                        if (this.mQuickSingleHandController.isPreConditionNotReady(false)) {
                            Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "QuickSingleHand precondition not ready at down");
                            this.mPreConditionNotReady = true;
                            return;
                        }
                        break;
                    case 1:
                        break;
                }
            }
            setUseProxyAngleStrategy(false);
            this.mQuickSingleHandController.setGestureResultAtUp(this.mGestureFailed ^ 1);
            this.mQuickSingleHandController.handleTouchEvent(event);
        }
    }

    private void handleQuickSlideOut(MotionEvent event) {
        if (!this.mPreConditionNotReady) {
            int actionMasked = event.getActionMasked();
            if (actionMasked != 3) {
                switch (actionMasked) {
                    case 0:
                        boolean onLeft = this.mStartupTarget == 2;
                        if (!this.mQuickSlideOutController.isPreConditionNotReady(onLeft)) {
                            this.mQuickSlideOutController.setSlidingSide(onLeft);
                            break;
                        }
                        String str = GestureNavConst.TAG_GESTURE_BOTTOM;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("QuickSlideOut precondition not ready at ");
                        stringBuilder.append(onLeft ? "left" : "right");
                        stringBuilder.append(" down");
                        Log.i(str, stringBuilder.toString());
                        this.mPreConditionNotReady = true;
                        return;
                    case 1:
                        break;
                }
            }
            this.mQuickSlideOutController.setGestureResultAtUp(this.mGestureFailed ^ 1);
            this.mQuickSlideOutController.handleTouchEvent(event);
        }
    }

    private void handleQuickStep(MotionEvent event) {
        this.mQuickStepController.handleTouchEvent(event);
    }

    private void hideCurrentInputMethod() {
        if (this.mInputMethodManagerInternal == null) {
            this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        }
        if (this.mInputMethodManagerInternal != null) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "hide input method if need");
            }
            this.mInputMethodManagerInternal.hideCurrentInputMethod();
        }
    }

    private boolean shouldCheckAftForThisGesture() {
        return HwGestureNavWhiteConfig.getInstance().isEnable();
    }

    private void showReTryToast() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "showReTryToast");
        }
        this.mBottomHandler.post(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(GestureNavBottomStrategy.this.mContext, 33686186, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                LayoutParams windowParams = toast.getWindowParams();
                windowParams.privateFlags |= 16;
                toast.show();
            }
        });
    }

    private void exitLockTaskMode() {
        Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "start exit lock task mode");
        this.mBottomHandler.post(new Runnable() {
            public void run() {
                GestureUtils.exitLockTaskMode();
            }
        });
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mShouldCheckAftForThisGesture=");
        stringBuilder.append(this.mShouldCheckAftForThisGesture);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mFirstAftTriggered=");
        stringBuilder.append(this.mFirstAftTriggered);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLastAftGestureTime=");
        stringBuilder.append(this.mLastAftGestureTime);
        pw.print(stringBuilder.toString());
        pw.println();
        if (this.mQuickSlideOutController != null) {
            this.mQuickSlideOutController.dump(prefix, pw, args);
        }
        if (this.mQuickStepController != null) {
            this.mQuickStepController.dump(prefix, pw, args);
        }
    }
}
