package com.android.server.gesture;

import android.app.ActivityManager;
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
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerPolicy.WindowState;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.DumpUtils.Dump;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.gesture.DeviceStateController.DeviceChangedListener;
import com.android.server.gesture.GestureNavView.IGestureEventProxy;
import com.android.server.gesture.GestureNavView.WindowConfig;
import huawei.com.android.server.policy.HwGlobalActionsData;
import java.io.PrintWriter;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsDetailModeID;

public class GestureNavManager implements IGestureEventProxy, Monitor {
    private static final int MSG_CONFIG_CHANGED = 3;
    private static final int MSG_DEVICE_STATE_CHANGED = 4;
    private static final int MSG_FOCUS_CHANGED = 5;
    private static final int MSG_KEYGUARD_STATE_CHANGED = 6;
    private static final int MSG_NOTCH_DISPLAY_CHANGED = 9;
    private static final int MSG_PREFER_CHANGED = 8;
    private static final int MSG_RELOAD_NAV_GLOBAL_STATE = 2;
    private static final int MSG_ROTATION_CHANGED = 7;
    private static final int MSG_UPDATE_NAV_GLOBAL_STATE = 1;
    private static final String TAG = "GestureNavManager";
    private GestureNavBottomStrategy mBottomStrategy;
    private Context mContext;
    private DensityObserver mDensityObserver;
    private String mDensityStr;
    private final DeviceChangedListener mDeviceChangedCallback = new DeviceChangedListener() {
        public void onDeviceProvisionedChanged(boolean provisioned) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Device provisioned changed, provisioned=" + provisioned);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onUserSetupChanged(boolean setup) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "User setup changed, setup=" + setup);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onGuideOtaStateChanged() {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Guide OTA state changed");
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onPreferredActivityChanged(boolean isPrefer) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Preferred activity changed, isPrefer=" + isPrefer);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(8);
            }
        }
    };
    private boolean mDeviceProvisioned;
    private DeviceStateController mDeviceStateController;
    private Point mDisplaySize = new Point();
    private int mFocusWinNavOptions;
    private String mFocusWindowTitle;
    private boolean mFocusWindowUsingNotch = true;
    private GestureNavAnimProxy mGestureNavAnimProxy;
    private GestureNavView mGestureNavBottom;
    private boolean mGestureNavEnabled;
    private GestureNavView mGestureNavLeft;
    private boolean mGestureNavReady;
    private GestureNavView mGestureNavRight;
    private boolean mGuideOtaFinished = true;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mHasNotch;
    private int mHoleHeight = 0;
    private String mHomeWindow;
    private boolean mInKeyguardMainWindow;
    private boolean mKeyguardShowing;
    private boolean mLandscape;
    private LauncherStateChangedReceiver mLauncherStateChangedReceiver;
    private GestureNavBaseStrategy mLeftBackStrategy;
    private final Object mLock = new Object();
    private boolean mNavBackEnabled;
    private boolean mNavBottomEnabled;
    private boolean mNavStarted;
    private boolean mNotchDisplayDisabled;
    private NotchObserver mNotchObserver;
    private GestureNavBaseStrategy mRightBackStrategy;
    private int mRotation = (SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90);
    private boolean mUserSetuped;
    private WindowManager mWindowManager;
    private boolean mWindowViewSetuped;

    private final class DensityObserver extends ContentObserver {
        public DensityObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (GestureNavManager.this.mNavStarted && GestureNavManager.this.updateDisplayDensity()) {
                GestureNavManager.this.mHandler.sendEmptyMessage(2);
            }
        }
    }

    private final class GestureHandler extends Handler {
        public GestureHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavManager.TAG, "handleMessage before msg=" + msg.what);
            }
            switch (msg.what) {
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
                    GestureNavManager gestureNavManager = GestureNavManager.this;
                    String str = (String) msg.obj;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    gestureNavManager.handleFocusChanged(str, z, msg.arg2);
                    break;
                case 6:
                    GestureNavManager gestureNavManager2 = GestureNavManager.this;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    gestureNavManager2.handleKeygaurdStateChanged(z);
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
            }
            if (GestureNavConst.DEBUG) {
                Log.d(GestureNavManager.TAG, "handleMessage after msg=" + msg.what);
            }
        }
    }

    private final class LauncherStateChangedReceiver extends BroadcastReceiver {
        private LauncherStateChangedReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    Log.i(GestureNavManager.TAG, "Launcher state changed, intent=" + intent);
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(8);
            }
        }
    }

    private final class NotchObserver extends ContentObserver {
        public NotchObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (GestureNavManager.this.mNavStarted) {
                GestureNavManager.this.mHandler.sendEmptyMessage(9);
            }
        }
    }

    public GestureNavManager(Context context) {
        this.mContext = context;
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new GestureHandler(this.mHandlerThread.getLooper());
    }

    public void systemReady() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        GestureUtils.systemReady();
        this.mHasNotch = GestureUtils.hasNotch();
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor(GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION), false, new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Log.i(GestureNavManager.TAG, "gesture nav status change");
                GestureNavManager.this.mHandler.sendEmptyMessage(1);
            }
        }, -1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    private void updateEnableStateLocked() {
        this.mGestureNavEnabled = GestureNavConst.isGestureNavEnabled(this.mContext, -2);
        Log.i(TAG, "mGestureNavEnabled=" + this.mGestureNavEnabled);
    }

    private void updateGestureNavGlobalState() {
        synchronized (this.mLock) {
            updateEnableStateLocked();
            if (this.mGestureNavEnabled) {
                startGestureNavLocked();
            } else {
                stopGestureNavLocked();
            }
        }
    }

    private void reloadGestureNavGlobalState() {
        synchronized (this.mLock) {
            Log.i(TAG, "force reloadGestureNavGlobalState");
            this.mGestureNavEnabled = false;
            stopGestureNavLocked();
            updateEnableStateLocked();
            if (this.mGestureNavEnabled) {
                startGestureNavLocked();
            }
        }
    }

    private void startGestureNavLocked() {
        Log.i(TAG, "startGestureNavLocked");
        this.mNavStarted = true;
        updateDisplayDensity();
        updateNotchDisplayStateLocked();
        this.mDeviceStateController = DeviceStateController.getInstance(this.mContext);
        this.mDeviceStateController.addCallback(this.mDeviceChangedCallback);
        updateGestureNavStateLocked();
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDensityObserver = new DensityObserver(this.mHandler);
        resolver.registerContentObserver(Secure.getUriFor("display_density_forced"), false, this.mDensityObserver, -1);
        this.mNotchObserver = new NotchObserver(this.mHandler);
        resolver.registerContentObserver(Secure.getUriFor("display_notch_status"), false, this.mNotchObserver, -1);
        this.mLauncherStateChangedReceiver = new LauncherStateChangedReceiver();
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, 0);
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, filter, null, this.mHandler);
    }

    private void stopGestureNavLocked() {
        Log.i(TAG, "stopGestureNavLocked");
        if (this.mLauncherStateChangedReceiver != null) {
            this.mContext.unregisterReceiver(this.mLauncherStateChangedReceiver);
            this.mLauncherStateChangedReceiver = null;
        }
        if (this.mDensityObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDensityObserver);
            this.mDensityObserver = null;
        }
        if (this.mNotchObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mNotchObserver);
            this.mNotchObserver = null;
        }
        updateGestureNavStateLocked();
        if (this.mDeviceStateController != null) {
            this.mDeviceStateController.removeCallback(this.mDeviceChangedCallback);
            this.mDeviceStateController = null;
        }
        this.mNavStarted = false;
    }

    private void updateGestureNavStateLocked() {
        updateDeviceStateLocked();
        updateConfigLocked();
        updateNavWindowLocked();
        updateNavVisibleLocked();
    }

    public void onUserChanged(int newUserId) {
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "User switched then reInit, newUserId=" + newUserId);
        }
        this.mHandler.sendEmptyMessage(1);
    }

    public void onConfigurationChanged() {
        if (this.mNavStarted) {
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "onConfigurationChanged");
            }
            this.mHandler.sendEmptyMessage(3);
        }
    }

    public void onRotationChanged(int rotation) {
        if (this.mNavStarted) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(7, rotation, 0));
        }
    }

    public void onFocusWindowChanged(WindowState lastFocus, WindowState newFocus) {
        if (this.mNavStarted && newFocus != null && newFocus.getAttrs() != null) {
            String focusWindowTitle = newFocus.getAttrs().getTitle().toString();
            boolean usingNotch = newFocus.isWindowUsingNotch();
            this.mHandler.sendMessage(this.mHandler.obtainMessage(5, usingNotch ? 1 : 0, newFocus.getHwGestureNavOptions(), focusWindowTitle));
        }
    }

    public void onKeyguardShowingChanged(boolean showing) {
        if (this.mNavStarted) {
            int i;
            Handler handler = this.mHandler;
            if (showing) {
                i = 1;
            } else {
                i = 0;
            }
            this.mHandler.sendMessage(handler.obtainMessage(6, i, 0));
        }
    }

    private void handleDeviceStateChanged() {
        synchronized (this.mLock) {
            updateDeviceStateLocked();
            updateNavVisibleLocked();
        }
    }

    private void handleConfigChanged() {
        synchronized (this.mLock) {
            if (this.mDeviceStateController != null) {
                this.mDeviceStateController.onConfigurationChanged();
            }
            updateConfigLocked();
            updateNavWindowLocked();
            updateNavVisibleLocked();
        }
    }

    private void handleRotationChanged(int rotation) {
        synchronized (this.mLock) {
            int lastRotation = this.mRotation;
            this.mRotation = rotation;
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "lastRotation=" + lastRotation + ", currentRotation=" + rotation);
            }
            if (isRotationChangedInLand(lastRotation, rotation)) {
                updateConfigLocked();
                updateNavWindowLocked();
                updateNavVisibleLocked();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handlePreferChanged() {
        synchronized (this.mLock) {
            if (this.mDeviceStateController == null) {
            } else if (updateHomeWindowLocked()) {
                updateNavVisibleLocked();
            }
        }
    }

    private void handleNotchDisplayChanged() {
        synchronized (this.mLock) {
            if (updateNotchDisplayStateLocked()) {
                updateConfigLocked();
                updateNavWindowLocked();
                updateNavVisibleLocked();
            }
        }
    }

    private void handleFocusChanged(String focusedWindow, boolean usingNotch, int focusWindowNavOptions) {
        synchronized (this.mLock) {
            this.mFocusWindowTitle = focusedWindow;
            this.mInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "Focus:" + this.mFocusWindowTitle + ", UN=" + usingNotch + ", LUN=" + this.mFocusWindowUsingNotch + ", FNO=" + focusWindowNavOptions + ", LFNO=" + this.mFocusWinNavOptions + ", IKMW=" + this.mInKeyguardMainWindow);
            }
            this.mFocusWinNavOptions = focusWindowNavOptions;
            if (this.mGestureNavAnimProxy != null) {
                this.mGestureNavAnimProxy.updateFocusWindow(this.mFocusWindowTitle);
            }
            if (this.mFocusWindowUsingNotch != usingNotch) {
                this.mFocusWindowUsingNotch = usingNotch;
                updateConfigLocked();
                updateNavWindowLocked();
            }
            updateNavVisibleLocked();
        }
    }

    private void handleKeygaurdStateChanged(boolean showing) {
        synchronized (this.mLock) {
            this.mKeyguardShowing = showing;
            this.mInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "keyguard showing=" + showing + ", IKMW=" + this.mInKeyguardMainWindow);
            }
            updateNavVisibleLocked();
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateDeviceState(this.mKeyguardShowing);
            }
        }
    }

    private void updateDeviceStateLocked() {
        if (this.mDeviceStateController != null) {
            this.mDeviceProvisioned = this.mDeviceStateController.isDeviceProvisioned();
            this.mUserSetuped = this.mDeviceStateController.isCurrentUserSetup();
            this.mGuideOtaFinished = this.mDeviceStateController.isGuideOtaFinished();
            this.mKeyguardShowing = this.mDeviceStateController.isKeyguardShowingOrOccluded();
            this.mInKeyguardMainWindow = this.mDeviceStateController.isKeyguardShowingAndNotOccluded();
            updateHomeWindowLocked();
            WindowState focusWindowState = this.mDeviceStateController.getFocusWindow();
            if (!(focusWindowState == null || focusWindowState.getAttrs() == null)) {
                this.mFocusWindowTitle = focusWindowState.getAttrs().getTitle().toString();
                this.mFocusWindowUsingNotch = focusWindowState.isWindowUsingNotch();
                if (this.mGestureNavAnimProxy != null) {
                    this.mGestureNavAnimProxy.updateFocusWindow(this.mFocusWindowTitle);
                }
            }
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateDeviceState(this.mKeyguardShowing);
            }
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "Update device state, provisioned:" + this.mDeviceProvisioned + ", userSetup:" + this.mUserSetuped + ", guideOtaFinish:" + this.mGuideOtaFinished + ", keyguardShowing:" + this.mKeyguardShowing + ", IKMW:" + this.mInKeyguardMainWindow + ", focus:" + this.mFocusWindowTitle + ", usingNotch:" + this.mFocusWindowUsingNotch + ", home:" + this.mHomeWindow);
            }
        }
    }

    private void updateConfigLocked() {
        if (this.mGestureNavReady) {
            this.mLandscape = this.mContext.getResources().getConfiguration().orientation == 2;
            this.mWindowManager.getDefaultDisplay().getRealSize(this.mDisplaySize);
            int displayWidth = this.mDisplaySize.x;
            int displayHeight = this.mDisplaySize.y;
            int backWindowHeight = Math.round(((float) displayHeight) * 0.75f);
            int backWindowWidth = GestureNavConst.getBackWindowWidth(this.mContext);
            int bottomWindowHeight = GestureNavConst.getBottomWindowHeight(this.mContext);
            this.mHoleHeight = GestureNavConst.getStatusBarHeight(this.mContext);
            boolean z = true;
            if (this.mHasNotch) {
                z = this.mLandscape ? !this.mNotchDisplayDisabled ? this.mFocusWindowUsingNotch : false : true;
                this.mGestureNavLeft.updateViewNotchState(z);
                this.mGestureNavRight.updateViewNotchState(z);
                this.mGestureNavAnimProxy.updateViewNotchState(z);
            }
            if (GestureNavConst.DEBUG) {
                Log.d(TAG, "w=" + displayWidth + ", h=" + displayHeight + ", backH=" + backWindowHeight + ", backW=" + backWindowWidth + ", bottomH=" + bottomWindowHeight + ", usingNotch=" + z + ", holeH=" + this.mHoleHeight);
            }
            updateViewConfigLocked(displayWidth, displayHeight, backWindowWidth, backWindowHeight, bottomWindowHeight, z);
        }
    }

    private void updateNavWindowLocked() {
        if (this.mGestureNavEnabled) {
            if (this.mWindowViewSetuped) {
                updateNavWindows();
            } else {
                createNavWindows();
            }
        } else if (this.mWindowViewSetuped) {
            destroyNavWindows();
        }
    }

    private void updateNavVisibleLocked() {
        if (this.mWindowViewSetuped) {
            boolean showBack = true;
            boolean showBottom = true;
            if (this.mInKeyguardMainWindow) {
                showBack = false;
                showBottom = false;
            } else {
                if (!(this.mDeviceProvisioned && (this.mUserSetuped ^ 1) == 0 && (this.mGuideOtaFinished ^ 1) == 0 && !isFocusWindowBottomDisabledLocked())) {
                    showBottom = false;
                }
                if (isSpecialWindowLocked(this.mFocusWindowTitle) || isFocusWindowBackDisabledLocked()) {
                    showBack = false;
                }
            }
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "updateNavVisible showBack:" + showBack + ", showBottom:" + showBottom);
            }
            enableBackNavLocked(showBack);
            enableBottomNavLocked(showBottom);
        }
    }

    private void enableBackNavLocked(boolean enable) {
        if (this.mWindowViewSetuped && this.mNavBackEnabled != enable) {
            this.mGestureNavLeft.show(enable);
            this.mGestureNavRight.show(enable);
            this.mNavBackEnabled = enable;
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "enableBackNav show:" + this.mNavBackEnabled);
            }
        }
    }

    private void enableBottomNavLocked(boolean enable) {
        if (this.mWindowViewSetuped && this.mNavBottomEnabled != enable) {
            this.mGestureNavBottom.show(enable);
            this.mNavBottomEnabled = enable;
            if (GestureNavConst.DEBUG) {
                Log.i(TAG, "enableBottomNav show:" + this.mNavBottomEnabled);
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
        this.mBottomStrategy = new GestureNavBottomStrategy(3, this.mContext, looper, this.mGestureNavAnimProxy);
        this.mGestureNavReady = true;
        Log.i(TAG, "gesture nav ready.");
        updateConfigLocked();
        configAndAddNavWindow("GestureNavLeft", this.mGestureNavLeft, this.mLeftBackStrategy);
        configAndAddNavWindow("GestureNavRight", this.mGestureNavRight, this.mRightBackStrategy);
        configAndAddNavWindow("GestureNavBottom", this.mGestureNavBottom, this.mBottomStrategy);
        this.mLeftBackStrategy.onNavCreate(this.mGestureNavBottom);
        this.mRightBackStrategy.onNavCreate(this.mGestureNavBottom);
        this.mBottomStrategy.onNavCreate(this.mGestureNavBottom);
        this.mGestureNavAnimProxy.onNavCreate();
        forceUpdateStateToProxyLocked();
        this.mBottomStrategy.updateDeviceState(this.mKeyguardShowing);
        this.mWindowViewSetuped = true;
        this.mNavBackEnabled = true;
        this.mNavBottomEnabled = true;
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
        this.mGestureNavReady = false;
        this.mWindowViewSetuped = false;
        this.mNavBackEnabled = false;
        this.mNavBottomEnabled = false;
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

    private LayoutParams createLayoutParams(String title, WindowConfig config) {
        LayoutParams lp = new LayoutParams(2024, 296);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= HwGlobalActionsData.FLAG_SHUTDOWN;
        }
        lp.format = -2;
        lp.alpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
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
        return lp;
    }

    private void configAndAddNavWindow(String title, GestureNavView view, GestureNavBaseStrategy strategy) {
        WindowConfig config = view.getViewConfig();
        LayoutParams params = createLayoutParams(title, config);
        strategy.updateConfig(config.displayWidth, config.displayHeight, new Rect(config.locationOnScreenX, config.locationOnScreenY, config.locationOnScreenX + config.width, config.locationOnScreenY + config.height));
        view.setGestureEventProxy(this);
        GestureUtils.addWindowView(this.mWindowManager, view, params);
    }

    private void reLayoutNavWindow(String title, GestureNavView view, GestureNavBaseStrategy strategy) {
        WindowConfig config = view.getViewConfig();
        LayoutParams params = createLayoutParams(title, config);
        strategy.updateConfig(config.displayWidth, config.displayHeight, new Rect(config.locationOnScreenX, config.locationOnScreenY, config.locationOnScreenX + config.width, config.locationOnScreenY + config.height));
        GestureUtils.updateViewLayout(this.mWindowManager, view, params);
    }

    private boolean isInKeyguardMainWindowLocked() {
        if (!this.mKeyguardShowing || ((this.mDeviceStateController == null || (this.mDeviceStateController.isKeyguardOccluded() ^ 1) == 0) && !GestureNavConst.STATUSBAR_WINDOW.equals(this.mFocusWindowTitle))) {
            return false;
        }
        return true;
    }

    private void updateViewConfigLocked(int displayWidth, int displayHeight, int backWindowWidth, int backWindowHeight, int bottomWindowHeight, boolean usingNotch) {
        int leftViewStartPos = 0;
        int rightViewOffset = 0;
        int viewWidth = displayWidth;
        if (this.mHasNotch && (usingNotch ^ 1) != 0) {
            switch (this.mRotation) {
                case 1:
                    leftViewStartPos = this.mHoleHeight;
                    viewWidth = displayWidth - this.mHoleHeight;
                    rightViewOffset = 0;
                    break;
                case 3:
                    leftViewStartPos = 0;
                    viewWidth = displayWidth - this.mHoleHeight;
                    rightViewOffset = this.mHoleHeight;
                    break;
            }
        }
        this.mGestureNavLeft.updateViewConfig(displayWidth, displayHeight, 0, displayHeight - backWindowHeight, backWindowWidth, backWindowHeight, leftViewStartPos, displayHeight - backWindowHeight);
        this.mGestureNavRight.updateViewConfig(displayWidth, displayHeight, (displayWidth - backWindowWidth) - rightViewOffset, displayHeight - backWindowHeight, backWindowWidth, backWindowHeight, (displayWidth - backWindowWidth) - rightViewOffset, displayHeight - backWindowHeight);
        this.mGestureNavBottom.updateViewConfig(displayWidth, displayHeight, 0, displayHeight - bottomWindowHeight, viewWidth, bottomWindowHeight, leftViewStartPos, displayHeight - bottomWindowHeight);
        this.mGestureNavAnimProxy.updateViewConfig(displayWidth, displayHeight, 0, 0, viewWidth, displayHeight, leftViewStartPos, 0);
    }

    private boolean updateHomeWindowLocked() {
        String homeWindow = this.mDeviceStateController.getCurrentHomeActivity();
        if (homeWindow == null || (homeWindow.equals(this.mHomeWindow) ^ 1) == 0) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.i(TAG, "newHomeWindow=" + homeWindow + ", oldHomeWindow=" + this.mHomeWindow);
        }
        this.mHomeWindow = homeWindow;
        if (this.mGestureNavAnimProxy != null) {
            this.mGestureNavAnimProxy.updateHomeWindow(this.mHomeWindow);
        }
        return true;
    }

    private void forceUpdateStateToProxyLocked() {
        this.mGestureNavAnimProxy.updateHomeWindow(this.mHomeWindow);
        this.mGestureNavAnimProxy.updateFocusWindow(this.mFocusWindowTitle);
    }

    private boolean isSpecialWindowLocked(String windowName) {
        if (this.mHomeWindow == null || !this.mHomeWindow.equals(windowName)) {
            return false;
        }
        return true;
    }

    private boolean isFocusWindowBackDisabledLocked() {
        return (this.mFocusWinNavOptions & HighBitsDetailModeID.MODE_FOLIAGE) != 0;
    }

    private boolean isFocusWindowBottomDisabledLocked() {
        return (this.mFocusWinNavOptions & 524288) != 0;
    }

    private boolean updateNotchDisplayStateLocked() {
        boolean notchStatus = Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, -2) == 1;
        if (notchStatus == this.mNotchDisplayDisabled) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "newNotch=" + notchStatus + ", oldNotch=" + this.mNotchDisplayDisabled);
        }
        this.mNotchDisplayDisabled = notchStatus;
        return true;
    }

    private boolean updateDisplayDensity() {
        String densityStr = Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", -2);
        if (densityStr == null || (densityStr.equals(this.mDensityStr) ^ 1) == 0) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            Log.d(TAG, "newDensity=" + densityStr + ", oldDensity=" + this.mDensityStr);
        }
        this.mDensityStr = densityStr;
        return true;
    }

    private boolean isRotationChangedInLand(int lastRotation, int newRotation) {
        if ((lastRotation == 1 && newRotation == 3) || (lastRotation == 3 && newRotation == 1)) {
            return true;
        }
        return false;
    }

    public boolean onTouchEvent(GestureNavView view, MotionEvent event) {
        switch (view.getNavId()) {
            case 1:
                if (this.mLeftBackStrategy != null) {
                    this.mLeftBackStrategy.onTouchEvent(event);
                    break;
                }
                break;
            case 2:
                if (this.mRightBackStrategy != null) {
                    this.mRightBackStrategy.onTouchEvent(event);
                    break;
                }
                break;
            case 3:
                if (this.mBottomStrategy != null) {
                    this.mBottomStrategy.onTouchEvent(event);
                    break;
                }
                break;
        }
        return true;
    }

    public void setRecentPosition(int x, int y, int width, int height) {
        synchronized (this.mLock) {
            if (this.mGestureNavAnimProxy != null) {
                this.mGestureNavAnimProxy.setRecentPosition(x, y, width, height);
            }
        }
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        if (GestureNavConst.DEBUG_DUMP && this.mGestureNavEnabled) {
            pw.println(prefix + TAG);
            prefix = prefix + "  ";
            pw.print(prefix);
            pw.print("mDeviceProvisioned=" + this.mDeviceProvisioned);
            pw.print(" mUserSetuped=" + this.mUserSetuped);
            pw.print(" mGuideOtaFinished=" + this.mGuideOtaFinished);
            pw.println();
            pw.print(prefix);
            pw.print("mKeyguardShowing=" + this.mKeyguardShowing);
            pw.print(" mInKeyguardMainWindow=" + this.mInKeyguardMainWindow);
            pw.print(" mNotchDisplayDisabled=" + this.mNotchDisplayDisabled);
            pw.print(" mFocusWindowUsingNotch=" + this.mFocusWindowUsingNotch);
            pw.println();
            pw.print(prefix);
            pw.println("mFocusWindowTitle=" + this.mFocusWindowTitle);
            pw.print(prefix);
            pw.println("mHomeWindow=" + this.mHomeWindow);
            pw.print(prefix);
            pw.print("mDensityStr=" + this.mDensityStr);
            pw.print(" mFocusWinNavOptions=0x" + Integer.toHexString(this.mFocusWinNavOptions));
            pw.println();
            pw.print(prefix);
            pw.print("mWindowViewSetuped=" + this.mWindowViewSetuped);
            pw.print(" mNavBackEnabled=" + this.mNavBackEnabled);
            pw.print(" mNavBottomEnabled=" + this.mNavBottomEnabled);
            pw.println();
            pw.print(prefix);
            pw.print("mRotation=" + this.mRotation);
            pw.print(" mLandscape=" + this.mLandscape);
            if (this.mHasNotch) {
                pw.print(" mHasNotch=" + this.mHasNotch);
            }
            pw.println();
            DumpUtils.dumpAsync(this.mHandler, new Dump() {
                public void dump(PrintWriter pw, String prefix) {
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavLeft != null) {
                        pw.print(prefix);
                        pw.println("GestureNavLeft=" + GestureNavManager.this.mGestureNavLeft.getViewConfig());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavRight != null) {
                        pw.print(prefix);
                        pw.println("GestureNavRight=" + GestureNavManager.this.mGestureNavRight.getViewConfig());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavBottom != null) {
                        pw.print(prefix);
                        pw.println("GestureNavBottom=" + GestureNavManager.this.mGestureNavBottom.getViewConfig());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mBottomStrategy != null) {
                        GestureNavManager.this.mBottomStrategy.dump(prefix, pw, null);
                    }
                }
            }, pw, prefix, 200);
        }
    }
}
