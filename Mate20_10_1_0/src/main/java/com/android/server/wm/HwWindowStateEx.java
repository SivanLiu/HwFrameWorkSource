package com.android.server.wm;

import android.freeform.HwFreeFormUtils;
import android.graphics.Rect;
import android.util.HwMwUtils;
import android.view.SurfaceControl;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;

public final class HwWindowStateEx implements IHwWindowStateEx {
    private static final String PERMISSION_CONTROLLER_PACKAGE = "com.android.permissioncontroller";
    private static final float SCALE_MODULUS = 0.5f;
    private static final String TAG = "HwWindowStateEx";
    private final Rect mDimBoundsRect = new Rect();
    final WindowManagerService mService;
    final WindowState mWinState;

    public HwWindowStateEx(WindowManagerService service, WindowState windowState) {
        this.mService = service;
        this.mWinState = windowState;
    }

    public Rect adjustImePosForFreeform(Rect contentFrame, Rect containingFrame) {
        if (!HwFreeFormUtils.isFreeFormEnable() || contentFrame == null || containingFrame == null) {
            return containingFrame;
        }
        int bottomDiff = contentFrame.bottom - containingFrame.bottom;
        int topDiff = contentFrame.top - containingFrame.top;
        int offsetY = bottomDiff > topDiff ? bottomDiff : topDiff;
        Rect taskBounds = new Rect();
        if (offsetY < 0) {
            this.mWinState.getTask().getBounds(taskBounds);
            taskBounds.offset(0, offsetY);
            this.mWinState.getTask().setBounds(taskBounds);
            containingFrame.offset(0, offsetY);
        }
        return containingFrame;
    }

    public boolean isInHwFreeFormWorkspace() {
        if (!HwFreeFormUtils.isFreeFormEnable()) {
            return false;
        }
        return this.mWinState.inFreeformWindowingMode();
    }

    public boolean isInHideCaptionList() {
        if (!isInHwFreeFormWorkspace() || this.mWinState.getDisplayContent().getConfiguration().orientation == 2) {
            return false;
        }
        String windowTitle = this.mWinState.toString();
        for (String str : HwFreeFormUtils.sHideCaptionActivity) {
            if (windowTitle.contains(str)) {
                return true;
            }
        }
        return false;
    }

    public int adjustTopForFreeform(Rect frame, Rect limitFrame, int minVisibleHeight) {
        if (frame == null || limitFrame == null) {
            return 0;
        }
        int top = frame.top > limitFrame.bottom - minVisibleHeight ? limitFrame.bottom - minVisibleHeight : frame.top;
        if (!isInHwFreeFormWorkspace() || isInHideCaptionList()) {
            return limitFrame.top > top ? limitFrame.top : top;
        }
        return top;
    }

    public void createMagicWindowDimmer() {
        if (HwMwUtils.ENABLED && this.mWinState.mAppToken != null && this.mWinState.getParent() != null && !PERMISSION_CONTROLLER_PACKAGE.equals(this.mWinState.getAttrs().packageName)) {
            if (!HwMwUtils.performPolicy((int) HwAPPQoEUtils.MSG_APP_STATE_UNKNOW, new Object[]{this.mWinState.mAppToken.mActivityRecord}).getBoolean("ACTIVITY_FULLSCREEN", false)) {
                WindowState windowState = this.mWinState;
                windowState.mMWDimmer = new HwMagicWindowDimmer(windowState);
            }
        }
    }

    public void destoryMagicWindowDimmer() {
        if (HwMwUtils.ENABLED && this.mWinState.mMWDimmer != null && (this.mWinState.mMWDimmer instanceof HwMagicWindowDimmer)) {
            this.mWinState.mMWDimmer.destroyDimmer(this.mWinState.getPendingTransaction());
        }
    }

    public boolean updateMagicWindowDimmer() {
        Dimmer dimmer = this.mWinState.mMWDimmer;
        Dimmer taskDimmer = this.mWinState.getDimmer();
        if (dimmer == null || taskDimmer == null) {
            return false;
        }
        boolean isDimming = false;
        dimmer.resetDimStates();
        if ((this.mWinState.mAttrs.flags & 2) != 0 && this.mWinState.isVisibleNow() && !this.mWinState.mHidden) {
            isDimming = true;
            SurfaceControl.Transaction pendingTransaction = this.mWinState.getPendingTransaction();
            WindowState windowState = this.mWinState;
            dimmer.dimBelow(pendingTransaction, windowState, windowState.mAttrs.dimAmount);
        }
        this.mDimBoundsRect.set(this.mWinState.getWindowFrames().mDisplayFrame);
        Rect rect = this.mDimBoundsRect;
        rect.right = rect.left + ((int) ((((float) this.mDimBoundsRect.width()) * this.mWinState.mMwUsedScaleFactor * this.mWinState.mGlobalScale) + 0.5f));
        Rect rect2 = this.mDimBoundsRect;
        rect2.bottom = rect2.top + ((int) ((((float) this.mDimBoundsRect.height()) * this.mWinState.mMwUsedScaleFactor * this.mWinState.mGlobalScale) + 0.5f));
        this.mDimBoundsRect.offsetTo(0, 0);
        if (dimmer.updateDims(this.mWinState.getPendingTransaction(), this.mDimBoundsRect)) {
            this.mWinState.scheduleAnimation();
        }
        return isDimming;
    }

    public void stopMagicWindowDimmer() {
        if (HwMwUtils.ENABLED && this.mWinState.mMWDimmer != null) {
            this.mWinState.mMWDimmer.stopDim(this.mWinState.getPendingTransaction());
        }
    }

    public boolean isNeedMoveAnimation(WindowState windowState) {
        if (windowState == null || windowState.mAppToken == null || !windowState.inHwMagicWindowingMode()) {
            return true;
        }
        return HwMwUtils.performPolicy((int) HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, new Object[]{windowState.mAppToken.appPackageName}).getBoolean("RESULT_NEED_SYSTEM_ANIMATION", true);
    }
}
