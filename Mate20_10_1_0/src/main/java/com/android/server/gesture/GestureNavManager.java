package com.android.server.gesture;

import android.app.ActivityManager;
import android.app.WindowConfiguration;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.CoordinationModeUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.internal.util.DumpUtils;
import com.android.server.Watchdog;
import com.android.server.gesture.DeviceStateController;
import com.android.server.gesture.GestureNavView;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.fsm.HwFoldScreenManager;
import com.huawei.android.util.HwNotchSizeUtil;
import com.huawei.hiai.awareness.AwarenessConstants;
import java.io.PrintWriter;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class GestureNavManager implements GestureNavPolicy, GestureNavView.IGestureEventProxy, Watchdog.Monitor {
    private static final long FOCUS_CHANGED_TIMEOUT = 1500;
    private static final boolean IS_GESTRUE_NAV_THREAD_DISABLED = SystemProperties.getBoolean("ro.config.gesturenavthread.disable", false);
    private static final int MSG_CONFIG_CHANGED = 3;
    private static final int MSG_COORDINATE_STATE_CHANGED = 19;
    private static final int MSG_DEVICE_STATE_CHANGED = 4;
    private static final int MSG_DISPLAY_CUTOUT_MODE_CHANGED = 12;
    private static final int MSG_FOCUS_CHANGED = 5;
    private static final int MSG_GESTURE_NAV_TIPS_CHANGED = 10;
    private static final int MSG_KEYGUARD_STATE_CHANGED = 6;
    private static final int MSG_LOCK_TASK_STATE_CHANGED = 13;
    private static final int MSG_MULTI_WINDOW_CHANGED = 18;
    private static final int MSG_NIGHT_MODE_CHANGED = 14;
    private static final int MSG_NOTCH_DISPLAY_CHANGED = 9;
    private static final int MSG_PREFER_CHANGED = 8;
    private static final int MSG_RELOAD_NAV_GLOBAL_STATE = 2;
    private static final int MSG_ROTATION_CHANGED = 7;
    private static final int MSG_SET_GESTURE_NAV_MODE = 11;
    private static final int MSG_SUB_SCREEN_BRING_TOP_NAV = 17;
    private static final int MSG_SUB_SCREEN_INIT_NAV = 15;
    private static final int MSG_SUB_SCREEN_REMOVE_NAV = 16;
    private static final int MSG_UPDATE_NAV_GLOBAL_STATE = 1;
    private static final int MSG_UPDATE_NAV_HORIZONTAL_SWITCH_STATE = 21;
    private static final int MSG_UPDATE_NAV_REGION = 22;
    private static final int MSG_USER_CHANGED = 20;
    private static final int NIGHT_MODE_DEFAULT = 1;
    private static final int NIGHT_MODE_ON = 2;
    private static final String TAG = "GestureNavManager";
    private int mAppGestureNavBottomMode;
    private int mAppGestureNavLeftMode;
    private int mAppGestureNavRightMode;
    /* access modifiers changed from: private */
    public GestureNavBottomStrategy mBottomStrategy;
    private Context mContext;
    private CoordinateObserver mCoordinateObserver;
    private int mCurrentUserId;
    private DensityObserver mDensityObserver;
    private String mDensityStr;
    private final DeviceStateController.DeviceChangedListener mDeviceChangedCallback = new DeviceStateController.DeviceChangedListener() {
        /* class com.android.server.gesture.GestureNavManager.AnonymousClass1 */

        @Override // com.android.server.gesture.DeviceStateController.DeviceChangedListener
        public void onDeviceProvisionedChanged(boolean isProvisioned) {
            if (GestureNavManager.this.mIsNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Device provisioned changed, provisioned=" + isProvisioned);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        @Override // com.android.server.gesture.DeviceStateController.DeviceChangedListener
        public void onUserSwitched(int newUserId) {
            if (GestureNavManager.this.mIsNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "User switched, newUserId=" + newUserId);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        @Override // com.android.server.gesture.DeviceStateController.DeviceChangedListener
        public void onUserSetupChanged(boolean isSetup) {
            if (GestureNavManager.this.mIsNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "User setup changed, setup=" + isSetup);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        @Override // com.android.server.gesture.DeviceStateController.DeviceChangedListener
        public void onPreferredActivityChanged(boolean isPrefer) {
            if (GestureNavManager.this.mIsNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Preferred activity changed, isPrefer=" + isPrefer);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(8);
            }
        }
    };
    private DeviceStateController mDeviceStateController;
    private Point mDisplaySize = new Point();
    private int mExcludedRegionHeight;
    private int mFocusAppUid;
    private String mFocusPackageName;
    private int mFocusWinNavOptions;
    private String mFocusWindowTitle;
    private GestureDataTracker mGestureDataTracker;
    private GestureNavAnimProxy mGestureNavAnimProxy;
    /* access modifiers changed from: private */
    public GestureNavView mGestureNavBottom;
    /* access modifiers changed from: private */
    public GestureNavView mGestureNavLeft;
    /* access modifiers changed from: private */
    public GestureNavView mGestureNavRight;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private HandlerThread mHandlerThread;
    /* access modifiers changed from: private */
    public boolean mHasGestureNavReady;
    private boolean mHasNotch;
    private int mHoleHeight = 0;
    private String mHomeWindow;
    private boolean mIsCoordinateState = false;
    private boolean mIsDeviceProvisioned;
    private boolean mIsFocusWindowUsingNotch = true;
    private boolean mIsGestureNavEnabled;
    private boolean mIsGestureNavTipsEnabled;
    private boolean mIsInKeyguardMainWindow;
    private boolean mIsInShrinkState;
    private boolean mIsKeyNavEnabled;
    private boolean mIsKeyguardShowing;
    private boolean mIsLandscape;
    private boolean mIsNavBottomEnabled;
    private boolean mIsNavLeftBackEnabled;
    private boolean mIsNavRightBackEnabled;
    /* access modifiers changed from: private */
    public boolean mIsNavStarted;
    private boolean mIsNeedChange2FullHeight = false;
    private boolean mIsNightMode = false;
    private boolean mIsNotchDisplayDisabled;
    private boolean mIsSubScreenEnableGestureNav;
    private boolean mIsUserSetuped;
    /* access modifiers changed from: private */
    public boolean mIsWindowViewSetuped;
    private LauncherStateChangedReceiver mLauncherStateChangedReceiver;
    private GestureNavBaseStrategy mLeftBackStrategy;
    private final Object mLock = new Object();
    private NightModeObserver mNightModeObserver;
    private NotchObserver mNotchObserver;
    private GestureNavBaseStrategy mRightBackStrategy;
    private int mRotation = GestureNavConst.DEFAULT_ROTATION;
    private int mShrinkNavId = 0;
    private GestureNavSubScreenManager mSubScreenGestureNavManager;
    private WindowManager mWindowManager;

    public GestureNavManager(Context context) {
        this.mContext = context;
        if (IS_GESTRUE_NAV_THREAD_DISABLED) {
            Log.w(TAG, "GestureNavManager thread is disabled.");
            return;
        }
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new GestureHandler(this.mHandlerThread.getLooper());
    }

    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void systemReady() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        Watchdog.getInstance().addMonitor(this);
        if (this.mHandler != null) {
            Watchdog.getInstance().addThread(this.mHandler);
        }
        HwGestureNavWhiteConfig.getInstance().init(this.mContext);
        GestureUtils.systemReady();
        this.mHasNotch = GestureUtils.hasNotch();
        this.mGestureDataTracker = GestureDataTracker.getInstance(this.mContext);
        if (this.mHandler != null) {
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION), false, new ContentObserver(new Handler()) {
                /* class com.android.server.gesture.GestureNavManager.AnonymousClass2 */

                public void onChange(boolean isSelfChange) {
                    Log.i(GestureNavManager.TAG, "gesture nav status change");
                    GestureNavManager.this.mHandler.sendEmptyMessage(1);
                }
            }, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.KEY_SECURE_MULTI_WIN), false, new ContentObserver(new Handler()) {
                /* class com.android.server.gesture.GestureNavManager.AnonymousClass3 */

                public void onChange(boolean isSelfChange) {
                    Log.i(GestureNavManager.TAG, "multi win switch status change");
                    GestureNavManager.this.mHandler.sendEmptyMessage(1);
                }
            }, -1);
            registerGuideFinishObserver();
            registerHorizontalSwitchObserver(resolver);
            this.mHandler.sendEmptyMessage(1);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public boolean isKeyNavEnabled() {
        return this.mIsKeyNavEnabled;
    }

    private void registerHorizontalSwitchObserver(ContentResolver resolver) {
        resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_HORIZONTAL_SWITCH), false, new ContentObserver(new Handler()) {
            /* class com.android.server.gesture.GestureNavManager.AnonymousClass4 */

            public void onChange(boolean isSelfChange) {
                Log.i(GestureNavManager.TAG, "horizontal quick switch status change");
                GestureNavManager.this.mHandler.sendEmptyMessage(21);
            }
        }, -1);
    }

    private void registerGuideFinishObserver() {
        if (isShowDockEnabled()) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, new ContentObserver(new Handler()) {
                /* class com.android.server.gesture.GestureNavManager.AnonymousClass5 */

                public void onChange(boolean isSelfChange) {
                    Log.i(GestureNavManager.TAG, "user setup complete status change");
                    if (!GestureNavManager.this.mIsNavStarted && GestureNavManager.this.isShowDockEnabled()) {
                        GestureNavManager.this.mHandler.sendEmptyMessage(1);
                    }
                }
            }, -1);
        }
    }

    private final class DensityObserver extends ContentObserver {
        DensityObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri) {
            if (GestureNavManager.this.mIsNavStarted && GestureNavManager.this.updateDisplayDensity()) {
                GestureNavManager.this.mHandler.sendEmptyMessage(2);
            }
        }
    }

    private final class NotchObserver extends ContentObserver {
        NotchObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri) {
            if (GestureNavManager.this.mIsNavStarted) {
                GestureNavManager.this.mHandler.sendEmptyMessage(9);
            }
        }
    }

    private final class CoordinateObserver extends ContentObserver {
        CoordinateObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri) {
            if (GestureNavManager.this.mIsNavStarted) {
                GestureNavManager.this.mHandler.sendEmptyMessage(19);
            }
        }
    }

    private final class NightModeObserver extends ContentObserver {
        NightModeObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri) {
            if (GestureNavManager.this.mIsNavStarted) {
                Log.d(GestureNavManager.TAG, "NightModeObserver onChange");
                GestureNavManager.this.mHandler.sendEmptyMessage(14);
            }
        }
    }

    private final class GestureNavTipsObserver extends ContentObserver {
        GestureNavTipsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean isSelfChange, Uri uri) {
            if (GestureNavManager.this.mIsNavStarted) {
                GestureNavManager.this.mHandler.sendEmptyMessage(10);
            }
        }
    }

    private final class LauncherStateChangedReceiver extends BroadcastReceiver {
        private LauncherStateChangedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (GestureNavManager.this.mIsNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Launcher state changed, intent=" + intent);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(8);
            }
        }
    }

    private final class GestureHandler extends Handler {
        GestureHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavManager.TAG, "handleMessage before msg=" + msg.what);
            }
            int i = msg.what;
            if (i != 21) {
                switch (i) {
                    case 1:
                        GestureNavManager.this.updateGestureNavGlobalState();
                        break;
                    case 2:
                        GestureNavManager.this.reloadGestureNavGlobalState();
                        break;
                    case 3:
                        GestureNavManager.this.handleConfigChanged();
                        break;
                    case 4:
                        GestureNavManager.this.handleDeviceStateChanged();
                        break;
                    case 5:
                        GestureNavManager.this.handleFocusChanged((FocusWindowState) msg.obj);
                        break;
                    case 6:
                        GestureNavManager gestureNavManager = GestureNavManager.this;
                        boolean z = true;
                        if (msg.arg1 != 1) {
                            z = false;
                        }
                        gestureNavManager.handleKeygaurdStateChanged(z);
                        break;
                    case 7:
                        GestureNavManager.this.handleRotationChanged(msg.arg1);
                        break;
                    case 8:
                        GestureNavManager.this.handlePreferChanged();
                        break;
                    case 9:
                        GestureNavManager.this.handleNotchDisplayChanged();
                        break;
                    case 10:
                        GestureNavManager.this.handleGestureNavTipsChanged();
                        break;
                    case 11:
                        GestureNavManager.this.handleAppGestureNavMode((AppGestureNavMode) msg.obj);
                        break;
                }
            } else {
                GestureNavManager.this.updateHorizontalSwitchState();
            }
            GestureNavManager.this.handleOtherMessage(msg);
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavManager.TAG, "handleMessage after msg=" + msg.what);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleOtherMessage(Message msg) {
        switch (msg.what) {
            case 12:
                handleDisplayCutoutModeChanged();
                return;
            case 13:
                handleLockTaskStateChanged(msg.arg1);
                return;
            case 14:
                handleNightModeChanged();
                return;
            case 15:
                handleSubScreenCreateNavView();
                return;
            case 16:
                handleSubScreenDestoryNavView();
                return;
            case 17:
                handleSubScreenBringTopNavView();
                return;
            case 18:
                handleMultiWindowChanged(msg.arg1);
                return;
            case 19:
                handleCoordinateStateChanged();
                return;
            case 20:
                handleUserChanged();
                return;
            case 21:
            default:
                return;
            case 22:
                boolean z = true;
                if (msg.arg1 != 1) {
                    z = false;
                }
                handleNavRegionChanged(z, msg.arg2);
                return;
        }
    }

    /* access modifiers changed from: private */
    public static final class FocusWindowState {
        public static final FocusWindowState EMPTY_FOCUS = new FocusWindowState(null, 0, null, true, 0);
        public int gestureNavOptions;
        public boolean isUseNotch;
        public String packageName;
        public String title;
        public int uid;

        FocusWindowState(String packageName2, int uid2, String title2, boolean isUseNotch2, int gestureNavOptions2) {
            this.packageName = packageName2;
            this.uid = uid2;
            this.title = title2;
            this.isUseNotch = isUseNotch2;
            this.gestureNavOptions = gestureNavOptions2;
        }
    }

    /* access modifiers changed from: private */
    public static final class AppGestureNavMode {
        public int bottomMode;
        public int leftMode;
        public String packageName;
        public int rightMode;
        public int uid;

        AppGestureNavMode(String packageName2, int uid2, int leftMode2, int rightMode2, int bottomMode2) {
            this.packageName = packageName2;
            this.uid = uid2;
            this.leftMode = leftMode2;
            this.rightMode = rightMode2;
            this.bottomMode = bottomMode2;
        }

        public boolean isFromSameApp(String packageName2, int uid2) {
            return packageName2 != null && packageName2.equals(this.packageName) && uid2 == this.uid;
        }

        public String toString() {
            return "pkg:" + this.packageName + ", uid:" + this.uid + ", left:" + this.leftMode + ", right:" + this.rightMode + ", bottom:" + this.bottomMode;
        }
    }

    private boolean updateEnableStateLocked() {
        boolean isOldStateEnable = this.mIsGestureNavEnabled;
        this.mIsGestureNavEnabled = GestureNavConst.isGestureNavEnabled(this.mContext, -2);
        this.mIsKeyNavEnabled = !this.mIsGestureNavEnabled;
        Log.i(TAG, "GestureNavEnabled=" + this.mIsGestureNavEnabled + ", tipsEnabled=" + this.mIsGestureNavTipsEnabled);
        if (this.mIsGestureNavEnabled != isOldStateEnable) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isShowDockEnabled() {
        return GestureNavConst.isShowDockEnabled(this.mContext, -2);
    }

    /* JADX INFO: Multiple debug info for r3v0 boolean: [D('devStateCtl' com.android.server.gesture.DeviceStateController), D('isNavEnableOld' boolean)] */
    private boolean updateNavEnableStateForHwMultiWinMode() {
        boolean isKeyNavEnabled;
        this.mIsGestureNavEnabled = true;
        try {
            isKeyNavEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION, -2) == 0;
        } catch (Settings.SettingNotFoundException e) {
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "updateNavEnableStateForHwMultiWinMode: setting not found.");
            }
            DeviceStateController devStateCtl = DeviceStateController.getInstance(this.mContext);
            if (!devStateCtl.isDeviceProvisioned() || !devStateCtl.isCurrentUserSetup()) {
                Log.i(TAG, "updateNavEnableStateForHwMultiWinMode: user is setting up.");
                this.mIsGestureNavEnabled = false;
                isKeyNavEnabled = false;
            } else {
                isKeyNavEnabled = true;
            }
        }
        boolean isNavEnableOld = this.mIsKeyNavEnabled;
        this.mIsKeyNavEnabled = isKeyNavEnabled;
        boolean z = this.mIsKeyNavEnabled;
        if (z == isNavEnableOld || z) {
            return false;
        }
        return true;
    }

    private void hideGestureNavBottomViewIfNeed() {
        GestureNavView gestureNavView = this.mGestureNavBottom;
        if (gestureNavView != null && this.mIsKeyNavEnabled) {
            gestureNavView.setVisibility(8);
        }
    }

    /* access modifiers changed from: private */
    public void updateGestureNavGlobalState() {
        synchronized (this.mLock) {
            if (isShowDockEnabled() ? updateNavEnableStateForHwMultiWinMode() : updateEnableStateLocked()) {
                if (this.mIsGestureNavEnabled) {
                    startGestureNavLocked();
                } else {
                    stopGestureNavLocked();
                }
                hideGestureNavBottomViewIfNeed();
            } else {
                Log.i(TAG, "changing maybe quickly, reload to avoid start again");
                reloadGestureNavGlobalStateLocked();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateHorizontalSwitchState() {
        synchronized (this.mLock) {
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateHorizontalSwitch();
            }
        }
    }

    /* access modifiers changed from: private */
    public void reloadGestureNavGlobalState() {
        synchronized (this.mLock) {
            reloadGestureNavGlobalStateLocked();
        }
    }

    private void reloadGestureNavGlobalStateLocked() {
        Log.i(TAG, "force reloadGestureNavGlobalState");
        this.mIsGestureNavEnabled = false;
        this.mIsKeyNavEnabled = false;
        stopGestureNavLocked();
        if (isShowDockEnabled()) {
            updateNavEnableStateForHwMultiWinMode();
        } else {
            updateEnableStateLocked();
        }
        if (this.mIsGestureNavEnabled) {
            startGestureNavLocked();
        }
        hideGestureNavBottomViewIfNeed();
    }

    private void startGestureNavLocked() {
        Log.i(TAG, "startGestureNavLocked");
        this.mIsNavStarted = true;
        resetAppGestureNavModeLocked();
        this.mGestureDataTracker.checkStartTrackerIfNeed();
        updateDisplayDensity();
        updateNotchDisplayStateLocked();
        this.mDeviceStateController = DeviceStateController.getInstance(this.mContext);
        this.mDeviceStateController.addCallback(this.mDeviceChangedCallback);
        updateGestureNavStateLocked(true);
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDensityObserver = new DensityObserver(this.mHandler);
        resolver.registerContentObserver(Settings.Secure.getUriFor("display_density_forced"), false, this.mDensityObserver, -1);
        this.mNotchObserver = new NotchObserver(this.mHandler);
        resolver.registerContentObserver(Settings.Secure.getUriFor("display_notch_status"), false, this.mNotchObserver, -1);
        if (HwFoldScreenManager.isFoldable()) {
            this.mCoordinateObserver = new CoordinateObserver(this.mHandler);
            resolver.registerContentObserver(Settings.Secure.getUriFor(GestureNavConst.FOLD_SCREEN_MODE), false, this.mCoordinateObserver, -1);
        }
        this.mNightModeObserver = new NightModeObserver(this.mHandler);
        resolver.registerContentObserver(Settings.Secure.getUriFor("ui_night_mode"), false, this.mNightModeObserver, -1);
        this.mLauncherStateChangedReceiver = new LauncherStateChangedReceiver();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart("com.huawei.android.launcher", 0);
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mLauncherStateChangedReceiver, UserHandle.ALL, filter, null, this.mHandler);
        handleSubScreenCreateNavView();
    }

    private void stopGestureNavLocked() {
        CoordinateObserver coordinateObserver;
        Log.i(TAG, "stopGestureNavLocked");
        LauncherStateChangedReceiver launcherStateChangedReceiver = this.mLauncherStateChangedReceiver;
        if (launcherStateChangedReceiver != null) {
            this.mContext.unregisterReceiver(launcherStateChangedReceiver);
            this.mLauncherStateChangedReceiver = null;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        DensityObserver densityObserver = this.mDensityObserver;
        if (densityObserver != null) {
            resolver.unregisterContentObserver(densityObserver);
            this.mDensityObserver = null;
        }
        NotchObserver notchObserver = this.mNotchObserver;
        if (notchObserver != null) {
            resolver.unregisterContentObserver(notchObserver);
            this.mNotchObserver = null;
        }
        if (HwFoldScreenManager.isFoldable() && (coordinateObserver = this.mCoordinateObserver) != null) {
            resolver.unregisterContentObserver(coordinateObserver);
            this.mCoordinateObserver = null;
        }
        NightModeObserver nightModeObserver = this.mNightModeObserver;
        if (nightModeObserver != null) {
            resolver.unregisterContentObserver(nightModeObserver);
            this.mNightModeObserver = null;
        }
        updateGestureNavStateLocked(true);
        DeviceStateController deviceStateController = this.mDeviceStateController;
        if (deviceStateController != null) {
            deviceStateController.removeCallback(this.mDeviceChangedCallback);
            this.mDeviceStateController = null;
        }
        this.mIsNavStarted = false;
        handleSubScreenDestoryNavView();
    }

    private void updateGestureNavStateLocked() {
        updateGestureNavStateLocked(false);
    }

    private void updateGestureNavStateLocked(boolean updateDeviceState) {
        if (updateDeviceState) {
            updateDeviceStateLocked();
        }
        updateConfigLocked();
        updateNavWindowLocked();
        updateNavVisibleLocked();
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public boolean isGestureNavStartedNotLocked() {
        return this.mIsNavStarted;
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onUserChanged(int newUserId) {
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "User switched then reInit, newUserId=" + newUserId);
        }
        this.mHandler.sendEmptyMessage(2);
        if (GestureNavConst.SUPPORT_DOCK_TRIGGER) {
            this.mHandler.sendEmptyMessage(20);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onConfigurationChanged() {
        if (this.mIsNavStarted) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "onConfigurationChanged");
            }
            this.mHandler.sendEmptyMessage(3);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onMultiWindowChanged(int state) {
        if (this.mIsNavStarted) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "onMultiWindowChanged, state=" + state);
            }
            this.mHandler.sendMessage(this.mHandler.obtainMessage(18, state, 0));
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onRotationChanged(int rotation) {
        if (this.mIsNavStarted) {
            HwGestureNavWhiteConfig.getInstance().updateRotation(rotation);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(7, rotation, 0));
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onKeyguardShowingChanged(boolean isShowing) {
        if (this.mIsNavStarted) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6, isShowing ? 1 : 0, 0), 300);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public boolean onFocusWindowChanged(WindowManagerPolicy.WindowState lastFocus, WindowManagerPolicy.WindowState newFocus) {
        if (!this.mIsNavStarted) {
            return true;
        }
        if (newFocus == null || newFocus.getAttrs() == null) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, FocusWindowState.EMPTY_FOCUS), FOCUS_CHANGED_TIMEOUT);
            return true;
        }
        this.mHandler.removeMessages(5, FocusWindowState.EMPTY_FOCUS);
        HwGestureNavWhiteConfig.getInstance().updateWindow(newFocus);
        String packageName = newFocus.getOwningPackage();
        int uid = newFocus.getOwningUid();
        String focusWindowTitle = newFocus.getAttrs().getTitle().toString();
        boolean isUseNotch = isUsingNotch(newFocus);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, new FocusWindowState(packageName, uid, focusWindowTitle, isUseNotch, newFocus.getHwGestureNavOptions())));
        return isUseNotch;
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onLayoutInDisplayCutoutModeChanged(WindowManagerPolicy.WindowState win, boolean isOldUsingNotch, boolean isNewUsingNotch) {
        if (this.mIsNavStarted && this.mHasNotch) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "oldUN=" + isOldUsingNotch + ", newUN=" + isNewUsingNotch);
            }
            this.mHandler.sendEmptyMessage(12);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void onLockTaskStateChanged(int lockTaskState) {
        if (this.mIsNavStarted) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(13, lockTaskState, 0));
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void setGestureNavMode(String packageName, int uid, int leftMode, int rightMode, int bottomMode) {
        if (!this.mIsNavStarted) {
            return;
        }
        if (packageName == null) {
            Log.i(TAG, "packageName is null, return");
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(11, new AppGestureNavMode(packageName, uid, leftMode, rightMode, bottomMode)));
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void updateGestureNavRegion(boolean shrink, int navId) {
        if (this.mIsNavStarted) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(22, shrink ? 1 : 0, navId));
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public boolean isPointInExcludedRegion(Point point) {
        if (!this.mIsNavStarted || point.y >= this.mExcludedRegionHeight) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void handleDeviceStateChanged() {
        synchronized (this.mLock) {
            updateDeviceStateLocked();
            updateNavVisibleLocked();
        }
        if (isShowDockEnabled()) {
            String str = this.mHomeWindow;
            setIsInHomeOfLauncher(str != null && str.equals(this.mFocusWindowTitle));
        }
    }

    /* access modifiers changed from: private */
    public void handleConfigChanged() {
        synchronized (this.mLock) {
            if (this.mDeviceStateController != null) {
                this.mDeviceStateController.onConfigurationChanged();
            }
            updateConfigLocked();
            updateNavWindowLocked();
            updateNavVisibleLocked();
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateScreenConfigState(this.mIsLandscape);
            }
            handleConfigChangedSubScreen();
        }
    }

    private void handleCoordinateStateChanged() {
        synchronized (this.mLock) {
            boolean isCoordinateStatus = true;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GestureNavConst.FOLD_SCREEN_MODE, 1, -2) != 4) {
                isCoordinateStatus = false;
            }
            boolean isCoordinateOld = this.mIsCoordinateState;
            this.mIsCoordinateState = isCoordinateStatus;
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "lastState= " + isCoordinateOld + ", curState= " + isCoordinateStatus);
            }
            if (isCoordinateOld != this.mIsCoordinateState) {
                updateConfigLocked();
                updateNavWindowLocked();
            }
        }
    }

    private void handleMultiWindowChanged(int state) {
        if (GestureNavConst.SUPPORT_DOCK_TRIGGER) {
            boolean isNeedChangeFull = false;
            boolean isNeedRefreshConfig = false;
            if (state == 0) {
                isNeedRefreshConfig = isNeedRefreshWinConfig(false, 0);
            } else if (state == 1) {
                isNeedChangeFull = false;
                isNeedRefreshConfig = isNeedRefreshWinConfig(false, 1);
            } else if (state == 2 || state == 3) {
                isNeedChangeFull = isNeedChange2FullHeight(state);
            } else {
                return;
            }
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "handleMultiWindowChanged: isNeedChangeFull=" + isNeedChangeFull);
            }
            if (this.mIsNeedChange2FullHeight == isNeedChangeFull) {
                if (GestureNavConst.DEBUG) {
                    Log.d(TAG, "handleMultiWindowChanged: no need update.");
                }
                if (!isNeedRefreshConfig) {
                    return;
                }
            }
            this.mIsNeedChange2FullHeight = isNeedChangeFull;
            updateConfigLocked();
            updateNavWindowLocked();
            updateNavVisibleLocked();
        }
    }

    private boolean isNeedRefreshWinConfig(boolean isNeedRefreshConfig, int state) {
        if (state != 0) {
            if (state == 1) {
                WindowManagerPolicy.WindowState focusWindowState = this.mDeviceStateController.getFocusWindow();
                if (!(!this.mHasNotch || focusWindowState == null || focusWindowState.getAttrs() == null)) {
                    this.mIsFocusWindowUsingNotch = focusWindowState.isWindowUsingNotch();
                }
            }
        } else if (!GestureNavConst.DEFAULT_DOCK_PACKAGE.equals(this.mFocusPackageName)) {
            this.mIsFocusWindowUsingNotch = false;
        }
        return true;
    }

    private void handleUserChanged() {
        GestureNavBaseStrategy gestureNavBaseStrategy = this.mLeftBackStrategy;
        if (gestureNavBaseStrategy != null) {
            gestureNavBaseStrategy.rmvDockDeathRecipient();
        }
        GestureNavBaseStrategy gestureNavBaseStrategy2 = this.mRightBackStrategy;
        if (gestureNavBaseStrategy2 != null) {
            gestureNavBaseStrategy2.rmvDockDeathRecipient();
        }
        GestureNavBaseStrategy.mDockService = null;
    }

    /* access modifiers changed from: private */
    public void handleRotationChanged(int rotation) {
        synchronized (this.mLock) {
            int lastRotation = this.mRotation;
            this.mRotation = rotation;
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "lastRotation=" + lastRotation + ", currentRotation=" + rotation);
            }
            if (isRotationChangedInLand(lastRotation, rotation)) {
                if (GestureNavConst.SUPPORT_DOCK_TRIGGER) {
                    this.mIsNeedChange2FullHeight = isNeedChange2FullHeight(3);
                }
                updateConfigLocked();
                updateNavWindowLocked();
                updateNavVisibleLocked();
                handleRotationChangedSubScreen(rotation);
            } else {
                handleMultiWindowChanged(3);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePreferChanged() {
        synchronized (this.mLock) {
            if (this.mDeviceStateController != null) {
                if (updateHomeWindowLocked()) {
                    updateNavVisibleLocked();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleNotchDisplayChanged() {
        synchronized (this.mLock) {
            if (updateNotchDisplayStateLocked()) {
                updateConfigLocked();
                updateNavWindowLocked();
                updateNavVisibleLocked();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleGestureNavTipsChanged() {
        synchronized (this.mLock) {
            if (updateGestureNavTipsStateLocked() && this.mBottomStrategy != null) {
                this.mBottomStrategy.updateNavTipsState(this.mIsGestureNavTipsEnabled);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFocusChanged(FocusWindowState focusWindowState) {
        synchronized (this.mLock) {
            resetAppGestureNavModeLocked();
            this.mFocusAppUid = focusWindowState.uid;
            this.mFocusPackageName = focusWindowState.packageName;
            this.mFocusWindowTitle = focusWindowState.title;
            this.mFocusWinNavOptions = focusWindowState.gestureNavOptions;
            this.mIsInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "Focus:" + this.mFocusWindowTitle + ", Uid=" + this.mFocusAppUid + ", UN=" + focusWindowState.isUseNotch + ", LUN=" + this.mIsFocusWindowUsingNotch + ", FNO=" + this.mFocusWinNavOptions + ", IKMW=" + this.mIsInKeyguardMainWindow + ", pkg:" + this.mFocusPackageName);
            }
            boolean updateConfig = false;
            if (this.mIsFocusWindowUsingNotch != focusWindowState.isUseNotch) {
                this.mIsFocusWindowUsingNotch = focusWindowState.isUseNotch;
                if (this.mIsLandscape) {
                    updateConfig = true;
                }
            }
            if (updateShrinkStateLocked()) {
                updateConfig = true;
            }
            if (updateConfig) {
                updateConfigLocked();
                updateNavWindowLocked();
            }
            updateNavVisibleLocked(this.mIsLandscape);
            if (isShowDockEnabled()) {
                setIsInHomeOfLauncher(this.mHomeWindow != null && this.mHomeWindow.equals(this.mFocusWindowTitle));
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleAppGestureNavMode(AppGestureNavMode appGestureNavMode) {
        synchronized (this.mLock) {
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "AppMode:" + appGestureNavMode);
            }
            if (isShowDockEnabled() && GestureNavConst.isLauncher(appGestureNavMode.packageName)) {
                checkIsHomeActivityOfLauncher(appGestureNavMode);
                if (appGestureNavMode.leftMode == 2 && appGestureNavMode.rightMode == 2) {
                    return;
                }
            }
            if (appGestureNavMode.isFromSameApp(this.mFocusPackageName, this.mFocusAppUid)) {
                this.mAppGestureNavLeftMode = appGestureNavMode.leftMode;
                this.mAppGestureNavRightMode = appGestureNavMode.rightMode;
                this.mAppGestureNavBottomMode = appGestureNavMode.bottomMode;
                updateNavVisibleLocked();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleKeygaurdStateChanged(boolean isShowing) {
        synchronized (this.mLock) {
            if (isShowing) {
                resetAppGestureNavModeLocked();
            }
            this.mIsKeyguardShowing = isShowing;
            this.mIsInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "keyguard showing=" + isShowing + ", IKMW=" + this.mIsInKeyguardMainWindow);
            }
            updateNavVisibleLocked();
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateKeyguardState(this.mIsKeyguardShowing);
            }
        }
    }

    private void handleDisplayCutoutModeChanged() {
        synchronized (this.mLock) {
            if (this.mDeviceStateController != null) {
                boolean isChanged = false;
                WindowManagerPolicy.WindowState focusWindowState = this.mDeviceStateController.getFocusWindow();
                if (!(focusWindowState == null || focusWindowState.getAttrs() == null)) {
                    String windowTitle = focusWindowState.getAttrs().getTitle().toString();
                    int uid = focusWindowState.getOwningUid();
                    if ((this.mFocusWindowTitle == null || this.mFocusWindowTitle.equals(windowTitle)) && this.mFocusAppUid == uid) {
                        boolean isUseNotch = isUsingNotch(focusWindowState);
                        if (isUseNotch != this.mIsFocusWindowUsingNotch) {
                            isChanged = true;
                            this.mIsFocusWindowUsingNotch = isUseNotch;
                        }
                        if (GestureNavConst.DEBUG) {
                            Log.i(TAG, "display cutout mode change:" + isChanged + ", UN:" + this.mIsFocusWindowUsingNotch);
                        }
                    } else {
                        return;
                    }
                }
                if (isChanged) {
                    if (this.mIsLandscape) {
                        updateConfigLocked();
                        updateNavWindowLocked();
                    }
                    updateNavVisibleLocked();
                }
            }
        }
    }

    private void handleLockTaskStateChanged(int lockTaskState) {
        synchronized (this.mLock) {
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateLockTaskState(lockTaskState);
            }
        }
    }

    private void handleNavRegionChanged(boolean shrink, int navId) {
        synchronized (this.mLock) {
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "shrink:" + shrink + ", navId:" + navId);
            }
            if (!shrink || !(navId == 1 || navId == 2)) {
                this.mShrinkNavId = 0;
            } else {
                this.mShrinkNavId = navId;
            }
            if (updateShrinkStateLocked()) {
                updateGestureNavStateLocked();
            }
        }
    }

    private void updateDeviceStateLocked() {
        DeviceStateController deviceStateController = this.mDeviceStateController;
        if (deviceStateController != null) {
            this.mCurrentUserId = deviceStateController.getCurrentUser();
            this.mIsDeviceProvisioned = this.mDeviceStateController.isDeviceProvisioned();
            this.mIsUserSetuped = this.mDeviceStateController.isCurrentUserSetup();
            this.mIsKeyguardShowing = this.mDeviceStateController.isKeyguardShowingOrOccluded();
            this.mIsInKeyguardMainWindow = this.mDeviceStateController.isKeyguardShowingAndNotOccluded();
            this.mRotation = this.mDeviceStateController.getCurrentRotation();
            updateHomeWindowLocked();
            WindowManagerPolicy.WindowState focusWindowState = this.mDeviceStateController.getFocusWindow();
            if (!(focusWindowState == null || focusWindowState.getAttrs() == null)) {
                this.mFocusWindowTitle = focusWindowState.getAttrs().getTitle().toString();
                this.mIsFocusWindowUsingNotch = isUsingNotch(focusWindowState);
                this.mFocusPackageName = focusWindowState.getOwningPackage();
                this.mFocusAppUid = focusWindowState.getOwningUid();
                this.mFocusWinNavOptions = focusWindowState.getHwGestureNavOptions();
            }
            this.mShrinkNavId = this.mDeviceStateController.getShrinkIdByDockPosition();
            updateShrinkStateLocked();
            HwGestureNavWhiteConfig.getInstance().updateRotation(this.mRotation);
            HwGestureNavWhiteConfig.getInstance().updateWindow(focusWindowState);
            GestureNavBottomStrategy gestureNavBottomStrategy = this.mBottomStrategy;
            if (gestureNavBottomStrategy != null) {
                gestureNavBottomStrategy.updateKeyguardState(this.mIsKeyguardShowing);
                this.mBottomStrategy.updateScreenConfigState(this.mIsLandscape);
                this.mBottomStrategy.updateNavTipsState(this.mIsGestureNavTipsEnabled);
                this.mBottomStrategy.updateHorizontalSwitch();
            }
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "Update device state, provisioned:" + this.mIsDeviceProvisioned + ", userSetup:" + this.mIsUserSetuped + ", KS:" + this.mIsKeyguardShowing + ", IKMW:" + this.mIsInKeyguardMainWindow + ", focus:" + this.mFocusWindowTitle + ", UN:" + this.mIsFocusWindowUsingNotch + ", home:" + this.mHomeWindow);
            }
        }
    }

    private void updateConfigLocked() {
        boolean isUsingNotch;
        boolean isUsingNotch2;
        if (this.mHasGestureNavReady) {
            boolean z = false;
            this.mIsLandscape = this.mContext.getResources().getConfiguration().orientation == 2;
            this.mExcludedRegionHeight = GestureNavConst.getStatusBarHeight(this.mContext);
            this.mWindowManager.getDefaultDisplay().getRealSize(this.mDisplaySize);
            int displayWidth = this.mDisplaySize.x;
            int displayHeight = this.mDisplaySize.y;
            int backWindowWidth = GestureNavConst.getBackWindowWidth(this.mContext);
            int leftBackWindowHeight = GestureNavConst.getBackWindowHeight(displayHeight, this.mIsInShrinkState && this.mShrinkNavId == 1, this.mExcludedRegionHeight, this.mIsNeedChange2FullHeight);
            int rightBackWindowHeight = GestureNavConst.getBackWindowHeight(displayHeight, this.mIsInShrinkState && this.mShrinkNavId == 2, this.mExcludedRegionHeight, this.mIsNeedChange2FullHeight);
            Point leftBackWindowSize = new Point(backWindowWidth, leftBackWindowHeight);
            Point rightBackWindowSize = new Point(backWindowWidth, rightBackWindowHeight);
            int bottomWindowHeight = GestureNavConst.getBottomWindowHeight(this.mContext);
            if (this.mHasNotch) {
                this.mHoleHeight = HwNotchSizeUtil.getNotchSize()[1];
                if (this.mIsLandscape) {
                    if (!this.mIsNotchDisplayDisabled && this.mIsFocusWindowUsingNotch) {
                        z = true;
                    }
                    isUsingNotch2 = z;
                } else {
                    isUsingNotch2 = true;
                }
                this.mGestureNavLeft.updateViewNotchState(isUsingNotch2);
                this.mGestureNavRight.updateViewNotchState(isUsingNotch2);
                this.mGestureNavAnimProxy.updateViewNotchState(isUsingNotch2);
                isUsingNotch = isUsingNotch2;
            } else {
                isUsingNotch = true;
            }
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "w=" + displayWidth + ", h=" + displayHeight + ", leftH=" + leftBackWindowHeight + ", rightH=" + rightBackWindowHeight + ", backW=" + backWindowWidth + ", bottomH=" + bottomWindowHeight + ", usingNotch=" + isUsingNotch + ", holeH=" + this.mHoleHeight);
            }
            updateViewConfigLocked(this.mDisplaySize, leftBackWindowSize, rightBackWindowSize, bottomWindowHeight, isUsingNotch);
        }
    }

    private boolean isNeedChange2FullHeight(int state) {
        boolean isUnFoldedState = HwFoldScreenManager.isFoldable() && HwFoldScreenManager.getFoldableState() != 2;
        int i = this.mRotation;
        boolean isLandscapeMode = i == 1 || i == 3;
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "isNeedChange2FullHeight: isUnFoldedState=" + isUnFoldedState + "; isLandscapeMode=" + isLandscapeMode);
        }
        if (isUnFoldedState || isLandscapeMode) {
            return false;
        }
        if (state == 0) {
            return true;
        }
        return isHasHwSplitScreen();
    }

    private boolean isHasHwSplitScreen() {
        List<ActivityManager.RunningTaskInfo> visibleTaskInfoList = HwActivityTaskManager.getVisibleTasks();
        if (visibleTaskInfoList == null || visibleTaskInfoList.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo rti : visibleTaskInfoList) {
            if (rti != null && WindowConfiguration.isHwSplitScreenWindowingMode(rti.windowMode)) {
                return true;
            }
        }
        return false;
    }

    private void updateNavWindowLocked() {
        if (this.mIsGestureNavEnabled) {
            if (!this.mIsWindowViewSetuped) {
                createNavWindows();
            } else {
                updateNavWindows();
            }
        } else if (this.mIsWindowViewSetuped) {
            destroyNavWindows();
        }
    }

    private void updateNavVisibleLocked() {
        updateNavVisibleLocked(false);
    }

    private void updateNavVisibleLocked(boolean isDelay) {
        if (this.mIsWindowViewSetuped) {
            boolean isEnableLeftBack = true;
            boolean isEnableRightBack = true;
            boolean isEnableBottom = true;
            if (this.mIsInKeyguardMainWindow) {
                isEnableLeftBack = false;
                isEnableRightBack = false;
                isEnableBottom = false;
            } else {
                if (isFocusWindowLeftBackDisabledLocked()) {
                    isEnableLeftBack = false;
                }
                if (isFocusWindowRightBackDisabledLocked()) {
                    isEnableRightBack = false;
                }
                if (!this.mIsDeviceProvisioned || !this.mIsUserSetuped || isFocusWindowBottomDisabledLocked() || this.mIsKeyNavEnabled) {
                    isEnableBottom = false;
                }
            }
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "updateNavVisible left:" + isEnableLeftBack + ", right:" + isEnableRightBack + ", bottom:" + isEnableBottom);
            }
            enableBackNavLocked(isEnableLeftBack, isEnableRightBack, isDelay);
            enableBottomNavLocked(isEnableBottom, isDelay);
            updateSubScreenNavVisibleLocked(isDelay);
        }
    }

    private void enableBackNavLocked(boolean isEnableLeft, boolean isEnableRight, boolean isDelay) {
        if (this.mIsWindowViewSetuped) {
            boolean isChanged = false;
            if (this.mIsNavLeftBackEnabled != isEnableLeft) {
                this.mGestureNavLeft.show(isEnableLeft, isDelay);
                this.mIsNavLeftBackEnabled = isEnableLeft;
                isChanged = true;
            }
            if (this.mIsNavRightBackEnabled != isEnableRight) {
                this.mGestureNavRight.show(isEnableRight, isDelay);
                this.mIsNavRightBackEnabled = isEnableRight;
                isChanged = true;
            }
            if (isChanged && GestureNavConst.DEBUG) {
                Log.i(TAG, "enableBackNav left:" + this.mIsNavLeftBackEnabled + ", right:" + this.mIsNavRightBackEnabled + ", delay:" + isDelay);
            }
        }
    }

    private void enableBottomNavLocked(boolean isEnable, boolean isDelay) {
        if (this.mIsWindowViewSetuped && this.mIsNavBottomEnabled != isEnable) {
            this.mGestureNavBottom.show(isEnable, false);
            this.mIsNavBottomEnabled = isEnable;
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "enableBottomNav enable:" + this.mIsNavBottomEnabled);
            }
        }
    }

    private void createNavWindows() {
        Log.i(TAG, "createNavWindows");
        this.mGestureNavLeft = new GestureNavView(this.mContext, 1);
        this.mGestureNavRight = new GestureNavView(this.mContext, 2);
        this.mGestureNavBottom = new GestureNavView(this.mContext, 3);
        Looper looper = this.mHandlerThread.getLooper();
        this.mGestureNavAnimProxy = new GestureNavAnimProxy(this.mContext, looper);
        this.mLeftBackStrategy = new GestureNavBackStrategy(1, this.mContext, looper, this.mGestureNavAnimProxy);
        this.mRightBackStrategy = new GestureNavBackStrategy(2, this.mContext, looper, this.mGestureNavAnimProxy);
        this.mBottomStrategy = new GestureNavBottomStrategy(3, this.mContext, looper);
        this.mHasGestureNavReady = true;
        Log.i(TAG, "gesture nav ready.");
        updateConfigLocked();
        configAndAddNavWindow("GestureNavLeft", this.mGestureNavLeft, this.mLeftBackStrategy);
        configAndAddNavWindow("GestureNavRight", this.mGestureNavRight, this.mRightBackStrategy);
        configAndAddNavWindow("GestureNavBottom", this.mGestureNavBottom, this.mBottomStrategy);
        this.mLeftBackStrategy.onNavCreate(this.mGestureNavBottom);
        this.mRightBackStrategy.onNavCreate(this.mGestureNavBottom);
        this.mBottomStrategy.onNavCreate(this.mGestureNavBottom);
        this.mGestureNavAnimProxy.onNavCreate();
        updateNightModeLocked();
        this.mBottomStrategy.updateKeyguardState(this.mIsKeyguardShowing);
        this.mBottomStrategy.updateScreenConfigState(this.mIsLandscape);
        this.mBottomStrategy.updateNavTipsState(this.mIsGestureNavTipsEnabled);
        this.mBottomStrategy.updateHorizontalSwitch();
        this.mIsWindowViewSetuped = true;
        this.mIsNavLeftBackEnabled = true;
        this.mIsNavRightBackEnabled = true;
        this.mIsNavBottomEnabled = true;
    }

    private void updateNavWindows() {
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "updateNavWindows");
        }
        reLayoutNavWindow("GestureNavLeft", this.mGestureNavLeft, this.mLeftBackStrategy);
        reLayoutNavWindow("GestureNavRight", this.mGestureNavRight, this.mRightBackStrategy);
        reLayoutNavWindow("GestureNavBottom", this.mGestureNavBottom, this.mBottomStrategy);
        this.mLeftBackStrategy.onNavUpdate();
        this.mRightBackStrategy.onNavUpdate();
        this.mBottomStrategy.onNavUpdate();
        this.mGestureNavAnimProxy.onNavUpdate();
    }

    private void destroyNavWindows() {
        Log.i(TAG, "destoryNavWindows");
        this.mHasGestureNavReady = false;
        this.mIsWindowViewSetuped = false;
        this.mIsNavLeftBackEnabled = false;
        this.mIsNavRightBackEnabled = false;
        this.mIsNavBottomEnabled = false;
        this.mLeftBackStrategy.onNavDestroy();
        this.mRightBackStrategy.onNavDestroy();
        this.mBottomStrategy.onNavDestroy();
        GestureUtils.removeWindowView(this.mWindowManager, this.mGestureNavLeft, true);
        GestureUtils.removeWindowView(this.mWindowManager, this.mGestureNavRight, true);
        GestureUtils.removeWindowView(this.mWindowManager, this.mGestureNavBottom, true);
        this.mGestureNavAnimProxy.onNavDestroy();
        this.mGestureNavAnimProxy = null;
        this.mLeftBackStrategy = null;
        this.mRightBackStrategy = null;
        this.mBottomStrategy = null;
        this.mGestureNavLeft = null;
        this.mGestureNavRight = null;
        this.mGestureNavBottom = null;
    }

    private WindowManager.LayoutParams createLayoutParams(String title, GestureNavView.WindowConfig config) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(HwArbitrationDEFS.MSG_VPN_STATE_OPEN, 296);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.flags |= 512;
        lp.format = -2;
        lp.alpha = 0.0f;
        lp.gravity = 51;
        lp.x = config.startX;
        lp.y = config.startY;
        lp.width = config.width;
        lp.height = config.height;
        lp.windowAnimations = 0;
        lp.softInputMode = 49;
        lp.setTitle(title);
        if (config.usingNotch) {
            lp.hwFlags |= 65536;
        } else {
            lp.hwFlags &= -65537;
        }
        lp.hwFlags |= 131072;
        lp.hwFlags |= HighBitsCompModeID.MODE_EYE_PROTECT;
        if (!isShowDockEnabled() || !this.mIsKeyNavEnabled) {
            lp.hwFlags &= -262145;
        } else {
            lp.hwFlags |= 262144;
        }
        return lp;
    }

    private void configAndAddNavWindow(String title, GestureNavView view, GestureNavBaseStrategy strategy) {
        GestureNavView.WindowConfig config = view.getViewConfig();
        WindowManager.LayoutParams params = createLayoutParams(title, config);
        strategy.updateConfig(config.displayWidth, config.displayHeight, new Rect(config.locationOnScreenX, config.locationOnScreenY, config.locationOnScreenX + config.width, config.locationOnScreenY + config.height), this.mRotation);
        view.setGestureEventProxy(this);
        GestureUtils.addWindowView(this.mWindowManager, view, params);
    }

    private void reLayoutNavWindow(String title, GestureNavView view, GestureNavBaseStrategy strategy) {
        GestureNavView.WindowConfig config = view.getViewConfig();
        WindowManager.LayoutParams params = createLayoutParams(title, config);
        strategy.updateConfig(config.displayWidth, config.displayHeight, new Rect(config.locationOnScreenX, config.locationOnScreenY, config.locationOnScreenX + config.width, config.locationOnScreenY + config.height), this.mRotation);
        GestureUtils.updateViewLayout(this.mWindowManager, view, params);
    }

    private boolean isInKeyguardMainWindowLocked() {
        if (!this.mIsKeyguardShowing) {
            return false;
        }
        DeviceStateController deviceStateController = this.mDeviceStateController;
        if ((deviceStateController == null || deviceStateController.isKeyguardOccluded()) && !GestureNavConst.STATUSBAR_WINDOW.equals(this.mFocusWindowTitle)) {
            return false;
        }
        return true;
    }

    private int getCoordinateOffset() {
        int coordinateOffset = 0;
        if (this.mIsCoordinateState) {
            coordinateOffset = CoordinationModeUtils.getFoldScreenFullWidth() - CoordinationModeUtils.getFoldScreenMainWidth();
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "coordinateOffset = " + coordinateOffset);
            }
        }
        return coordinateOffset;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0043  */
    private Rect getViewAdaptRect(int rotation) {
        Rect viewAdapt = new Rect(0, 0, 0, 0);
        int gestureNavOffset = GestureNavConst.getGestureCurvedOffset(this.mContext);
        if (GestureUtils.isCurvedSideDisp()) {
            int i = this.mRotation;
            if (i != 0) {
                if (i == 1) {
                    viewAdapt.bottom = GestureUtils.getCurvedSideLeftDisp() + gestureNavOffset;
                } else if (i != 2) {
                    if (i == 3) {
                        viewAdapt.bottom = GestureUtils.getCurvedSideRightDisp() + gestureNavOffset;
                    }
                }
                if (GestureNavConst.DEBUG) {
                    Log.i(TAG, "viewAdapt: " + viewAdapt);
                }
            }
            viewAdapt.left = GestureUtils.getCurvedSideLeftDisp() + gestureNavOffset;
            viewAdapt.right = GestureUtils.getCurvedSideRightDisp() + gestureNavOffset;
            if (GestureNavConst.DEBUG) {
            }
        }
        return viewAdapt;
    }

    private void updateViewConfigLocked(Point displaySize, Point leftBackWindowSize, Point rightBackWindowSize, int bottomWindowHeight, boolean isUsingNotch) {
        int viewWidth;
        int leftViewStartPos = 0;
        int rightViewOffset = 0;
        int displayWidth = displaySize.x;
        if (this.mHasNotch && !isUsingNotch) {
            int i = this.mRotation;
            if (i == 1) {
                leftViewStartPos = this.mHoleHeight;
                rightViewOffset = 0;
                viewWidth = displayWidth - this.mHoleHeight;
            } else if (i == 3) {
                leftViewStartPos = 0;
                rightViewOffset = this.mHoleHeight;
                viewWidth = displayWidth - this.mHoleHeight;
            }
            Rect viewAdapt = getViewAdaptRect(this.mRotation);
            viewAdapt.left += leftBackWindowSize.x;
            viewAdapt.right += rightBackWindowSize.x;
            viewAdapt.bottom += bottomWindowHeight;
            int coordinateOffset = getCoordinateOffset();
            int dispalyHeight = displaySize.y;
            this.mGestureNavLeft.updateViewConfig(displayWidth, dispalyHeight, leftViewStartPos + coordinateOffset, dispalyHeight - leftBackWindowSize.y, viewAdapt.left, leftBackWindowSize.y, leftViewStartPos + coordinateOffset, dispalyHeight - leftBackWindowSize.y);
            this.mGestureNavRight.updateViewConfig(displayWidth, dispalyHeight, (displayWidth - viewAdapt.right) - rightViewOffset, dispalyHeight - rightBackWindowSize.y, viewAdapt.right, rightBackWindowSize.y, (displayWidth - viewAdapt.right) - rightViewOffset, dispalyHeight - rightBackWindowSize.y);
            this.mGestureNavBottom.updateViewConfig(displayWidth, dispalyHeight, leftViewStartPos + coordinateOffset, dispalyHeight - viewAdapt.bottom, viewWidth - coordinateOffset, viewAdapt.bottom, leftViewStartPos + coordinateOffset, dispalyHeight - viewAdapt.bottom);
            this.mGestureNavAnimProxy.updateViewConfig(displayWidth, dispalyHeight, leftViewStartPos + coordinateOffset, 0, viewWidth - coordinateOffset, dispalyHeight, leftViewStartPos + coordinateOffset, 0);
        }
        viewWidth = displayWidth;
        Rect viewAdapt2 = getViewAdaptRect(this.mRotation);
        viewAdapt2.left += leftBackWindowSize.x;
        viewAdapt2.right += rightBackWindowSize.x;
        viewAdapt2.bottom += bottomWindowHeight;
        int coordinateOffset2 = getCoordinateOffset();
        int dispalyHeight2 = displaySize.y;
        this.mGestureNavLeft.updateViewConfig(displayWidth, dispalyHeight2, leftViewStartPos + coordinateOffset2, dispalyHeight2 - leftBackWindowSize.y, viewAdapt2.left, leftBackWindowSize.y, leftViewStartPos + coordinateOffset2, dispalyHeight2 - leftBackWindowSize.y);
        this.mGestureNavRight.updateViewConfig(displayWidth, dispalyHeight2, (displayWidth - viewAdapt2.right) - rightViewOffset, dispalyHeight2 - rightBackWindowSize.y, viewAdapt2.right, rightBackWindowSize.y, (displayWidth - viewAdapt2.right) - rightViewOffset, dispalyHeight2 - rightBackWindowSize.y);
        this.mGestureNavBottom.updateViewConfig(displayWidth, dispalyHeight2, leftViewStartPos + coordinateOffset2, dispalyHeight2 - viewAdapt2.bottom, viewWidth - coordinateOffset2, viewAdapt2.bottom, leftViewStartPos + coordinateOffset2, dispalyHeight2 - viewAdapt2.bottom);
        this.mGestureNavAnimProxy.updateViewConfig(displayWidth, dispalyHeight2, leftViewStartPos + coordinateOffset2, 0, viewWidth - coordinateOffset2, dispalyHeight2, leftViewStartPos + coordinateOffset2, 0);
    }

    private boolean updateHomeWindowLocked() {
        String homeWindow = this.mDeviceStateController.getCurrentHomeActivity(this.mCurrentUserId);
        if (homeWindow == null || homeWindow.equals(this.mHomeWindow)) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newHome=" + homeWindow + ", oldHome=" + this.mHomeWindow);
        }
        this.mHomeWindow = homeWindow;
        return true;
    }

    private boolean isHomeWindowLocked(String windowName) {
        String str = this.mHomeWindow;
        if (str == null || !str.equals(windowName) || isShowDockEnabled()) {
            return false;
        }
        return true;
    }

    private boolean isFocusWindowLeftBackDisabledLocked() {
        return isFocusWindowBackDisabledLocked(this.mAppGestureNavLeftMode, 4194304);
    }

    private boolean isFocusWindowRightBackDisabledLocked() {
        return isFocusWindowBackDisabledLocked(this.mAppGestureNavRightMode, 8388608);
    }

    private boolean isFocusWindowBackDisabledLocked(int sideMode, int sideDisableOptions) {
        if (sideMode == 1) {
            return false;
        }
        if (sideMode != 2) {
            return isHomeWindowLocked(this.mFocusWindowTitle) || (this.mFocusWinNavOptions & sideDisableOptions) != 0;
        }
        return true;
    }

    private boolean isFocusWindowBottomDisabledLocked() {
        boolean isDisabled;
        int i = this.mAppGestureNavBottomMode;
        boolean z = true;
        if (i == 1) {
            isDisabled = false;
        } else if (i != 2) {
            if ((this.mFocusWinNavOptions & AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE) == 0) {
                z = false;
            }
            isDisabled = z;
        } else {
            isDisabled = true;
        }
        if (!isDisabled || isAppCanDisableGesture()) {
            return isDisabled;
        }
        Log.i(TAG, "Permission denied for disabling bottom");
        return false;
    }

    private void resetAppGestureNavModeLocked() {
        this.mAppGestureNavLeftMode = 0;
        this.mAppGestureNavRightMode = 0;
        this.mAppGestureNavBottomMode = 0;
    }

    private boolean isAppCanDisableGesture() {
        return GestureUtils.isSystemOrSignature(this.mContext, this.mFocusPackageName);
    }

    private void handleNightModeChanged() {
        synchronized (this.mLock) {
            updateNightModeLocked();
        }
    }

    private void updateNightModeLocked() {
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "ui_night_mode", 1, -2) != 2) {
            z = false;
        }
        this.mIsNightMode = z;
        if (this.mGestureNavAnimProxy != null) {
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "New nightMode=" + this.mIsNightMode);
            }
            this.mGestureNavAnimProxy.setNightMode(this.mIsNightMode);
        }
    }

    private boolean updateNotchDisplayStateLocked() {
        boolean isDisplayNotchStatus = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, -2) == 1;
        if (isDisplayNotchStatus == this.mIsNotchDisplayDisabled) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "isDisplayNotchStatus=" + isDisplayNotchStatus + ", oldNotch=" + this.mIsNotchDisplayDisabled);
        }
        this.mIsNotchDisplayDisabled = isDisplayNotchStatus;
        return true;
    }

    private boolean updateShrinkStateLocked() {
        int i;
        boolean shrinkState = GestureNavConst.IS_SUPPORT_FULL_BACK && ((i = this.mShrinkNavId) == 1 || i == 2) && !GestureNavConst.STATUSBAR_WINDOW.equals(this.mFocusWindowTitle);
        if (this.mIsInShrinkState == shrinkState) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newShrink=" + shrinkState + ", oldShrink=" + this.mIsInShrinkState);
        }
        this.mIsInShrinkState = shrinkState;
        return true;
    }

    private boolean updateGestureNavTipsStateLocked() {
        if (false == this.mIsGestureNavTipsEnabled) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newTips=" + false + ", oldTips=" + this.mIsGestureNavTipsEnabled);
        }
        this.mIsGestureNavTipsEnabled = false;
        return true;
    }

    private void updateNavBarModeProp(boolean isEnableTips) {
        int oldPropValue = SystemProperties.getInt("persist.sys.navigationbar.mode", 0);
        int newPropValue = isEnableTips ? oldPropValue | 2 : oldPropValue & -3;
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newPropValue=" + newPropValue + ", oldPropValue=" + oldPropValue);
        }
        if (newPropValue != oldPropValue) {
            SystemProperties.set("persist.sys.navigationbar.mode", String.valueOf(newPropValue));
        }
    }

    /* access modifiers changed from: private */
    public boolean updateDisplayDensity() {
        String densityStr = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", -2);
        if (densityStr == null || densityStr.equals(this.mDensityStr)) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newDensity=" + densityStr + ", oldDensity=" + this.mDensityStr);
        }
        this.mDensityStr = densityStr;
        return true;
    }

    private boolean isUsingNotch(WindowManagerPolicy.WindowState win) {
        if (this.mHasNotch) {
            return isSplitWinUsingNotch(win);
        }
        return true;
    }

    private boolean isSplitWinUsingNotch(WindowManagerPolicy.WindowState win) {
        if (!isShowDockEnabled() || !isHasHwSplitScreen()) {
            return win.isWindowUsingNotch();
        }
        return false;
    }

    private boolean isRotationChangedInLand(int lastRotation, int newRotation) {
        if ((lastRotation == 1 && newRotation == 3) || (lastRotation == 3 && newRotation == 1)) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.gesture.GestureNavView.IGestureEventProxy
    public boolean onTouchEvent(GestureNavView view, MotionEvent event) {
        if (view == null) {
            Log.i(TAG, "GestureNavView is NULL, return.");
            return false;
        }
        int navId = view.getNavId();
        if (navId == 1) {
            GestureNavBaseStrategy gestureNavBaseStrategy = this.mLeftBackStrategy;
            if (gestureNavBaseStrategy != null) {
                gestureNavBaseStrategy.onTouchEvent(event, false);
            }
        } else if (navId == 2) {
            GestureNavBaseStrategy gestureNavBaseStrategy2 = this.mRightBackStrategy;
            if (gestureNavBaseStrategy2 != null) {
                gestureNavBaseStrategy2.onTouchEvent(event, false);
            }
        } else if (navId != 3) {
            switch (navId) {
                case 11:
                    GestureNavBaseStrategy gestureNavBaseStrategy3 = this.mLeftBackStrategy;
                    if (gestureNavBaseStrategy3 != null) {
                        gestureNavBaseStrategy3.onTouchEvent(event, true);
                        break;
                    }
                    break;
                case 12:
                    GestureNavBaseStrategy gestureNavBaseStrategy4 = this.mRightBackStrategy;
                    if (gestureNavBaseStrategy4 != null) {
                        gestureNavBaseStrategy4.onTouchEvent(event, true);
                        break;
                    }
                    break;
                case 13:
                    GestureNavBottomStrategy gestureNavBottomStrategy = this.mBottomStrategy;
                    if (gestureNavBottomStrategy != null && !this.mIsKeyNavEnabled) {
                        gestureNavBottomStrategy.onTouchEvent(event, true);
                        break;
                    }
            }
        } else {
            GestureNavBottomStrategy gestureNavBottomStrategy2 = this.mBottomStrategy;
            if (gestureNavBottomStrategy2 != null && !this.mIsKeyNavEnabled) {
                gestureNavBottomStrategy2.onTouchEvent(event, false);
            }
        }
        return true;
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void dump(String dumpPrefix, PrintWriter pw, String[] args) {
        if (GestureNavConst.DEBUG_DUMP && this.mIsGestureNavEnabled) {
            pw.println(dumpPrefix + TAG);
            String prefix = dumpPrefix + "  ";
            printDump(prefix, pw);
            GestureDataTracker gestureDataTracker = this.mGestureDataTracker;
            if (gestureDataTracker != null) {
                gestureDataTracker.dump(prefix, pw, args);
            }
            Handler handler = this.mHandler;
            if (handler != null) {
                DumpUtils.dumpAsync(handler, new DumpUtils.Dump() {
                    /* class com.android.server.gesture.GestureNavManager.AnonymousClass6 */

                    public void dump(PrintWriter pw, String prefix) {
                        if (GestureNavManager.this.mHasGestureNavReady && GestureNavManager.this.mIsWindowViewSetuped && GestureNavManager.this.mGestureNavLeft != null) {
                            pw.print(prefix);
                            pw.println("GestureNavLeft=" + GestureNavManager.this.mGestureNavLeft.getViewConfig());
                        }
                        if (GestureNavManager.this.mHasGestureNavReady && GestureNavManager.this.mIsWindowViewSetuped && GestureNavManager.this.mGestureNavRight != null) {
                            pw.print(prefix);
                            pw.println("GestureNavRight=" + GestureNavManager.this.mGestureNavRight.getViewConfig());
                        }
                        if (GestureNavManager.this.mHasGestureNavReady && GestureNavManager.this.mIsWindowViewSetuped && GestureNavManager.this.mGestureNavBottom != null) {
                            pw.print(prefix);
                            pw.println("GestureNavBottom=" + GestureNavManager.this.mGestureNavBottom.getViewConfig());
                        }
                        if (GestureNavManager.this.mHasGestureNavReady && GestureNavManager.this.mIsWindowViewSetuped && GestureNavManager.this.mBottomStrategy != null) {
                            GestureNavManager.this.mBottomStrategy.dump(prefix, pw, null);
                        }
                    }
                }, pw, prefix, 200);
            }
        }
    }

    private void printDump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mIsGestureNavTipsEnabled=" + this.mIsGestureNavTipsEnabled);
        pw.print(" mCurrentUserId=" + this.mCurrentUserId);
        pw.println();
        pw.print(prefix);
        pw.print("mIsDeviceProvisioned=" + this.mIsDeviceProvisioned);
        pw.print(" mIsUserSetuped=" + this.mIsUserSetuped);
        pw.print(" mIsWindowViewSetuped=" + this.mIsWindowViewSetuped);
        pw.println();
        pw.print(prefix);
        pw.print("mIsKeyguardShowing=" + this.mIsKeyguardShowing);
        pw.print(" mIsInKeyguardMainWindow=" + this.mIsInKeyguardMainWindow);
        pw.print(" mIsNotchDisplayDisabled=" + this.mIsNotchDisplayDisabled);
        pw.println();
        pw.print(prefix);
        pw.println("mHomeWindow=" + this.mHomeWindow);
        pw.print(prefix);
        pw.println("mFocusWindowTitle=" + this.mFocusWindowTitle);
        pw.print(prefix);
        pw.print("mIsFocusWindowUsingNotch=" + this.mIsFocusWindowUsingNotch);
        pw.print(" mFocusWinNavOptions=0x" + Integer.toHexString(this.mFocusWinNavOptions));
        pw.print(" mFocusAppUid=" + this.mFocusAppUid);
        pw.println();
        pw.print(prefix);
        pw.print("mAppGestureNavLeftMode=" + this.mAppGestureNavLeftMode);
        pw.print(" mAppGestureNavRightMode=" + this.mAppGestureNavRightMode);
        pw.print(" mAppGestureNavBottomMode=" + this.mAppGestureNavBottomMode);
        pw.println();
        pw.print(prefix);
        pw.print("mIsNavLeftBackEnabled=" + this.mIsNavLeftBackEnabled);
        pw.print(" mIsNavRightBackEnabled=" + this.mIsNavRightBackEnabled);
        pw.print(" mIsNavBottomEnabled=" + this.mIsNavBottomEnabled);
        pw.println();
        pw.print(prefix);
        pw.print("mRotation=" + this.mRotation);
        pw.print(" mIsLandscape=" + this.mIsLandscape);
        pw.print(" mDensityStr=" + this.mDensityStr);
        if (this.mHasNotch) {
            pw.print(" mHasNotch=" + this.mHasNotch);
        }
        pw.println();
        pw.print(prefix);
        pw.print("mExcludedRegionHeight=" + this.mExcludedRegionHeight);
        pw.print(" mIsInShrinkState=" + this.mIsInShrinkState);
        pw.print(" mShrinkNavId=" + this.mShrinkNavId);
        pw.println();
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void destroySubScreenNavView() {
        this.mIsSubScreenEnableGestureNav = false;
        if (this.mIsNavStarted) {
            this.mHandler.sendEmptyMessage(16);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void initSubScreenNavView() {
        this.mIsSubScreenEnableGestureNav = true;
        if (this.mIsNavStarted) {
            this.mHandler.sendEmptyMessage(15);
        }
    }

    @Override // com.android.server.gesture.GestureNavPolicy
    public void bringTopSubScreenNavView() {
        if (this.mIsSubScreenEnableGestureNav && this.mIsNavStarted) {
            this.mHandler.sendEmptyMessage(17);
        }
    }

    private void handleSubScreenCreateNavView() {
        if (this.mIsSubScreenEnableGestureNav && this.mIsGestureNavEnabled) {
            GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
            if (gestureNavSubScreenManager != null) {
                gestureNavSubScreenManager.destroySubScreenNavWindows();
                this.mSubScreenGestureNavManager = null;
            }
            Context context = this.mContext;
            int i = this.mRotation;
            boolean z = this.mIsNavLeftBackEnabled;
            this.mSubScreenGestureNavManager = new GestureNavSubScreenManager(context, i, z, z, this.mIsNavBottomEnabled);
            this.mSubScreenGestureNavManager.setGestureEventProxy(this);
        }
    }

    private void handleSubScreenDestoryNavView() {
        GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
        if (gestureNavSubScreenManager != null) {
            gestureNavSubScreenManager.destroySubScreenNavWindows();
            this.mSubScreenGestureNavManager = null;
        }
    }

    private void handleSubScreenBringTopNavView() {
        GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
        if (gestureNavSubScreenManager != null) {
            if (!this.mIsSubScreenEnableGestureNav || !this.mIsGestureNavEnabled) {
                handleSubScreenDestoryNavView();
            } else {
                gestureNavSubScreenManager.bringSubScreenNavViewToTop();
            }
        }
    }

    private void handleRotationChangedSubScreen(int rotation) {
        GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
        if (gestureNavSubScreenManager != null) {
            if (!this.mIsSubScreenEnableGestureNav || !this.mIsGestureNavEnabled) {
                handleSubScreenDestoryNavView();
            } else {
                gestureNavSubScreenManager.handleRotationChangedSubScreen(rotation);
            }
        }
    }

    private void handleConfigChangedSubScreen() {
        GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
        if (gestureNavSubScreenManager != null) {
            if (!this.mIsSubScreenEnableGestureNav || !this.mIsGestureNavEnabled) {
                handleSubScreenDestoryNavView();
            } else {
                gestureNavSubScreenManager.handleConfigChangedSubScreen(this.mRotation);
            }
        }
    }

    private void updateSubScreenNavVisibleLocked(boolean isDelay) {
        GestureNavSubScreenManager gestureNavSubScreenManager = this.mSubScreenGestureNavManager;
        if (gestureNavSubScreenManager != null) {
            if (!this.mIsSubScreenEnableGestureNav || !this.mIsGestureNavEnabled) {
                handleSubScreenDestoryNavView();
            } else {
                gestureNavSubScreenManager.updateSubScreenNavVisibleLocked(this.mIsNavLeftBackEnabled, this.mIsNavRightBackEnabled, this.mIsNavBottomEnabled, this.mRotation, isDelay);
            }
        }
    }

    private void checkIsHomeActivityOfLauncher(AppGestureNavMode appGestureNavMode) {
        if (GestureNavConst.isLauncher(this.mFocusPackageName)) {
            if (appGestureNavMode.leftMode == 1 && appGestureNavMode.rightMode == 1) {
                setIsInHomeOfLauncher(false);
            } else if (appGestureNavMode.leftMode == 2 && appGestureNavMode.rightMode == 2) {
                setIsInHomeOfLauncher(true);
            }
        }
    }

    private void setIsInHomeOfLauncher(boolean status) {
        GestureNavBaseStrategy gestureNavBaseStrategy = this.mLeftBackStrategy;
        if (gestureNavBaseStrategy != null) {
            gestureNavBaseStrategy.mIsInHomeOfLauncher = status;
        }
        GestureNavBaseStrategy gestureNavBaseStrategy2 = this.mRightBackStrategy;
        if (gestureNavBaseStrategy2 != null) {
            gestureNavBaseStrategy2.mIsInHomeOfLauncher = status;
        }
    }
}
