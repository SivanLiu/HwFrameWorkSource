package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.WindowManager.LayoutParams;
import com.android.server.am.ActivityRecord;
import com.android.server.am.HwActivityRecord;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.input.InputWindowHandle;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.google.android.collect.Sets;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import huawei.android.hwutil.HwFullScreenDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public final class HwWindowManagerServiceEx implements IHwWindowManagerServiceEx {
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
    private static final String ASSOC_WINDOW_HEIGHT = "height";
    private static final String ASSOC_WINDOW_MODE = "windowmode";
    private static final String ASSOC_WINDOW_PHIDE = "permanentlyhidden";
    private static final String ASSOC_WINDOW_TYPE = "windowtype";
    private static final String ASSOC_WINDOW_WIDTH = "width";
    private static final int FORBIDDEN_ADDVIEW_BROADCAST = 1;
    public static final int NOTIFY_FINGER_WIN_COVERED = 101;
    private static final String RESOURCE_APPASSOC = "RESOURCE_APPASSOC";
    private static final String RESOURCE_SYSLOAD = "RESOURCE_SYSLOAD";
    private static final String SYSLOAD_SINGLEHAND_TYPE = "LazyMode";
    static final String TAG = "HwWindowManagerServiceEx";
    private static final int UPDATE_WINDOW_STATE = 0;
    final Context mContext;
    private HandlerThread mHandlerThread = new HandlerThread("hw_ops_handler_thread");
    private Handler mHwHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 101 && HwWindowManagerServiceEx.this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
                boolean z = true;
                if (msg.arg1 != 1) {
                    z = false;
                }
                boolean covered = z;
                Rect frame = msg.obj;
                if (((FingerprintManager) HwWindowManagerServiceEx.this.mContext.getSystemService("fingerprint")) != null) {
                    String str = HwWindowManagerServiceEx.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMessage: NOTIFY_FINGER_WIN_COVERED covered=");
                    stringBuilder.append(covered);
                    stringBuilder.append(" frame=");
                    stringBuilder.append(frame);
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
    };
    IHwWindowManagerInner mIWmsInner = null;
    private Rect mLastCoveredFrame = new Rect();
    private boolean mLastCoveredState;
    private OpsUpdateHandler mOpsHandler;
    private int mPCScreenDisplayMode = 0;
    private float mPCScreenScale = 1.0f;

    private class OpsUpdateHandler extends Handler {
        public OpsUpdateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    HwWindowManagerServiceEx.this.mIWmsInner.updateAppOpsState();
                    return;
                case 1:
                    HwWindowManagerServiceEx.this.sendForbiddenBroadcast(msg.getData());
                    return;
                default:
                    return;
            }
        }
    }

    public HwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        this.mIWmsInner = wms;
        this.mContext = context;
        this.mHandlerThread.start();
        this.mOpsHandler = new OpsUpdateHandler(this.mHandlerThread.getLooper());
    }

    public void onChangeConfiguration(MergedConfiguration mergedConfiguration, WindowState ws) {
        if (HwPCUtils.enabled() && HwPCUtils.isPcCastModeInServer() && ws != null && HwPCUtils.isValidExtDisplayId(ws.getDisplayId()) && mergedConfiguration != null && ws.getTask() != null && ws.getTask().isFullscreen()) {
            Configuration cf = mergedConfiguration.getOverrideConfiguration();
            DisplayContent dc = ws.getDisplayContent();
            if (cf != null && dc != null) {
                DisplayInfo displayInfo = ws.getDisplayInfo();
                if (displayInfo != null) {
                    int displayWidth = displayInfo.logicalWidth;
                    int displayHeight = displayInfo.logicalHeight;
                    float scale = ((float) displayInfo.logicalDensityDpi) / 160.0f;
                    cf.screenWidthDp = (int) ((((float) displayWidth) / scale) + 0.5f);
                    cf.screenHeightDp = (int) ((((float) displayHeight) / scale) + 0.5f);
                    mergedConfiguration.setOverrideConfiguration(cf);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set pc fullscreen, width:");
                    stringBuilder.append(displayWidth);
                    stringBuilder.append(" height:");
                    stringBuilder.append(displayHeight);
                    stringBuilder.append(" scale:");
                    stringBuilder.append(scale);
                    stringBuilder.append(" cf.screenWidthDp:");
                    stringBuilder.append(cf.screenWidthDp);
                    stringBuilder.append(" cf.screenHeightDp:");
                    stringBuilder.append(cf.screenHeightDp);
                    HwPCUtils.log(str, stringBuilder.toString());
                }
            }
        }
    }

    private boolean isInputTargetWindow(WindowState windowState, WindowState inputTargetWin) {
        boolean z = false;
        if (inputTargetWin == null) {
            return false;
        }
        Task inputMethodTask = inputTargetWin.getTask();
        Task task = windowState.getTask();
        if (inputMethodTask == null || task == null) {
            return false;
        }
        if (inputMethodTask.mTaskId == task.mTaskId) {
            z = true;
        }
        return z;
    }

    public void adjustWindowPosForPadPC(Rect containingFrame, Rect contentFrame, WindowState imeWin, WindowState inputTargetWin, WindowState win) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad() && HwPCUtils.isValidExtDisplayId(win.getDisplayId()) && imeWin != null && imeWin.isVisibleNow() && isInputTargetWindow(win, inputTargetWin)) {
            int windowState = -1;
            ActivityRecord r = ActivityRecord.forToken(win.getAttrs().token);
            if (r != null) {
                if (r instanceof HwActivityRecord) {
                    windowState = ((HwActivityRecord) r).getWindowState();
                }
                if (windowState != -1 && !HwPCMultiWindowCompatibility.isLayoutFullscreen(windowState) && !HwPCMultiWindowCompatibility.isLayoutMaximized(windowState) && !contentFrame.isEmpty() && containingFrame.bottom > contentFrame.bottom) {
                    int D1 = contentFrame.bottom - containingFrame.bottom;
                    int D2 = contentFrame.top - containingFrame.top;
                    int offsetY = D1 > D2 ? D1 : D2;
                    if (offsetY < 0) {
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
        this.mOpsHandler.removeMessages(0);
        this.mOpsHandler.sendEmptyMessage(0);
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("this win:");
            stringBuilder.append(win);
            stringBuilder.append(" hase children size:");
            stringBuilder.append(N);
            Slog.i(str, stringBuilder.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkFullWindowWithoutTransparent = true , don't allow anyway,");
        stringBuilder.append(win);
        Slog.i(str, stringBuilder.toString());
        return false;
    }

    private void sendForbiddenMessage(WindowState win) {
        Message msg = this.mOpsHandler.obtainMessage(1);
        Bundle bundle = new Bundle();
        bundle.putInt("uid", win.getOwningUid());
        bundle.putString("package", win.getOwningPackage());
        msg.setData(bundle);
        this.mOpsHandler.sendMessage(msg);
    }

    private void sendForbiddenBroadcast(Bundle data) {
        Intent preventIntent = new Intent("com.android.server.wm.addview.preventnotify");
        preventIntent.putExtras(data);
        this.mContext.sendBroadcastAsUser(preventIntent, UserHandle.ALL);
    }

    private boolean checkFullWindowWithoutTransparent(LayoutParams attrs) {
        return -1 == attrs.width && -1 == attrs.height && 0.0d != ((double) attrs.alpha);
    }

    public void setVisibleFromParent(WindowState win) {
        if (parentHiddenByAppOp(win)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parent is hidden by app ops, should also hide this win:");
            stringBuilder.append(win);
            Slog.i(str, stringBuilder.toString());
            setWinAndChildrenVisibility(win, false);
        }
    }

    private boolean parentHiddenByAppOp(WindowState win) {
        if (win == null || !win.isChildWindow()) {
            return false;
        }
        if (win.getParentWindow().mAppOpVisibility) {
            return parentHiddenByAppOp(win.getParentWindow());
        }
        return true;
    }

    public void checkSingleHandMode(AppWindowToken oldFocus, AppWindowToken newFocus) {
        if ((oldFocus != newFocus) && newFocus != null) {
            int requestedOrientation = newFocus.mOrientation;
            if (requestedOrientation == 0 || requestedOrientation == 6 || requestedOrientation == 8 || requestedOrientation == 11) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("requestedOrientation: ");
                stringBuilder.append(requestedOrientation);
                Slog.i(str, stringBuilder.toString());
                Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
            }
        }
    }

    public void updateSurfacePositionForPCMode(WindowState win, Point outPoint) {
        WindowState windowState = win;
        Point point = outPoint;
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(win.getDisplayId()) && getPCScreenDisplayMode() != 0) {
            DisplayInfo di = win.getDisplayInfo();
            DisplayInfo displayInfo;
            if (di == null) {
                displayInfo = di;
            } else if (win.getDisplayContent() == null) {
                displayInfo = di;
            } else {
                int width = di.logicalWidth;
                int height = di.logicalHeight;
                float pcScreenScale = this.mPCScreenScale;
                float pendingX = (((float) width) * (1.0f - pcScreenScale)) / 2.0f;
                float pendingY = (((float) height) * (1.0f - pcScreenScale)) / 2.0f;
                Rect surfaceInsets = windowState.mAttrs.surfaceInsets;
                point.x = (int) ((((float) win.getFrameLw().left) * pcScreenScale) + pendingX);
                point.y = (int) ((((float) win.getFrameLw().top) * pcScreenScale) + pendingY);
                WindowContainer parentWindowContainer = win.getParent();
                if (win.isChildWindow()) {
                    WindowState parent = win.getParentWindow();
                    Rect parentSurfaceInsets = parent.mAttrs.surfaceInsets;
                    Rect parentFrame = parent.getFrameLw();
                    point.offset((int) (((float) ((-parentFrame.left) + parentSurfaceInsets.left)) * pcScreenScale), (int) (((float) ((-parentFrame.top) + parentSurfaceInsets.top)) * pcScreenScale));
                } else {
                    if (parentWindowContainer != null) {
                        Rect parentBounds = parentWindowContainer.getBounds();
                        point.offset(-((int) (((float) parentBounds.left) * pcScreenScale)), -((int) (((float) parentBounds.top) * pcScreenScale)));
                    }
                }
                di = win.getStack();
                if (di != null) {
                    int outset = di.getStackOutset();
                    point.offset((int) (((float) outset) * pcScreenScale), (int) (((float) outset) * pcScreenScale));
                }
                point.offset(-((int) (((float) surfaceInsets.left) * pcScreenScale)), -((int) (((float) surfaceInsets.top) * pcScreenScale)));
                windowState.mOverscanPosition.set(point.x, point.y);
            }
            HwPCUtils.log(TAG, "fail to get display info");
            return;
        }
    }

    public void updateDimPositionForPCMode(WindowContainer host, Rect outBounds) {
        if (HwPCUtils.isPcCastModeInServer() && getPCScreenDisplayMode() != 0) {
            int displayId = -1;
            int screenWidth = 0;
            int screenHeight = 0;
            float pcScreenScale = getPCScreenScale();
            if (host instanceof Task) {
                Task task = (Task) host;
                if (task.getDisplayContent() != null) {
                    displayId = task.getDisplayContent().getDisplayId();
                    screenWidth = task.getDisplayContent().mInitialDisplayWidth;
                    screenHeight = task.getDisplayContent().mInitialDisplayHeight;
                }
            } else if (host instanceof TaskStack) {
                TaskStack taskStack = (TaskStack) host;
                if (taskStack.getDisplayContent() != null) {
                    displayId = taskStack.getDisplayContent().getDisplayId();
                    screenWidth = taskStack.getDisplayContent().mInitialDisplayWidth;
                    screenHeight = taskStack.getDisplayContent().mInitialDisplayHeight;
                }
            }
            if (!HwPCUtils.isValidExtDisplayId(displayId)) {
                return;
            }
            int left;
            int top;
            if (host.getParent() == null) {
                left = (int) ((((float) outBounds.left) * pcScreenScale) + ((((float) screenWidth) * (1.0f - pcScreenScale)) / 2.0f));
                top = (int) ((((float) outBounds.top) * pcScreenScale) + ((((float) screenHeight) * (1.0f - pcScreenScale)) / 2.0f));
                outBounds.set(left, top, (int) (((float) left) + (((float) outBounds.width()) * pcScreenScale)), (int) (((float) top) + (((float) outBounds.height()) * pcScreenScale)));
            } else if (outBounds.right >= 0 || outBounds.bottom >= 0) {
                left = (int) (((((float) outBounds.left) * pcScreenScale) + ((((float) screenWidth) * (1.0f - pcScreenScale)) / 2.0f)) - 1.0f);
                top = (int) (((((float) outBounds.top) * pcScreenScale) + ((((float) screenHeight) * (1.0f - pcScreenScale)) / 2.0f)) - 1.0f);
                outBounds.set(left, top, ((int) ((((float) outBounds.width()) * pcScreenScale) + 1.0f)) + left, ((int) ((((float) outBounds.height()) * pcScreenScale) + 1.0f)) + top);
            } else {
                outBounds.left = (int) (((float) outBounds.left) * pcScreenScale);
                outBounds.top = (int) (((float) outBounds.top) * pcScreenScale);
            }
        }
    }

    public int getPCScreenDisplayMode() {
        return this.mPCScreenDisplayMode;
    }

    public float getPCScreenScale() {
        return this.mPCScreenScale;
    }

    public void computeShownFrameLockedByPCScreenDpMode(int curMode) {
        this.mPCScreenDisplayMode = curMode;
        if (curMode == 0) {
            this.mPCScreenScale = 1.0f;
        } else if (curMode == 1) {
            this.mPCScreenScale = 0.95f;
        } else if (curMode == 2) {
            this.mPCScreenScale = 0.9f;
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
        synchronized (this.mIWmsInner.getWindowMap()) {
            for (WindowState win : this.mIWmsInner.getWindowMap().values()) {
                if (!(win == null || win.mAttrs == null || win.mSession == null)) {
                    if (!windows.containsKey(win)) {
                        if (win.mAppOp == 24) {
                            if (this.mIWmsInner.getAppOps() == null) {
                                return;
                            }
                            windows.put(win, Integer.valueOf(this.mIWmsInner.getAppOps().startOpNoThrow(win.mAppOp, win.getOwningUid(), win.getOwningPackage())));
                        }
                    }
                }
            }
        }
    }

    public List<Bundle> getVisibleWindows(int ops) {
        ArrayMap<WindowState, Integer> windows = new ArrayMap();
        getVisibleWindows(windows, ops);
        List<Bundle> windowsList = new ArrayList();
        for (Entry<WindowState, Integer> win : windows.entrySet()) {
            WindowState state = (WindowState) win.getKey();
            Bundle bundle = new Bundle();
            bundle.putInt("window_pid", state.mSession.mPid);
            bundle.putInt("window_value", ((Integer) win.getValue()).intValue());
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
        synchronized (this.mIWmsInner.getWindowMap()) {
            for (WindowState win : this.mIWmsInner.getWindowMap().values()) {
                if (!(win == null || win.mAttrs == null || win.mSession == null)) {
                    if (!windows.containsKey(win)) {
                        if (win.mAttrs.type == HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_OK_NOTIFY) {
                            windows.put(win, Integer.valueOf(3));
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
        WindowState windowState = win;
        boolean z = windowState != null && windowState.getAttrs().type == HwArbitrationDEFS.MSG_MPLINK_BIND_CHECK_OK_NOTIFY;
        boolean isToast = z;
        if (windowState == null || (!(windowState.mAppOp == 24 || isToast) || windowState.mSession == null)) {
            return;
        }
        if (this.mIWmsInner.getWMMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            updateVisibleWindows(eventType, mode, isToast ? 45 : 24, windowState, requestedWidth, requestedHeight, isupdate);
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

    public void updateAppOpsStateReport(int ops, String packageName) {
        if (ops == 24 && this.mIWmsInner.getWMMonitor().isResourceNeeded(RESOURCE_APPASSOC)) {
            updateVisibleWindowsOps(10, packageName);
        }
    }

    public void notifyFingerWinCovered(boolean covered, Rect frame) {
        if (this.mLastCoveredState != covered || !this.mLastCoveredFrame.equals(frame)) {
            this.mHwHandler.sendMessage(this.mHwHandler.obtainMessage(101, covered, 0, frame));
            this.mLastCoveredState = covered;
            this.mLastCoveredFrame.set(frame);
        }
    }

    public int getFocusWindowWidth(WindowState mCurrentFocus, WindowState mInputMethodTarget) {
        WindowState mFocusWindow;
        if (mInputMethodTarget == null) {
            mFocusWindow = mCurrentFocus;
        } else {
            mFocusWindow = mInputMethodTarget;
        }
        if (mFocusWindow == null) {
            Log.e(TAG, "WMS getFocusWindowWidth error");
            return 0;
        }
        Rect rect;
        if (mFocusWindow.getAttrs().type == 2) {
            rect = mFocusWindow.getDisplayFrameLw();
        } else {
            rect = mFocusWindow.getContentFrameLw();
        }
        return rect.width();
    }

    public void reportLazyModeToIAware(int lazyMode) {
        Bundle args = new Bundle();
        args.putInt(SYSLOAD_SINGLEHAND_TYPE, lazyMode);
        this.mIWmsInner.getWMMonitor().reportData(RESOURCE_SYSLOAD, System.currentTimeMillis(), args);
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

    /* JADX WARNING: Missing block: B:27:0x006d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void getCurrFocusedWinInExtDisplay(Bundle outBundle) {
        if (outBundle != null) {
            synchronized (this.mIWmsInner.getWindowMap()) {
                if (this.mIWmsInner.getInputMonitor() == null) {
                    return;
                }
                InputWindowHandle inputWindowHandle = this.mIWmsInner.getInputMonitor().getFousedWinExtDisplayInPCCastMode();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCurrFocusedWinInExtDisplay inputWindowHandle = ");
                stringBuilder.append(inputWindowHandle);
                HwPCUtils.log(str, stringBuilder.toString());
                if (inputWindowHandle != null && (inputWindowHandle.windowState instanceof WindowState)) {
                    WindowState ws = inputWindowHandle.windowState;
                    Parcelable parcelable = null;
                    outBundle.putString(AwareIntelligentRecg.CMP_PKGNAME, ws == null ? null : ws.getAttrs().packageName);
                    boolean isApp = false;
                    if (!(ws == null || ws.getAppToken() == null)) {
                        isApp = true;
                    }
                    outBundle.putBoolean("isApp", isApp);
                    String str2 = "bounds";
                    if (isApp) {
                        parcelable = ws.getBounds();
                    }
                    outBundle.putParcelable(str2, parcelable);
                }
            }
        }
    }

    public boolean hasLighterViewInPCCastMode() {
        synchronized (this.mIWmsInner.getWindowMap()) {
            if (this.mIWmsInner.getInputMonitor() == null) {
                return false;
            }
            boolean hasLighterViewInPCCastMode = this.mIWmsInner.getInputMonitor().hasLighterViewInPCCastMode();
            return hasLighterViewInPCCastMode;
        }
    }

    public boolean shouldDropMotionEventForTouchPad(float x, float y) {
        DisplayContent dc = this.mIWmsInner.getRoot().getDisplayContent(0);
        if (dc != null && (dc instanceof HwDisplayContent)) {
            return ((HwDisplayContent) dc).shouldDropMotionEventForTouchPad(x, y);
        }
        return false;
    }

    public void updateHwStartWindowRecord(String packageName) {
        HwStartWindowRecord.getInstance().resetStartWindowApp(packageName);
    }

    public HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(TaskSnapshotController mTaskSnapshotController, WindowState focusedWindow, boolean refresh) {
        TaskSnapshot taskSnapshot;
        if (refresh) {
            mTaskSnapshotController.clearForegroundTaskSnapshot();
        }
        if (focusedWindow == null || !refresh) {
            taskSnapshot = mTaskSnapshotController.getForegroundTaskSnapshot();
        } else {
            taskSnapshot = mTaskSnapshotController.createForegroundTaskSnapshot(focusedWindow.mAppToken);
        }
        HwTaskSnapshotWrapper hwTaskSnapshotWrapper = new HwTaskSnapshotWrapper();
        hwTaskSnapshotWrapper.setTaskSnapshot(taskSnapshot);
        return hwTaskSnapshotWrapper;
    }

    public void takeTaskSnapshot(IBinder binder) {
        AppWindowToken appWindowToken = this.mIWmsInner.getRoot().getAppWindowToken(binder);
        if (appWindowToken != null) {
            WindowContainer wc = appWindowToken.getParent();
            if (wc == null || !(wc instanceof Task)) {
                Slog.v(TAG, "takeTaskSnapshot has no tasks");
                return;
            }
            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{(Task) wc});
            synchronized (this.mIWmsInner.getWindowMap()) {
                this.mIWmsInner.getTaskSnapshotController().snapshotTasks(tasks);
            }
            return;
        }
        Slog.v(TAG, "takeTaskSnapshot appWindowToken is null");
    }
}
