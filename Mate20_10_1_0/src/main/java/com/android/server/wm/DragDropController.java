package com.android.server.wm;

import android.content.ClipData;
import android.filterfw.geometry.Point;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.InputChannel;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.internal.util.Preconditions;
import com.android.server.input.InputManagerService;
import com.android.server.wm.DragState;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.view.HwWindowManager;
import java.util.concurrent.atomic.AtomicReference;

class DragDropController {
    private static final long DELAY_CANCEL_HW_MULTI_WINDOW_DRAG = 500;
    private static final long DRAG_DELAYED_TIMEOUT_MS = 3000;
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = 0.7071f;
    private static final long DRAG_TIMEOUT_MS = 5000;
    private static final String HW_FREE_FROM_PRE_DRAG_INPUT_CHANNEL = "hwFreeFormPreDragInputChannel";
    static final int MSG_ANIMATION_END = 2;
    private static final int MSG_CANCEL_HW_MULTI_WINDOW_DRAG = 5;
    static final int MSG_DRAG_END_TIMEOUT = 0;
    static final int MSG_REMOVE_HW_MULTI_WINDOW_DRAG_SURFACE = 3;
    static final int MSG_REMOVE_HW_MULTI_WINDOW_DROP_SURFACE = 4;
    static final int MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT = 1;
    private AtomicReference<WindowManagerInternal.IDragDropCallback> mCallback = new AtomicReference<>(new WindowManagerInternal.IDragDropCallback() {
        /* class com.android.server.wm.DragDropController.AnonymousClass1 */
    });
    /* access modifiers changed from: private */
    public DragState mDragState;
    private final Handler mHandler;
    private boolean mIsCancelHwMultiWindowDragMsgExist = false;
    private WindowManagerService mService;

    /* access modifiers changed from: package-private */
    public boolean dragDropActiveLocked() {
        DragState dragState = this.mDragState;
        return dragState != null && !dragState.isClosing();
    }

    /* access modifiers changed from: package-private */
    public void registerCallback(WindowManagerInternal.IDragDropCallback callback) {
        Preconditions.checkNotNull(callback);
        this.mCallback.set(callback);
    }

    DragDropController(WindowManagerService service, Looper looper) {
        this.mService = service;
        this.mHandler = new DragHandler(service, looper);
    }

    /* access modifiers changed from: package-private */
    public void sendDragStartedIfNeededLocked(WindowState window) {
        this.mDragState.sendDragStartedIfNeededLocked(window);
    }

    /* JADX INFO: Multiple debug info for r2v18 'callingWin'  com.android.server.wm.WindowState: [D('callingWin' com.android.server.wm.WindowState), D('custInputChannel' android.view.InputChannel)] */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:222:0x03ed, code lost:
        r0 = th;
     */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x01d9 A[SYNTHETIC, Splitter:B:100:0x01d9] */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x01f2  */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x01f4  */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x0241  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x024a  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0258  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x0290  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x0333 A[Catch:{ all -> 0x038d }] */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x0354  */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x035a A[SYNTHETIC, Splitter:B:171:0x035a] */
    /* JADX WARNING: Removed duplicated region for block: B:207:0x03ce A[SYNTHETIC, Splitter:B:207:0x03ce] */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01d0 A[Catch:{ all -> 0x039b }] */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x01d2 A[Catch:{ all -> 0x039b }] */
    public IBinder performDrag(SurfaceSession session, int callerPid, int callerUid, IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        float touchX2;
        float f;
        float thumbCenterY2;
        SurfaceControl surface2;
        float thumbCenterY3;
        InputChannel custInputChannel;
        Display display;
        WindowManagerInternal.IDragDropCallback iDragDropCallback;
        DragState dragState;
        InputManagerService inputManagerService;
        InputChannel winBinder;
        InputChannel custInputChannel2;
        float touchY2;
        float touchX3;
        float touchX4 = touchX;
        float thumbCenterY4 = thumbCenterY;
        IBinder dragToken = new Binder();
        boolean callbackResult = this.mCallback.get().prePerformDrag(window, dragToken, touchSource, touchX, touchY, thumbCenterX, thumbCenterY, data);
        try {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!callbackResult) {
                        try {
                            Slog.w("WindowManager", "IDragDropCallback rejects the performDrag request");
                            if (surface != null) {
                                try {
                                    surface.release();
                                } catch (Throwable th) {
                                    th = th;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                            if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                this.mDragState.closeLocked();
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            this.mCallback.get().postPerformDrag();
                            return null;
                        } catch (Throwable th3) {
                            touchPoint = th3;
                            touchX2 = touchX4;
                            thumbCenterY2 = thumbCenterY4;
                            surface2 = surface;
                            f = touchY;
                        }
                    } else if (dragDropActiveLocked()) {
                        Slog.w("WindowManager", "Drag already in progress");
                        if (surface != null) {
                            surface.release();
                        }
                        if (this.mDragState != null && !this.mDragState.isInProgress()) {
                            this.mDragState.closeLocked();
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        this.mCallback.get().postPerformDrag();
                        return null;
                    } else {
                        InputChannel callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                        if (callingWin == null) {
                            Slog.w("WindowManager", "Bad requesting window " + window);
                            if (surface != null) {
                                surface.release();
                            }
                            if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                this.mDragState.closeLocked();
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            this.mCallback.get().postPerformDrag();
                            return null;
                        }
                        if (HwPCUtils.isInSinkWindowsCastMode()) {
                            this.mService.setDragWinState(callingWin);
                        }
                        Slog.i("WindowManager", "performDrag callingWin=" + callingWin);
                        DisplayContent displayContent = callingWin.getDisplayContent();
                        if (displayContent == null) {
                            Slog.w("WindowManager", "display content is null");
                            if (surface != null) {
                                surface.release();
                            }
                            if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                this.mDragState.closeLocked();
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            this.mCallback.get().postPerformDrag();
                            return null;
                        }
                        if (isDragOnRemoteDisplay()) {
                            HwWindowManager.dragStartForMultiDisplay(data);
                        }
                        float alpha = (flags & 512) == 0 ? DRAG_SHADOW_ALPHA_TRANSPARENT : 1.0f;
                        try {
                            f = touchY;
                            touchX2 = touchX4;
                        } catch (Throwable th4) {
                            touchPoint = th4;
                            thumbCenterY3 = thumbCenterY;
                            touchX2 = touchX4;
                            surface2 = surface;
                            f = touchY;
                            if (surface2 != null) {
                            }
                            this.mDragState.closeLocked();
                            throw touchPoint;
                        }
                        try {
                            this.mDragState = new DragState(this.mService, this, new Binder(), surface, flags, window.asBinder());
                            surface2 = null;
                            try {
                                this.mDragState.mPid = callerPid;
                                this.mDragState.mUid = callerUid;
                                if (HwPCUtils.isInWindowsCastMode()) {
                                    try {
                                        if (this.mService.getFocusedDisplayId() == HwPCUtils.getWindowsCastDisplayId()) {
                                            this.mDragState.mOriginalAlpha = 0.0f;
                                            this.mDragState.mToken = dragToken;
                                            this.mDragState.mDisplayContent = displayContent;
                                            this.mIsCancelHwMultiWindowDragMsgExist = (flags & 1024) == 0;
                                            if (this.mIsCancelHwMultiWindowDragMsgExist) {
                                                try {
                                                    sendTimeoutMessageDelayed(5, new Point(touchX2, f), DELAY_CANCEL_HW_MULTI_WINDOW_DRAG);
                                                } catch (Throwable th5) {
                                                    touchPoint = th5;
                                                    thumbCenterY2 = thumbCenterY;
                                                }
                                            }
                                            this.mDragState.setIsDropSuccessAnimEnabled((flags & 1024) == 0);
                                            custInputChannel = null;
                                            if (!((1073741824 & flags) == 0 || data == null || data.getItemAt(0) == null || data.getItemAt(0).getIntent() == null)) {
                                                try {
                                                    custInputChannel = data.getItemAt(0).getIntent().getParcelableExtra(HW_FREE_FROM_PRE_DRAG_INPUT_CHANNEL);
                                                } catch (ClassCastException e) {
                                                    Slog.w("WindowManager", "get a wrong input channel, maybe put a wrong clip data.");
                                                }
                                            }
                                            display = displayContent.getDisplay();
                                            iDragDropCallback = this.mCallback.get();
                                            dragState = this.mDragState;
                                            inputManagerService = this.mService.mInputManager;
                                            if (custInputChannel == null) {
                                                custInputChannel2 = callingWin;
                                                winBinder = custInputChannel;
                                            } else {
                                                custInputChannel2 = callingWin;
                                                winBinder = ((WindowState) custInputChannel2).mInputChannel;
                                            }
                                            if (iDragDropCallback.registerInputChannel(dragState, display, inputManagerService, winBinder)) {
                                                Slog.e("WindowManager", "Unable to transfer touch focus");
                                                if (0 != 0) {
                                                    try {
                                                        surface2.release();
                                                    } catch (Throwable th6) {
                                                        th = th6;
                                                        thumbCenterY4 = thumbCenterY;
                                                        touchX4 = touchX2;
                                                        while (true) {
                                                            break;
                                                        }
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                                    this.mDragState.closeLocked();
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                this.mCallback.get().postPerformDrag();
                                                return null;
                                            }
                                            this.mDragState.mData = data;
                                            this.mDragState.broadcastDragStartedLocked(touchX2, f);
                                            this.mDragState.overridePointerIconLocked(touchSource);
                                            float thumbCenterX2 = thumbCenterX;
                                            try {
                                                this.mDragState.mThumbOffsetX = thumbCenterX2;
                                                thumbCenterY2 = thumbCenterY;
                                                try {
                                                    this.mDragState.mThumbOffsetY = thumbCenterY2;
                                                    SurfaceControl surfaceControl = this.mDragState.mSurfaceControl;
                                                    if (display.getDisplayId() == 0) {
                                                        if (this.mService.getLazyMode() != 0) {
                                                            int dw = display.getWidth();
                                                            int dh = display.getHeight();
                                                            if (1 == this.mService.getLazyMode()) {
                                                                touchX3 = touchX2 * 0.75f;
                                                            } else if (2 == this.mService.getLazyMode()) {
                                                                touchX3 = (touchX2 * 0.75f) + (((float) dw) * 0.25f);
                                                            } else {
                                                                touchX3 = touchX2;
                                                            }
                                                            touchY2 = (((float) dh) * 0.25f) + (touchY * 0.75f);
                                                            thumbCenterX2 *= 0.75f;
                                                            thumbCenterY2 *= 0.75f;
                                                            touchX2 = touchX3;
                                                            SurfaceControl.Transaction transaction = custInputChannel2.getPendingTransaction();
                                                            transaction.setAlpha(surfaceControl, this.mDragState.mOriginalAlpha);
                                                            try {
                                                                transaction.setPosition(surfaceControl, touchX2 - thumbCenterX2, touchY2 - thumbCenterY2);
                                                                transaction.show(surfaceControl);
                                                                displayContent.reparentToOverlay(transaction, surfaceControl);
                                                                custInputChannel2.scheduleAnimation();
                                                                this.mDragState.notifyLocationLocked(touchX2, touchY2);
                                                                if (!HwPCUtils.isPcCastModeInServer()) {
                                                                    if (HwPCUtils.getPCDisplayID() == display.getDisplayId()) {
                                                                        this.mHandler.removeMessages(0);
                                                                        this.mHandler.sendEmptyMessageDelayed(0, DRAG_DELAYED_TIMEOUT_MS);
                                                                    }
                                                                }
                                                                if (0 != 0) {
                                                                    try {
                                                                        surface2.release();
                                                                    } catch (Throwable th7) {
                                                                        th = th7;
                                                                        thumbCenterY4 = thumbCenterY2;
                                                                        touchX4 = touchX2;
                                                                        while (true) {
                                                                            break;
                                                                        }
                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                        throw th;
                                                                    }
                                                                }
                                                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                                                    this.mDragState.closeLocked();
                                                                }
                                                            } catch (Throwable th8) {
                                                                touchPoint = th8;
                                                                f = touchY2;
                                                                if (surface2 != null) {
                                                                }
                                                                this.mDragState.closeLocked();
                                                                throw touchPoint;
                                                            }
                                                            try {
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                this.mCallback.get().postPerformDrag();
                                                                return dragToken;
                                                            } catch (Throwable th9) {
                                                                th = th9;
                                                                this.mCallback.get().postPerformDrag();
                                                                throw th;
                                                            }
                                                        }
                                                    }
                                                    touchY2 = touchY;
                                                    try {
                                                        SurfaceControl.Transaction transaction2 = custInputChannel2.getPendingTransaction();
                                                        transaction2.setAlpha(surfaceControl, this.mDragState.mOriginalAlpha);
                                                        transaction2.setPosition(surfaceControl, touchX2 - thumbCenterX2, touchY2 - thumbCenterY2);
                                                        transaction2.show(surfaceControl);
                                                        displayContent.reparentToOverlay(transaction2, surfaceControl);
                                                        custInputChannel2.scheduleAnimation();
                                                        this.mDragState.notifyLocationLocked(touchX2, touchY2);
                                                        if (!HwPCUtils.isPcCastModeInServer()) {
                                                        }
                                                        if (0 != 0) {
                                                        }
                                                        this.mDragState.closeLocked();
                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                        this.mCallback.get().postPerformDrag();
                                                        return dragToken;
                                                    } catch (Throwable th10) {
                                                        touchPoint = th10;
                                                        f = touchY2;
                                                        if (surface2 != null) {
                                                        }
                                                        this.mDragState.closeLocked();
                                                        throw touchPoint;
                                                    }
                                                } catch (Throwable th11) {
                                                    touchPoint = th11;
                                                    f = touchY;
                                                    if (surface2 != null) {
                                                    }
                                                    this.mDragState.closeLocked();
                                                    throw touchPoint;
                                                }
                                            } catch (Throwable th12) {
                                                touchPoint = th12;
                                                thumbCenterY2 = thumbCenterY;
                                                f = touchY;
                                                if (surface2 != null) {
                                                }
                                                this.mDragState.closeLocked();
                                                throw touchPoint;
                                            }
                                        }
                                    } catch (Throwable th13) {
                                        touchPoint = th13;
                                        thumbCenterY2 = thumbCenterY;
                                        if (surface2 != null) {
                                        }
                                        this.mDragState.closeLocked();
                                        throw touchPoint;
                                    }
                                }
                                this.mDragState.mOriginalAlpha = alpha;
                            } catch (Throwable th14) {
                                touchPoint = th14;
                                thumbCenterY2 = thumbCenterY;
                                f = touchY;
                                if (surface2 != null) {
                                    try {
                                        surface2.release();
                                    } catch (Throwable th15) {
                                        th = th15;
                                        thumbCenterY4 = thumbCenterY2;
                                        touchX4 = touchX2;
                                        while (true) {
                                            break;
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                    this.mDragState.closeLocked();
                                }
                                throw touchPoint;
                            }
                        } catch (Throwable th16) {
                            touchPoint = th16;
                            thumbCenterY2 = thumbCenterY;
                            surface2 = surface;
                            f = touchY;
                            if (surface2 != null) {
                            }
                            this.mDragState.closeLocked();
                            throw touchPoint;
                        }
                        try {
                            this.mDragState.mToken = dragToken;
                            this.mDragState.mDisplayContent = displayContent;
                            this.mIsCancelHwMultiWindowDragMsgExist = (flags & 1024) == 0;
                            if (this.mIsCancelHwMultiWindowDragMsgExist) {
                            }
                            this.mDragState.setIsDropSuccessAnimEnabled((flags & 1024) == 0);
                            custInputChannel = null;
                            custInputChannel = data.getItemAt(0).getIntent().getParcelableExtra(HW_FREE_FROM_PRE_DRAG_INPUT_CHANNEL);
                            display = displayContent.getDisplay();
                            iDragDropCallback = this.mCallback.get();
                            dragState = this.mDragState;
                            inputManagerService = this.mService.mInputManager;
                            if (custInputChannel == null) {
                            }
                            if (iDragDropCallback.registerInputChannel(dragState, display, inputManagerService, winBinder)) {
                            }
                        } catch (Throwable th17) {
                            touchPoint = th17;
                            thumbCenterY2 = thumbCenterY;
                            f = touchY;
                            if (surface2 != null) {
                            }
                            this.mDragState.closeLocked();
                            throw touchPoint;
                        }
                    }
                } catch (Throwable th18) {
                    touchPoint = th18;
                    touchX2 = touchX4;
                    thumbCenterY3 = thumbCenterY4;
                    surface2 = surface;
                    f = touchY;
                    if (surface2 != null) {
                    }
                    this.mDragState.closeLocked();
                    throw touchPoint;
                }
            }
        } catch (Throwable th19) {
            th = th19;
            this.mCallback.get().postPerformDrag();
            throw th;
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void reportDropResult(IWindow window, boolean consumed) {
        IBinder token = window.asBinder();
        this.mCallback.get().preReportDropResult(window, consumed);
        try {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mDragState == null) {
                        Slog.w("WindowManager", "Drop result given but no drag in progress");
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } else if (this.mDragState.mToken == token || HwPCUtils.isInSinkWindowsCastMode()) {
                        this.mHandler.removeMessages(0, window.asBinder());
                        if (this.mService.windowForClientLocked((Session) null, window, false) == null) {
                            Slog.w("WindowManager", "Bad result-reporting window " + window);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            this.mCallback.get().postReportDropResult();
                            return;
                        }
                        this.mDragState.mDragResult = consumed;
                        Slog.i("WindowManager", "report drop result : " + consumed);
                        this.mDragState.endDragLocked();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        this.mCallback.get().postReportDropResult();
                    } else {
                        Slog.w("WindowManager", "Invalid drop-result claim by " + window);
                        throw new IllegalStateException("reportDropResult() by non-recipient");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } finally {
            this.mCallback.get().postReportDropResult();
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public void cancelDragAndDrop(IBinder dragToken, boolean skipAnimation) {
        this.mCallback.get().preCancelDragAndDrop(dragToken);
        try {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mDragState == null) {
                        Slog.w("WindowManager", "cancelDragAndDrop() without prepareDrag()");
                        throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
                    } else if (this.mDragState.mToken == dragToken) {
                        this.mDragState.mDragResult = false;
                        this.mDragState.cancelDragLocked(skipAnimation);
                    } else {
                        Slog.w("WindowManager", "cancelDragAndDrop() does not match prepareDrag()");
                        throw new IllegalStateException("cancelDragAndDrop() does not match prepareDrag()");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            this.mCallback.get().postCancelDragAndDrop();
        }
    }

    /* access modifiers changed from: package-private */
    public void handleMotionEvent(boolean keepHandling, float newX, float newY) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (dragDropActiveLocked()) {
                    if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.getPCDisplayID() == this.mDragState.mDisplayContent.getDisplayId()) {
                        this.mHandler.removeMessages(0);
                    }
                    if (this.mIsCancelHwMultiWindowDragMsgExist) {
                        this.mHandler.removeMessages(5);
                        this.mIsCancelHwMultiWindowDragMsgExist = false;
                    }
                    if (keepHandling) {
                        this.mDragState.notifyMoveLocked(newX, newY);
                    } else {
                        this.mDragState.notifyDropLocked(newX, newY);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void dragRecipientEntered(IWindow window) {
    }

    /* access modifiers changed from: package-private */
    public void dragRecipientExited(IWindow window) {
    }

    /* access modifiers changed from: package-private */
    public void setPendingDragEndedLoc(IWindow window, int x, int y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mDragState == null) {
                    Slog.i("WindowManager", "mDragState is null, not to setPendingDragEndedLoc");
                    return;
                }
                this.mDragState.setPendingDragEndedLoc(x, y);
                WindowManagerService.resetPriorityAfterLockedSection();
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setOriginalDragViewCenter(IWindow window, int x, int y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mDragState == null) {
                    Slog.i("WindowManager", "mDragState is null, not to setOriginalDragViewCenter");
                    return;
                }
                this.mDragState.setOriginalDragViewCenter(x, y);
                WindowManagerService.resetPriorityAfterLockedSection();
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void sendHandlerMessage(int what, Object arg) {
        this.mHandler.obtainMessage(what, arg).sendToTarget();
    }

    /* access modifiers changed from: package-private */
    public void sendTimeoutMessage(int what, Object arg) {
        this.mHandler.removeMessages(what, arg);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(what, arg), DRAG_TIMEOUT_MS);
    }

    /* access modifiers changed from: package-private */
    public void sendTimeoutMessageDelayed(int what, Object arg, long delayMillis) {
        this.mHandler.removeMessages(what, arg);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(what, arg), delayMillis);
    }

    /* access modifiers changed from: package-private */
    public void onDragStateClosedLocked(DragState dragState) {
        if (this.mDragState != dragState) {
            Slog.wtf("WindowManager", "Unknown drag state is closed");
        } else {
            this.mDragState = null;
        }
    }

    private class DragHandler extends Handler {
        private final WindowManagerService mService;

        DragHandler(WindowManagerService service, Looper looper) {
            super(looper);
            this.mService = service;
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                Slog.w("WindowManager", "Timeout ending drag to win " + ((IBinder) msg.obj));
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (DragDropController.this.mDragState != null) {
                            DragDropController.this.mDragState.mDragResult = false;
                            DragDropController.this.mDragState.endDragLocked();
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 1) {
                DragState.InputInterceptor interceptor = (DragState.InputInterceptor) msg.obj;
                if (interceptor != null) {
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            interceptor.tearDown();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            } else if (i == 2) {
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (DragDropController.this.mDragState == null) {
                            Slog.wtf("WindowManager", "mDragState unexpectedly became null while plyaing animation");
                            return;
                        }
                        DragDropController.this.mDragState.closeLocked();
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 3) {
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Object object = msg.obj;
                        if (object instanceof SurfaceControl) {
                            SurfaceControl.Transaction transaction = this.mService.mTransactionFactory.make();
                            transaction.remove((SurfaceControl) object).apply();
                            transaction.close();
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 4) {
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Object object2 = msg.obj;
                        if (object2 instanceof SurfaceControl) {
                            SurfaceControl.Transaction transaction2 = this.mService.mTransactionFactory.make();
                            Slog.d("WindowManager", "try to clear surface in handler.");
                            transaction2.remove((SurfaceControl) object2).apply();
                            transaction2.close();
                        }
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i == 5) {
                Object object3 = msg.obj;
                if (object3 instanceof Point) {
                    Point touchPoint = (Point) object3;
                    DragDropController.this.handleMotionEvent(false, touchPoint.x, touchPoint.y);
                }
            }
        }
    }

    private boolean isDragOnRemoteDisplay() {
        if ((!HwPCUtils.isInWindowsCastMode() || this.mService.getFocusedDisplayId() != HwPCUtils.getWindowsCastDisplayId()) && !HwPCUtils.isInSinkWindowsCastMode()) {
            return false;
        }
        Slog.d("WindowManager", "do not need draw shadow on phone when drag on remote dispaly.");
        return true;
    }
}
