package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Flog;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.PrintWriter;
import java.util.List;

public class QuickSlideOutController extends QuickStartupStub {
    private static final String ACTION_STATUSBAR_CHANGE = "com.android.systemui.statusbar.visible.change";
    private static final String ASSISTANT_ACTION = "com.huawei.action.VOICE_ASSISTANT";
    private static final String ASSISTANT_ACTIVITY_NAME = "com.huawei.vassistant.ui.main.VAssistantActivity";
    private static final String ASSISTANT_ICON_METADATA_NAME = "com.android.systemui.action_assist_icon";
    private static final String GOOGLE_ASSISTANT_PACKAGE_NAME = "com.google.android.googlequicksearchbox";
    private static final String HW_ASSISTANT_PACKAGE_NAME = "com.huawei.vassistant";
    private static final String KEY_INVOKE = "invoke";
    private static final String KYE_VDRIVE_IS_RUN = "vdrive_is_run_state";
    private static final int MSG_CLOSE_VIEW = 1;
    private static final int MSG_UPDATE_WINDOW_VIEW = 2;
    private static final String SCANNER_CLASS_NAME = "com.huawei.scanner.view.ScannerActivity";
    private static final String SCANNER_PACKAGE_NAME = "com.huawei.scanner";
    private static final String VALUE_INVOKE = "gesture_nav";
    private static final int VALUE_VDRIVE_IS_RUN = 1;
    private static final int VALUE_VDRIVE_IS_UNRUN = 0;
    private static ComponentName sVAssistantComponentName = null;
    private AssistUtils mAssistUtils;
    private float mCurrentTouch;
    private boolean mGestureNavReady;
    private Handler mHandler = new MyHandler(this, null);
    private boolean mInDriveMode = false;
    private boolean mIsStatusBarExplaned = false;
    private final Object mLock = new Object();
    private SettingsObserver mSettingsObserver;
    private int mSlideMaxDistance;
    private boolean mSlideOutEnabled;
    private boolean mSlideOverThreshold;
    private float mSlidePhasePos;
    private int mSlideStartThreshold;
    private boolean mSlidingOnLeft = true;
    private boolean mSlowAnimTriggered;
    private float mStartTouch;
    private StatusBarStatesChangedReceiver mStatusBarReceiver;
    private final Runnable mSuccessRunnable = new Runnable() {
        public void run() {
            QuickSlideOutController.this.gestureSuccessAtEnd(false);
        }
    };
    private boolean mThresholdTriggered;
    private SlideOutContainer mViewContainer;
    private WindowManager mWindowManager;
    private boolean mWindowViewSetuped;

    private final class MyHandler extends Handler {
        private MyHandler() {
        }

        /* synthetic */ MyHandler(QuickSlideOutController x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    QuickSlideOutController.this.hideSlideOutView();
                    return;
                case 2:
                    QuickSlideOutController.this.updateSlideOutWindow();
                    return;
                default:
                    return;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            QuickSlideOutController.this.updateSettings();
        }
    }

    private final class StatusBarStatesChangedReceiver extends BroadcastReceiver {
        private StatusBarStatesChangedReceiver() {
        }

        /* synthetic */ StatusBarStatesChangedReceiver(QuickSlideOutController x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && QuickSlideOutController.ACTION_STATUSBAR_CHANGE.equals(intent.getAction())) {
                String visible = "false";
                if (intent.getExtras() != null) {
                    visible = intent.getExtras().getString("visible");
                }
                QuickSlideOutController.this.mIsStatusBarExplaned = Boolean.valueOf(visible).booleanValue();
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavConst.TAG_GESTURE_QSO;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mIsStatusBarExplaned:");
                    stringBuilder.append(QuickSlideOutController.this.mIsStatusBarExplaned);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private class TouchOutsideListener implements OnTouchListener {
        private int mMsg;

        public TouchOutsideListener(int msg) {
            this.mMsg = msg;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && action != 0) {
                return false;
            }
            QuickSlideOutController.this.mHandler.removeMessages(this.mMsg);
            QuickSlideOutController.this.mHandler.sendEmptyMessage(this.mMsg);
            return true;
        }
    }

    public QuickSlideOutController(Context context, Looper looper) {
        super(context);
        this.mAssistUtils = new AssistUtils(context);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private void notifyStart() {
        this.mGestureNavReady = true;
        this.mStatusBarReceiver = new StatusBarStatesChangedReceiver(this, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STATUSBAR_CHANGE);
        this.mContext.registerReceiver(this.mStatusBarReceiver, filter, "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM", this.mHandler);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION_ASSISTANT), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Global.getUriFor(KYE_VDRIVE_IS_RUN), false, this.mSettingsObserver, -1);
        updateSettings();
        updateConfig();
    }

    private void notifyStop() {
        if (this.mStatusBarReceiver != null) {
            this.mContext.unregisterReceiver(this.mStatusBarReceiver);
            this.mStatusBarReceiver = null;
        }
        if (this.mSettingsObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            this.mSettingsObserver = null;
        }
        this.mGestureNavReady = false;
    }

    private boolean isSuperPowerSaveMode() {
        return GestureUtils.isSuperPowerSaveMode();
    }

    private boolean isInLockTaskMode() {
        return GestureUtils.isInLockTaskMode();
    }

    private boolean isTargetLaunched(boolean onLeft) {
        String focusApp = this.mDeviceStateController.getFocusPackageName();
        if (focusApp == null) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLeft:");
            stringBuilder.append(onLeft);
            stringBuilder.append(", focusApp:");
            stringBuilder.append(focusApp);
            Log.d(str, stringBuilder.toString());
        }
        if (!GestureNavConst.CHINA_REGION) {
            return focusApp.equals(GOOGLE_ASSISTANT_PACKAGE_NAME);
        }
        if (onLeft) {
            return focusApp.equals(HW_ASSISTANT_PACKAGE_NAME);
        }
        return focusApp.equals(SCANNER_PACKAGE_NAME);
    }

    private void updateSlideOutWindow() {
        synchronized (this.mLock) {
            if (this.mSlideOutEnabled && this.mGestureNavReady) {
                if (this.mWindowViewSetuped) {
                    updateSlideOutView();
                } else {
                    createSlideOutView();
                }
            } else if (this.mWindowViewSetuped) {
                destroySlideOutView();
            }
        }
    }

    public boolean isSlideOutEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSlideOutEnabled;
        }
        return z;
    }

    public void onNavCreate(GestureNavView navView) {
        super.onNavCreate(navView);
        notifyStart();
        updateSlideOutWindow();
    }

    public void onNavUpdate() {
        super.onNavUpdate();
        updateSlideOutWindow();
    }

    public void onNavDestroy() {
        super.onNavDestroy();
        notifyStop();
        updateSlideOutWindow();
    }

    public void updateConfig() {
        this.mSlideStartThreshold = this.mContext.getResources().getDimensionPixelSize(34472405);
        this.mSlideMaxDistance = this.mContext.getResources().getDimensionPixelSize(34472404);
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("threshold=");
            stringBuilder.append(this.mSlideStartThreshold);
            stringBuilder.append(", max=");
            stringBuilder.append(this.mSlideMaxDistance);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void updateSettings() {
        synchronized (this.mLock) {
            boolean lastSlideOutEnabled = this.mSlideOutEnabled;
            ContentResolver resolver = this.mContext.getContentResolver();
            this.mSlideOutEnabled = GestureNavConst.isSlideOutEnabled(this.mContext, -2);
            boolean z = false;
            if (Global.getInt(resolver, KYE_VDRIVE_IS_RUN, 0) == 1) {
                z = true;
            }
            this.mInDriveMode = z;
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_QSO;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enabled=");
                stringBuilder.append(this.mSlideOutEnabled);
                stringBuilder.append(", driveMode=");
                stringBuilder.append(this.mInDriveMode);
                Log.i(str, stringBuilder.toString());
            }
            if (this.mSlideOutEnabled != lastSlideOutEnabled) {
                this.mHandler.sendEmptyMessage(2);
            }
        }
    }

    public boolean isPreConditionNotReady(boolean onLeft) {
        if (!isSuperPowerSaveMode() && !this.mIsStatusBarExplaned && !this.mInDriveMode && !isInLockTaskMode() && !this.mDeviceStateController.isKeyguardLocked() && !isTargetLaunched(onLeft)) {
            return false;
        }
        String str = GestureNavConst.TAG_GESTURE_QSO;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StatusBarExplaned:");
        stringBuilder.append(this.mIsStatusBarExplaned);
        stringBuilder.append(",inDriveMode:");
        stringBuilder.append(this.mInDriveMode);
        Log.i(str, stringBuilder.toString());
        if (onLeft) {
            Flog.bdReport(this.mContext, 850, GestureNavConst.REPORT_FAILURE);
        } else {
            Flog.bdReport(this.mContext, 851, GestureNavConst.REPORT_FAILURE);
        }
        return true;
    }

    public void setSlidingSide(boolean onLeft) {
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSlidingSide onLeft:");
            stringBuilder.append(onLeft);
            Log.d(str, stringBuilder.toString());
        }
        this.mSlidingOnLeft = onLeft;
        if (this.mViewContainer != null) {
            this.mViewContainer.setSlidingSide(onLeft);
        }
    }

    public void handleTouchEvent(MotionEvent event) {
        if (this.mViewContainer != null) {
            switch (event.getActionMasked()) {
                case 0:
                    handleActionDown(event);
                    break;
                case 1:
                case 3:
                    handleActionUp(event);
                    break;
                case 2:
                    handleActionMove(event);
                    break;
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        super.resetAtDown();
        this.mViewContainer.reset();
        this.mThresholdTriggered = false;
        this.mSlideOverThreshold = false;
        this.mSlowAnimTriggered = false;
        float y = event.getY();
        this.mCurrentTouch = y;
        this.mStartTouch = y;
    }

    private void handleActionMove(MotionEvent event) {
        this.mCurrentTouch = event.getY();
        if (this.mGestureReallyStarted && !this.mViewContainer.isShowing()) {
            showOrb();
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavConst.TAG_GESTURE_QSO, "start showOrb");
            }
        }
        if (!this.mSlowAnimTriggered && (GestureNavConst.USE_ANIM_LEGACY ? !this.mGestureReallyStarted : !this.mGestureSlowProcessStarted)) {
            this.mSlowAnimTriggered = true;
            notifyAnimStarted();
        }
        if (!this.mThresholdTriggered && this.mViewContainer.isVisible() && (slideOverThreshold(false) || !this.mViewContainer.isAnimationRunning())) {
            this.mThresholdTriggered = true;
            this.mSlidePhasePos = this.mCurrentTouch;
            if (GestureNavConst.DEBUG) {
                String str = GestureNavConst.TAG_GESTURE_QSO;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("slide over threshold, slidePos=");
                stringBuilder.append(this.mSlidePhasePos);
                Log.d(str, stringBuilder.toString());
            }
        }
        if (this.mThresholdTriggered && this.mSlowAnimTriggered) {
            this.mSlideOverThreshold = slideOverThreshold(false);
            this.mViewContainer.setSlideOverThreshold(this.mSlideOverThreshold);
            float offset = this.mSlidePhasePos - this.mCurrentTouch;
            if (offset < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                offset = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            }
            this.mViewContainer.setSlideDistance(offset, MathUtils.constrain(offset, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, (float) this.mSlideMaxDistance) / ((float) this.mSlideMaxDistance));
        }
    }

    private void handleActionUp(MotionEvent event) {
        int reportType;
        this.mCurrentTouch = event.getY();
        this.mSlideOverThreshold = slideOverThreshold(true);
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("slideOver=");
            stringBuilder.append(this.mSlideOverThreshold);
            stringBuilder.append(", valid=");
            stringBuilder.append(this.mIsValidGuesture);
            stringBuilder.append(", fast=");
            stringBuilder.append(this.mIsFastSlideGesture);
            Log.i(str, stringBuilder.toString());
        }
        if (this.mSlidingOnLeft) {
            reportType = 850;
        } else {
            reportType = 851;
        }
        if (this.mIsValidGuesture && this.mSlideOverThreshold) {
            performSlideEndAction(this.mIsFastSlideGesture);
            Flog.bdReport(this.mContext, reportType, GestureNavConst.REPORT_SUCCESS);
            return;
        }
        this.mViewContainer.startExitAnimation(false, false);
        Flog.bdReport(this.mContext, reportType, GestureNavConst.REPORT_FAILURE);
    }

    private boolean slideOverThreshold(boolean checkAtEnd) {
        boolean z = true;
        if (!GestureNavConst.USE_ANIM_LEGACY && !checkAtEnd) {
            return true;
        }
        if (Math.abs(this.mStartTouch - this.mCurrentTouch) <= ((float) this.mSlideStartThreshold)) {
            z = false;
        }
        return z;
    }

    private void performSlideEndAction(boolean isFastSlide) {
        if (this.mViewContainer.isAnimationRunning()) {
            if (GestureNavConst.DEBUG) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "preform action until anim finished");
            }
            this.mViewContainer.performOnAnimationFinished(this.mSuccessRunnable);
            return;
        }
        gestureSuccessAtEnd(isFastSlide);
    }

    private void gestureSuccessAtEnd(boolean isFastSlide) {
        if (GestureNavConst.DEBUG) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("execute animation and start target, isFastSlide:");
            stringBuilder.append(isFastSlide);
            Log.i(str, stringBuilder.toString());
        }
        this.mViewContainer.startExitAnimation(isFastSlide, true);
        startTarget();
    }

    private void startTarget() {
        if (!GestureNavConst.CHINA_REGION) {
            startVoiceAssist();
        } else if (this.mSlidingOnLeft) {
            startVoiceAssist();
        } else {
            startScanner();
        }
    }

    private void startVoiceAssist() {
        if (!hasAssist()) {
            Log.e(GestureNavConst.TAG_GESTURE_QSO, "assist not found");
        }
        try {
            StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarManager != null) {
                Log.i(GestureNavConst.TAG_GESTURE_QSO, "startAssist");
                Bundle bundle = new Bundle();
                bundle.putBoolean("isFromGesture", true);
                statusBarManager.startAssist(bundle);
            }
        } catch (Exception exp) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startVoiceAssist error:");
            stringBuilder.append(exp.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private ComponentName getAssistInfo() {
        if (this.mAssistUtils != null) {
            return this.mAssistUtils.getAssistComponentForUser(-2);
        }
        return null;
    }

    private boolean hasAssist() {
        if (GestureNavConst.CHINA_REGION) {
            boolean hasHwAssist = false;
            try {
                this.mContext.getPackageManager().getPackageInfo(HW_ASSISTANT_PACKAGE_NAME, 128);
                hasHwAssist = true;
            } catch (NameNotFoundException e) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "huawei hivoice not found");
                hasHwAssist = false;
            } catch (Exception e2) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "assist is null, return");
                hasHwAssist = false;
            }
            return hasHwAssist;
        } else if (getAssistInfo() != null) {
            return true;
        } else {
            Log.w(GestureNavConst.TAG_GESTURE_QSO, "assist is null, return");
            return false;
        }
    }

    private ComponentName getVoiceInteractorComponentName() {
        if (this.mAssistUtils != null) {
            return this.mAssistUtils.getActiveServiceComponentName();
        }
        return null;
    }

    private void createSlideOutView() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "createSlideOutView");
        }
        this.mViewContainer = (SlideOutContainer) LayoutInflater.from(this.mContext).inflate(34013298, null);
        this.mViewContainer.setOnTouchListener(new TouchOutsideListener(1));
        this.mViewContainer.setVisibility(8);
        GestureUtils.addWindowView(this.mWindowManager, this.mViewContainer, getSlideOutLayoutParams());
        this.mWindowViewSetuped = true;
    }

    private void updateSlideOutView() {
        GestureUtils.updateViewLayout(this.mWindowManager, this.mViewContainer, getSlideOutLayoutParams());
    }

    private void destroySlideOutView() {
        if (GestureNavConst.DEBUG) {
            Log.d(GestureNavConst.TAG_GESTURE_QSO, "destroySlideOutView");
        }
        this.mWindowViewSetuped = false;
        GestureUtils.removeWindowView(this.mWindowManager, this.mViewContainer, true);
        this.mViewContainer = null;
    }

    private void hideSlideOutView() {
        try {
            if (this.mViewContainer != null) {
                this.mViewContainer.hide(true);
            }
        } catch (Exception exp) {
            String str = GestureNavConst.TAG_GESTURE_QSO;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hideSearchPanelView");
            stringBuilder.append(exp.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private LayoutParams getSlideOutLayoutParams() {
        LayoutParams lp = new LayoutParams(HwArbitrationDEFS.MSG_VPN_STATE_OPEN, 8519936, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.width = -1;
        lp.height = -1;
        lp.windowAnimations = 16974590;
        lp.softInputMode = 49;
        lp.setTitle("GestureSildeOut");
        return lp;
    }

    private void showOrb() {
        maybeSwapSearchIcon();
        this.mViewContainer.show(true);
    }

    private void notifyAnimStarted() {
        this.mViewContainer.startEnterAnimation();
    }

    private void maybeSwapSearchIcon() {
        if (GestureNavConst.USE_ANIM_LEGACY) {
            ImageView logo = this.mViewContainer.getMaybeSwapLogo();
            if (logo != null) {
                if (!GestureNavConst.CHINA_REGION) {
                    logo.setImageResource(33751950);
                } else if (!this.mSlidingOnLeft) {
                    logo.setImageResource(33751949);
                } else if (hasAssist()) {
                    replaceDrawable(logo, getVAssistantComponentName(), ASSISTANT_ICON_METADATA_NAME);
                } else {
                    Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(false);
                    if (intent != null) {
                        replaceDrawable(logo, intent.getComponent(), ASSISTANT_ICON_METADATA_NAME);
                    } else {
                        logo.setImageDrawable(null);
                    }
                }
            }
        }
    }

    private void replaceDrawable(ImageView imageView, ComponentName component, String name) {
        replaceDrawable(imageView, component, name, false);
    }

    private void replaceDrawable(ImageView imageView, ComponentName component, String name, boolean isService) {
        String str;
        StringBuilder stringBuilder;
        if (component != null) {
            try {
                Bundle metaData;
                PackageManager packageManager = this.mContext.getPackageManager();
                if (isService) {
                    metaData = packageManager.getServiceInfo(component, 128).metaData;
                } else {
                    metaData = packageManager.getActivityInfo(component, 128).metaData;
                }
                if (metaData != null) {
                    int iconResId = metaData.getInt(name);
                    if (iconResId != 0) {
                        Resources res;
                        if (isService) {
                            res = packageManager.getResourcesForApplication(component.getPackageName());
                        } else {
                            res = packageManager.getResourcesForActivity(component);
                        }
                        imageView.setImageDrawable(res.getDrawable(iconResId));
                    }
                }
            } catch (NameNotFoundException e) {
                str = GestureNavConst.TAG_GESTURE_QSO;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to swap drawable; ");
                stringBuilder.append(component.flattenToShortString());
                stringBuilder.append(" not found");
                Log.w(str, stringBuilder.toString(), e);
            } catch (NotFoundException nfe) {
                str = GestureNavConst.TAG_GESTURE_QSO;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to swap drawable from ");
                stringBuilder.append(component.flattenToShortString());
                Log.w(str, stringBuilder.toString(), nfe);
            }
        }
    }

    private ComponentName getVAssistantComponentName() {
        if (sVAssistantComponentName != null) {
            return sVAssistantComponentName;
        }
        if (GestureNavConst.CHINA_REGION) {
            sVAssistantComponentName = new ComponentName(HW_ASSISTANT_PACKAGE_NAME, ASSISTANT_ACTIVITY_NAME);
            PackageManager packageManager = this.mContext.getPackageManager();
            if (packageManager == null) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "packageManager is null");
                return sVAssistantComponentName;
            }
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(new Intent(ASSISTANT_ACTION), 65536);
            if (resolveInfos == null || resolveInfos.size() == 0) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "resolveInfos is null");
                return sVAssistantComponentName;
            }
            ComponentInfo info = ((ResolveInfo) resolveInfos.get(0)).activityInfo;
            if (info == null) {
                Log.w(GestureNavConst.TAG_GESTURE_QSO, "activityInfo is null");
                return sVAssistantComponentName;
            }
            sVAssistantComponentName = new ComponentName(info.packageName, info.name);
        } else {
            sVAssistantComponentName = getAssistInfo();
        }
        return sVAssistantComponentName;
    }

    private boolean startScanner() {
        if (this.mContext == null) {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "context error");
            return false;
        }
        Intent aiIntent = new Intent();
        aiIntent.setFlags(268435456);
        aiIntent.setFlags(65536);
        aiIntent.setClassName(SCANNER_PACKAGE_NAME, SCANNER_CLASS_NAME);
        aiIntent.putExtra(KEY_INVOKE, VALUE_INVOKE);
        try {
            Log.i(GestureNavConst.TAG_GESTURE_QSO, "start scanner");
            this.mContext.startActivityAsUser(aiIntent, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSidleOutEnabled=");
        stringBuilder.append(this.mSlideOutEnabled);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" inDriveMode=");
        stringBuilder.append(this.mInDriveMode);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" statusBarExplaned=");
        stringBuilder.append(this.mIsStatusBarExplaned);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" isSuperPowerSaveMode=");
        stringBuilder.append(isSuperPowerSaveMode());
        pw.print(stringBuilder.toString());
        pw.println();
    }
}
