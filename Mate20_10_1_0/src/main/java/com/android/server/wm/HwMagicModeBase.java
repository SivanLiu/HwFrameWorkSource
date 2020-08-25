package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.HwMwUtils;
import android.util.Slog;
import com.android.server.magicwin.HwMagicWindowService;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import java.util.ArrayList;
import java.util.Iterator;

public class HwMagicModeBase {
    private static final String TAG = "HwMagicModeBase";
    protected Context mContext;
    protected IBinder mOrigActivityToken = null;
    protected HwMagicWinAmsPolicy mPolicy;
    protected HwMagicWindowService mService;

    public HwMagicModeBase(HwMagicWindowService service, Context context) {
        this.mService = service;
        this.mPolicy = service.getAmsPolicy();
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void setOrigActivityToken(HwActivityRecord activityRecord) {
        if (activityRecord != null && !this.mPolicy.isSpecTransActivity(activityRecord)) {
            this.mOrigActivityToken = activityRecord.appToken;
        }
    }

    public void overrideIntent(HwActivityRecord focus, HwActivityRecord next, boolean isNewTask) {
        int focusPosition;
        int targetPosition = 3;
        boolean isSlaveToMaster = false;
        boolean isDialogWin = false;
        if (focus.inHwMagicWindowingMode() && !isNewTask) {
            targetPosition = getTargetWindowPosition(focus, next);
            if (targetPosition == 1) {
                this.mPolicy.mMagicWinSplitMng.setAboveStackToDefault(focus.getTaskRecord().getStack(), targetPosition);
            }
            boolean isFocusSpecTrans = this.mPolicy.isSpecTransActivity(focus) && focus.mIsFinishAllRightBottom;
            if ((this.mService.isMaster(focus) || isFocusSpecTrans) && targetPosition == 2) {
                next.mIsFinishAllRightBottom = true;
                ActivityRecord slaveTop = this.mPolicy.getActvityByPosition(focus, 2, 0);
                if (this.mPolicy.isRelatedActivity(slaveTop)) {
                    this.mPolicy.setMagicWindowToPause(slaveTop);
                }
            }
            this.mService.getWmsPolicy().setIsMiddleOnClickBefore(this.mService.isMiddle(focus));
            if (isMoveActivityToMaster(focus, next, targetPosition)) {
                isSlaveToMaster = this.mService.isSlave(focus);
                setLeftTopActivityToPause(focus);
                next.mIsAniRunningBelow = true;
                this.mPolicy.moveWindow(focus, 1);
                moveOtherActivities(focus, 2);
            }
        }
        if (!next.fullscreen && this.mPolicy.mFullScreenBounds.equals(focus.getRequestedOverrideBounds())) {
            isDialogWin = true;
        }
        if (focus.inHwMagicWindowingMode() && isNewTask && this.mPolicy.mMagicWinSplitMng.isPkgSpliteScreenMode(next, true) && ((focusPosition = this.mService.getBoundsPosition(focus.getRequestedOverrideBounds())) == 1 || focusPosition == 2)) {
            targetPosition = focusPosition;
        }
        if (!focus.inHwMagicWindowingMode() || (!next.mIsFullScreenVideoInLandscape && !isDialogWin)) {
            HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
            if (hwMagicWinAmsPolicy.isPkgInLogoffStatus(hwMagicWinAmsPolicy.getPackageName((ActivityRecord) next), next.mUserId)) {
                targetPosition = 3;
            }
            setActivityBoundByPosition(next, targetPosition);
        } else {
            Slog.i(TAG, "overrideIntent set next = " + next + ", bounds = " + this.mPolicy.mFullScreenBounds);
            next.setBounds(this.mPolicy.mFullScreenBounds);
        }
        this.mService.getWmsPolicy().setFocusBound(focus.getRequestedOverrideBounds());
        this.mService.getWmsPolicy().overrideStartActivityAnimation(next.getRequestedOverrideBounds(), isSlaveToMaster, this.mPolicy.isSpecTransActivity(focus), this.mService.getWmsPolicy().getAllDrawnByActivity(focus.appToken));
        Slog.i(TAG, "overrideIntent focus=" + focus + " next=" + next + " targetPosition=" + targetPosition);
    }

    public void finishRightAfterFinishingLeft(HwActivityRecord finishActivity) {
        this.mPolicy.finishMagicWindow(finishActivity, true);
    }

    /* access modifiers changed from: protected */
    public boolean checkStatus(HwActivityRecord focus, HwActivityRecord next) {
        if (focus == null || next == null) {
            Slog.w(TAG, "focus or next is a null, next = " + next);
            return false;
        } else if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && HwFoldScreenManagerEx.getDisplayMode() != 1) {
            Slog.w(TAG, "Fold Device is not in full screen mode ");
            return false;
        } else if (!this.mService.getHwMagicWinEnabled(this.mPolicy.getPackageName((ActivityRecord) next))) {
            return false;
        } else {
            if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                if (!focus.inHwMagicWindowingMode() && focus.getActivityStack() == next.getActivityStack()) {
                    return false;
                }
                boolean isNextFullScreen = next.mIsFullScreenVideoInLandscape || next.info.screenOrientation == 3;
                if (focus.getWindowingMode() == 1 && focus.mIsFullScreenVideoInLandscape && isNextFullScreen) {
                    Slog.w(TAG, "do nothing when app is in fullscreen status and fullscreen mode");
                    return false;
                } else if (!this.mService.needRelaunch(this.mPolicy.getPackageName((ActivityRecord) next))) {
                    next.info.configChanges |= 3328;
                } else {
                    next.info.configChanges &= -3329;
                }
            }
            if (!HwMwUtils.isInSuitableScene(true) || focus.getActivityStack().inSplitScreenWindowingMode()) {
                Slog.w(TAG, "the current status does not need to enter magic widnow");
                return false;
            } else if (HwMwUtils.IS_FOLD_SCREEN_DEVICE || this.mContext.getResources().getConfiguration().orientation == 2) {
                return true;
            } else {
                Slog.w(TAG, "orientation is not landscape");
                return false;
            }
        }
    }

    public void addNewTaskFlag(HwActivityRecord focus, HwActivityRecord next) {
        boolean isNewTask = true;
        if (this.mService.getHwMagicWinEnabled(this.mPolicy.getPackageName((ActivityRecord) next))) {
            int targetPosition = this.mService.getMode(this.mPolicy.getPackageName((ActivityRecord) focus)).getTargetWindowPosition(focus, next);
            HwActivityRecord origActivity = ActivityRecord.forToken(this.mOrigActivityToken);
            boolean isStartWithTrans = this.mPolicy.isSpecTransActivity(focus) && origActivity != null && (this.mService.isMaster(origActivity) || this.mService.isMiddle(origActivity));
            if ((this.mService.isMiddle(focus) || this.mService.isMaster(focus) || isStartWithTrans) && targetPosition == 2 && !this.mPolicy.isMainActivity(next)) {
                next.intent.removeFlags(603979776);
                if (next.launchMode == 1 || next.launchMode == 2) {
                    if (next.taskAffinity == null || next.taskAffinity.equals(focus.taskAffinity) || (next.intent.getFlags() & 268435456) == 0 || !this.mPolicy.getPackageName((ActivityRecord) next).equals("com.baidu.searchbox")) {
                        isNewTask = false;
                    }
                    if (!isNewTask) {
                        next.launchMode = 0;
                    }
                }
            }
        } else if (HwMwUtils.IS_FOLD_SCREEN_DEVICE && next.resultTo != null) {
            Slog.d(TAG, "need to return result to another app, do not add the new task flag");
        } else if (focus.appInfo != null && next.launchedFromUid != focus.appInfo.uid) {
            Slog.d(TAG, "need to return for not start from current app");
        } else if (HwMagicWinAmsPolicy.PERMISSION_ACTIVITY.equals(this.mPolicy.getClassName((ActivityRecord) next)) || HwMagicWinAmsPolicy.DEVICE_ADMIN_ACTIVITY.equals(this.mPolicy.getClassName((ActivityRecord) next))) {
            this.mService.getUIController().updateSplitBarVisibility(false);
        } else {
            next.intent.addFlags(402653184);
            next.mIsMwNewTask = true;
            Slog.i(TAG, "add new task flag for next = " + next);
        }
    }

    public int getTargetWindowPosition(HwActivityRecord focus, HwActivityRecord next) {
        boolean isHomePageAndNotMainRelated;
        String nextPkg = this.mPolicy.getPackageName((ActivityRecord) next);
        if (this.mService.isHomePage(nextPkg, this.mPolicy.getClassName((ActivityRecord) next))) {
            HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
            if (!hwMagicWinAmsPolicy.isSupportMainRelatedMode(hwMagicWinAmsPolicy.getFocusedStackPackageName())) {
                isHomePageAndNotMainRelated = true;
                if (!isHomePageAndNotMainRelated || this.mService.getConfig().getWindowMode(nextPkg) == 0) {
                    return 3;
                }
                if (this.mPolicy.isMainActivity(next)) {
                    return 1;
                }
                int focusPosition = this.mService.getBoundsPosition(focus.getRequestedOverrideBounds());
                Slog.i(TAG, "getTargetWindowPosition focusPosition=" + focusPosition);
                if (this.mPolicy.isSpecTransActivityPreDefined(next)) {
                    return focusPosition;
                }
                if (this.mPolicy.isRelatedActivity(next)) {
                    return 2;
                }
                if (this.mService.getConfig().isSupportAppTaskSplitScreen(nextPkg)) {
                    boolean isPkgSpliteMode = this.mPolicy.mMagicWinSplitMng.isPkgSpliteScreenMode(next, true);
                    if (nextPkg.equals(this.mPolicy.getPackageName((ActivityRecord) focus)) && isPkgSpliteMode) {
                        return focusPosition;
                    }
                    if (next.launchMode == 2 && !nextPkg.equals(next.taskAffinity)) {
                        return 3;
                    }
                }
                if (!this.mPolicy.isDefaultFullscreenActivity(next)) {
                    return getTargetWindowPositionInner(focus, next);
                }
                if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                    return 3;
                }
                return 5;
            }
        }
        isHomePageAndNotMainRelated = false;
        if (!isHomePageAndNotMainRelated) {
        }
        return 3;
    }

    private int getTargetWindowPositionInner(HwActivityRecord focus, HwActivityRecord next) {
        int focusPosition = this.mService.getBoundsPosition(focus.getRequestedOverrideBounds());
        if (focusPosition != 0) {
            boolean isFull = true;
            if (focusPosition == 1 || focusPosition == 2) {
                return 2;
            }
            if (focusPosition != 3) {
                if (focusPosition == 5) {
                    if (HwMagicWinAmsPolicy.PERMISSION_ACTIVITY.equals(this.mPolicy.getClassName((ActivityRecord) focus)) || focus.getTaskRecord() != next.getTaskRecord()) {
                        isFull = false;
                    }
                    if (isFull) {
                        return 5;
                    }
                    return 3;
                }
            } else if (!this.mService.isHomePage(this.mPolicy.getPackageName((ActivityRecord) focus), this.mPolicy.getClassName((ActivityRecord) focus)) || this.mPolicy.isSpecTransActivity(focus)) {
                return 3;
            } else {
                return 2;
            }
        } else if (!this.mPolicy.mFullScreenBounds.equals(focus.getRequestedOverrideBounds()) || !this.mService.isSlave(focus)) {
            return 3;
        } else {
            return 2;
        }
        return 3;
    }

    public void adjustWindowForFinish(HwActivityRecord activity, String finishReason) {
    }

    public void setActivityBoundByMode(ArrayList<ActivityRecord> activities, String pkgName) {
        setActivityBoundMainRelatedIfNeed(activities, pkgName);
        boolean hasFullscreenActivity = false;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord ar = activities.get(i);
            ar.setBounds(this.mService.getBounds(3, this.mPolicy.getRealPkgName(ar)));
            hasFullscreenActivity = setDefaultFullscreenBounds(ar, hasFullscreenActivity);
        }
    }

    /* access modifiers changed from: protected */
    public boolean setDefaultFullscreenBounds(ActivityRecord ar, boolean isFullscreen) {
        if (!isFullscreen && !this.mPolicy.isDefaultFullscreenActivity(ar)) {
            return false;
        }
        ar.setBounds(this.mService.getBounds(5, this.mPolicy.getRealPkgName(ar)));
        return true;
    }

    /* access modifiers changed from: protected */
    public void setActivityBoundMainRelatedIfNeed(ArrayList<ActivityRecord> activities, String pkgName) {
        if (this.mPolicy.isFoldedState() || !this.mPolicy.isSupportMainRelatedMode(pkgName) || !this.mService.getHwMagicWinEnabled(pkgName)) {
            Slog.d(TAG, "activity is not A1A0 or folded, return");
        } else if (activities.size() < 1) {
            Slog.d(TAG, "there is not any activity in the list, return");
        } else {
            ActivityRecord mainActivity = activities.get(0);
            if (this.mPolicy.isMainActivity(mainActivity)) {
                ActivityStack stack = mainActivity.getActivityStack();
                activities.remove(0);
                stack.setWindowingMode(103, false, false, false, true, false);
                stack.resize((Rect) null, (Rect) null, (Rect) null);
                mainActivity.setBounds(this.mService.getBounds(1, pkgName));
                if (stack == this.mPolicy.getFocusedTopStack()) {
                    Slog.d(TAG, "set activity bound startRelateActivityIfNeed " + mainActivity);
                    this.mPolicy.startRelateActivityIfNeed(mainActivity, false);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public ActivityRecord getHomePageActivityRecord(ArrayList<ActivityRecord> activities) {
        Iterator<ActivityRecord> it = activities.iterator();
        while (it.hasNext()) {
            ActivityRecord ar = it.next();
            if (this.mService.isHomePage(this.mPolicy.getPackageName(ar), this.mPolicy.getClassName(ar))) {
                return ar;
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void adjustWindowForDoubleWindows(HwActivityRecord activity, String finishReason) {
        int windowIndex;
        if (!this.mService.isMaster(activity)) {
            ActivityRecord slaveNext = this.mPolicy.getActvityByPosition(activity, 2, 1);
            ActivityRecord masterNext = this.mPolicy.getActvityByPosition(activity, 1, 1);
            boolean isLeftTopMove = (this.mPolicy.isRelatedActivity(slaveNext) || ((slaveNext instanceof HwActivityRecord) && this.mPolicy.isSpecTransActivity((HwActivityRecord) slaveNext))) && this.mService.isSupportAnAnMode(this.mPolicy.getPackageName(activity)) && masterNext != null;
            if (slaveNext == null || isLeftTopMove) {
                windowIndex = 0;
            } else {
                Slog.i(TAG, "adjustWindowForDoubleWindows abort because right index 1 have activity");
                this.mPolicy.moveToFrontInner(slaveNext);
                return;
            }
        } else if (!activity.isTopRunningActivity()) {
            moveRightActivityToMiddleIfNeeded(activity);
            return;
        } else {
            windowIndex = 1;
        }
        if (!shouldStartRelatedActivityForFinish(activity, finishReason)) {
            ActivityRecord ar = this.mPolicy.getActvityByPosition(activity, 1, windowIndex);
            if (ar != null) {
                if (this.mPolicy.getActvityByPosition(activity, 1, windowIndex + 1) == null || this.mPolicy.isHomeActivity(ar)) {
                    if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                        activity.mIsAniRunningBelow = true;
                        this.mPolicy.moveWindow(ar, 3);
                    }
                } else if (!HwMagicWinAmsPolicy.MAGIC_WINDOW_FINISH_EVENT.equals(finishReason)) {
                    activity.mIsAniRunningBelow = true;
                    this.mPolicy.moveWindow(ar, 2);
                    moveOtherActivities(ar, 1);
                }
            } else if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                this.mPolicy.mModeSwitcher.updateActivityToFullScreenConfiguration(this.mPolicy.getActvityByPosition(activity, 0, 1));
            }
        }
    }

    private void moveRightActivityToMiddleIfNeeded(HwActivityRecord activity) {
        ActivityRecord leftNext = this.mPolicy.getActvityByPosition(activity, 1, 1);
        ActivityRecord rightTop = this.mPolicy.getActvityByPosition(activity, 2, 0);
        if (leftNext == null && rightTop != null) {
            activity.mIsAniRunningBelow = true;
            this.mPolicy.moveWindow(rightTop, 3);
        }
    }

    private boolean shouldStartRelatedActivityForFinish(HwActivityRecord activity, String finishReason) {
        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
        boolean z = false;
        if (!hwMagicWinAmsPolicy.isSupportMainRelatedMode(hwMagicWinAmsPolicy.getPackageName((ActivityRecord) activity)) || HwMagicWinAmsPolicy.MAGIC_WINDOW_FINISH_EVENT.equals(finishReason)) {
            return false;
        }
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(activity.getActivityStack());
        tempActivityList.remove(activity);
        if (tempActivityList.size() != 0 && !this.mPolicy.isRelatedActivity(activity)) {
            ActivityRecord topActivity = tempActivityList.get(0);
            if (this.mPolicy.isMainActivity(topActivity) && topActivity.inHwMagicWindowingMode()) {
                HwMagicWinAmsPolicy hwMagicWinAmsPolicy2 = this.mPolicy;
                if (tempActivityList.size() == 1) {
                    z = true;
                }
                hwMagicWinAmsPolicy2.startRelateActivityIfNeed(topActivity, z);
                return true;
            }
        }
        return false;
    }

    public boolean isMoveActivityToMaster(HwActivityRecord focus, HwActivityRecord next, int targetPosition) {
        return false;
    }

    public boolean shouldEnterMagicWinForTah(HwActivityRecord focus, HwActivityRecord next) {
        return false;
    }

    public boolean isSkippingMoveToMaster(HwActivityRecord focus, HwActivityRecord next) {
        return this.mPolicy.isSpecTransActivity(focus) || this.mPolicy.isSpecTransActivityPreDefined(next) || ((next.info.launchMode == 2 || next.info.launchMode == 1) && (this.mPolicy.getClassName(focus).equals(this.mPolicy.getClassName(next)) && this.mPolicy.getPackageName(focus).equals(this.mPolicy.getPackageName(next)))) || this.mPolicy.isRelatedActivity(focus) || this.mPolicy.isHomeStackHotStart(focus, next);
    }

    public void setActivityBoundByPosition(HwActivityRecord next, int position) {
        if (next != null) {
            if (next.getActivityStack() != null && !next.getActivityStack().inHwMagicWindowingMode()) {
                next.getActivityStack().setWindowingMode(103, true, false, false, true, false);
            }
            Rect bound = new Rect(this.mService.getBounds(position, this.mPolicy.getRealPkgName(next)));
            if (this.mPolicy.mMagicWinSplitMng.isSpliteModeStack(next.getActivityStack())) {
                this.mService.getConfig().adjustSplitBound(position, bound);
            }
            if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
                Slog.i(TAG, "setActivityBoundByPosition next = " + next + " position = " + position + " bound = " + bound);
            }
            next.setBounds(bound);
        }
    }

    public void setLeftTopActivityToPause(HwActivityRecord focus) {
    }

    public boolean isNonFullScreen(HwActivityRecord activityRecord) {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            return false;
        }
        boolean isSpecTrans = this.mPolicy.isSpecTransActivityPreDefined(activityRecord);
        if (activityRecord.fullscreen || isSpecTrans) {
            return false;
        }
        return true;
    }

    public void moveOtherActivities(ActivityRecord focus, int currentPosition) {
    }
}
