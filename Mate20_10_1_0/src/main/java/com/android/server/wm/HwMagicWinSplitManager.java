package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.service.voice.IVoiceInteractionSession;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.am.ActivityManagerService;
import com.android.server.magicwin.HwMagicWindowService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HwMagicWinSplitManager {
    private static final int INVALID_STACK_ID = -1;
    private static final String MAGIC_WINDOW_FINISH_EVENT = "activity finish for magicwindow";
    private static final String TAG = "HwMagicWinSplitManager";
    private static final int VALUE_LAST_ACTIVITY_COUTN = 1;
    private static final int VALUE_TOP_ACTIVITY_INDEX = 0;
    private ActivityManagerService mAms;
    private HwMagicWinAmsPolicy mAmsPolicy;
    private HwMagicWindowService mHwMagicWinService = null;
    private ConcurrentHashMap<String, Integer> mMainActivityStackList = new ConcurrentHashMap<>();

    public HwMagicWinSplitManager(ActivityManagerService ams, HwMagicWindowService mws, HwMagicWinAmsPolicy policy) {
        this.mHwMagicWinService = mws;
        this.mAms = ams;
        this.mAmsPolicy = policy;
    }

    private String getKeyStr(String pkgName, ActivityStack stack) {
        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mAmsPolicy;
        return hwMagicWinAmsPolicy.getJoinStr(pkgName, hwMagicWinAmsPolicy.getStackUserId(stack));
    }

    private int getFocusedUserId() {
        HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mAmsPolicy;
        return hwMagicWinAmsPolicy.getStackUserId(hwMagicWinAmsPolicy.getFocusedTopStack());
    }

    private void addStackToSplitScreenList(ActivityStack stack, int position, String pkgName) {
        HwMagicWinCombineManager.getInstance().addStackToSplitScreenList(stack, position, pkgName, this.mAmsPolicy.getStackUserId(stack));
    }

    private void clearSplitScreenList(String pkgName, int userId) {
        HwMagicWinCombineManager.getInstance().clearSplitScreenList(pkgName, userId);
    }

    private void updateSplitScreenForegroundList(String pkgName, int userId) {
        HwMagicWinCombineManager.getInstance().updateForegroundTaskIds(pkgName, userId, this);
    }

    private void removeStackFromSplitScreenList(ActivityStack stack, String pkgName) {
        HwMagicWinCombineManager.getInstance().removeStackFromSplitScreenList(stack, pkgName, this, this.mAmsPolicy.getStackUserId(stack));
    }

    private void takeTaskSnapshot(ActivityRecord topActivity) {
        if (topActivity != null) {
            this.mAms.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity.appToken, false);
        }
    }

    public void updateStackVisibility(List params, Bundle result) {
        int mStackId = ((Integer) params.get(0)).intValue();
        ActivityStack stack = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getStack(mStackId);
        if (stack == null) {
            return;
        }
        if (stack.inHwMagicWindowingMode()) {
            String stackPkg = this.mAmsPolicy.getRealPkgName(stack.getTopActivity());
            String foucsPkg = this.mAmsPolicy.getFocusedStackPackageName();
            int stackUserId = this.mAmsPolicy.getStackUserId(stack);
            if (getFocusedUserId() == stackUserId) {
                int stackPos = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                if (stackPos == 1 || stackPos == 2) {
                    int otherPos = stackPos == 1 ? 2 : 1;
                    ActivityStack currentStack = getMwStackByPosition(stackPos, 0, stackPkg, true, stackUserId);
                    ActivityStack otherStack = getMwStackByPosition(otherPos, 0, stackPkg, false, stackUserId);
                    if (otherStack != null) {
                        if (!otherStack.isTopActivityVisible()) {
                            if (!stackPkg.equals(foucsPkg)) {
                                return;
                            }
                            if (!isSpliteModeStack(this.mAmsPolicy.getFocusedTopStack())) {
                                return;
                            }
                        }
                        if (currentStack != null && currentStack.mStackId == mStackId) {
                            result.putBoolean("RESULT_STACK_VISIBILITY", true);
                        }
                    }
                }
            }
        }
    }

    public ActivityStack getMwStackByPosition(int windowPosition, int windowIndex, String pkg, boolean isNeedCheckUnderHomeStack) {
        return getMwStackByPosition(windowPosition, windowIndex, pkg, isNeedCheckUnderHomeStack, getFocusedUserId());
    }

    public ActivityStack getMwStackByPosition(int windowPosition, int windowIndex, String pkg, boolean isNeedCheckUnderHomeStack, int userId) {
        int offsetIndex = 0;
        Iterator<ActivityStack> it = getWindowModeOrAllStack(pkg, false, isNeedCheckUnderHomeStack, userId).iterator();
        while (it.hasNext()) {
            ActivityStack stack = it.next();
            if (windowPosition == this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds())) {
                if (offsetIndex == windowIndex) {
                    return stack;
                }
                offsetIndex++;
            }
        }
        return null;
    }

    public void addOrUpdateMainActivityStat(ActivityRecord ar) {
        if (this.mAmsPolicy.isMainActivity(ar) || this.mAmsPolicy.isRelatedActivity(ar)) {
            ConcurrentHashMap<String, Integer> concurrentHashMap = this.mMainActivityStackList;
            HwMagicWinAmsPolicy hwMagicWinAmsPolicy = this.mAmsPolicy;
            concurrentHashMap.put(hwMagicWinAmsPolicy.getJoinStr(hwMagicWinAmsPolicy.getPackageName(ar), ar.mUserId), Integer.valueOf(ar.getStackId()));
        }
    }

    public void quitMagicSplitScreenMode(String pkgName, int skipStackId, boolean isClearStack, int userId) {
        clearSplitScreenList(pkgName, userId);
        ArrayList<ActivityStack> stacksInSamePkg = getWindowModeOrAllStack(pkgName, false, true, userId);
        synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
            this.mAms.mWindowManager.deferSurfaceLayout();
            ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
            ActivityStack mainStack = getMainActivityStack(pkgName, userId);
            if (!(mainStack == null || !stacksInSamePkg.contains(mainStack) || mainStack.mStackId == skipStackId)) {
                if (isClearStack) {
                    display.moveStackBehindStack(mainStack, display.getHomeStack());
                }
                this.mHwMagicWinService.getMode(pkgName).setActivityBoundByMode(this.mAmsPolicy.getAllActivities(mainStack), pkgName);
                this.mAms.resizeStack(mainStack.mStackId, this.mHwMagicWinService.getBounds(3, pkgName), false, false, false, 0);
            }
            Iterator<ActivityStack> it = stacksInSamePkg.iterator();
            while (it.hasNext()) {
                ActivityStack stack = it.next();
                int position = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                if (position == 1 || position == 2) {
                    if (stack.mStackId != skipStackId) {
                        if (stack != mainStack) {
                            if (!isClearStack || !isNeedNewTaskStack(stack)) {
                                if (isClearStack) {
                                    display.moveStackBehindStack(stack, display.getHomeStack());
                                }
                                moveStackToPostion(stack, 3, pkgName);
                                combineStackToMainStack(stack, mainStack);
                            } else {
                                this.mAms.mActivityTaskManager.mStackSupervisor.removeStack(stack);
                            }
                        }
                    }
                }
            }
            if (!isClearStack && pkgName != null && pkgName.equals(this.mAmsPolicy.getFocusedStackPackageName())) {
                updateSystemUiVisibility(this.mAmsPolicy.getFocusedTopStack());
            }
            this.mAms.mWindowManager.continueSurfaceLayout();
        }
        this.mHwMagicWinService.getUIController().updateBgColor();
    }

    public boolean isNeedProcessCombineStack(ActivityStack stack, boolean isNeedChangeBound) {
        ActivityStack mainStack;
        if (stack == null || !isNeedNewTaskStack(stack) || (mainStack = getMainActivityStack(stack.getTopActivity())) == null || mainStack == stack) {
            return false;
        }
        if (isNeedChangeBound) {
            moveStackToPostion(stack, 3, this.mAmsPolicy.getRealPkgName(stack.getTopActivity()));
        }
        combineStackToMainStack(stack, mainStack);
        return true;
    }

    private void combineStackToMainStack(ActivityStack stack, ActivityStack toStack) {
        if (toStack != null && isNeedNewTaskStack(stack) && stack != toStack) {
            TaskRecord topTask = toStack.topTask();
            ArrayList<TaskRecord> tasks = stack.getAllTasks();
            for (int i = 0; i < tasks.size(); i++) {
                TaskRecord task = tasks.get(i);
                boolean isTopTask = true;
                if (i != tasks.size() - 1) {
                    isTopTask = false;
                }
                task.reparent(toStack, true, 1, isTopTask, true, true, "reparent for quit magic window split mode");
                if (isTopTask) {
                    int adjustPostion = 0;
                    if (isVoipActivity(topTask.getTopActivity())) {
                        adjustPostion = 1;
                    }
                    Iterator<ActivityRecord> it = new ArrayList<>(task.mActivities).iterator();
                    while (it.hasNext()) {
                        it.next().reparent(topTask, topTask.getChildCount() - adjustPostion, "reparent for quit magic window split mode");
                    }
                    task.mService.mRecentTasks.remove(task);
                    task.mService.mRecentTasks.add(topTask);
                    toStack.removeTask(task, "Remove for quit magic window split mode", 0);
                    this.mAms.mActivityTaskManager.mStackSupervisor.removeTaskByIdLocked(task.taskId, true, true, true, "Remove for quit magic window split mode");
                }
            }
            this.mAms.mActivityTaskManager.mStackSupervisor.removeStack(stack);
        }
    }

    public void showMoveAnimation(ActivityRecord activityRecord, int rightCheckPosition) {
        String pkgName = this.mAmsPolicy.getRealPkgName(activityRecord);
        if (this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName) && !this.mAmsPolicy.isRelatedActivity(activityRecord)) {
            int position = this.mHwMagicWinService.getBoundsPosition(activityRecord.getActivityStack().getRequestedOverrideBounds());
            ActivityStack leftTopStack = getMwStackByPosition(1, 0, pkgName, false, activityRecord.mUserId);
            ActivityStack rightUnderStack = getMwStackByPosition(2, rightCheckPosition, pkgName, false, activityRecord.mUserId);
            if (position == 2 && rightUnderStack == null && activityRecord.getActivityStack() != leftTopStack && leftTopStack != null && !this.mAmsPolicy.isMainActivity(leftTopStack.getTopActivity())) {
                if (!isMainStack(pkgName, leftTopStack) || hasDefaultFullscreenActivity(pkgName, leftTopStack)) {
                    this.mHwMagicWinService.getWmsPolicy().startMoveAnimationFullScreen(leftTopStack.getTopActivity().appToken, activityRecord.appToken);
                } else {
                    this.mHwMagicWinService.getWmsPolicy().startMoveAnimation(leftTopStack.getTopActivity().appToken, activityRecord.appToken, pkgName, false);
                }
            }
        }
    }

    private boolean hasDefaultFullscreenActivity(String packageName, ActivityStack stack) {
        Iterator<ActivityRecord> it = this.mAmsPolicy.getAllActivities(stack).iterator();
        while (it.hasNext()) {
            if (this.mHwMagicWinService.getConfig().isDefaultFullscreenActivity(packageName, this.mAmsPolicy.getClassName(it.next()))) {
                return true;
            }
        }
        return false;
    }

    private ActivityStack createStackForSplit(ActivityRecord activity, ActivityStack stack) {
        synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
            TaskRecord newTask = stack.createTaskRecord(this.mAms.mActivityTaskManager.getStackSupervisor().getNextTaskIdForUserLocked(activity.mUserId), activity.info, activity.intent, (IVoiceInteractionSession) null, (IVoiceInteractor) null, true);
            if (newTask == null) {
                return null;
            }
            activity.reparent(newTask, 0, "magicwin move activity to new task for split mode");
            ActivityOptions tmpOptions = ActivityOptions.makeBasic();
            tmpOptions.setLaunchWindowingMode(103);
            ActivityStack newStack = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getOrCreateStack((ActivityRecord) null, tmpOptions, newTask, newTask.getActivityType(), true);
            if (newStack != stack) {
                if (newStack != null) {
                    newTask.reparent(newStack, true, 0, true, true, true, "magicwin move task to new stack for split mode");
                    if (newStack instanceof HwActivityStack) {
                        ((HwActivityStack) newStack).isMwNewTaskSplitStack = true;
                    }
                    return newStack;
                }
            }
            return null;
        }
    }

    public void setTaskPosition(String pkg, int taskId, int targetPosition) {
        ActivityStack stack = this.mAmsPolicy.getFocusedTopStack();
        int newTaskId = taskId;
        Slog.i(TAG, "setTaskPosition : stack = " + stack + ", pkg = " + pkg);
        if (stack != null && pkg != null && this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkg)) {
            int position = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
            ActivityStack mainStack = getMainActivityStack(stack.getTopActivity());
            if (mainStack != null && mainStack.inHwMagicWindowingMode() && targetPosition != 1 && position != targetPosition) {
                if (mainStack == stack) {
                    ActivityRecord top = stack.getTopActivity();
                    if (this.mHwMagicWinService.getConfig().isNeedStartByNewTaskActivity(pkg, this.mAmsPolicy.getClassName(top)) && (stack = createStackForSplit(top, stack)) != null) {
                        newTaskId = stack.topTask().taskId;
                    } else {
                        return;
                    }
                }
                showTaskMoveAnimation(pkg, stack, targetPosition, position);
                if (pkg.equals(this.mAmsPolicy.getFocusedStackPackageName()) && stack.isInStackLocked(stack.taskForIdLocked(newTaskId))) {
                    if (targetPosition == 5 || targetPosition == 0 || targetPosition == 3) {
                        Slog.i(TAG, "setTaskPosition quit split mode");
                        quitMagicSplitScreenMode(pkg, -1, false, this.mAmsPolicy.getStackUserId(stack));
                        return;
                    }
                    Slog.i(TAG, "setTaskPosition function: call resizeStack");
                    adjustBackgroundStackPosition(pkg, stack, this.mAmsPolicy.getStackUserId(stack));
                    this.mHwMagicWinService.getUIController().updateBgColor();
                }
            }
        }
    }

    private void updateSystemUiVisibility(ActivityStack stack) {
        if (stack != null && stack.getDisplay() != null && stack.getDisplay().mDisplayContent != null && stack.getDisplay().mDisplayContent.getDisplayPolicy() != null) {
            stack.getDisplay().mDisplayContent.getDisplayPolicy().resetSystemUiVisibilityLw();
        }
    }

    private void showTaskMoveAnimation(String pkg, ActivityStack stack, int targetPosition, int position) {
        ActivityStack exitStack;
        if ((position == 5 || position == 3) && targetPosition == 2) {
            if (stack.getTopActivity() != null) {
                this.mHwMagicWinService.getWmsPolicy().startSplitAnimation(stack.getTopActivity().appToken, pkg);
            }
        } else if ((targetPosition == 5 || targetPosition == 0 || targetPosition == 3) && position == 2) {
            if (stack.getTopActivity() != null) {
                this.mHwMagicWinService.getWmsPolicy().startExitSplitAnimation(stack.getTopActivity().appToken, 0.0f);
            }
        } else if (targetPosition == 2 && position == 1) {
            ActivityRecord enterRecord = stack.getTopActivity();
            if (enterRecord != null && (exitStack = getMwStackByPosition(2, 0, pkg, false, enterRecord.mUserId)) != null && exitStack.getTopActivity() != null) {
                this.mHwMagicWinService.getWmsPolicy().startMoveAnimation(enterRecord.appToken, exitStack.getTopActivity().appToken, pkg, true);
            }
        } else {
            Slog.d(TAG, "not need show animation");
        }
    }

    private ArrayList<ActivityStack> getWindowModeOrAllStack(String pkg, boolean isAll, boolean getUnderHomeStacks, int userId) {
        String stackPkg;
        ArrayList<ActivityStack> stacks = new ArrayList<>();
        if (pkg == null) {
            return stacks;
        }
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        for (int i = display.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack otherStack = (ActivityStack) display.mStacks.get(i);
            if (otherStack.mStackId == display.getHomeStack().mStackId && !getUnderHomeStacks) {
                break;
            }
            if ((otherStack.getWindowingMode() == 103 || isAll) && (stackPkg = this.mAmsPolicy.getRealPkgName(otherStack.getTopActivity())) != null && stackPkg.equals(pkg) && userId == this.mAmsPolicy.getStackUserId(otherStack)) {
                stacks.add(otherStack);
            }
        }
        return stacks;
    }

    public void multWindowModeProcess(ActivityRecord focus, int windowMode) {
        ActivityStack stack;
        if (focus != null) {
            String focusPkg = this.mAmsPolicy.getRealPkgName(focus);
            if (isSpliteModeStack(focus.getActivityStack())) {
                if (windowMode == 100) {
                    ActivityStack stack2 = getMwStackByPosition(2, 0, focusPkg, false, focus.mUserId);
                    if (stack2 != null) {
                        quitMagicSplitScreenMode(focusPkg, stack2.mStackId, true, focus.mUserId);
                        synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
                            stack2.moveToFront("move split right to top");
                            stack2.setWindowingMode(101);
                        }
                    }
                } else if (windowMode == 101 && (stack = getMwStackByPosition(1, 0, focusPkg, false, focus.mUserId)) != null) {
                    quitMagicSplitScreenMode(focusPkg, stack.mStackId, true, focus.mUserId);
                    synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
                        stack.moveToFront("move split left to top");
                        stack.setWindowingMode(100);
                    }
                }
            }
        }
    }

    private void adjustBackgroundStackPosition(String pkg, ActivityStack currentStack, int userId) {
        ArrayList<ActivityStack> stacks = getWindowModeOrAllStack(pkg, true, true, userId);
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        ActivityStack lastLeftStack = currentStack;
        ActivityStack mainStack = null;
        ActivityStack topLeftStack = null;
        ArrayList<ActivityStack> rightStackList = new ArrayList<>();
        synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
            this.mAms.mWindowManager.deferSurfaceLayout();
            for (int i = stacks.size() - 1; i >= 0; i--) {
                ActivityStack stack = stacks.get(i);
                int stackPos = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                if (stack != currentStack) {
                    if (stackPos == 5 || stackPos == 3) {
                        if (display.getIndexOf(stack) <= display.getIndexOf(display.getHomeStack())) {
                            if (isMainStack(pkg, stack)) {
                            }
                        }
                        moveStackToPostion(stack, 1, pkg);
                        addStackToSplitScreenList(stack, 1, pkg);
                        if (isMainStack(pkg, stack)) {
                            this.mAmsPolicy.removeRelatedActivity(stack);
                            mainStack = stack;
                        } else {
                            topLeftStack = stack;
                            if (lastLeftStack == currentStack) {
                                lastLeftStack = stack;
                            }
                        }
                    }
                    if (stackPos == 2 && stack.inHwMagicWindowingMode()) {
                        rightStackList.add(stack);
                        removeStackFromSplitScreenList(stack, pkg);
                        display.moveStackBehindStack(stack, display.getHomeStack());
                    } else if (stackPos != 1 || !stack.inHwMagicWindowingMode()) {
                        Slog.d(TAG, "adjustBackgroundStackPosition stack not need move");
                    } else {
                        stack.moveToFront("move left stack under home to front for start split mode");
                    }
                }
            }
            if (mainStack != null) {
                finishActivitiesExceptMainAndRelated(mainStack);
                display.moveStackBehindStack(mainStack, lastLeftStack);
                if (topLeftStack == null) {
                    topLeftStack = mainStack;
                }
            }
            stackPostProcess(rightStackList, topLeftStack, pkg, currentStack);
            updateSystemUiVisibility(currentStack);
            this.mAms.mWindowManager.continueSurfaceLayout();
        }
    }

    private void stackPostProcess(ArrayList<ActivityStack> rightStackList, ActivityStack topLeftStack, String pkg, ActivityStack currentStack) {
        moveStackToPostion(currentStack, 2, pkg);
        addStackToSplitScreenList(currentStack, 2, pkg);
        Iterator<ActivityStack> it = rightStackList.iterator();
        while (it.hasNext()) {
            ActivityStack rightStack = it.next();
            if (isNeedDestroyWhenReplaceOnRightStack(rightStack)) {
                this.mAms.mActivityTaskManager.mStackSupervisor.removeStack(rightStack);
            } else {
                takeTaskSnapshot(rightStack.getTopActivity());
                moveStackToPostion(rightStack, 3, pkg);
            }
        }
        if (topLeftStack != null) {
            topLeftStack.ensureActivitiesVisibleLocked((ActivityRecord) null, 0, false);
        }
    }

    private void finishActivitiesExceptMainAndRelated(ActivityStack stack) {
        ArrayList<ActivityRecord> records = this.mAmsPolicy.getAllActivities(stack);
        if (records != null && records.size() >= 1) {
            int mainIndex = records.size();
            for (int index = 0; index < records.size(); index++) {
                ActivityRecord record = records.get(index);
                if (this.mAmsPolicy.isMainActivity(record)) {
                    mainIndex = index;
                } else if (mainIndex != records.size() && !this.mAmsPolicy.isRelatedActivity(record)) {
                    stack.finishActivityLocked(record, 0, (Intent) null, "activity finish for magicwindow", true, false);
                }
            }
        }
    }

    private boolean isNeedDestroyWhenReplaceOnRightStack(ActivityStack stack) {
        if (!isNeedNewTaskStack(stack)) {
            return false;
        }
        Iterator<ActivityRecord> it = this.mAmsPolicy.getAllActivities(stack).iterator();
        while (it.hasNext()) {
            if (isNeedDestroyWhenReplaceOnRight(it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNeedDestroyWhenReplaceOnRight(ActivityRecord activity) {
        return this.mHwMagicWinService.getConfig().isNeedDestroyWhenReplaceOnRight(this.mAmsPolicy.getPackageName(activity), this.mAmsPolicy.getClassName(activity));
    }

    private boolean isNeedNewTaskActivity(ActivityRecord activity) {
        return this.mHwMagicWinService.getConfig().isNeedStartByNewTaskActivity(this.mAmsPolicy.getPackageName(activity), this.mAmsPolicy.getClassName(activity));
    }

    public boolean isNeedNewTaskStack(ActivityStack stack) {
        if (stack instanceof HwActivityStack) {
            return ((HwActivityStack) stack).isMwNewTaskSplitStack;
        }
        return false;
    }

    public boolean isVoipActivity(ActivityRecord activity) {
        return isNeedNewTaskActivity(activity) && !isNeedDestroyWhenReplaceOnRight(activity);
    }

    public void moveTaskToFullscreenIfNeed(ActivityRecord currentActivity, boolean isMoveStack) {
        ActivityStack currentStack = currentActivity.getActivityStack();
        String pkg = this.mAmsPolicy.getRealPkgName(currentActivity);
        if (!this.mAmsPolicy.isFoldedState() && this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkg) && currentStack != null && !this.mAmsPolicy.isRelatedActivity(currentActivity)) {
            if (isMoveStack) {
                moveStackToPostion(currentStack, 3, pkg);
            }
            ArrayList<ActivityRecord> tempActivityList = this.mAmsPolicy.getAllActivities(currentStack);
            if ((tempActivityList.size() == 1 && tempActivityList.get(0) == currentActivity) || isMoveStack) {
                removeStackFromSplitScreenList(currentActivity.getActivityStack(), pkg);
            }
            ActivityStack topLeftStack = null;
            Iterator<ActivityStack> it = getWindowModeOrAllStack(pkg, false, true, currentActivity.mUserId).iterator();
            while (it.hasNext()) {
                ActivityStack stack = it.next();
                if (currentStack.mStackId != stack.mStackId || currentStack.numActivities() > 1) {
                    int stackPos = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                    if (stackPos == 1 && topLeftStack == null) {
                        topLeftStack = stack;
                    }
                    if (stackPos == 2) {
                        return;
                    }
                }
            }
            if (topLeftStack != null) {
                this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
                if (isMainStack(pkg, topLeftStack)) {
                    clearSplitScreenList(pkg, currentActivity.mUserId);
                    if (!isMoveStack && currentActivity.isTopRunningActivity()) {
                        topLeftStack.moveToFront("move main stack to top");
                    }
                    this.mHwMagicWinService.getMode(pkg).setActivityBoundByMode(this.mAmsPolicy.getAllActivities(topLeftStack), pkg);
                    this.mAms.resizeStack(topLeftStack.mStackId, this.mHwMagicWinService.getBounds(3, pkg), false, false, false, 0);
                } else {
                    quitMagicSplitScreenMode(pkg, isMoveStack ? -1 : currentStack.mStackId, false, currentActivity.mUserId);
                }
                this.mHwMagicWinService.getUIController().updateBgColor();
            }
        }
    }

    private void moveStackToPostion(ActivityStack stack, int position, String pkg) {
        if (stack.getWindowingMode() != 103) {
            stack.setWindowingMode(103);
        }
        Rect bound = new Rect(this.mHwMagicWinService.getBounds(position, pkg));
        this.mAms.mWindowManager.deferSurfaceLayout();
        if (!isMainStack(pkg, stack) || position == 1 || position == 2) {
            this.mHwMagicWinService.getConfig().adjustSplitBound(position, bound);
            Iterator<TaskRecord> it = stack.getAllTasks().iterator();
            while (it.hasNext()) {
                Iterator<ActivityRecord> it2 = it.next().mActivities.iterator();
                while (it2.hasNext()) {
                    it2.next().setBounds(bound);
                }
            }
        }
        this.mAms.resizeStack(stack.mStackId, bound, false, false, false, 0);
        this.mAms.mWindowManager.continueSurfaceLayout();
    }

    public void resizeStackIfNeedOnresume(ActivityRecord resumeActivity) {
        ActivityStack mainStack;
        String pkgName = this.mAmsPolicy.getRealPkgName(resumeActivity);
        ActivityStack stack = resumeActivity.getActivityStack();
        if (pkgName != null && this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName) && (mainStack = getMainActivityStack(resumeActivity)) != null && mainStack != stack && isPkgSpliteScreenMode(resumeActivity, true)) {
            int targetPosition = this.mHwMagicWinService.getBoundsPosition(resumeActivity.getRequestedOverrideBounds());
            if ((targetPosition == 1 || targetPosition == 2) && targetPosition != this.mHwMagicWinService.getBoundsPosition(stack.getBounds())) {
                moveStackToPostion(stack, targetPosition, pkgName);
                addStackToSplitScreenList(stack, targetPosition, pkgName);
            }
        }
    }

    public boolean isMainStack(String pkg, ActivityStack stack) {
        if (!this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkg) || stack == null) {
            return false;
        }
        ActivityStack mainStack = getMainActivityStack(stack.getTopActivity());
        Slog.i(TAG, "isMainStack mainStack " + mainStack + " stack " + stack);
        if (mainStack == null || mainStack.mStackId != stack.mStackId) {
            return false;
        }
        return true;
    }

    private ActivityStack getMainActivityStack(ActivityRecord ar) {
        String pkg = this.mAmsPolicy.getRealPkgName(ar);
        if (pkg == null) {
            return null;
        }
        return getMainActivityStack(pkg, ar.mUserId);
    }

    public ActivityStack getMainActivityStack(String pkgName, int userId) {
        Integer stackId = this.mMainActivityStackList.get(this.mAmsPolicy.getJoinStr(pkgName, userId));
        if (stackId == null) {
            return null;
        }
        return this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getStack(stackId.intValue());
    }

    public boolean isMainStackInMwMode(ActivityRecord ar) {
        ActivityStack stack = getMainActivityStack(ar);
        return stack != null && stack.inHwMagicWindowingMode();
    }

    public boolean isPkgSpliteScreenMode(ActivityRecord ar, boolean checkUnderHomeStacks) {
        ActivityStack mainStack;
        String pkg = this.mAmsPolicy.getRealPkgName(ar);
        if (pkg == null || !this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkg) || (mainStack = getMainActivityStack(ar)) == null || this.mHwMagicWinService.getBoundsPosition(mainStack.getRequestedOverrideBounds()) != 1) {
            return false;
        }
        Iterator<ActivityStack> it = getWindowModeOrAllStack(pkg, false, checkUnderHomeStacks, ar.mUserId).iterator();
        while (it.hasNext()) {
            if (this.mHwMagicWinService.getBoundsPosition(it.next().getRequestedOverrideBounds()) == 2) {
                return true;
            }
        }
        return false;
    }

    public void updateSpliteStackSequence(ActivityDisplay display) {
        String packageName = this.mAmsPolicy.getFocusedStackPackageName();
        if (this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(packageName)) {
            ActivityStack voipStack = null;
            ArrayList<ActivityStack> stacks = getWindowModeOrAllStack(packageName, false, false, getFocusedUserId());
            int i = stacks.size();
            while (true) {
                i--;
                if (i < 0) {
                    break;
                }
                ActivityStack stack = stacks.get(i);
                if (isVoipActivity(stack.getTopActivity())) {
                    voipStack = stack;
                } else if (this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds()) == 2) {
                    stack.moveToFront("set magic window stack to focus");
                }
            }
            if (voipStack != null) {
                voipStack.moveToFront("set magic window stack to focus");
            }
        }
    }

    public void setAboveStackToDefault(ActivityStack targetStack, int targetPosition) {
        String pkgName = this.mAmsPolicy.getRealPkgName(targetStack.getTopActivity());
        if (isPkgSpliteScreenMode(targetStack.getTopActivity(), true) && isMainStack(pkgName, targetStack)) {
            ArrayList<ActivityStack> stacks = getWindowModeOrAllStack(pkgName, false, true, this.mAmsPolicy.getStackUserId(targetStack));
            synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
                ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
                for (int i = stacks.size() - 1; i >= 0; i--) {
                    ActivityStack stack = stacks.get(i);
                    int stackPos = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                    if (stack != targetStack) {
                        if (stackPos == targetPosition) {
                            if (isNeedDestroyWhenReplaceOnRightStack(stack)) {
                                this.mAms.mActivityTaskManager.mStackSupervisor.removeStack(stack);
                            } else {
                                moveStackToPostion(stack, 3, pkgName);
                                display.moveStackBehindStack(stack, targetStack);
                            }
                            removeStackFromSplitScreenList(stack, pkgName);
                        }
                    }
                }
            }
        }
    }

    private List<ActivityStack> getAllStackUnderHomeOfPkg(String pkgName, int userId, ActivityStack mainStack) {
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        boolean isUnderHome = false;
        List<ActivityStack> underHomeStacks = new ArrayList<>();
        for (int i = display.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack stack = (ActivityStack) display.mStacks.get(i);
            if (stack.mStackId == display.getHomeStack().mStackId) {
                isUnderHome = true;
            } else if (this.mAmsPolicy.getStackUserId(stack) == userId && pkgName.equals(this.mAmsPolicy.getRealPkgName(stack.getTopActivity())) && isUnderHome && isSpliteModeStack(stack)) {
                underHomeStacks.add(stack);
            }
        }
        return underHomeStacks;
    }

    private void moveSplitStacksToFront(List<ActivityStack> stacks, ActivityStack resumeStack, ActivityStack mainStack, String pkgName) {
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
            if (resumeStack == mainStack) {
                if (isSpliteModeStack(resumeStack)) {
                    ActivityStack otherSideStack = getMwStackByPosition(2, 0, pkgName, true);
                    if (otherSideStack != null && stacks.contains(otherSideStack)) {
                        this.mAms.mActivityTaskManager.startActivityFromRecents(otherSideStack.topTask().taskId, (Bundle) null);
                    }
                    return;
                }
            }
            ActivityStack lastLeftStack = null;
            for (int i = stacks.size() - 1; i >= 0; i--) {
                ActivityStack stack = stacks.get(i);
                int stackPosition = this.mHwMagicWinService.getBoundsPosition(stack.getBounds());
                if (isSpliteModeStack(stack) && stack != mainStack) {
                    display.moveStackBehindStack(stack, resumeStack);
                    if (stackPosition == 1 && lastLeftStack == null) {
                        lastLeftStack = stack;
                    }
                }
            }
            setLeftStackFocued(resumeStack, pkgName, mainStack, lastLeftStack);
        }
    }

    private void setLeftStackFocued(ActivityStack resumeStack, String pkgName, ActivityStack mainStack, ActivityStack lastLeftStack) {
        ActivityStack otherSideStack;
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        if (isSpliteModeStack(mainStack)) {
            if (lastLeftStack == null) {
                display.moveStackBehindStack(mainStack, resumeStack);
            } else {
                display.moveStackBehindStack(mainStack, lastLeftStack);
            }
            if (this.mHwMagicWinService.isSlave(resumeStack.getTopActivity()) && (otherSideStack = getMwStackByPosition(1, 0, pkgName, false)) != null) {
                otherSideStack.mService.setFocusedStack(otherSideStack.mStackId);
            }
        }
    }

    public void resizeStackWhileResumeSplitAppIfNeed(String pkgName, ActivityRecord activity) {
        ActivityStack mainStack = getMainActivityStack(activity);
        if (mainStack != null && this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName)) {
            updateSplitScreenForegroundList(pkgName, activity.mUserId);
            if (!isSpliteModeStack(activity.getActivityStack()) || isPkgSpliteScreenMode(activity, true)) {
                List<ActivityStack> underHomeStacks = getAllStackUnderHomeOfPkg(pkgName, activity.mUserId, mainStack);
                if (!underHomeStacks.isEmpty() && !underHomeStacks.contains(activity.getActivityStack()) && isSpliteModeStack(activity.getActivityStack())) {
                    moveSplitStacksToFront(underHomeStacks, activity.getActivityStack(), mainStack, pkgName);
                    return;
                }
                return;
            }
            Slog.d(TAG, " resizeMainStackWhileResumeIfNeed quit split mode: " + activity);
            quitMagicSplitScreenMode(pkgName, -1, false, activity.mUserId);
        }
    }

    public void resizeSplitStackBeforeResume(ActivityRecord activity, String pkgName) {
        ActivityStack resumeStack = activity.getActivityStack();
        ActivityStack mainStack = getMainActivityStack(activity);
        if (mainStack != null && mainStack.mStackId != resumeStack.mStackId && isPkgSpliteScreenMode(activity, true)) {
            boolean isMainStackAboveHome = false;
            ActivityStack topSplitStack = null;
            Iterator<ActivityStack> it = getWindowModeOrAllStack(pkgName, false, false, activity.mUserId).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityStack stack = it.next();
                int stackPosition = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
                if (topSplitStack == null && (stackPosition == 2 || stackPosition == 1)) {
                    topSplitStack = stack;
                }
                if (stack.mStackId == mainStack.mStackId) {
                    isMainStackAboveHome = true;
                    break;
                }
            }
            if (isMainStackAboveHome && topSplitStack != null) {
                ActivityStack topRightStack = getMwStackByPosition(2, 0, pkgName, false, activity.mUserId);
                int targetPosition = this.mHwMagicWinService.getBoundsPosition(topSplitStack.getRequestedOverrideBounds());
                if (topRightStack == null || topRightStack == resumeStack) {
                    targetPosition = 2;
                }
                if (targetPosition != this.mHwMagicWinService.getBoundsPosition(resumeStack.getRequestedOverrideBounds())) {
                    moveStackToPostion(resumeStack, targetPosition, pkgName);
                    addStackToSplitScreenList(resumeStack, targetPosition, pkgName);
                }
            } else if (resumeStack.getWindowingMode() == 103) {
                moveTaskToFullscreenIfNeed(activity, true);
            }
        }
    }

    public void resizeWhenMoveBackIfNeed(int stackId) {
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        ActivityStack stack = display.getStack(stackId);
        if (stack != null) {
            String pkgName = this.mAmsPolicy.getRealPkgName(stack.getTopActivity());
            if (this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName) && stack.inHwMagicWindowingMode()) {
                int position = this.mHwMagicWinService.getBoundsPosition(stack.getBounds());
                if (position == 1 && isMainStack(pkgName, stack)) {
                    synchronized (this.mAms.mActivityTaskManager.mGlobalLock) {
                        Iterator<ActivityStack> it = getWindowModeOrAllStack(pkgName, false, false, this.mAmsPolicy.getStackUserId(stack)).iterator();
                        while (it.hasNext()) {
                            ActivityStack currentStack = it.next();
                            if (this.mHwMagicWinService.getBoundsPosition(currentStack.getRequestedOverrideBounds()) == 2) {
                                display.moveStackBehindStack(currentStack, stack);
                            }
                        }
                    }
                    removeStackFromSplitScreenList(stack, pkgName);
                } else if (position == 1 || position == 2) {
                    this.mAms.mWindowManager.deferSurfaceLayout();
                    if (position == 2) {
                        showMoveAnimation(stack.getTopActivity(), 0);
                    }
                    ActivityRecord topRecord = stack.getTopActivity();
                    takeTaskSnapshot(topRecord);
                    moveTaskToFullscreenIfNeed(topRecord, true);
                    this.mAms.mWindowManager.continueSurfaceLayout();
                }
            }
        }
    }

    public boolean isInAppSplite(int stackId, boolean isUnderHomeStacks) {
        ActivityStack topStack = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay().getStack(stackId);
        if (topStack == null) {
            return false;
        }
        String pkgName = this.mAmsPolicy.getRealPkgName(topStack.getTopActivity());
        if (!this.mHwMagicWinService.getConfig().isSupportAppTaskSplitScreen(pkgName)) {
            return false;
        }
        boolean ret = isPkgSpliteScreenMode(topStack.getTopActivity(), isUnderHomeStacks);
        Slog.i(TAG, "isInAppSplite:ret " + ret + " pkgName " + pkgName);
        return ret;
    }

    public ActivityRecord getLatestActivityBySplitMode(String packageName, ActivityStack stack, ActivityRecord topActivity, ActivityRecord latestActivity) {
        ActivityStack aboveStack;
        if (!stack.inHwMagicWindowingMode()) {
            return latestActivity;
        }
        if (isSpliteModeStack(stack)) {
            return topActivity;
        }
        ActivityDisplay display = this.mAms.mActivityTaskManager.mRootActivityContainer.getDefaultDisplay();
        if (!isMainStack(packageName, stack)) {
            return topActivity;
        }
        int mainIndex = display.getIndexOf(stack);
        if (mainIndex >= display.mStacks.size() - 1 || (aboveStack = (ActivityStack) display.mStacks.get(mainIndex + 1)) == null || !packageName.equals(this.mAmsPolicy.getRealPkgName(aboveStack.getTopActivity())) || stack.mLRUActivities.size() <= 0 || this.mAmsPolicy.getStackUserId(stack) != topActivity.mUserId) {
            return latestActivity;
        }
        return (ActivityRecord) stack.mLRUActivities.get(stack.mLRUActivities.size() - 1);
    }

    public boolean isSpliteModeStack(ActivityStack stack) {
        int stackPos = this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds());
        if ((stackPos == 1 || stackPos == 2) && stack.inHwMagicWindowingMode()) {
            return true;
        }
        return false;
    }

    public ActivityStack getNewTopStack(ActivityStack oldStack, int otherSideModeToChange) {
        ActivityStack newTopTask;
        String pkgName = this.mAmsPolicy.getRealPkgName(oldStack.getTopActivity());
        if (!isPkgSpliteScreenMode(oldStack.getTopActivity(), false)) {
            return null;
        }
        int oldStackMWPos = this.mHwMagicWinService.getBoundsPosition(oldStack.getRequestedOverrideBounds());
        int position = otherSideModeToChange == 100 ? 1 : 2;
        if (oldStackMWPos == position || (newTopTask = getMwStackByPosition(position, 0, pkgName, false)) == null || oldStack.equals(newTopTask)) {
            return null;
        }
        newTopTask.moveToFront("move new top to front");
        return newTopTask;
    }

    public void addOtherSnapShot(ActivityStack stack, HwActivityTaskManagerServiceEx HwAtmsEx, List<ActivityManager.TaskSnapshot> snapShots) {
        String pkgName = this.mAmsPolicy.getRealPkgName(stack.getTopActivity());
        if (isPkgSpliteScreenMode(stack.getTopActivity(), false)) {
            int otherTaskPos = 2;
            if (this.mHwMagicWinService.getBoundsPosition(stack.getRequestedOverrideBounds()) == 2) {
                otherTaskPos = 1;
            }
            ActivityStack otherStack = getMwStackByPosition(otherTaskPos, 0, pkgName, false);
            TaskRecord otherMWTask = otherStack != null ? otherStack.topTask() : null;
            if (otherMWTask != null) {
                ActivityManager.TaskSnapshot shot = HwAtmsEx.getTaskSnapshot(otherMWTask.taskId, false);
                if (shot != null && otherTaskPos == 1) {
                    snapShots.add(0, shot);
                } else if (shot != null) {
                    snapShots.add(shot);
                } else {
                    Slog.d(TAG, "not snapShot found");
                }
            }
        }
    }
}
