package com.android.server.wm;

import android.content.res.HwPCMultiWindowCompatibility;
import android.freeform.HwFreeFormUtils;
import android.graphics.Rect;
import android.os.RemoteException;
import android.pc.IHwPCManager;
import android.util.HwPCUtils;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.server.HwPCFactory;

public final class HwTaskPositionerEx implements IHwTaskPositionerEx {
    private static final int BOTHWAY = 0;
    private static final int HIDE_SIDE_WINDOW_PREVIEW = 0;
    private static final int HOT_AREA_MARGIN = 48;
    private static final int LEFT = 1;
    private static final int PC_WINDOW_MIN_SIZE_DEFAULT = 640;
    private static final int PC_WINDOW_MIN_SIZE_DIVIDER = 2;
    private static final int RIGHT = 2;
    private static final int SHOW_SIDE_WINDOW_PREVIEW = 1;
    private static final Rect SIDELEFT_WINDOW_RECT_REF = new Rect(-2, -2, -2, -2);
    private static final Rect SIDERIGHT_WINDOW_RECT_REF = new Rect(-3, -3, -3, -3);
    public static final String TAG = "HwTaskPositionerEx";
    private static final int TYPE_HEIGHT = 2;
    private static final int TYPE_WIDTH = 1;
    private boolean isSplitWindowAnimateDisplayed = false;
    private final Rect leftCursorHotArea = new Rect();
    private int mPCWindowMinHeight;
    private int mPCWindowMinWidth;
    final WindowManagerService mService;
    private final Rect maximizeWindowBounds = new Rect();
    private final Rect rightCursorHotArea = new Rect();

    public HwTaskPositionerEx(WindowManagerService service) {
        this.mService = service;
    }

    public void updateFreeFormOutLine(int color) {
        if (HwFreeFormUtils.isFreeFormEnable()) {
            this.mService.mH.post(new Runnable(color) {
                /* class com.android.server.wm.$$Lambda$HwTaskPositionerEx$Zftv5fom_A29AO7VYT4LjAET0jI */
                private final /* synthetic */ int f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    HwActivityTaskManager.updateFreeFormOutLine(this.f$0);
                }
            });
        }
    }

    public int limitPCWindowSize(int length, int limitType) {
        getPCWindowSizeLimit();
        if (limitType == 1) {
            return Math.max(this.mPCWindowMinWidth, length);
        }
        if (limitType == 2) {
            return Math.max(this.mPCWindowMinHeight, length);
        }
        return PC_WINDOW_MIN_SIZE_DEFAULT;
    }

    public void processPCWindowFinishDragHitHotArea(TaskRecord taskRecord, float newX, float newY) {
        if (HwPCMultiWindowCompatibility.isResizable(taskRecord.getWindowState())) {
            try {
                IHwPCManager pcManager = HwPCUtils.getHwPCManager();
                if (pcManager != null) {
                    if (cursorHitHotArea(newX, newY, 1)) {
                        triggerSplitWindowPreviewLayer(0, 0);
                        pcManager.hwResizeTask(taskRecord.taskId, SIDELEFT_WINDOW_RECT_REF);
                        pcManager.triggerRecentTaskSplitView(2, taskRecord.taskId);
                    }
                    if (cursorHitHotArea(newX, newY, 2)) {
                        triggerSplitWindowPreviewLayer(0, 0);
                        pcManager.hwResizeTask(taskRecord.taskId, SIDERIGHT_WINDOW_RECT_REF);
                        pcManager.triggerRecentTaskSplitView(1, taskRecord.taskId);
                    }
                }
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "processPCWindowFinishDragHitHotArea RemoteException." + e);
            }
        }
    }

    public void processPCWindowDragHitHotArea(TaskRecord taskRecord, float newX, float newY) {
        if (HwPCMultiWindowCompatibility.isResizable(taskRecord.getWindowState())) {
            getPCCursorHotArea();
            if (HwPCMultiWindowCompatibility.isLayoutSplit(taskRecord.getWindowState())) {
                return;
            }
            if (cursorHitHotArea(newX, newY, 1)) {
                triggerSplitWindowPreviewLayer(1, 1);
            } else if (cursorHitHotArea(newX, newY, 2)) {
                triggerSplitWindowPreviewLayer(2, 1);
            } else {
                triggerSplitWindowPreviewLayer(0, 0);
            }
        }
    }

    private boolean cursorHitHotArea(float cursorX, float cursorY, int hitSide) {
        if (hitSide == 1) {
            return this.leftCursorHotArea.contains((int) cursorX, (int) cursorY);
        }
        if (hitSide != 2) {
            return false;
        }
        return this.rightCursorHotArea.contains((int) cursorX, (int) cursorY);
    }

    private void getPCWindowSizeLimit() {
        DefaultHwPCMultiWindowManager multiWindowManager;
        if (this.maximizeWindowBounds.isEmpty() && (multiWindowManager = getHwPCMultiWindowManager(buildAtmsEx())) != null) {
            this.maximizeWindowBounds.set(multiWindowManager.getMaximizedBounds());
            this.mPCWindowMinHeight = this.maximizeWindowBounds.height() / 2;
            this.mPCWindowMinWidth = this.maximizeWindowBounds.width() / 2;
        }
    }

    private void getPCCursorHotArea() {
        DefaultHwPCMultiWindowManager multiWindowManager;
        if (this.maximizeWindowBounds.isEmpty() && (multiWindowManager = getHwPCMultiWindowManager(buildAtmsEx())) != null) {
            this.maximizeWindowBounds.set(multiWindowManager.getMaximizedBounds());
            this.leftCursorHotArea.set(this.maximizeWindowBounds.left, this.maximizeWindowBounds.top, 48, this.maximizeWindowBounds.bottom);
            this.rightCursorHotArea.set(this.maximizeWindowBounds.right - 48, this.maximizeWindowBounds.top, this.maximizeWindowBounds.right, this.maximizeWindowBounds.bottom);
        }
    }

    private void triggerSplitWindowPreviewLayer(int side, int action) {
        try {
            IHwPCManager pcManager = HwPCUtils.getHwPCManager();
            if (pcManager == null) {
                return;
            }
            if (action == 1) {
                if (!this.isSplitWindowAnimateDisplayed) {
                    pcManager.triggerSplitWindowPreviewLayer(side, 1);
                    this.isSplitWindowAnimateDisplayed = true;
                }
            } else if (action == 0 && this.isSplitWindowAnimateDisplayed) {
                pcManager.triggerSplitWindowPreviewLayer(0, 0);
                this.isSplitWindowAnimateDisplayed = false;
            }
        } catch (RemoteException e) {
            HwPCUtils.log(TAG, "triggerSideWindowPreviewLayer RemoteException." + e);
        }
    }

    private ActivityTaskManagerServiceEx buildAtmsEx() {
        ActivityTaskManagerServiceEx atmsEx = new ActivityTaskManagerServiceEx();
        atmsEx.setActivityTaskManagerService(new ActivityTaskManagerService(this.mService.mContext));
        return atmsEx;
    }

    private DefaultHwPCMultiWindowManager getHwPCMultiWindowManager(ActivityTaskManagerServiceEx atmsEx) {
        return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCMultiWindowManager(atmsEx);
    }
}
