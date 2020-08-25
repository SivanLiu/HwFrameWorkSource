package com.android.server.wm;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.freeform.HwFreeFormUtils;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.HwFoldScreenState;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.CoordinationModeUtils;
import android.util.DisplayMetrics;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.DragEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.RemoteViews;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.input.HwInputManagerService;
import com.android.server.magicwin.HwMagicWinAnimation;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.feature.StartWindowFeature;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.rms.iaware.qos.AwareBinderSchedManager;
import com.google.android.collect.Sets;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import com.huawei.android.view.IHwMultiDisplayBitmapDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStateListener;
import com.huawei.android.view.IHwMultiDisplayDropStartListener;
import com.huawei.android.view.IHwMultiDisplayDroppableListener;
import com.huawei.android.view.IHwMultiDisplayPhoneOperateListener;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.server.HwPCFactory;
import com.huawei.server.hwmultidisplay.DefaultHwMultiDisplayUtils;
import com.huawei.server.hwmultidisplay.windows.DefaultHwWindowsCastManager;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.hwutil.HwFullScreenDisplay;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class HwWindowManagerServiceEx implements IHwWindowManagerServiceEx {
    private static final int ANIMATION_SIZE = 2;
    private static final int APP_ASSOC_WINDOWADD = 8;
    private static final int APP_ASSOC_WINDOWDEL = 9;
    private static final int APP_ASSOC_WINDOWUPDATE = 27;
    private static final int APP_ASSOC_WINDOWUPOPS = 10;
    private static final String ASSOC_PKGNAME = "pkgname";
    private static final String ASSOC_RELATION_TYPE = "relationType";
    private static final String ASSOC_UID = "uid";
    private static final String ASSOC_WINDOW = "window";
    private static final String ASSOC_WINDOW_ALPHA = "alpha";
    private static final String ASSOC_WINDOW_HASHCODE = "hashcode";
    private static final String ASSOC_WINDOW_HAS_SURFACE = "hasSurface";
    private static final String ASSOC_WINDOW_HEIGHT = "height";
    private static final String ASSOC_WINDOW_MODE = "windowmode";
    private static final String ASSOC_WINDOW_PHIDE = "permanentlyhidden";
    private static final String ASSOC_WINDOW_TYPE = "windowtype";
    private static final String ASSOC_WINDOW_WIDTH = "width";
    private static final int DRAG_ACTION_CLEAR_SAVED_WINDOWSTATE = 10001;
    private static final int DRAG_ACTION_SAVE_WINDOWSTATE = 10000;
    private static final int ENTER_INDEX = 0;
    private static final int EXIT_INDEX = 1;
    private static final int FORBIDDEN_ADDVIEW_BROADCAST = 1;
    private static final int HALF = 2;
    private static final boolean IS_NOTCH_PROP = (true ^ SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    private static final boolean IS_OPS_HANDLER_THREAD_DISABLED = SystemProperties.getBoolean("ro.config.hwopshandler.disable", false);
    private static final boolean IS_SUPPORT_SINGLE_MODE = SystemProperties.getBoolean("ro.feature.wms.singlemode", true);
    private static final float LAZY_SCALE = 0.75f;
    private static final int LAZY_TYPE_DEFAULT = 0;
    private static final int LAZY_TYPE_LEFT = 1;
    private static final int LAZY_TYPE_RIGHT = 2;
    private static final long MSG_ROG_FREEZE_TIME_DELEAYED = 6000;
    private static final int NOTIFY_FINGER_WIN_COVERED = 101;
    private static final String RESOURCE_APPASSOC = "RESOURCE_APPASSOC";
    private static final int ROG_FREEZE_TIMEOUT = 100;
    private static final String SPLASH_SCREEN = "Splash Screen";
    static final String TAG = "HwWindowManagerServiceEx";
    private static final int UPDATE_WINDOW_STATE = 0;
    private static final int WMS_SET_LAZY_MODE = 7009;
    private static int lastFocusPid = -1;
    private static WindowState sDragWin;
    private static DefaultHwMultiDisplayUtils sHwMultiDisplayUtils = HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwMultiDisplayUtils();
    private static DefaultHwWindowsCastManager sHwWindowsCastManager = HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwWindowsCastManager();
    private static boolean sSwitched = false;
    private static WindowState sTouchWin = null;
    private Animation[] mActivityFinishAnimations = new Animation[2];
    private Animation[] mActivityStartAnimations = new Animation[2];
    private IHwMultiDisplayBitmapDragStartListener mBitmapDragListenerForMultiDisplay;
    final Context mContext;
    Configuration mCurNaviConfiguration;
    private IHwMultiDisplayDragStartListener mDragListenerForMultiDisplay;
    private WindowState mDragLocWindowState = null;
    private IHwMultiDisplayDropStartListener mDropListenerForMultiDisplay;
    private WindowState mDropWindowState = null;
    private int mExitFlag = 0;
    private Bitmap mExitIconBitmap;
    private int mExitIconHeight = 0;
    private int mExitIconWidth = 0;
    private float mExitPivotX = -1.0f;
    private float mExitPivotY = -1.0f;
    private boolean mGainFocus = true;
    private HandlerThread mHandlerThread;
    private boolean mHintShowing;
    private Handler mHwHandler = new Handler() {
        /* class com.android.server.wm.HwWindowManagerServiceEx.AnonymousClass1 */

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 100) {
                Slog.d(HwWindowManagerServiceEx.TAG, "ROG_FREEZE_TIMEOUT");
                SurfaceControl.unfreezeDisplay();
            } else if (i == 101 && HwWindowManagerServiceEx.this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
                boolean covered = true;
                if (msg.arg1 != 1) {
                    covered = false;
                }
                Rect frame = (Rect) msg.obj;
                if (((FingerprintManager) HwWindowManagerServiceEx.this.mContext.getSystemService("fingerprint")) != null) {
                    Slog.i(HwWindowManagerServiceEx.TAG, "handleMessage: NOTIFY_FINGER_WIN_COVERED covered=" + covered + " frame=" + frame);
                }
            }
        }
    };
    private HwInputManagerService.HwInputManagerLocalService mHwInputManagerInternal;
    private boolean mHwSafeMode;
    IHwWindowManagerInner mIWmsInner = null;
    /* access modifiers changed from: private */
    public boolean mIgnoreFrozen = false;
    boolean mIsCoverOpen = true;
    private IHwMultiDisplayDroppableListener mIsDroppableListener;
    private boolean mIsLanucherLandscape = false;
    private Rect mLastCoveredFrame = new Rect();
    private boolean mLastCoveredState;
    boolean mLayoutNaviBar = false;
    private int mLazyModeOnEx;
    private Interpolator mMagicWindowMoveInterpolator = null;
    private IHwMultiDisplayDragStateListener mMultiDisplayDragStateListener;
    private OpsUpdateHandler mOpsHandler;
    private IHwMultiDisplayPhoneOperateListener mPhoneOperateListenerForMultiDisplay;
    private final Runnable mReevaluateStatusBarSize = new Runnable() {
        /* class com.android.server.wm.HwWindowManagerServiceEx.AnonymousClass2 */

        public void run() {
            synchronized (HwWindowManagerServiceEx.this.mIWmsInner.getGlobalLock()) {
                boolean unused = HwWindowManagerServiceEx.this.mIgnoreFrozen = true;
                if (HwWindowManagerServiceEx.this.mLayoutNaviBar) {
                    HwWindowManagerServiceEx.this.mLayoutNaviBar = false;
                    HwWindowManagerServiceEx.this.mCurNaviConfiguration = HwWindowManagerServiceEx.this.mIWmsInner.computeNewConfiguration(HwWindowManagerServiceEx.this.mIWmsInner.getDefaultDisplayContentLocked().getDisplayId());
                    HwWindowManagerServiceEx.this.performhwLayoutAndPlaceSurfacesLocked();
                } else {
                    HwWindowManagerServiceEx.this.performhwLayoutAndPlaceSurfacesLocked();
                }
            }
        }
    };
    private ArrayList<WindowState> mSecureScreenRecords = new ArrayList<>();
    private ArrayList<WindowState> mSecureScreenShot = new ArrayList<>();
    private ArrayList<WindowState> mSecureWindows = new ArrayList<>();
    private SingleHandAdapter mSingleHandAdapter;
    private boolean mSingleHandSwitch;
    private final Handler mUiHandler;

    public HwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        this.mIWmsInner = wms;
        this.mContext = context;
        if (!IS_OPS_HANDLER_THREAD_DISABLED) {
            this.mHandlerThread = new HandlerThread("hw_ops_handler_thread");
            this.mHandlerThread.start();
            this.mOpsHandler = new OpsUpdateHandler(this.mHandlerThread.getLooper());
        }
        this.mUiHandler = UiThread.getHandler();
    }

    public void onChangeConfiguration(MergedConfiguration mergedConfiguration, WindowState ws) {
        DisplayInfo displayInfo;
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer() && !HwPCUtils.isHiCarCastMode() && ws != null && HwPCUtils.isValidExtDisplayId(ws.getDisplayId()) && mergedConfiguration != null && ws.getTask() != null && ws.getTask().getWindowingMode() == 1) {
            Configuration cf = mergedConfiguration.getOverrideConfiguration();
            DisplayContent dc = ws.getDisplayContent();
            if (cf != null && dc != null && (displayInfo = ws.getDisplayInfo()) != null) {
                int displayWidth = displayInfo.logicalWidth;
                int displayHeight = displayInfo.logicalHeight;
                float scale = ((float) displayInfo.logicalDensityDpi) / 160.0f;
                cf.screenWidthDp = (int) ((((float) displayWidth) / scale) + 0.5f);
                cf.screenHeightDp = (int) ((((float) displayHeight) / scale) + 0.5f);
                mergedConfiguration.setOverrideConfiguration(cf);
                ws.onConfigurationChanged(mergedConfiguration.getMergedConfiguration());
                HwPCUtils.log(TAG, "set pc fullscreen, width:" + displayWidth + " height:" + displayHeight + " scale:" + scale + " cf.screenWidthDp:" + cf.screenWidthDp + " cf.screenHeightDp:" + cf.screenHeightDp);
            }
        }
    }

    private boolean isInputTargetWindow(WindowState windowState, WindowState inputTargetWin) {
        Task task = windowState.getTask();
        boolean z = true;
        if (inputTargetWin != null && inputTargetWin.getTask() != null) {
            return inputTargetWin.getTask().mTaskId == task.mTaskId;
        }
        synchronized (this.mIWmsInner.getGlobalLock()) {
            DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(HwPCUtils.getPCDisplayID());
            if (dc == null) {
                return false;
            }
            WindowState win = dc.mCurrentFocusInHwPc;
            if (win == null || win.getTask() == null) {
                return false;
            }
            if (win.getTask().mTaskId != task.mTaskId) {
                z = false;
            }
            return z;
        }
    }

    public void adjustWindowPosForPadPC(Rect containingFrame, Rect contentFrame, WindowState imeWin, WindowState inputTargetWin, WindowState win) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad() && HwPCUtils.isValidExtDisplayId(win.getDisplayId()) && imeWin != null && imeWin.isVisibleNow() && isInputTargetWindow(win, inputTargetWin) && win.getTask() != null) {
            int windowState = -1;
            ActivityRecord r = ActivityRecord.forToken(win.getAttrs().token);
            if (r != null) {
                if (r instanceof HwActivityRecord) {
                    windowState = ((HwActivityRecord) r).getWindowState();
                }
                if (windowState != -1 && !HwPCMultiWindowCompatibility.isLayoutFullscreen(windowState) && !HwPCMultiWindowCompatibility.isLayoutMaximized(windowState) && !contentFrame.isEmpty()) {
                    int D1 = 0;
                    int D2 = 0;
                    if (win.getAttrs() == null || (win.getAttrs().softInputMode & 240) != 16) {
                        Rect imeBounds = new Rect();
                        imeWin.getBounds(imeBounds);
                        if (!imeBounds.isEmpty() && containingFrame.bottom > imeBounds.top) {
                            D1 = imeBounds.top - containingFrame.bottom;
                            D2 = contentFrame.top - containingFrame.top;
                        }
                    } else if (containingFrame.bottom > contentFrame.bottom) {
                        D1 = contentFrame.bottom - containingFrame.bottom;
                        D2 = contentFrame.top - containingFrame.top;
                    }
                    int offsetY = D1 > D2 ? D1 : D2;
                    Rect taskBounds = new Rect();
                    if (offsetY < 0) {
                        win.getTask().getBounds(taskBounds);
                        taskBounds.offset(0, offsetY);
                        win.getTask().setBounds(taskBounds);
                        containingFrame.offset(0, offsetY);
                    }
                }
            }
        }
    }

    public void layoutWindowForPadPCMode(WindowState win, WindowState inputTargetWin, WindowState imeWin, Rect pf, Rect df, Rect cf, Rect vf, int contentBottom) {
        if (isInputTargetWindow(win, inputTargetWin)) {
            int inputMethodTop = 0;
            if (imeWin != null && imeWin.isVisibleLw()) {
                int top = (imeWin.getDisplayFrameLw().top > imeWin.getContentFrameLw().top ? imeWin.getDisplayFrameLw() : imeWin.getContentFrameLw()).top + imeWin.getGivenContentInsetsLw().top;
                inputMethodTop = contentBottom < top ? contentBottom : top;
            }
            if (inputMethodTop > 0) {
                vf.bottom = inputMethodTop;
                cf.bottom = inputMethodTop;
                df.bottom = inputMethodTop;
                pf.bottom = inputMethodTop;
            }
        }
    }

    public void sendUpdateAppOpsState() {
        OpsUpdateHandler opsUpdateHandler = this.mOpsHandler;
        if (opsUpdateHandler != null) {
            opsUpdateHandler.removeMessages(0);
            this.mOpsHandler.sendEmptyMessage(0);
        }
    }

    public void setAppOpHideHook(WindowState win, boolean visible) {
        if (!visible) {
            setAppOpVisibilityChecked(win, visible);
        }
    }

    private boolean setAppOpVisibilityChecked(WindowState win, boolean visible) {
        if (visible) {
            setWinAndChildrenVisibility(win, true);
            return true;
        } else if (allowAnyway(win)) {
            setWinAndChildrenVisibility(win, true);
            return true;
        } else {
            setWinAndChildrenVisibility(win, false);
            sendForbiddenMessage(win);
            return false;
        }
    }

    private void setWinAndChildrenVisibility(WindowState win, boolean visible) {
        if (win != null) {
            win.setAppOpVisibilityLw(visible);
            int N = win.mChildren.size();
            Slog.i(TAG, "this win:" + win + " hase children size:" + N);
            for (int i = 0; i < N; i++) {
                setWinAndChildrenVisibility((WindowState) win.mChildren.get(i), visible);
            }
        }
    }

    private boolean allowAnyway(WindowState win) {
        if (win == null) {
            return true;
        }
        if (!checkFullWindowWithoutTransparent(win.mAttrs)) {
            return false;
        }
        Slog.i(TAG, "don't allow anyway," + win);
        return false;
    }

    private void sendForbiddenMessage(WindowState win) {
        OpsUpdateHandler opsUpdateHandler = this.mOpsHandler;
        if (opsUpdateHandler != null) {
            Message msg = opsUpdateHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putInt("uid", win.getOwningUid());
            bundle.putString("package", win.getOwningPackage());
            msg.setData(bundle);
            this.mOpsHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void sendForbiddenBroadcast(Bundle data) {
        Intent preventIntent = new Intent("com.android.server.wm.addview.preventnotify");
        preventIntent.putExtras(data);
        this.mContext.sendBroadcastAsUser(preventIntent, UserHandle.ALL);
    }

    private boolean checkFullWindowWithoutTransparent(WindowManager.LayoutParams attrs) {
        return -1 == attrs.width && -1 == attrs.height && 0.0d != ((double) attrs.alpha);
    }

    public void setVisibleFromParent(WindowState win) {
        if (parentHiddenByAppOp(win)) {
            Slog.i(TAG, "parent is hidden by app ops, should also hide this win:" + win);
            setWinAndChildrenVisibility(win, false);
        }
    }

    private boolean parentHiddenByAppOp(WindowState win) {
        if (win == null || !win.isChildWindow()) {
            return false;
        }
        if (!win.getParentWindow().mAppOpVisibility) {
            return true;
        }
        return parentHiddenByAppOp(win.getParentWindow());
    }

    public void checkSingleHandMode(AppWindowToken oldFocus, AppWindowToken newFocus) {
        if ((oldFocus != newFocus) && newFocus != null) {
            int requestedOrientation = newFocus.mOrientation;
            if (requestedOrientation == 0 || requestedOrientation == 6 || requestedOrientation == 8 || requestedOrientation == 11) {
                Slog.i(TAG, "requestedOrientation: " + requestedOrientation);
                Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
            }
        }
    }

    private class OpsUpdateHandler extends Handler {
        public OpsUpdateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 0) {
                HwWindowManagerServiceEx.this.mIWmsInner.updateAppOpsState();
            } else if (i == 1) {
                HwWindowManagerServiceEx.this.sendForbiddenBroadcast(msg.getData());
            }
        }
    }

    public void updateAppView(RemoteViews remoteViews) {
        WindowManagerPolicy policy = this.mIWmsInner.getPolicy();
        if (policy instanceof HwPhoneWindowManager) {
            ((HwPhoneWindowManager) policy).updateAppView(remoteViews);
        }
    }

    public void removeAppView() {
        removeAppView(true);
    }

    public void removeAppView(boolean isNeedBtnView) {
        WindowManagerPolicy policy = this.mIWmsInner.getPolicy();
        if (policy instanceof HwPhoneWindowManager) {
            ((HwPhoneWindowManager) policy).removeAppView(isNeedBtnView);
        }
    }

    public boolean isFullScreenDevice() {
        return HwFullScreenDisplay.isFullScreenDevice();
    }

    public float getDeviceMaxRatio() {
        return HwFullScreenDisplay.getDeviceMaxRatio();
    }

    public float getDefaultNonFullMaxRatio() {
        return HwFullScreenDisplay.getDefaultNonFullMaxRatio();
    }

    public float getExclusionNavBarMaxRatio() {
        return HwFullScreenDisplay.getExclusionNavBarMaxRatio();
    }

    public void setNotchHeight(int notchHeight) {
        HwFullScreenDisplay.setNotchHeight(notchHeight);
    }

    public void getAppDisplayRect(float appMaxRatio, Rect rect, int left, int rotation) {
        HwFullScreenDisplay.getAppDisplayRect(appMaxRatio, rect, left, rotation);
    }

    public Rect getTopAppDisplayBounds(float appMaxRatio, int rotation, int screenWidth) {
        return HwFullScreenDisplay.getTopAppDisplayBounds(appMaxRatio, rotation, screenWidth);
    }

    public List<String> getNotchSystemApps() {
        return HwNotchScreenWhiteConfig.getInstance().getNotchSystemApps();
    }

    public int getAppUseNotchMode(String packageName) {
        return HwNotchScreenWhiteConfig.getInstance().getAppUseNotchMode(packageName);
    }

    public boolean isInNotchAppWhitelist(WindowState win) {
        return HwNotchScreenWhiteConfig.getInstance().isNotchAppInfo(win);
    }

    private void getAlertWindows(ArrayMap<WindowState, Integer> windows) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            for (WindowState win : this.mIWmsInner.getWindowMap().values()) {
                if (!(win == null || win.mAttrs == null || win.mSession == null)) {
                    if (!windows.containsKey(win)) {
                        if (win.mAppOp == 24) {
                            if (this.mIWmsInner.getAppOps() != null) {
                                windows.put(win, Integer.valueOf(this.mIWmsInner.getAppOps().startOpNoThrow(win.mAppOp, win.getOwningUid(), win.getOwningPackage())));
                            } else {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Bundle> getVisibleWindows(int ops) {
        ArrayMap<WindowState, Integer> windows = new ArrayMap<>();
        getVisibleWindows(windows, ops);
        List<Bundle> windowsList = new ArrayList<>();
        for (Map.Entry<WindowState, Integer> win : windows.entrySet()) {
            WindowState state = win.getKey();
            Bundle bundle = new Bundle();
            bundle.putInt("window_pid", state.mSession.mPid);
            bundle.putInt("window_value", win.getValue().intValue());
            bundle.putInt("window_state", System.identityHashCode(state));
            bundle.putInt("window_width", state.mRequestedWidth);
            bundle.putInt("window_height", state.mRequestedHeight);
            bundle.putFloat("window_alpha", state.mAttrs.alpha);
            bundle.putBoolean("window_hidden", state.mPermanentlyHidden);
            bundle.putString("window_package", state.getOwningPackage());
            bundle.putInt("window_uid", state.getOwningUid());
            windowsList.add(bundle);
        }
        return windowsList;
    }

    private void getVisibleWindows(ArrayMap<WindowState, Integer> windows, int ops) {
        if (windows != null) {
            if (ops == 45) {
                getToastWindows(windows);
            } else {
                getAlertWindows(windows);
            }
        }
    }

    private void getToastWindows(ArrayMap<WindowState, Integer> windows) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            for (WindowState win : this.mIWmsInner.getWindowMap().values()) {
                if (!(win == null || win.mAttrs == null || win.mSession == null)) {
                    if (!windows.containsKey(win)) {
                        if (win.mAttrs.type == 2005) {
                            windows.put(win, 3);
                        }
                    }
                }
            }
        }
    }

    private void updateVisibleWindows(int eventType, int mode, int type, WindowState win, int requestedWidth, int requestedHeight, boolean isupdate) {
        if (requestedWidth != win.mRequestedWidth || requestedHeight != win.mRequestedHeight || !isupdate) {
            Bundle args = new Bundle();
            args.putInt(ASSOC_WINDOW, win.mSession.mPid);
            args.putInt(ASSOC_WINDOW_MODE, mode);
            args.putInt(ASSOC_RELATION_TYPE, eventType);
            args.putInt(ASSOC_WINDOW_HASHCODE, System.identityHashCode(win));
            args.putInt(ASSOC_WINDOW_TYPE, type);
            args.putInt(ASSOC_WINDOW_WIDTH, eventType == 8 ? win.getAttrs().width : requestedWidth);
            args.putInt(ASSOC_WINDOW_HEIGHT, eventType == 8 ? win.getAttrs().height : requestedHeight);
            args.putFloat(ASSOC_WINDOW_ALPHA, win.getAttrs().alpha);
            args.putBoolean(ASSOC_WINDOW_PHIDE, win.mPermanentlyHidden);
            args.putBoolean(ASSOC_WINDOW_HAS_SURFACE, win.mHasSurface);
            args.putString("pkgname", win.getOwningPackage());
            args.putInt("uid", win.getOwningUid());
            this.mIWmsInner.getWMMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), args);
        }
    }

    private void updateVisibleWindowsOps(int eventType, String pkgName) {
        Bundle args = new Bundle();
        args.putString("pkgname", pkgName);
        args.putInt(ASSOC_RELATION_TYPE, eventType);
        this.mIWmsInner.getWMMonitor().reportData(RESOURCE_APPASSOC, System.currentTimeMillis(), args);
    }

    private void reportWindowStatusToIAware(int eventType, WindowState win, int mode, int requestedWidth, int requestedHeight, boolean isupdate) {
        boolean isToast = win != null && win.getAttrs().type == 2005;
        if (win == null) {
            return;
        }
        if ((win.mAppOp != 24 && !isToast) || win.mSession == null) {
            return;
        }
        if (this.mIWmsInner.getWMMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            updateVisibleWindows(eventType, mode, isToast ? 45 : 24, win, requestedWidth, requestedHeight, isupdate);
        }
    }

    public void addWindowReport(WindowState win, int mode) {
        reportWindowStatusToIAware(8, win, mode, 0, 0, false);
    }

    public void removeWindowReport(WindowState win) {
        reportWindowStatusToIAware(9, win, 3, 0, 0, false);
    }

    public void updateWindowReport(WindowState win, int requestedWidth, int requestedHeight) {
        reportWindowStatusToIAware(27, win, 3, requestedWidth, requestedHeight, true);
    }

    public void setStartWindowTransitionReady(WindowState win) {
        if (win != null && StartWindowFeature.getConcurrentSwitch()) {
            CharSequence title = win.getWindowTag();
            DisplayContent dc = win.getDisplayContent();
            if (title != null && dc != null) {
                AppTransition transition = dc.mAppTransition;
                if (win.mAttrs.type == 3 && transition.isTransitionSet() && title.toString().startsWith(SPLASH_SCREEN)) {
                    transition.setReady();
                }
            }
        }
    }

    public void updateAppOpsStateReport(int ops, String packageName) {
        if (ops == 24 && this.mIWmsInner.getWMMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            updateVisibleWindowsOps(10, packageName);
        }
    }

    public void notifyFingerWinCovered(boolean covered, Rect frame) {
        if (this.mLastCoveredState != covered || !this.mLastCoveredFrame.equals(frame)) {
            this.mHwHandler.sendMessage(this.mHwHandler.obtainMessage(101, covered ? 1 : 0, 0, frame));
            this.mLastCoveredState = covered;
            this.mLastCoveredFrame.set(frame);
        }
    }

    public int getFocusWindowWidth(WindowState currentFocus, WindowState inputMethodTarget) {
        WindowState focusWindow;
        Rect rect;
        if (HwPCUtils.isHiCarCastMode() && currentFocus != null && !currentFocus.isDefaultDisplay()) {
            return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwHiCarMultiWindowManager().getInputMethodWidth();
        }
        if (inputMethodTarget == null) {
            focusWindow = currentFocus;
        } else {
            focusWindow = inputMethodTarget;
        }
        if (focusWindow == null) {
            Log.e(TAG, "WMS getFocusWindowWidth error");
            return 0;
        }
        if (focusWindow.getAttrs().type == 2) {
            rect = focusWindow.getDisplayFrameLw();
        } else {
            rect = focusWindow.getContentFrameLw();
        }
        if (focusWindow.inHwMagicWindowingMode()) {
            rect = focusWindow.getDecorFrame();
        }
        return rect.width();
    }

    public void handleNewDisplayConfiguration(Configuration overrideConfig, int displayId) {
        WindowManagerPolicy wmPolicy = this.mIWmsInner.getPolicy();
        if (wmPolicy instanceof HwPhoneWindowManager) {
            HwPhoneWindowManager policy = (HwPhoneWindowManager) wmPolicy;
            if (policy.getHwWindowCallback() != null && (this.mIWmsInner.getRoot().getDisplayContent(displayId).getConfiguration().diff(overrideConfig) & 128) != 0) {
                Slog.v(TAG, "handleNewDisplayConfiguration notify window callback");
                try {
                    policy.getHwWindowCallback().handleConfigurationChanged();
                } catch (Exception ex) {
                    Slog.w(TAG, "mIHwWindowCallback handleNewDisplayConfiguration", ex);
                }
            }
        }
    }

    public void getCurrFocusedWinInExtDisplay(Bundle outBundle) {
        if (outBundle != null) {
            synchronized (this.mIWmsInner.getGlobalLock()) {
                DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(HwPCUtils.getPCDisplayID());
                if (dc != null) {
                    WindowState ws = dc.findFocusedWindow();
                    if (ws != null) {
                        outBundle.putString("pkgName", ws.getAttrs().packageName);
                        boolean isApp = ws.getAppToken() != null;
                        outBundle.putBoolean("isApp", isApp);
                        outBundle.putParcelable("bounds", isApp ? ws.getBounds() : null);
                    }
                }
            }
        }
    }

    public boolean hasLighterViewInPCCastMode() {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(HwPCUtils.getPCDisplayID());
            if (dc == null) {
                return false;
            }
            if (!(dc instanceof HwDisplayContent)) {
                return false;
            }
            return ((HwDisplayContent) dc).hasLighterViewInPCCastMode();
        }
    }

    public boolean shouldDropMotionEventForTouchPad(float x, float y) {
        DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(0);
        if (dc != null && (dc instanceof HwDisplayContent)) {
            return ((HwDisplayContent) dc).shouldDropMotionEventForTouchPad(x, y);
        }
        return false;
    }

    public void updateHwStartWindowRecord(int appUid) {
        HwStartWindowRecord.getInstance().resetStartWindowApp(Integer.valueOf(appUid));
    }

    public HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(TaskSnapshotController mTaskSnapshotController, WindowState focusedWindow, boolean refresh) {
        ActivityManager.TaskSnapshot taskSnapshot;
        synchronized (this.mIWmsInner.getGlobalLock()) {
            if (refresh) {
                try {
                    mTaskSnapshotController.clearForegroundTaskSnapshot();
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (focusedWindow == null || focusedWindow.mAppToken == null || !refresh) {
                taskSnapshot = mTaskSnapshotController.getForegroundTaskSnapshot();
            } else {
                taskSnapshot = mTaskSnapshotController.createForegroundTaskSnapshot(focusedWindow.mAppToken);
            }
        }
        HwTaskSnapshotWrapper hwTaskSnapshotWrapper = new HwTaskSnapshotWrapper();
        hwTaskSnapshotWrapper.setTaskSnapshot(taskSnapshot);
        return hwTaskSnapshotWrapper;
    }

    public boolean detectSafeMode() {
        WindowManagerPolicy wmPolicy = this.mIWmsInner.getPolicy();
        boolean sCheckSafeModeState = false;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "isSafeModeDisabled", 0) == 1) {
            Slog.i(TAG, "safemode is disabled by dpm");
            this.mHwSafeMode = false;
            wmPolicy.setSafeMode(this.mHwSafeMode);
            sCheckSafeModeState = true;
        }
        if (!"1".equals(SystemProperties.get("sys.bootfail.safemode"))) {
            return sCheckSafeModeState;
        }
        Slog.i(TAG, "safemode is enabled eRecovery");
        this.mHwSafeMode = true;
        wmPolicy.setSafeMode(this.mHwSafeMode);
        return true;
    }

    public boolean getSafeMode() {
        return this.mHwSafeMode;
    }

    private void transferLazyModeToSurfaceFlinger(int lazyMode) {
        Parcel dataIn = Parcel.obtain();
        dataIn.writeInt(lazyMode);
        dataIn.writeFloat(0.75f);
        try {
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            Log.i(TAG, "transferLazyModeToSurfaceFlinger：  lazyMode " + lazyMode + ",lazyScale " + 0.75f);
            if (sfBinder != null && !sfBinder.transact(WMS_SET_LAZY_MODE, dataIn, null, 1)) {
                Log.e(TAG, "transferLazyModeToSurfaceFlinger error!");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "transferLazyModeToSurfaceFlinger RemoteException on notify lazy mode end");
        } catch (Throwable th) {
            dataIn.recycle();
            throw th;
        }
        dataIn.recycle();
    }

    public void setLazyModeEx(int lazyMode, boolean hintShowing, String windowName) {
        Slog.i(TAG, "curSingleHand: " + this.mLazyModeOnEx + " toSingleHand: " + lazyMode + ", hintShowing:" + hintShowing + ", windowName:" + windowName);
        if (windowName == null) {
            Slog.e(TAG, "setLazyModeEx windowName is null");
            return;
        }
        if (windowName.contains("blurpaper")) {
            if (this.mHintShowing != hintShowing) {
                this.mHintShowing = hintShowing;
            } else {
                return;
            }
        } else if (!windowName.contains("virtual")) {
            Slog.e(TAG, "setLazyModeEx windowName:" + windowName + "mLazyModeOnEx" + this.mLazyModeOnEx + "mHintShowing" + this.mHintShowing);
            return;
        } else if (this.mLazyModeOnEx != lazyMode) {
            this.mLazyModeOnEx = lazyMode;
            transferLazyModeToSurfaceFlinger(this.mLazyModeOnEx);
        } else {
            return;
        }
        HwInputManagerService.HwInputManagerLocalService hwInputManagerLocalService = this.mHwInputManagerInternal;
        if (hwInputManagerLocalService != null) {
            hwInputManagerLocalService.setLazyMode(SingleHandWindow.getExpandLazyMode(this.mLazyModeOnEx, this.mHintShowing));
        }
    }

    public int getLazyModeEx() {
        return this.mLazyModeOnEx;
    }

    public final void performhwLayoutAndPlaceSurfacesLocked() {
        this.mIWmsInner.getWindowSurfacePlacer().performSurfacePlacement();
    }

    public Configuration getCurNaviConfiguration() {
        return this.mCurNaviConfiguration;
    }

    public boolean getIgnoreFrozen() {
        return this.mIgnoreFrozen;
    }

    public void setIgnoreFrozen(boolean flag) {
        this.mIgnoreFrozen = flag;
    }

    public void reevaluateStatusBarSize(boolean layoutNaviBar) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            this.mLayoutNaviBar = layoutNaviBar;
            this.mIWmsInner.getWindowMangerServiceHandler().post(this.mReevaluateStatusBarSize);
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        this.mIWmsInner.getInputManager().setCurrentUser(newUserId, currentProfileIds);
        WindowManagerPolicy policy = this.mIWmsInner.getPolicy();
        if (policy instanceof HwPhoneWindowManager) {
            ((HwPhoneWindowManager) policy).setCurrentUser(newUserId, currentProfileIds);
        }
    }

    public void hwSystemReady() {
        if (IS_SUPPORT_SINGLE_MODE) {
            this.mSingleHandSwitch = judgeSingleHandSwitchBySize();
            Slog.i(TAG, "WMS systemReady mSingleHandSwitch = " + this.mSingleHandSwitch);
            if (this.mSingleHandSwitch || HwFoldScreenState.isFoldScreenDevice()) {
                this.mSingleHandAdapter = new SingleHandAdapter(this.mContext, this.mHwHandler, this.mUiHandler, this.mIWmsInner.getService());
                this.mSingleHandAdapter.registerLocked();
            }
        }
        this.mHwInputManagerInternal = (HwInputManagerService.HwInputManagerLocalService) LocalServices.getService(HwInputManagerService.HwInputManagerLocalService.class);
        if (CoordinationModeUtils.isFoldable()) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "coordination_create_mode", 0);
        }
    }

    private boolean judgeSingleHandSwitchBySize() {
        return this.mContext.getResources().getBoolean(34537473);
    }

    public boolean isSupportSingleHand() {
        if (HwFoldScreenState.isFoldScreenDevice()) {
            return true;
        }
        return this.mSingleHandSwitch;
    }

    public void setCoverManagerState(boolean isCoverOpen) {
        this.mIsCoverOpen = isCoverOpen;
        HwServiceFactory.setIfCoverClosed(!isCoverOpen);
    }

    public boolean isCoverOpen() {
        return this.mIsCoverOpen;
    }

    public void freezeOrThawRotation(int rotation) {
        if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        }
        Slog.v(TAG, "freezeRotationTemporarily rotation:" + rotation);
        this.mIWmsInner.getDefaultDisplayContentLocked().getDisplayRotation().freezeOrThawRotation(rotation);
    }

    public void preAddWindow(WindowManager.LayoutParams attrs) {
        if (attrs.type == 2101) {
            attrs.token = null;
        }
    }

    private boolean checkPermission() {
        int uid = UserHandle.getAppId(Binder.getCallingUid());
        if (uid == 1000) {
            return true;
        }
        Slog.e(TAG, "Process Permission error! uid:" + uid);
        return false;
    }

    public void notifySwingRotation(int rotation) {
        if (checkPermission()) {
            Slog.d(TAG, "notifySwingRotation rotation:" + rotation);
            this.mIWmsInner.getService().getDefaultDisplayContentLocked().getDisplayRotation().getHwDisplayRotationEx().setSwingRotation(rotation);
        }
    }

    public Rect getSafeInsets(int type) {
        DisplayContent dc = this.mIWmsInner.getDefaultDisplayContentLocked();
        if (dc == null || !(dc instanceof HwDisplayContent)) {
            return null;
        }
        return ((HwDisplayContent) dc).getSafeInsetsByType(type);
    }

    public List<Rect> getBounds(int type) {
        DisplayContent dc = this.mIWmsInner.getDefaultDisplayContentLocked();
        if (dc == null || !(dc instanceof HwDisplayContent)) {
            return null;
        }
        return ((HwDisplayContent) dc).getBoundsByType(type);
    }

    public void setForcedDisplayDensityAndSize(int displayId, int density, int width, int height) {
        int height2;
        if (density >= 200 && width >= 400) {
            if (height >= 400) {
                Slog.d(TAG, "setForcedDisplayDensityAndSize size: " + width + "x" + height + " density: " + density);
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                    throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
                } else if (displayId == 0) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        synchronized (this.mIWmsInner.getGlobalLock()) {
                            try {
                                DisplayContent displayContent = this.mIWmsInner.getRoot().getDisplayContent(displayId);
                                if (displayContent != null) {
                                    int width2 = width < displayContent.mInitialDisplayWidth * 2 ? width : displayContent.mInitialDisplayWidth * 2;
                                    try {
                                        height2 = height < displayContent.mInitialDisplayHeight * 2 ? height : displayContent.mInitialDisplayHeight * 2;
                                    } catch (Throwable th) {
                                        th = th;
                                        try {
                                            throw th;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    try {
                                        displayContent.mBaseDisplayWidth = width2;
                                        displayContent.mBaseDisplayHeight = height2;
                                        displayContent.mBaseDisplayDensity = density;
                                        this.mHwHandler.removeMessages(100);
                                        this.mHwHandler.sendEmptyMessageDelayed(100, MSG_ROG_FREEZE_TIME_DELEAYED);
                                        updateResourceConfiguration(displayId, density, width2, height2);
                                        this.mIWmsInner.getService().reconfigureDisplayLocked(displayContent);
                                        ScreenRotationAnimation screenRotationAnimation = this.mIWmsInner.getWindowAnimator().getScreenRotationAnimationLocked(displayId);
                                        if (screenRotationAnimation != null) {
                                            screenRotationAnimation.kill();
                                        }
                                        ContentResolver contentResolver = this.mContext.getContentResolver();
                                        Settings.Global.putString(contentResolver, "display_size_forced", width2 + "," + height2);
                                        List<UserInfo> userList = UserManager.get(this.mContext).getUsers();
                                        if (userList != null) {
                                            for (Iterator<UserInfo> it = userList.iterator(); it.hasNext(); it = it) {
                                                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(density), it.next().id);
                                                userList = userList;
                                            }
                                        }
                                        SystemProperties.set("persist.sys.realdpi", density + "");
                                        SystemProperties.set("persist.sys.rog.width", width2 + "");
                                        SystemProperties.set("persist.sys.rog.height", height2 + "");
                                        if (IS_NOTCH_PROP) {
                                            this.mIWmsInner.getDisplayManagerInternal().updateCutoutInfoForRog(0);
                                            Slog.d(TAG, "updateCutoutInfoForRog width: " + width2 + " height " + height2);
                                        }
                                        Display display = displayContent.getDisplay();
                                        Point maxDisplaySize = new Point();
                                        display.getRealSize(maxDisplaySize);
                                        HwFreeFormUtils.computeFreeFormSize(maxDisplaySize);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                }
                                Binder.restoreCallingIdentity(ident);
                                return;
                            } catch (Throwable th4) {
                                th = th4;
                                throw th;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                } else {
                    throw new IllegalArgumentException("Can only set the default display");
                }
            }
        }
        Slog.d(TAG, "the para of setForcedDisplayDensityAndSize is illegal : size = " + width + "x" + height + "; density = " + density);
    }

    public void updateResourceConfiguration(int displayId, int density, int width, int height) {
        if (density == 0) {
            Slog.e(TAG, "setForcedDisplayDensityAndSize density is 0");
            return;
        }
        Slog.d(TAG, "setForcedDisplay and updateResourceConfiguration, density = " + density + " width = " + width + " height = " + height);
        Configuration tempResourceConfiguration = new Configuration(this.mIWmsInner.getRoot().getDisplayContent(displayId).getConfiguration());
        DisplayMetrics tempMetrics = this.mContext.getResources().getDisplayMetrics();
        tempResourceConfiguration.densityDpi = density;
        tempResourceConfiguration.screenWidthDp = (width * 160) / density;
        tempResourceConfiguration.smallestScreenWidthDp = (width * 160) / density;
        tempMetrics.density = ((float) density) / 160.0f;
        tempMetrics.densityDpi = density;
        this.mContext.getResources().updateConfiguration(tempResourceConfiguration, tempMetrics);
        Slog.d(TAG, "setForcedDisplay and updateResourceConfiguration, tempResourceConfiguration is: " + tempResourceConfiguration + "tempMetrics is: " + tempMetrics);
    }

    public void setAppWindowExitInfo(Bundle bundle, Bitmap iconBitmap, int callingUid) {
        if (!HwWmConstants.IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM) {
            return;
        }
        if (bundle != null) {
            String pkgName = bundle.getString(HwWmConstants.LAUNCHER_PKG_NAME_STR);
            if (!HwWmConstants.isLauncherPkgName(pkgName)) {
                Slog.w(TAG, "Package " + pkgName + " is not qualified!");
                return;
            }
            try {
                if (this.mContext.getPackageManager().getApplicationInfoAsUser(pkgName, 0, UserHandle.getUserId(callingUid)).uid == callingUid) {
                    this.mExitIconWidth = iconBitmap != null ? iconBitmap.getWidth() : bundle.getInt(HwWmConstants.ICON_WIDTH_STR);
                    this.mExitIconHeight = iconBitmap != null ? iconBitmap.getHeight() : bundle.getInt(HwWmConstants.ICON_HEIGHT_STR);
                    this.mExitPivotX = bundle.getFloat(HwWmConstants.PIVOTX_STR);
                    this.mExitPivotY = bundle.getFloat(HwWmConstants.PIVOTY_STR);
                    this.mExitIconBitmap = iconBitmap;
                    this.mExitFlag = bundle.getInt(HwWmConstants.FLAG_STR);
                    this.mIsLanucherLandscape = bundle.getBoolean(HwWmConstants.IS_LANDSCAPE_STR);
                    Slog.i(TAG, "set app win exit info, bundle = " + bundle + ", iconBitmap = " + iconBitmap);
                    return;
                }
                throw new SecurityException("Package " + pkgName + " not in UID " + callingUid);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "set app win exit info, not found app of name: " + pkgName);
            }
        } else {
            Slog.i(TAG, "empty bundle!");
            this.mExitPivotX = -1.0f;
            this.mExitPivotY = -1.0f;
            this.mExitIconWidth = -1;
            this.mExitIconHeight = -1;
            this.mExitIconBitmap = null;
            this.mExitFlag = 0;
            this.mIsLanucherLandscape = false;
        }
    }

    public void resetAppWindowExitInfo(int transit, AppWindowToken topOpeningApp) {
        if (HwWmConstants.IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM && transit == 13 && topOpeningApp != null && HwWmConstants.containsLauncherCmpName(topOpeningApp.toString())) {
            this.mExitPivotX = -1.0f;
            this.mExitPivotY = -1.0f;
            this.mExitIconBitmap = null;
            this.mExitIconWidth = -1;
            this.mExitIconHeight = -1;
            this.mExitFlag = -1;
            this.mIsLanucherLandscape = false;
            Slog.i(TAG, "exit info has been reset.");
        }
    }

    public void clearAppWindowIconInfo(WindowState win, int viewVisibility) {
        if (HwWmConstants.IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM && win.mWinAnimator != null && win.mAnimatingExit && viewVisibility == 0) {
            win.mWinAnimator.setWindowIconInfo(0, 0, 0, (Bitmap) null);
            Slog.i(TAG, "Relayout clear set window icon info flag");
        }
    }

    public boolean isRightInMagicWindow(WindowState ws) {
        if (HwMwUtils.ENABLED && ws != null && ws.inHwMagicWindowingMode()) {
            Bundle bundle = HwMwUtils.performPolicy(104, new Object[]{ws.getBounds()});
            boolean isRight = bundle.getBoolean("BUNDLE_ISRIGHT_INMW", false);
            boolean isLeft = bundle.getBoolean("RESULT_ISLEFT_INMW", false);
            DisplayInfo displayInfo = ws.getDisplayInfo();
            if (displayInfo != null) {
                int width = displayInfo.logicalWidth;
                if ((this.mExitPivotX < ((float) (width / 2)) || !isLeft) && (!isRight || this.mExitPivotX >= ((float) (width / 2)))) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public AppWindowToken findExitToLauncherMaxWindowSizeToken(ArraySet<AppWindowToken> closingApps, int appsCount, int transit) {
        WindowState win;
        int maxSize = -1;
        AppWindowToken maxSizeToken = null;
        Slog.i(TAG, "closing apps count = " + appsCount);
        if (HwWmConstants.IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM && transit == 13) {
            for (int i = 0; i < appsCount; i++) {
                AppWindowToken wtoken = closingApps.valueAt(i);
                if (!(wtoken == null || (win = wtoken.findMainWindow(false)) == null || win.getFrameLw() == null)) {
                    int height = win.getFrameLw().height();
                    int width = win.getFrameLw().width();
                    int size = height * width;
                    if (!this.mIWmsInner.getService().mHwWMSEx.isRightInMagicWindow(win) && !wtoken.toString().contains(HwWmConstants.PERMISSION_DIALOG_CMP) && (size > maxSize || (size == maxSize && !wtoken.isHidden()))) {
                        maxSize = height * width;
                        maxSizeToken = wtoken;
                    }
                }
            }
        }
        return maxSizeToken;
    }

    private boolean isClosingAppsContains(AppWindowToken atoken, DisplayContent dc) {
        return (dc.mClosingApps != null) && dc.mClosingApps.contains(atoken);
    }

    private boolean isOpeningAppsContains(AppWindowToken atoken, DisplayContent dc) {
        return (dc.mOpeningApps != null) && dc.mOpeningApps.contains(atoken);
    }

    private boolean isAppLauncher(AppWindowToken atoken) {
        return (atoken != null) && HwWmConstants.containsLauncherCmpName(atoken.toString());
    }

    public boolean isLastOneApp(DisplayContent dc) {
        boolean isLastOneApp = false;
        if (dc == null) {
            Slog.w(TAG, "find no display content when try to check freeform exit by back");
            return false;
        }
        AppWindowToken topOpeningApp = getTopApp(dc.mOpeningApps, false);
        Slog.d(TAG, "is app exit to launcher info: dc , mClosingApps = " + dc.mClosingApps + ", topOpeningApp = " + topOpeningApp + ", mExitIconBitmap = " + this.mExitIconBitmap + ", mExitIconHeight = " + this.mExitIconHeight + ", mExitIconWidth = " + this.mExitIconWidth);
        if (topOpeningApp == null) {
            isLastOneApp = true;
        }
        return isLastOneApp;
    }

    private boolean isAppExitToLauncher(AppWindowToken atoken, int transit, DisplayContent dc) {
        boolean isAppExitToLauncher = false;
        WindowState window = atoken.findMainWindow(false);
        AppWindowToken topOpeningApp = getTopApp(dc.mOpeningApps, false);
        Slog.d(TAG, "is app exit to launcher info: transit = " + transit + ", app = " + atoken + ", window = " + window + ", mClosingApps = " + dc.mClosingApps + ", topOpeningApp = " + topOpeningApp + ", mExitIconBitmap = " + this.mExitIconBitmap + ", mExitIconHeight = " + this.mExitIconHeight + ", mExitIconWidth = " + this.mExitIconWidth);
        boolean isExitBitmapNotNull = true;
        boolean isWindowNotNull = window != null;
        boolean isTransitWallpaperOpen = transit == 13;
        boolean isTokenNotStkDialog = !atoken.toString().contains(HwWmConstants.STK_DIALOG_CMP);
        if (this.mExitIconBitmap == null) {
            isExitBitmapNotNull = false;
        }
        if (isWindowNotNull && isTransitWallpaperOpen && isClosingAppsContains(atoken, dc) && isAppLauncher(topOpeningApp) && isTokenNotStkDialog && isExitBitmapNotNull) {
            isAppExitToLauncher = true;
            if (window.mAttrs != null && (window.mAttrs.flags & AwarenessConstants.MSDP_ENVIRONMENT_TYPE_WAY_OFFICE) == 524288 && (window.mAttrs.flags & 4194304) == 4194304) {
                Slog.d(TAG, "app to launcher window flag = " + window.mAttrs.flags);
                isAppExitToLauncher = false;
            }
        }
        if (!isAppExitToLauncher && isWindowNotNull && window.mWinAnimator != null) {
            window.mWinAnimator.setWindowIconInfo(0, 0, 0, (Bitmap) null);
        }
        return isAppExitToLauncher;
    }

    private boolean isLauncherOpen(AppWindowToken atoken, int transit, DisplayContent dc) {
        if (!(transit == 13) || !HwWmConstants.containsLauncherCmpName(atoken.toString()) || dc.mClosingApps == null || !isOpeningAppsContains(atoken, dc)) {
            return false;
        }
        Slog.i(TAG, dc.mClosingApps + " is closing and " + dc.mOpeningApps + "is opening");
        return true;
    }

    private AppWindowToken getTopApp(ArraySet<AppWindowToken> apps, boolean ignoreHidden) {
        int prefixOrderIndex;
        int topPrefixOrderIndex = Integer.MIN_VALUE;
        AppWindowToken topApp = null;
        for (int i = apps.size() - 1; i >= 0; i--) {
            AppWindowToken app = apps.valueAt(i);
            if ((!ignoreHidden || !app.isHidden()) && (prefixOrderIndex = app.getPrefixOrderIndex()) > topPrefixOrderIndex) {
                topPrefixOrderIndex = prefixOrderIndex;
                topApp = app;
            }
        }
        return topApp;
    }

    public Interpolator getMagicWindowMoveInterpolator() {
        return this.mMagicWindowMoveInterpolator;
    }

    public void setMagicWindowMoveInterpolator(Interpolator interpolator) {
        this.mMagicWindowMoveInterpolator = interpolator;
    }

    public void setMagicWindowAnimation(boolean isStart, Animation enter, Animation exit) {
        if (isStart) {
            Animation[] animationArr = this.mActivityStartAnimations;
            animationArr[0] = enter;
            animationArr[1] = exit;
            return;
        }
        Animation[] animationArr2 = this.mActivityFinishAnimations;
        animationArr2[0] = enter;
        animationArr2[1] = exit;
    }

    public Animation getMagicWindowAnimation(Animation animation, boolean enter, int transit, AppWindowToken appWindowToken, Rect frame) {
        if (HwMwUtils.ENABLED && appWindowToken.inHwMagicWindowingMode()) {
            if (HwMwUtils.performPolicy(25, new Object[]{appWindowToken, Integer.valueOf(transit), Boolean.valueOf(enter)}).getBoolean("BUNDLE_IS_CLEAR_ANIMATION", false)) {
                return null;
            }
            if (!HwMwUtils.performPolicy((int) HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, new Object[]{appWindowToken.appPackageName}).getBoolean("RESULT_NEED_SYSTEM_ANIMATION", true)) {
                return animation;
            }
            WindowState windowState = appWindowToken.findMainWindow();
            if (appWindowToken.inHwMagicWindowingMode() && windowState != null) {
                frame.set(0, 0, (int) (((float) appWindowToken.getBounds().width()) * windowState.mMwUsedScaleFactor), (int) (((float) appWindowToken.getBounds().height()) * windowState.mMwUsedScaleFactor));
            }
            if (!isMagicWindowAnimation(transit)) {
                return animation;
            }
            boolean isStart = (transit == 9 || transit == 7 || transit == 25) ? false : true;
            if (transit == 12 && enter) {
                return animation;
            }
            if (isStart) {
                Animation[] animationArr = this.mActivityStartAnimations;
                return enter ? animationArr[0] : animationArr[1];
            }
            Animation[] animationArr2 = this.mActivityFinishAnimations;
            return enter ? animationArr2[0] : animationArr2[1];
        } else if (!HwMwUtils.ENABLED || enter || transit != 12 || !isAppLauncher(appWindowToken) || animation == null) {
            return animation;
        } else {
            return HwMagicWinAnimation.getMwWallpaperCloseAnimation();
        }
    }

    private boolean isMagicWindowAnimation(int transit) {
        return (transit == 13 || transit == 20 || transit == 10 || transit == 0) ? false : true;
    }

    public Animation loadAppWindowExitToLauncherAnimation(Animation a, int transit, Rect frame, AppWindowToken atoken) {
        boolean isSplitMode;
        Animation launcherEnterAnimation;
        if (!HwWmConstants.IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM) {
            return a;
        }
        if (atoken == null) {
            Slog.w(TAG, "find no atoken when try to override app exit to launcher animation");
            return a;
        }
        DisplayContent dc = atoken.getDisplayContent();
        if (dc == null) {
            Slog.w(TAG, "find no display content when try to override app exit to launcher animation");
            return a;
        }
        if (atoken.inHwMagicWindowingMode()) {
            isSplitMode = HwMwUtils.performPolicy((int) BigMemoryConstant.ACTIVITY_NAME_MAX_LEN, new Object[]{Integer.valueOf(atoken.getStack().mStackId), true}).getBoolean("RESULT_IN_APP_SPLIT", false);
        } else {
            isSplitMode = false;
        }
        if (isAppExitToLauncher(atoken, transit, dc) && frame != null && !isSplitMode) {
            Animation appExitToIconAnimation = HwAppTransitionImpl.createAppExitToIconAnimation(atoken, frame.height(), this.mExitIconWidth, this.mExitIconHeight, this.mExitPivotX, this.mExitPivotY, this.mExitIconBitmap, this.mExitFlag, this.mLazyModeOnEx);
            if (appExitToIconAnimation != null) {
                return appExitToIconAnimation;
            }
            return a;
        } else if (!isLauncherOpen(atoken, transit, dc) || frame == null || (launcherEnterAnimation = HwAppTransitionImpl.createLauncherEnterAnimation(atoken, frame.height(), this.mExitIconWidth, this.mExitIconHeight, this.mExitPivotX, this.mExitPivotY, this.mExitIconBitmap)) == null) {
            return a;
        } else {
            return launcherEnterAnimation;
        }
    }

    public void setRtgThreadForAnimation(boolean flag) {
        AwareBinderSchedManager.getInstance().setRtgThreadForAnimation(flag);
    }

    public void setHwSecureScreenShot(WindowState win) {
        WindowStateAnimator winAnimator = win.mWinAnimator;
        if (winAnimator.mSurfaceController != null) {
            if ((win.mAttrs.hwFlags & 4096) != 0) {
                if (!this.mSecureScreenShot.contains(win)) {
                    winAnimator.mSurfaceController.setSecureScreenShot(true);
                    this.mSecureScreenShot.add(win);
                    Slog.i(TAG, "Set SecureScreenShot by: " + win);
                }
            } else if (this.mSecureScreenShot.contains(win)) {
                this.mSecureScreenShot.remove(win);
                winAnimator.mSurfaceController.setSecureScreenShot(false);
                Slog.i(TAG, "Remove SecureScreenShot by: " + win);
            }
            if ((win.mAttrs.hwFlags & 8192) != 0) {
                if (!this.mSecureScreenRecords.contains(win)) {
                    winAnimator.mSurfaceController.setSecureScreenRecord(true);
                    this.mSecureScreenRecords.add(win);
                    Slog.i(TAG, "Set SecureScreenRecord by: " + win);
                }
            } else if (this.mSecureScreenRecords.contains(win)) {
                this.mSecureScreenRecords.remove(win);
                winAnimator.mSurfaceController.setSecureScreenRecord(false);
                Slog.i(TAG, "Remove SecureScreenRecord by: " + win);
            }
        }
        if (HwPCUtils.isInWindowsCastMode()) {
            if (isSecureWindow(win) && !this.mSecureWindows.contains(win)) {
                this.mSecureWindows.add(win);
            }
            showSecureWindowForWindowCastMode();
        }
    }

    private boolean isSecureWindow(WindowState win) {
        if (win == null) {
            return false;
        }
        if ((win.mAttrs.hwFlags & 4096) == 0 && (win.mAttrs.hwFlags & 8192) == 0 && (win.mAttrs.flags & 8192) == 0) {
            return false;
        }
        return true;
    }

    private void showSecureWindowForWindowCastMode() {
        boolean isNeedShowSecureWindow = false;
        for (int i = this.mSecureWindows.size() - 1; i >= 0; i--) {
            WindowState windowState = this.mSecureWindows.get(i);
            if (windowState == null || !isSecureWindow(windowState) || !windowState.isVisible()) {
                this.mSecureWindows.remove(i);
            } else {
                isNeedShowSecureWindow = true;
            }
        }
        if (!isNeedShowSecureWindow || this.mIWmsInner.getPolicy() == null || this.mIWmsInner.getPolicy().isKeyguardLocked()) {
            sHwWindowsCastManager.sendHideViewMsg(2);
        } else {
            sHwWindowsCastManager.sendShowViewMsg(2);
        }
    }

    public Point updateLazyModePoint(int type, Point point) {
        int width;
        int height;
        if (type == 0) {
            return point;
        }
        DisplayInfo defaultDisplayInfo = new DisplayInfo();
        this.mIWmsInner.getDefaultDisplayContentLocked().getDisplay().getDisplayInfo(defaultDisplayInfo);
        boolean isPortrait = defaultDisplayInfo.logicalHeight > defaultDisplayInfo.logicalWidth;
        if (isPortrait) {
            width = defaultDisplayInfo.logicalWidth;
        } else {
            width = defaultDisplayInfo.logicalHeight;
        }
        if (isPortrait) {
            height = defaultDisplayInfo.logicalHeight;
        } else {
            height = defaultDisplayInfo.logicalWidth;
        }
        float pendingX = 0.0f;
        float pendingY = 0.0f;
        if (type == 1) {
            pendingY = ((float) height) * 0.25f;
        } else if (type == 2) {
            pendingX = ((float) width) * 0.25f;
            pendingY = ((float) height) * 0.25f;
        }
        return new Point((int) ((((float) point.x) * 0.75f) + pendingX), (int) ((((float) point.y) * 0.75f) + pendingY));
    }

    public float getLazyModeScale() {
        return 0.75f;
    }

    public void takeTaskSnapshot(IBinder binder, boolean alwaysTake) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            AppWindowToken appWindowToken = this.mIWmsInner.getRoot().getAppWindowToken(binder);
            if (appWindowToken == null || (!alwaysTake && appWindowToken.mHadTakenSnapShot)) {
                Slog.v(TAG, "takeTaskSnapshot appWindowToken is null");
            } else {
                if (alwaysTake) {
                    appWindowToken.mHadTakenSnapShot = false;
                }
                WindowContainer wc = appWindowToken.getParent();
                if (wc instanceof Task) {
                    this.mIWmsInner.getTaskSnapshotController().snapshotTasks(Sets.newArraySet(new Task[]{(Task) wc}));
                    if (alwaysTake) {
                        appWindowToken.mHadTakenSnapShot = false;
                    }
                } else {
                    Slog.v(TAG, "takeTaskSnapshot has no tasks");
                }
            }
        }
    }

    public Rect getFocuseWindowVisibleFrame(WindowManagerService wms) {
        Rect currentRect;
        WindowState currentWindowState = wms.getFocusedWindow();
        if (currentWindowState != null && (currentRect = currentWindowState.getVisibleFrameLw()) != null) {
            return currentRect;
        }
        HwFreeFormUtils.log(TAG, "getFocuseWindowVisibleFrame is null");
        return null;
    }

    public String getTopAppPackageByWindowMode(int windowMode, RootWindowContainer mRoot) {
        TaskStack stack = mRoot.getStack(windowMode, 1);
        if (stack == null || stack.getTopChild() == null || stack.getTopChild().getTopFullscreenAppToken() == null) {
            return null;
        }
        return stack.getTopChild().getTopFullscreenAppToken().appPackageName;
    }

    public void relaunchIMEProcess() {
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.relaunchIMEIfNecessary();
            } catch (RemoteException e) {
                Log.e(TAG, "relaunchIMEProcess()");
            }
        }
    }

    public void togglePCMode(boolean pcmode, int displayId) {
        RootWindowContainer root = this.mIWmsInner.getRoot();
        if (root != null) {
            DisplayPolicy displayPolicy = null;
            DisplayContent dc = root.getDisplayContent(displayId);
            if (dc instanceof HwDisplayContent) {
                displayPolicy = dc.getDisplayPolicy();
            }
            if (pcmode) {
                HwPCUtils.log(TAG, "registerExternalPointerEventListener for screenlock");
                if (displayPolicy != null) {
                    displayPolicy.registerExternalPointerEventListener();
                    return;
                }
                return;
            }
            HwPCUtils.log(TAG, "unRegisterExternalPointerEventListener for screenlock");
            if (displayPolicy != null) {
                displayPolicy.unRegisterExternalPointerEventListener();
            }
            synchronized (this.mIWmsInner.getGlobalLock()) {
                if (dc instanceof HwDisplayContent) {
                    ((HwDisplayContent) dc).togglePCMode(pcmode);
                }
            }
        }
    }

    public Bitmap getDisplayBitmap(int displayId, int width, int height) {
        DisplayManagerInternal displayManagerInternal;
        RootWindowContainer root = this.mIWmsInner.getRoot();
        if (root == null) {
            return null;
        }
        ArrayList<WindowState> windows = new ArrayList<>();
        synchronized (this.mIWmsInner.getGlobalLock()) {
            root.forAllWindows(new ToBooleanFunction(windows) {
                /* class com.android.server.wm.$$Lambda$HwWindowManagerServiceEx$gEZYm7u4OWLqiLN3ywydYbHxaRY */
                private final /* synthetic */ ArrayList f$0;

                {
                    this.f$0 = r1;
                }

                public final boolean apply(Object obj) {
                    return HwWindowManagerServiceEx.lambda$getDisplayBitmap$0(this.f$0, (WindowState) obj);
                }
            }, false);
        }
        if (windows.size() > 0 || (displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)) == null) {
            return null;
        }
        IBinder token = displayManagerInternal.getDisplayToken(displayId);
        if (token != null) {
            return SurfaceControl.screenshot(token, width, height);
        }
        HwPCUtils.log(TAG, "getDisplayBitmap, getDisplayToken is null , displayId=" + displayId);
        return null;
    }

    static /* synthetic */ boolean lambda$getDisplayBitmap$0(ArrayList windows, WindowState w) {
        if (w == null || !HwPCUtils.isValidExtDisplayId(w.getDisplayId()) || w.mAttrs == null || (w.mAttrs.flags & 8192) == 0 || !w.isVisible()) {
            return false;
        }
        windows.add(w);
        return true;
    }

    public boolean isSecureForPCDisplay(WindowState w) {
        if (w.getStack() == null || !HwPCUtils.isExtDynamicStack(w.getStack().mStackId) || w.getDisplayInfo() == null || (w.getDisplayInfo().flags & 2) != 0) {
            return true;
        }
        return false;
    }

    public void setGestureNavMode(String packageName, int uid, int leftMode, int rightMode, int bottomMode) {
        GestureNavPolicy gestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
        if (gestureNavPolicy != null) {
            gestureNavPolicy.setGestureNavMode(packageName, uid, leftMode, rightMode, bottomMode);
        }
    }

    public ArrayList<WindowState> getSecureScreenWindow() {
        ArrayList<WindowState> secureScreenWindow = new ArrayList<>(2);
        secureScreenWindow.addAll(this.mSecureScreenRecords);
        secureScreenWindow.addAll(this.mSecureScreenShot);
        return secureScreenWindow;
    }

    public void removeSecureScreenWindow(WindowState win) {
        WindowStateAnimator winAnimator;
        if (win != null && (winAnimator = win.mWinAnimator) != null && winAnimator.mSurfaceController != null) {
            if (this.mSecureScreenRecords.contains(win)) {
                this.mSecureScreenRecords.remove(win);
                winAnimator.mSurfaceController.setSecureScreenRecord(false);
                Slog.i(TAG, "Remove SecureScreenRecord : " + win);
            }
            if (this.mSecureScreenShot.contains(win)) {
                this.mSecureScreenShot.remove(win);
                winAnimator.mSurfaceController.setSecureScreenShot(false);
                Slog.i(TAG, "Remove SecureScreenShot : " + win);
            }
        }
    }

    public void updateStatusBarInMagicWindow(int mode, WindowManager.LayoutParams attrs) {
        Bundle bundle;
        if (mode == 103 && HwMwUtils.ENABLED) {
            if (((attrs.flags & 1024) != 0 && (attrs.flags & Integer.MIN_VALUE) != 0) || attrs.type > 99 || attrs.type < 1 || attrs.type == 2) {
                return;
            }
            if ((attrs.type == 3 || "com.android.packageinstaller".equals(attrs.packageName)) && (bundle = HwMwUtils.performPolicy(101, new Object[]{Integer.valueOf(attrs.flags), attrs.packageName})) != null && bundle.size() != 0) {
                attrs.flags = bundle.getInt("enableStatusBar");
            }
        }
    }

    public final void performDisplayTraversalLocked() {
        this.mIWmsInner.getRoot().performDisplayTraversal();
    }

    public boolean isShowDimForPCMode(WindowContainer host, Rect outBounds) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return true;
        }
        int displayId = -1;
        if (host instanceof Task) {
            Task task = (Task) host;
            if (task.getDisplayContent() != null) {
                displayId = task.getDisplayContent().getDisplayId();
            }
        } else if (host instanceof TaskStack) {
            TaskStack taskStack = (TaskStack) host;
            if (taskStack.getDisplayContent() != null) {
                displayId = taskStack.getDisplayContent().getDisplayId();
            }
        }
        if (!HwPCUtils.isValidExtDisplayId(displayId)) {
            return true;
        }
        if (outBounds.width() >= host.getBounds().width() && outBounds.height() >= host.getBounds().height()) {
            return true;
        }
        HwPCUtils.log(TAG, "The dim's bounds is smaller. Skip to show dim.");
        return false;
    }

    public boolean isSkipComputeImeTargetForHwMultiDisplay(WindowManagerPolicy.WindowState inputMethodWin, DisplayContent dc) {
        if (inputMethodWin != null && dc != null && (inputMethodWin instanceof WindowState) && HwPCUtils.isPcCastModeInServer() && !HwPCUtils.isValidExtDisplayId(dc.getDisplayId())) {
            if (HwPCUtils.enabledInPad()) {
                return true;
            }
            if (HwPCUtils.mTouchDeviceID == -1 || !((WindowState) inputMethodWin).isVisible() || !HwPCUtils.isValidExtDisplayId(inputMethodWin.getDisplayId())) {
                return false;
            }
            return true;
        }
        return false;
    }

    public String getTouchedWinPackageName(float x, float y, int displayId) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(displayId);
            if (dc == null) {
                Slog.e(TAG, "getTouchedWinState error, dc is null, displayid: " + displayId);
                return "";
            }
            WindowState win = dc.getTouchableWinAtPointLocked(x, y);
            if (win == null) {
                Slog.e(TAG, "getTouchedWinState error, win is null, displayid: " + displayId);
                return "";
            }
            return win.getOwningPackage();
        }
    }

    public boolean notifyDragAndDropForMultiDisplay(float x, float y, int displayId, DragEvent evt) {
        synchronized (this.mIWmsInner.getGlobalLock()) {
            if (evt == null) {
                Slog.e(TAG, "notifyDragAndDropForMultiDisplay error, evt is null!");
                return false;
            }
            DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(displayId);
            if (dc == null) {
                Slog.e(TAG, "notifyDragAndDropForMultiDisplay error, dc is null " + displayId + ".  Aborting.");
                return false;
            }
            WindowState win = dc.getTouchableWinAtPointLocked(x, y);
            if (win == null) {
                Slog.e(TAG, "notifyDragAndDropForMultiDisplay error, win is null " + displayId + ".  Aborting.");
                return false;
            } else if (evt.getAction() == 10000) {
                this.mDropWindowState = win;
                return true;
            } else if (evt.getAction() == 10001) {
                this.mDropWindowState = null;
                sTouchWin = null;
                this.mDragLocWindowState = null;
                return true;
            } else {
                if (evt.getAction() == 5 && win != this.mDragLocWindowState) {
                    this.mDragLocWindowState = win;
                }
                if (this.mDropWindowState != null) {
                    if (evt.getAction() == 5) {
                        Slog.w(TAG, "check droppable while saved win not null, set saved win null");
                        this.mDropWindowState = null;
                    } else if (win != this.mDropWindowState) {
                        Slog.i(TAG, "win changed during drop, set current win as saved win.");
                        win = this.mDropWindowState;
                    } else {
                        Slog.d(TAG, "will dispatchDragEvent to win of viewroot.");
                    }
                }
                try {
                    win.mClient.dispatchDragEvent(convertDragEventForSplitWindowIfNeeded(dc, evt));
                    return true;
                } catch (RemoteException e) {
                    Slog.e(TAG, "can't send drop notification to win.");
                    return false;
                }
            }
        }
    }

    private DragEvent convertDragEventForSplitWindowIfNeeded(DisplayContent dc, DragEvent event) {
        WindowState windowState = sTouchWin;
        if (windowState != null) {
            return obtainDragEvent(windowState, event, null);
        }
        WindowState windowState2 = this.mDragLocWindowState;
        if (windowState2 == null || !windowState2.inMultiWindowMode()) {
            return event;
        }
        return obtainDragEvent(this.mDragLocWindowState, event, null);
    }

    private DragEvent obtainDragEvent(WindowState win, DragEvent event, IDragAndDropPermissions dragAndDropPermissions) {
        return DragEvent.obtain(event.getAction(), win.translateToWindowX(event.getX()), win.translateToWindowY(event.getY()), event.getLocalState(), event.getClipDescription(), event.getClipData(), dragAndDropPermissions, event.getResult());
    }

    public void registerDropListenerForMultiDisplay(IHwMultiDisplayDropStartListener listener) {
        Slog.i(TAG, "registerDropListenerForMultiDisplay, listener:" + listener);
        this.mDropListenerForMultiDisplay = listener;
    }

    public void unregisterDropListenerForMultiDisplay() {
        Slog.i(TAG, "unregisterDropListenerForMultiDisplay.");
        this.mDropListenerForMultiDisplay = null;
    }

    public boolean dropStartForMultiDisplay(DragEvent dragEvent) {
        Slog.i(TAG, "dropStartForMultiDisplay, mDropListenerForMultiDisplay:" + this.mDropListenerForMultiDisplay);
        if (!sHwMultiDisplayUtils.isInSinkWindowsCastMode()) {
            Slog.i(TAG, "dropStartForMultiDisplay  is not in cast mode");
            return false;
        }
        if (this.mDropListenerForMultiDisplay != null) {
            try {
                if (!(sTouchWin == null || sTouchWin.mGlobalScale == 0.0f)) {
                    dragEvent = DragEvent.obtain(dragEvent.getAction(), (dragEvent.getX() / sTouchWin.mGlobalScale) + ((float) sTouchWin.mWindowFrames.mFrame.left), (dragEvent.getY() / sTouchWin.mGlobalScale) + ((float) sTouchWin.mWindowFrames.mFrame.top), dragEvent.getLocalState(), dragEvent.getClipDescription(), dragEvent.getClipData(), null, dragEvent.getResult());
                }
                this.mDropListenerForMultiDisplay.onDropStart(dragEvent);
                return true;
            } catch (RemoteException e) {
                Slog.e(TAG, "onDragStart failed");
            }
        }
        return false;
    }

    public void setOriginalDropPoint(float x, float y) {
        if (!sHwMultiDisplayUtils.isInSinkWindowsCastMode()) {
            Slog.i(TAG, "setOriginalDropPoint is not in cast mode");
            return;
        }
        IHwMultiDisplayDropStartListener iHwMultiDisplayDropStartListener = this.mDropListenerForMultiDisplay;
        if (iHwMultiDisplayDropStartListener == null) {
            Slog.i(TAG, "mDropListenerForMultiDisplay is null");
            return;
        }
        try {
            iHwMultiDisplayDropStartListener.setOriginalDropPoint(x, y);
        } catch (RemoteException e) {
            Slog.e(TAG, "setOriginalDropPoint failed");
        }
    }

    public void updateDragState(int dragState) {
        Slog.i(TAG, "updateDragState: " + dragState);
        IHwMultiDisplayDragStateListener iHwMultiDisplayDragStateListener = this.mMultiDisplayDragStateListener;
        if (iHwMultiDisplayDragStateListener == null) {
            Slog.i(TAG, "mMultiDisplayDragStateListener is null");
            return;
        }
        try {
            iHwMultiDisplayDragStateListener.updateDragState(dragState);
        } catch (RemoteException e) {
            Slog.e(TAG, "updateDragState failed");
        }
    }

    public void registerHwMultiDisplayDragStateListener(IHwMultiDisplayDragStateListener listener) {
        Slog.i(TAG, "registerHwMultiDisplayDragStateListener.");
        this.mMultiDisplayDragStateListener = listener;
    }

    public void unregisterHwMultiDisplayDragStateListener() {
        Slog.i(TAG, "unregisterHwMultiDisplayDragStateListener.");
        this.mMultiDisplayDragStateListener = null;
    }

    public void registerDragListenerForMultiDisplay(IHwMultiDisplayDragStartListener listener) {
        Slog.i(TAG, "registerDragListenerForMultiDisplay, listener:" + listener);
        this.mDragListenerForMultiDisplay = listener;
        this.mDragLocWindowState = null;
        sTouchWin = null;
    }

    public void unregisterDragListenerForMultiDisplay() {
        Slog.i(TAG, "unregisterDragListenerForMultiDisplay.");
        this.mDragListenerForMultiDisplay = null;
        this.mIsDroppableListener = null;
        this.mDragLocWindowState = null;
        sTouchWin = null;
    }

    public void registerBitmapDragListenerForMultiDisplay(IHwMultiDisplayBitmapDragStartListener listener) {
        Slog.i(TAG, "registerBitmapDragListenerForMultiDisplayBitmap, listener:" + listener);
        this.mBitmapDragListenerForMultiDisplay = listener;
    }

    public void unregisterBitmapDragListenerForMultiDisplay() {
        Slog.i(TAG, "unregisterDragListenerForMultiDisplay.");
        this.mBitmapDragListenerForMultiDisplay = null;
    }

    public boolean dragStartForMultiDisplay(ClipData clipData) {
        Slog.i(TAG, "dragStartForMultiDisplay, mDragListenerForMultiDisplay:" + this.mDragListenerForMultiDisplay);
        if (sHwMultiDisplayUtils.isInWindowsCastMode() || sHwMultiDisplayUtils.isInSinkWindowsCastMode()) {
            IHwMultiDisplayDragStartListener iHwMultiDisplayDragStartListener = this.mDragListenerForMultiDisplay;
            if (iHwMultiDisplayDragStartListener != null) {
                try {
                    iHwMultiDisplayDragStartListener.onDragStart(clipData);
                    return true;
                } catch (RemoteException e) {
                    Slog.e(TAG, "onDragStart failed");
                }
            }
            return false;
        }
        Slog.i(TAG, "dragStartForMultiDisplay return false");
        return false;
    }

    public boolean setDragStartBitmap(Bitmap b) {
        IHwMultiDisplayBitmapDragStartListener iHwMultiDisplayBitmapDragStartListener;
        Slog.i(TAG, "setDragStartBitmap, listener:" + this.mBitmapDragListenerForMultiDisplay);
        if (sHwMultiDisplayUtils.isInWindowsCastMode() && (iHwMultiDisplayBitmapDragStartListener = this.mBitmapDragListenerForMultiDisplay) != null) {
            try {
                iHwMultiDisplayBitmapDragStartListener.onDragStart(b);
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                }
                return true;
            } catch (RemoteException e) {
                Slog.e(TAG, "onDragStart failed");
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                }
            } catch (Throwable th) {
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                }
                throw th;
            }
        }
        return false;
    }

    public void registerIsDroppableForMultiDisplay(IHwMultiDisplayDroppableListener listener) {
        Slog.i(TAG, "registerIsDroppableForMultiDisplay.");
        this.mIsDroppableListener = listener;
    }

    public void setDroppableForMultiDisplay(float posX, float posY, boolean result) {
        if (!sHwMultiDisplayUtils.isInWindowsCastMode()) {
            Slog.i(TAG, "setDroppableForMultiDisplay: not in window cast mode.");
        } else if (this.mIsDroppableListener == null) {
            Slog.i(TAG, "mIsDroppableListener is null.");
        } else {
            try {
                Slog.i(TAG, "setDroppableForMultiDisplay " + result);
                if (!(this.mDragLocWindowState == null || this.mDragLocWindowState.mGlobalScale == 0.0f || !this.mDragLocWindowState.inMultiWindowMode())) {
                    posX = (posX / this.mDragLocWindowState.mGlobalScale) + ((float) this.mDragLocWindowState.mWindowFrames.mFrame.left);
                    posY = (posY / this.mDragLocWindowState.mGlobalScale) + ((float) this.mDragLocWindowState.mWindowFrames.mFrame.top);
                }
                this.mIsDroppableListener.onDroppableResult(posX, posY, result);
            } catch (RemoteException e) {
                Slog.e(TAG, "setDroppableForMultiDisplay RemoteException.");
            }
        }
    }

    public void sendFocusProcessToRMS(WindowState curFocus, WindowState oldFocus) {
        int newPid = -1;
        int oldPid = -1;
        if (!(curFocus == null || curFocus.mSession == null)) {
            newPid = curFocus.mSession.mPid;
        }
        if (!(oldFocus == null || oldFocus.mSession == null)) {
            oldPid = oldFocus.mSession.mPid;
        }
        if (newPid > 0 && newPid != oldPid && newPid != lastFocusPid) {
            lastFocusPid = newPid;
            Bundle args = new Bundle();
            args.putInt(SceneRecogFeature.DATA_PID, newPid);
            args.putInt(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, 3);
            this.mIWmsInner.getWMMonitor().reportData("RESOURCE_WINSTATE", System.currentTimeMillis(), args);
        }
    }

    public void setNotchFlags(WindowState win, WindowManager.LayoutParams attrs, DisplayPolicy displayPolicy, int systemUiFlags) {
        if (win != null && attrs != null && displayPolicy != null && displayPolicy.getHwDisplayPolicyEx().canUpdateDisplayFrames(win, attrs, systemUiFlags)) {
            attrs.layoutInDisplayCutoutMode = 1;
        }
    }

    public void switchDragShadow(boolean droppable) {
        try {
            sSwitched = true;
            sDragWin.mClient.dispatchDragEvent(DragEvent.obtain(7, 0.0f, 0.0f, null, null, null, null, droppable));
        } catch (RemoteException e) {
            Slog.e(TAG, "can't siwtchDragShadwo to windows");
        }
    }

    public void restoreShadow() {
        try {
            sSwitched = false;
            sDragWin.mClient.dispatchDragEvent(DragEvent.obtain(9, 0.0f, 0.0f, null, null, null, null, true));
        } catch (RemoteException e) {
            Slog.e(TAG, "can't restoreShadow to windows");
        }
    }

    public void setTouchWinState(WindowState win) {
        sTouchWin = win;
    }

    public void setDragWinState(WindowState win) {
        sDragWin = win;
        sSwitched = false;
    }

    public String getDragSrcPkgName() {
        return sDragWin.getOwningPackage();
    }

    public boolean isSwitched() {
        return sSwitched;
    }

    public void updateFocusWindowFreezed(boolean isGainFocus) {
        WindowState win;
        if (this.mGainFocus != isGainFocus) {
            this.mGainFocus = isGainFocus;
            synchronized (this.mIWmsInner.getGlobalLock()) {
                win = this.mIWmsInner.getDefaultDisplayContentLocked().mCurrentFocus;
            }
            if (win != null) {
                Slog.i(TAG, "reportFocusChangedSerialized:" + isGainFocus);
                win.reportFocusChangedSerialized(isGainFocus, true);
            }
        }
    }

    @GuardedBy({"mPhoneOperateListenerForMultiDisplay"})
    public void registerPhoneOperateListenerForHwMultiDisplay(IHwMultiDisplayPhoneOperateListener listener) {
        this.mPhoneOperateListenerForMultiDisplay = listener;
    }

    @GuardedBy({"mPhoneOperateListenerForMultiDisplay"})
    public void unregisterPhoneOperateListenerForHwMultiDisplay() {
        this.mPhoneOperateListenerForMultiDisplay = null;
    }

    @GuardedBy({"mPhoneOperateListenerForMultiDisplay"})
    public void onOperateOnPhone() {
        Slog.i(TAG, "onOperateOnPhone");
        if (this.mPhoneOperateListenerForMultiDisplay == null) {
            Slog.e(TAG, "mPhoneOperateListenerForMultiDisplay is null.");
            return;
        }
        try {
            Slog.d(TAG, "onOperateOnPhone()");
            this.mPhoneOperateListenerForMultiDisplay.onOperateOnPhone();
        } catch (RemoteException e) {
            Slog.e(TAG, "onOperateOnPhone RemoteException.");
        }
    }

    public int getTopActivityAdaptNotchState(String packageName) {
        return this.mIWmsInner.getService().getDefaultDisplayContentLocked().getTopActivityAdaptNotchState(packageName);
    }

    public Animation reloadHwSplitScreenOpeningAnimation(Animation animation, AppWindowToken token, ArraySet<AppWindowToken> openingApps, boolean isEnter) {
        if (animation == null || token == null || openingApps == null) {
            return animation;
        }
        int hwSplitScreensCount = 0;
        boolean isOpenTwoSplitScreens = false;
        Iterator<AppWindowToken> it = openingApps.iterator();
        while (true) {
            if (it.hasNext()) {
                AppWindowToken appWindowToken = it.next();
                if (appWindowToken != null && appWindowToken.inHwSplitScreenWindowingMode()) {
                    hwSplitScreensCount++;
                    continue;
                }
                if (hwSplitScreensCount > 1) {
                    isOpenTwoSplitScreens = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (!isOpenTwoSplitScreens) {
            return animation;
        }
        if (!isEnter) {
            animation.setZAdjustment(1);
        } else if (token.inHwSplitScreenWindowingMode()) {
            animation.setStartOffset(animation.computeDurationHint());
            animation.setDuration(0);
            animation.setZAdjustment(0);
        }
        return animation;
    }
}
