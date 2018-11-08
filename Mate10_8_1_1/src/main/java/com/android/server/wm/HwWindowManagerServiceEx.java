package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.WindowManager.LayoutParams;
import com.android.server.am.ActivityRecord;
import com.android.server.am.HwActivityRecord;
import com.android.server.gesture.GestureNavConst;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import java.util.ArrayList;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public final class HwWindowManagerServiceEx implements IHwWindowManagerServiceEx {
    private static final boolean IS_GESTURE_PROP = SystemProperties.getBoolean("ro.config.gesture_nav_support", false);
    private static final String KEY_SECURE_GESTURE_NAVIGATION = "secure_gesture_navigation";
    private static final int MODE_OFF = 0;
    private static final int MODE_ON = 1;
    static final String TAG = "HwWindowManagerServiceEx";
    final Context mContext;
    private boolean mDisableSnapshots;
    IHwWindowManagerInner mIWmsInner = null;
    private final ArrayList<WindowState> mWindowStateList = new ArrayList();

    private final class GestureSettingObserver extends ContentObserver {
        private final Uri mGestureUri = Secure.getUriFor("secure_gesture_navigation");

        public GestureSettingObserver() {
            super(new Handler());
            HwWindowManagerServiceEx.this.mContext.getContentResolver().registerContentObserver(this.mGestureUri, false, this, -2);
            HwWindowManagerServiceEx.this.setDisableSnapshotsByGesture();
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mGestureUri.equals(uri)) {
                HwWindowManagerServiceEx.this.setDisableSnapshotsByGesture();
            }
        }
    }

    public HwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        this.mIWmsInner = wms;
        this.mContext = context;
        if (IS_GESTURE_PROP) {
            GestureSettingObserver gestureSettingObserver = new GestureSettingObserver();
        }
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
                    cf.setAppBounds(0, 0, displayWidth, displayHeight);
                    float scale = ((float) displayInfo.logicalDensityDpi) / 160.0f;
                    cf.screenWidthDp = (int) ((((float) displayWidth) / scale) + 0.5f);
                    cf.screenHeightDp = (int) ((((float) displayHeight) / scale) + 0.5f);
                    HwPCUtils.log(TAG, "set pc fullscreen, width:" + displayWidth + " height:" + displayHeight + " scale:" + scale + " cf.screenWidthDp:" + cf.screenWidthDp + " cf.screenHeightDp:" + cf.screenHeightDp);
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
                if (windowState != -1 && (HwPCMultiWindowCompatibility.isLayoutFullscreen(windowState) ^ 1) != 0 && (HwPCMultiWindowCompatibility.isLayoutMaximized(windowState) ^ 1) != 0 && !contentFrame.isEmpty() && containingFrame.bottom > contentFrame.bottom) {
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
                int top = (imeWin.getDisplayFrameLw().top > imeWin.getContentFrameLw().top ? imeWin.getDisplayFrameLw().top : imeWin.getContentFrameLw().top) + imeWin.getGivenContentInsetsLw().top;
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

    public List<String> getNotchSystemApps() {
        return HwNotchScreenWhiteConfig.getInstance().getNotchSystemApps();
    }

    public int getAppUseNotchMode(String packageName) {
        return HwNotchScreenWhiteConfig.getInstance().getAppUseNotchMode(packageName);
    }

    public boolean isAppNotchSupport(String packageName) {
        return HwNotchScreenWhiteConfig.getInstance().isAppNotchSupport(packageName);
    }

    public int getFocusWindowWidth(WindowState mCurrentFocus, WindowState mInputMethodTarget) {
        WindowState mFocusWindow;
        if (mInputMethodTarget == null) {
            mFocusWindow = mCurrentFocus;
        } else {
            mFocusWindow = mInputMethodTarget;
        }
        if (mFocusWindow == null) {
            Log.e("TAG", "WMS getFocusWindowWidth error");
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

    public void setViewAlpha(float alpha, RootWindowContainer root) {
        SurfaceControl.openTransaction();
        if (alpha > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            for (WindowState w : this.mWindowStateList) {
                if (!(w == null || w.mAppToken == null)) {
                    w.mWinAnimator.setViewAlpha(alpha);
                    w.mAppToken.hiddenRequested = false;
                }
            }
        } else {
            this.mWindowStateList.clear();
            root.forAllWindows(new -$Lambda$LKBDmK5wNnohAzS-c0F058QFNUI(alpha, this), false);
        }
        SurfaceControl.closeTransaction();
    }

    /* synthetic */ void lambda$-com_android_server_wm_HwWindowManagerServiceEx_10926(float alpha, WindowState w) {
        if (!(w == null || !w.isVisible() || w.mAppToken == null)) {
            w.mWinAnimator.setViewAlpha(alpha);
            w.mAppToken.hiddenRequested = true;
            this.mWindowStateList.add(w);
        }
        if (w != null && (w.mAttrs.flags & 2) != 0) {
            LayoutParams layoutParams = w.mAttrs;
            layoutParams.flags &= -3;
            w.getAttrs().hwFlags = (w.getAttrs().hwFlags & -1048577) | HighBitsCompModeID.MODE_COLOR_ENHANCE;
            Log.i(TAG, "Gesture to remove dimlayer, Window: " + w.toString());
        }
    }

    private void setDisableSnapshotsByGesture() {
        boolean z = true;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "secure_gesture_navigation", 0, -2) != 1) {
            z = false;
        }
        this.mDisableSnapshots = z;
        Slog.i(TAG, "HwGesture has change, is open: " + this.mDisableSnapshots);
    }

    public boolean getGestureState() {
        return this.mDisableSnapshots;
    }
}
