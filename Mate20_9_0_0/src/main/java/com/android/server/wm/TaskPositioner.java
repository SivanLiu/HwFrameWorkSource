package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.Display;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class TaskPositioner {
    private static final int CTRL_BOTTOM = 8;
    private static final int CTRL_LEFT = 1;
    private static final int CTRL_NONE = 0;
    private static final int CTRL_RIGHT = 2;
    private static final int CTRL_TOP = 4;
    private static final boolean DEBUG_ORIENTATION_VIOLATIONS = false;
    private static final int DISPOSE_MSG = 0;
    @VisibleForTesting
    static final float MIN_ASPECT = 1.2f;
    public static final float RESIZING_HINT_ALPHA = 0.5f;
    public static final int RESIZING_HINT_DURATION_MS = 0;
    static final int SIDE_MARGIN_DIP = 100;
    private static final String TAG = "WindowManager";
    private static final String TAG_LOCAL = "TaskPositioner";
    private static Factory sFactory;
    Handler PCHandler = null;
    InputChannel mClientChannel;
    private int mCtrlType = 0;
    private Display mDisplay;
    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    InputApplicationHandle mDragApplicationHandle;
    private boolean mDragEnded = false;
    InputWindowHandle mDragWindowHandle;
    private WindowPositionerEventReceiver mInputEventReceiver;
    private final Point mMaxVisibleSize = new Point();
    private int mMinVisibleHeight;
    private int mMinVisibleWidth;
    private boolean mPreserveOrientation;
    private boolean mResizing;
    InputChannel mServerChannel;
    private final WindowManagerService mService;
    private int mSideMargin;
    private float mStartDragX;
    private float mStartDragY;
    private boolean mStartOrientationWasLandscape;
    private Task mTask;
    private Rect mTmpRect = new Rect();
    private final Rect mWindowDragBounds = new Rect();
    private final Rect mWindowOriginalBounds = new Rect();

    @Retention(RetentionPolicy.SOURCE)
    @interface CtrlType {
    }

    interface Factory {
        TaskPositioner create(WindowManagerService service) {
            return new TaskPositioner(service);
        }
    }

    private final class WindowPositionerEventReceiver extends BatchedInputEventReceiver {
        public WindowPositionerEventReceiver(InputChannel inputChannel, Looper looper, Choreographer choreographer) {
            super(inputChannel, looper, choreographer);
        }

        /* JADX WARNING: Removed duplicated region for block: B:37:0x00af A:{Catch:{ all -> 0x0116, all -> 0x009a, Exception -> 0x0120 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onInputEvent(InputEvent event, int displayId) {
            if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                MotionEvent motionEvent = (MotionEvent) event;
                boolean handled = false;
                if (TaskPositioner.this.mDragEnded) {
                    finishInputEvent(event, true);
                    return;
                }
                float newX = motionEvent.getRawX();
                float newY = motionEvent.getRawY();
                switch (motionEvent.getAction()) {
                    case 1:
                        TaskPositioner.this.mDragEnded = true;
                    case 2:
                        synchronized (TaskPositioner.this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                TaskPositioner.this.mDragEnded = TaskPositioner.this.notifyMoveLocked(newX, newY);
                                TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                            } catch (Exception e) {
                                try {
                                    Slog.e(TaskPositioner.TAG, "Exception caught by drag handleMotion", e);
                                    break;
                                } catch (Throwable th) {
                                    finishInputEvent(event, false);
                                }
                            } catch (Throwable th2) {
                                while (true) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    break;
                                }
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (!TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                            Trace.traceBegin(32, "wm.TaskPositioner.resizeTask");
                            try {
                                TaskPositioner.this.mService.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 1);
                            } catch (RemoteException e2) {
                            }
                            Trace.traceEnd(32);
                        }
                    case 3:
                        TaskPositioner.this.mDragEnded = true;
                        if (TaskPositioner.this.mDragEnded) {
                            boolean wasResizing = TaskPositioner.this.mResizing;
                            synchronized (TaskPositioner.this.mService.mWindowMap) {
                                WindowManagerService.boostPriorityForLockedSection();
                                TaskPositioner.this.endDragLocked();
                                TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (wasResizing) {
                                try {
                                    if (!TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                                        TaskPositioner.this.mService.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 3);
                                    }
                                } catch (RemoteException e3) {
                                }
                            }
                            TaskPositioner.this.mService.mTaskPositioningController.finishTaskPositioning();
                        }
                        handled = true;
                        break;
                }
                if (TaskPositioner.this.mDragEnded) {
                }
                handled = true;
                finishInputEvent(event, handled);
            }
        }
    }

    TaskPositioner(WindowManagerService service) {
        this.mService = service;
    }

    @VisibleForTesting
    Rect getWindowDragBounds() {
        return this.mWindowDragBounds;
    }

    void register(DisplayContent displayContent) {
        Display display = displayContent.getDisplay();
        if (this.mClientChannel != null) {
            Slog.e(TAG, "Task positioner already registered");
            return;
        }
        this.mDisplay = display;
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        InputChannel[] channels = InputChannel.openInputChannelPair(TAG);
        this.mServerChannel = channels[0];
        this.mClientChannel = channels[1];
        this.mService.mInputManager.registerInputChannel(this.mServerChannel, null);
        this.mInputEventReceiver = new WindowPositionerEventReceiver(this.mClientChannel, this.mService.mAnimationHandler.getLooper(), this.mService.mAnimator.getChoreographer());
        this.mDragApplicationHandle = new InputApplicationHandle(null);
        this.mDragApplicationHandle.name = TAG;
        this.mDragApplicationHandle.dispatchingTimeoutNanos = 5000000000L;
        this.mDragWindowHandle = new InputWindowHandle(this.mDragApplicationHandle, null, null, this.mDisplay.getDisplayId());
        this.mDragWindowHandle.name = TAG;
        this.mDragWindowHandle.inputChannel = this.mServerChannel;
        this.mDragWindowHandle.layer = this.mService.getDragLayerLocked();
        this.mDragWindowHandle.layoutParamsFlags = 0;
        this.mDragWindowHandle.layoutParamsType = 2016;
        this.mDragWindowHandle.dispatchingTimeoutNanos = 5000000000L;
        this.mDragWindowHandle.visible = true;
        this.mDragWindowHandle.canReceiveKeys = false;
        this.mDragWindowHandle.hasFocus = true;
        this.mDragWindowHandle.hasWallpaper = false;
        this.mDragWindowHandle.paused = false;
        this.mDragWindowHandle.ownerPid = Process.myPid();
        this.mDragWindowHandle.ownerUid = Process.myUid();
        this.mDragWindowHandle.inputFeatures = 0;
        this.mDragWindowHandle.scaleFactor = 1.0f;
        this.mDragWindowHandle.touchableRegion.setEmpty();
        this.mDragWindowHandle.frameLeft = 0;
        this.mDragWindowHandle.frameTop = 0;
        Point p = new Point();
        this.mDisplay.getRealSize(p);
        this.mDragWindowHandle.frameRight = p.x;
        this.mDragWindowHandle.frameBottom = p.y;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d(TAG, "Pausing rotation during re-position");
        }
        this.mService.pauseRotationLocked();
        this.mSideMargin = WindowManagerService.dipToPixel(100, this.mDisplayMetrics);
        this.mMinVisibleWidth = WindowManagerService.dipToPixel(42, this.mDisplayMetrics);
        this.mMinVisibleHeight = WindowManagerService.dipToPixel(36, this.mDisplayMetrics);
        this.mDisplay.getRealSize(this.mMaxVisibleSize);
        this.mDragEnded = false;
    }

    void unregister() {
        if (this.mClientChannel == null) {
            Slog.e(TAG, "Task positioner not registered");
            return;
        }
        this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
        if (HwPCUtils.enabledInPad() && this.mDisplay != null && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplay.getDisplayId())) {
            this.PCHandler = new Handler(this.mService.mAnimationHandler.getLooper()) {
                public void handleMessage(Message msg) {
                    if (msg.what == 0) {
                        TaskPositioner.this.mInputEventReceiver.dispose();
                        TaskPositioner.this.mInputEventReceiver = null;
                    }
                }
            };
            this.PCHandler.removeMessages(0);
            this.PCHandler.sendEmptyMessage(0);
        } else {
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        this.mClientChannel.dispose();
        this.mServerChannel.dispose();
        this.mClientChannel = null;
        this.mServerChannel = null;
        this.mDragWindowHandle = null;
        this.mDragApplicationHandle = null;
        this.mDisplay = null;
        this.mDragEnded = true;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d(TAG, "Resuming rotation after re-position");
        }
        this.mService.resumeRotationLocked();
    }

    void startDrag(WindowState win, boolean resize, boolean preserveOrientation, float startX, float startY) {
        this.mTask = win.getTask();
        this.mTask.getDimBounds(this.mTmpRect);
        startDrag(resize, preserveOrientation, startX, startY, this.mTmpRect);
    }

    @VisibleForTesting
    void startDrag(boolean resize, boolean preserveOrientation, float startX, float startY, Rect startBounds) {
        boolean z = false;
        this.mCtrlType = 0;
        this.mStartDragX = startX;
        this.mStartDragY = startY;
        this.mPreserveOrientation = preserveOrientation;
        if (resize) {
            if (startX < ((float) startBounds.left)) {
                this.mCtrlType |= 1;
            }
            if (startX > ((float) startBounds.right)) {
                this.mCtrlType |= 2;
            }
            if (startY < ((float) startBounds.top)) {
                this.mCtrlType |= 4;
            }
            if (startY > ((float) startBounds.bottom)) {
                this.mCtrlType |= 8;
            }
            this.mResizing = this.mCtrlType != 0;
        }
        if (startBounds.width() >= startBounds.height()) {
            z = true;
        }
        this.mStartOrientationWasLandscape = z;
        this.mWindowOriginalBounds.set(startBounds);
        if (this.mResizing) {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    notifyMoveLocked(startX, startY);
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            this.mService.mH.post(new -$$Lambda$TaskPositioner$TE0EjYzJeOSFARmUlY6wF3y3c2U(this, startBounds));
        }
        this.mWindowDragBounds.set(startBounds);
    }

    public static /* synthetic */ void lambda$startDrag$0(TaskPositioner taskPositioner, Rect startBounds) {
        try {
            taskPositioner.mService.mActivityManager.resizeTask(taskPositioner.mTask.mTaskId, startBounds, 3);
        } catch (RemoteException e) {
        }
    }

    private void endDragLocked() {
        this.mResizing = false;
        this.mTask.setDragResizing(false, 0);
    }

    private boolean notifyMoveLocked(float x, float y) {
        if (this.mCtrlType != 0) {
            resizeDrag(x, y);
            this.mTask.setDragResizing(true, 0);
            return false;
        }
        this.mTask.mStack.getDimBounds(this.mTmpRect);
        if (HwPCUtils.isExtDynamicStack(this.mTask.mStack.mStackId)) {
            if (this.mService == null || this.mService.mHwWMSEx == null || this.mService.mHwWMSEx.getPCScreenDisplayMode() == 0 || this.mDisplay == null) {
                this.mTmpRect.bottom = this.mTask.mStack.getDisplayInfo().appHeight;
            } else {
                float screenScale = this.mService.mHwWMSEx.getPCScreenScale();
                Point displaySize = new Point();
                this.mDisplay.getRealSize(displaySize);
                float offsetY = (((float) displaySize.y) * (1.0f - screenScale)) / 2.0f;
                this.mTmpRect.bottom = (int) ((((float) this.mTask.mStack.getDisplayInfo().appHeight) * screenScale) + offsetY);
            }
        }
        int nX = (int) x;
        int nY = (int) y;
        if (!this.mTmpRect.contains(nX, nY)) {
            nX = Math.min(Math.max(nX, this.mTmpRect.left), this.mTmpRect.right);
            nY = Math.min(Math.max(nY, this.mTmpRect.top), this.mTmpRect.bottom);
        }
        updateWindowDragBounds(nX, nY, this.mTmpRect);
        return false;
    }

    @VisibleForTesting
    void resizeDrag(float x, float y) {
        float f;
        int width;
        int height;
        int deltaX = Math.round(x - this.mStartDragX);
        int deltaY = Math.round(y - this.mStartDragY);
        int left = this.mWindowOriginalBounds.left;
        int top = this.mWindowOriginalBounds.top;
        int right = this.mWindowOriginalBounds.right;
        int bottom = this.mWindowOriginalBounds.bottom;
        if (this.mPreserveOrientation) {
            f = this.mStartOrientationWasLandscape ? MIN_ASPECT : 0.8333333f;
        } else {
            f = 1.0f;
        }
        float minAspect = f;
        int width2 = right - left;
        int height2 = bottom - top;
        if ((this.mCtrlType & 1) != 0) {
            width2 = Math.max(this.mMinVisibleWidth, width2 - deltaX);
        } else if ((this.mCtrlType & 2) != 0) {
            width2 = Math.max(this.mMinVisibleWidth, width2 + deltaX);
        }
        if ((this.mCtrlType & 4) != 0) {
            height2 = Math.max(this.mMinVisibleHeight, height2 - deltaY);
        } else if ((this.mCtrlType & 8) != 0) {
            height2 = Math.max(this.mMinVisibleHeight, height2 + deltaY);
        }
        float aspect = ((float) width2) / ((float) height2);
        if (!this.mPreserveOrientation || ((!this.mStartOrientationWasLandscape || aspect >= MIN_ASPECT) && (this.mStartOrientationWasLandscape || ((double) aspect) <= 0.8333333002196431d))) {
            width = width2;
        } else {
            int height22;
            int width22;
            int width1;
            int height1;
            int width12;
            int height12;
            if (this.mStartOrientationWasLandscape) {
                width12 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, width2));
                height12 = Math.min(height2, Math.round(((float) width12) / MIN_ASPECT));
                if (height12 < this.mMinVisibleHeight) {
                    height12 = this.mMinVisibleHeight;
                    width12 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(((float) height12) * MIN_ASPECT)));
                } else {
                    int i = width12;
                }
                height22 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, height2));
                width22 = Math.max(width2, Math.round(((float) height22) * MIN_ASPECT));
                int height23 = height22;
                if (width22 < this.mMinVisibleWidth) {
                    width22 = this.mMinVisibleWidth;
                    int width13 = width12;
                    int height13 = height12;
                    height22 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(((float) width22) / MIN_ASPECT)));
                    width1 = width13;
                    height1 = height13;
                } else {
                    height22 = height23;
                    width1 = width12;
                    height1 = height12;
                }
            } else {
                height22 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, width2));
                width12 = Math.max(height2, Math.round(((float) height22) * MIN_ASPECT));
                if (width12 < this.mMinVisibleHeight) {
                    width12 = this.mMinVisibleHeight;
                    height12 = width12;
                    width12 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(((float) width12) / MIN_ASPECT)));
                } else {
                    height12 = width12;
                    width12 = height22;
                }
                height22 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, height2));
                width22 = Math.min(width2, Math.round(((float) height22) / MIN_ASPECT));
                int height24 = height22;
                if (width22 < this.mMinVisibleWidth) {
                    width22 = this.mMinVisibleWidth;
                    width1 = width12;
                    height1 = height12;
                    height22 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(((float) width22) * MIN_ASPECT)));
                } else {
                    width1 = width12;
                    height1 = height12;
                    height22 = height24;
                }
            }
            boolean grows = width2 > right - left || height2 > bottom - top;
            if (grows == (width1 * height1 > width22 * height22)) {
                height2 = height1;
                width = width1;
            } else {
                width = width22;
                height = height22;
                updateDraggedBounds(left, top, right, bottom, width, height);
            }
        }
        height = height2;
        updateDraggedBounds(left, top, right, bottom, width, height);
    }

    void updateDraggedBounds(int left, int top, int right, int bottom, int newWidth, int newHeight) {
        if ((this.mCtrlType & 1) != 0) {
            left = right - newWidth;
        } else {
            right = left + newWidth;
        }
        if ((this.mCtrlType & 4) != 0) {
            top = bottom - newHeight;
        } else {
            bottom = top + newHeight;
        }
        this.mWindowDragBounds.set(left, top, right, bottom);
        checkBoundsForOrientationViolations(this.mWindowDragBounds);
    }

    private void checkBoundsForOrientationViolations(Rect bounds) {
    }

    private void updateWindowDragBounds(int x, int y, Rect stackBounds) {
        int offsetX = Math.round(((float) x) - this.mStartDragX);
        int offsetY = Math.round(((float) y) - this.mStartDragY);
        this.mWindowDragBounds.set(this.mWindowOriginalBounds);
        int maxTop = stackBounds.bottom - this.mMinVisibleHeight;
        this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + offsetX, (stackBounds.left + this.mMinVisibleWidth) - this.mWindowOriginalBounds.width()), stackBounds.right - this.mMinVisibleWidth), Math.min(Math.max(this.mWindowOriginalBounds.top + offsetY, stackBounds.top), maxTop));
    }

    public String toShortString() {
        return TAG;
    }

    static void setFactory(Factory factory) {
        sFactory = factory;
    }

    static TaskPositioner create(WindowManagerService service) {
        if (sFactory == null) {
            sFactory = new Factory() {
            };
        }
        return sFactory.create(service);
    }
}
