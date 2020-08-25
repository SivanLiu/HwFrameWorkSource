package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.HwMwUtils;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManagerPolicyConstants;
import android.view.animation.Animation;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.magicwin.HwMagicWinAnimation;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.wm.HwMagicWinSplitAnimation;
import com.huawei.hiai.awareness.AwarenessConstants;
import java.util.List;

public class HwMagicWinWmsPolicy extends HwMwUtils.ModulePolicy {
    private static final String BYTEDANCE_PUBLISHER_WINDOW_NAME = "com.ss.android.publisher.PublisherActivity";
    private static final Rect CLIP_RECT_FOR_CLEAR = new Rect(0, 0, -1, -1);
    private static final float DELAY_MOVE = 0.375f;
    private static final int MSG_SHOW_DIALOG = 1;
    private static final int NUM_BOUNDS_LIST = 3;
    private static final String PACKAGE_INSTALLER_NAME = "com.android.permissioncontroller";
    private static final int PARAM_INDEX_FIVE = 5;
    private static final int PARAM_INDEX_FOUR = 4;
    private static final int PARAM_INDEX_ONE = 1;
    private static final int PARAM_INDEX_THREE = 3;
    private static final int PARAM_INDEX_TWO = 2;
    private static final int PARAM_INDEX_ZERO = 0;
    private static final String TAG = "HwMagicWinWmsPolicy";
    private HwMwUtils.IPolicyOperation clearTransitionAnimation = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$YvZac7xb4ioW8XAiJG4GMQb_pB8 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$4$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation computePivotForMagic = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$Xg1yUgyV1PF0nw4TT0dLDKiNyFg */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$8$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation getRotationAnima = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$wW36O0hULUIhGqhiC9JJhBbNyGI */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$6$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isNeedAnimation = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$EcdvhMmBWR_5GvftnrE4rRdfFRw */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$5$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isNeedScale = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$9sR3EwhaXemV5WDIAHZvWfoWBPI */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$3$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isNeedSync = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$bSyKkVZRHXAIC8MfoBaf5uQVNdE */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$11$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation isRightInMagicWindow = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$6ke3ESGA5Vj1Ce8o1BO69Gt860 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$9$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    /* access modifiers changed from: private */
    public Context mContext = null;
    private HwMagicWinAnimation mMagicWinAnimation;
    /* access modifiers changed from: private */
    public HwMagicWindowService mService = null;
    private SettingsObserver mSettingsObserver;
    private int mUserRotation = 0;
    private int mUserRotationMode = 0;
    /* access modifiers changed from: private */
    public WindowManagerService mWms = null;
    private HwMwUtils.IPolicyOperation setMwRoundCorner = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$EQjIoQiJ5lHI_YNcbGaOemsmM0Y */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$2$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation showDialogIfNeeded = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$qff7KUcQ5rAWYVBRcGCBYeiNczU */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$12$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updatePageTypeByKeyEvent = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$Jlh6nJJUgnNyvRGyKh0G8iMjne8 */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$0$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateStatusBar = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$FG7IRfZOsEdZIgH74I7sjfs93k */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$10$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateSystemUiVisibility = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$eHoQ6e6tlnjp8QxdXn4ghex9Ako */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$1$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateWindowAttrs = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$VxR7ltw6O5G0cwDDswLEi9rjUtI */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$7$HwMagicWinWmsPolicy(list, bundle);
        }
    };
    private HwMwUtils.IPolicyOperation updateWindowFrame = new HwMwUtils.IPolicyOperation() {
        /* class com.android.server.wm.$$Lambda$HwMagicWinWmsPolicy$Yb0PRPq0GYiveVm4uo2ipaXrJGo */

        public final void execute(List list, Bundle bundle) {
            HwMagicWinWmsPolicy.this.lambda$new$13$HwMagicWinWmsPolicy(list, bundle);
        }
    };

    public HwMagicWinWmsPolicy(HwMagicWindowService service, Context context, WindowManagerService wms) {
        this.mService = service;
        this.mContext = context;
        this.mWms = wms;
        this.mMagicWinAnimation = new HwMagicWinAnimation(this.mContext, this.mService, this.mWms);
        this.mWms.registerPointerEventListener(new WindowManagerPolicyConstants.PointerEventListener() {
            /* class com.android.server.wm.HwMagicWinWmsPolicy.AnonymousClass1 */

            public void onPointerEvent(MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == 0) {
                    synchronized (HwMagicWinWmsPolicy.this.mWms.mGlobalLock) {
                        AppWindowToken focusApp = HwMagicWinWmsPolicy.this.mWms.getRoot().getDisplayContent(0).mFocusedApp;
                        if (focusApp != null) {
                            HwMagicWinWmsPolicy.this.mService.getAmsPolicy().setWindowType((IBinder) focusApp.appToken, 1);
                        }
                    }
                }
            }
        }, 0);
        this.mSettingsObserver = new SettingsObserver(new Handler());
        this.mSettingsObserver.observe();
        addPolicy(102, this.updateWindowFrame, new Class[]{WindowState.class, Rect[].class, WindowState.class, Boolean.class});
        addPolicy(101, this.updateStatusBar, new Class[]{Integer.class, String.class});
        addPolicy(51, this.showDialogIfNeeded, new Class[]{IBinder.class, String.class});
        addPolicy(103, this.updateWindowAttrs, new Class[]{WindowState.class});
        addPolicy(71, this.isNeedSync, new Class[]{Integer.class, Integer.class, WindowState.class});
        addPolicy(70, this.getRotationAnima, new Class[]{ScreenRotationAnimation.class, Integer.class, Integer.class});
        addPolicy(105, this.computePivotForMagic, new Class[]{WindowState.class, float[].class, Integer.class, Integer.class});
        addPolicy(104, this.isRightInMagicWindow, new Class[]{Rect.class});
        addPolicy(21, this.isNeedScale, new Class[]{Integer.class, Integer.class, Point.class, Point.class, WindowState.class});
        addPolicy(106, this.setMwRoundCorner, new Class[]{WindowState.class, SurfaceControl.class, Rect.class});
        addPolicy(25, this.clearTransitionAnimation, new Class[]{AppWindowToken.class, Integer.class, Boolean.class});
        addPolicy(107, this.updateSystemUiVisibility, new Class[]{WindowState.class, Integer.class, Boolean.class});
        addPolicy(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, this.isNeedAnimation, new Class[]{String.class});
        addPolicy(128, this.updatePageTypeByKeyEvent, new Class[]{WindowState.class});
    }

    public /* synthetic */ void lambda$new$0$HwMagicWinWmsPolicy(List params, Bundle result) {
        WindowState curWindow = (WindowState) params.get(0);
        if (curWindow != null) {
            this.mService.getAmsPolicy().setWindowType((IBinder) curWindow.getAppToken(), 1);
        }
    }

    public /* synthetic */ void lambda$new$1$HwMagicWinWmsPolicy(List params, Bundle result) {
        int curVis;
        WindowState curWindow = (WindowState) params.get(0);
        int curVis2 = ((Integer) params.get(1)).intValue();
        if (((Boolean) params.get(2)).booleanValue()) {
            curVis = (curVis2 | AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION) & -17;
            if (this.mService.getConfig().isNotchModeEnabled(curWindow.getAttrs().packageName) && 5 != this.mService.getBoundsPosition(curWindow.getBounds())) {
                curVis &= -1073741833;
            }
            if (this.mService.isInAppSplitWinMode(curWindow.mAppToken == null ? null : curWindow.mAppToken.mActivityRecord)) {
                curVis &= 2147450879;
            }
        } else {
            curVis = curVis2 & 2147450879;
        }
        result.putInt("RESULT_UPDATE_SYSUIVISIBILITY", curVis);
    }

    public /* synthetic */ void lambda$new$2$HwMagicWinWmsPolicy(List params, Bundle result) {
        if (this.mService.getConfig().isSystemSupport(0)) {
            WindowState targetWindow = (WindowState) params.get(0);
            SurfaceControl targetSurfaceControl = (SurfaceControl) params.get(1);
            Rect cropSize = (Rect) params.get(2);
            if (targetWindow.getTask() == null) {
                Slog.i(TAG, "MW round corner get task is null");
            } else if (this.mService.getConfig().isSupportAppTaskSplitScreen(targetWindow.getAttrs().packageName)) {
                Slog.i(TAG, "App split is not supported round corner");
            } else if (targetWindow.inHwMagicWindowingMode()) {
                if (targetWindow.getBounds().equals(targetWindow.getTask().getBounds())) {
                    targetSurfaceControl.setWindowCrop(CLIP_RECT_FOR_CLEAR);
                    targetSurfaceControl.setCornerRadius(0.0f);
                    return;
                }
                targetSurfaceControl.setWindowCrop(cropSize);
                targetSurfaceControl.setCornerRadius(this.mService.getConfig().getCornerRadius());
                targetWindow.mMwIsCornerCropSet = true;
                if (targetWindow.mWinAnimator != null) {
                    targetWindow.mWinAnimator.setOpaqueLocked(false);
                }
            } else if (targetWindow.mMwIsCornerCropSet) {
                targetSurfaceControl.setWindowCrop(CLIP_RECT_FOR_CLEAR);
                targetSurfaceControl.setCornerRadius(0.0f);
                targetWindow.mMwIsCornerCropSet = false;
            }
        }
    }

    public /* synthetic */ void lambda$new$3$HwMagicWinWmsPolicy(List params, Bundle result) {
        boolean needScale = false;
        int left = ((Integer) params.get(0)).intValue();
        int top = ((Integer) params.get(1)).intValue();
        Point outPoint = (Point) params.get(2);
        Point tmpPoint = (Point) params.get(3);
        WindowState ws = (WindowState) params.get(4);
        outPoint.offset((int) (((float) (-tmpPoint.x)) * ws.mMwUsedScaleFactor), (int) (((float) (-tmpPoint.y)) * ws.mMwUsedScaleFactor));
        if (!((this.mService.getBounds(3, false).left == left && this.mService.getBounds(3, false).top == top) || (this.mService.getBounds(2, false).left == left && this.mService.getBounds(2, false).top == top))) {
            needScale = true;
        }
        if (needScale) {
            outPoint.x = (int) (((float) outPoint.x) * ws.mMwUsedScaleFactor);
            outPoint.y = (int) (((float) outPoint.y) * ws.mMwUsedScaleFactor);
        }
    }

    public /* synthetic */ void lambda$new$4$HwMagicWinWmsPolicy(List params, Bundle result) {
        boolean z = false;
        AppWindowToken appWindowToken = (AppWindowToken) params.get(0);
        int transit = ((Integer) params.get(1)).intValue();
        boolean enter = ((Boolean) params.get(2)).booleanValue();
        if (this.mService.getBoundsPosition(appWindowToken.getRequestedOverrideBounds()) == 1 && transit == 6) {
            Task task = appWindowToken.getTask();
            boolean isClearAnimation = !enter;
            if (enter && task != null) {
                int index = task.mChildren.size() - 1;
                while (true) {
                    if (index < 0) {
                        break;
                    }
                    AppWindowToken token = (AppWindowToken) task.mChildren.get(index);
                    if (token.mIsExiting || token.isClientHidden() || token.hiddenRequested || this.mService.getBoundsPosition(token.getRequestedOverrideBounds()) != 1) {
                        index--;
                    } else {
                        if (appWindowToken.getLayer() < token.getLayer()) {
                            z = true;
                        }
                        isClearAnimation = z;
                    }
                }
            }
            result.putBoolean("BUNDLE_IS_CLEAR_ANIMATION", isClearAnimation);
        }
    }

    public /* synthetic */ void lambda$new$5$HwMagicWinWmsPolicy(List params, Bundle result) {
        result.putBoolean("RESULT_NEED_SYSTEM_ANIMATION", this.mService.getConfig().isUsingSystemActivityAnimation((String) params.get(0)));
    }

    public void getInputMethodTouchableRegion(Region region) {
        WindowState inputWindow = this.mWms.getRoot().getCurrentInputMethodWindow();
        if (inputWindow != null) {
            inputWindow.getTouchableRegion(region);
        }
    }

    public boolean isInputMethodWindowVisible() {
        WindowState ime = this.mWms.getRoot().getCurrentInputMethodWindow();
        if (ime != null) {
            return ime.isVisible();
        }
        return false;
    }

    public void getRatio(List params, Bundle result) {
        Object param = params.get(0);
        if (param != null && (param instanceof AppWindowToken)) {
            String pkgName = this.mService.getAmsPolicy().getRealPkgName(((AppWindowToken) param).mActivityRecord);
            float ratio = this.mService.getRatio(pkgName);
            boolean isScaled = this.mService.isScaled(pkgName);
            result.putFloat("RESULT_GET_RATIO", ratio);
            result.putBoolean("RESULT_SCALE_ENABLE", isScaled);
        }
    }

    public void overrideFinishActivityAnimation(Rect finishBound, boolean isMoveToMiddleOrRight, boolean isTransition, boolean isRightEmpty) {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.overrideFinishActivityAnimation(finishBound, isMoveToMiddleOrRight, isTransition, isRightEmpty);
        }
    }

    public void setFocusBound(Rect focus) {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.setFocusBound(focus);
        }
    }

    public void setIsMiddleOnClickBefore(boolean isMiddle) {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.setIsMiddleOnClickBefore(isMiddle);
        }
    }

    public void overrideStartActivityAnimation(Rect next, boolean isRightToLeft, boolean isTransition, boolean allDrawn) {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.overrideStartActivityAnimation(next, isRightToLeft, isTransition, allDrawn);
        }
    }

    public void setOpenAppAnimation() {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.setOpenAppAnimation();
        }
    }

    public void setRotationAnimation(Rect focusBounds, int focusMode) {
        HwMagicWinAnimation hwMagicWinAnimation = this.mMagicWinAnimation;
        if (hwMagicWinAnimation != null) {
            hwMagicWinAnimation.setParamsForRotation(focusBounds, focusMode);
        }
    }

    public /* synthetic */ void lambda$new$6$HwMagicWinWmsPolicy(List params, Bundle result) {
        boolean isRotation = false;
        ScreenRotationAnimation animation = (ScreenRotationAnimation) params.get(0);
        int delta = ((Integer) params.get(1)).intValue();
        int finalHeight = ((Integer) params.get(2)).intValue();
        if (animation != null) {
            Animation[] results = new Animation[2];
            Integer[] param = {Integer.valueOf(animation.mOriginalRotation), Integer.valueOf(delta), Integer.valueOf(animation.mOriginalHeight), Integer.valueOf(finalHeight)};
            if (this.mMagicWinAnimation != null && !this.mWms.isKeyguardLocked()) {
                isRotation = this.mMagicWinAnimation.getRotationAnim(param, results);
                this.mMagicWinAnimation.resetParamsForRotation();
            }
            if (isRotation) {
                animation.mIsHwMagicWindow = true;
                animation.mRotateExitAnimation = results[0];
                animation.mRotateEnterAnimation = results[1];
            }
        }
    }

    public /* synthetic */ void lambda$new$7$HwMagicWinWmsPolicy(List params, Bundle result) {
        WindowState mFocusedWindow = (WindowState) params.get(0);
        if (mFocusedWindow.getWindowingMode() == 103 && mFocusedWindow.getAppToken() != null && !PACKAGE_INSTALLER_NAME.equals(mFocusedWindow.getAttrs().packageName)) {
            if (mFocusedWindow.getAttrs().hwFlags != 1073741824) {
                mFocusedWindow.getOriginAttrs().copyFrom(mFocusedWindow.getAttrs());
            }
            mFocusedWindow.getAttrs().hwFlags = 1073741824;
            if (this.mService.getConfig().isShowStatusBar(mFocusedWindow.getAttrs().packageName)) {
                if (mFocusedWindow.getAttrs().type != 1) {
                    return;
                }
                if (!this.mService.getAmsPolicy().isFullScreenActivity(mFocusedWindow.mAppToken.mActivityRecord) || (mFocusedWindow.getAttrs().flags & 1024) == 0) {
                    mFocusedWindow.getAttrs().flags |= 2048;
                }
            } else if (!this.mService.getConfig().isNotchModeEnabled(mFocusedWindow.getAttrs().packageName) || 5 == this.mService.getBoundsPosition(mFocusedWindow.getBounds())) {
                mFocusedWindow.getAttrs().flags |= mFocusedWindow.getAttrs().flags | 1024 | Integer.MIN_VALUE;
                mFocusedWindow.getAttrs().flags &= -2049;
            } else {
                mFocusedWindow.getAttrs().flags |= 2048;
                mFocusedWindow.getAttrs().flags &= -67108865;
            }
        } else if ((mFocusedWindow.getAttrs().hwFlags & 1073741824) != 0) {
            mFocusedWindow.getAttrs().flags = mFocusedWindow.getOriginAttrs().flags;
        } else {
            Slog.i(TAG, "updateWindowAttrs do nothing");
        }
    }

    public /* synthetic */ void lambda$new$8$HwMagicWinWmsPolicy(List params, Bundle result) {
        WindowState ws = (WindowState) params.get(0);
        if (ws.mAppToken == null) {
            Slog.w(TAG, "Window state's AppWindowToken is null. w:" + ws);
            return;
        }
        String pkgName = this.mService.getAmsPolicy().getRealPkgName(ws.mAppToken.mActivityRecord);
        Rect vRect = new Rect(ws.getFrameLw());
        vRect.right = vRect.left + ((int) (((float) vRect.width()) * this.mService.getRatio(pkgName)));
        vRect.bottom = vRect.top + ((int) (((float) vRect.height()) * this.mService.getRatio(pkgName)));
        int iconWidth = ((Integer) params.get(2)).intValue();
        int iconHeight = ((Integer) params.get(3)).intValue();
        float[] scaleTo = HwMagicWinAnimation.computeScaleToForAppExit(vRect, iconWidth, iconHeight);
        result.putFloat("BUNDLE_EXITANIM_SCALETOX", scaleTo[0]);
        result.putFloat("BUNDLE_EXITANIM_SCALETOY", scaleTo[1]);
        float[] pivotTo = HwMagicWinAnimation.computePivotForAppExit(vRect, iconWidth, iconHeight, (float[]) params.get(1));
        result.putFloat("BUNDLE_EXITANIM_PIVOTX", pivotTo[0]);
        result.putFloat("BUNDLE_EXITANIM_PIVOTY", pivotTo[1]);
    }

    public /* synthetic */ void lambda$new$9$HwMagicWinWmsPolicy(List params, Bundle result) {
        Rect rect = (Rect) params.get(0);
        if (2 == this.mService.getBoundsPosition(rect)) {
            result.putBoolean("BUNDLE_ISRIGHT_INMW", true);
        }
        if (1 == this.mService.getBoundsPosition(rect)) {
            result.putBoolean("RESULT_ISLEFT_INMW", true);
        }
    }

    public /* synthetic */ void lambda$new$10$HwMagicWinWmsPolicy(List params, Bundle result) {
        int flags = ((Integer) params.get(0)).intValue();
        String pkg = (String) params.get(1);
        if (this.mService.isAppSupportMagicWin(pkg) && !this.mService.getConfig().isShowStatusBar(pkg)) {
            result.putInt("enableStatusBar", (flags | -2147482624) & -2049);
        }
    }

    public /* synthetic */ void lambda$new$11$HwMagicWinWmsPolicy(List params, Bundle result) {
        boolean needSync = false;
        int fromX = ((Integer) params.get(0)).intValue();
        int toX = ((Integer) params.get(1)).intValue();
        WindowState windowState = (WindowState) params.get(2);
        int middleLeft = this.mService.getBounds(3, "").left;
        int leftLeft = this.mService.getBounds(1, "").left;
        int rightLeft = this.mService.getBounds(2, "").left;
        ActivityRecord ar = this.mService.getAmsPolicy().getTopActivity();
        boolean isFinishedRightinAnAn = true;
        if (!(windowState.mAppToken == null || ar == null || ar.mAppWindowToken == null)) {
            isFinishedRightinAnAn = windowState.mAppToken.appToken != ar.mAppWindowToken.appToken;
        }
        if (middleLeft - leftLeft == fromX - toX || rightLeft - leftLeft == fromX - toX || (leftLeft - rightLeft == fromX - toX && isFinishedRightinAnAn)) {
            needSync = true;
        }
        if (this.mService.getAmsPolicy().mFullScreenBounds.equals(windowState.getBounds())) {
            needSync = false;
        }
        if (ar != null && (ar instanceof HwActivityRecord) && ((HwActivityRecord) ar).mIsFullScreenVideoInLandscape) {
            needSync = false;
        }
        result.putBoolean("IS_NEED_SYNC", needSync);
    }

    public /* synthetic */ void lambda$new$12$HwMagicWinWmsPolicy(List params, Bundle result) {
        synchronized (this) {
            if (!ActivityRecord.forToken((IBinder) params.get(0)).inHwMagicWindowingMode()) {
                this.mService.getUIController().dismissDialog();
                return;
            }
            String packageName = (String) params.get(1);
            if (!TextUtils.isEmpty(packageName)) {
                if (!packageName.equals(PACKAGE_INSTALLER_NAME)) {
                    this.mService.getUIController().whetherShowDialog(packageName);
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$13$HwMagicWinWmsPolicy(List params, Bundle result) {
        Rect[] windowFrame = (Rect[]) params.get(1);
        if (windowFrame.length == 5) {
            adjustWindowFrame((WindowState) params.get(0), windowFrame, (WindowState) params.get(2), ((Boolean) params.get(3)).booleanValue());
        }
    }

    private void adjustWindowFrame(WindowState win, Rect[] windowFrame, WindowState naviBar, boolean isNaviBarMini) {
        float ratio;
        int sysui;
        Rect df = windowFrame[0];
        Rect pf = windowFrame[1];
        Rect cf = windowFrame[2];
        Rect vf = windowFrame[3];
        Rect sf = windowFrame[4];
        Rect bounds = new Rect(win.getBounds());
        if (!win.getAttrs().getTitle().equals("MagicWindowGuideDialog")) {
            float ratio2 = win.mMwScaleEnabled ? win.mMwScaleRatioConfig : 1.0f;
            int bottom = (int) (((float) bounds.top) + (((float) (naviBar.getFrameLw().top - bounds.top)) / ratio2) + 0.5f);
            int position = this.mService.getBoundsPosition(bounds);
            int sysui2 = win.getAttrs().systemUiVisibility | win.getAttrs().subtreeSystemUiVisibility;
            boolean isLayoutInStable = (sysui2 & 256) != 0;
            boolean isHideNaviBar = (sysui2 & 2) != 0;
            boolean isOnBottom = isNaviBarOnBottom(naviBar);
            boolean isNaviVisible = naviBar.isVisibleLw();
            if (position == 5) {
                boolean isNotInstallerWindow = !PACKAGE_INSTALLER_NAME.equals(win.getAttrs().packageName);
                if (isOnBottom) {
                    int stableBottom = bounds.bottom - naviBar.getFrameLw().height();
                    if (((sysui2 & 512) != 0 || isHideNaviBar) && win.getAttrs().type >= 1 && win.getAttrs().type <= 1999) {
                        int i = bounds.bottom;
                        df.bottom = i;
                        pf.bottom = i;
                    } else {
                        int i2 = isNaviVisible ? stableBottom : bounds.bottom;
                        df.bottom = i2;
                        pf.bottom = i2;
                    }
                    int i3 = bounds.top;
                    vf.top = i3;
                    cf.top = i3;
                    df.top = i3;
                    pf.top = i3;
                    int i4 = (!isNotInstallerWindow || !isNaviVisible) ? bounds.bottom : stableBottom;
                    vf.bottom = i4;
                    cf.bottom = i4;
                    if (!isNaviVisible && !isNaviBarMini && isLayoutInStable) {
                        cf.bottom = stableBottom;
                    }
                    if ((win.getAttrs().flags & 512) != 0) {
                        int i5 = bounds.bottom;
                        vf.bottom = i5;
                        cf.bottom = i5;
                        df.bottom = i5;
                    }
                } else {
                    int stableRight = bounds.right - naviBar.getFrameLw().width();
                    if (win.getAttrs().type == 1000) {
                        int i6 = isNaviVisible ? stableRight : bounds.right;
                        df.right = i6;
                        pf.right = i6;
                    } else {
                        int i7 = bounds.right;
                        df.right = i7;
                        pf.right = i7;
                    }
                    int i8 = (!isNotInstallerWindow || !isNaviVisible) ? bounds.right : stableRight;
                    vf.right = i8;
                    cf.right = i8;
                    if ((win.getAttrs().flags & 512) != 0) {
                        int i9 = bounds.right;
                        vf.right = i9;
                        cf.right = i9;
                        df.right = i9;
                        pf.right = i9;
                    }
                }
                sysui = sysui2;
                ratio = ratio2;
            } else {
                boolean isShowStatusBar = this.mService.getConfig().isShowStatusBar(win.getAttrs().packageName);
                int cfVfTop = bounds.top;
                if (this.mService.getConfig().isNotchModeEnabled(win.getAttrs().packageName) || isShowStatusBar) {
                    cfVfTop = (int) (((float) bounds.top) + (((float) (this.mWms.mContext.getResources().getDimensionPixelSize(17105443) - bounds.top)) / ratio2));
                }
                int right = bounds.left + ((int) (((float) (naviBar.getFrameLw().left - bounds.left)) / ratio2));
                if (!isNaviVisible) {
                    sysui = sysui2;
                    ratio = ratio2;
                    df.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                    pf.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                    cf.set(bounds.left, cfVfTop, bounds.right, (isNaviBarMini || !isLayoutInStable || !isHideNaviBar || !isOnBottom) ? bounds.bottom : bottom);
                    vf.set(bounds.left, cfVfTop, bounds.right, bounds.bottom);
                    sf.set(bounds.left, bounds.top, (isOnBottom || isNaviBarMini) ? bounds.right : right, (!isOnBottom || isNaviBarMini) ? bounds.bottom : bottom);
                } else if (isOnBottom) {
                    sysui = sysui2;
                    ratio = ratio2;
                    df.set(bounds.left, bounds.top, bounds.right, (win.isChildWindow() ? win.getTopParentWindow() : win).getAttrs().getTitle().toString().contains(BYTEDANCE_PUBLISHER_WINDOW_NAME) ? bounds.bottom : bottom);
                    pf.set(bounds.left, bounds.top, bounds.right, bottom);
                    cf.set(bounds.left, cfVfTop, bounds.right, bottom);
                    vf.set(bounds.left, cfVfTop, bounds.right, bottom);
                    sf.set(bounds.left, bounds.top, bounds.right, bottom);
                } else {
                    sysui = sysui2;
                    ratio = ratio2;
                    if (position == 2) {
                        df.set(bounds.left, bounds.top, right, bounds.bottom);
                        pf.set(bounds.left, bounds.top, right, bounds.bottom);
                        cf.set(bounds.left, cfVfTop, right, bounds.bottom);
                        vf.set(bounds.left, cfVfTop, right, bounds.bottom);
                        sf.set(bounds.left, bounds.top, right, bounds.bottom);
                    } else {
                        df.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                        pf.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                        cf.set(bounds.left, cfVfTop, bounds.right, bounds.bottom);
                        vf.set(bounds.left, cfVfTop, bounds.right, bounds.bottom);
                        sf.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                    }
                }
            }
            WindowState ime = this.mWms.getRoot().getCurrentInputMethodWindow();
            if (ime != null && ime.isVisibleLw() && isInputMethodTarget(win)) {
                int top = (ime.getDisplayFrameLw().top > ime.getContentFrameLw().top ? ime.getDisplayFrameLw() : ime.getContentFrameLw()).top + ime.getGivenContentInsetsLw().top;
                int adjustBottom = position == 5 ? top : (int) ((((float) (top - bounds.top)) / ratio) + ((float) bounds.top) + 0.5f);
                boolean isLayoutFullScreen = (sysui & 1024) != 0;
                boolean isLightStatusBar = (sysui & 8192) != 0;
                boolean isLightNaviBar = (sysui & 16) != 0;
                if ((win.getAttrs().flags & 65792) == 0) {
                    vf.bottom = adjustBottom;
                    cf.bottom = adjustBottom;
                    df.bottom = adjustBottom;
                    pf.bottom = adjustBottom;
                } else if ((win.mOriginAttrs != null && (win.mOriginAttrs.flags & 1024) != 0) || (win.getAttrs().softInputMode & 240) != 16) {
                    vf.bottom = adjustBottom;
                } else if (!isLayoutFullScreen || !isLightStatusBar || !isLightNaviBar) {
                    vf.bottom = adjustBottom;
                    cf.bottom = adjustBottom;
                } else {
                    vf.bottom = adjustBottom;
                    cf.bottom = adjustBottom;
                    df.bottom = adjustBottom;
                    pf.bottom = adjustBottom;
                }
                if (win.getParentWindow() != null && (win.getParentWindow().getAttrs().softInputMode & 240) != 48) {
                    win.getParentWindow().getVisibleFrameLw().bottom = adjustBottom;
                    win.getParentWindow().getVisibleInsets().bottom = win.getParentWindow().getFrameLw().bottom - adjustBottom;
                }
            }
        } else if (!isNaviBarOnBottom(naviBar)) {
            int i10 = bounds.right;
            df.right = i10;
            pf.right = i10;
        }
    }

    private boolean isNaviBarOnBottom(WindowState naviBar) {
        return naviBar.getFrameLw().width() >= naviBar.getFrameLw().height();
    }

    private boolean isInputMethodTarget(WindowState win) {
        TaskStack imeTargetStack = this.mWms.getImeFocusStackLocked();
        WindowState target = this.mWms.getRoot().getDisplayContent(0).mInputMethodTarget;
        if (win == target || (imeTargetStack != null && imeTargetStack.hasChild(win.getTask()) && target != null && !target.inHwMagicWindowingMode())) {
            return true;
        }
        return false;
    }

    public int getOrientation() {
        if (this.mUserRotationMode != 1) {
            return 4;
        }
        int i = this.mUserRotation;
        if (i == 0 || i == 2) {
            return -3;
        }
        return -1;
    }

    public void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        int i = 0;
        this.mUserRotation = Settings.System.getIntForUser(resolver, "user_rotation", 0, -2);
        if (Settings.System.getIntForUser(resolver, "accelerometer_rotation", 0, -2) == 0) {
            i = 1;
        }
        this.mUserRotationMode = i;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        /* access modifiers changed from: package-private */
        public void observe() {
            ContentResolver resolver = HwMagicWinWmsPolicy.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation"), false, this, -1);
            HwMagicWinWmsPolicy.this.updateSettings();
        }

        public void onChange(boolean selfChange) {
            HwMagicWinWmsPolicy.this.updateSettings();
        }
    }

    public boolean getAllDrawnByActivity(IBinder binder) {
        AppWindowToken wtoken = this.mWms.getRoot().getAppWindowToken(binder);
        if (wtoken != null) {
            if ((wtoken.allDrawn && !wtoken.isRelaunching()) || wtoken.startingDisplayed || wtoken.startingMoved) {
                return true;
            }
            return false;
        }
        return true;
    }

    public void startSplitAnimation(IBinder token, String packageName) {
        AppWindowToken appToken;
        if (token != null && packageName != null && this.mService != null && (appToken = this.mWms.mRoot.getAppWindowToken(token)) != null) {
            Rect startBounds = appToken.getTask().getBounds();
            Rect endBounds = new Rect(this.mService.getBounds(2, packageName));
            this.mService.getConfig().adjustSplitBound(2, endBounds);
            new HwMagicWinSplitAnimation.SplitScreenAnimation().startSplitScreenAnimation(appToken, this.mMagicWinAnimation.getSplitAnimation(startBounds, endBounds, appToken.mDisplayContent.getDisplayInfo()), false, 0.0f);
        }
    }

    public void startExitSplitAnimation(IBinder token, float cornerRadius) {
        AppWindowToken appToken;
        if (token != null && (appToken = this.mWms.mRoot.getAppWindowToken(token)) != null) {
            Rect startBounds = appToken.getTask().getBounds();
            DisplayInfo displayInfo = appToken.mDisplayContent.getDisplayInfo();
            new HwMagicWinSplitAnimation.SplitScreenAnimation().startSplitScreenAnimation(appToken, this.mMagicWinAnimation.getSplitAnimation(startBounds, new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight), displayInfo), true, cornerRadius);
        }
    }

    public void startMoveAnimation(IBinder enterToken, IBinder exitToken, String packageName, boolean isAdjust) {
        if (enterToken != null && exitToken != null && packageName != null) {
            AppWindowToken enterAppToken = this.mWms.mRoot.getAppWindowToken(enterToken);
            AppWindowToken exitAppToken = this.mWms.mRoot.getAppWindowToken(exitToken);
            if (enterAppToken != null && exitAppToken != null && this.mService != null) {
                Rect startBounds = enterAppToken.getTask().getBounds();
                Rect endBounds = new Rect(this.mService.getBounds(2, packageName));
                if (isAdjust) {
                    this.mService.getConfig().adjustSplitBound(2, endBounds);
                }
                DisplayInfo displayInfo = enterAppToken.mDisplayContent.getDisplayInfo();
                new HwMagicWinSplitAnimation.SplitScreenAnimation().startMultiTaskAnimation(enterAppToken.getTask(), exitAppToken.getTask(), this.mMagicWinAnimation.getSplitAnimation(startBounds, endBounds, displayInfo), this.mMagicWinAnimation.getExitTaskAnimation(endBounds, displayInfo), !HwMwUtils.IS_FOLD_SCREEN_DEVICE);
            }
        }
    }

    public void startMoveAnimationFullScreen(IBinder enterToken, IBinder exitToken) {
        if (enterToken != null && exitToken != null) {
            AppWindowToken enterAppToken = this.mWms.mRoot.getAppWindowToken(enterToken);
            AppWindowToken exitAppToken = this.mWms.mRoot.getAppWindowToken(exitToken);
            if (enterAppToken != null && exitAppToken != null) {
                Rect startBounds = enterAppToken.getTask().getBounds();
                DisplayInfo displayInfo = enterAppToken.mDisplayContent.getDisplayInfo();
                Rect endBounds = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
                new HwMagicWinSplitAnimation.SplitScreenAnimation().startMultiTaskAnimation(enterAppToken.getTask(), exitAppToken.getTask(), this.mMagicWinAnimation.getSplitAnimation(startBounds, endBounds, displayInfo), this.mMagicWinAnimation.getExitTaskAnimation(endBounds, displayInfo), true);
            }
        }
    }
}
