package com.android.server.wm;

import android.app.HwRecentTaskInfo;
import android.content.res.HwPCMultiWindowCompatibility;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.pc.IHwPCManager;
import android.util.HwPCUtils;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;

public class TaskTapPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    public static final int INVALID_POS = -1;
    private static final int INVALID_TASK_ID = -1;
    private final DisplayContent mDisplayContent;
    private final Region mHwPCtouchExcludeRegion = new Region();
    private int mLastfreeformTaskId = -2;
    private int mPointerIconType = 1;
    private final WindowManagerService mService;
    private final Rect mTmpRect = new Rect();
    private final Region mTouchExcludeRegion = new Region();

    public TaskTapPointerEventListener(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    /* JADX INFO: finally extract failed */
    /* JADX WARNING: Code restructure failed: missing block: B:100:?, code lost:
        com.android.server.wm.WindowManagerService.boostPriorityForLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:101:0x017f, code lost:
        if (r10.mDisplayContent == null) goto L_0x01bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x0181, code lost:
        r6 = r10.mDisplayContent.getStack(5, 1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:103:0x0188, code lost:
        if (r6 == null) goto L_0x01bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x018a, code lost:
        r5 = r6.taskIdFromPoint(r0, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x018f, code lost:
        if (r5 != -1) goto L_0x01a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x0191, code lost:
        r4 = r10.mDisplayContent.findTaskForResizePoint(r0, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x0197, code lost:
        if (r4 == null) goto L_0x01a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x019d, code lost:
        if (r4.inFreeformWindowingMode() == false) goto L_0x01a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x019f, code lost:
        r5 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:0x01a3, code lost:
        if ((r10.mLastfreeformTaskId * r5) < 0) goto L_0x01aa;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x01a8, code lost:
        if (r10.mLastfreeformTaskId != -2) goto L_0x01bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x01aa, code lost:
        if (r5 >= 0) goto L_0x01b0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:116:0x01ac, code lost:
        updateFreeForm(r0, r2, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:0x01b0, code lost:
        r10.mService.mH.post(com.android.server.wm.$$Lambda$TaskTapPointerEventListener$H5E5vP2wnfNbGDfOb4ylBrufjlg.INSTANCE);
        r10.mLastfreeformTaskId = r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:0x01bb, code lost:
        monitor-exit(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x01bc, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:120:0x01c3, code lost:
        if (android.util.HwPCUtils.isInWindowsCastMode() == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x01c5, code lost:
        r3 = r10.mDisplayContent;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:122:0x01c7, code lost:
        if (r3 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:124:0x01cd, code lost:
        if (r3.getDisplayId() != 0) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x01cf, code lost:
        r3 = android.view.InputDevice.getDevice(r11.getDeviceId());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:0x01d8, code lost:
        if (r3 == null) goto L_0x0215;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x01e0, code lost:
        if (r3.supportsSource(4098) == false) goto L_0x0215;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:0x01e2, code lost:
        r10.mService.setFocusedDisplay(0, false, "handleTapOutsideTaskXY");
        com.huawei.android.inputmethod.HwInputMethodManager.restartInputMethodForMultiDisplay();
        android.util.HwPCUtils.log("onPointerEvent", "onOperateOnPhone.");
        r10.mService.getWindowManagerServiceEx().onOperateOnPhone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:131:?, code lost:
        r4 = android.util.HwPCUtils.getHwPCManager();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x0200, code lost:
        if (r4 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:134:0x0206, code lost:
        if (r4.isScreenPowerOn() != false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:0x0208, code lost:
        r4.setScreenPower(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x020d, code lost:
        android.util.HwPCUtils.log("onPointerEvent", "getDesiredScreenPolicyLocked RemoteException");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0215, code lost:
        r10.mService.setFocusedDisplay(android.util.HwPCUtils.getWindowsCastDisplayId(), false, "handleTapOutsideTaskXY");
        com.huawei.android.inputmethod.HwInputMethodManager.restartInputMethodForMultiDisplay();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:?, code lost:
        r1 = android.util.HwPCUtils.getHwPCManager();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x0227, code lost:
        if (r1 == null) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x022d, code lost:
        if (r1.isScreenPowerOn() == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x022f, code lost:
        r1.setScreenPower(false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x0234, code lost:
        android.util.HwPCUtils.log("onPointerEvent", "getDesiredScreenPolicyLocked RemoteException");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:147:0x023d, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:149:0x023f, code lost:
        com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:150:0x0242, code lost:
        throw r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:164:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:165:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:168:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:169:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:172:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x0172, code lost:
        r3 = r10.mService.getGlobalLock();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:0x0178, code lost:
        monitor-enter(r3);
     */
    public void onPointerEvent(MotionEvent motionEvent) {
        Task task;
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            synchronized (this) {
                if (HwPCUtils.isPcCastModeInServer() && this.mHwPCtouchExcludeRegion.contains(x, y)) {
                    return;
                }
                if ((motionEvent.getFlags() & 65536) == 0) {
                    if (!this.mTouchExcludeRegion.contains(x, y)) {
                        this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, x, y);
                    } else if (HwPCUtils.isPcCastModeInServer() || HwPCUtils.isInWindowsCastMode()) {
                        this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, -1, -1);
                    }
                }
            }
        } else if (actionMasked == 7 || actionMasked == 9) {
            int x2 = (int) motionEvent.getX();
            int y2 = (int) motionEvent.getY();
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    task = this.mDisplayContent.findTaskForResizePoint(x2, y2);
                    if (task != null) {
                        task.getDimBounds(this.mTmpRect);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            int iconType = 1;
            if (task != null) {
                if (!this.mTmpRect.isEmpty() && !this.mTmpRect.contains(x2, y2)) {
                    int i = 1014;
                    if (x2 < this.mTmpRect.left) {
                        if (y2 < this.mTmpRect.top) {
                            i = 1017;
                        } else if (y2 > this.mTmpRect.bottom) {
                            i = 1016;
                        }
                        iconType = i;
                    } else if (x2 > this.mTmpRect.right) {
                        if (y2 < this.mTmpRect.top) {
                            i = 1016;
                        } else if (y2 > this.mTmpRect.bottom) {
                            i = 1017;
                        }
                        iconType = i;
                    } else if (y2 < this.mTmpRect.top || y2 > this.mTmpRect.bottom) {
                        iconType = 1015;
                    }
                }
                if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplayContent.getDisplayId())) {
                    HwRecentTaskInfo ti = null;
                    try {
                        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
                        if (pcManager != null) {
                            ti = pcManager.getHwRecentTaskInfo(task.mTaskId);
                        }
                    } catch (RemoteException e) {
                    }
                    if (ti != null && !HwPCMultiWindowCompatibility.isResizable(ti.windowState)) {
                        iconType = 1;
                    } else if (this.mTouchExcludeRegion.contains(x2, y2)) {
                        iconType = 1;
                    }
                }
            }
            if (this.mPointerIconType != iconType) {
                this.mPointerIconType = iconType;
                if (this.mPointerIconType == 1) {
                    this.mService.mH.removeMessages(55);
                    this.mService.mH.obtainMessage(55, x2, y2, this.mDisplayContent).sendToTarget();
                    return;
                }
                InputManager.getInstance().setPointerIconType(this.mPointerIconType);
            }
        } else if (actionMasked == 10) {
            int x3 = (int) motionEvent.getX();
            int y3 = (int) motionEvent.getY();
            if (this.mPointerIconType != 1) {
                this.mPointerIconType = 1;
                this.mService.mH.removeMessages(55);
                this.mService.mH.obtainMessage(55, x3, y3, this.mDisplayContent).sendToTarget();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setTouchExcludeRegion(Region newRegion) {
        synchronized (this) {
            this.mTouchExcludeRegion.set(newRegion);
        }
    }

    /* access modifiers changed from: package-private */
    public void setHwPCTouchExcludeRegion(Region newRegion) {
        synchronized (this) {
            this.mHwPCtouchExcludeRegion.set(newRegion);
        }
    }

    private void updateFreeForm(int x, int y, int freeformTaskId) {
        boolean needUpdate = false;
        if (this.mService.getInputMethodWindowLw() != null) {
            Region region = new Region();
            if (this.mService.getInputMethodWindowLw() instanceof WindowState) {
                this.mService.getInputMethodWindowLw().getTouchableRegion(region);
            }
            if (!region.contains(x, y)) {
                needUpdate = true;
            }
        } else {
            needUpdate = true;
        }
        if (needUpdate) {
            this.mService.mH.post($$Lambda$TaskTapPointerEventListener$uXH_olFfBoCNv8x_8EAPv6meu8E.INSTANCE);
            this.mLastfreeformTaskId = freeformTaskId;
        }
    }
}
