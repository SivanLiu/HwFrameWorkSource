package com.android.server.wm;

import android.content.ClipData;
import android.graphics.Point;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import com.android.internal.util.Preconditions;
import com.android.server.input.InputWindowHandle;
import com.android.server.wm.WindowManagerInternal.IDragDropCallback;
import java.util.concurrent.atomic.AtomicReference;

class DragDropController {
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = 0.7071f;
    private static final long DRAG_TIMEOUT_MS = 5000;
    static final int MSG_ANIMATION_END = 2;
    static final int MSG_DRAG_END_TIMEOUT = 0;
    static final int MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT = 1;
    private AtomicReference<IDragDropCallback> mCallback = new AtomicReference(new IDragDropCallback() {
    });
    private DragState mDragState;
    private final Handler mHandler;
    private WindowManagerService mService;

    private class DragHandler extends Handler {
        private final WindowManagerService mService;

        DragHandler(WindowManagerService service, Looper looper) {
            super(looper);
            this.mService = service;
        }

        /* JADX WARNING: Missing block: B:9:0x0023, code:
            return;
     */
        /* JADX WARNING: Missing block: B:13:0x002e, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            InputInterceptor interceptor;
            switch (msg.what) {
                case 0:
                    interceptor = msg.obj;
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState != null) {
                                DragDropController.this.mDragState.mDragResult = false;
                                DragDropController.this.mDragState.endDragLocked();
                            }
                        } finally {
                            while (true) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    break;
                case 1:
                    interceptor = msg.obj;
                    if (interceptor != null) {
                        synchronized (this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                interceptor.tearDown();
                            } finally {
                                while (true) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    return;
                case 2:
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState != null) {
                                DragDropController.this.mDragState.closeLocked();
                                break;
                            } else {
                                Slog.wtf("WindowManager", "mDragState unexpectedly became null while plyaing animation");
                                break;
                            }
                        } finally {
                            while (true) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
            }
        }
    }

    boolean dragDropActiveLocked() {
        return this.mDragState != null;
    }

    InputWindowHandle getInputWindowHandleLocked() {
        return this.mDragState.getInputWindowHandle();
    }

    void registerCallback(IDragDropCallback callback) {
        Preconditions.checkNotNull(callback);
        this.mCallback.set(callback);
    }

    DragDropController(WindowManagerService service, Looper looper) {
        this.mService = service;
        this.mHandler = new DragHandler(service, looper);
    }

    void sendDragStartedIfNeededLocked(WindowState window) {
        this.mDragState.sendDragStartedIfNeededLocked(window);
    }

    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x0331 A:{SYNTHETIC, Splitter: B:179:0x0331} */
    /* JADX WARNING: Missing block: B:21:0x005e, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:22:0x006c, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:40:0x009e, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:41:0x00ac, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:57:0x00e5, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:58:0x00f3, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:74:0x011a, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:75:0x0128, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:105:0x01ae, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:106:0x01bd, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:158:?, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:159:0x02e9, code:
            ((com.android.server.wm.WindowManagerInternal.IDragDropCallback) r8.mCallback.get()).postPerformDrag();
     */
    /* JADX WARNING: Missing block: B:160:0x02f5, code:
            return r9;
     */
    /* JADX WARNING: Missing block: B:161:0x02f6, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:162:0x02f7, code:
            r18 = r1;
            r14 = r5;
     */
    /* JADX WARNING: Missing block: B:194:0x0354, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:195:0x0355, code:
            r4 = r1;
            r14 = r3;
            r6 = r5;
            r3 = r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    IBinder performDrag(SurfaceSession session, int callerPid, int callerUid, IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        Throwable th;
        SurfaceControl surfaceControl;
        boolean z;
        float thumbCenterY2;
        SurfaceControl surface2;
        IWindow iWindow = window;
        float touchX2 = touchX;
        float touchY2 = touchY;
        float thumbCenterX2 = thumbCenterX;
        float thumbCenterY3 = thumbCenterY;
        IBinder dragToken = new Binder();
        boolean callbackResult = ((IDragDropCallback) this.mCallback.get()).prePerformDrag(iWindow, dragToken, touchSource, touchX2, touchY2, thumbCenterX2, thumbCenterY3, data);
        float touchY3;
        float touchX3;
        try {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!callbackResult) {
                        try {
                            Slog.w("WindowManager", "IDragDropCallback rejects the performDrag request");
                            if (surface != null) {
                                try {
                                    surface.release();
                                } catch (Throwable th2) {
                                    th = th2;
                                    surfaceControl = surface;
                                    z = callbackResult;
                                    callbackResult = dragToken;
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
                            if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                this.mDragState.closeLocked();
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            touchY3 = touchY2;
                            touchX3 = touchX2;
                            touchY2 = thumbCenterX2;
                            callbackResult = dragToken;
                            thumbCenterY2 = thumbCenterY3;
                        }
                    } else if (dragDropActiveLocked()) {
                        Slog.w("WindowManager", "Drag already in progress");
                        if (surface != null) {
                            surface.release();
                        }
                        if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                            this.mDragState.closeLocked();
                        }
                    } else {
                        WindowState callingWin = this.mService.windowForClientLocked(null, iWindow, false);
                        if (callingWin == null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad requesting window ");
                            stringBuilder.append(iWindow);
                            Slog.w("WindowManager", stringBuilder.toString());
                            if (surface != null) {
                                surface.release();
                            }
                            if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                this.mDragState.closeLocked();
                            }
                        } else {
                            DisplayContent displayContent = callingWin.getDisplayContent();
                            if (displayContent == null) {
                                Slog.w("WindowManager", "display content is null");
                                if (surface != null) {
                                    surface.release();
                                }
                                if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                    this.mDragState.closeLocked();
                                }
                            } else {
                                int i = flags;
                                float alpha = (i & 512) == 0 ? DRAG_SHADOW_ALPHA_TRANSPARENT : 1.0f;
                                IBinder winBinder = window.asBinder();
                                z = callbackResult;
                                callbackResult = dragToken;
                                dragToken = new Binder();
                                try {
                                    DragState dragState = dragState;
                                    try {
                                        this.mDragState = new DragState(this.mService, this, dragToken, surface, i, winBinder);
                                        surface2 = null;
                                        try {
                                            this.mDragState.mPid = callerPid;
                                            this.mDragState.mUid = callerUid;
                                            this.mDragState.mOriginalAlpha = alpha;
                                            this.mDragState.mToken = callbackResult;
                                            Display display = displayContent.getDisplay();
                                            if (((IDragDropCallback) this.mCallback.get()).registerInputChannel(this.mDragState, display, this.mService.mInputManager, callingWin.mInputChannel)) {
                                                this.mDragState.mDisplayContent = displayContent;
                                                this.mDragState.mData = data;
                                                touchX3 = touchX;
                                                touchY3 = touchY;
                                                try {
                                                    this.mDragState.broadcastDragStartedLocked(touchX3, touchY3);
                                                    this.mDragState.overridePointerIconLocked(touchSource);
                                                    touchY2 = thumbCenterX;
                                                    try {
                                                        this.mDragState.mThumbOffsetX = touchY2;
                                                        thumbCenterY2 = thumbCenterY;
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        thumbCenterY2 = thumbCenterY;
                                                        if (surface2 != null) {
                                                            try {
                                                                surface2.release();
                                                            } catch (Throwable th6) {
                                                                th = th6;
                                                                thumbCenterX2 = touchY2;
                                                                thumbCenterY3 = thumbCenterY2;
                                                                touchX2 = touchX3;
                                                                touchY2 = touchY3;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        }
                                                        if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                                            this.mDragState.closeLocked();
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    touchY2 = thumbCenterX;
                                                    thumbCenterY2 = thumbCenterY;
                                                    if (surface2 != null) {
                                                    }
                                                    this.mDragState.closeLocked();
                                                    throw th;
                                                }
                                                try {
                                                    this.mDragState.mThumbOffsetY = thumbCenterY2;
                                                    SurfaceControl surfaceControl2 = this.mDragState.mSurfaceControl;
                                                    int lazyMode;
                                                    if (display.getDisplayId() == 0) {
                                                        float touchX4;
                                                        lazyMode = this.mService.getLazyMode();
                                                        if (lazyMode != 0) {
                                                            int dw = display.getWidth();
                                                            int dh = display.getHeight();
                                                            if (1 == this.mService.getLazyMode()) {
                                                                touchX2 = touchX3 * 0.75f;
                                                                winBinder = dw;
                                                            } else if (2 == this.mService.getLazyMode()) {
                                                                touchX2 = (touchX3 * 0.75f) + (((float) dw) * 0.25f);
                                                            } else {
                                                                winBinder = dw;
                                                                touchX2 = touchX3;
                                                            }
                                                            touchX4 = touchX2;
                                                            touchX2 = (((float) dh) * 0.25f) + (touchY3 * 0.75f);
                                                            touchY2 *= 0.75f;
                                                            thumbCenterY2 *= 0.75f;
                                                        } else {
                                                            touchX4 = touchX3;
                                                            touchX2 = touchY3;
                                                        }
                                                        Display display2 = display;
                                                        touchX3 = touchX4;
                                                    } else {
                                                        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(display.getDisplayId())) {
                                                            lazyMode = this.mService.getPCScreenDisplayMode();
                                                            if (lazyMode != 0) {
                                                                Point point = new Point();
                                                                display.getRealSize(point);
                                                                i = point.x;
                                                                display = point.y;
                                                                float pcDisplayScale = lazyMode == 1 ? 0.95f : 0.9f;
                                                                touchX2 = ((((float) display) * (1.0f - pcDisplayScale)) / 2.0f) + (touchY3 * pcDisplayScale);
                                                                touchY2 *= pcDisplayScale;
                                                                thumbCenterY2 *= pcDisplayScale;
                                                                touchX3 = (touchX3 * pcDisplayScale) + ((((float) i) * (1.0f - pcDisplayScale)) / 2.0f);
                                                            }
                                                        }
                                                        touchX2 = touchY3;
                                                    }
                                                    try {
                                                        Transaction transaction = callingWin.getPendingTransaction();
                                                        transaction.setAlpha(surfaceControl2, this.mDragState.mOriginalAlpha);
                                                        transaction.setPosition(surfaceControl2, touchX3 - touchY2, touchX2 - thumbCenterY2);
                                                        transaction.show(surfaceControl2);
                                                        displayContent.reparentToOverlay(transaction, surfaceControl2);
                                                        callingWin.scheduleAnimation();
                                                        this.mDragState.notifyLocationLocked(touchX3, touchX2);
                                                        if (surface2 != null) {
                                                            try {
                                                                surface2.release();
                                                            } catch (Throwable th8) {
                                                                th = th8;
                                                                surfaceControl = surface2;
                                                                thumbCenterX2 = touchY2;
                                                                thumbCenterY3 = thumbCenterY2;
                                                                touchY2 = touchX2;
                                                                touchX2 = touchX3;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        }
                                                        if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                                            this.mDragState.closeLocked();
                                                        }
                                                    } catch (Throwable th9) {
                                                        th = th9;
                                                        touchY3 = touchX2;
                                                        if (surface2 != null) {
                                                        }
                                                        this.mDragState.closeLocked();
                                                        throw th;
                                                    }
                                                } catch (Throwable th10) {
                                                    th = th10;
                                                    if (surface2 != null) {
                                                    }
                                                    this.mDragState.closeLocked();
                                                    throw th;
                                                }
                                            }
                                            Slog.e("WindowManager", "Unable to transfer touch focus");
                                            if (surface2 != null) {
                                                try {
                                                    surface2.release();
                                                } catch (Throwable th11) {
                                                    th = th11;
                                                    surfaceControl = surface2;
                                                    thumbCenterY3 = thumbCenterY;
                                                    thumbCenterX2 = thumbCenterX;
                                                    touchY2 = touchY;
                                                    touchX2 = touchX;
                                                    while (true) {
                                                        break;
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            if (!(this.mDragState == null || this.mDragState.isInProgress())) {
                                                this.mDragState.closeLocked();
                                            }
                                        } catch (Throwable th12) {
                                            th = th12;
                                            touchY2 = thumbCenterX;
                                            thumbCenterY2 = thumbCenterY;
                                            touchX3 = touchX;
                                            touchY3 = touchY;
                                            if (surface2 != null) {
                                            }
                                            this.mDragState.closeLocked();
                                            throw th;
                                        }
                                    } catch (Throwable th13) {
                                        th = th13;
                                        touchY2 = thumbCenterX;
                                        thumbCenterY2 = thumbCenterY;
                                        touchX3 = touchX;
                                        touchY3 = touchY;
                                        surface2 = surface;
                                        if (surface2 != null) {
                                        }
                                        this.mDragState.closeLocked();
                                        throw th;
                                    }
                                } catch (Throwable th14) {
                                    th = th14;
                                    thumbCenterY2 = thumbCenterY3;
                                    touchY3 = touchY2;
                                    touchX3 = touchX2;
                                    touchY2 = thumbCenterX2;
                                    surface2 = surface;
                                    if (surface2 != null) {
                                    }
                                    this.mDragState.closeLocked();
                                    throw th;
                                }
                            }
                        }
                    }
                } catch (Throwable th15) {
                    th = th15;
                    touchY3 = touchY2;
                    touchX3 = touchX2;
                    z = callbackResult;
                    touchY2 = thumbCenterX2;
                    callbackResult = dragToken;
                    thumbCenterY2 = thumbCenterY3;
                    surface2 = surface;
                    if (surface2 != null) {
                    }
                    this.mDragState.closeLocked();
                    throw th;
                }
            }
        } catch (Throwable th16) {
            th = th16;
            touchY3 = touchY2;
            touchX3 = touchX2;
            z = callbackResult;
            surfaceControl = surface;
            ((IDragDropCallback) this.mCallback.get()).postPerformDrag();
            throw th;
        }
    }

    void reportDropResult(IWindow window, boolean consumed) {
        IBinder token = window.asBinder();
        ((IDragDropCallback) this.mCallback.get()).preReportDropResult(window, consumed);
        try {
            synchronized (this.mService.mWindowMap) {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mDragState == null) {
                    Slog.w("WindowManager", "Drop result given but no drag in progress");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    ((IDragDropCallback) this.mCallback.get()).postReportDropResult();
                } else if (this.mDragState.mToken == token) {
                    this.mHandler.removeMessages(0, window.asBinder());
                    if (this.mService.windowForClientLocked(null, window, false) == null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad result-reporting window ");
                        stringBuilder.append(window);
                        Slog.w("WindowManager", stringBuilder.toString());
                        WindowManagerService.resetPriorityAfterLockedSection();
                        ((IDragDropCallback) this.mCallback.get()).postReportDropResult();
                        return;
                    }
                    this.mDragState.mDragResult = consumed;
                    this.mDragState.endDragLocked();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    ((IDragDropCallback) this.mCallback.get()).postReportDropResult();
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid drop-result claim by ");
                    stringBuilder2.append(window);
                    Slog.w("WindowManager", stringBuilder2.toString());
                    throw new IllegalStateException("reportDropResult() by non-recipient");
                }
            }
        } catch (Throwable th) {
            ((IDragDropCallback) this.mCallback.get()).postReportDropResult();
        }
    }

    void cancelDragAndDrop(IBinder dragToken) {
        ((IDragDropCallback) this.mCallback.get()).preCancelDragAndDrop(dragToken);
        try {
            synchronized (this.mService.mWindowMap) {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mDragState == null) {
                    Slog.w("WindowManager", "cancelDragAndDrop() without prepareDrag()");
                    throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
                } else if (this.mDragState.mToken == dragToken) {
                    this.mDragState.mDragResult = false;
                    this.mDragState.cancelDragLocked();
                } else {
                    Slog.w("WindowManager", "cancelDragAndDrop() does not match prepareDrag()");
                    throw new IllegalStateException("cancelDragAndDrop() does not match prepareDrag()");
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            ((IDragDropCallback) this.mCallback.get()).postCancelDragAndDrop();
        } catch (Throwable th) {
            ((IDragDropCallback) this.mCallback.get()).postCancelDragAndDrop();
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0021, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:13:0x0024, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleMotionEvent(boolean keepHandling, float newX, float newY) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!dragDropActiveLocked()) {
                } else if (keepHandling) {
                    this.mDragState.notifyMoveLocked(newX, newY);
                } else {
                    this.mDragState.notifyDropLocked(newX, newY);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void dragRecipientEntered(IWindow window) {
    }

    void dragRecipientExited(IWindow window) {
    }

    void sendHandlerMessage(int what, Object arg) {
        this.mHandler.obtainMessage(what, arg).sendToTarget();
    }

    void sendTimeoutMessage(int what, Object arg) {
        this.mHandler.removeMessages(what, arg);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(what, arg), DRAG_TIMEOUT_MS);
    }

    void onDragStateClosedLocked(DragState dragState) {
        if (this.mDragState != dragState) {
            Slog.wtf("WindowManager", "Unknown drag state is closed");
        } else {
            this.mDragState = null;
        }
    }
}
