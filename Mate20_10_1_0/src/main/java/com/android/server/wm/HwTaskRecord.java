package com.android.server.wm;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.HwPCMultiWindowCompatibility;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Process;
import android.os.SystemProperties;
import android.service.voice.IVoiceInteractionSession;
import android.util.HwPCUtils;
import android.util.Log;
import com.android.internal.app.IVoiceInteractor;
import com.huawei.server.HwPCFactory;
import java.util.ArrayList;

public class HwTaskRecord extends TaskRecord {
    private boolean mIsInLockScreen = false;
    private boolean mIsLaunchBoundsFirst = true;
    boolean mIsSaveBounds = true;
    private boolean mIsWindowStateBeforeLockScreenValid = false;
    private int mWindowStateBeforeLockScreen = 4;

    public HwTaskRecord(ActivityTaskManagerService service, int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        super(service, taskId, info, intent, voiceSession, voiceInteractor);
        if (HwPCUtils.isPcCastModeInServer()) {
            this.mRootActivityInfo = info;
        }
    }

    public HwTaskRecord(ActivityTaskManagerService service, int taskId, ActivityInfo info, Intent intent, ActivityManager.TaskDescription taskDescription) {
        super(service, taskId, info, intent, taskDescription);
        if (HwPCUtils.isPcCastModeInServer()) {
            this.mRootActivityInfo = info;
        }
    }

    public HwTaskRecord(ActivityTaskManagerService service, int taskId, Intent intent, Intent affinityIntent, String affinity, String rootAffinity, ComponentName realActivity, ComponentName origActivity, boolean isRootWasReset, boolean isAutoRemoveRecents, boolean isAskedCompatMode, int userId, int effectiveUid, String lastDescription, ArrayList<ActivityRecord> activities, long lastTimeMoved, boolean isNeverRelinquishIdentity, ActivityManager.TaskDescription lastTaskDescription, int taskAffiliation, int prevTaskId, int nextTaskId, int taskAffiliationColor, int callingUid, String callingPackage, int resizeMode, boolean isSupportsPictureInPicture, boolean isRealActivitySuspended, boolean isUserSetupComplete, int minWidth, int minHeight) {
        super(service, taskId, intent, affinityIntent, affinity, rootAffinity, realActivity, origActivity, isRootWasReset, isAutoRemoveRecents, isAskedCompatMode, userId, effectiveUid, lastDescription, activities, lastTimeMoved, isNeverRelinquishIdentity, lastTaskDescription, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage, resizeMode, isSupportsPictureInPicture, isRealActivitySuspended, isUserSetupComplete, minWidth, minHeight);
        if (HwPCUtils.isPcCastModeInServer() && this.mActivities.size() > 0) {
            this.mRootActivityInfo = ((ActivityRecord) this.mActivities.get(0)).info;
        }
    }

    @SuppressLint({"AvoidMax/Min"})
    public void overrideConfigOrienForFreeForm(Configuration config) {
        ActivityRecord topActivity = getTopActivity();
        if (topActivity != null) {
            int i = 1;
            if (inFreeformWindowingMode()) {
                ApplicationInfo info = this.mService.getPackageManagerInternalLocked().getApplicationInfo(topActivity.packageName, 0, Process.myUid(), this.userId);
                if (info != null && (info.flags & 1) != 0) {
                    Resources res = this.mService.mContext.getResources();
                    Configuration serviceConfig = getParent().getConfiguration();
                    if (res.getConfiguration().orientation == 1) {
                        config.orientation = 1;
                    } else if (serviceConfig != null) {
                        if (config.screenWidthDp > Math.min(serviceConfig.screenWidthDp, serviceConfig.screenHeightDp)) {
                            i = 2;
                        }
                        config.orientation = i;
                    }
                }
            } else if (inSplitScreenWindowingMode()) {
                if (this.mService.getPackageManagerInternalLocked().isInMWPortraitWhiteList(topActivity.packageName)) {
                    config.orientation = 1;
                }
            } else if (Log.HWINFO) {
                Log.i("HwTaskRecord", "Config freeform for other mode");
            }
        }
    }

    public void setWindowState(int state) {
        if (this.mWindowState != state) {
            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "setWindowState: " + Integer.toHexString(this.mWindowState) + " to " + Integer.toHexString(state));
            this.mWindowState = state;
            if (this.mService.mStackSupervisor instanceof HwActivityStackSupervisor) {
                this.mService.mStackSupervisor.scheduleReportPCWindowStateChangedLocked(this);
            }
            this.mService.mHwATMSEx.getHwTaskChangeController().notifyTaskProfileLocked(this.taskId, this.mWindowState);
        }
    }

    /* access modifiers changed from: protected */
    public void updateHwOverrideConfiguration(Rect bounds) {
        int windowStateLayout;
        DefaultHwPCMultiWindowManager multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx());
        if (this.mStack != null && HwPCUtils.isExtDynamicStack(this.mStack.getStackId()) && multiWindowMgr != null) {
            if (bounds == null || bounds.isEmpty()) {
                setLastNonFullscreenBounds(getRequestedOverrideBounds());
                windowStateLayout = 4;
            } else if (multiWindowMgr.getMaximizedBounds().equals(getRequestedOverrideBounds())) {
                windowStateLayout = 3;
            } else if (multiWindowMgr.getSplitLeftWindowBounds().equals(getRequestedOverrideBounds())) {
                setLastNonFullscreenBounds(getRequestedOverrideBounds());
                windowStateLayout = 5;
            } else if (multiWindowMgr.getSplitRightWindowBounds().equals(getRequestedOverrideBounds())) {
                setLastNonFullscreenBounds(getRequestedOverrideBounds());
                windowStateLayout = 6;
            } else if (getRequestedOverrideBounds().width() > getRequestedOverrideBounds().height()) {
                windowStateLayout = 2;
            } else {
                windowStateLayout = 1;
            }
            int finalState = (this.mNextWindowState & 65280) | windowStateLayout;
            ActivityRecord topActivity = getTopActivity();
            if (topActivity != null && multiWindowMgr.isSpecialVideo(topActivity.packageName)) {
                finalState |= 65536;
            }
            if (this.mWindowState != finalState && !HwPCUtils.enabledInPad()) {
                if (Log.HWINFO) {
                    HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "force to update task:" + toString());
                }
                multiWindowMgr.setForceUpdateTask(this.taskId);
            }
            setWindowState(finalState);
            multiWindowMgr.storeTaskSettings(buildTaskRecordEx());
        }
    }

    /* access modifiers changed from: protected */
    public boolean isMaximizedPortraitAppOnPCMode(String packageName) {
        if (!HwPCUtils.isPcCastModeInServer() || getStack() == null || !HwPCUtils.isValidExtDisplayId(getStack().mDisplayId) || !getHwPCMultiWindowManager(buildAtmsEx()).getPortraitMaximizedPkgList().contains(packageName)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void setStack(ActivityStack stack) {
        if (stack != null && HwPCUtils.isExtDynamicStack(stack.getStackId())) {
            int density = this.mService.mWindowManager.getBaseDisplayDensity(0);
            int extDensity = this.mService.mWindowManager.getBaseDisplayDensity(stack.mDisplayId);
            Point size = new Point();
            this.mService.mWindowManager.getBaseDisplaySize(stack.mDisplayId, size);
            if (density > 0 && extDensity > 0) {
                float ratio = (((float) extDensity) * 1.0f) / ((float) density);
                if (this.mMinWidth == -1) {
                    this.mMinWidth = this.mService.mRootActivityContainer.mDefaultMinSizeOfResizeableTaskDp;
                }
                if (this.mMinHeight == -1) {
                    this.mMinHeight = this.mService.mRootActivityContainer.mDefaultMinSizeOfResizeableTaskDp;
                }
                int halfWidth = size.x / 2;
                this.mMinWidth = (int) (((float) this.mMinWidth) * ratio);
                this.mMinWidth = this.mMinWidth > halfWidth ? halfWidth : this.mMinWidth;
                int halfHeight = size.y / 2;
                this.mMinHeight = (int) (((float) this.mMinHeight) * ratio);
                this.mMinHeight = this.mMinHeight > halfHeight ? halfHeight : this.mMinHeight;
            }
        }
        HwTaskRecord.super.setStack(stack);
    }

    /* access modifiers changed from: protected */
    public Rect getLaunchBounds() {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        if (this.mStack == null || !HwPCUtils.isExtDynamicStack(this.mStack.getStackId()) || !this.mIsLaunchBoundsFirst || this.mRootActivityInfo == null || (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx())) == null) {
            return HwTaskRecord.super.getLaunchBounds();
        }
        this.mIsLaunchBoundsFirst = false;
        TaskRecordEx taskRecordEx = buildTaskRecordEx();
        multiWindowMgr.restoreTaskWindowState(taskRecordEx);
        this.mLastNonFullscreenBounds = multiWindowMgr.getLaunchBounds(taskRecordEx);
        return this.mLastNonFullscreenBounds;
    }

    /* access modifiers changed from: package-private */
    public boolean removeActivity(ActivityRecord activityRecord, boolean isReparenting) {
        DefaultHwPCMultiWindowManager multiWindowMgr;
        boolean isPrecheckRst = false;
        if (activityRecord == null) {
            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "input parameter ActivityRecord is null");
            return false;
        }
        boolean isRemoveSuccess = HwTaskRecord.super.removeActivity(activityRecord, isReparenting);
        boolean isChanged = false;
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && !(isChanged = checkInKeyGuard()) && this.mIsInLockScreen && getTopActivity() != null) {
            isChanged = activityRecord.canShowWhenLocked() == getTopActivity().canShowWhenLocked();
        }
        if (((this.mStack != null && HwPCUtils.isExtDynamicStack(this.mStack.getStackId())) && getTopActivity() != null && (getTopActivity() instanceof HwActivityRecord) && (activityRecord instanceof HwActivityRecord)) && (getTopActivity().mCustomRequestedOrientation != ((HwActivityRecord) activityRecord).mCustomRequestedOrientation || isChanged)) {
            isPrecheckRst = true;
        }
        if (isPrecheckRst && (this.mService instanceof ActivityTaskManagerService) && (multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx())) != null) {
            int customRequestedOrientation = getTopActivity().mCustomRequestedOrientation;
            if (Log.HWINFO) {
                HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "removeActivity: (" + activityRecord.toString() + ")");
                HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "newTopActivity: (" + getTopActivity().toString() + ")[" + customRequestedOrientation + "]");
            }
            TaskRecordEx taskRecordEx = buildTaskRecordEx();
            if (customRequestedOrientation == 0) {
                this.mIsSaveBounds = true;
                multiWindowMgr.restoreTaskWindowState(taskRecordEx);
                multiWindowMgr.resizeTaskFromPC(taskRecordEx, multiWindowMgr.getLaunchBounds(taskRecordEx));
            } else if (HwPCUtils.enabledInPad()) {
                multiWindowMgr.updateTaskByRequestedOrientation(taskRecordEx, customRequestedOrientation);
            }
        }
        return isRemoveSuccess;
    }

    /* access modifiers changed from: package-private */
    public void createTask(boolean isOnTop, boolean isShowForAllUsers) {
        HwTaskRecord.super.createTask(isOnTop, isShowForAllUsers);
        if (this.mRootActivityInfo != null && this.mStack != null && HwPCUtils.isExtDynamicStack(this.mStack.mStackId)) {
            this.mService.mHwATMSEx.getHwTaskChangeController().notifyTaskCreated(this.taskId, this.mRootActivityInfo.getComponentName());
        }
    }

    /* access modifiers changed from: package-private */
    public void removeWindowContainer() {
        HwTaskRecord.super.removeWindowContainer();
        if (this.mStack != null && HwPCUtils.isExtDynamicStack(this.mStack.mStackId)) {
            this.mService.mHwATMSEx.getHwTaskChangeController().notifyTaskRemoved(this.taskId);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isResizeable(boolean isSupportsPip) {
        if (this.mStack == null || !HwPCUtils.isExtDynamicStack(this.mStack.getStackId())) {
            return HwTaskRecord.super.isResizeable(isSupportsPip);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void adjustForMinimalTaskDimensions(Rect bounds, Rect previousBounds) {
        if (SystemProperties.getInt("persist.sys.rog.configmode", 0) == 1) {
            this.mService.mStackSupervisor.reCalculateDefaultMinimalSizeOfResizeableTasks();
        }
        this.mDefaultMinSize = this.mService.mRootActivityContainer.mDefaultMinSizeOfResizeableTaskDp;
        Rect originBounds = new Rect();
        Point size = new Point();
        if (HwPCUtils.isExtDynamicStack(getStackId())) {
            originBounds.set(bounds);
            int densityDpi = getConfiguration().densityDpi;
            int serviceDpi = this.mService.getGlobalConfiguration().densityDpi;
            if (densityDpi > 0 && serviceDpi > 0) {
                this.mDefaultMinSize = (this.mDefaultMinSize * densityDpi) / serviceDpi;
                if (!(this.mService.mWindowManager == null || getStack() == null)) {
                    this.mService.mWindowManager.getBaseDisplaySize(getStack().mDisplayId, size);
                    int minSizePx = (int) (((float) (size.x < size.y ? size.x : size.y)) * 0.2f);
                    this.mDefaultMinSize = (this.mDefaultMinSize <= minSizePx || minSizePx == 0) ? this.mDefaultMinSize : minSizePx;
                }
            }
        }
        HwTaskRecord.super.adjustForMinimalTaskDimensions(bounds, previousBounds);
        if (HwPCUtils.isExtDynamicStack(getStackId())) {
            updateBoundsByRatio(originBounds, bounds, size);
        }
    }

    private void updateBoundsByRatio(Rect oldBound, Rect newBound, Point displaySize) {
        boolean isPreCheckDisplay = false;
        boolean isPreCheckBound = oldBound == null || oldBound.isEmpty() || newBound == null || newBound.isEmpty();
        if (displaySize == null || displaySize.x <= 0 || displaySize.y <= 0) {
            isPreCheckDisplay = true;
        }
        if (!isPreCheckBound && !isPreCheckDisplay) {
            int oldW = oldBound.right - oldBound.left;
            int oldH = oldBound.bottom - oldBound.top;
            if (oldW != 0 && oldH != 0) {
                int newW = newBound.right - newBound.left;
                int newH = newBound.bottom - newBound.top;
                if (newW != 0 && newH != 0) {
                    float ratio = ((float) oldW) / ((float) oldH);
                    if (newW != oldW) {
                        int tmpH = (int) (((float) newW) / ratio);
                        if (((float) tmpH) > ((float) displaySize.y) * 0.8f) {
                            newBound.set(oldBound);
                        } else {
                            newBound.bottom = newBound.top + tmpH;
                        }
                    } else if (newH != oldH) {
                        int tmpW = (int) (((float) newH) * ratio);
                        if (((float) tmpW) > ((float) displaySize.x) * 0.8f) {
                            newBound.set(oldBound);
                        } else {
                            newBound.right = newBound.left + tmpW;
                        }
                    }
                }
            }
        }
    }

    private boolean checkInKeyGuard() {
        HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "checkInKeyGuard");
        boolean isInLockScreen = this.mIsInLockScreen;
        if (((KeyguardManager) this.mService.mContext.getSystemService("keyguard")).inKeyguardRestrictedInputMode()) {
            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "checkInKeyGuard mIsInLockScreen true");
            this.mIsInLockScreen = true;
        } else {
            this.mIsInLockScreen = false;
            HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "checkInKeyGuard mIsInLockScreen false");
        }
        return isInLockScreen != this.mIsInLockScreen;
    }

    /* access modifiers changed from: package-private */
    public void activityResumedInTop() {
        int i;
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            checkInKeyGuard();
            if (this.mWindowState != 4 && this.mIsInLockScreen) {
                HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "activityResumedInTop WINDOW_FULLSCREEN");
                this.mWindowStateBeforeLockScreen = this.mWindowState;
                this.mIsWindowStateBeforeLockScreenValid = true;
                setWindowState(4);
                setBounds(null);
            } else if (this.mIsInLockScreen || !this.mIsWindowStateBeforeLockScreenValid || this.mWindowState == (i = this.mWindowStateBeforeLockScreen)) {
                HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "Resumed in top for activity for other mode.");
            } else {
                setWindowState(i);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addActivityToTop(ActivityRecord activityRecord) {
        HwTaskRecord.super.addActivityToTop(activityRecord);
        if (HwPCUtils.isExtDynamicStack(getStackId()) && (this.mService instanceof ActivityTaskManagerService) && activityRecord.info != null && (getTopActivity() instanceof HwActivityRecord)) {
            DefaultHwPCMultiWindowManager multiWindowMgr = getHwPCMultiWindowManager(buildAtmsEx());
            int requestedOrientation = activityRecord.info.screenOrientation;
            int newCustomRequestOrientation = 0;
            if (multiWindowMgr != null) {
                if (multiWindowMgr.isFixedOrientationPortrait(requestedOrientation)) {
                    newCustomRequestOrientation = 1;
                } else if (multiWindowMgr.isFixedOrientationLandscape(requestedOrientation)) {
                    newCustomRequestOrientation = 2;
                }
            }
            if (newCustomRequestOrientation != 0) {
                int customRequestedOrientation = 1;
                if (HwPCMultiWindowCompatibility.getWindowStateLayout(this.mWindowState) != 1) {
                    customRequestedOrientation = 2;
                }
                HwPCUtils.log(DefaultHwPCMultiWindowManager.TAG, "requestedOrientation:" + newCustomRequestOrientation + " oldRequestedOrientation:" + customRequestedOrientation);
                if (newCustomRequestOrientation != customRequestedOrientation) {
                    activityRecord.setRequestedOrientation(newCustomRequestOrientation);
                }
            }
            activityResumedInTop();
        }
    }

    public ArrayList<ActivityRecord> getActivities() {
        return this.mActivities;
    }

    private TaskRecordEx buildTaskRecordEx() {
        TaskRecordEx taskRecordEx = new TaskRecordEx();
        taskRecordEx.setTaskRecord(this);
        return taskRecordEx;
    }

    private ActivityTaskManagerServiceEx buildAtmsEx() {
        ActivityTaskManagerServiceEx atmsEx = new ActivityTaskManagerServiceEx();
        atmsEx.setActivityTaskManagerService(this.mService);
        return atmsEx;
    }

    private DefaultHwPCMultiWindowManager getHwPCMultiWindowManager(ActivityTaskManagerServiceEx atmsEx) {
        return HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCMultiWindowManager(atmsEx);
    }

    public boolean isSaveBounds() {
        return this.mIsSaveBounds;
    }

    public void setSaveBounds(boolean bool) {
        this.mIsSaveBounds = bool;
    }
}
