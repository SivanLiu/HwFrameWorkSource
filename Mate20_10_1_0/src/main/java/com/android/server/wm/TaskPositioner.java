package com.android.server.wm;

import android.app.IActivityTaskManager;
import android.content.Context;
import android.freeform.HwFreeFormUtils;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.Display;
import android.view.IWindow;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputWindowHandle;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.HwServiceExFactory;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class TaskPositioner implements IBinder.DeathRecipient {
    private static final int CTRL_BOTTOM = 8;
    private static final int CTRL_LEFT = 1;
    private static final int CTRL_NONE = 0;
    private static final int CTRL_RIGHT = 2;
    private static final int CTRL_TOP = 4;
    private static final boolean DEBUG_ORIENTATION_VIOLATIONS = false;
    private static final int DISABLE_ANIMATION_DURATION = -1;
    private static final int DISPOSE_MSG = 0;
    @VisibleForTesting
    static final float MIN_ASPECT = 1.2f;
    public static final float RESIZING_HINT_ALPHA = 0.5f;
    public static final int RESIZING_HINT_DURATION_MS = 0;
    static final int SIDE_MARGIN_DIP = 100;
    private static final String TAG = "WindowManager";
    private static final String TAG_LOCAL = "TaskPositioner";
    private static final int TYPE_HEIGHT = 2;
    private static final int TYPE_WIDTH = 1;
    private static Factory sFactory;
    Handler PCHandler;
    /* access modifiers changed from: private */
    public final IActivityTaskManager mActivityManager;
    IBinder mClientCallback;
    InputChannel mClientChannel;
    private int mCtrlType;
    private int mDelta;
    private DisplayContent mDisplayContent;
    private final DisplayMetrics mDisplayMetrics;
    InputApplicationHandle mDragApplicationHandle;
    @VisibleForTesting
    boolean mDragEnded;
    InputWindowHandle mDragWindowHandle;
    private int mDragbarHeight;
    private int mDragbarWidth;
    private boolean mHasSideinScreen;
    IHwActivityTaskManagerServiceEx mHwATMSEx;
    private int mHwFreeformCaptionBarHeight;
    IHwTaskPositionerEx mHwTPEx;
    /* access modifiers changed from: private */
    public WindowPositionerEventReceiver mInputEventReceiver;
    private boolean mIsOutBound;
    /* access modifiers changed from: private */
    public boolean mIsTouching;
    private final Point mMaxVisibleSize;
    private int mMinVisibleHeight;
    private int mMinVisibleWidth;
    private boolean mPreserveOrientation;
    /* access modifiers changed from: private */
    public boolean mResizing;
    private int mSafeSideWidth;
    InputChannel mServerChannel;
    /* access modifiers changed from: private */
    public final WindowManagerService mService;
    private int mSideMargin;
    private float mStartDragX;
    private float mStartDragY;
    private boolean mStartOrientationWasLandscape;
    @VisibleForTesting
    Task mTask;
    /* access modifiers changed from: private */
    public Rect mTmpRect;
    /* access modifiers changed from: private */
    public final Rect mWindowDragBounds;
    /* access modifiers changed from: private */
    public final Rect mWindowOriginalBounds;

    @Retention(RetentionPolicy.SOURCE)
    @interface CtrlType {
    }

    /* access modifiers changed from: private */
    public final class WindowPositionerEventReceiver extends BatchedInputEventReceiver {
        public WindowPositionerEventReceiver(InputChannel inputChannel, Looper looper, Choreographer choreographer) {
            super(inputChannel, looper, choreographer);
        }

        public void onInputEvent(InputEvent event) {
            if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
                MotionEvent motionEvent = (MotionEvent) event;
                boolean handled = false;
                try {
                    if (TaskPositioner.this.mDragEnded) {
                        finishInputEvent(event, true);
                        return;
                    }
                    float newX = motionEvent.getRawX();
                    float newY = motionEvent.getRawY();
                    int action = motionEvent.getAction();
                    if (action != 0) {
                        if (action == 1) {
                            if (HwFreeFormUtils.isFreeFormEnable()) {
                                boolean unused = TaskPositioner.this.mIsTouching = false;
                                TaskPositioner.this.updateFreeFormOutLine(2);
                            }
                            if (!TaskPositioner.this.mResizing && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(TaskPositioner.this.mTask.getDisplayContent().getDisplayId())) {
                                TaskPositioner.this.processPCWindowFinishDragHitHotArea(TaskPositioner.this.mTask.mTaskRecord, newX, newY);
                            }
                            TaskPositioner.this.relocateOffScreenHwFreeformWindow();
                            TaskPositioner.this.mDragEnded = true;
                        } else if (action == 2) {
                            synchronized (TaskPositioner.this.mService.mGlobalLock) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    TaskPositioner.this.mDragEnded = TaskPositioner.this.notifyMoveLocked(newX, newY);
                                    TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                                } catch (Throwable th) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (!TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                                Trace.traceBegin(32, "wm.TaskPositioner.resizeTask");
                                try {
                                    if (TaskPositioner.this.mTask.inHwFreeFormWindowingMode()) {
                                        TaskPositioner.this.mActivityManager.resizeStack(TaskPositioner.this.mTask.mTaskRecord.getStack().getStackId(), TaskPositioner.this.mWindowDragBounds, false, false, false, -1);
                                    } else if (TaskPositioner.this.mTask.getWindowingMode() == 1) {
                                        Slog.i(TaskPositioner.TAG, "No resize in fullscreen mode");
                                    } else {
                                        TaskPositioner.this.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 1);
                                    }
                                } catch (RemoteException e) {
                                }
                                Trace.traceEnd(32);
                            }
                            if (!TaskPositioner.this.mResizing && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(TaskPositioner.this.mTask.getDisplayContent().getDisplayId())) {
                                TaskPositioner.this.processPCWindowDragHitHotArea(TaskPositioner.this.mTask.mTaskRecord, newX, newY);
                            }
                        } else if (action == 3) {
                            if (!TaskPositioner.this.mResizing && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(TaskPositioner.this.mTask.getDisplayContent().getDisplayId())) {
                                TaskPositioner.this.processPCWindowFinishDragHitHotArea(TaskPositioner.this.mTask.mTaskRecord, newX, newY);
                            }
                            TaskPositioner.this.relocateOffScreenHwFreeformWindow();
                            TaskPositioner.this.mDragEnded = true;
                        }
                    }
                    if (TaskPositioner.this.mDragEnded) {
                        boolean wasResizing = TaskPositioner.this.mResizing;
                        synchronized (TaskPositioner.this.mService.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                TaskPositioner.this.endDragLocked();
                                TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                            } catch (Throwable th2) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th2;
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        try {
                            if (HwFreeFormUtils.isFreeFormEnable() && !((TaskPositioner.this.mWindowOriginalBounds.width() == TaskPositioner.this.mWindowDragBounds.width() && TaskPositioner.this.mWindowOriginalBounds.height() == TaskPositioner.this.mWindowDragBounds.height()) || TaskPositioner.this.mTask.getParent() == null)) {
                                Rect availableRect = TaskPositioner.this.mTask.getParent().getBounds();
                                Context context = TaskPositioner.this.mService.mContext;
                                Flog.bdReport(context, 10067, "{ height:" + availableRect.height() + ",width:" + availableRect.width() + ",left:" + TaskPositioner.this.mWindowDragBounds.left + ",top:" + TaskPositioner.this.mWindowDragBounds.top + ",right:" + TaskPositioner.this.mWindowDragBounds.right + ",bottom:" + TaskPositioner.this.mWindowDragBounds.bottom + "}");
                            }
                            if (wasResizing && !TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                                if (TaskPositioner.this.mTask.inHwFreeFormWindowingMode()) {
                                    TaskPositioner.this.mActivityManager.resizeStack(TaskPositioner.this.mTask.mTaskRecord.getStack().getStackId(), TaskPositioner.this.mWindowDragBounds, false, false, false, -1);
                                } else if (TaskPositioner.this.mTask.getWindowingMode() == 1) {
                                    Slog.i(TaskPositioner.TAG, "No resize in fullscreen mode");
                                } else {
                                    TaskPositioner.this.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 3);
                                }
                            }
                        } catch (RemoteException e2) {
                        }
                        TaskPositioner.this.mService.mTaskPositioningController.finishTaskPositioning();
                    }
                    handled = true;
                    finishInputEvent(event, handled);
                } catch (Exception e3) {
                    Slog.e(TaskPositioner.TAG, "Exception caught by drag handleMotion", e3);
                } catch (Throwable th3) {
                    finishInputEvent(event, false);
                    throw th3;
                }
            }
        }
    }

    @VisibleForTesting
    TaskPositioner(WindowManagerService service, IActivityTaskManager activityManager) {
        this.mDelta = 0;
        this.mIsTouching = false;
        this.mIsOutBound = false;
        this.mDisplayMetrics = new DisplayMetrics();
        this.mTmpRect = new Rect();
        this.mWindowOriginalBounds = new Rect();
        this.mWindowDragBounds = new Rect();
        this.mMaxVisibleSize = new Point();
        this.mCtrlType = 0;
        this.PCHandler = null;
        this.mHasSideinScreen = false;
        this.mSafeSideWidth = 0;
        this.mHwTPEx = null;
        this.mHwATMSEx = null;
        this.mService = service;
        this.mActivityManager = activityManager;
    }

    TaskPositioner(WindowManagerService service) {
        this(service, service.mActivityTaskManager);
        this.mHwTPEx = HwServiceExFactory.getHwTaskPositionerEx(service);
        this.mHwATMSEx = service.mAtmService.mHwATMSEx;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public Rect getWindowDragBounds() {
        return this.mWindowDragBounds;
    }

    /* access modifiers changed from: package-private */
    public void register(DisplayContent displayContent) {
        Display display = displayContent.getDisplay();
        if (this.mClientChannel != null) {
            Slog.e(TAG, "Task positioner already registered");
            return;
        }
        this.mDisplayContent = displayContent;
        display.getMetrics(this.mDisplayMetrics);
        InputChannel[] channels = InputChannel.openInputChannelPair(TAG);
        this.mServerChannel = channels[0];
        this.mClientChannel = channels[1];
        this.mService.mInputManager.registerInputChannel(this.mServerChannel, (IBinder) null);
        this.mInputEventReceiver = new WindowPositionerEventReceiver(this.mClientChannel, this.mService.mAnimationHandler.getLooper(), this.mService.mAnimator.getChoreographer());
        this.mDragApplicationHandle = new InputApplicationHandle(new Binder());
        InputApplicationHandle inputApplicationHandle = this.mDragApplicationHandle;
        inputApplicationHandle.name = TAG;
        inputApplicationHandle.dispatchingTimeoutNanos = 5000000000L;
        this.mDragWindowHandle = new InputWindowHandle(inputApplicationHandle, (IWindow) null, display.getDisplayId());
        InputWindowHandle inputWindowHandle = this.mDragWindowHandle;
        inputWindowHandle.name = TAG;
        inputWindowHandle.token = this.mServerChannel.getToken();
        this.mDragWindowHandle.layer = this.mService.getDragLayerLocked();
        InputWindowHandle inputWindowHandle2 = this.mDragWindowHandle;
        inputWindowHandle2.layoutParamsFlags = 0;
        inputWindowHandle2.layoutParamsType = 2016;
        inputWindowHandle2.dispatchingTimeoutNanos = 5000000000L;
        inputWindowHandle2.visible = true;
        inputWindowHandle2.canReceiveKeys = false;
        inputWindowHandle2.hasFocus = true;
        inputWindowHandle2.hasWallpaper = false;
        inputWindowHandle2.paused = false;
        inputWindowHandle2.ownerPid = Process.myPid();
        this.mDragWindowHandle.ownerUid = Process.myUid();
        InputWindowHandle inputWindowHandle3 = this.mDragWindowHandle;
        inputWindowHandle3.inputFeatures = 0;
        inputWindowHandle3.scaleFactor = 1.0f;
        inputWindowHandle3.touchableRegion.setEmpty();
        InputWindowHandle inputWindowHandle4 = this.mDragWindowHandle;
        inputWindowHandle4.frameLeft = 0;
        inputWindowHandle4.frameTop = 0;
        Point p = new Point();
        display.getRealSize(p);
        this.mDragWindowHandle.frameRight = p.x;
        this.mDragWindowHandle.frameBottom = p.y;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d(TAG, "Pausing rotation during re-position");
        }
        this.mDisplayContent.pauseRotationLocked();
        this.mDisplayContent.getInputMonitor().updateInputWindowsLw(true);
        this.mSideMargin = WindowManagerService.dipToPixel(SIDE_MARGIN_DIP, this.mDisplayMetrics);
        this.mMinVisibleWidth = WindowManagerService.dipToPixel(48, this.mDisplayMetrics);
        this.mMinVisibleHeight = WindowManagerService.dipToPixel(32, this.mDisplayMetrics);
        this.mDragbarWidth = WindowManagerService.dipToPixel(70, this.mDisplayMetrics);
        this.mDragbarHeight = WindowManagerService.dipToPixel(4, this.mDisplayMetrics);
        this.mHwFreeformCaptionBarHeight = WindowManagerService.dipToPixel(36, this.mDisplayMetrics);
        display.getRealSize(this.mMaxVisibleSize);
        HwFreeFormUtils.computeFreeFormSize(this.mMaxVisibleSize);
        this.mDelta = WindowManagerService.dipToPixel(10, this.mDisplayMetrics);
        this.mDragEnded = false;
    }

    /* access modifiers changed from: package-private */
    public void unregister() {
        if (this.mClientChannel == null) {
            Slog.e(TAG, "Task positioner not registered");
            return;
        }
        this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
        if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mDisplayContent.getDisplayId())) {
            this.mService.mAnimationHandler.post(new Runnable() {
                /* class com.android.server.wm.$$Lambda$TaskPositioner$2nYLiRaGrINcRTe4opwrzYSOcIU */

                public final void run() {
                    TaskPositioner.this.lambda$unregister$0$TaskPositioner();
                }
            });
        } else {
            this.PCHandler = new Handler(this.mService.mAnimationHandler.getLooper()) {
                /* class com.android.server.wm.TaskPositioner.AnonymousClass1 */

                public void handleMessage(Message msg) {
                    if (msg.what == 0) {
                        TaskPositioner.this.mInputEventReceiver.dispose();
                        WindowPositionerEventReceiver unused = TaskPositioner.this.mInputEventReceiver = null;
                    }
                }
            };
            this.PCHandler.removeMessages(0);
            this.PCHandler.sendEmptyMessage(0);
        }
        this.mClientChannel.dispose();
        this.mServerChannel.dispose();
        this.mClientChannel = null;
        this.mServerChannel = null;
        this.mDragWindowHandle = null;
        this.mDragApplicationHandle = null;
        this.mDragEnded = true;
        this.mDisplayContent.getInputMonitor().updateInputWindowsLw(true);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d(TAG, "Resuming rotation after re-position");
        }
        this.mDisplayContent.resumeRotationLocked();
        this.mDisplayContent = null;
        this.mClientCallback.unlinkToDeath(this, 0);
    }

    public /* synthetic */ void lambda$unregister$0$TaskPositioner() {
        this.mInputEventReceiver.dispose();
        this.mInputEventReceiver = null;
    }

    /* access modifiers changed from: package-private */
    public void startDrag(WindowState win, boolean resize, boolean preserveOrientation, float startX, float startY) {
        try {
            this.mClientCallback = win.mClient.asBinder();
            this.mClientCallback.linkToDeath(this, 0);
            this.mTask = win.getTask();
            this.mTask.getBounds(this.mTmpRect);
            this.mHasSideinScreen = HwDisplaySizeUtil.hasSideInScreen();
            this.mSafeSideWidth = HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
            startDrag(resize, preserveOrientation, startX, startY, this.mTmpRect);
        } catch (RemoteException e) {
            this.mService.mTaskPositioningController.finishTaskPositioning();
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: protected */
    public void startDrag(boolean resize, boolean preserveOrientation, float startX, float startY, Rect startBounds) {
        boolean z = false;
        this.mCtrlType = 0;
        this.mStartDragX = startX;
        this.mStartDragY = startY;
        this.mPreserveOrientation = preserveOrientation;
        if (resize) {
            if (startX < ((float) (startBounds.left + this.mDelta))) {
                this.mCtrlType |= 1;
            }
            if (startX > ((float) (startBounds.right - this.mDelta))) {
                this.mCtrlType |= 2;
            }
            if (startY < ((float) startBounds.top)) {
                this.mCtrlType |= 4;
            }
            if (startY > ((float) (startBounds.bottom - this.mDelta))) {
                this.mCtrlType |= CTRL_BOTTOM;
            }
            this.mResizing = this.mCtrlType != 0;
        }
        if (startBounds.width() >= startBounds.height()) {
            z = true;
        }
        this.mStartOrientationWasLandscape = z;
        this.mWindowOriginalBounds.set(startBounds);
        if (this.mResizing) {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    notifyMoveLocked(startX, startY);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            this.mService.mH.post(new Runnable(startBounds) {
                /* class com.android.server.wm.$$Lambda$TaskPositioner$EdlcBmEljXYYxPq6EsgrXICOpqw */
                private final /* synthetic */ Rect f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    TaskPositioner.this.lambda$startDrag$1$TaskPositioner(this.f$1);
                }
            });
        }
        this.mWindowDragBounds.set(startBounds);
    }

    public /* synthetic */ void lambda$startDrag$1$TaskPositioner(Rect startBounds) {
        try {
            if (this.mTask.inHwFreeFormWindowingMode()) {
                this.mActivityManager.resizeStack(this.mTask.mTaskRecord.getStackId(), startBounds, false, false, false, -1);
            } else {
                this.mActivityManager.resizeTask(this.mTask.mTaskId, startBounds, 3);
            }
        } catch (RemoteException e) {
        }
    }

    /* access modifiers changed from: private */
    public void endDragLocked() {
        this.mResizing = false;
        this.mTask.setDragResizing(false, 0);
    }

    /* JADX INFO: Multiple debug info for r0v7 int: [D('nX' int), D('stableBounds' android.graphics.Rect)] */
    /* access modifiers changed from: private */
    public boolean notifyMoveLocked(float x, float y) {
        if (this.mCtrlType != 0) {
            if (!this.mIsTouching) {
                updateFreeFormOutLine(4);
                this.mIsTouching = true;
            }
            resizeDrag(x, y);
            this.mTask.setDragResizing(true, 0);
            return false;
        }
        if (this.mTask.inHwFreeFormWindowingMode()) {
            allowFreeformDragPos(this.mTmpRect);
        } else {
            this.mTask.mStack.getDimBounds(this.mTmpRect);
        }
        if (this.mTask.mStack != null && HwPCUtils.isExtDynamicStack(this.mTask.mStack.mStackId)) {
            this.mTmpRect.bottom = this.mTask.mStack.getDisplayInfo().appHeight;
        }
        if (!this.mTask.inHwFreeFormWindowingMode()) {
            Rect stableBounds = new Rect();
            this.mDisplayContent.getStableRect(stableBounds);
            this.mTmpRect.intersect(stableBounds);
        }
        int nX = (int) x;
        int nY = (int) y;
        if (!this.mTmpRect.contains(nX, nY)) {
            nX = Math.min(Math.max(nX, this.mTmpRect.left), this.mTmpRect.right);
            nY = Math.min(Math.max(nY, this.mTmpRect.top), this.mTmpRect.bottom);
        }
        if (this.mTask.inHwFreeFormWindowingMode()) {
            updateHwFreeformWindowDragBounds(nX, nY, this.mTmpRect);
        } else {
            updateWindowDragBounds(nX, nY, this.mTmpRect);
        }
        return false;
    }

    private void allowFreeformDragPos(Rect outBounds) {
        DisplayContent displayContent;
        this.mDisplayContent.getStableRect(outBounds);
        if (this.mService.mAtmService.mHwATMSEx.isPhoneLandscape(this.mDisplayContent) && (displayContent = this.mDisplayContent) != null && displayContent.getDisplayPolicy() != null) {
            int i = 0;
            outBounds.top = 0;
            outBounds.top += this.mHasSideinScreen ? this.mSafeSideWidth : 0;
            WindowState statusBar = this.mDisplayContent.getDisplayPolicy().getStatusBar();
            if (this.mService.mAtmService.mHwATMSEx.isStatusBarPermenantlyShowing()) {
                int i2 = outBounds.top;
                if (statusBar != null) {
                    i = statusBar.mWindowFrames.mFrame.height();
                }
                outBounds.top = i2 + i;
                return;
            }
            int i3 = outBounds.top;
            if (statusBar != null) {
                i = statusBar.mWindowFrames.mFrame.height() / 2;
            }
            outBounds.top = i3 + i;
        }
    }

    /* JADX INFO: Multiple debug info for r0v25 'tmpWidth'  int: [D('width' int), D('tmpWidth' int)] */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x024b, code lost:
        if (r4 != r0) goto L_0x0252;
     */
    @VisibleForTesting
    public void resizeDrag(float x, float y) {
        char c;
        int width;
        int width2;
        int height;
        int tmpWidth;
        int width1;
        int height1;
        int width22;
        int height2;
        int width12;
        int height12;
        int deltaX = Math.round(x - this.mStartDragX);
        int deltaY = Math.round(y - this.mStartDragY);
        int left = this.mWindowOriginalBounds.left;
        int top = this.mWindowOriginalBounds.top;
        int right = this.mWindowOriginalBounds.right;
        int bottom = this.mWindowOriginalBounds.bottom;
        if (!this.mPreserveOrientation) {
            c = 0;
        } else {
            c = this.mStartOrientationWasLandscape ? (char) 39322 : 21845;
        }
        int width3 = right - left;
        int height3 = bottom - top;
        int i = this.mCtrlType;
        if ((i & 1) != 0) {
            width3 = Math.max(this.mMinVisibleWidth, width3 - deltaX);
        } else if ((i & 2) != 0) {
            width3 = Math.max(this.mMinVisibleWidth, width3 + deltaX);
        }
        int i2 = this.mCtrlType;
        if ((i2 & 4) != 0) {
            height3 = Math.max(this.mMinVisibleHeight, height3 - deltaY);
        } else if ((i2 & CTRL_BOTTOM) != 0) {
            height3 = Math.max(this.mMinVisibleHeight, height3 + deltaY);
        }
        float aspect = ((float) width3) / ((float) height3);
        if (!this.mPreserveOrientation || ((!this.mStartOrientationWasLandscape || aspect >= MIN_ASPECT) && (this.mStartOrientationWasLandscape || ((double) aspect) <= 0.8333333002196431d))) {
            width = width3;
        } else {
            if (this.mStartOrientationWasLandscape) {
                int width13 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, width3));
                height1 = Math.min(height3, Math.round(((float) width13) / MIN_ASPECT));
                if (height1 < this.mMinVisibleHeight) {
                    height1 = this.mMinVisibleHeight;
                    width1 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(((float) height1) * MIN_ASPECT)));
                } else {
                    width1 = width13;
                }
                height2 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, height3));
                width22 = Math.max(width3, Math.round(((float) height2) * MIN_ASPECT));
                if (width22 < this.mMinVisibleWidth) {
                    width22 = this.mMinVisibleWidth;
                    height2 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(((float) width22) / MIN_ASPECT)));
                }
            } else {
                int width14 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, width3));
                int height13 = Math.max(height3, Math.round(((float) width14) * MIN_ASPECT));
                if (height13 < this.mMinVisibleHeight) {
                    int height14 = this.mMinVisibleHeight;
                    width12 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(((float) height14) / MIN_ASPECT)));
                    height12 = height14;
                } else {
                    width12 = width14;
                    height12 = height13;
                }
                height2 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, height3));
                width22 = Math.min(width3, Math.round(((float) height2) / MIN_ASPECT));
                if (width22 < this.mMinVisibleWidth) {
                    width22 = this.mMinVisibleWidth;
                    height2 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(((float) width22) * MIN_ASPECT)));
                }
            }
            if ((width3 > right - left || height3 > bottom - top) == (width1 * height1 > width22 * height2)) {
                width = width1;
                height3 = height1;
            } else {
                width = width22;
                height3 = height2;
            }
        }
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mTask.getDisplayContent().getDisplayId())) {
            width = limitPCWindowSize(width, 1);
            height3 = limitPCWindowSize(height3, 2);
        }
        if (HwFreeFormUtils.isFreeFormEnable()) {
            boolean isLandscape = false;
            DisplayContent displayContent = this.mTask.getDisplayContent();
            if (displayContent != null) {
                if (HwFreeFormUtils.getFreeformMaxLength() == 0) {
                    Point maxDisplaySize = new Point();
                    displayContent.getDisplay().getRealSize(maxDisplaySize);
                    HwFreeFormUtils.computeFreeFormSize(maxDisplaySize);
                }
                Rect displayBounds = displayContent.mAcitvityDisplay.getBounds();
                WindowManagerService windowManagerService = this.mService;
                isLandscape = !WindowManagerService.IS_TABLET && displayBounds.width() > displayBounds.height();
            }
            int tmpFreeformMaxLength = HwFreeFormUtils.getFreeformMaxLength();
            int tmpFreeformMinLength = HwFreeFormUtils.getFreeformMinLength();
            int tmpLandscapeMaxLength = isLandscape ? HwFreeFormUtils.getLandscapeFreeformMaxLength() : tmpFreeformMaxLength;
            int tmpHeight = tmpLandscapeMaxLength > height3 ? height3 : tmpLandscapeMaxLength;
            int height4 = tmpHeight > tmpFreeformMinLength ? tmpHeight : tmpFreeformMinLength;
            int tmpWidth2 = tmpFreeformMaxLength > width ? width : tmpFreeformMaxLength;
            int width4 = tmpWidth2 > tmpFreeformMinLength ? tmpWidth2 : tmpFreeformMinLength;
            if (tmpFreeformMaxLength == height4) {
                tmpWidth = width4;
            } else {
                tmpWidth = width4;
            }
            if (!(height4 == tmpFreeformMinLength && tmpWidth == tmpFreeformMinLength)) {
                width2 = tmpWidth;
                if (this.mIsOutBound) {
                    updateFreeFormOutLine(4);
                    this.mIsOutBound = false;
                }
                height = height4;
            }
            width2 = tmpWidth;
            if (!this.mIsOutBound) {
                updateFreeFormOutLine(3);
                this.mIsOutBound = true;
            }
            height = height4;
        } else {
            width2 = width;
            height = height3;
        }
        updateDraggedBounds(left, top, right, bottom, width2, height);
    }

    /* access modifiers changed from: package-private */
    public void updateDraggedBounds(int left, int top, int right, int bottom, int newWidth, int newHeight) {
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

    /* access modifiers changed from: private */
    public void relocateOffScreenHwFreeformWindow() {
        Rect tmpRect;
        if (this.mTask.inHwFreeFormWindowingMode() && this.mTask.mStack != null) {
            Rect originalWindowBounds = new Rect();
            if (!isImeVisible() || this.mService.mAtmService.mHwATMSEx.isPhoneLandscape(this.mDisplayContent)) {
                this.mTask.getDimBounds(originalWindowBounds);
                tmpRect = this.mTmpRect;
            } else {
                this.mTask.getBounds(originalWindowBounds);
                tmpRect = originalWindowBounds;
            }
            Rect validHwFreeformWindowBounds = relocateOffScreenWindow(tmpRect, this.mTask.mStack.mActivityStack);
            if (!validHwFreeformWindowBounds.equals(originalWindowBounds)) {
                try {
                    this.mActivityManager.resizeStack(this.mTask.mTaskRecord.getStack().getStackId(), validHwFreeformWindowBounds, false, false, false, -1);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Exception caught by relocateOffScreenHwFreeformWindow", e);
                }
            }
            this.mHwATMSEx.updateDragFreeFormPos(validHwFreeformWindowBounds, this.mTask.mStack.mActivityStack.getDisplay());
        }
    }

    private void updateHwFreeformWindowDragBounds(int x, int y, Rect stackBounds) {
        int offsetX = Math.round(((float) x) - this.mStartDragX);
        int offsetY = Math.round(((float) y) - this.mStartDragY);
        int captionSpareWidth = (this.mWindowOriginalBounds.width() - this.mDragbarWidth) / 2;
        int maxLeft = (stackBounds.right - captionSpareWidth) - (this.mDragbarWidth / 2);
        int minLeft = (stackBounds.left - captionSpareWidth) - (this.mDragbarWidth / 2);
        int minTop = stackBounds.top;
        int maxTop = (stackBounds.bottom - ((this.mHwFreeformCaptionBarHeight - this.mDragbarHeight) / 2)) - this.mDragbarHeight;
        if (!isImeVisible() || this.mService.mAtmService.mHwATMSEx.isPhoneLandscape(this.mDisplayContent)) {
            this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + offsetX, minLeft), maxLeft), Math.min(Math.max(this.mWindowOriginalBounds.top + offsetY, minTop), maxTop));
        } else {
            this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + offsetX, minLeft), maxLeft), this.mWindowOriginalBounds.top);
        }
    }

    private boolean isImeVisible() {
        WindowState imeWin = this.mService.mRoot.getCurrentInputMethodWindow();
        return imeWin != null && imeWin.isVisibleNow();
    }

    private void updateWindowDragBounds(int x, int y, Rect stackBounds) {
        int offsetX = Math.round(((float) x) - this.mStartDragX);
        int offsetY = Math.round(((float) y) - this.mStartDragY);
        this.mWindowDragBounds.set(this.mWindowOriginalBounds);
        int maxLeft = stackBounds.right - this.mMinVisibleWidth;
        int minLeft = (stackBounds.left + this.mMinVisibleWidth) - this.mWindowOriginalBounds.width();
        int minTop = stackBounds.top;
        this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + offsetX, minLeft), maxLeft), Math.min(Math.max(this.mWindowOriginalBounds.top + offsetY, minTop), stackBounds.bottom - this.mMinVisibleHeight));
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
                /* class com.android.server.wm.TaskPositioner.AnonymousClass2 */
            };
        }
        return sFactory.create(service);
    }

    public void binderDied() {
        this.mService.mTaskPositioningController.finishTaskPositioning();
    }

    interface Factory {
        default TaskPositioner create(WindowManagerService service) {
            return new TaskPositioner(service);
        }
    }

    /* access modifiers changed from: private */
    public void updateFreeFormOutLine(int color) {
        this.mHwTPEx.updateFreeFormOutLine(color);
    }

    /* access modifiers changed from: private */
    public void processPCWindowDragHitHotArea(TaskRecord taskRecord, float newX, float newY) {
        this.mHwTPEx.processPCWindowDragHitHotArea(taskRecord, newX, newY);
    }

    /* access modifiers changed from: private */
    public void processPCWindowFinishDragHitHotArea(TaskRecord taskRecord, float newX, float newY) {
        this.mHwTPEx.processPCWindowFinishDragHitHotArea(taskRecord, newX, newY);
    }

    private int limitPCWindowSize(int legnth, int limitType) {
        return this.mHwTPEx.limitPCWindowSize(legnth, limitType);
    }

    public Rect relocateOffScreenWindow(Rect originalWindowBounds, ActivityStack stack) {
        return this.mHwATMSEx.relocateOffScreenWindow(originalWindowBounds, stack);
    }
}
