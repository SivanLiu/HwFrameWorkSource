package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.hardware.display.HwFoldScreenState;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.Log;
import android.util.MathUtils;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavView;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.wm.WindowState;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.hwdockbar.IDockAidlInterface;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class GestureNavBackStrategy extends GestureNavBaseStrategy {
    private static final int BINDER_FLAG = 0;
    private static final int MSG_CHECK_HAPTICS_VIBRATOR = 1;
    private static final String TAG = "GestureNavBackStrategy";
    /* access modifiers changed from: private */
    public ServiceConnection conn = new ServiceConnection() {
        /* class com.android.server.gesture.GestureNavBackStrategy.AnonymousClass1 */

        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            GestureNavBaseStrategy.mDockService = IDockAidlInterface.Stub.asInterface(binder);
            try {
                GestureNavBaseStrategy.mDockService.asBinder().linkToDeath(GestureNavBackStrategy.this.mDeathRecipient, 0);
                GestureNavBaseStrategy.mDockService.connect(GestureNavBackStrategy.this.mNavId);
            } catch (RemoteException e) {
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavBackStrategy.TAG, "onServiceConnected failed");
                }
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            if (GestureNavBackStrategy.this.mContext != null) {
                GestureNavBackStrategy.this.mContext.unbindService(GestureNavBackStrategy.this.conn);
            }
        }
    };
    private float mAnimStartPosition;
    private Handler mBackHandler;
    private int mBackMaxDistance1;
    private int mBackMaxDistance2;
    /* access modifiers changed from: private */
    public IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        /* class com.android.server.gesture.GestureNavBackStrategy.AnonymousClass2 */

        public void binderDied() {
            if (GestureNavBaseStrategy.mDockService != null) {
                GestureNavBaseStrategy.mDockService.asBinder().unlinkToDeath(GestureNavBackStrategy.this.mDeathRecipient, 0);
                GestureNavBaseStrategy.mDockService = null;
            }
        }
    };
    private GestureDataTracker mGestureDataTracker;
    private GestureNavView.IGestureNavBackAnim mGestureNavBackAnim;
    private boolean mIsAnimPositionSetup;
    private boolean mIsAnimProcessedOnce;
    private boolean mIsAnimProcessing;
    private boolean mIsDisLargeEnough = false;
    private boolean mIsGestureNavEnable = false;
    private boolean mIsInHomeOfLauncherTmp = false;
    private boolean mIsShouldTurnOffAnim = false;
    private boolean mIsShowDockEnable = false;

    public GestureNavBackStrategy(int navId, Context context, Looper looper, GestureNavView.IGestureNavBackAnim backAnim) {
        super(navId, context, looper);
        this.mGestureNavBackAnim = backAnim;
        this.mBackHandler = new BackHandler(looper);
        this.mGestureDataTracker = GestureDataTracker.getInstance(context);
    }

    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void updateConfig(int displayWidth, int displayHeight, Rect r, int rotation) {
        super.updateConfig(displayWidth, displayHeight, r, rotation);
        this.mBackMaxDistance1 = GestureNavConst.getBackMaxDistanceOne(this.mContext);
        this.mBackMaxDistance2 = GestureNavConst.getBackMaxDistanceTwo(this.mContext);
        if (GestureNavConst.DEBUG_ALL) {
            Log.d(GestureNavConst.TAG_GESTURE_BACK, "distance1:" + this.mBackMaxDistance1 + ", distance2:" + this.mBackMaxDistance2);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureStarted(float rawX, float rawY) {
        super.onGestureStarted(rawX, rawY);
        this.mIsAnimPositionSetup = false;
        this.mIsAnimProcessing = false;
        this.mIsAnimProcessedOnce = false;
        this.mAnimStartPosition = rawY;
        this.mGestureNavBackAnim.setSide(this.mNavId == 1);
        this.mIsInHomeOfLauncherTmp = this.mIsInHomeOfLauncher;
        this.mIsDisLargeEnough = false;
        this.mIsGestureNavEnable = GestureNavConst.isGestureNavEnabled(this.mContext, -2);
        this.mIsShowDockEnable = GestureNavConst.isShowDockEnabled(this.mContext, -2);
        this.mIsShowDockPreCondSatisfied = isShowDockPreCondSatisfied();
        this.mGestureNavBackAnim.setDockIcon(isShouldShowDockAnimation());
        this.mIsShouldTurnOffAnim = isShouldTurnOffAnimation();
        switchFocusIfNeeded((int) rawX, (int) rawY);
        if (HwMwUtils.ENABLED && this.mIsGestureNavEnable) {
            HwMwUtils.performPolicy((int) CPUFeature.MSG_SET_LIMIT_CGROUP, new Object[]{Integer.valueOf((int) rawX), Integer.valueOf((int) rawY)});
        }
    }

    private boolean isShowDockPreCondSatisfied() {
        return this.mIsShowDockEnable && !GestureNavConst.isSimpleMode(this.mContext, -2) && !GestureNavConst.isInSuperSaveMode() && !GestureNavConst.isInScreenReaderMode(this.mContext, -2) && !isInLazyMode() && !isInSubOrCoorFoldDisplayMode() && !GestureNavConst.isKeyguardLocked(this.mContext) && !isNavBarOnRightSide() && !isDockInEditState() && !isMultiWindowDisabled() && !isInStartUpGuide();
    }

    private boolean isInLazyMode() {
        String defaultMode = Settings.Global.getString(this.mContext.getContentResolver(), "single_hand_mode");
        Log.i(GestureNavConst.TAG_GESTURE_BACK, "defaultMode:" + defaultMode);
        if (defaultMode == null || "".equals(defaultMode)) {
            return false;
        }
        return true;
    }

    private boolean isInSubOrCoorFoldDisplayMode() {
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal;
        if (!HwFoldScreenState.isFoldScreenDevice() || (hwFoldScreenManagerInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class)) == null) {
            return false;
        }
        int displayMode = hwFoldScreenManagerInternal.getDisplayMode();
        if (displayMode == 3 || displayMode == 4) {
            return true;
        }
        return false;
    }

    private boolean isShouldShowDockAnimation() {
        if (!this.mIsShowDockEnable) {
            return false;
        }
        if (!this.mIsGestureNavEnable) {
            return true;
        }
        if (this.mIsShowDockPreCondSatisfied) {
            return this.mIsInHomeOfLauncherTmp;
        }
        return false;
    }

    private boolean isShouldTurnOffAnimation() {
        if (!this.mIsShowDockEnable) {
            return false;
        }
        if (!this.mIsGestureNavEnable) {
            return !this.mIsShowDockPreCondSatisfied;
        }
        if (this.mIsShowDockPreCondSatisfied || !this.mIsInHomeOfLauncherTmp) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureReallyStarted() {
        super.onGestureReallyStarted();
        if (this.mIsSubScreenGestureNav) {
            this.mIsAnimPositionSetup = false;
        } else if (this.mIsShouldTurnOffAnim) {
            this.mIsAnimPositionSetup = false;
        } else if (!this.mIsAnimPositionSetup) {
            this.mIsAnimPositionSetup = true;
            this.mGestureNavBackAnim.setAnimPosition(this.mAnimStartPosition);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSlowProcessStarted(ArrayList<Float> pendingMoveDistance) {
        int size;
        super.onGestureSlowProcessStarted(pendingMoveDistance);
        if (this.mIsSubScreenGestureNav) {
            this.mIsAnimProcessing = false;
        } else if (this.mIsShouldTurnOffAnim) {
            this.mIsAnimProcessing = false;
        } else {
            if (!this.mIsAnimProcessing) {
                this.mIsAnimProcessing = true;
            }
            if (pendingMoveDistance != null && (size = pendingMoveDistance.size()) > 0) {
                for (int i = 0; i < size; i++) {
                    notifyAnimProcess(pendingMoveDistance.get(i).floatValue());
                }
                if (GestureNavConst.DEBUG) {
                    Log.d(GestureNavConst.TAG_GESTURE_BACK, "interpolate " + size + " pending datas");
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSlowProcess(float distance, float offsetX, float offsetY) {
        super.onGestureSlowProcess(distance, offsetX, offsetY);
        notifyAnimProcess(distance);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureAnimateScatterProcess(float fromProcess, float toProcess) {
        super.onGestureAnimateScatterProcess(fromProcess, toProcess);
        animteScatterProcess(fromProcess, toProcess);
    }

    private void animteScatterProcess(float fromProcess, float toProcess) {
        this.mGestureNavBackAnim.playScatterProcessAnim(fromProcess, toProcess);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureFailed(int reason, int action) {
        super.onGestureFailed(reason, action);
        if (this.mIsAnimPositionSetup) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_BACK, "gesture failed, disappear anim");
            }
            this.mGestureNavBackAnim.playDisappearAnim();
        }
        if (isEffectiveFailedReason(reason)) {
            this.mGestureDataTracker.gestureBackEvent(this.mNavId, false);
        }
        Flog.bdReport(this.mContext, 854, GestureNavConst.reportResultStr(false, this.mNavId, reason));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGesturePreLoad() {
        super.onGesturePreLoad();
        Intent i = new Intent(GestureNavConst.DEFAULT_DOCK_AIDL_INTERFACE);
        i.setComponent(new ComponentName(GestureNavConst.DEFAULT_DOCK_PACKAGE, GestureNavConst.DEFAULT_DOCK_MAIN_CLASS));
        if (mDockService == null || !mDockService.asBinder().isBinderAlive() || !mDockService.asBinder().pingBinder()) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "mDockService == null!");
            }
            this.mContext.bindServiceAsUser(i, this.conn, 1, UserHandle.CURRENT);
            return;
        }
        try {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "mDockService.connect()!");
            }
            mDockService.connect(this.mNavId);
        } catch (RemoteException e) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "mDockService connect failed");
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void onGestureSuccessFinished(float distance, long durationTime, float velocity, boolean isFastSlideGesture, boolean isDockGesture) {
        super.onGestureSuccessFinished(distance, durationTime, velocity, isFastSlideGesture, isDockGesture);
        checkHwHapticsVibrator();
        boolean isHomeOfLauncher = this.mIsShowDockEnable && this.mIsInHomeOfLauncherTmp;
        if (!isDockGesture && this.mIsGestureNavEnable && !isHomeOfLauncher) {
            sendKeyEvent(4);
        }
        if (!this.mIsSubScreenGestureNav && !this.mIsShouldTurnOffAnim) {
            if (this.mIsAnimProcessing && this.mIsAnimProcessedOnce) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavConst.TAG_GESTURE_BACK, "gesture finished, disappear anim");
                }
                this.mGestureNavBackAnim.playDisappearAnim();
            } else if (isFastSlideGesture) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavConst.TAG_GESTURE_BACK, "gesture finished, play fast anim, velocity=" + velocity);
                }
                if (!this.mIsAnimPositionSetup) {
                    this.mIsAnimPositionSetup = true;
                    this.mGestureNavBackAnim.setAnimPosition(this.mAnimStartPosition);
                }
                this.mGestureNavBackAnim.playFastSlidingAnim();
            } else {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavConst.TAG_GESTURE_BACK, "velocity does not meet the threshold, disappear anim");
                }
                this.mGestureNavBackAnim.playDisappearAnim();
            }
            this.mGestureDataTracker.gestureBackEvent(this.mNavId, true);
            Flog.bdReport(this.mContext, 854, GestureNavConst.reportResultStr(true, this.mNavId, -1));
        }
    }

    private void notifyAnimProcess(float distance) {
        if (!this.mIsSubScreenGestureNav && !this.mIsShouldTurnOffAnim) {
            float process = getRubberbandProcess(distance);
            boolean isSuccess = this.mGestureNavBackAnim.setAnimProcess(process);
            if (!this.mIsAnimProcessedOnce && isSuccess) {
                this.mIsAnimProcessedOnce = true;
            }
            if (GestureNavConst.DEBUG_ALL) {
                Log.d(GestureNavConst.TAG_GESTURE_BACK, "process=" + process + ", distance=" + distance + ", animOnce=" + this.mIsAnimProcessedOnce);
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void switchAnimationForDockIfNeed(boolean isDockShowing, boolean isDisLargeEnough) {
        if (GestureNavConst.DEBUG_ALL) {
            Log.d(TAG, "switchAnimationForDockIfNeed: isDockShowing=" + isDockShowing + "; mIsDisLargeEnough=" + this.mIsDisLargeEnough + "; isDisLargeEnough=" + isDisLargeEnough);
        }
        if (!this.mIsInHomeOfLauncherTmp) {
            if ((!isDockShowing || !isDisLargeEnough) && this.mIsDisLargeEnough != isDisLargeEnough) {
                if (this.mIsShowDockEnable && this.mIsGestureNavEnable) {
                    this.mGestureNavBackAnim.switchDockIcon(isDisLargeEnough);
                }
                this.mIsDisLargeEnough = isDisLargeEnough;
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public float getRubberbandProcess(float distance) {
        float rubber;
        if (distance < 0.0f) {
            return 0.0f;
        }
        int i = this.mBackMaxDistance1;
        if (distance < ((float) i)) {
            float process = (distance / ((float) i)) * 0.88f;
            if (process < 0.1f) {
                return 0.1f;
            }
            return process;
        }
        int backMaxDistanceDiff = this.mBackMaxDistance2 - i;
        if (backMaxDistanceDiff != 0) {
            rubber = (distance - ((float) i)) / ((float) backMaxDistanceDiff);
        } else {
            rubber = distance - ((float) i);
        }
        return 0.88f + (MathUtils.constrain(rubber, 0.0f, 1.0f) * 0.120000005f);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void rmvDockDeathRecipient() {
        if (mDockService != null) {
            try {
                mDockService.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            } catch (NoSuchElementException e) {
                Log.i(TAG, "rmvDockDeathRecipient: no such element.");
            }
        }
    }

    private final class BackHandler extends Handler {
        BackHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                GestureUtils.performHapticFeedbackIfNeed(GestureNavBackStrategy.this.mContext);
            }
        }
    }

    private void checkHwHapticsVibrator() {
        if (!this.mBackHandler.hasMessages(1)) {
            this.mBackHandler.sendEmptyMessage(1);
        }
    }

    private boolean isNavBarOnRightSide() {
        WindowManagerPolicy.WindowState navBar;
        if (this.mIsGestureNavEnable || this.mNavId != 2 || (navBar = DeviceStateController.getInstance(this.mContext).getNavigationBar()) == null || navBar.getDisplayFrameLw() == null || navBar.getDisplayFrameLw().top != 0) {
            return false;
        }
        return true;
    }

    private static boolean isDockInEditState() {
        boolean isEditStatus = false;
        if (mDockService != null && mDockService.asBinder().isBinderAlive() && mDockService.asBinder().pingBinder()) {
            try {
                isEditStatus = mDockService.isEditState();
            } catch (RemoteException e) {
                Log.i(TAG, "dock service exception.");
            }
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "isDockInEditState: isEditStatus=" + isEditStatus);
            }
        }
        return isEditStatus;
    }

    private boolean isImeWinContainsPoint(int posX, int posY) {
        WindowManagerPolicy.WindowState ws = DeviceStateController.getInstance(this.mContext).getInputMethodWindow();
        WindowState imeWin = null;
        if (ws instanceof WindowState) {
            imeWin = (WindowState) ws;
        }
        int i = 0;
        if (imeWin == null || !imeWin.isVisibleLw() || imeWin.getBounds() == null || imeWin.getDisplayFrameLw() == null || imeWin.getContentFrameLw() == null || imeWin.getGivenContentInsetsLw() == null) {
            return false;
        }
        Rect inputRect = new Rect(imeWin.getBounds());
        int top = Math.max(imeWin.getDisplayFrameLw().top, imeWin.getContentFrameLw().top) + imeWin.getGivenContentInsetsLw().top;
        if (top != inputRect.bottom) {
            i = top;
        }
        inputRect.top = i;
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "isImeWinContainsPoint: (" + posX + ", " + posY + "); rect=" + inputRect);
        }
        return inputRect.contains(posX, posY);
    }

    private void switchFocusIfNeeded(int touchDownX, int touchDownY) {
        List<ActivityManager.RunningTaskInfo> visibleTaskInfoList;
        if (this.mIsShowDockEnable && this.mIsGestureNavEnable && (visibleTaskInfoList = HwActivityTaskManager.getVisibleTasks()) != null && !visibleTaskInfoList.isEmpty()) {
            boolean isHasHwMultiWindow = false;
            Iterator<ActivityManager.RunningTaskInfo> it = visibleTaskInfoList.iterator();
            while (true) {
                if (it.hasNext()) {
                    ActivityManager.RunningTaskInfo rti = it.next();
                    if (rti != null && WindowConfiguration.isHwMultiStackWindowingMode(rti.windowMode)) {
                        isHasHwMultiWindow = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (isHasHwMultiWindow) {
                int touchDownX2 = getTouchDownOffsetX(touchDownX);
                if (!isImeWinContainsPoint(touchDownX2, touchDownY)) {
                    for (ActivityManager.RunningTaskInfo rti2 : visibleTaskInfoList) {
                        if (rti2 != null && !WindowConfiguration.isHwFreeFormWindowingMode(rti2.windowMode) && rti2.bounds != null && rti2.bounds.contains(touchDownX2, touchDownY)) {
                            try {
                                ActivityTaskManager.getService().setFocusedTask(rti2.taskId);
                                return;
                            } catch (RemoteException remoteExp) {
                                Log.d("TaskPositioningController", "switchFocusIfNeeded: ", remoteExp);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private int getTouchDownOffsetX(int touchDownX) {
        if (!this.mHasNotch) {
            return touchDownX;
        }
        int statusBarHeight = GestureNavConst.getStatusBarHeight(this.mContext);
        int i = this.mRotation;
        if (i != 1) {
            if (i == 3 && this.mNavId == 2) {
                return touchDownX - statusBarHeight;
            }
            return touchDownX;
        } else if (this.mNavId == 1) {
            return touchDownX + statusBarHeight;
        } else {
            return touchDownX;
        }
    }

    private boolean isMultiWindowDisabled() {
        return HwActivityTaskManager.getMultiWindowDisabled();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.server.gesture.GestureNavBaseStrategy
    public void dismissDockBar() {
        super.dismissDockBar();
        if (mDockService != null && mDockService.asBinder().isBinderAlive() && mDockService.asBinder().pingBinder()) {
            try {
                mDockService.dismissWithAnimation();
            } catch (RemoteException e) {
                Log.e(TAG, "Dock dismiss failed");
            }
        } else if (GestureNavConst.DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("mDockService != null:");
            sb.append(mDockService != null);
            Log.d(TAG, sb.toString());
            if (mDockService != null) {
                Log.d(TAG, "isBinderAlive:" + mDockService.asBinder().isBinderAlive());
                Log.d(TAG, "pingBinder:" + mDockService.asBinder().pingBinder());
            }
        }
    }

    private boolean isInStartUpGuide() {
        DeviceStateController deviceStateController = DeviceStateController.getInstance(this.mContext);
        return !deviceStateController.isDeviceProvisioned() || !deviceStateController.isCurrentUserSetup() || deviceStateController.isOOBEActivityEnabled() || deviceStateController.isSetupWizardEnabled();
    }
}
