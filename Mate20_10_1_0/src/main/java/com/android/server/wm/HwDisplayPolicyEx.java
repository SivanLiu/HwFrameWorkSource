package com.android.server.wm;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.swing.HwSwingManager;
import android.swing.IHwSwingService;
import android.util.HwPCUtils;
import android.util.HwSlog;
import android.util.HwStylusUtils;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.server.LocalServices;
import com.android.server.displayside.HwDisplaySideRegionConfig;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.inputmethod.HwInputMethodManagerService;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.policy.HwGameDockGesture;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.IHwPhoneWindowManagerEx;
import com.android.server.policy.NavigationBarPolicy;
import com.android.server.policy.NavigationCallOut;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.server.HwPCFactory;
import com.huawei.server.wm.IHwDisplayPolicyEx;
import com.huawei.server.wm.IHwDisplayPolicyInner;
import huawei.com.android.server.policy.HwFalseTouchMonitor;
import huawei.com.android.server.policy.stylus.StylusGestureListener;
import java.io.PrintWriter;
import java.util.List;

public class HwDisplayPolicyEx implements IHwDisplayPolicyEx {
    static final boolean DEBUG = false;
    private static final int EVENT_DURING_MIN_TIME = 500;
    private static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    public static final int NAVIGATION_BAR_HEIGHT_MAX = 1;
    public static final int NAVIGATION_BAR_HEIGHT_MIN = 0;
    public static final int NAVIGATION_BAR_WIDTH_MAX = 2;
    public static final int NAVIGATION_BAR_WIDTH_MIN = 3;
    public static final int ROTATION_LANDACAPE_DEF = 0;
    public static final int ROTATION_LANDACAPE_OTHER = 1;
    public static final int ROTATION_PORTRAIT_DEF = 2;
    public static final int ROTATION_PORTRAIT_OTHER = 3;
    public static final String TAG = "HwDisplayPolicyEx";
    private static final boolean mIsFactory = SystemProperties.get("ro.runmode", "normal").equals("factory");
    static final boolean mIsHwNaviBar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private static final boolean mSupportGameAssist;
    private boolean isAppWindow = false;
    private Context mContext;
    private DisplayContent mDisplayContent;
    IHwDisplayPolicyInner mDisplayPolicy = null;
    private boolean mFocusWindowUsingNotch = true;
    private HwGameDockGesture mGameDockGesture;
    private GestureNavPolicy mGestureNavPolicy;
    private boolean mInputMethodWindowVisible;
    private boolean mIsDefaultDisplay;
    protected boolean mIsImmersiveMode = false;
    private boolean mIsNavibarHide;
    /* access modifiers changed from: private */
    public String mLastFgPackageName;
    /* access modifiers changed from: private */
    public long mLastKeyPointerTime = 0;
    private WindowManagerPolicyConstants.PointerEventListener mLockScreenBuildInDisplayListener;
    private WindowManagerPolicyConstants.PointerEventListener mLockScreenListener;
    private final BarController mNavigationBarControllerExternal = new BarController("NavigationBarExternal", 0, 134217728, 536870912, Integer.MIN_VALUE, 2, 134217728, (int) AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION);
    private WindowState mNavigationBarExternal = null;
    int mNavigationBarHeightExternal = 0;
    int[] mNavigationBarHeightForRotationMax = new int[4];
    int[] mNavigationBarHeightForRotationMin = new int[4];
    protected NavigationBarPolicy mNavigationBarPolicy = null;
    private int mNavigationBarWidthExternal = 0;
    int[] mNavigationBarWidthForRotationMax = new int[4];
    int[] mNavigationBarWidthForRotationMin = new int[4];
    private NavigationCallOut mNavigationCallOut = null;
    private final WindowManagerService mService;
    private StylusGestureListener mStylusGestureListener = null;
    private StylusGestureListener mStylusGestureListener4PCMode = null;

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.config.gameassist", 0) != 1) {
            z = false;
        }
        mSupportGameAssist = z;
    }

    public void setNavigationBarExternal(WindowState state) {
        this.mNavigationBarExternal = state;
    }

    public WindowState getNavigationBarExternal() {
        return this.mNavigationBarExternal;
    }

    public HwDisplayPolicyEx(WindowManagerService service, IHwDisplayPolicyInner displayPolicy, DisplayContent displayContent, Context context, boolean isDefaultDisplay) {
        this.mService = service;
        this.mContext = context;
        this.mDisplayContent = displayContent;
        this.mDisplayPolicy = displayPolicy;
        this.mIsDefaultDisplay = isDefaultDisplay;
        this.mGestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
        this.mGameDockGesture = (HwGameDockGesture) LocalServices.getService(HwGameDockGesture.class);
    }

    public void registerExternalPointerEventListener() {
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer()) {
            unRegisterExternalPointerEventListener();
            this.mLockScreenListener = new WindowManagerPolicyConstants.PointerEventListener() {
                /* class com.android.server.wm.HwDisplayPolicyEx.AnonymousClass1 */

                public void onPointerEvent(MotionEvent motionEvent) {
                    if (motionEvent.getEventTime() - HwDisplayPolicyEx.this.mLastKeyPointerTime > 500) {
                        long unused = HwDisplayPolicyEx.this.mLastKeyPointerTime = motionEvent.getEventTime();
                        HwDisplayPolicyEx.this.userActivityOnDesktop();
                    }
                }
            };
            this.mService.registerPointerEventListener(this.mLockScreenListener, HwPCUtils.getPCDisplayID());
            this.mLockScreenBuildInDisplayListener = new WindowManagerPolicyConstants.PointerEventListener() {
                /* class com.android.server.wm.HwDisplayPolicyEx.AnonymousClass2 */

                public void onPointerEvent(MotionEvent motionEvent) {
                    if (motionEvent.getEventTime() - HwDisplayPolicyEx.this.mLastKeyPointerTime > 500) {
                        long unused = HwDisplayPolicyEx.this.mLastKeyPointerTime = motionEvent.getEventTime();
                        HwDisplayPolicyEx.this.userActivityOnDesktop();
                    }
                }
            };
            this.mService.registerPointerEventListener(this.mLockScreenBuildInDisplayListener, 0);
        }
    }

    public void unRegisterExternalPointerEventListener() {
        StylusGestureListener stylusGestureListener;
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer()) {
            WindowManagerPolicyConstants.PointerEventListener pointerEventListener = this.mLockScreenListener;
            if (pointerEventListener != null) {
                this.mService.unregisterPointerEventListener(pointerEventListener, HwPCUtils.getPCDisplayID());
                this.mLockScreenListener = null;
            }
            WindowManagerPolicyConstants.PointerEventListener pointerEventListener2 = this.mLockScreenBuildInDisplayListener;
            if (pointerEventListener2 != null) {
                this.mService.unregisterPointerEventListener(pointerEventListener2, 0);
                this.mLockScreenBuildInDisplayListener = null;
            }
        }
        if (HwPCUtils.enabledInPad() && HwStylusUtils.hasStylusFeature(this.mContext) && HwPCUtils.isPcCastModeInServer() && (stylusGestureListener = this.mStylusGestureListener4PCMode) != null) {
            this.mService.unregisterPointerEventListener(stylusGestureListener, HwPCUtils.getPCDisplayID());
            this.mStylusGestureListener4PCMode = null;
        }
    }

    /* access modifiers changed from: private */
    public void userActivityOnDesktop() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.userActivityOnDesktop();
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "RemoteException userActivityOnDesktop");
            }
        }
    }

    private boolean isHardwareKeyboardConnected() {
        Log.i(TAG, "isHardwareKeyboardConnected--begin");
        int[] devices = InputDevice.getDeviceIds();
        boolean isConnected = false;
        int i = 0;
        while (true) {
            if (i >= devices.length) {
                break;
            }
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device != null) {
                if (device.getProductId() != 4817 || device.getVendorId() != 1455) {
                    if (device.isExternal() && (device.getSources() & 257) != 0) {
                        isConnected = true;
                        break;
                    }
                } else {
                    isConnected = true;
                    break;
                }
            }
            i++;
        }
        Log.i(TAG, "isHardwareKeyboardConnected--end");
        return isConnected;
    }

    private boolean isRightKey(int keyCode) {
        if (keyCode >= 7 && keyCode <= 16) {
            return true;
        }
        if (keyCode < 29 || keyCode > 54) {
            return false;
        }
        return true;
    }

    private void setToolType() {
        StylusGestureListener stylusGestureListener;
        if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || (stylusGestureListener = this.mStylusGestureListener4PCMode) == null) {
            StylusGestureListener stylusGestureListener2 = this.mStylusGestureListener;
            if (stylusGestureListener2 != null) {
                stylusGestureListener2.setToolType();
                return;
            }
            return;
        }
        stylusGestureListener.setToolType();
    }

    public void onConfigurationChanged() {
        GestureNavPolicy gestureNavPolicy;
        NavigationBarPolicy navigationBarPolicy = this.mNavigationBarPolicy;
        if (navigationBarPolicy != null) {
            if (navigationBarPolicy.getMinNavigationBar()) {
                this.mDisplayPolicy.setNavigationBarHeightDef((int[]) this.mNavigationBarHeightForRotationMin.clone());
                this.mDisplayPolicy.setNavigationBarWidthDef((int[]) this.mNavigationBarWidthForRotationMin.clone());
            } else {
                this.mDisplayPolicy.setNavigationBarHeightDef((int[]) this.mNavigationBarHeightForRotationMax.clone());
                this.mDisplayPolicy.setNavigationBarWidthDef((int[]) this.mNavigationBarWidthForRotationMax.clone());
            }
        }
        NavigationCallOut navigationCallOut = this.mNavigationCallOut;
        if (navigationCallOut != null) {
            navigationCallOut.updateHotArea();
        }
        if (this.mIsDefaultDisplay && (gestureNavPolicy = this.mGestureNavPolicy) != null) {
            gestureNavPolicy.onConfigurationChanged();
        }
        if (this.mIsDefaultDisplay && this.mGameDockGesture != null && HwGameDockGesture.isGameDockGestureFeatureOn()) {
            this.mGameDockGesture.updateOnConfigurationChange();
        }
        if (this.mService.getPolicy() != null && (this.mService.getPolicy() instanceof HwPhoneWindowManager)) {
            this.mService.getPolicy().onConfigurationChanged();
        }
    }

    private void updateSplitScreenSideSurfaceBox(WindowState focusedWindow) {
        WindowState dividerWindow;
        int windowingMode = focusedWindow.getWindowingMode();
        if (windowingMode == 4 && (dividerWindow = this.mDisplayContent.getDockedDividerController().getDockedStackDividerWindow()) != null) {
            synchronized (dividerWindow.mWinAnimator.mInsetSurfaceLock) {
                if (dividerWindow.mWinAnimator.mSplitScreenSideSurfaceBox != null) {
                    if (isNeedExceptDisplaySideForSplitScreen(windowingMode, focusedWindow)) {
                        dividerWindow.mWinAnimator.mSplitScreenSideSurfaceBox.show();
                    } else {
                        dividerWindow.mWinAnimator.mSplitScreenSideSurfaceBox.destroy();
                    }
                }
            }
        }
    }

    public int focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        GestureNavPolicy gestureNavPolicy;
        if (this.mIsDefaultDisplay && (gestureNavPolicy = this.mGestureNavPolicy) != null) {
            this.mFocusWindowUsingNotch = gestureNavPolicy.onFocusWindowChanged(lastFocus, newFocus);
        }
        if (this.mIsDefaultDisplay && this.mGameDockGesture != null && HwGameDockGesture.isGameDockGestureFeatureOn()) {
            this.mGameDockGesture.updateOnFocusChange(newFocus);
        }
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            int pid = 0;
            String title = null;
            if (!(newFocus == null || newFocus.getAttrs() == null)) {
                pid = newFocus.mSession.mPid;
                title = newFocus.getAttrs().getTitle().toString();
            }
            try {
                hwAft.notifyFocusChange(pid, title);
            } catch (RemoteException e) {
                Log.e(TAG, "binder call hwAft throw " + e);
            }
        }
        NavigationCallOut navigationCallOut = this.mNavigationCallOut;
        if (navigationCallOut != null) {
            navigationCallOut.notifyFocusChange(newFocus);
        }
        IHwSwingService hwSwing = HwSwingManager.getService();
        if (hwSwing != null) {
            String title2 = null;
            String pkgName = null;
            if (!(newFocus == null || newFocus.getAttrs() == null)) {
                title2 = newFocus.getAttrs().getTitle().toString();
                pkgName = newFocus.getOwningPackage();
            }
            try {
                hwSwing.notifyFocusChange(title2, pkgName);
            } catch (RemoteException e2) {
                Log.e(TAG, "binder call hwSwing throw RemoteException");
            }
        }
        if (HwDisplaySizeUtil.hasSideInScreen() && newFocus != null) {
            this.mService.getPolicy().notchControlFilletForSideScreen(newFocus, false);
            updateSplitScreenSideSurfaceBox(newFocus);
        }
        HwFalseTouchMonitor.getInstance().handleFocusChanged(lastFocus, newFocus);
        return 0;
    }

    public void layoutWindowLw(WindowState win, WindowState attached, WindowState focusedWindow, boolean layoutBeyondDisplayCutout) {
        GestureNavPolicy gestureNavPolicy;
        if (IS_NOTCH_PROP && this.mIsDefaultDisplay && (gestureNavPolicy = this.mGestureNavPolicy) != null && gestureNavPolicy.isGestureNavStartedNotLocked() && isFocusedWindow(win, focusedWindow)) {
            boolean oldUsingNotch = this.mFocusWindowUsingNotch;
            this.mFocusWindowUsingNotch = !layoutBeyondDisplayCutout;
            boolean z = this.mFocusWindowUsingNotch;
            if (oldUsingNotch != z) {
                this.mGestureNavPolicy.onLayoutInDisplayCutoutModeChanged(win, oldUsingNotch, z);
            }
        }
    }

    private boolean isFocusedWindow(WindowState win, WindowState focusedWindow) {
        if (win == null || focusedWindow == null || win.getAttrs() == null || focusedWindow.getAttrs() == null) {
            return false;
        }
        return focusedWindow.getAttrs().getTitle().toString().equals(win.getAttrs().getTitle().toString());
    }

    public void systemReadyEx() {
        if (mIsHwNaviBar && this.mService.getPolicy() != null && (this.mService.getPolicy() instanceof PhoneWindowManager)) {
            this.mNavigationBarPolicy = new NavigationBarPolicy(this.mContext, this.mService.getPolicy());
            WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs = this.mService.getPolicy().getWindowManagerFuncs();
            windowManagerFuncs.registerPointerEventListener(new WindowManagerPolicyConstants.PointerEventListener() {
                /* class com.android.server.wm.HwDisplayPolicyEx.AnonymousClass3 */

                public void onPointerEvent(MotionEvent motionEvent) {
                    if (HwDisplayPolicyEx.this.mNavigationBarPolicy != null) {
                        HwDisplayPolicyEx.this.mNavigationBarPolicy.addPointerEvent(motionEvent);
                    }
                }
            }, 0);
            if (this.mNavigationCallOut == null && this.mIsDefaultDisplay) {
                this.mNavigationCallOut = new NavigationCallOut(this.mContext, this.mService.getPolicy(), windowManagerFuncs);
            }
        }
        if (mSupportGameAssist) {
            ActivityManagerEx.registerGameObserver(new IGameObserver.Stub() {
                /* class com.android.server.wm.HwDisplayPolicyEx.AnonymousClass4 */

                public void onGameStatusChanged(String packageName, int event) {
                    Log.i(HwDisplayPolicyEx.TAG, "currentFgApp=" + packageName + ", mLastFgPackageName=" + HwDisplayPolicyEx.this.mLastFgPackageName);
                    if (!(packageName == null || packageName.equals(HwDisplayPolicyEx.this.mLastFgPackageName) || HwDisplayPolicyEx.this.mNavigationBarPolicy == null)) {
                        HwDisplayPolicyEx.this.mNavigationBarPolicy.setEnableSwipeInCurrentGameApp(false);
                        Log.i(HwDisplayPolicyEx.TAG, "setEnableSwipeInCurrentGameApp false");
                    }
                    String unused = HwDisplayPolicyEx.this.mLastFgPackageName = packageName;
                }

                public void onGameListChanged() {
                }
            });
        }
    }

    public int getNaviBarHeightForRotationMin(int index) {
        return this.mNavigationBarHeightForRotationMin[index];
    }

    public int getNaviBarWidthForRotationMin(int index) {
        return this.mNavigationBarWidthForRotationMin[index];
    }

    public int getNaviBarHeightForRotationMax(int index) {
        return this.mNavigationBarHeightForRotationMax[index];
    }

    public int getNaviBarWidthForRotationMax(int index) {
        return this.mNavigationBarWidthForRotationMax[index];
    }

    public boolean isNaviBarMini() {
        NavigationBarPolicy navigationBarPolicy = this.mNavigationBarPolicy;
        if (navigationBarPolicy == null || !navigationBarPolicy.getMinNavigationBar()) {
            return false;
        }
        return true;
    }

    public void setNaviBarFlag(boolean flag) {
        if (flag != this.mIsNavibarHide) {
            this.mIsNavibarHide = flag;
            HwSlog.d(TAG, "setNeedHideWindow setFlag isNavibarHide is " + this.mIsNavibarHide);
        }
    }

    public boolean getNaviBarFlag() {
        return this.mIsNavibarHide;
    }

    public void setNaviImmersiveMode(boolean mode) {
        NavigationBarPolicy navigationBarPolicy = this.mNavigationBarPolicy;
        if (navigationBarPolicy != null) {
            navigationBarPolicy.setImmersiveMode(mode);
        }
        this.mIsImmersiveMode = mode;
    }

    public boolean getImmersiveMode() {
        return this.mIsImmersiveMode;
    }

    public void setInputMethodWindowVisible(boolean visible) {
        this.mInputMethodWindowVisible = visible;
    }

    public NavigationBarPolicy getNavigationBarPolicy() {
        return this.mNavigationBarPolicy;
    }

    public int getRotationValueByType(int type) {
        if (type == 0) {
            return this.mDisplayContent.getDisplayRotation().getLandscapeRotation();
        }
        if (type == 1) {
            return this.mDisplayContent.getDisplayRotation().getSeascapeRotation();
        }
        if (type == 2) {
            return this.mDisplayContent.getDisplayRotation().getPortraitRotation();
        }
        if (type != 3) {
            return 0;
        }
        return this.mDisplayContent.getDisplayRotation().getUpsideDownRotation();
    }

    public int[] getNavigationBarValueForRotation(int index) {
        if (index == 0) {
            return (int[]) this.mNavigationBarHeightForRotationMin.clone();
        }
        if (index == 1) {
            return (int[]) this.mNavigationBarHeightForRotationMax.clone();
        }
        if (index == 2) {
            return (int[]) this.mNavigationBarWidthForRotationMax.clone();
        }
        if (index != 3) {
            return new int[4];
        }
        return (int[]) this.mNavigationBarWidthForRotationMin.clone();
    }

    public void setNavigationBarValueForRotation(int index, int type, int value) {
        if (index == 0) {
            this.mNavigationBarHeightForRotationMin[type] = value;
        } else if (index == 1) {
            this.mNavigationBarHeightForRotationMax[type] = value;
        } else if (index == 2) {
            this.mNavigationBarWidthForRotationMax[type] = value;
        } else if (index == 3) {
            this.mNavigationBarWidthForRotationMin[type] = value;
        }
    }

    public boolean computeNaviBarFlag() {
        boolean forceNavibar;
        WindowManager.LayoutParams focusAttrs = this.mDisplayPolicy.getFocusedWindow() != null ? this.mDisplayPolicy.getFocusedWindow().getAttrs() : null;
        int type = focusAttrs != null ? focusAttrs.type : 0;
        if (focusAttrs != null) {
            forceNavibar = (focusAttrs.hwFlags & 1) == 1;
        } else {
            forceNavibar = false;
        }
        boolean keyguardOn = type == 2101 || type == 2100;
        boolean iskeyguardDialog = type == 2009 && this.mService.getPolicy().isKeyGuardOn();
        boolean dreamOn = focusAttrs != null && focusAttrs.type == 2023;
        boolean isNeedHideNaviBarWin = (focusAttrs == null || (focusAttrs.privateFlags & Integer.MIN_VALUE) == 0) ? false : true;
        IHwPhoneWindowManagerEx hwPWMEx = this.mService.getPolicy().getPhoneWindowManagerEx();
        if (hwPWMEx != null && hwPWMEx.getFPAuthState()) {
            Log.i(TAG, "in fingerprint authentication,hide nav bar");
            return true;
        } else if (this.mDisplayPolicy.getStatusBar() == this.mDisplayPolicy.getFocusedWindow()) {
            return false;
        } else {
            if (iskeyguardDialog && !forceNavibar) {
                return true;
            }
            if (dreamOn) {
                return false;
            }
            if (keyguardOn || isNeedHideNaviBarWin) {
                return true;
            }
            if (!getNaviBarFlag() || this.mInputMethodWindowVisible) {
                return false;
            }
            return true;
        }
    }

    public void updateNavigationBar(boolean minNaviBar) {
        if (minNaviBar) {
            this.mDisplayPolicy.setNavigationBarHeightDef(getNavigationBarValueForRotation(0));
            this.mDisplayPolicy.setNavigationBarWidthDef(getNavigationBarValueForRotation(3));
        } else {
            HwSlog.d(TAG, "updateNavigationBar navigationbar mode: " + SystemProperties.getInt("persist.sys.navigationbar.mode", 0));
            Resources res = this.mContext.getResources();
            setNavigationBarValueForRotation(1, getRotationValueByType(2), res.getDimensionPixelSize(17105305));
            setNavigationBarValueForRotation(1, getRotationValueByType(3), res.getDimensionPixelSize(17105305));
            setNavigationBarValueForRotation(1, getRotationValueByType(0), res.getDimensionPixelSize(17105307));
            setNavigationBarValueForRotation(1, getRotationValueByType(1), res.getDimensionPixelSize(17105307));
            setNavigationBarValueForRotation(2, getRotationValueByType(2), res.getDimensionPixelSize(17105310));
            setNavigationBarValueForRotation(2, getRotationValueByType(3), res.getDimensionPixelSize(17105310));
            setNavigationBarValueForRotation(2, getRotationValueByType(0), res.getDimensionPixelSize(17105310));
            setNavigationBarValueForRotation(2, getRotationValueByType(1), res.getDimensionPixelSize(17105310));
            this.mDisplayPolicy.setNavigationBarHeightDef(getNavigationBarValueForRotation(1));
            this.mDisplayPolicy.setNavigationBarWidthDef(getNavigationBarValueForRotation(2));
        }
        this.mNavigationBarPolicy.updateNavigationBar(minNaviBar);
    }

    public void initialNavigationSize(Display display, int width, int height, int density) {
        if (density == 0) {
            Log.e(TAG, "density is 0");
        } else if (this.mContext != null) {
            initNavigationBarHightExternal(display, width, height);
            Resources res = this.mContext.getResources();
            ContentResolver resolver = this.mContext.getContentResolver();
            int[] iArr = this.mNavigationBarHeightForRotationMax;
            int portraitRotation = this.mDisplayContent.getDisplayRotation().getPortraitRotation();
            int[] iArr2 = this.mNavigationBarHeightForRotationMax;
            int upsideDownRotation = this.mDisplayContent.getDisplayRotation().getUpsideDownRotation();
            int dimensionPixelSize = res.getDimensionPixelSize(17105305);
            iArr2[upsideDownRotation] = dimensionPixelSize;
            iArr[portraitRotation] = dimensionPixelSize;
            int[] iArr3 = this.mNavigationBarHeightForRotationMax;
            int landscapeRotation = this.mDisplayContent.getDisplayRotation().getLandscapeRotation();
            int[] iArr4 = this.mNavigationBarHeightForRotationMax;
            int seascapeRotation = this.mDisplayContent.getDisplayRotation().getSeascapeRotation();
            int dimensionPixelSize2 = res.getDimensionPixelSize(17105307);
            iArr4[seascapeRotation] = dimensionPixelSize2;
            iArr3[landscapeRotation] = dimensionPixelSize2;
            int[] iArr5 = this.mNavigationBarHeightForRotationMin;
            int portraitRotation2 = this.mDisplayContent.getDisplayRotation().getPortraitRotation();
            int[] iArr6 = this.mNavigationBarHeightForRotationMin;
            int upsideDownRotation2 = this.mDisplayContent.getDisplayRotation().getUpsideDownRotation();
            int[] iArr7 = this.mNavigationBarHeightForRotationMin;
            int landscapeRotation2 = this.mDisplayContent.getDisplayRotation().getLandscapeRotation();
            int[] iArr8 = this.mNavigationBarHeightForRotationMin;
            int seascapeRotation2 = this.mDisplayContent.getDisplayRotation().getSeascapeRotation();
            int i = Settings.System.getInt(resolver, "navigationbar_height_min", 0);
            iArr8[seascapeRotation2] = i;
            iArr7[landscapeRotation2] = i;
            iArr6[upsideDownRotation2] = i;
            iArr5[portraitRotation2] = i;
            int[] iArr9 = this.mNavigationBarWidthForRotationMax;
            int portraitRotation3 = this.mDisplayContent.getDisplayRotation().getPortraitRotation();
            int[] iArr10 = this.mNavigationBarWidthForRotationMax;
            int upsideDownRotation3 = this.mDisplayContent.getDisplayRotation().getUpsideDownRotation();
            int[] iArr11 = this.mNavigationBarWidthForRotationMax;
            int landscapeRotation3 = this.mDisplayContent.getDisplayRotation().getLandscapeRotation();
            int[] iArr12 = this.mNavigationBarWidthForRotationMax;
            int seascapeRotation3 = this.mDisplayContent.getDisplayRotation().getSeascapeRotation();
            int dimensionPixelSize3 = res.getDimensionPixelSize(17105310);
            iArr12[seascapeRotation3] = dimensionPixelSize3;
            iArr11[landscapeRotation3] = dimensionPixelSize3;
            iArr10[upsideDownRotation3] = dimensionPixelSize3;
            iArr9[portraitRotation3] = dimensionPixelSize3;
            int[] iArr13 = this.mNavigationBarWidthForRotationMin;
            int portraitRotation4 = this.mDisplayContent.getDisplayRotation().getPortraitRotation();
            int[] iArr14 = this.mNavigationBarWidthForRotationMin;
            int upsideDownRotation4 = this.mDisplayContent.getDisplayRotation().getUpsideDownRotation();
            int[] iArr15 = this.mNavigationBarWidthForRotationMin;
            int landscapeRotation4 = this.mDisplayContent.getDisplayRotation().getLandscapeRotation();
            int[] iArr16 = this.mNavigationBarWidthForRotationMin;
            int seascapeRotation4 = this.mDisplayContent.getDisplayRotation().getSeascapeRotation();
            int i2 = Settings.System.getInt(resolver, "navigationbar_width_min", 0);
            iArr16[seascapeRotation4] = i2;
            iArr15[landscapeRotation4] = i2;
            iArr14[upsideDownRotation4] = i2;
            iArr13[portraitRotation4] = i2;
        }
    }

    public void showTopBar(Handler handler, int displayId) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            handler.postDelayed(new Runnable() {
                /* class com.android.server.wm.HwDisplayPolicyEx.AnonymousClass5 */

                public void run() {
                    if (!HwDisplayPolicyEx.this.isCloudOnPCTOP()) {
                        try {
                            IHwPCManager pcManager = HwPCUtils.getHwPCManager();
                            if (pcManager != null) {
                                pcManager.showTopBar();
                            }
                        } catch (Exception e) {
                            Log.e(HwDisplayPolicyEx.TAG, "RemoteException");
                        }
                    }
                }
            }, 200);
        }
    }

    /* access modifiers changed from: private */
    public boolean isCloudOnPCTOP() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningTasks(1);
            if (tasks != null) {
                if (!tasks.isEmpty()) {
                    for (ActivityManager.RunningTaskInfo info : tasks) {
                        if (info.topActivity != null) {
                            if (info.baseActivity != null) {
                                if ("com.huawei.cloud".equals(info.topActivity.getPackageName()) && "com.huawei.cloud".equals(info.baseActivity.getPackageName()) && HwPCUtils.isPcDynamicStack(info.stackId) && "com.huawei.ahdp.session.VmActivity".equals(info.topActivity.getClassName())) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                    return false;
                }
            }
            return false;
        } catch (RuntimeException e) {
            HwPCUtils.log(TAG, "isCloudOnPCTOP->RuntimeException happened");
        } catch (Exception e2) {
            HwPCUtils.log(TAG, "isCloudOnPCTOP->other exception happened");
        }
    }

    private int getNavigationBarWidthExternal() {
        return this.mNavigationBarWidthExternal;
    }

    /* access modifiers changed from: protected */
    public int getNavigationBarHeightExternal() {
        return this.mNavigationBarHeightExternal;
    }

    public void resetCurrentNaviBarHeightExternal() {
        HwPCUtils.log(TAG, "resetCurrentNaviBarHeightExternal");
        if (HwPCUtils.enabled() && this.mNavigationBarHeightExternal != 0) {
            this.mNavigationBarHeightExternal = 0;
            this.mNavigationBarWidthExternal = 0;
        }
    }

    private void initNavigationBarHightExternal(Display display, int width, int height) {
        if (display == null || this.mContext == null) {
            Log.e(TAG, "fail to ini nav, display or context is null");
        } else if (HwPCUtils.enabled() && HwPCUtils.isValidExtDisplayId(display.getDisplayId())) {
            Context externalContext = this.mContext.createDisplayContext(display);
            if (HwPCUtils.isHiCarCastMode()) {
                this.mNavigationBarWidthExternal = getHwHiCarMultiWindowManager().getAppDockWidth();
                this.mNavigationBarHeightExternal = getHwHiCarMultiWindowManager().getAppDockHeight();
            } else {
                this.mNavigationBarHeightExternal = externalContext.getResources().getDimensionPixelSize(34472195);
            }
            HwPCUtils.log(TAG, "mNavigationBarWidthExternal = " + this.mNavigationBarWidthExternal + " mNavigationBarHeightExternal = " + this.mNavigationBarHeightExternal);
        }
    }

    public void removeWindowForPC(WindowState win) {
        if (!HwPCUtils.enabled()) {
            return;
        }
        if (getNavigationBarExternal() == win) {
            setNavigationBarExternal(null);
            this.mNavigationBarControllerExternal.setWindow((WindowState) null);
        } else if (this.mService.getPolicy() instanceof HwPhoneWindowManager) {
            HwPhoneWindowManager policy = this.mService.getPolicy();
            if (policy.mLighterDrawView == win) {
                policy.mLighterDrawView = null;
            }
        }
    }

    public void dumpPC(String prefix, PrintWriter pw) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (getNavigationBarExternal() != null) {
                pw.print(prefix);
                pw.print("mNavigationBarExternal=");
                pw.println(getNavigationBarExternal());
            }
            BarController barController = this.mNavigationBarControllerExternal;
            if (barController != null) {
                barController.dump(pw, prefix);
            }
        }
    }

    public int prepareAddWindowForPC(WindowState win, WindowManager.LayoutParams attrs) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return -10;
        }
        if ((attrs.type == 2019 || (HwPCUtils.isHiCarCastMode() && getHwHiCarMultiWindowManager().isHiCarNavigationBar(attrs))) && HwPCUtils.isValidExtDisplayId(win.getDisplayId())) {
            if (getNavigationBarExternal() != null && getNavigationBarExternal().isAlive()) {
                return -7;
            }
            setNavigationBarExternal(win);
            this.mNavigationBarControllerExternal.setWindow(win);
            return 0;
        } else if (attrs.type != 2104 || !HwPCUtils.isValidExtDisplayId(win.getDisplayId())) {
            return -10;
        } else {
            if (this.mService.getPolicy() instanceof HwPhoneWindowManager) {
                HwPhoneWindowManager policy = this.mService.getPolicy();
                if (policy.mLighterDrawView != null) {
                    return -7;
                }
                policy.mLighterDrawView = win;
            }
            return 0;
        }
    }

    public boolean getStableInsetsForPC(Rect outInsets, int displayId) {
        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId)) {
            return false;
        }
        outInsets.setEmpty();
        getNonDecorInsetsForPC(outInsets, displayId);
        outInsets.top = 0;
        return true;
    }

    public boolean getNonDecorInsetsForPC(Rect outInsets, int displayId) {
        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId)) {
            return false;
        }
        outInsets.setEmpty();
        if (!this.mDisplayContent.getDisplayPolicy().hasNavigationBar()) {
            return true;
        }
        if (getNavigationBarExternal() == null || !getNavigationBarExternal().isVisibleLw()) {
            outInsets.bottom = 0;
            return true;
        }
        if (HwPCUtils.isHiCarCastMode()) {
            outInsets.left = getNavigationBarWidthExternal();
        }
        outInsets.bottom = getNavigationBarHeightExternal();
        return true;
    }

    public void beginLayoutForPC(DisplayFrames displayFrames) {
        Context context;
        if (HwPCUtils.isPcCastModeInServer() && getNavigationBarExternal() != null && HwPCUtils.isValidExtDisplayId(displayFrames.mDisplayId)) {
            if (this.mNavigationBarHeightExternal == 0 && (context = this.mContext) != null) {
                initNavigationBarHightExternal(((DisplayManager) context.getSystemService("display")).getDisplay(displayFrames.mDisplayId), displayFrames.mDisplayWidth, displayFrames.mDisplayHeight);
            }
            layoutNavigationBarExternal(displayFrames.mDisplayHeight, displayFrames.mDisplayWidth, 0, displayFrames);
            if (HwPCUtils.enabledInPad() && this.mDisplayContent.getDisplayPolicy().getStatusBar() != null) {
                if (this.mService.isKeyguardLocked()) {
                    this.mDisplayContent.getDisplayPolicy().getStatusBar().computeFrameLw();
                } else {
                    this.mDisplayContent.getDisplayPolicy().getStatusBar().hideLw(false);
                }
            }
        }
    }

    private boolean layoutNavigationBarExternal(int displayHeight, int displayWidth, int overscanBottom, DisplayFrames displayFrames) {
        int top;
        Rect navigationFrame = new Rect();
        if (!HwPCUtils.isHiCarCastMode() || !getHwHiCarMultiWindowManager().isRotationLandscape()) {
            if (getNavigationBarExternal().isVisibleLw()) {
                top = (displayHeight - overscanBottom) - getNavigationBarHeightExternal();
            } else {
                top = displayHeight - overscanBottom;
            }
            navigationFrame.set(0, top, displayWidth, displayHeight - overscanBottom);
            Rect rect = displayFrames.mStable;
            displayFrames.mStableFullscreen.bottom = top;
            rect.bottom = top;
            this.mNavigationBarControllerExternal.setBarShowingLw(true);
            displayFrames.mDock.bottom = displayFrames.mStable.bottom;
            displayFrames.mRestricted.bottom = displayFrames.mStable.bottom;
            displayFrames.mRestrictedOverscan.bottom = displayFrames.mDock.bottom;
            displayFrames.mSystem.bottom = displayFrames.mStable.bottom;
        } else {
            navigationFrame.set(0, 0, getNavigationBarWidthExternal(), displayHeight);
            Rect rect2 = displayFrames.mStable;
            Rect rect3 = displayFrames.mStableFullscreen;
            int i = navigationFrame.right;
            rect3.left = i;
            rect2.left = i;
            this.mNavigationBarControllerExternal.setBarShowingLw(true);
            displayFrames.mDock.left = displayFrames.mStable.left;
            displayFrames.mRestricted.left = displayFrames.mStable.left;
            displayFrames.mRestrictedOverscan.left = displayFrames.mDock.left;
            displayFrames.mSystem.left = displayFrames.mStable.left;
        }
        displayFrames.mContent.set(displayFrames.mDock);
        displayFrames.mVoiceContent.set(displayFrames.mDock);
        displayFrames.mCurrent.set(displayFrames.mDock);
        getNavigationBarExternal().getVisibleFrameLw().set(navigationFrame);
        getNavigationBarExternal().getContentFrameLw().set(navigationFrame);
        getNavigationBarExternal().getStableFrameLw().set(navigationFrame);
        getNavigationBarExternal().getParentFrame().set(navigationFrame);
        getNavigationBarExternal().getDisplayFrameLw().set(navigationFrame);
        getNavigationBarExternal().computeFrameLw();
        return false;
    }

    public boolean layoutWindowForPCNavigationBar(WindowState win) {
        return HwPCUtils.isPcCastModeInServer() && getNavigationBarExternal() == win;
    }

    public boolean focusChangedLwForPC(WindowState newFocus) {
        return HwPCUtils.isPcCastModeInServer() && newFocus != null && HwPCUtils.isValidExtDisplayId(newFocus.getDisplayId());
    }

    public void updateWindowFramesForPC(WindowFrames windowFrames, Rect pf, Rect df, Rect cf, Rect vf, boolean isPCDisplay) {
        if (HwPCUtils.isPcCastModeInServer() && isPCDisplay) {
            windowFrames.mParentFrame.set(pf);
            windowFrames.mDisplayFrame.set(df);
            windowFrames.mContentFrame.set(cf);
            windowFrames.mVisibleFrame.set(vf);
        }
    }

    public boolean isGestureIsolated(WindowState focusedWindow, WindowState topFullscreenOpaqueWindowState) {
        WindowState win = focusedWindow != null ? focusedWindow : topFullscreenOpaqueWindowState;
        if (win == null || (win.getAttrs().hwFlags & 512) != 512) {
            return false;
        }
        return true;
    }

    public boolean swipeFromBottom() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
            return true;
        }
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null && gestureNavPolicy.isGestureNavStartedNotLocked() && !this.mGestureNavPolicy.isKeyNavEnabled()) {
            return true;
        }
        DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
        if (!mIsHwNaviBar || !displayPolicy.isLastImmersiveMode() || displayPolicy.getNavigationBar() == null || displayPolicy.getNavBarPosition() != 4) {
            return false;
        }
        NavigationBarPolicy navigationBarPolicy = this.mNavigationBarPolicy;
        if (navigationBarPolicy == null || !navigationBarPolicy.getGameControlReslut(2)) {
            displayPolicy.requestHwTransientBars(displayPolicy.getNavigationBar());
        }
        return true;
    }

    public boolean swipeFromRight() {
        DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null && gestureNavPolicy.isGestureNavStartedNotLocked() && !this.mGestureNavPolicy.isKeyNavEnabled()) {
            if (GestureNavConst.IS_SUPPORT_FULL_BACK) {
                if (this.mGestureNavPolicy.isPointInExcludedRegion(displayPolicy.getGestureStartedPoint()) && displayPolicy.getStatusBar() != null) {
                    displayPolicy.requestHwTransientBars(displayPolicy.getStatusBar());
                }
            }
            return true;
        } else if (!mIsHwNaviBar || !displayPolicy.isLastImmersiveMode() || displayPolicy.getNavigationBar() == null || displayPolicy.getNavBarPosition() == 4) {
            return false;
        } else {
            NavigationBarPolicy navigationBarPolicy = this.mNavigationBarPolicy;
            if (navigationBarPolicy == null || !navigationBarPolicy.getGameControlReslut(3)) {
                displayPolicy.requestHwTransientBars(displayPolicy.getNavigationBar());
            }
            return true;
        }
    }

    public boolean swipeFromLeft() {
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy == null || !gestureNavPolicy.isGestureNavStartedNotLocked() || this.mGestureNavPolicy.isKeyNavEnabled() || !GestureNavConst.IS_SUPPORT_FULL_BACK) {
            return false;
        }
        DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
        if (!this.mGestureNavPolicy.isPointInExcludedRegion(displayPolicy.getGestureStartedPoint()) || displayPolicy.getStatusBar() == null) {
            return false;
        }
        displayPolicy.requestHwTransientBars(displayPolicy.getStatusBar());
        return false;
    }

    public void onPointDown() {
        HwPhoneWindowManager policy;
        if ((this.mService.getPolicy() instanceof HwPhoneWindowManager) && (policy = this.mService.getPolicy()) != null) {
            policy.onPointDown();
        }
    }

    public int getNonDecorDisplayHeight(int fullHeight, int displayId) {
        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId) || getNavigationBarExternal() == null || !getNavigationBarExternal().isVisibleLw()) {
            return fullHeight;
        }
        return fullHeight - getNavigationBarHeightExternal();
    }

    public void onLockTaskStateChangedLw(int lockTaskState) {
        NavigationCallOut navigationCallOut;
        GestureNavPolicy gestureNavPolicy;
        if (this.mIsDefaultDisplay && (gestureNavPolicy = this.mGestureNavPolicy) != null) {
            gestureNavPolicy.onLockTaskStateChanged(lockTaskState);
        }
        if (this.mIsDefaultDisplay && (navigationCallOut = this.mNavigationCallOut) != null) {
            navigationCallOut.updateLockTaskState(lockTaskState);
        }
    }

    public int getNonDecorDisplayWidthForExtraDisplay(int fullWidth, int displayId) {
        if (!HwPCUtils.isHiCarCastMode() || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId)) {
            return -1;
        }
        return fullWidth - getNavigationBarWidthExternal();
    }

    public int getInputMethodRightForHwMultiDisplay(int dockLeft, int dockRight) {
        if (HwPCUtils.isHiCarCastMode()) {
            return getHwHiCarMultiWindowManager().getInputMethodWidth() + dockLeft;
        }
        return dockRight;
    }

    public void addPointerEvent(MotionEvent motionEvent) {
        this.mNavigationBarPolicy.addPointerEvent(motionEvent);
    }

    public boolean isAppNeedExpand(String packageName) {
        return HwDisplaySideRegionConfig.getInstance().isExtendApp(packageName);
    }

    public boolean isSystemApp(String packageName) {
        for (String pName : HwNotchScreenWhiteConfig.getInstance().getNotchSystemApps()) {
            if (packageName.equals(pName)) {
                return true;
            }
        }
        return false;
    }

    public void updateDisplayFrames(WindowState win, DisplayFrames displayFrames, int systemUiFlags, Rect cf, int navHeight) {
        if (win != null && displayFrames != null && cf != null && canUpdateDisplayFrames(win, win.getAttrs(), systemUiFlags) && displayFrames.mRotation == 1) {
            cf.left = 0;
        }
    }

    public boolean canUpdateDisplayFrames(WindowState win, WindowManager.LayoutParams attrs, int systemUiFlags) {
        if (win == null || attrs == null) {
            return false;
        }
        boolean isFullScreen = ((attrs.flags & 1024) != 0) || ((systemUiFlags & 4) != 0 && !win.toString().contains(GestureNavConst.STATUSBAR_WINDOW));
        if (((win.isWindowUsingNotch() && win.getAttrs().layoutInDisplayCutoutMode != 1) || isFullScreen) && !win.inHwMagicWindowingMode()) {
            return false;
        }
        return true;
    }

    private boolean isNeedExceptDisplaySideForExactWindow(WindowState win) {
        WindowManager.LayoutParams attrs = win.getAttrs();
        if ("com.huawei.android.launcher/com.huawei.android.launcher.splitscreen.SplitScreenAppActivity".equals(attrs.getTitle()) || HwInputMethodManagerService.SECURE_IME_PACKAGENAME.equals(attrs.packageName)) {
            return false;
        }
        if (!isSystemApp(attrs.packageName)) {
            return !isAppNeedExpand(attrs.packageName);
        }
        if (attrs.layoutInDisplaySideMode != 1) {
            return true;
        }
        return false;
    }

    private boolean isNeedExceptDisplaySideForSplitScreen(int splitWindowMode, WindowState win) {
        Task topTask;
        WindowState topWindow;
        if (isNeedExceptDisplaySideForExactWindow(win)) {
            return true;
        }
        int anotherSplitWindowMode = 3;
        if (splitWindowMode == 3) {
            anotherSplitWindowMode = 4;
        }
        TaskStack anotherSplitStack = this.mDisplayContent.getTopStackInWindowingMode(anotherSplitWindowMode);
        if (anotherSplitStack == null || (topTask = anotherSplitStack.getTopChild()) == null || (topWindow = topTask.getTopVisibleAppMainWindow()) == null) {
            return false;
        }
        return isNeedExceptDisplaySideForExactWindow(topWindow);
    }

    private boolean isNeedExceptDisplaySideInternal(WindowManager.LayoutParams attrs, WindowState win, int displayRotation) {
        if (!HwDisplaySizeUtil.hasSideInScreen() || mIsFactory || "com.huawei.mmitest".equals(attrs.packageName) || win.getAttrs().type == 2034 || this.mService.mPolicy.isKeyguardLockedAndOccluded() || "VolumeIndex".equals(win.getAttrs().getTitle())) {
            return false;
        }
        if (win.getAttrs().type == 3 && win.getAttrs().getTitle() != null && win.getAttrs().getTitle().toString().startsWith("SnapshotStartingWindow")) {
            return false;
        }
        if (this.mService.getLazyMode() != 0 && !win.toString().contains("hwSingleMode_window")) {
            return false;
        }
        if (displayRotation == 1 || displayRotation == 3) {
            return true;
        }
        int windowingMode = win.getWindowingMode();
        if (windowingMode == 3 || windowingMode == 4) {
            return isNeedExceptDisplaySideForSplitScreen(windowingMode, win);
        }
        return isNeedExceptDisplaySideForExactWindow(win);
    }

    public boolean isNeedExceptDisplaySide(WindowManager.LayoutParams attrs, WindowState win, int displayRotation) {
        return isNeedExceptDisplaySideInternal(attrs, win, displayRotation);
    }

    private DefaultHwHiCarMultiWindowManager getHwHiCarMultiWindowManager() {
        return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwHiCarMultiWindowManager();
    }
}
