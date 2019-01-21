package com.android.server.wm;

import android.app.HwRecentTaskInfo;
import android.content.res.HwPCMultiWindowCompatibility;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.util.HwPCUtils;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;

public class TaskTapPointerEventListener implements PointerEventListener {
    public static final int INVALID_POS = -1;
    private final DisplayContent mDisplayContent;
    private final Region mHwPCtouchExcludeRegion = new Region();
    private int mPointerIconType = 1;
    private final WindowManagerService mService;
    private final Rect mTmpRect = new Rect();
    private final Region mTouchExcludeRegion = new Region();

    public TaskTapPointerEventListener(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    public void onPointerEvent(MotionEvent motionEvent, int displayId) {
        if (displayId == getDisplayId()) {
            try {
                onPointerEvent(motionEvent);
            } catch (IndexOutOfBoundsException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IndexOutOfBoundsException occured : ");
                stringBuilder.append(e);
                HwPCUtils.log("TaskTapPointerEventListener", stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:81:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onPointerEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        int y;
        if (action == 0) {
            action = (int) motionEvent.getX();
            y = (int) motionEvent.getY();
            synchronized (this) {
                if (HwPCUtils.isPcCastModeInServer() && this.mHwPCtouchExcludeRegion.contains(action, y)) {
                } else if ((motionEvent.getFlags() & 65536) != 0) {
                } else if (!this.mTouchExcludeRegion.contains(action, y)) {
                    this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, action, y);
                } else if (HwPCUtils.isPcCastModeInServer()) {
                    this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, -1, -1);
                }
            }
        } else if (action == 7) {
            action = (int) motionEvent.getX();
            y = (int) motionEvent.getY();
            Task task = this.mDisplayContent.findTaskForResizePoint(action, y);
            int iconType = 1;
            if (task != null) {
                task.getDimBounds(this.mTmpRect);
                if (!(this.mTmpRect.isEmpty() || this.mTmpRect.contains(action, y))) {
                    int i = 1014;
                    if (action < this.mTmpRect.left) {
                        if (y < this.mTmpRect.top) {
                            i = 1017;
                        } else if (y > this.mTmpRect.bottom) {
                            i = 1016;
                        }
                        iconType = i;
                    } else if (action > this.mTmpRect.right) {
                        if (y < this.mTmpRect.top) {
                            i = 1016;
                        } else if (y > this.mTmpRect.bottom) {
                            i = 1017;
                        }
                        iconType = i;
                    } else if (y < this.mTmpRect.top || y > this.mTmpRect.bottom) {
                        iconType = 1015;
                    }
                }
                if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplayContent.getDisplayId())) {
                    HwRecentTaskInfo ti = null;
                    try {
                        if (this.mService.mPCManager != null) {
                            ti = this.mService.mPCManager.getHwRecentTaskInfo(task.mTaskId);
                        }
                    } catch (RemoteException e) {
                    }
                    if (ti != null && !HwPCMultiWindowCompatibility.isResizable(ti.windowState)) {
                        iconType = 1;
                    } else if (this.mTouchExcludeRegion.contains(action, y)) {
                        iconType = 1;
                    }
                }
            }
            if (this.mPointerIconType != iconType) {
                this.mPointerIconType = iconType;
                if (this.mPointerIconType == 1) {
                    this.mService.mH.obtainMessage(55, action, y, this.mDisplayContent).sendToTarget();
                } else {
                    InputManager.getInstance().setPointerIconType(this.mPointerIconType);
                }
            }
        }
    }

    void setTouchExcludeRegion(Region newRegion) {
        synchronized (this) {
            this.mTouchExcludeRegion.set(newRegion);
        }
    }

    private int getDisplayId() {
        return this.mDisplayContent.getDisplayId();
    }

    void setHwPCTouchExcludeRegion(Region newRegion) {
        synchronized (this) {
            this.mHwPCtouchExcludeRegion.set(newRegion);
        }
    }
}
