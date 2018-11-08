package com.android.server.gesture;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.MotionEvent;
import com.android.server.gesture.GestureNavView.IGestureNavBottomAnim;
import java.io.PrintWriter;

public class QuickStepController extends QuickStartupStub {
    public static final int HIT_TARGET_OVERVIEW = 3;
    private static final String TALKBACK_COMPONENT_NAME = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private IGestureNavBottomAnim mGestureNavBottomAnim;
    private boolean mKeyguardShowing;
    private Looper mLooper;
    private GestureNavView mNavView;
    private Object mOverviewEventSender;
    private boolean mQuickStepStarted;
    private SettingsObserver mSettingsObserver;
    private boolean mTalkBackOn;
    private final Matrix mTransformGlobalMatrix = new Matrix();
    private final Matrix mTransformLocalMatrix = new Matrix();

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            QuickStepController.this.updateSettings();
        }
    }

    public QuickStepController(Context context, Looper looper, IGestureNavBottomAnim bottomAnim) {
        super(context);
        this.mLooper = looper;
        this.mGestureNavBottomAnim = bottomAnim;
    }

    private Object getOverviewProxy() {
        return this.mOverviewEventSender != null ? null : null;
    }

    public boolean isSupportMultiTouch() {
        return this.mTalkBackOn;
    }

    public void updateDeviceState(boolean keyguardShowing) {
        this.mKeyguardShowing = keyguardShowing;
    }

    public void updateSettings() {
        boolean contains;
        boolean z = false;
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean accessibilityEnabled = Secure.getIntForUser(resolver, "accessibility_enabled", 0, -2) == 1;
        String enabledSerices = Secure.getStringForUser(resolver, "enabled_accessibility_services", -2);
        if (enabledSerices != null) {
            contains = enabledSerices.contains(TALKBACK_COMPONENT_NAME);
        } else {
            contains = false;
        }
        if (accessibilityEnabled) {
            z = contains;
        }
        this.mTalkBackOn = z;
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QS, "accessEnabled:" + accessibilityEnabled + ", hasTalkback:" + contains);
        }
    }

    public void onNavCreate(GestureNavView navView) {
        super.onNavCreate(navView);
        this.mNavView = navView;
        this.mSettingsObserver = new SettingsObserver(new Handler(this.mLooper));
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Secure.getUriFor("accessibility_enabled"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("enabled_accessibility_services"), false, this.mSettingsObserver, -1);
        updateSettings();
    }

    public void onNavUpdate() {
        super.onNavUpdate();
        if (this.mNavView != null) {
            this.mNavView.invalidate();
        }
    }

    public void onNavDestroy() {
        super.onNavDestroy();
        if (this.mSettingsObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            this.mSettingsObserver = null;
        }
        if (this.mOverviewEventSender != null) {
            this.mOverviewEventSender = null;
        }
        this.mNavView = null;
    }

    public void onGestureStarted() {
        super.onGestureStarted();
        if (GestureNavConst.USE_ANIM_LEGACY && (this.mKeyguardShowing ^ 1) != 0) {
            this.mGestureNavBottomAnim.onGestureStarted();
        }
    }

    public void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        if (GestureNavConst.USE_ANIM_LEGACY && (this.mKeyguardShowing ^ 1) != 0) {
            this.mGestureNavBottomAnim.onGestureReallyStarted();
        }
    }

    public void onGestureSlowProcessStarted() {
        super.onGestureSlowProcessStarted();
        if (GestureNavConst.USE_ANIM_LEGACY && (this.mKeyguardShowing ^ 1) != 0) {
            this.mGestureNavBottomAnim.onGestureSlowProcessStarted();
        }
    }

    public void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        super.onGestureSlowProcess(distance, offsetX, offsetY);
        if (GestureNavConst.USE_ANIM_LEGACY && (this.mKeyguardShowing ^ 1) != 0) {
            this.mGestureNavBottomAnim.onGestureSlowProcess(distance, offsetX, offsetY);
        }
    }

    public void onGestureFailed() {
        super.onGestureFailed();
        if (GestureNavConst.USE_ANIM_LEGACY && (this.mKeyguardShowing ^ 1) != 0) {
            this.mGestureNavBottomAnim.onGestureFailed();
        }
    }

    public void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture, Runnable runnable) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, runnable);
        if (this.mKeyguardShowing) {
            if (isGoHomeAllRight(distance, durationTime) && runnable != null) {
                runnable.run();
            } else if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_QS, "keyguard go home fail, distance=" + distance + ", durationTime=" + durationTime);
            }
        } else if (GestureNavConst.USE_ANIM_LEGACY) {
            this.mGestureNavBottomAnim.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, runnable);
        }
    }

    public void onGestureEnd(int action) {
        super.onGestureEnd(action);
    }

    public void handleTouchEvent(MotionEvent event) {
        if (!GestureNavConst.USE_ANIM_LEGACY && getOverviewProxy() != null && !this.mKeyguardShowing) {
            switch (event.getActionMasked()) {
                case 0:
                    this.mTransformGlobalMatrix.set(Matrix.IDENTITY_MATRIX);
                    this.mTransformLocalMatrix.set(Matrix.IDENTITY_MATRIX);
                    if (this.mNavView != null) {
                        this.mNavView.transformMatrixToGlobal(this.mTransformGlobalMatrix);
                        this.mNavView.transformMatrixToLocal(this.mTransformLocalMatrix);
                    }
                    this.mQuickStepStarted = false;
                    this.mGestureReallyStarted = false;
                    break;
                case 2:
                    if (!this.mQuickStepStarted && this.mGestureReallyStarted) {
                        startQuickStep(event);
                        break;
                    }
            }
            proxyMotionEvents(event);
        }
    }

    private void startQuickStep(MotionEvent event) {
        this.mQuickStepStarted = true;
        if (getOverviewProxy() != null) {
        }
    }

    private boolean proxyMotionEvents(MotionEvent event) {
        return getOverviewProxy() == null ? false : false;
    }

    private boolean isGoHomeAllRight(float distance, long durationTime) {
        if (distance <= 180.0f || durationTime >= 500) {
            return false;
        }
        return true;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.println("mTalkBackOn=" + this.mTalkBackOn);
        Object obj = this.mOverviewEventSender;
    }
}
