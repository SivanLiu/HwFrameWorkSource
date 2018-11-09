package com.android.server.gesture;

import android.content.Context;
import android.view.MotionEvent;
import com.android.server.gesture.DeviceStateController.DeviceChangedListener;

public class QuickStartupStub {
    protected Context mContext;
    private final DeviceChangedListener mDeviceChangedCallback = new DeviceChangedListener() {
        public void onUserSwitched(int newUserId) {
            QuickStartupStub.this.updateSettings();
        }

        public void onConfigurationChanged() {
            QuickStartupStub.this.updateConfig();
        }
    };
    protected DeviceStateController mDeviceStateController;
    protected boolean mGestureFailed;
    protected boolean mGestureReallyStarted;
    protected boolean mGestureSlowProcessStarted;
    protected boolean mIsFastSlideGesture;
    protected boolean mIsValidGuesture = true;

    public QuickStartupStub(Context context) {
        this.mContext = context;
        this.mDeviceStateController = DeviceStateController.getInstance(context);
    }

    public void setGestureResultAtUp(boolean success) {
        this.mIsValidGuesture &= success;
    }

    public void onGestureStarted() {
    }

    public void onGestureReallyStarted() {
        this.mGestureReallyStarted = true;
    }

    public void onGestureSlowProcessStarted() {
        this.mGestureSlowProcessStarted = true;
    }

    public void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
    }

    public void onGestureFailed() {
        this.mGestureFailed = true;
    }

    public void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture, Runnable runnable) {
        this.mIsFastSlideGesture = isFastSlideGesture;
    }

    public void onGestureEnd(int action) {
    }

    protected void resetAtDown() {
        this.mIsValidGuesture = true;
        this.mGestureReallyStarted = false;
        this.mGestureSlowProcessStarted = false;
        this.mGestureFailed = false;
        this.mIsFastSlideGesture = false;
    }

    public void handleTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                resetAtDown();
                return;
            case 1:
            case 3:
                if (this.mIsValidGuesture) {
                    quickStartup();
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void onNavCreate(GestureNavView navView) {
        this.mDeviceStateController.addCallback(this.mDeviceChangedCallback);
    }

    public void onNavUpdate() {
    }

    public void onNavDestroy() {
        this.mDeviceStateController.removeCallback(this.mDeviceChangedCallback);
    }

    public boolean isPreConditionNotReady(boolean left) {
        return false;
    }

    public void quickStartup() {
    }

    public void updateSettings() {
    }

    public void updateConfig() {
    }
}
