package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import com.android.server.magicwin.HwMagicWindowService;
import java.util.ArrayList;

public class HwMagicModeA1An extends HwMagicModeBase {
    private static final String TAG = "HwMagicModeA1An";

    public HwMagicModeA1An(HwMagicWindowService service, Context context) {
        super(service, context);
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public void setActivityBoundByMode(ArrayList<ActivityRecord> activities, String pkgName) {
        Rect bounds;
        ActivityRecord homeActivity = getHomePageActivityRecord(activities);
        if (homeActivity == null || homeActivity == homeActivity.getActivityStack().getTopActivity()) {
            super.setActivityBoundByMode(activities, pkgName);
            return;
        }
        boolean hasFullscreenActivity = false;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord ar = activities.get(i);
            String pkg = this.mPolicy.getRealPkgName(ar);
            if (ar == homeActivity) {
                bounds = this.mService.getBounds(1, pkg);
            } else {
                bounds = this.mService.getBounds(2, pkg);
            }
            if (ar.isTopRunningActivity() && this.mPolicy.isKeyguardLockedAndOccluded()) {
                bounds = this.mService.getBounds(3, pkg);
            }
            ar.setBounds(bounds);
            hasFullscreenActivity = setDefaultFullscreenBounds(ar, hasFullscreenActivity);
        }
    }

    @Override // com.android.server.wm.HwMagicModeBase
    public boolean isMoveActivityToMaster(HwActivityRecord focus, HwActivityRecord next, int targetPosition) {
        if (!isSkippingMoveToMaster(focus, next) && targetPosition == 2 && this.mService.isMiddle(focus)) {
            return true;
        }
        return false;
    }
}
