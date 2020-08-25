package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.service.voice.IVoiceInteractionSession;
import android.util.HwMwUtils;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.am.ActivityManagerService;
import com.android.server.magicwin.HwMagicWinStatistics;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.wm.ActivityStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HwMagicModeSwitcher {
    private static final String CAMERA_PACKAGE = "com.huawei.camera";
    private static final int COMPUTER_RETRUN_VALUE_NEGATIVE = -1;
    private static final String TAG = "HwMagicModeSwitcher";
    private ActivityTaskManagerService mActivityTaskManager;
    private final ActivityManagerService mAms;
    private Comparator<ActivityRecord> mComputor = new Comparator<ActivityRecord>() {
        /* class com.android.server.wm.HwMagicModeSwitcher.AnonymousClass1 */

        public int compare(ActivityRecord activity1, ActivityRecord activity2) {
            if (((HwActivityRecord) activity1).mCreateTime > ((HwActivityRecord) activity2).mCreateTime) {
                return 1;
            }
            return -1;
        }
    };
    private HwMagicWinAmsPolicy mPolicy;
    private HwMagicWindowService mService;
    private Set<Integer> mStackIdsLastInMagicWindow = new HashSet();
    private Set<Integer> mStackIdsMoveToMagicWindow = new HashSet(1);

    public HwMagicModeSwitcher(HwMagicWinAmsPolicy policy, HwMagicWindowService service, ActivityManagerService ams) {
        this.mPolicy = policy;
        this.mService = service;
        this.mAms = ams;
        this.mActivityTaskManager = ams.mActivityTaskManager;
    }

    public void processSpliteScreenForMutilWin(int stackId, boolean isFreeze, int orientation, Bundle result) {
        boolean isMagic = true;
        if (!HwMwUtils.isInSuitableScene(true)) {
            Slog.w(TAG, "orientation is portrait or not in suit scene");
        } else if ((!HwMwUtils.IS_FOLD_SCREEN_DEVICE || this.mPolicy.isFoldedState()) && orientation != 2) {
            Slog.w(TAG, "device is not foldable and orientation is portrait ");
        } else {
            ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
            ActivityStack primaryStack = display.getStack(stackId);
            if (primaryStack == null) {
                Slog.w(TAG, "primaryStack is a null object");
                return;
            }
            ActivityRecord topActivity = primaryStack.getTopActivity();
            if (!isStayInOtherWinMode(topActivity)) {
                String pkgName = this.mPolicy.getRealPkgName(topActivity);
                if (!this.mService.getHwMagicWinEnabled(pkgName) || !primaryStack.inPinnedWindowingMode()) {
                    if (primaryStack.inSplitScreenPrimaryWindowingMode()) {
                        moveActivityToMagicWindow(display, null, true);
                    }
                    if (this.mService.getHwMagicWinEnabled(pkgName)) {
                        primaryStack.setWindowingMode(103);
                        primaryStack.resize((Rect) null, (Rect) null, (Rect) null);
                        setMagicActivityBound(primaryStack, pkgName);
                        if (primaryStack.getWindowingMode() != 103) {
                            isMagic = false;
                        }
                        result.putBoolean("RESULT_SPLITE_SCREEN", isMagic);
                        if (isFreeze) {
                            this.mAms.mWindowManager.stopFreezingScreen();
                            return;
                        }
                        return;
                    }
                    return;
                }
                reparentToMagicWindow(primaryStack, pkgName);
                result.putBoolean("RESULT_SPLITE_SCREEN", true);
            }
        }
    }

    private boolean isNeedShowFreeFormAnimation(ActivityStack stack, ActivityRecord topActivity, boolean isHwFreeFormStack, int orientation) {
        return isHwFreeFormStack && (this.mPolicy.getAllActivities(stack).size() > 1 || this.mPolicy.isMainActivity(topActivity)) && (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || orientation != 2);
    }

    public void processHwMultiStack(int stackId, int orientation, Bundle result) {
        ActivityStack stack = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getStack(stackId);
        if (stack == null) {
            Slog.w(TAG, "HwMultiStack is a null object");
        } else if (!this.mPolicy.mMagicWinSplitMng.isNeedProcessCombineStack(stack, true)) {
            boolean isNormalPort = !HwMwUtils.IS_FOLD_SCREEN_DEVICE && orientation != 2;
            boolean isFolded = this.mPolicy.isFoldedState();
            if (isNormalPort || isFolded) {
                Slog.w(TAG, "processHwMultiStack orientation is portrait");
                return;
            }
            ActivityRecord topAr = stack.getTopActivity();
            if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || orientation != 2 || topAr == null || !(topAr instanceof HwActivityRecord) || !((HwActivityRecord) topAr).mIsFullScreenVideoInLandscape) {
                String pkgName = this.mPolicy.getRealPkgName(stack.getTopActivity());
                if (this.mService.getHwMagicWinEnabled(pkgName) && !isStayInOtherWinMode(topAr)) {
                    boolean isHwFreeFormStack = stack.inHwFreeFormWindowingMode();
                    ActivityRecord topActivity = stack.getTopActivity();
                    if (isNeedShowFreeFormAnimation(stack, topActivity, isHwFreeFormStack, orientation)) {
                        this.mService.getWmsPolicy().startExitSplitAnimation(topActivity.appToken, (float) this.mActivityTaskManager.mUiContext.getResources().getDimensionPixelSize(34472524));
                    }
                    if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || !this.mService.isSupportOpenMode(pkgName)) {
                        stack.setWindowingMode(103);
                        stack.resize((Rect) null, (Rect) null, (Rect) null);
                    }
                    setMagicActivityBound(stack, pkgName);
                    if (isHwFreeFormStack && topActivity != null && topActivity.inHwMagicWindowingMode()) {
                        this.mPolicy.checkBackgroundForMagicWindow(topActivity);
                    }
                    result.putBoolean("RESULT_HWMULTISTACK", true);
                    return;
                }
                return;
            }
            Slog.w(TAG, "processHwMultiStack original window is in FullScreenVideoInLandscape");
            if (this.mService.getHwMagicWinEnabled(this.mPolicy.getRealPkgName(topAr))) {
                this.mStackIdsLastInMagicWindow.remove(Integer.valueOf(stackId));
                this.mStackIdsMoveToMagicWindow.add(Integer.valueOf(stackId));
            }
        }
    }

    public void moveMwToHwMultiStack(int stackId, Rect bounds, Bundle result) {
        ActivityStack stack = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getStack(stackId);
        if (stack == null || bounds == null || bounds.isEmpty()) {
            Slog.w(TAG, "moveMwToHwMultiStack exception return");
            return;
        }
        ActivityRecord topAr = stack.getTopActivity();
        if (topAr != null) {
            String pkgName = this.mPolicy.getRealPkgName(topAr);
            if (this.mPolicy.isSupportMainRelatedMode(pkgName)) {
                this.mPolicy.removeRelatedActivity(stack);
            }
            int stackPos = this.mService.getBoundsPosition(stack.getRequestedOverrideBounds());
            if (stackPos == 1 || stackPos == 2) {
                this.mPolicy.mMagicWinSplitMng.quitMagicSplitScreenMode(pkgName, stackId, true, topAr.mUserId);
            }
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                moveLatestActivityToTop(false, false);
            }
            stack.resize(bounds, (Rect) null, (Rect) null);
            setActivityBoundForStack(stack, bounds);
        }
    }

    private void reparentToMagicWindow(ActivityStack sourceStack, String pkgName) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        ArrayList<TaskRecord> tasks = sourceStack.getAllTasks();
        if (!tasks.isEmpty()) {
            ActivityOptions tmpOptions = ActivityOptions.makeBasic();
            tmpOptions.setLaunchWindowingMode(103);
            int size = tasks.size();
            int i = 0;
            while (i < size) {
                TaskRecord task = tasks.get(i);
                ActivityStack toStack = display.getOrCreateStack((ActivityRecord) null, tmpOptions, task, task.getActivityType(), true);
                boolean isTopTask = i == size + -1;
                if (toStack != null) {
                    task.reparent(toStack, true, 0, isTopTask, true, true, "moveTasksToMagicWindowStack - onTop");
                    toStack.resize((Rect) null, (Rect) null, (Rect) null);
                    setMagicActivityBound(toStack, pkgName);
                }
                i++;
            }
        }
    }

    private void moveActivityToMagicWindow(ActivityDisplay display, IBinder token, boolean isFromSplit) {
        if (!this.mPolicy.isFoldedState()) {
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (!(stack.topTask() == null || stack.topTask().getTopActivity() == null || stack.getActivityType() == 2)) {
                    if (isFromSplit) {
                        moveActivityToMagicWindowForSplit(stack);
                    } else {
                        moveActivityToMagicWindowInner(stack, token);
                    }
                }
            }
            if (display.getTopStack() != null && display.getTopStack().inHwMagicWindowingMode()) {
                HwMagicWinStatistics.getInstance().startTick(this.mService.getConfig(), this.mPolicy.getPackageName(display), -1);
            }
        }
    }

    private void moveActivityToMagicWindowForSplit(ActivityStack stack) {
        String pkgName = this.mPolicy.getRealPkgName(stack.topTask().getTopActivity());
        if (stack.inSplitScreenSecondaryWindowingMode() && this.mService.getHwMagicWinEnabled(pkgName)) {
            stack.setWindowingMode(103, false, false, false, true, false);
            stack.resize((Rect) null, (Rect) null, (Rect) null);
            setMagicActivityBound(stack, pkgName);
        }
    }

    public void moveAppToMagicWinWhenFinishingFullscreen(ActivityRecord finishActivity) {
        Slog.i(TAG, "moveAppToMagicWinWhenFinishingFullscreen");
        if (finishActivity == null || finishActivity.getTaskRecord().getStack() == null) {
            Slog.i(TAG, "finish activity or stack is null");
            return;
        }
        ActivityStack stack = finishActivity.getTaskRecord().getStack();
        String pkgName = this.mPolicy.getRealPkgName(finishActivity);
        stack.setWindowingMode(103, false, false, false, true, false);
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(stack);
        tempActivityList.remove(finishActivity);
        this.mService.getMode(pkgName).setActivityBoundByMode(tempActivityList, pkgName);
    }

    private void setMagicActivityBound(ActivityStack stack, String pkgName) {
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(stack);
        if (tempActivityList.size() < 1) {
            Slog.d(TAG, "there is not any activity in the list, return");
            return;
        }
        ActivityRecord bottomActivity = tempActivityList.get(0);
        if (HwMagicWinAmsPolicy.PERMISSION_ACTIVITY.equals(this.mPolicy.getClassName(bottomActivity))) {
            bottomActivity.setBounds((Rect) null);
            tempActivityList.remove(bottomActivity);
        }
        if (tempActivityList.size() == 1) {
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                stack.setWindowingMode(1);
            }
            this.mService.getBaseMode().setActivityBoundByMode(tempActivityList, pkgName);
            return;
        }
        this.mService.getMode(pkgName).setActivityBoundByMode(tempActivityList, pkgName);
    }

    private void moveActivityToMagicWindowInner(ActivityStack stack, IBinder token) {
        if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || this.mStackIdsLastInMagicWindow.contains(Integer.valueOf(stack.mStackId)) || this.mStackIdsMoveToMagicWindow.contains(Integer.valueOf(stack.mStackId))) {
            HwActivityRecord topActivity = stack.topTask().getTopActivity();
            boolean needMove = false;
            boolean isAppRequest = ActivityRecord.forToken(token) == topActivity && topActivity.mIsFullScreenVideoInLandscape;
            if (topActivity.info != null) {
                isAppRequest |= ActivityInfo.isFixedOrientationLandscape(topActivity.info.screenOrientation);
            }
            if (stack.getWindowingMode() == 1 && !isAppRequest) {
                needMove = true;
            }
            String pkgName = this.mPolicy.getRealPkgName(topActivity);
            if (this.mService.getHwMagicWinEnabled(pkgName) && needMove) {
                if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || !this.mStackIdsLastInMagicWindow.contains(Integer.valueOf(stack.mStackId))) {
                    if (!HwMwUtils.IS_FOLD_SCREEN_DEVICE || !this.mService.isSupportOpenMode(pkgName)) {
                        stack.setWindowingMode(103, false, false, false, true, false);
                    }
                    setMagicActivityBound(stack, pkgName);
                    return;
                }
                stack.setWindowingMode(103, false, false, false, true, false);
                setActivityBoundAsLast(stack);
            }
        }
    }

    private void setActivityBoundAsLast(ActivityStack stack) {
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(stack);
        if (tempActivityList.size() < 1) {
            Slog.d(TAG, "there is not any activity in the list, return");
            return;
        }
        ActivityRecord bottomActivity = tempActivityList.get(0);
        if (HwMagicWinAmsPolicy.PERMISSION_ACTIVITY.equals(this.mPolicy.getClassName(bottomActivity))) {
            bottomActivity.setBounds((Rect) null);
            tempActivityList.remove(bottomActivity);
        }
        Iterator<ActivityRecord> it = tempActivityList.iterator();
        while (it.hasNext()) {
            ActivityRecord ar = it.next();
            if (((HwActivityRecord) ar).mLastBound != null) {
                ar.setBounds(((HwActivityRecord) ar).mLastBound);
            }
        }
    }

    public void updateMagicWindowConfiguration(int oldOrientation, int newOrientation, IBinder token) {
        if (!(oldOrientation == newOrientation || oldOrientation == 0 || newOrientation == 0)) {
            updateMagicWindowConfigurationInner(newOrientation, token);
        }
        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
        hwMagicWinAmsPolicy.sendMessageForSetMultiWinCameraProp(hwMagicWinAmsPolicy.isNeedRotateCamera());
    }

    private void cleanRelatedActivityIfExist(ActivityDisplay display) {
        for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = display.getChildAt(stackNdx);
            if (this.mPolicy.isSupportMainRelatedMode(this.mPolicy.getPackageName(stack.getTopActivity()))) {
                this.mPolicy.removeRelatedActivity(stack);
            }
        }
    }

    private void updateMagicWindowConfigurationInner(int newOrientation, IBinder token) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        Rect rect = null;
        if (newOrientation == 1) {
            if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
                moveActivityToMagicWindow(display, token, false);
                return;
            }
            cleanRelatedActivityIfExist(display);
            if (display.getFocusedStack() != null) {
                this.mService.getWmsPolicy().setRotationAnimation(this.mPolicy.getTopActivity() == null ? null : this.mPolicy.getTopActivity().getBounds(), display.getFocusedStack().getWindowingMode());
                moveActivityToFullScreen(display);
                this.mService.getUIController().dismissDialog();
                ActivityStack activityStack = this.mPolicy.getFocusedTopStack();
                if (activityStack != null && activityStack.isHomeOrRecentsStack()) {
                    this.mService.getUIController().hideMwWallpaperInNeed();
                }
            } else {
                return;
            }
        }
        if (newOrientation != 2) {
            return;
        }
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            cleanRelatedActivityIfExist(display);
            moveActivityToFullScreen(display);
            return;
        }
        ActivityRecord activityRecord = this.mPolicy.getTopActivity();
        if (!isStayInOtherWinMode(activityRecord)) {
            if (HwMwUtils.IS_TABLET && activityRecord != null && this.mPolicy.isMainActivity(activityRecord)) {
                this.mPolicy.startRelateActivityIfNeed(activityRecord, false);
            }
            moveSystemActivityToNewTask(display, token);
            moveActivityToMagicWindow(display, token, false);
            if (display.getFocusedStack() != null) {
                HwMagicWinWmsPolicy wmsPolicy = this.mService.getWmsPolicy();
                if (this.mPolicy.getTopActivity() != null) {
                    rect = this.mPolicy.getTopActivity().getBounds();
                }
                wmsPolicy.setRotationAnimation(rect, display.getFocusedStack().getWindowingMode());
                if (display.getTopStack() != null && display.getTopStack().inHwMagicWindowingMode()) {
                    this.mService.getUIController().whetherShowDialog(this.mPolicy.getPackageName(display));
                }
            }
        }
    }

    private boolean isStayInOtherWinMode(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            return false;
        }
        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mPolicy;
        return hwMagicWinAmsPolicy.isPkgInLogoffStatus(hwMagicWinAmsPolicy.getPackageName(activityRecord), activityRecord.mUserId);
    }

    private void moveActivityToFullScreen(ActivityDisplay display) {
        this.mStackIdsLastInMagicWindow.clear();
        this.mStackIdsMoveToMagicWindow.clear();
        for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = display.getChildAt(stackNdx);
            if (stack.inHwMagicWindowingMode()) {
                this.mStackIdsLastInMagicWindow.add(Integer.valueOf(stack.mStackId));
                setActivityBoundForStack(stack, null);
                if (this.mPolicy.mMagicWinSplitMng.isSpliteModeStack(stack)) {
                    HwMagicWinCombineManager.getInstance().clearSplitScreenList(this.mPolicy.getPackageName(stack.getTopActivity()), this.mPolicy.getStackUserId(stack));
                }
                stack.setWindowingMode(1, false, false, false, true, false);
            }
        }
        if (display.getTopStack() != null && !display.getTopStack().inHwMagicWindowingMode()) {
            HwMagicWinStatistics.getInstance().stopTick();
        }
    }

    private void moveSystemActivityToNewTask(ActivityDisplay display, IBinder token) {
        ActivityStack topStack = display.getTopStack();
        if (topStack != null && topStack.topTask() != null && topStack.topTask().getTopActivity() != null) {
            HwActivityRecord top = topStack.topTask().getTopActivity();
            String realName = this.mPolicy.getRealPkgName(top);
            String pkgName = this.mPolicy.getPackageName((ActivityRecord) top);
            if (this.mService.getHwMagicWinEnabled(realName) && "com.huawei.camera".equals(pkgName)) {
                TaskRecord newTask = topStack.createTaskRecord(this.mActivityTaskManager.getStackSupervisor().getNextTaskIdForUserLocked(top.mUserId), top.info, top.intent, (IVoiceInteractionSession) null, (IVoiceInteractor) null, true);
                top.reparent(newTask, 0, "magicwin app launch camera portraitly,move camera activity to new task");
                ActivityOptions tmpOptions = ActivityOptions.makeBasic();
                tmpOptions.setLaunchWindowingMode(1);
                newTask.reparent(display.getOrCreateStack((ActivityRecord) null, tmpOptions, newTask, newTask.getActivityType(), true), true, 0, true, true, true, "moveCamera - onTop");
            }
        }
    }

    public void moveLatestActivityToTop(boolean isQuitMagicWindow, boolean isNeedClearBounds) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            if (isQuitMagicWindow) {
                this.mPolicy.mMagicWinSplitMng.updateSpliteStackSequence(display);
            }
            this.mPolicy.finishActivitiesAfterTopActivity();
            ArrayList<ActivityStack> needProcessStacks = new ArrayList<>();
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                String pkgName = this.mPolicy.getRealPkgName(stack.getTopActivity());
                if (this.mService.getHwMagicWinEnabled(pkgName) && (stack.inHwMagicWindowingMode() || stack.inSplitScreenPrimaryWindowingMode() || stack.inHwSplitScreenWindowingMode())) {
                    sortActivities(stack);
                    if (isQuitMagicWindow && this.mPolicy.mMagicWinSplitMng.isNeedNewTaskStack(stack) && stack.inHwMagicWindowingMode()) {
                        needProcessStacks.add(stack);
                    }
                    ActivityRecord topActivity = stack.getTopActivity();
                    ActivityRecord masterTop = this.mPolicy.getActvityByPosition(topActivity, 1, 0);
                    if (masterTop != null && masterTop.inHwMagicWindowingMode() && !this.mPolicy.mMagicWinSplitMng.isSpliteModeStack(stack)) {
                        this.mPolicy.setMagicWindowToPause(masterTop);
                    }
                    ActivityRecord latestActivity = this.mPolicy.getActvityByPosition(topActivity, 2, 0);
                    if (this.mService.isMiddle(topActivity)) {
                        latestActivity = topActivity;
                    }
                    if (this.mPolicy.isSupportMainRelatedMode(pkgName)) {
                        if (this.mPolicy.isRelatedActivity(latestActivity)) {
                            latestActivity = this.mPolicy.getActvityByPosition(topActivity, 1, 0);
                        } else if (this.mService.getConfig().isSupportAppTaskSplitScreen(pkgName)) {
                            latestActivity = this.mPolicy.mMagicWinSplitMng.getLatestActivityBySplitMode(pkgName, stack, topActivity, latestActivity);
                        }
                        if (isQuitMagicWindow || isNeedClearBounds) {
                            this.mPolicy.removeRelatedActivity(stack);
                        }
                    }
                    resetStackMode(stack, latestActivity, isQuitMagicWindow, isNeedClearBounds);
                }
            }
            Iterator<ActivityStack> it = needProcessStacks.iterator();
            while (it.hasNext()) {
                this.mPolicy.mMagicWinSplitMng.isNeedProcessCombineStack(it.next(), false);
            }
        }
    }

    private void resetStackMode(ActivityStack stack, ActivityRecord latestActivity, boolean isQuitMagicWindow, boolean isNeedClearBounds) {
        if (latestActivity != null) {
            Slog.i(TAG, "latest activity is:" + latestActivity);
            stack.getTopActivity().getTaskRecord().moveActivityToFrontLocked(latestActivity);
            this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
            this.mPolicy.checkResumeStateForMagicWindow(latestActivity);
            if (isQuitMagicWindow) {
                stack.setWindowingMode(1, false, false, false, true, false);
                setActivityBoundForStack(stack, null);
                if (!this.mService.getConfig().isSupportAppTaskSplitScreen(this.mPolicy.getRealPkgName(latestActivity))) {
                    latestActivity.forceNewConfig = (latestActivity.info.configChanges & 3328) != 3328;
                    latestActivity.ensureActivityConfiguration(0, false);
                }
            }
        } else if (isQuitMagicWindow && stack.inHwMagicWindowingMode()) {
            stack.setWindowingMode(1, false, false, false, true, false);
            setActivityBoundForStack(stack, null);
        }
        if (isNeedClearBounds) {
            setActivityBoundForStack(stack, null);
        }
    }

    private void setActivityBoundForStack(ActivityStack stack, Rect bounds) {
        ArrayList<ActivityRecord> tempActivityList;
        if (HwMwUtils.IS_FOLD_SCREEN_DEVICE) {
            tempActivityList = this.mPolicy.getAllActivities(stack);
        } else {
            tempActivityList = getActivitiesFocusToRight(stack);
        }
        Iterator<ActivityRecord> it = tempActivityList.iterator();
        while (it.hasNext()) {
            ActivityRecord ar = it.next();
            boolean isSetLastBound = HwMwUtils.IS_FOLD_SCREEN_DEVICE || !this.mService.getConfig().isSupportAppTaskSplitScreen(this.mPolicy.getPackageName(ar));
            if (bounds == null && isSetLastBound) {
                ((HwActivityRecord) ar).mLastBound = new Rect(((HwActivityRecord) ar).getRequestedOverrideBounds());
            }
            ar.setBounds(bounds);
            ar.onRequestedOverrideConfigurationChanged(Configuration.EMPTY);
        }
    }

    private ArrayList<ActivityRecord> getActivitiesFocusToRight(ActivityStack stack) {
        ArrayList<ActivityRecord> tempActivityList = this.mPolicy.getAllActivities(stack);
        ActivityRecord topActivity = stack.getTopActivity();
        if (topActivity != null) {
            ActivityRecord masterTop = this.mPolicy.getActvityByPosition(topActivity, 1, 0);
            if (masterTop != null && !this.mService.getConfig().isSupportAppTaskSplitScreen(this.mPolicy.getPackageName(masterTop))) {
                this.mPolicy.setMagicWindowToPause(masterTop);
            }
            int topActivityPosition = this.mService.getBoundsPosition(topActivity.getRequestedOverrideBounds());
            ActivityRecord bottomActivity = null;
            if (!tempActivityList.isEmpty()) {
                bottomActivity = tempActivityList.get(tempActivityList.size() - 1);
            }
            ActivityRecord slaveTop = this.mPolicy.getActvityByPosition(topActivity, 2, 0);
            if (!(masterTop == null || slaveTop == null || (tempActivityList.indexOf(masterTop) >= tempActivityList.indexOf(slaveTop) && !this.mService.isSlave(bottomActivity)))) {
                adjustActivitiesOrder(masterTop, tempActivityList);
                if (topActivityPosition == 5) {
                    return tempActivityList;
                }
                this.mActivityTaskManager.mRootActivityContainer.resumeFocusedStacksTopActivities();
                if (slaveTop.getActivityStack().mResumedActivity != slaveTop && slaveTop.isState(ActivityStack.ActivityState.RESUMED)) {
                    slaveTop.getActivityStack().onActivityStateChanged(slaveTop, ActivityStack.ActivityState.RESUMED, "moveActivityToFullScreen");
                }
            }
        }
        return tempActivityList;
    }

    public void adjustActivitiesOrder(ActivityRecord left, ArrayList<ActivityRecord> activities) {
        if (left != null) {
            synchronized (this.mActivityTaskManager.mGlobalLock) {
                int rightIndex = -1;
                ActivityRecord right = null;
                Iterator<ActivityRecord> it = activities.iterator();
                while (it.hasNext()) {
                    ActivityRecord ar = it.next();
                    if (this.mService.isSlave(ar) && ((HwActivityRecord) ar).mCreateTime > ((HwActivityRecord) left).mCreateTime) {
                        rightIndex = left.getTaskRecord().mActivities.indexOf(ar);
                        right = ar;
                    }
                }
                if (rightIndex >= 0) {
                    moveActivityToIndex(left, right, rightIndex);
                }
            }
        }
    }

    private void moveActivityToIndex(ActivityRecord activity, ActivityRecord right, int index) {
        TaskRecord task = activity.getTaskRecord();
        task.removeActivity(activity);
        task.addActivityAtIndex(index, activity);
        task.mTask.positionChildAt(activity.mAppWindowToken, index);
        task.updateEffectiveIntent();
        task.setFrontOfTask();
        ActivityStack targetStack = activity.getActivityStack();
        int lruRightIndex = targetStack.mLRUActivities.indexOf(right);
        if (lruRightIndex >= 0 && targetStack.mLRUActivities.indexOf(activity) > lruRightIndex) {
            targetStack.mLRUActivities.remove(activity);
            targetStack.mLRUActivities.add(lruRightIndex, activity);
        }
    }

    private void updateActivityModeAndBounds(ActivityRecord activityRecord, Rect bounds, int windowMode) {
        ActivityStack activityStack = activityRecord.getActivityStack();
        if (!(activityStack == null || activityStack.getWindowingMode() == windowMode)) {
            activityStack.setWindowingMode(windowMode, false, false, false, true, false);
        }
        if (!activityRecord.finishing) {
            activityRecord.setWindowingMode(windowMode);
            activityRecord.setBounds(bounds);
        }
    }

    public void updateActivityToFullScreenConfiguration(ActivityRecord activityRecord) {
        if (!this.mPolicy.isMainActivity(activityRecord)) {
            synchronized (this.mActivityTaskManager.mGlobalLock) {
                if (activityRecord != null) {
                    updateActivityModeAndBounds(activityRecord, this.mPolicy.mFullScreenBounds, 1);
                    activityRecord.forceNewConfig = this.mService.isReLaunchWhenResize(this.mPolicy.getPackageName(activityRecord));
                    activityRecord.ensureActivityConfiguration(0, true);
                }
            }
        }
    }

    public void moveToMagicWinFromFullscreenForTah(HwActivityRecord focus, HwActivityRecord next) {
        if (focus != null && next != null && this.mService.getMode(this.mPolicy.getPackageName((ActivityRecord) focus)).shouldEnterMagicWinForTah(focus, next)) {
            updateActivityModeAndBounds(focus, this.mService.getBounds(1, this.mPolicy.getPackageName((ActivityRecord) focus)), 103);
            focus.forceNewConfig = this.mService.isReLaunchWhenResize(this.mPolicy.getPackageName((ActivityRecord) focus));
            focus.ensureActivityConfiguration(0, true);
            updateActivityModeAndBounds(next, this.mService.getBounds(2, this.mPolicy.getPackageName((ActivityRecord) next)), 103);
            next.setLastActivityHash(System.identityHashCode(focus));
        }
    }

    public void moveToMagicWinFromFullscreenForMain(HwActivityRecord focus, HwActivityRecord next) {
        if (focus != null && next != null) {
            String nextPkg = this.mPolicy.getPackageName((ActivityRecord) next);
            if (!this.mPolicy.isPkgInLogoffStatus(nextPkg, next.mUserId)) {
                updateActivityModeAndBounds(next, this.mService.getBounds(1, nextPkg), 103);
                next.setLastActivityHash(System.identityHashCode(focus));
                next.forceNewConfig = this.mService.isReLaunchWhenResize(this.mPolicy.getPackageName((ActivityRecord) next));
                next.ensureActivityConfiguration(0, true);
            }
        }
    }

    public void backToFoldFullDisplay() {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            boolean isLand = display.getConfiguration().orientation == 2;
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                String packageName = this.mPolicy.getRealPkgName(stack.getTopActivity());
                if (this.mService.getHwMagicWinEnabled(packageName)) {
                    if (stack.getWindowingMode() == 1) {
                        if (isLand && stack.isFocusedStackOnDisplay()) {
                            this.mStackIdsMoveToMagicWindow.add(Integer.valueOf(stack.mStackId));
                        } else if (!this.mPolicy.isPkgInLogoffStatus(packageName, this.mPolicy.getStackUserId(stack))) {
                            if (!this.mService.isSupportOpenMode(packageName)) {
                                stack.setWindowingMode(103, false, false, false, true, false);
                            }
                            setMagicActivityBound(stack, packageName);
                        }
                    }
                }
            }
            ActivityStack activityStack = this.mPolicy.getFocusedTopStack();
            if (activityStack != null && activityStack.inHwMagicWindowingMode()) {
                activityStack.ensureActivitiesVisibleLocked((ActivityRecord) null, 0, false);
            }
        }
    }

    private void sortActivities(ActivityStack stack) {
        for (int taskNdx = stack.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            Collections.sort(((TaskRecord) stack.mTaskHistory.get(taskNdx)).mActivities, this.mComputor);
        }
    }

    public void clearOverrideBounds(int taskId) {
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            TaskRecord task = this.mActivityTaskManager.mRootActivityContainer.anyTaskForId(taskId, 0);
            if (!(task == null || task.getStack() == null || !task.getStack().inHwMagicWindowingMode())) {
                if (this.mPolicy.isSupportMainRelatedMode(this.mPolicy.getPackageName(task.getStack().getTopActivity()))) {
                    this.mPolicy.removeRelatedActivity(task.getStack());
                }
                setActivityBoundForStack(task.getStack(), null);
            }
        }
    }

    public void clearOverrideBounds(ActivityRecord r) {
        if (r != null && r.inHwMagicWindowingMode()) {
            r.setBounds((Rect) null);
            r.onRequestedOverrideConfigurationChanged(Configuration.EMPTY);
        }
    }

    public void changeLayoutDirection(boolean isRtl) {
        ActivityDisplay display = this.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        synchronized (this.mActivityTaskManager.mGlobalLock) {
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (stack.inHwMagicWindowingMode()) {
                    String packageName = this.mPolicy.getRealPkgName(stack.getTopActivity());
                    if (this.mService.getConfig().isSupportAppTaskSplitScreen(packageName)) {
                        this.mPolicy.forceStopPackage(packageName);
                    } else {
                        Iterator<ActivityRecord> it = this.mPolicy.getAllActivities(stack).iterator();
                        while (it.hasNext()) {
                            ActivityRecord ar = it.next();
                            if (this.mService.isMaster(ar)) {
                                this.mPolicy.setWindowBoundsLocked(ar, this.mService.getBounds(2, this.mPolicy.getRealPkgName(ar)));
                            } else if (this.mService.isSlave(ar)) {
                                this.mPolicy.setWindowBoundsLocked(ar, this.mService.getBounds(1, this.mPolicy.getRealPkgName(ar)));
                            }
                        }
                    }
                }
            }
        }
    }
}
