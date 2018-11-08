package com.android.server.wm;

import android.app.ActivityManager.StackId;
import android.graphics.Rect;
import android.util.Slog;
import com.android.server.power.IHwShutdownThread;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.util.ArrayDeque;
import java.util.function.Consumer;

public class WindowLayersController {
    private boolean mAboveImeTarget;
    private ArrayDeque<WindowState> mAboveImeTargetAppWindows = new ArrayDeque();
    private boolean mAnyLayerChanged;
    private final Consumer<WindowState> mAssignWindowLayersConsumer = new -$Lambda$YIZfR4m-B8z_tYbP2x4OJ3o7OYE(UsbDescriptor.CLASSID_TYPECBRIDGE, this);
    private ArrayDeque<WindowState> mAssistantWindows = new ArrayDeque();
    private int mCurBaseLayer;
    private int mCurLayer;
    private WindowState mDockDivider = null;
    private ArrayDeque<WindowState> mDockedWindows = new ArrayDeque();
    private LayerChangeObserver mFingerLayerObserver;
    private int mHighestApplicationLayer;
    private int mHighestDockedAffectedLayer;
    private int mHighestLayerInImeTargetBaseLayer;
    private WindowState mImeTarget;
    private ArrayDeque<WindowState> mInputMethodWindows = new ArrayDeque();
    private ArrayDeque<WindowState> mPinnedWindows = new ArrayDeque();
    private ArrayDeque<WindowState> mReplacingWindows = new ArrayDeque();
    private final WindowManagerService mService;

    class LayerChangeObserver {
        WindowState mObserveWin = null;
        String mObserveWinTitle;
        WindowState mTopAppSysAlertWin = null;
        boolean mWinEverCovered = false;

        LayerChangeObserver(String observeWinTitle) {
            this.mObserveWinTitle = observeWinTitle;
        }

        void reset() {
            this.mObserveWin = null;
            this.mTopAppSysAlertWin = null;
        }

        void checkWinLayer(WindowState win) {
            if (win.toString().contains(this.mObserveWinTitle)) {
                this.mObserveWin = win;
            } else if (!win.isAppVisibleSysAlertWin()) {
            } else {
                if (this.mTopAppSysAlertWin == null || this.mTopAppSysAlertWin.isLayerLowerThan(win)) {
                    this.mTopAppSysAlertWin = win;
                }
            }
        }

        void checkNeedNotify() {
            if (this.mObserveWin != null && this.mObserveWin.isVisibleOrAdding()) {
                boolean fingerWinCovered;
                boolean needNotify;
                if (this.mObserveWin.isLayerLowerThan(this.mTopAppSysAlertWin)) {
                    Slog.v("WindowManager", "(" + this.mObserveWin.mAttrs.getTitle() + ", layer=" + this.mObserveWin.mWinAnimator.mAnimLayer + ") is covered , notify FingerprintService to handle it.");
                    fingerWinCovered = true;
                    this.mWinEverCovered = true;
                    needNotify = true;
                } else {
                    fingerWinCovered = false;
                    needNotify = this.mWinEverCovered;
                    this.mWinEverCovered = false;
                    if (needNotify) {
                        Slog.v("WindowManager", "(" + this.mObserveWin.mAttrs.getTitle() + ") no longer being covered by other app window, " + "notify FingerprintService to handle it.needNotify=" + needNotify);
                    }
                }
                if (needNotify) {
                    Rect winFrame = new Rect();
                    if (this.mTopAppSysAlertWin != null) {
                        winFrame = this.mTopAppSysAlertWin.getVisibleFrameLw();
                    }
                    WindowLayersController.this.mService.notifyFingerWinCovered(fingerWinCovered, winFrame);
                }
            }
        }
    }

    WindowLayersController(WindowManagerService service) {
        this.mService = service;
        this.mFingerLayerObserver = new LayerChangeObserver("hw_ud_fingerprint");
    }

    /* synthetic */ void lambda$-com_android_server_wm_WindowLayersController_4495(WindowState w) {
        boolean layerChanged = false;
        int oldLayer = w.mLayer;
        if (w.mBaseLayer == this.mCurBaseLayer) {
            this.mCurLayer += 5;
        } else {
            int i = w.mBaseLayer;
            this.mCurLayer = i;
            this.mCurBaseLayer = i;
        }
        assignAnimLayer(w, this.mCurLayer);
        if (!(w.mLayer == oldLayer && w.mWinAnimator.mAnimLayer == oldLayer)) {
            layerChanged = true;
            this.mAnyLayerChanged = true;
        }
        if (w.mAppToken != null) {
            this.mHighestApplicationLayer = Math.max(this.mHighestApplicationLayer, w.mWinAnimator.mAnimLayer);
        }
        if (this.mImeTarget != null && w.mBaseLayer == this.mImeTarget.mBaseLayer) {
            this.mHighestLayerInImeTargetBaseLayer = Math.max(this.mHighestLayerInImeTargetBaseLayer, w.mWinAnimator.mAnimLayer);
        }
        if (w.getAppToken() != null && StackId.isResizeableByDockedStack(w.getStackId())) {
            this.mHighestDockedAffectedLayer = Math.max(this.mHighestDockedAffectedLayer, w.mWinAnimator.mAnimLayer);
        }
        collectSpecialWindows(w);
        if (layerChanged) {
            w.scheduleAnimationIfDimming();
        }
        this.mFingerLayerObserver.checkWinLayer(w);
    }

    final void assignWindowLayers(DisplayContent dc) {
        reset();
        dc.forAllWindows(this.mAssignWindowLayersConsumer, false);
        adjustSpecialWindows();
        if (this.mService.mAccessibilityController != null && this.mAnyLayerChanged && dc.getDisplayId() == 0) {
            this.mService.mAccessibilityController.onWindowLayersChangedLocked();
        }
        this.mFingerLayerObserver.checkNeedNotify();
    }

    static /* synthetic */ void lambda$-com_android_server_wm_WindowLayersController_7016(WindowState w) {
        Slog.v("WindowManager", "Assign layer " + w + ": " + "mBase=" + w.mBaseLayer + " mLayer=" + w.mLayer + (w.mAppToken == null ? "" : " mAppLayer=" + w.mAppToken.getAnimLayerAdjustment()) + " =mAnimLayer=" + w.mWinAnimator.mAnimLayer);
    }

    private void logDebugLayers(DisplayContent dc) {
        dc.forAllWindows((Consumer) -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0.$INST$5, false);
    }

    private void reset() {
        this.mPinnedWindows.clear();
        this.mInputMethodWindows.clear();
        this.mDockedWindows.clear();
        this.mAssistantWindows.clear();
        this.mReplacingWindows.clear();
        this.mDockDivider = null;
        this.mCurBaseLayer = 0;
        this.mCurLayer = 0;
        this.mAnyLayerChanged = false;
        this.mHighestApplicationLayer = 0;
        this.mHighestDockedAffectedLayer = 0;
        this.mHighestLayerInImeTargetBaseLayer = this.mImeTarget != null ? this.mImeTarget.mBaseLayer : 0;
        this.mImeTarget = this.mService.mInputMethodTarget;
        this.mAboveImeTarget = false;
        this.mAboveImeTargetAppWindows.clear();
        this.mFingerLayerObserver.reset();
    }

    private void collectSpecialWindows(WindowState w) {
        if (w.mAttrs.type == 2034) {
            this.mDockDivider = w;
            return;
        }
        if (w.mWillReplaceWindow) {
            this.mReplacingWindows.add(w);
        }
        if (w.mIsImWindow) {
            this.mInputMethodWindows.add(w);
            return;
        }
        if (this.mImeTarget != null) {
            if (w.getParentWindow() == this.mImeTarget && w.mSubLayer > 0) {
                this.mAboveImeTargetAppWindows.add(w);
            } else if (this.mAboveImeTarget && w.mAppToken != null) {
                this.mAboveImeTargetAppWindows.add(w);
            }
            if (w == this.mImeTarget) {
                this.mAboveImeTarget = true;
            }
        }
        int stackId = w.getAppToken() != null ? w.getStackId() : -1;
        if (stackId == 4) {
            this.mPinnedWindows.add(w);
        } else if (stackId == 3) {
            this.mDockedWindows.add(w);
        } else if (stackId == 6) {
            this.mAssistantWindows.add(w);
        }
    }

    private void adjustSpecialWindows() {
        int layer = this.mHighestDockedAffectedLayer + 1000;
        if (!this.mDockedWindows.isEmpty() && this.mHighestDockedAffectedLayer > 0) {
            while (!this.mDockedWindows.isEmpty()) {
                layer = assignAndIncreaseLayerIfNeeded((WindowState) this.mDockedWindows.remove(), layer);
            }
            layer = assignAndIncreaseLayerIfNeeded(this.mDockDivider, layer);
            while (!this.mAssistantWindows.isEmpty()) {
                WindowState window = (WindowState) this.mAssistantWindows.remove();
                if (window.mLayer > this.mHighestDockedAffectedLayer) {
                    layer = assignAndIncreaseLayerIfNeeded(window, layer);
                }
            }
        }
        layer = Math.max(layer, this.mHighestApplicationLayer + 5);
        while (!this.mReplacingWindows.isEmpty()) {
            layer = assignAndIncreaseLayerIfNeeded((WindowState) this.mReplacingWindows.remove(), layer);
        }
        while (!this.mPinnedWindows.isEmpty()) {
            layer = assignAndIncreaseLayerIfNeeded((WindowState) this.mPinnedWindows.remove(), layer);
        }
        if (this.mImeTarget != null) {
            if (this.mImeTarget.mAppToken == null) {
                layer = this.mHighestLayerInImeTargetBaseLayer + 5;
            }
            while (!this.mInputMethodWindows.isEmpty()) {
                layer = assignAndIncreaseLayerIfNeeded((WindowState) this.mInputMethodWindows.remove(), layer);
            }
            while (!this.mAboveImeTargetAppWindows.isEmpty()) {
                layer = assignAndIncreaseLayerIfNeeded((WindowState) this.mAboveImeTargetAppWindows.remove(), layer);
            }
        }
    }

    private int assignAndIncreaseLayerIfNeeded(WindowState win, int layer) {
        if (win == null) {
            return layer;
        }
        int adjustLayer = (win.getAttrs().type != 2034 || (win.getAttrs().flags & 536870912) == 0) ? layer : layer + 1;
        assignAnimLayer(win, adjustLayer);
        return layer + 5;
    }

    private void assignAnimLayer(WindowState w, int layer) {
        w.mLayer = layer;
        w.mWinAnimator.mAnimLayer = w.getAnimLayerAdjustment() + w.getSpecialWindowAnimLayerAdjustment();
        if (w.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && (this.mService.isCoverOpen() ^ 1) != 0) {
            w.mWinAnimator.mAnimLayer = AbsWindowManagerService.TOP_LAYER;
        }
        if (w.mAppToken != null && w.mAppToken.mAppAnimator.thumbnailForceAboveLayer > 0) {
            if (w.mWinAnimator.mAnimLayer > w.mAppToken.mAppAnimator.thumbnailForceAboveLayer) {
                w.mAppToken.mAppAnimator.thumbnailForceAboveLayer = w.mWinAnimator.mAnimLayer;
            }
            int highestLayer = w.mAppToken.getHighestAnimLayer();
            if (highestLayer > 0 && w.mAppToken.mAppAnimator.thumbnail != null && w.mAppToken.mAppAnimator.thumbnailForceAboveLayer != highestLayer) {
                w.mAppToken.mAppAnimator.thumbnailForceAboveLayer = highestLayer;
                w.mAppToken.mAppAnimator.thumbnail.setLayer(highestLayer + 1);
            }
        }
    }
}
