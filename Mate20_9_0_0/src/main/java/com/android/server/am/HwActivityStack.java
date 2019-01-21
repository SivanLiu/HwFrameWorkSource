package com.android.server.am;

import android.app.ActivityOptions;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.UiThread;
import com.android.server.wm.IntelliServiceManager;
import java.util.ArrayList;

public class HwActivityStack extends ActivityStack implements IHwActivityStack {
    static final int MAX_TASK_NUM = SystemProperties.getInt("ro.config.pc_mode_win_num", 8);
    private static final boolean mIsHwNaviBar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    boolean mHiddenFromHome = false;
    private boolean mStackVisible = true;
    private boolean mStackVisibleBeforeHidden = false;

    public HwActivityStack(ActivityDisplay display, int stackId, ActivityStackSupervisor supervisor, int windowingMode, int activityType, boolean onTop) {
        super(display, stackId, supervisor, windowingMode, activityType, onTop);
    }

    public int getInvalidFlag(int changes, Configuration newConfig, Configuration naviConfig) {
        if (newConfig == null || naviConfig == null) {
            return changes;
        }
        if (mIsHwNaviBar) {
            int newChanges = naviConfig.diff(newConfig);
            if ((newChanges & 1280) == 0) {
                changes &= -1281;
            } else if ((newChanges & 128) != 0) {
                if (changes == 1280 || changes == 1024) {
                    changes &= -1025;
                }
                changes &= -257;
            }
        }
        return changes;
    }

    void moveHomeStackTaskToTop() {
        super.moveHomeStackTaskToTop();
        this.mService.checkIfScreenStatusRequestAndSendBroadcast();
    }

    public boolean isSplitActivity(Intent intent) {
        return (intent == null || (intent.getHwFlags() & 4) == 0) ? false : true;
    }

    public void resumeCustomActivity(ActivityRecord next) {
        if (next != null) {
            this.mService.customActivityResuming(next.packageName);
        }
    }

    /* JADX WARNING: Missing block: B:13:0x004c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void makeStackVisible(boolean visible) {
        synchronized (this.mService) {
            this.mStackVisible = visible;
            if (HwVRUtils.isVRDynamicStack(getStackId())) {
            } else if (visible) {
                this.mHiddenFromHome = false;
                if (this.mTaskHistory.size() > 0) {
                    this.mService.getHwTaskChangeController().notifyTaskMovedToFront(((TaskRecord) this.mTaskHistory.get(0)).taskId);
                }
            } else {
                this.mService.getHwTaskChangeController().notifyTaskMovedToFront(-1);
                this.mService.mHwAMSEx.updateUsageStatsForPCMode(getTopActivity(), visible, this.mService.mUsageStatsService);
            }
        }
    }

    protected void resetOtherStacksVisible(boolean visible) {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcDynamicStack(this.mStackId) && hasFullscreenTaskInPad()) {
            makeStackVisibleInPad(visible);
        }
    }

    private boolean hasFullscreenTaskInPad() {
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            int WindowState = ((TaskRecord) this.mTaskHistory.get(i)).getWindowState();
            if (HwPCMultiWindowCompatibility.isLayoutFullscreen(WindowState) || HwPCMultiWindowCompatibility.isLayoutMaximized(WindowState)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInCallActivityStack() {
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            ActivityRecord topActivity = ((TaskRecord) this.mTaskHistory.get(i)).getTopActivity();
            if (topActivity != null) {
                ActivityManagerService activityManagerService = this.mService;
                if (ActivityManagerService.isInCallActivity(topActivity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean getStackVisibleBeforeHidden() {
        return this.mStackVisibleBeforeHidden;
    }

    private void setStackVisibleBeforeHidden(boolean visible) {
        this.mStackVisibleBeforeHidden = visible;
    }

    private void makeStackVisibleInPad(boolean visible) {
        ActivityDisplay activityDisplay = (ActivityDisplay) this.mStackSupervisor.mActivityDisplays.get(this.mDisplayId);
        if (activityDisplay == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Display with displayId=");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append(" not found.");
            HwPCUtils.log("ActivityManager", stringBuilder.toString());
            return;
        }
        for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            if (activityDisplay.getChildAt(stackNdx) instanceof HwActivityStack) {
                HwActivityStack stack = (HwActivityStack) activityDisplay.getChildAt(stackNdx);
                if (stack.mStackId != this.mStackId) {
                    StringBuilder stringBuilder2;
                    if (!visible || (stack.getStackVisibleBeforeHidden() && !stack.isInCallActivityStack())) {
                        if (!visible && stack.mStackVisible) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("makeStackVisibleInPad stack=");
                            stringBuilder2.append(stack);
                            stringBuilder2.append(" make invisible because the top activity is fullscreen ,mStackVisibleBeforeHidden=");
                            stringBuilder2.append(stack.getStackVisibleBeforeHidden());
                            HwPCUtils.log("ActivityManager", stringBuilder2.toString());
                            stack.setStackVisibleBeforeHidden(true);
                        }
                        stack.makeStackVisible(visible);
                        if (visible) {
                            stack.setStackVisibleBeforeHidden(false);
                        }
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("makeStackVisibleInPad stack=");
                        stringBuilder2.append(stack);
                        stringBuilder2.append(" Skipping: is invisible before launch fullscreen activity or this stack contains InCallUI activity ,mStackVisibleBeforeHidden=");
                        stringBuilder2.append(stack.getStackVisibleBeforeHidden());
                        HwPCUtils.log("ActivityManager", stringBuilder2.toString());
                    }
                }
            }
        }
    }

    protected boolean moveTaskToBackLocked(int taskId) {
        if ((!HwVRUtils.isVRMode() || HwFrameworkFactory.getVRSystemServiceManager().isVirtualScreenMode()) && !HwPCUtils.isExtDynamicStack(this.mStackId)) {
            return super.moveTaskToBackLocked(taskId);
        }
        makeStackVisible(false);
        if (HwPCUtils.enabledInPad() && hasFullscreenTaskInPad()) {
            makeStackVisibleInPad(true);
        }
        setStackVisibleBeforeHidden(false);
        ensureActivitiesVisibleLocked(topRunningActivityLocked(), 0, false);
        adjustFocusToNextFocusableStack("minTask");
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return true;
    }

    protected void moveToFront(String reason, TaskRecord task) {
        if (isAttached()) {
            if (HwPCUtils.isExtDynamicStack(this.mStackId)) {
                if (HwPCUtils.enabledInPad() && hasFullscreenTaskInPad()) {
                    makeStackVisibleInPad(false);
                }
                makeStackVisible(true);
            }
            super.moveToFront(reason, task);
            minimalLRUTaskIfNeed();
        }
    }

    protected void setKeepPortraitFR() {
        IntelliServiceManager.getInstance(this.mService.mContext).setKeepPortrait(true);
    }

    protected boolean shouldBeVisible(ActivityRecord starting) {
        if (!HwPCUtils.isExtDynamicStack(this.mStackId) || (this.mStackVisible && !this.mTaskHistory.isEmpty())) {
            return super.shouldBeVisible(starting);
        }
        return false;
    }

    protected void moveTaskToFrontLocked(TaskRecord tr, boolean noAnimation, ActivityOptions options, AppTimeTracker timeTracker, String reason) {
        if (HwPCUtils.isExtDynamicStack(this.mStackId)) {
            makeStackVisible(true);
        }
        super.moveTaskToFrontLocked(tr, noAnimation, options, timeTracker, reason);
        minimalLRUTaskIfNeed();
    }

    private void minimalLRUTaskIfNeed() {
        if (HwPCUtils.isExtDynamicStack(this.mStackId)) {
            int visibleNum = 0;
            TaskRecord lastVisibleTask = null;
            int N = getDisplay().getChildCount();
            for (int i = 0; i < N; i++) {
                ActivityStack stack = getDisplay().getChildAt(i);
                String title = null;
                if (stack.shouldBeVisible(null)) {
                    if (lastVisibleTask == null && stack.topTask() != null) {
                        lastVisibleTask = stack.topTask();
                    }
                    if (lastVisibleTask != null) {
                        visibleNum++;
                        if (visibleNum > MAX_TASK_NUM && lastVisibleTask.mStack != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("max task num, minimial the task: ");
                            stringBuilder.append(lastVisibleTask.taskId);
                            HwPCUtils.log("ActivityManager", stringBuilder.toString());
                            this.mService.moveTaskBackwards(lastVisibleTask.taskId);
                            final Context context = HwPCUtils.getDisplayContext(this.mService.mContext, lastVisibleTask.mStack.mDisplayId);
                            if (context != null) {
                                ActivityRecord ar = lastVisibleTask.getRootActivity();
                                if (!(ar == null || ar.info == null)) {
                                    title = ar.info.loadLabel(context.getPackageManager()).toString();
                                }
                                if (!TextUtils.isEmpty(title)) {
                                    UiThread.getHandler().post(new Runnable() {
                                        public void run() {
                                            Toast.makeText(context, context.getString(33685972, new Object[]{title}), 0).show();
                                        }
                                    });
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                    }
                    return;
                }
            }
        }
    }

    public boolean isVisibleLocked(String packageName, boolean deepRecur) {
        String str = packageName;
        boolean z = false;
        if (str == null || packageName.isEmpty()) {
            return false;
        }
        TaskRecord lastTask = null;
        HwTaskRecord hwTask = null;
        int size = this.mTaskHistory.size();
        if (size <= 0) {
            return false;
        }
        int maxTaskIdx = deepRecur ? 0 : size - 1;
        int taskNdx = size - 1;
        while (taskNdx >= maxTaskIdx) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx);
            if (task instanceof HwTaskRecord) {
                hwTask = (HwTaskRecord) task;
            }
            if (hwTask != null) {
                ArrayList<ActivityRecord> activities = hwTask.getActivities();
                if (activities != null && activities.size() > 0) {
                    int numActivities = activities.size();
                    TaskRecord lastTask2 = lastTask;
                    int activityNdx = z;
                    while (true) {
                        int activityNdx2 = activityNdx;
                        if (activityNdx2 >= numActivities) {
                            lastTask = lastTask2;
                            break;
                        }
                        try {
                            ActivityRecord lastTask3 = (ActivityRecord) activities.get(activityNdx2);
                            if (lastTask3 != null && (str.equals(lastTask3.packageName) || lastTask3.getTask() == lastTask2)) {
                                if (lastTask3.visible || lastTask3.visibleIgnoringKeyguard) {
                                    return true;
                                }
                                lastTask2 = lastTask3.getTask();
                            }
                        } catch (IndexOutOfBoundsException e) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("IndexOutOfBoundsException: Index: +");
                            stringBuilder.append(activityNdx2);
                            stringBuilder.append(", Size: ");
                            stringBuilder.append(activities.size());
                            Slog.e("ActivityManager", stringBuilder.toString());
                        }
                        activityNdx = activityNdx2 + 1;
                    }
                }
            }
            taskNdx--;
            z = false;
        }
        return false;
    }
}
