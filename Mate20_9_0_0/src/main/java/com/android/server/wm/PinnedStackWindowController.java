package com.android.server.wm;

import android.app.RemoteAction;
import android.graphics.Rect;
import android.os.Handler;
import android.util.HwPCUtils;
import java.util.List;

public class PinnedStackWindowController extends StackWindowController {
    private static final String TAG = "PinnedStackWindowController";
    private Rect mTmpFromBounds = new Rect();
    private Rect mTmpToBounds = new Rect();

    public PinnedStackWindowController(int stackId, PinnedStackWindowListener listener, int displayId, boolean onTop, Rect outBounds, WindowManagerService service) {
        super(stackId, listener, displayId, onTop, outBounds, service);
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:16:0x002a=Splitter:B:16:0x002a, B:36:0x005a=Splitter:B:36:0x005a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Rect getPictureInPictureBounds(float aspectRatio, Rect stackBounds) {
        Rect rect;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                rect = null;
                if (this.mService.mSupportsPictureInPicture) {
                    if (this.mContainer != null) {
                        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                            HwPCUtils.log(TAG, "ignore getPictureInPictureBounds in pad pc mode");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                        DisplayContent displayContent = ((TaskStack) this.mContainer).getDisplayContent();
                        if (displayContent == null) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                        rect = displayContent.getPinnedStackController();
                        if (stackBounds == null) {
                            stackBounds = rect.getDefaultOrLastSavedBounds();
                        }
                        if (rect.isValidPictureInPictureAspectRatio(aspectRatio)) {
                            Rect transformBoundsToAspectRatio = rect.transformBoundsToAspectRatio(stackBounds, aspectRatio, true);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return transformBoundsToAspectRatio;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return stackBounds;
                    }
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return rect;
    }

    public void animateResizePinnedStack(Rect toBounds, Rect sourceHintBounds, int animationDuration, boolean fromFullscreen) {
        Throwable th;
        Rect rect;
        Rect rect2;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    Rect toBounds2;
                    int schedulePipModeChangedState;
                    if (HwPCUtils.enabledInPad()) {
                        try {
                            if (HwPCUtils.isPcCastModeInServer()) {
                                HwPCUtils.log(TAG, "ignore animateResizePinnedStack in pad pc mode");
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            rect = toBounds;
                            rect2 = sourceHintBounds;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    Rect fromBounds = new Rect();
                    ((TaskStack) this.mContainer).getBounds(fromBounds);
                    int schedulePipModeChangedState2 = 0;
                    boolean toFullscreen = toBounds == null;
                    if (!toFullscreen) {
                        if (fromFullscreen) {
                            schedulePipModeChangedState2 = 2;
                        }
                        toBounds2 = toBounds;
                        schedulePipModeChangedState = schedulePipModeChangedState2;
                    } else if (fromFullscreen) {
                        throw new IllegalArgumentException("Should not defer scheduling PiP mode change on animation to fullscreen.");
                    } else {
                        Rect toBounds3;
                        this.mService.getStackBounds(1, 1, this.mTmpToBounds);
                        if (this.mTmpToBounds.isEmpty()) {
                            toBounds3 = new Rect();
                            try {
                                ((TaskStack) this.mContainer).getDisplayContent().getBounds(toBounds3);
                            } catch (Throwable th4) {
                                th = th4;
                                rect2 = sourceHintBounds;
                                rect = toBounds3;
                            }
                        } else {
                            toBounds3 = new Rect(this.mTmpToBounds);
                        }
                        schedulePipModeChangedState = 1;
                        toBounds2 = toBounds3;
                    }
                    try {
                        try {
                            ((TaskStack) this.mContainer).setAnimationFinalBounds(sourceHintBounds, toBounds2, toFullscreen);
                            Rect finalToBounds = toBounds2;
                            int finalSchedulePipModeChangedState = schedulePipModeChangedState;
                            -$$Lambda$PinnedStackWindowController$x7R9b-0MaS9BJmen-irckXpBNyg -__lambda_pinnedstackwindowcontroller_x7r9b-0mas9bjmen-irckxpbnyg = r1;
                            Handler handler = this.mService.mBoundsAnimationController.getHandler();
                            -$$Lambda$PinnedStackWindowController$x7R9b-0MaS9BJmen-irckXpBNyg -__lambda_pinnedstackwindowcontroller_x7r9b-0mas9bjmen-irckxpbnyg2 = new -$$Lambda$PinnedStackWindowController$x7R9b-0MaS9BJmen-irckXpBNyg(this, fromBounds, finalToBounds, animationDuration, finalSchedulePipModeChangedState, fromFullscreen, toFullscreen);
                            handler.post(-__lambda_pinnedstackwindowcontroller_x7r9b-0mas9bjmen-irckxpbnyg);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th5) {
                            th = th5;
                            while (true) {
                                break;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        rect2 = sourceHintBounds;
                        while (true) {
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                rect2 = sourceHintBounds;
                try {
                    throw new IllegalArgumentException("Pinned stack container not found :(");
                } catch (Throwable th7) {
                    th = th7;
                    while (true) {
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th8) {
                th = th8;
                rect2 = sourceHintBounds;
                while (true) {
                    break;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public static /* synthetic */ void lambda$animateResizePinnedStack$0(PinnedStackWindowController pinnedStackWindowController, Rect fromBounds, Rect finalToBounds, int animationDuration, int finalSchedulePipModeChangedState, boolean fromFullscreen, boolean toFullscreen) {
        if (pinnedStackWindowController.mContainer != null) {
            pinnedStackWindowController.mService.mBoundsAnimationController.animateBounds((BoundsAnimationTarget) pinnedStackWindowController.mContainer, fromBounds, finalToBounds, animationDuration, finalSchedulePipModeChangedState, fromFullscreen, toFullscreen);
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:16:0x0029=Splitter:B:16:0x0029, B:30:0x007a=Splitter:B:30:0x007a} */
    /* JADX WARNING: Missing block: B:28:0x0076, code skipped:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:29:0x0079, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPictureInPictureAspectRatio(float aspectRatio) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mSupportsPictureInPicture) {
                    if (this.mContainer != null) {
                        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                            HwPCUtils.log(TAG, "ignore setPictureInPictureActions in pad pc mode");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        PinnedStackController pinnedStackController = ((TaskStack) this.mContainer).getDisplayContent().getPinnedStackController();
                        if (Float.compare(aspectRatio, pinnedStackController.getAspectRatio()) != 0) {
                            float f;
                            ((TaskStack) this.mContainer).getAnimationOrCurrentBounds(this.mTmpFromBounds);
                            this.mTmpToBounds.set(this.mTmpFromBounds);
                            getPictureInPictureBounds(aspectRatio, this.mTmpToBounds);
                            if (!this.mTmpToBounds.equals(this.mTmpFromBounds)) {
                                animateResizePinnedStack(this.mTmpToBounds, null, -1, false);
                            }
                            if (pinnedStackController.isValidPictureInPictureAspectRatio(aspectRatio)) {
                                f = aspectRatio;
                            } else {
                                f = -1.0f;
                            }
                            pinnedStackController.setAspectRatio(f);
                        }
                    }
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:16:0x0029=Splitter:B:16:0x0029, B:21:0x003d=Splitter:B:21:0x003d} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPictureInPictureActions(List<RemoteAction> actions) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mSupportsPictureInPicture) {
                    if (this.mContainer != null) {
                        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
                            HwPCUtils.log(TAG, "ignore getPictureInPictureActions in pad pc mode");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        ((TaskStack) this.mContainer).getDisplayContent().getPinnedStackController().setActions(actions);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean deferScheduleMultiWindowModeChanged() {
        boolean deferScheduleMultiWindowModeChanged;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                deferScheduleMultiWindowModeChanged = ((TaskStack) this.mContainer).deferScheduleMultiWindowModeChanged();
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return deferScheduleMultiWindowModeChanged;
    }

    public boolean isAnimatingBoundsToFullscreen() {
        boolean isAnimatingBoundsToFullscreen;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                isAnimatingBoundsToFullscreen = ((TaskStack) this.mContainer).isAnimatingBoundsToFullscreen();
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return isAnimatingBoundsToFullscreen;
    }

    public boolean pinnedStackResizeDisallowed() {
        boolean pinnedStackResizeDisallowed;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pinnedStackResizeDisallowed = ((TaskStack) this.mContainer).pinnedStackResizeDisallowed();
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return pinnedStackResizeDisallowed;
    }

    public void updatePictureInPictureModeForPinnedStackAnimation(Rect targetStackBounds, boolean forceUpdate) {
        if (this.mListener != null) {
            this.mListener.updatePictureInPictureModeForPinnedStackAnimation(targetStackBounds, forceUpdate);
        }
    }
}
