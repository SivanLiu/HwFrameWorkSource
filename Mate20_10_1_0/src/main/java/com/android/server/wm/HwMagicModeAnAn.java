package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import com.android.server.magicwin.HwMagicWindowService;
import java.util.ArrayList;

public class HwMagicModeAnAn extends HwMagicModeBase {
    private static final String TAG = "HwMagicModeAnAn";

    public HwMagicModeAnAn(HwMagicWindowService service, Context context) {
        super(service, context);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public boolean shouldEnterMagicWinForTah(HwActivityRecord focus, HwActivityRecord next) {
        return !this.mPolicy.isDefaultFullscreenActivity(next) && this.mService.isHomePage(this.mPolicy.getPackageName(focus), this.mPolicy.getClassName(focus)) && !this.mPolicy.isSpecTransActivityPreDefined(next);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void setLeftTopActivityToPause(HwActivityRecord focus) {
        ActivityRecord masterTop = this.mPolicy.getActvityByPosition(focus, 1, 0);
        if (masterTop != null) {
            this.mPolicy.setMagicWindowToPause(masterTop);
        }
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public boolean isMoveActivityToMaster(HwActivityRecord focus, HwActivityRecord next, int targetPosition) {
        if (isSkippingMoveToMaster(focus, next) || !next.fullscreen || targetPosition != 2) {
            return false;
        }
        if (this.mService.isMiddle(focus) || this.mService.isSlave(focus)) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void adjustWindowForFinish(HwActivityRecord activity, String finishReason) {
        if (!isNonFullScreen(activity) || !this.mService.isMaster(activity) || this.mPolicy.getActvityByPosition(activity, 1, 1) == null) {
            adjustWindowForDoubleWindows(activity, finishReason);
        }
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void setActivityBoundByMode(ArrayList<ActivityRecord> activities, String pkgName) {
        Rect bounds;
        setActivityBoundMainRelatedIfNeed(activities, pkgName);
        ActivityRecord homeActivity = getHomePageActivityRecord(activities);
        if (homeActivity == null || homeActivity == homeActivity.getActivityStack().getTopActivity()) {
            super.setActivityBoundByMode(activities, pkgName);
            return;
        }
        boolean hasFullscreenActivity = false;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord ar = activities.get(i);
            String pkg = this.mPolicy.getRealPkgName(ar);
            boolean isTop = ar == ar.getTaskRecord().getTopActivity();
            if (isTop) {
                bounds = this.mService.getBounds(2, pkg);
            } else {
                bounds = this.mService.getBounds(1, pkg);
            }
            if (isTop && this.mPolicy.isKeyguardLockedAndOccluded()) {
                bounds = this.mService.getBounds(3, pkg);
            }
            ar.setBounds(bounds);
            hasFullscreenActivity = setDefaultFullscreenBounds(ar, hasFullscreenActivity);
        }
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public int getTargetWindowPosition(HwActivityRecord focus, HwActivityRecord next) {
        if (isNonFullScreen(next)) {
            return this.mService.getBoundsPosition(focus.getRequestedOverrideBounds());
        }
        return super.getTargetWindowPosition(focus, next);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void moveOtherActivities(ActivityRecord movedActivity, int currentPosition) {
        ActivityRecord needMoveActivity;
        if ((movedActivity instanceof HwActivityRecord) && !isNonFullScreen((HwActivityRecord) movedActivity)) {
            return;
        }
        if (!(currentPosition == 1 && this.mPolicy.getActvityByPosition(movedActivity, 1, 1) == null) && (needMoveActivity = this.mPolicy.getActvityByPosition(movedActivity, currentPosition, 0)) != null && !this.mPolicy.isRelatedActivity(needMoveActivity) && !this.mPolicy.isMainActivity(needMoveActivity)) {
            int targetPosition = currentPosition == 1 ? 2 : 1;
            if (targetPosition == 1) {
                needMoveActivity.getTaskRecord().moveActivityToFrontLocked(needMoveActivity);
                needMoveActivity.getTaskRecord().moveActivityToFrontLocked(movedActivity);
            }
            this.mPolicy.moveWindow(needMoveActivity, targetPosition);
        }
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void finishRightAfterFinishingLeft(HwActivityRecord finishActivity) {
        if (!isNonFullScreen(finishActivity) || this.mPolicy.getActvityByPosition(finishActivity, 1, 1) == null) {
            super.finishRightAfterFinishingLeft(finishActivity);
        }
    }
}
