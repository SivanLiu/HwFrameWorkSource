package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.InputDevice;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.util.ArrayList;
import java.util.List;

public class HwDisplayContent extends DisplayContent {
    private static final int MRX_KEYBOARD_PID = 4253;
    private static final int MRX_KEYBOARD_VID = 4817;
    private static DisplayRoundCorner sDisplayRoundCorner = null;
    private boolean mHasLighterViewInPCCastMode;
    private boolean mLastMagicWinStackVisibility = false;
    private boolean mTmpshouldDropMotionEventForTouchPad;

    @FunctionalInterface
    private interface ScreenshoterForExternalDisplay<E> {
        E screenshotForExternalDisplay(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, int i5);
    }

    public HwDisplayContent(Display display, WindowManagerService service, ActivityDisplay activityDisplay) {
        super(display, service, activityDisplay);
    }

    /* access modifiers changed from: package-private */
    public void computeScreenConfiguration(Configuration config) {
        HwDisplayContent.super.computeScreenConfiguration(config);
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            InputDevice[] devices = this.mWmService.mInputManager.getInputDevices();
            int len = devices != null ? devices.length : 0;
            for (int i = 0; i < len; i++) {
                InputDevice device = devices[i];
                if (device.getProductId() == MRX_KEYBOARD_PID && device.getVendorId() == MRX_KEYBOARD_VID) {
                    config.keyboard = 2;
                    config.hardKeyboardHidden = 1;
                    return;
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateBaseDisplayMetrics(int baseWidth, int baseHeight, int baseDensity) {
        HwDisplayContent.super.updateBaseDisplayMetrics(baseWidth, baseHeight, baseDensity);
        if (DisplayRoundCorner.isRoundCornerDisplay() || HwDisplaySizeUtil.hasSideInScreen()) {
            sDisplayRoundCorner = DisplayRoundCorner.getInstance(this.mWmService);
            sDisplayRoundCorner.setScreenSize(baseWidth, baseHeight);
            Slog.d("WindowManager", "updateBaseDisplayMetrics " + sDisplayRoundCorner);
        }
    }

    /* access modifiers changed from: package-private */
    public Rect getSafeInsetsByType(int type) {
        DisplayRoundCorner displayRoundCorner = sDisplayRoundCorner;
        if (displayRoundCorner == null) {
            Slog.v("WindowManager", "sDisplayRoundCorner is null");
            return null;
        } else if (type == 1) {
            return displayRoundCorner.getRoundCornerSafeInsets(getRotation());
        } else {
            if (type != 2) {
                return null;
            }
            return displayRoundCorner.getSideDisplaySafeInsets(getRotation());
        }
    }

    /* access modifiers changed from: package-private */
    public List<Rect> getBoundsByType(int type) {
        DisplayRoundCorner displayRoundCorner = sDisplayRoundCorner;
        if (displayRoundCorner == null) {
            Slog.v("WindowManager", "sDisplayRoundCorner is null");
            return null;
        } else if (type == 1) {
            return displayRoundCorner.getRoundCornerUnsafeBounds(getRotation());
        } else {
            if (type != 2) {
                return null;
            }
            return displayRoundCorner.getSideDisplayUnsafeBounds(getRotation());
        }
    }

    /* access modifiers changed from: package-private */
    public List taskIdFromTop() {
        int taskId;
        List<Integer> tasks = new ArrayList<>();
        for (int stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = this.mTaskStackContainers.getChildAt(stackNdx);
            int taskNdx = stack.mChildren.size() - 1;
            while (taskNdx >= 0) {
                Task task = (Task) stack.mChildren.get(taskNdx);
                if (task.getTopVisibleAppMainWindow() == null || (taskId = task.mTaskId) == -1) {
                    taskNdx--;
                } else {
                    tasks.add(Integer.valueOf(taskId));
                    return tasks;
                }
            }
        }
        return tasks;
    }

    public void setDisplayRotationFR(int rotation) {
        IntelliServiceManager.setDisplayRotation(rotation);
    }

    public void togglePCMode(boolean pcMode) {
        if (!pcMode && HwPCUtils.isValidExtDisplayId(this.mDisplay.getDisplayId())) {
            try {
                WindowToken topChild = this.mAboveAppWindowsContainers.getTopChild();
                while (topChild != null) {
                    topChild.removeImmediately();
                    topChild = (WindowToken) this.mAboveAppWindowsContainers.getTopChild();
                }
            } catch (Exception e) {
                HwPCUtils.log("PCManager", "togglePCMode failed!!!");
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean updateRotationUnchecked(boolean forceUpdate) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
                return false;
            }
            if (HwPCUtils.enabledInPad() && getRotation() == 1) {
                return false;
            }
        }
        return HwDisplayContent.super.updateRotationUnchecked(forceUpdate);
    }

    /* access modifiers changed from: package-private */
    public void performLayout(boolean initial, boolean updateInputWindows) {
        HwDisplayContent.super.performLayout(initial, updateInputWindows);
        if (HwMwUtils.ENABLED) {
            boolean isMagicWindowStackVisible = false;
            TaskStack topStack = getTopStack();
            if (topStack == null) {
                return;
            }
            if (topStack.isVisible() || topStack.inHwFreeFormWindowingMode()) {
                synchronized (this.mWmService.getGlobalLock()) {
                    if (isStackVisible(103)) {
                        isMagicWindowStackVisible = true;
                    }
                }
                if (this.mLastMagicWinStackVisibility != isMagicWindowStackVisible) {
                    this.mLastMagicWinStackVisibility = isMagicWindowStackVisible;
                    HwMwUtils.performPolicy(8, new Object[]{Boolean.valueOf(isMagicWindowStackVisible)});
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void uploadOrientation(int rotation) {
        String rotationState;
        if (this.mWmService.mContext != null) {
            if (rotation == 1 || rotation == 3) {
                rotationState = "is horizontal screen";
            } else {
                rotationState = "is vertical screen";
            }
            StatisticalUtils.reporte(this.mWmService.mContext, (int) HwSecDiagnoseConstant.OEMINFO_ID_ROOT_CHECK, "{ " + rotationState + " rotation:" + rotation + " }");
        }
    }

    public boolean shouldDropMotionEventForTouchPad(float x, float y) {
        this.mTmpshouldDropMotionEventForTouchPad = false;
        forAllWindows(new ToBooleanFunction(x, y) {
            /* class com.android.server.wm.$$Lambda$HwDisplayContent$qion8MzS_F8S6YfztCtcQoD0s7c */
            private final /* synthetic */ float f$1;
            private final /* synthetic */ float f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final boolean apply(Object obj) {
                return HwDisplayContent.this.lambda$shouldDropMotionEventForTouchPad$0$HwDisplayContent(this.f$1, this.f$2, (WindowState) obj);
            }
        }, true);
        return this.mTmpshouldDropMotionEventForTouchPad;
    }

    public /* synthetic */ boolean lambda$shouldDropMotionEventForTouchPad$0$HwDisplayContent(float x, float y, WindowState w) {
        String title = w.getAttrs().getTitle() == null ? null : w.getAttrs().getTitle().toString();
        if ("com.huawei.desktop.systemui/com.huawei.systemui.mk.activity.ImitateActivity".equalsIgnoreCase(title)) {
            this.mTmpshouldDropMotionEventForTouchPad = false;
            return true;
        }
        if (w.isVisible() && !"Emui:A11WaterMarkWnd".equalsIgnoreCase(title)) {
            Region outRegion = new Region();
            w.getTouchableRegion(outRegion);
            if (outRegion.contains((int) x, (int) y)) {
                Slog.d("WindowManager", "consume event in title = " + title);
                this.mTmpshouldDropMotionEventForTouchPad = true;
                return true;
            }
        }
        this.mTmpshouldDropMotionEventForTouchPad = false;
        return false;
    }

    public boolean hasLighterViewInPCCastMode() {
        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
            Slog.d("WindowManager", "hasLighterViewInPCCastMode not in PC cast mode");
            return false;
        }
        this.mHasLighterViewInPCCastMode = false;
        forAllWindows(new ToBooleanFunction() {
            /* class com.android.server.wm.$$Lambda$HwDisplayContent$eaQqFnYl2IDHRhVZSJbtBzLAoco */

            public final boolean apply(Object obj) {
                return HwDisplayContent.this.lambda$hasLighterViewInPCCastMode$1$HwDisplayContent((WindowState) obj);
            }
        }, true);
        return this.mHasLighterViewInPCCastMode;
    }

    public /* synthetic */ boolean lambda$hasLighterViewInPCCastMode$1$HwDisplayContent(WindowState w) {
        if (!"com.huawei.systemui.mk.lighterdrawer.LighterDrawView".equalsIgnoreCase(w.getAttrs().getTitle() == null ? null : w.getAttrs().getTitle().toString()) || !w.isVisible()) {
            this.mHasLighterViewInPCCastMode = false;
            return false;
        }
        this.mHasLighterViewInPCCastMode = true;
        return true;
    }
}
