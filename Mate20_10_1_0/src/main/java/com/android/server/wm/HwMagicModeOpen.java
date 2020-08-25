package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.HwMwUtils;
import android.util.Slog;
import com.android.server.magicwin.HwMagicWindowService;
import java.util.ArrayList;
import java.util.List;

public class HwMagicModeOpen extends HwMagicModeBase {
    private static final int LEFT_BACK_LOOP_MAX_COUNT = 10;
    private static final String TAG = "HwMagicModeOpen";

    public HwMagicModeOpen(HwMagicWindowService service, Context context) {
        super(service, context);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void finishRightAfterFinishingLeft(HwActivityRecord finishActivity) {
        ActivityRecord needFinishActivity = getRightNeedFinishActivity(finishActivity);
        Slog.i(TAG, "finishRightAfterFinishingLeft needFinishActivity=" + needFinishActivity);
        int exitCount = 0;
        while (needFinishActivity != null && exitCount < 10) {
            ActivityRecord next = this.mPolicy.getActvityByPosition(finishActivity, 2, 0);
            if (next != null) {
                if (needFinishActivity != next) {
                    exitCount++;
                    if (!this.mPolicy.isRelatedInSlave(next)) {
                        ActivityStack activityStack = finishActivity.getActivityStack();
                        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
                        activityStack.finishActivityLocked(next, 0, (Intent) null, HwMagicWinAmsPolicy.MAGIC_WINDOW_FINISH_EVENT, true, false);
                    }
                } else if (!this.mPolicy.isRelatedInSlave(needFinishActivity)) {
                    ActivityStack activityStack2 = finishActivity.getActivityStack();
                    HwMagicWinAmsPolicy hwMagicWinAmsPolicy2 = this.mPolicy;
                    activityStack2.finishActivityLocked(needFinishActivity, 0, (Intent) null, HwMagicWinAmsPolicy.MAGIC_WINDOW_FINISH_EVENT, true, false);
                    return;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private HwActivityRecord getRightNeedFinishActivity(HwActivityRecord finishActivity) {
        int hashValue = System.identityHashCode(finishActivity);
        List<TaskRecord> taskHistory = finishActivity.getActivityStack().getAllTasks();
        for (int taskIndex = taskHistory.size() - 1; taskIndex >= 0; taskIndex--) {
            List<ActivityRecord> activityRecords = taskHistory.get(taskIndex).mActivities;
            for (int activityIndex = activityRecords.size() - 1; activityIndex >= 0; activityIndex--) {
                HwActivityRecord activity = (HwActivityRecord) activityRecords.get(activityIndex);
                if (activity != null && !activity.finishing && this.mService.getBoundsPosition(activity.getRequestedOverrideBounds()) == 2 && activity.getLastActivityHash() == hashValue) {
                    return activity;
                }
            }
        }
        return null;
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public boolean shouldEnterMagicWinForTah(HwActivityRecord focus, HwActivityRecord next) {
        super.setOrigActivityToken(focus);
        ActivityRecord origActivity = ActivityRecord.forTokenLocked(this.mOrigActivityToken);
        if (this.mPolicy.isDefaultFullscreenActivity(next) || origActivity == null || origActivity.getStackId() != next.getStackId()) {
            return false;
        }
        return isSpecPairActivities(origActivity, next);
    }

    private boolean isSpecPairActivities(ActivityRecord focusRecord, ActivityRecord targetRecord) {
        String focusPkg = this.mPolicy.getPackageName(focusRecord);
        String focusCls = this.mPolicy.getClassName(focusRecord);
        String targetName = this.mPolicy.getClassName(targetRecord);
        if (focusPkg == null || focusPkg.equals(this.mPolicy.getPackageName(targetRecord))) {
            return this.mService.getConfig().isSpecPairActivities(focusPkg, focusCls, targetName);
        }
        return false;
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void overrideIntent(HwActivityRecord focus, HwActivityRecord next, boolean isNewTask) {
        HwActivityRecord origActivity = focus;
        super.setOrigActivityToken(focus);
        if (focus.inHwMagicWindowingMode()) {
            if (this.mPolicy.isSpecTransActivityPreDefined(next) && !this.mPolicy.mMagicWinSplitMng.isPkgSpliteScreenMode(next, true)) {
                next.setBounds(this.mService.getBounds(2, this.mPolicy.getRealPkgName(next)));
                return;
            } else if (this.mOrigActivityToken != null) {
                origActivity = ActivityRecord.forToken(this.mOrigActivityToken);
                if (origActivity == null) {
                    Slog.w(TAG, "overrideIntent origActivity is null");
                    super.overrideIntent(focus, next, isNewTask);
                    return;
                }
                next.setLastActivityHash(System.identityHashCode(origActivity));
            }
        }
        if (origActivity.getStackId() != next.getStackId()) {
            origActivity = focus;
        }
        super.overrideIntent(origActivity, next, isNewTask);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public int getTargetWindowPosition(HwActivityRecord focus, HwActivityRecord next) {
        if (this.mPolicy.isRelatedActivity(next)) {
            return 2;
        }
        if (this.mPolicy.isMainActivity(next)) {
            return 1;
        }
        if (this.mService.isHomePage(this.mPolicy.getPackageName((ActivityRecord) next), this.mPolicy.getClassName((ActivityRecord) next))) {
            return 3;
        }
        if (!this.mService.isMiddle(focus)) {
            return super.getTargetWindowPosition(focus, next);
        }
        if (this.mPolicy.isDefaultFullscreenActivity(next)) {
            return 5;
        }
        if (isSpecPairActivities(focus, next)) {
            return 2;
        }
        return 3;
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public boolean isMoveActivityToMaster(HwActivityRecord focus, HwActivityRecord next, int targetPosition) {
        if (isSkippingMoveToMaster(focus, next) || targetPosition != 2) {
            return false;
        }
        if ((this.mService.isMiddle(focus) || this.mService.isSlave(focus)) && isSpecPairActivities(focus, next)) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void adjustWindowForFinish(HwActivityRecord activity, String finishReason) {
        if (!this.mService.isSlave(activity) || this.mPolicy.getActvityByPosition(activity, 2, 1) != null || !setActivityBoundAfterFinishing(activity)) {
            adjustWindowForDoubleWindows(activity, finishReason);
        }
    }

    private boolean setActivityBoundAfterFinishing(HwActivityRecord activity) {
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(activity.getActivityStack());
        tempActivityList.remove(activity);
        return setActivityBoundForOpenEx(tempActivityList, this.mPolicy.getRealPkgName(activity), true);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void setActivityBoundByMode(ArrayList<ActivityRecord> activities, String packageName) {
        ActivityRecord topActivity;
        setActivityBoundMainRelatedIfNeed(activities, packageName);
        boolean isBackToMw = setActivityBoundForOpenEx(activities, packageName, false);
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && isBackToMw && (topActivity = activities.get(0)) != null && (topActivity instanceof HwActivityRecord) && !((HwActivityRecord) topActivity).mIsFullScreenVideoInLandscape) {
            topActivity.forceNewConfig = (topActivity.info.configChanges & 3328) != 3328;
            topActivity.ensureActivityConfiguration(0, false);
        }
    }

    private boolean setActivityBoundForOpenEx(ArrayList<ActivityRecord> activities, String packageName, boolean isFromFinish) {
        if (activities.size() <= 1) {
            return false;
        }
        activities.get(0);
        int middleIndex = 0;
        int rightIndex = 0;
        int leftIndex = 0;
        for (int activityIndex = 0; activityIndex < activities.size() - 1; activityIndex++) {
            ActivityRecord prevActivity = activities.get(activityIndex + 1);
            ActivityRecord current = activities.get(activityIndex);
            if (!current.finishing && !prevActivity.finishing) {
                if (this.mPolicy.isDefaultFullscreenActivity(current)) {
                    middleIndex = activityIndex + 1;
                    leftIndex = 0;
                    rightIndex = 0;
                } else if (isSpecPairActivities(prevActivity, current) && leftIndex == 0) {
                    leftIndex = activityIndex + 1;
                    rightIndex = activityIndex;
                }
            }
        }
        if (rightIndex == 0 && leftIndex == 0) {
            Slog.i(TAG, "setActivityBoundForOpenEx Didn't find the pair activit");
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && !isFromFinish) {
                activities.get(0).getActivityStack().setWindowingMode(1);
            }
            if (!HwMwUtils.IS_TABLET || middleIndex == 0) {
                return false;
            }
        }
        setActivityBoundForOpenExInner(activities, packageName, leftIndex, middleIndex, isFromFinish);
        return true;
    }

    private void setActivityBoundForOpenExInner(ArrayList<ActivityRecord> activities, String packageName, int leftIndex, int middleIndex, boolean isFromFinish) {
        Rect middleBounds = this.mService.getBounds(3, packageName);
        Rect fullbounds = this.mService.getBounds(5, packageName);
        Rect masterBounds = this.mService.getBounds(1, packageName);
        Rect slaveBounds = this.mService.getBounds(2, packageName);
        ActivityStack leftActivityStack = activities.get(leftIndex).getActivityStack();
        if (isFromFinish || leftActivityStack.getWindowingMode() != 1) {
            leftActivityStack.setWindowingMode(103);
        } else {
            leftActivityStack.setWindowingMode(103, false, false, false, true, false);
        }
        leftActivityStack.resize((Rect) null, (Rect) null, (Rect) null);
        for (int index = 0; index < activities.size(); index++) {
            ActivityRecord activity = activities.get(index);
            if (!activity.finishing) {
                if (index < middleIndex) {
                    activity.setBounds(HwMwUtils.IS_FOLD_SCREEN_DEVICE ? middleBounds : fullbounds);
                } else if (index < leftIndex) {
                    activity.setBounds(slaveBounds);
                } else if (index == leftIndex) {
                    activity.setBounds(masterBounds);
                } else {
                    if (HwMwUtils.IS_TABLET) {
                        activity.setBounds(middleBounds);
                    }
                    Slog.i(TAG, "setActivityBoundForOpenEx keep bound");
                }
            }
        }
    }
}
