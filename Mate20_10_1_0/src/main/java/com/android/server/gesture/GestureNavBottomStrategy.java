package com.android.server.gesture;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.CoordinationModeUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.widget.Toast;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.fsm.HwFoldScreenManager;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GestureNavBottomStrategy extends GestureNavBaseStrategy {
    private static final int GESTURE_NAV_EVENT_TRANSACTION_CODE = 123;
    private static final int MSG_HIDE_INPUTMETHOD_IF_NEED = 1;
    private static final int STARTUP_TARGET_LEFT_CORNER = 1;
    private static final int STARTUP_TARGET_NONE = 0;
    private static final int STARTUP_TARGET_QUICK_STEP = 3;
    private static final int STARTUP_TARGET_RIGHT_CORNER = 5;
    private static final int STARTUP_TARGET_SLIDE_LEFT = 2;
    private static final int STARTUP_TARGET_SLIDE_RIGHT = 4;
    private Handler mBottomHandler;
    private boolean mFirstAftTriggered;
    private final Runnable mGoHomeRunnable = new Runnable() {
        /* class com.android.server.gesture.GestureNavBottomStrategy.AnonymousClass1 */

        public void run() {
            GestureNavBottomStrategy.this.sendKeyEvent(3);
        }
    };
    private InputMethodManagerInternal mInputMethodManagerInternal;
    private boolean mIsGestureNavTipsEnabled;
    private boolean mIsInTaskLockMode;
    private boolean mIsKeyguardShowing;
    private boolean mIsLandscape;
    private long mLastAftGestureTime;
    private boolean mPreConditionNotReady;
    private QuickSingleHandController mQuickSingleHandController;
    private QuickSlideOutController mQuickSlideOutController;
    private QuickStepController mQuickStepController;
    private final Object mServiceAquireLock = new Object();
    private boolean mShouldCheckAftForThisGesture;
    private int mStartupTarget = 0;
    private IStatusBarService mStatusBarService;
    private int mTabletSideWidth;
    private WindowManagerInternal mWindowManagerInternal;

    public GestureNavBottomStrategy(int navId, Context context, Looper looper) {
        super(navId, context, looper);
        this.mBottomHandler = new BottomHandler(looper);
    }

    private final class BottomHandler extends Handler {
        BottomHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                GestureNavBottomStrategy.this.hideCurrentInputMethod();
            }
        }
    }

    private void notifyStart() {
        this.mIsInTaskLockMode = GestureUtils.isInLockTaskMode();
        this.mQuickStepController = new QuickStepController(this.mContext, this.mLooper);
        this.mQuickSingleHandController = new QuickSingleHandController(this.mContext, this.mLooper);
        this.mQuickSlideOutController = new QuickSlideOutController(this.mContext, this.mLooper);
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "notifyStart mIsInTaskLockMode=" + this.mIsInTaskLockMode);
        }
    }

    private void notifyStop() {
        this.mQuickStepController = null;
        this.mQuickSingleHandController = null;
        this.mQuickSlideOutController = null;
    }

    private boolean isSingleHandEnableAndAvailable() {
        QuickSingleHandController quickSingleHandController = this.mQuickSingleHandController;
        return quickSingleHandController != null && quickSingleHandController.isSingleHandEnableAndAvailable();
    }

    private boolean isSlideOutEnableAndAvailable(boolean isLeft) {
        QuickSlideOutController quickSlideOutController = this.mQuickSlideOutController;
        return quickSlideOutController != null && quickSlideOutController.isSlideOutEnableAndAvailable(isLeft);
    }

    private int checkStartupTarget(int rawX, int rawY) {
        if (this.mIsKeyguardShowing) {
            return 3;
        }
        int rawAdapt = fitPoint(rawX);
        int width = getRegion().width();
        int singleHandWidth = (int) (GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO * ((float) getRegion().height()));
        int sideWidth = GestureNavConst.IS_TABLET ? this.mTabletSideWidth : (int) (((float) width) * ((1.0f - (this.mIsLandscape ? 0.8333333f : 0.75f)) / 2.0f));
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "checkStartupTarget width=" + width + ", rawX=" + rawX + ", rawY=" + rawY + ", singleHandWidth=" + singleHandWidth + ", rawAdapt=" + rawAdapt + ", sideWidth=" + sideWidth);
        }
        if (!this.mIsLandscape) {
            if (rawAdapt <= GestureUtils.getCurvedSideLeftDisp() + singleHandWidth && isSingleHandEnableAndAvailable()) {
                return 1;
            }
            if (rawAdapt >= (width - singleHandWidth) - GestureUtils.getCurvedSideRightDisp() && isSingleHandEnableAndAvailable()) {
                return 5;
            }
        }
        if (this.mIsInTaskLockMode || this.mIsSubScreenGestureNav) {
            return 3;
        }
        if (rawAdapt < sideWidth && isSlideOutEnableAndAvailable(true)) {
            return 2;
        }
        if ((rawAdapt < sideWidth || rawAdapt > width - sideWidth) && rawAdapt > width - sideWidth && isSlideOutEnableAndAvailable(false)) {
            return 4;
        }
        return 3;
    }

    public void updateLockTaskState(int lockTaskState) {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "lock task state changed, lockTaskState=" + lockTaskState);
        }
        this.mIsInTaskLockMode = GestureUtils.isInLockTaskMode(lockTaskState);
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateConfig(int displayWidth, int displayHeight, Rect rect, int rotation) {
        super.updateConfig(displayWidth, displayHeight, rect, rotation);
        if (GestureNavConst.IS_TABLET) {
            this.mTabletSideWidth = GestureNavConst.getBottomSideWidthForTablet(this.mContext);
        }
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateKeyguardState(boolean isKeyguardShowing) {
        super.updateKeyguardState(isKeyguardShowing);
        this.mIsKeyguardShowing = isKeyguardShowing;
        QuickStepController quickStepController = this.mQuickStepController;
        if (quickStepController != null) {
            quickStepController.updateKeyguardState(isKeyguardShowing);
        }
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateScreenConfigState(boolean isLand) {
        super.updateScreenConfigState(isLand);
        this.mIsLandscape = isLand;
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateNavTipsState(boolean isTipsEnable) {
        super.updateNavTipsState(isTipsEnable);
        this.mIsGestureNavTipsEnabled = isTipsEnable;
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onNavCreate(GestureNavView navView) {
        notifyStart();
        this.mQuickStepController.onNavCreate(navView);
        this.mQuickSlideOutController.onNavCreate(navView);
        this.mQuickSingleHandController.onNavCreate(navView);
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onNavUpdate() {
        this.mQuickStepController.onNavUpdate();
        this.mQuickSlideOutController.onNavUpdate();
        this.mQuickSingleHandController.onNavUpdate();
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onNavDestroy() {
        this.mQuickStepController.onNavDestroy();
        this.mQuickSlideOutController.onNavDestroy();
        this.mQuickSingleHandController.onNavDestroy();
        notifyStop();
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateHorizontalSwitch() {
        this.mIsHorizontalSwitch = GestureNavConst.isHorizontalSwitchEnabled(this.mContext, -2);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public int moveOutAngleThreshold() {
        int i = this.mStartupTarget;
        if (i == 2 || i == 4) {
            return 45;
        }
        return super.moveOutAngleThreshold();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public int slideOutThresholdMajorAxis() {
        QuickSlideOutController quickSlideOutController;
        int i = this.mStartupTarget;
        if ((i == 2 || i == 4) && (quickSlideOutController = this.mQuickSlideOutController) != null) {
            return quickSlideOutController.slideOutThreshold(getWindowThreshold());
        }
        return super.slideOutThresholdMajorAxis();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureStarted(float rawX, float rawY) {
        super.onGestureStarted(rawX, rawY);
        this.mPreConditionNotReady = false;
        this.mStartupTarget = checkStartupTarget((int) rawX, (int) rawY);
        int i = this.mStartupTarget;
        boolean z = true;
        if (i == 1 || i == 5) {
            this.mShouldCheckAftForThisGesture = false;
        } else {
            if (!this.mIsInTaskLockMode && !shouldCheckAftForThisGesture()) {
                z = false;
            }
            this.mShouldCheckAftForThisGesture = z;
        }
        long diffTime = 0;
        if (!this.mShouldCheckAftForThisGesture) {
            this.mFirstAftTriggered = false;
        } else if (this.mFirstAftTriggered) {
            diffTime = SystemClock.uptimeMillis() - this.mLastAftGestureTime;
            if (diffTime > 2500) {
                this.mFirstAftTriggered = false;
            }
        }
        if (GestureNavConst.DEBUG) {
            Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "StartupTarget=" + this.mStartupTarget + ", checkAft=" + this.mShouldCheckAftForThisGesture + ", firstAftTriggered=" + this.mFirstAftTriggered + ", diffTime=" + diffTime);
        }
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mIsInTaskLockMode) {
            int i2 = this.mStartupTarget;
            if (i2 == 2 || i2 == 4) {
                this.mQuickSlideOutController.onGestureStarted();
            } else if (i2 == 3) {
                this.mQuickStepController.onGestureStarted();
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        dismissDock();
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mIsInTaskLockMode) {
            int i = this.mStartupTarget;
            if (i == 2 || i == 4) {
                this.mQuickSlideOutController.onGestureReallyStarted();
            } else if (i == 3) {
                this.mQuickStepController.onGestureReallyStarted();
            }
        } else {
            showReTryToast();
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSlowProcessStarted(ArrayList<Float> pendingMoveDistance) {
        super.onGestureSlowProcessStarted(pendingMoveDistance);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mIsInTaskLockMode) {
            int i = this.mStartupTarget;
            if (i == 2 || i == 4) {
                this.mQuickSlideOutController.onGestureSlowProcessStarted();
            } else if (i == 3) {
                this.mQuickStepController.onGestureSlowProcessStarted();
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        super.onGestureSlowProcess(distance, offsetX, offsetY);
        if (this.mShouldCheckAftForThisGesture && !this.mFirstAftTriggered && this.mIsInTaskLockMode) {
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureFailed(int reason, int action) {
        super.onGestureFailed(reason, action);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mIsInTaskLockMode) {
            int i = this.mStartupTarget;
            if (i == 2 || i == 4) {
                this.mQuickSlideOutController.onGestureFailed(reason, action);
            } else if (i == 3) {
                this.mQuickStepController.onGestureFailed(reason, action);
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture, boolean isDockGesture) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, isDockGesture);
        if (!this.mShouldCheckAftForThisGesture || this.mFirstAftTriggered || this.mIsInTaskLockMode) {
            int i = this.mStartupTarget;
            if (i == 2 || i == 4) {
                this.mQuickSlideOutController.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, null);
            } else if (i == 3) {
                this.mQuickStepController.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, this.mGoHomeRunnable);
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGesturePreLoad() {
        super.onGesturePreLoad();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureEnd(int action) {
        super.onGestureEnd(action);
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public boolean onTouchEvent(MotionEvent event, boolean isFromSubScreenView) {
        boolean result = super.onTouchEvent(event, isFromSubScreenView);
        boolean isFirstAftChecking = this.mShouldCheckAftForThisGesture && !this.mFirstAftTriggered;
        if (isFirstAftChecking) {
            if (this.mGestureEnd && !this.mGestureFailed) {
                this.mFirstAftTriggered = true;
                this.mLastAftGestureTime = SystemClock.uptimeMillis();
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "gesture end, mLastAftGestureTime=" + this.mLastAftGestureTime);
                }
            }
            if (!this.mIsInTaskLockMode) {
                return result;
            }
        }
        handleTouchEvent(event);
        if (this.mIsGestureNavTipsEnabled) {
            transactGestureNavEvent(event);
        }
        if (!isFirstAftChecking && this.mGestureEnd && !this.mGestureFailed && this.mFirstAftTriggered) {
            this.mFirstAftTriggered = false;
            if (this.mIsInTaskLockMode) {
                exitLockTaskMode();
            }
        }
        return result;
    }

    private void handleTouchEvent(MotionEvent event) {
        int i = this.mStartupTarget;
        if (i != 1) {
            if (i != 2) {
                if (i == 3) {
                    handleQuickStep(event);
                    return;
                } else if (i != 4) {
                    if (i != 5) {
                        return;
                    }
                }
            }
            handleQuickSlideOut(event);
            return;
        }
        handleSingleHand(event);
    }

    private void dismissDock() {
        if (mDockService != null && mDockService.asBinder().isBinderAlive() && mDockService.asBinder().pingBinder()) {
            try {
                mDockService.dismiss();
            } catch (RemoteException e) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "Dock dismiss failed");
                }
            }
        } else if (GestureNavConst.DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("mDockService != null:");
            sb.append(mDockService != null);
            Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, sb.toString());
            if (mDockService != null) {
                Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "isBinderAlive:" + mDockService.asBinder().isBinderAlive());
                Log.d(GestureNavConst.TAG_GESTURE_BOTTOM, "pingBinder:" + mDockService.asBinder().pingBinder());
            }
        }
    }

    private void handleSingleHand(MotionEvent event) {
        if (!this.mPreConditionNotReady) {
            int actionMasked = event.getActionMasked();
            if (actionMasked == 0) {
                setUseProxyAngleStrategy(true);
                if (this.mQuickSingleHandController.isPreConditionNotReady(false)) {
                    if (GestureNavConst.DEBUG) {
                        Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "QuickSingleHand not ready at down");
                    }
                    this.mPreConditionNotReady = true;
                    return;
                }
            } else if (actionMasked == 1 || actionMasked == 3) {
                setUseProxyAngleStrategy(false);
                this.mQuickSingleHandController.setGestureResultAtUp(!this.mGestureFailed, this.mGestureFailedReason);
            }
            this.mQuickSingleHandController.handleTouchEvent(event);
            if (this.mQuickSingleHandController.isBeginFailedAsExceedDegree()) {
                Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "start transfer target to slide out");
                if (transferTargetToSlideOut()) {
                    this.mQuickSingleHandController.interrupt();
                }
            }
        }
    }

    private void handleQuickSlideOut(MotionEvent event) {
        if (!this.mPreConditionNotReady) {
            int actionMasked = event.getActionMasked();
            if (actionMasked == 0) {
                boolean isOnLeft = this.mStartupTarget == 2;
                if (this.mQuickSlideOutController.isPreConditionNotReady(isOnLeft)) {
                    if (GestureNavConst.DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("QuickSlideOut not ready at ");
                        sb.append(isOnLeft ? HwMultiWinConstants.LEFT_HAND_LAZY_MODE_STR : HwMultiWinConstants.RIGHT_HAND_LAZY_MODE_STR);
                        sb.append(" down");
                        Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, sb.toString());
                    }
                    this.mPreConditionNotReady = true;
                    return;
                }
                this.mQuickSlideOutController.setSlidingSide(isOnLeft);
            } else if (actionMasked == 1 || actionMasked == 3) {
                this.mQuickSlideOutController.setGestureResultAtUp(true ^ this.mGestureFailed, this.mGestureFailedReason);
            }
            this.mQuickSlideOutController.handleTouchEvent(event);
        }
    }

    private void handleQuickStep(MotionEvent event) {
        showOrHideInsetSurface(event);
        this.mQuickStepController.handleTouchEvent(event);
    }

    private boolean transferTargetToSlideOut() {
        int toTarget;
        int fromTarget = this.mStartupTarget;
        if (fromTarget == 1) {
            toTarget = 2;
        } else if (fromTarget != 5) {
            return false;
        } else {
            toTarget = 4;
        }
        boolean isOnLeft = toTarget == 2;
        if (!isSlideOutEnableAndAvailable(isOnLeft) || this.mQuickSlideOutController.isPreConditionNotReady(isOnLeft)) {
            return false;
        }
        this.mPreConditionNotReady = false;
        this.mStartupTarget = toTarget;
        this.mQuickSlideOutController.onGestureStarted();
        this.mQuickSlideOutController.setSlidingSide(isOnLeft);
        this.mQuickSlideOutController.resetState((float) (getRegion().height() - 1));
        this.mQuickSlideOutController.onGestureReallyStarted();
        if (isSlowProcessStarted()) {
            this.mQuickSlideOutController.onGestureSlowProcessStarted();
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void hideCurrentInputMethod() {
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

    private void showOrHideInsetSurface(MotionEvent event) {
        if (this.mWindowManagerInternal == null) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
        if (windowManagerInternal != null) {
            windowManagerInternal.showOrHideInsetSurface(event);
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
            /* class com.android.server.gesture.GestureNavBottomStrategy.AnonymousClass2 */

            public void run() {
                Toast toast = Toast.makeText(new ContextThemeWrapper(GestureNavBottomStrategy.this.mContext, 33947656), 33686228, 0);
                toast.getWindowParams().type = HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL;
                toast.getWindowParams().privateFlags |= 16;
                toast.show();
            }
        });
    }

    private void exitLockTaskMode() {
        Log.i(GestureNavConst.TAG_GESTURE_BOTTOM, "start exit lock task mode");
        this.mBottomHandler.post(new Runnable() {
            /* class com.android.server.gesture.GestureNavBottomStrategy.AnonymousClass3 */

            public void run() {
                GestureUtils.exitLockTaskMode();
            }
        });
    }

    private IStatusBarService getHwStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    private void transactGestureNavEvent(MotionEvent event) {
        Parcel data = Parcel.obtain();
        try {
            IBinder statusBarServiceBinder = getHwStatusBarService().asBinder();
            if (statusBarServiceBinder != null) {
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                data.writeParcelable(event, 0);
                statusBarServiceBinder.transact(GESTURE_NAV_EVENT_TRANSACTION_CODE, data, null, 0);
            }
        } catch (RemoteException e) {
            Log.e(GestureNavConst.TAG_GESTURE_BOTTOM, "exception occured", e);
        } catch (Throwable th) {
            data.recycle();
            throw th;
        }
        data.recycle();
    }

    private int fitPoint(int rawX) {
        if (!HwFoldScreenManager.isFoldable() || Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GestureNavConst.FOLD_SCREEN_MODE, 1, -2) != 4) {
            return rawX;
        }
        return rawX - (CoordinationModeUtils.getFoldScreenFullWidth() - CoordinationModeUtils.getFoldScreenMainWidth());
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.print("mShouldCheckAft=" + this.mShouldCheckAftForThisGesture);
        pw.print(" mFirstAftTriggered=" + this.mFirstAftTriggered);
        pw.print(" mLastAftGestureTime=" + this.mLastAftGestureTime);
        pw.print(" mIsInTaskLockMode=" + this.mIsInTaskLockMode);
        pw.println();
        QuickSlideOutController quickSlideOutController = this.mQuickSlideOutController;
        if (quickSlideOutController != null) {
            quickSlideOutController.dump(prefix, pw, args);
        }
        QuickStepController quickStepController = this.mQuickStepController;
        if (quickStepController != null) {
            quickStepController.dump(prefix, pw, args);
        }
    }
}
