package com.android.server.wm;

import android.graphics.Rect;
import android.util.Flog;
import android.util.Slog;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class HwSplitScreenCombination {
    private static final String TAG = "HwSplitScreenCombination";
    int mDisplayId = -1;
    private ActivityStack mHwSplitScreenPrimaryStack = null;
    private ActivityStack mHwSplitScreenSecondaryStack = null;
    int mSplitRatio = 0;

    public boolean isSplitScreenCombined() {
        return (this.mHwSplitScreenPrimaryStack == null || this.mHwSplitScreenSecondaryStack == null) ? false : true;
    }

    public boolean hasHwSplitScreenPrimaryStack() {
        return this.mHwSplitScreenPrimaryStack != null;
    }

    public boolean hasHwSplitScreenSecondaryStack() {
        return this.mHwSplitScreenSecondaryStack != null;
    }

    public boolean hasHwSplitScreenStack(int stackId) {
        ActivityStack activityStack;
        ActivityStack activityStack2 = this.mHwSplitScreenPrimaryStack;
        return (activityStack2 != null && activityStack2.mStackId == stackId) || ((activityStack = this.mHwSplitScreenSecondaryStack) != null && activityStack.mStackId == stackId);
    }

    public boolean hasHwSplitScreenStack(ActivityStack stack) {
        if (stack == null) {
            return false;
        }
        if (this.mHwSplitScreenPrimaryStack == stack || this.mHwSplitScreenSecondaryStack == stack) {
            return true;
        }
        return false;
    }

    public boolean hasStackAndMatchWindowMode(ActivityStack stack) {
        if (stack == null) {
            return false;
        }
        if ((this.mHwSplitScreenPrimaryStack != stack || !stack.inHwSplitScreenPrimaryWindowingMode()) && (this.mHwSplitScreenSecondaryStack != stack || !stack.inHwSplitScreenSecondaryWindowingMode())) {
            return false;
        }
        return true;
    }

    public void addStackReferenceIfNeeded(ActivityStack stack) {
        if (stack != null && stack.inHwMultiStackWindowingMode()) {
            if (stack.inHwSplitScreenPrimaryWindowingMode()) {
                this.mHwSplitScreenPrimaryStack = stack;
                this.mDisplayId = stack.mDisplayId;
            } else if (stack.inHwSplitScreenSecondaryWindowingMode()) {
                this.mHwSplitScreenSecondaryStack = stack;
                this.mDisplayId = stack.mDisplayId;
            }
        }
    }

    public void removeStackReferenceIfNeeded(ActivityStack stack) {
        if (stack != null) {
            if (this.mHwSplitScreenPrimaryStack == stack) {
                this.mHwSplitScreenPrimaryStack = null;
                ActivityStack stackToReparent = this.mHwSplitScreenSecondaryStack;
                this.mHwSplitScreenSecondaryStack = null;
                if (stackToReparent != null) {
                    if (stackToReparent.getTaskStack() != null) {
                        stackToReparent.getTaskStack().clearAdjustedBounds();
                    }
                    if (stack.inHwFreeFormWindowingMode()) {
                        HwMultiWindowManager.exitHwMultiStack(stackToReparent, false, false, false, true, false);
                    } else {
                        HwMultiWindowManager.exitHwMultiStack(stackToReparent);
                    }
                    setResumedActivityUncheckLocked(stackToReparent);
                }
            } else if (this.mHwSplitScreenSecondaryStack == stack) {
                this.mHwSplitScreenSecondaryStack = null;
                ActivityStack stackToReparent2 = this.mHwSplitScreenPrimaryStack;
                this.mHwSplitScreenPrimaryStack = null;
                if (stackToReparent2 != null) {
                    if (stackToReparent2.getTaskStack() != null) {
                        stackToReparent2.getTaskStack().clearAdjustedBounds();
                    }
                    if (stack.inHwFreeFormWindowingMode()) {
                        HwMultiWindowManager.exitHwMultiStack(stackToReparent2, false, false, false, true, false);
                    } else {
                        HwMultiWindowManager.exitHwMultiStack(stackToReparent2);
                    }
                    setResumedActivityUncheckLocked(stackToReparent2);
                }
            }
        }
    }

    public Rect getHwSplitScreenStackBounds(int windowingMode) {
        ActivityStack activityStack = this.mHwSplitScreenPrimaryStack;
        if (activityStack != null && activityStack.getWindowingMode() == windowingMode) {
            return this.mHwSplitScreenPrimaryStack.getBounds();
        }
        ActivityStack activityStack2 = this.mHwSplitScreenSecondaryStack;
        if (activityStack2 == null || activityStack2.getWindowingMode() != windowingMode) {
            return null;
        }
        return this.mHwSplitScreenSecondaryStack.getBounds();
    }

    public List<ActivityStack> findCombinedSplitScreenStacks(ActivityStack stack) {
        List<ActivityStack> combinedStacks = new ArrayList<>();
        ActivityStack activityStack = this.mHwSplitScreenPrimaryStack;
        if (activityStack == stack) {
            combinedStacks.add(this.mHwSplitScreenSecondaryStack);
        } else if (this.mHwSplitScreenSecondaryStack == stack) {
            combinedStacks.add(activityStack);
        }
        return combinedStacks;
    }

    public void replaceCombinedSplitScreenStack(ActivityStack stack) {
        if (stack.inHwSplitScreenPrimaryWindowingMode()) {
            ActivityStack stackToReparent = this.mHwSplitScreenPrimaryStack;
            this.mHwSplitScreenPrimaryStack = stack;
            if (stackToReparent != null) {
                ActivityRecord topActivity = stackToReparent.getTopActivity();
                if (topActivity != null) {
                    stackToReparent.mService.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity.appToken, false);
                }
                stackToReparent.getDisplay().positionChildAtBottom(stackToReparent);
                HwMultiWindowManager.exitHwMultiStack(stackToReparent, false, false, false, true, false);
            }
        } else if (stack.inHwSplitScreenSecondaryWindowingMode()) {
            ActivityStack stackToReparent2 = this.mHwSplitScreenSecondaryStack;
            this.mHwSplitScreenSecondaryStack = stack;
            if (stackToReparent2 != null) {
                ActivityRecord topActivity2 = stackToReparent2.getTopActivity();
                if (topActivity2 != null) {
                    stackToReparent2.mService.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity2.appToken, false);
                }
                stackToReparent2.getDisplay().positionChildAtBottom(stackToReparent2);
                HwMultiWindowManager.exitHwMultiStack(stackToReparent2, false, false, false, true, false);
            }
        }
    }

    private void setResumedActivityUncheckLocked(ActivityStack stack) {
        ActivityRecord top = stack.topRunningActivityLocked();
        if (top != null && top != stack.mService.getLastResumedActivityRecord() && top == stack.mService.mRootActivityContainer.getTopResumedActivity()) {
            stack.mService.setResumedActivityUncheckLocked(top, "onHwSplitScreenModeDismissed");
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("hw split primary: ");
        ActivityStack activityStack = this.mHwSplitScreenPrimaryStack;
        String str = null;
        sb.append(activityStack != null ? activityStack.toShortString() : null);
        sb.append(", hw split secondary: ");
        ActivityStack activityStack2 = this.mHwSplitScreenSecondaryStack;
        if (activityStack2 != null) {
            str = activityStack2.toShortString();
        }
        sb.append(str);
        return sb.toString();
    }

    public boolean isSplitScreenCombinedAndVisible() {
        if (isSplitScreenCombined() && isHwSplitScreenVisible(this.mHwSplitScreenPrimaryStack) && isHwSplitScreenVisible(this.mHwSplitScreenSecondaryStack)) {
            return true;
        }
        return false;
    }

    private boolean isHwSplitScreenVisible(ActivityStack activityStack) {
        return (activityStack.getTaskStack() != null && activityStack.getTaskStack().isVisible()) || activityStack.isTopActivityVisible();
    }

    public boolean isSplitScreenVisible() {
        if (this.mHwSplitScreenPrimaryStack == null && this.mHwSplitScreenSecondaryStack == null) {
            return false;
        }
        ActivityStack activityStack = this.mHwSplitScreenSecondaryStack;
        if (activityStack != null) {
            ActivityStack activityStack2 = this.mHwSplitScreenPrimaryStack;
            if (activityStack2 == null) {
                if (activityStack.getTaskStack() == null || !this.mHwSplitScreenSecondaryStack.getTaskStack().isVisible()) {
                    return false;
                }
                return true;
            } else if (activityStack2.getTaskStack() == null || !this.mHwSplitScreenPrimaryStack.getTaskStack().isVisible() || this.mHwSplitScreenSecondaryStack.getTaskStack() == null || !this.mHwSplitScreenSecondaryStack.getTaskStack().isVisible()) {
                return false;
            } else {
                return true;
            }
        } else if (this.mHwSplitScreenPrimaryStack.getTaskStack() == null || !this.mHwSplitScreenPrimaryStack.getTaskStack().isVisible()) {
            return false;
        } else {
            return true;
        }
    }

    public void resizeHwSplitStacks(int splitRatio, boolean isEnsureVisible) {
        if (isSplitScreenCombined()) {
            ActivityStack activityStack = this.mHwSplitScreenPrimaryStack;
            if (!(activityStack == null || this.mHwSplitScreenSecondaryStack == null)) {
                DividerBarDragEventReport.bdReport(activityStack.mService.mContext, this.mSplitRatio, splitRatio, this.mHwSplitScreenPrimaryStack.getConfiguration().orientation);
            }
            if (splitRatio == 0 || splitRatio == 1 || splitRatio == 2) {
                this.mSplitRatio = splitRatio;
                Rect primaryOutBounds = new Rect();
                Rect secondaryOutBounds = new Rect();
                HwMultiWindowManager.calcHwSplitStackBounds(this.mHwSplitScreenPrimaryStack.getDisplay(), splitRatio, primaryOutBounds, secondaryOutBounds);
                this.mHwSplitScreenPrimaryStack.resize(primaryOutBounds, (Rect) null, (Rect) null);
                this.mHwSplitScreenSecondaryStack.resize(secondaryOutBounds, (Rect) null, (Rect) null);
                if (isEnsureVisible) {
                    this.mHwSplitScreenSecondaryStack.mService.mStackSupervisor.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, true);
                }
            } else if (splitRatio == 3) {
                this.mSplitRatio = splitRatio;
                ActivityStack stackSecondary = this.mHwSplitScreenSecondaryStack;
                ActivityRecord topSecondary = stackSecondary.getTopActivity();
                if (topSecondary != null) {
                    stackSecondary.mService.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topSecondary.appToken, false);
                }
                stackSecondary.getDisplay().positionChildAtBottom(stackSecondary);
                HwMultiWindowManager.exitHwMultiStack(stackSecondary, false, false, false, true, false);
            } else if (splitRatio == 4) {
                this.mSplitRatio = splitRatio;
                ActivityStack stackPrimary = this.mHwSplitScreenPrimaryStack;
                ActivityRecord topPrimary = stackPrimary.getTopActivity();
                if (topPrimary != null) {
                    stackPrimary.mService.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topPrimary.appToken, false);
                }
                stackPrimary.getDisplay().positionChildAtBottom(stackPrimary);
                HwMultiWindowManager.exitHwMultiStack(stackPrimary, false, false, false, true, false);
            }
        }
    }

    public ActivityStack getHwSplitScreenPrimaryStack() {
        return this.mHwSplitScreenPrimaryStack;
    }

    public void reportPkgNameEvent(ActivityTaskManagerService activityTaskManagerService) {
        activityTaskManagerService.mH.post(new Runnable(activityTaskManagerService) {
            /* class com.android.server.wm.$$Lambda$HwSplitScreenCombination$4Sw_1_xoH8oXU5IKsyrokC4punE */
            private final /* synthetic */ ActivityTaskManagerService f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwSplitScreenCombination.this.lambda$reportPkgNameEvent$0$HwSplitScreenCombination(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$reportPkgNameEvent$0$HwSplitScreenCombination(ActivityTaskManagerService activityTaskManagerService) {
        synchronized (activityTaskManagerService.getGlobalLock()) {
            if (isSplitScreenCombined()) {
                ActivityRecord primaryTopActivity = this.mHwSplitScreenPrimaryStack.getTopActivity();
                ActivityRecord secondTopActivity = this.mHwSplitScreenSecondaryStack.getTopActivity();
                if (!(primaryTopActivity == null || secondTopActivity == null)) {
                    try {
                        JSONObject comboPkgNameRecord = new JSONObject();
                        comboPkgNameRecord.put("priPkg", primaryTopActivity.packageName);
                        comboPkgNameRecord.put("secPkg", secondTopActivity.packageName);
                        Flog.bdReport(activityTaskManagerService.mUiContext, (int) HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, comboPkgNameRecord);
                    } catch (JSONException e) {
                        Slog.e(TAG, "create json from split screen combination package names failed.");
                    }
                }
            }
        }
    }
}
