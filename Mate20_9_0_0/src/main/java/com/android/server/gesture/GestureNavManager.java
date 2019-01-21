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
import com.android.internal.util.DumpUtils;
import com.android.internal.util.DumpUtils.Dump;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.gesture.DeviceStateController.DeviceChangedListener;
import com.android.server.gesture.GestureNavView.IGestureEventProxy;
import com.android.server.gesture.GestureNavView.WindowConfig;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.WindowManagerPolicy.WindowState;
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
    private static final int MSG_SET_GESTURE_NAV_MODE = 11;
    private static final int MSG_UPDATE_NAV_GLOBAL_STATE = 1;
    private static final String TAG = "GestureNavManager";
    private int mAppGestureNavBottomMode;
    private int mAppGestureNavLeftMode;
    private int mAppGestureNavRightMode;
    private GestureNavBottomStrategy mBottomStrategy;
    private Context mContext;
    private int mCurrentUserId;
    private DensityObserver mDensityObserver;
    private String mDensityStr;
    private final DeviceChangedListener mDeviceChangedCallback = new DeviceChangedListener() {
        public void onDeviceProvisionedChanged(boolean provisioned) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Device provisioned changed, provisioned=");
                    stringBuilder.append(provisioned);
                    Log.i(str, stringBuilder.toString());
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onUserSwitched(int newUserId) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User switched, newUserId=");
                    stringBuilder.append(newUserId);
                    Log.i(str, stringBuilder.toString());
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onUserSetupChanged(boolean setup) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User setup changed, setup=");
                    stringBuilder.append(setup);
                    Log.i(str, stringBuilder.toString());
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(4);
            }
        }

        public void onPreferredActivityChanged(boolean isPrefer) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Preferred activity changed, isPrefer=");
                    stringBuilder.append(isPrefer);
                    Log.i(str, stringBuilder.toString());
                }
                GestureNavManager.this.mHandler.sendEmptyMessage(8);
            }
        }
    };
    private boolean mDeviceProvisioned;
    private DeviceStateController mDeviceStateController;
    private Point mDisplaySize = new Point();
    private int mFocusAppUid;
    private String mFocusPackageName;
    private int mFocusWinNavOptions;
    private String mFocusWindowTitle;
    private boolean mFocusWindowUsingNotch = true;
    private GestureNavAnimProxy mGestureNavAnimProxy;
    private GestureNavView mGestureNavBottom;
    private boolean mGestureNavEnabled;
    private GestureNavView mGestureNavLeft;
    private boolean mGestureNavReady;
    private GestureNavView mGestureNavRight;
    private boolean mGuideOrOtaAlive;
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

    private static final class AppGestureNavMode {
        public int bottomMode;
        public int leftMode;
        public String packageName;
        public int rightMode;
        public int uid;

        public AppGestureNavMode(String _packageName, int _uid, int _leftMode, int _rightMode, int _bottomMode) {
            this.packageName = _packageName;
            this.uid = _uid;
            this.leftMode = _leftMode;
            this.rightMode = _rightMode;
            this.bottomMode = _bottomMode;
        }

        public boolean isFromSameApp(String _packageName, int _uid) {
            return this.packageName != null && this.packageName.equals(_packageName) && this.uid == _uid;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pkg:");
            stringBuilder.append(this.packageName);
            stringBuilder.append(", uid:");
            stringBuilder.append(this.uid);
            stringBuilder.append(", left:");
            stringBuilder.append(this.leftMode);
            stringBuilder.append(", right:");
            stringBuilder.append(this.rightMode);
            stringBuilder.append(", bottom:");
            stringBuilder.append(this.bottomMode);
            return stringBuilder.toString();
        }
    }

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

    private static final class FocusWindowState {
        public int gestureNavOptions;
        public String packageName;
        public String title;
        public int uid;
        public boolean usingNotch;

        public FocusWindowState(String _packageName, int _uid, String _title, boolean _usingNotch, int _gestureNavOptions) {
            this.packageName = _packageName;
            this.uid = _uid;
            this.title = _title;
            this.usingNotch = _usingNotch;
            this.gestureNavOptions = _gestureNavOptions;
        }
    }

    private final class GestureHandler extends Handler {
        public GestureHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            if (GestureNavConst.DEBUG) {
                str = GestureNavManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage before msg=");
                stringBuilder.append(msg.what);
                Log.d(str, stringBuilder.toString());
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
                case 11:
                    GestureNavManager.this.handleAppGestureNavMode((AppGestureNavMode) msg.obj);
                    break;
            }
            if (GestureNavConst.DEBUG) {
                str = GestureNavManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage after msg=");
                stringBuilder.append(msg.what);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private final class LauncherStateChangedReceiver extends BroadcastReceiver {
        private LauncherStateChangedReceiver() {
        }

        /* synthetic */ LauncherStateChangedReceiver(GestureNavManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (GestureNavManager.this.mNavStarted) {
                if (GestureNavConst.DEBUG) {
                    String str = GestureNavManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Launcher state changed, intent=");
                    stringBuilder.append(intent);
                    Log.i(str, stringBuilder.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mGestureNavEnabled=");
        stringBuilder.append(this.mGestureNavEnabled);
        Log.i(str, stringBuilder.toString());
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
        resetAppGestureNavModeLocked();
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
        this.mLauncherStateChangedReceiver = new LauncherStateChangedReceiver(this, null);
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User switched then reInit, newUserId=");
            stringBuilder.append(newUserId);
            Log.i(str, stringBuilder.toString());
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

    public void onKeyguardShowingChanged(boolean showing) {
        if (this.mNavStarted) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(6, showing, 0));
        }
    }

    public void onFocusWindowChanged(WindowState lastFocus, WindowState newFocus) {
        if (this.mNavStarted && newFocus != null && newFocus.getAttrs() != null) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(5, new FocusWindowState(newFocus.getOwningPackage(), newFocus.getOwningUid(), newFocus.getAttrs().getTitle().toString(), newFocus.isWindowUsingNotch(), newFocus.getHwGestureNavOptions())));
        }
    }

    public void setGestureNavMode(String packageName, int uid, int leftMode, int rightMode, int bottomMode) {
        if (this.mNavStarted) {
            if (packageName == null) {
                Log.i(TAG, "packageName is null, return");
            } else if (leftMode != rightMode) {
                Log.i(TAG, "leftMode must be equal with right for current version, return");
            } else {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(11, new AppGestureNavMode(packageName, uid, leftMode, rightMode, bottomMode)));
            }
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lastRotation=");
                stringBuilder.append(lastRotation);
                stringBuilder.append(", currentRotation=");
                stringBuilder.append(rotation);
                Log.d(str, stringBuilder.toString());
            }
            if (isRotationChangedInLand(lastRotation, rotation)) {
                updateConfigLocked();
                updateNavWindowLocked();
                updateNavVisibleLocked();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0013, code skipped:
            return;
     */
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

    private void handleFocusChanged(FocusWindowState focusWindowState) {
        synchronized (this.mLock) {
            resetAppGestureNavModeLocked();
            this.mFocusAppUid = focusWindowState.uid;
            this.mFocusPackageName = focusWindowState.packageName;
            this.mFocusWindowTitle = focusWindowState.title;
            this.mFocusWinNavOptions = focusWindowState.gestureNavOptions;
            this.mInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Focus:");
                stringBuilder.append(this.mFocusWindowTitle);
                stringBuilder.append(", Uid=");
                stringBuilder.append(this.mFocusAppUid);
                stringBuilder.append(", UN=");
                stringBuilder.append(focusWindowState.usingNotch);
                stringBuilder.append(", LUN=");
                stringBuilder.append(this.mFocusWindowUsingNotch);
                stringBuilder.append(", FNO=");
                stringBuilder.append(this.mFocusWinNavOptions);
                stringBuilder.append(", IKMW=");
                stringBuilder.append(this.mInKeyguardMainWindow);
                stringBuilder.append(", pkg:");
                stringBuilder.append(this.mFocusPackageName);
                Log.d(str, stringBuilder.toString());
            }
            if (this.mFocusWindowUsingNotch != focusWindowState.usingNotch) {
                this.mFocusWindowUsingNotch = focusWindowState.usingNotch;
                updateConfigLocked();
                updateNavWindowLocked();
            }
            updateNavVisibleLocked(this.mLandscape);
        }
    }

    private void handleAppGestureNavMode(AppGestureNavMode appGestureNavMode) {
        synchronized (this.mLock) {
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AppMode:");
                stringBuilder.append(appGestureNavMode);
                Log.d(str, stringBuilder.toString());
            }
            if (appGestureNavMode.isFromSameApp(this.mFocusPackageName, this.mFocusAppUid)) {
                this.mAppGestureNavLeftMode = appGestureNavMode.leftMode;
                this.mAppGestureNavRightMode = appGestureNavMode.rightMode;
                this.mAppGestureNavBottomMode = appGestureNavMode.bottomMode;
                updateNavVisibleLocked();
                return;
            }
        }
    }

    private void handleKeygaurdStateChanged(boolean showing) {
        synchronized (this.mLock) {
            if (showing) {
                try {
                    resetAppGestureNavModeLocked();
                } catch (Throwable th) {
                }
            }
            this.mKeyguardShowing = showing;
            this.mInKeyguardMainWindow = isInKeyguardMainWindowLocked();
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("keyguard showing=");
                stringBuilder.append(showing);
                stringBuilder.append(", IKMW=");
                stringBuilder.append(this.mInKeyguardMainWindow);
                Log.d(str, stringBuilder.toString());
            }
            updateNavVisibleLocked();
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateDeviceState(this.mKeyguardShowing);
            }
        }
    }

    private void updateDeviceStateLocked() {
        if (this.mDeviceStateController != null) {
            this.mCurrentUserId = this.mDeviceStateController.getCurrentUser();
            this.mDeviceProvisioned = this.mDeviceStateController.isDeviceProvisioned();
            this.mUserSetuped = this.mDeviceStateController.isCurrentUserSetup();
            this.mKeyguardShowing = this.mDeviceStateController.isKeyguardShowingOrOccluded();
            this.mInKeyguardMainWindow = this.mDeviceStateController.isKeyguardShowingAndNotOccluded();
            updateHomeWindowLocked();
            WindowState focusWindowState = this.mDeviceStateController.getFocusWindow();
            if (!(focusWindowState == null || focusWindowState.getAttrs() == null)) {
                this.mFocusWindowTitle = focusWindowState.getAttrs().getTitle().toString();
                this.mFocusWindowUsingNotch = focusWindowState.isWindowUsingNotch();
            }
            if (this.mBottomStrategy != null) {
                this.mBottomStrategy.updateDeviceState(this.mKeyguardShowing);
            }
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Update device state, provisioned:");
                stringBuilder.append(this.mDeviceProvisioned);
                stringBuilder.append(", userSetup:");
                stringBuilder.append(this.mUserSetuped);
                stringBuilder.append(", guideOrOtaAlive:");
                stringBuilder.append(this.mGuideOrOtaAlive);
                stringBuilder.append(", keyguardShowing:");
                stringBuilder.append(this.mKeyguardShowing);
                stringBuilder.append(", IKMW:");
                stringBuilder.append(this.mInKeyguardMainWindow);
                stringBuilder.append(", focus:");
                stringBuilder.append(this.mFocusWindowTitle);
                stringBuilder.append(", usingNotch:");
                stringBuilder.append(this.mFocusWindowUsingNotch);
                stringBuilder.append(", home:");
                stringBuilder.append(this.mHomeWindow);
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    private void updateConfigLocked() {
        if (this.mGestureNavReady) {
            boolean z = false;
            this.mLandscape = this.mContext.getResources().getConfiguration().orientation == 2;
            this.mWindowManager.getDefaultDisplay().getRealSize(this.mDisplaySize);
            int displayWidth = this.mDisplaySize.x;
            int displayHeight = this.mDisplaySize.y;
            int backWindowHeight = Math.round(((float) displayHeight) * 0.75f);
            int backWindowWidth = GestureNavConst.getBackWindowWidth(this.mContext);
            int bottomWindowHeight = GestureNavConst.getBottomWindowHeight(this.mContext);
            this.mHoleHeight = GestureNavConst.getStatusBarHeight(this.mContext);
            boolean usingNotch = true;
            if (this.mHasNotch) {
                if (this.mLandscape) {
                    if (!this.mNotchDisplayDisabled && this.mFocusWindowUsingNotch) {
                        z = true;
                    }
                    usingNotch = z;
                } else {
                    usingNotch = true;
                }
                this.mGestureNavLeft.updateViewNotchState(usingNotch);
                this.mGestureNavRight.updateViewNotchState(usingNotch);
                this.mGestureNavAnimProxy.updateViewNotchState(usingNotch);
            }
            boolean usingNotch2 = usingNotch;
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("w=");
                stringBuilder.append(displayWidth);
                stringBuilder.append(", h=");
                stringBuilder.append(displayHeight);
                stringBuilder.append(", backH=");
                stringBuilder.append(backWindowHeight);
                stringBuilder.append(", backW=");
                stringBuilder.append(backWindowWidth);
                stringBuilder.append(", bottomH=");
                stringBuilder.append(bottomWindowHeight);
                stringBuilder.append(", usingNotch=");
                stringBuilder.append(usingNotch2);
                stringBuilder.append(", holeH=");
                stringBuilder.append(this.mHoleHeight);
                Log.d(str, stringBuilder.toString());
            }
            updateViewConfigLocked(displayWidth, displayHeight, backWindowWidth, backWindowHeight, bottomWindowHeight, usingNotch2);
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
        updateNavVisibleLocked(false);
    }

    private void updateNavVisibleLocked(boolean delay) {
        if (this.mWindowViewSetuped) {
            boolean showBack = true;
            boolean showBottom = true;
            if (this.mInKeyguardMainWindow) {
                showBack = false;
                showBottom = false;
            } else {
                if (isFocusWindowBackDisabledLocked()) {
                    showBack = false;
                }
                if (!this.mDeviceProvisioned || !this.mUserSetuped || this.mGuideOrOtaAlive || isFocusWindowBottomDisabledLocked()) {
                    showBottom = false;
                }
            }
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateNavVisible showBack:");
                stringBuilder.append(showBack);
                stringBuilder.append(", showBottom:");
                stringBuilder.append(showBottom);
                Log.i(str, stringBuilder.toString());
            }
            enableBackNavLocked(showBack, delay);
            enableBottomNavLocked(showBottom, delay);
        }
    }

    private void enableBackNavLocked(boolean enable, boolean delay) {
        if (this.mWindowViewSetuped && this.mNavBackEnabled != enable) {
            this.mGestureNavLeft.show(enable, delay);
            this.mGestureNavRight.show(enable, delay);
            this.mNavBackEnabled = enable;
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enableBackNav show:");
                stringBuilder.append(this.mNavBackEnabled);
                stringBuilder.append(", delay:");
                stringBuilder.append(delay);
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    private void enableBottomNavLocked(boolean enable, boolean delay) {
        if (this.mWindowViewSetuped && this.mNavBottomEnabled != enable) {
            this.mGestureNavBottom.show(enable, false);
            this.mNavBottomEnabled = enable;
            if (GestureNavConst.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enableBottomNav show:");
                stringBuilder.append(this.mNavBottomEnabled);
                Log.i(str, stringBuilder.toString());
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
        LayoutParams lp = new LayoutParams(HwArbitrationDEFS.MSG_VPN_STATE_OPEN, 296);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
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
        if (!this.mKeyguardShowing || ((this.mDeviceStateController == null || this.mDeviceStateController.isKeyguardOccluded()) && !GestureNavConst.STATUSBAR_WINDOW.equals(this.mFocusWindowTitle))) {
            return false;
        }
        return true;
    }

    private void updateViewConfigLocked(int displayWidth, int displayHeight, int backWindowWidth, int backWindowHeight, int bottomWindowHeight, boolean usingNotch) {
        int leftViewStartPos = 0;
        int rightViewOffset = 0;
        int viewWidth = displayWidth;
        if (this.mHasNotch && !usingNotch) {
            int i = this.mRotation;
            if (i == 1) {
                leftViewStartPos = this.mHoleHeight;
                viewWidth = displayWidth - this.mHoleHeight;
                rightViewOffset = 0;
            } else if (i == 3) {
                leftViewStartPos = 0;
                viewWidth = displayWidth - this.mHoleHeight;
                rightViewOffset = this.mHoleHeight;
            }
        }
        int leftViewStartPos2 = leftViewStartPos;
        int rightViewOffset2 = rightViewOffset;
        int viewWidth2 = viewWidth;
        viewWidth = displayHeight;
        int i2 = backWindowWidth;
        int i3 = backWindowHeight;
        this.mGestureNavLeft.updateViewConfig(displayWidth, viewWidth, 0, displayHeight - backWindowHeight, i2, i3, leftViewStartPos2, displayHeight - backWindowHeight);
        rightViewOffset = displayWidth;
        this.mGestureNavRight.updateViewConfig(rightViewOffset, viewWidth, (displayWidth - backWindowWidth) - rightViewOffset2, displayHeight - backWindowHeight, i2, i3, (displayWidth - backWindowWidth) - rightViewOffset2, displayHeight - backWindowHeight);
        i2 = viewWidth2;
        int i4 = leftViewStartPos2;
        this.mGestureNavBottom.updateViewConfig(rightViewOffset, viewWidth, 0, displayHeight - bottomWindowHeight, i2, bottomWindowHeight, i4, displayHeight - bottomWindowHeight);
        this.mGestureNavAnimProxy.updateViewConfig(rightViewOffset, viewWidth, 0, 0, i2, displayHeight, i4, 0);
    }

    private boolean updateHomeWindowLocked() {
        String homeWindow = this.mDeviceStateController.getCurrentHomeActivity(this.mCurrentUserId);
        if (homeWindow == null || homeWindow.equals(this.mHomeWindow)) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("newHome=");
            stringBuilder.append(homeWindow);
            stringBuilder.append(", oldHome=");
            stringBuilder.append(this.mHomeWindow);
            Log.i(str, stringBuilder.toString());
        }
        this.mHomeWindow = homeWindow;
        this.mGuideOrOtaAlive = GestureNavConst.STARTUP_GUIDE_HOME_COMPONENT.equals(this.mHomeWindow);
        return true;
    }

    private boolean isSpecialWindowLocked(String windowName) {
        if (this.mHomeWindow == null || !this.mHomeWindow.equals(windowName)) {
            return false;
        }
        return true;
    }

    private boolean isFocusWindowBackDisabledLocked() {
        boolean z = false;
        switch (this.mAppGestureNavLeftMode) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                if (isSpecialWindowLocked(this.mFocusWindowTitle) || (this.mFocusWinNavOptions & HighBitsDetailModeID.MODE_FOLIAGE) != 0) {
                    z = true;
                }
                return z;
        }
    }

    private boolean isFocusWindowBottomDisabledLocked() {
        boolean z = false;
        switch (this.mAppGestureNavBottomMode) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                if ((this.mFocusWinNavOptions & 524288) != 0) {
                    z = true;
                }
                return z;
        }
    }

    private void resetAppGestureNavModeLocked() {
        this.mAppGestureNavLeftMode = 0;
        this.mAppGestureNavRightMode = 0;
        this.mAppGestureNavBottomMode = 0;
    }

    private boolean updateNotchDisplayStateLocked() {
        boolean notchStatus = Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, -2) == 1;
        if (notchStatus == this.mNotchDisplayDisabled) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("newNotch=");
            stringBuilder.append(notchStatus);
            stringBuilder.append(", oldNotch=");
            stringBuilder.append(this.mNotchDisplayDisabled);
            Log.d(str, stringBuilder.toString());
        }
        this.mNotchDisplayDisabled = notchStatus;
        return true;
    }

    private boolean updateDisplayDensity() {
        String densityStr = Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", -2);
        if (densityStr == null || densityStr.equals(this.mDensityStr)) {
            return false;
        }
        if (GestureNavConst.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("newDensity=");
            stringBuilder.append(densityStr);
            stringBuilder.append(", oldDensity=");
            stringBuilder.append(this.mDensityStr);
            Log.d(str, stringBuilder.toString());
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

    public void dump(String prefix, PrintWriter pw, String[] args) {
        if (GestureNavConst.DEBUG_DUMP && this.mGestureNavEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(TAG);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mDeviceProvisioned=");
            stringBuilder.append(this.mDeviceProvisioned);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mUserSetuped=");
            stringBuilder.append(this.mUserSetuped);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mGuideOrOtaAlive=");
            stringBuilder.append(this.mGuideOrOtaAlive);
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mKeyguardShowing=");
            stringBuilder.append(this.mKeyguardShowing);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mInKeyguardMainWindow=");
            stringBuilder.append(this.mInKeyguardMainWindow);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mNotchDisplayDisabled=");
            stringBuilder.append(this.mNotchDisplayDisabled);
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mHomeWindow=");
            stringBuilder.append(this.mHomeWindow);
            pw.println(stringBuilder.toString());
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFocusWindowTitle=");
            stringBuilder.append(this.mFocusWindowTitle);
            pw.println(stringBuilder.toString());
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFocusWindowUsingNotch=");
            stringBuilder.append(this.mFocusWindowUsingNotch);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFocusWinNavOptions=0x");
            stringBuilder.append(Integer.toHexString(this.mFocusWinNavOptions));
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mFocusAppUid=");
            stringBuilder.append(this.mFocusAppUid);
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mAppGestureNavLeftMode=");
            stringBuilder.append(this.mAppGestureNavLeftMode);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mAppGestureNavRightMode=");
            stringBuilder.append(this.mAppGestureNavRightMode);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mAppGestureNavBottomMode=");
            stringBuilder.append(this.mAppGestureNavBottomMode);
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mWindowViewSetuped=");
            stringBuilder.append(this.mWindowViewSetuped);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mNavBackEnabled=");
            stringBuilder.append(this.mNavBackEnabled);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mNavBottomEnabled=");
            stringBuilder.append(this.mNavBottomEnabled);
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mRotation=");
            stringBuilder.append(this.mRotation);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mLandscape=");
            stringBuilder.append(this.mLandscape);
            pw.print(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mDensityStr=");
            stringBuilder.append(this.mDensityStr);
            pw.print(stringBuilder.toString());
            if (this.mHasNotch) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" mHasNotch=");
                stringBuilder.append(this.mHasNotch);
                pw.print(stringBuilder.toString());
            }
            pw.println();
            DumpUtils.dumpAsync(this.mHandler, new Dump() {
                public void dump(PrintWriter pw, String prefix) {
                    StringBuilder stringBuilder;
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavLeft != null) {
                        pw.print(prefix);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("GestureNavLeft=");
                        stringBuilder.append(GestureNavManager.this.mGestureNavLeft.getViewConfig());
                        pw.println(stringBuilder.toString());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavRight != null) {
                        pw.print(prefix);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("GestureNavRight=");
                        stringBuilder.append(GestureNavManager.this.mGestureNavRight.getViewConfig());
                        pw.println(stringBuilder.toString());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mGestureNavBottom != null) {
                        pw.print(prefix);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("GestureNavBottom=");
                        stringBuilder.append(GestureNavManager.this.mGestureNavBottom.getViewConfig());
                        pw.println(stringBuilder.toString());
                    }
                    if (GestureNavManager.this.mGestureNavReady && GestureNavManager.this.mWindowViewSetuped && GestureNavManager.this.mBottomStrategy != null) {
                        GestureNavManager.this.mBottomStrategy.dump(prefix, pw, null);
                    }
                }
            }, pw, prefix, 200);
        }
    }
}
