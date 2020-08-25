package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.ResolveInfo;
import android.freeform.HwFreeFormUtils;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.HwPCUtils;
import android.util.Slog;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.wm.ActivityStack;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.hiai.awareness.AwarenessConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class HwActivityStackSupervisor extends ActivityStackSupervisor {
    private static final int CRASH_INTERVAL_THRESHOLD = 60000;
    private static final int CRASH_TIMES_THRESHOLD = 3;
    private static final String SPLIT_SCREEN_APP_NAME = "splitscreen.SplitScreenAppActivity";
    private static final String STACK_DIVIDER_APP_NAME = "stackdivider.ForcedResizableInfoActivity";
    private int mCrashTimes;
    private long mFirstLaunchTime;
    private String mLastHomePkg;
    private final ArrayList<ActivityRecord> mWindowStateChangedActivities = new ArrayList<>();

    public HwActivityStackSupervisor(ActivityTaskManagerService service, Looper looper) {
        super(service, looper);
    }

    private boolean isUninstallableApk(String pkgName, Intent homeIntent, int userId) {
        ResolveInfo homeInfo;
        ComponentInfo ci;
        if (pkgName == null || AppStartupDataMgr.HWPUSH_PKGNAME.equals(pkgName) || (homeInfo = resolveIntent(homeIntent, homeIntent.resolveTypeIfNeeded(this.mService.mContext.getContentResolver()), userId, 786432, Binder.getCallingUid())) == null || (ci = homeInfo.getComponentInfo()) == null) {
            return false;
        }
        if (((ci.applicationInfo.flags & 1) != 0) || !pkgName.equals(ci.packageName)) {
            return false;
        }
        return true;
    }

    public void recognitionMaliciousApp(IApplicationThread caller, Intent intent, int userId) {
        Intent homeIntent;
        if (caller == null && intent != null && (homeIntent = this.mService.getHomeIntent()) != null && homeIntent.getCategories() != null) {
            String action = intent.getAction();
            Set<String> categories = intent.getCategories();
            boolean isCategoryContainsHome = true;
            boolean isActionEqualsHome = action != null && action.equals(homeIntent.getAction());
            if (categories == null || !categories.containsAll(homeIntent.getCategories())) {
                isCategoryContainsHome = false;
            }
            if (isActionEqualsHome && isCategoryContainsHome) {
                ComponentName cmp = intent.getComponent();
                String strPkg = cmp != null ? cmp.getPackageName() : null;
                if (strPkg != null && !HwDeviceManager.disallowOp(26, strPkg) && isUninstallableApk(strPkg, homeIntent, userId) && (intent.getHwFlags() & AwarenessConstants.TRAVEL_HELPER_DATA_CHANGE_ACTION) == 0) {
                    updateCrashInfo(strPkg);
                }
            }
        }
    }

    private void updateCrashInfo(String strPkg) {
        if (strPkg.equals(this.mLastHomePkg)) {
            this.mCrashTimes++;
            long now = SystemClock.uptimeMillis();
            if (this.mCrashTimes < 3) {
                return;
            }
            if (now - this.mFirstLaunchTime < AppHibernateCst.DELAY_ONE_MINS) {
                try {
                    ActivityThread.getPackageManager().clearPackagePreferredActivities(strPkg);
                } catch (RemoteException e) {
                    Slog.e("ActivityManager", " Update crash info fail.");
                }
                this.mService.showUninstallLauncherDialog(strPkg);
                this.mLastHomePkg = null;
                this.mCrashTimes = 0;
                return;
            }
            this.mCrashTimes = 1;
            this.mFirstLaunchTime = now;
            return;
        }
        this.mLastHomePkg = strPkg;
        this.mCrashTimes = 0;
        this.mFirstLaunchTime = SystemClock.uptimeMillis();
    }

    /* access modifiers changed from: package-private */
    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags, int filterCallingUid) {
        int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
        if (userId == callingUserId && userId == 0) {
            return HwActivityStackSupervisor.super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
        }
        if (!this.mService.mUserManagerInternal.isSameGroupForClone(callingUserId, userId)) {
            return HwActivityStackSupervisor.super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            return HwActivityStackSupervisor.super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private ArrayList<ActivityRecord> getWindowStateChangedActivities() {
        return this.mWindowStateChangedActivities;
    }

    private boolean isActivityInStack(TaskRecord task, String pkgName, String processName) {
        ArrayList<ActivityRecord> activities;
        int numActivities;
        if (task == null || pkgName == null || processName == null || (numActivities = (activities = task.mActivities).size()) <= 0) {
            return false;
        }
        for (int activityNdx = numActivities - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord record = activities.get(activityNdx);
            if (record != null && !record.finishing && !record.isState(ActivityStack.ActivityState.DESTROYED) && record.app != null && pkgName.equals(record.packageName) && processName.equals(record.app.mName)) {
                return true;
            }
        }
        return false;
    }

    public void scheduleReportPCWindowStateChangedLocked(TaskRecord task) {
        if (task != null) {
            for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord record = (ActivityRecord) task.mActivities.get(i);
                if (!(record.app == null || record.app.mThread == null)) {
                    getWindowStateChangedActivities().add(record);
                }
            }
            if (!this.mHandler.hasMessages((int) IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT)) {
                this.mHandler.sendEmptyMessage((int) IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT);
            }
        }
    }

    public void handlePCWindowStateChanged() {
        synchronized (this.mService.mGlobalLock) {
            List<ActivityRecord> records = getWindowStateChangedActivities();
            for (int i = records.size() - 1; i >= 0; i--) {
                ActivityRecord record = records.remove(i);
                if (record instanceof HwActivityRecord) {
                    ((HwActivityRecord) record).schedulePCWindowStateChanged();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean restoreRecentTaskLocked(TaskRecord task, ActivityOptions activityOptions, boolean isTop) {
        if ((HwPCUtils.isPcCastModeInServer() || this.mService.mVrMananger.isVRMode()) && task == null) {
            return false;
        }
        return HwActivityStackSupervisor.super.restoreRecentTaskLocked(task, activityOptions, isTop);
    }

    /* access modifiers changed from: protected */
    public boolean keepStackResumed(ActivityStack stack) {
        if (HwPCUtils.isPcCastModeInServer() && stack != null) {
            if (HwPCUtils.isPcDynamicStack(stack.mStackId) && stack.shouldBeVisible((ActivityRecord) null)) {
                return true;
            }
            if (HwPCUtils.isPcDynamicStack(this.mRootActivityContainer.getTopDisplayFocusedStack().mStackId) && !HwPCUtils.isPcDynamicStack(stack.mStackId)) {
                return true;
            }
        }
        return HwActivityStackSupervisor.super.keepStackResumed(stack);
    }

    /* access modifiers changed from: protected */
    public boolean isStackInVisible(ActivityStack stack) {
        if (stack != null && HwPCUtils.isExtDynamicStack(stack.mStackId) && stack.shouldBeVisible((ActivityRecord) null)) {
            return true;
        }
        if (!HwFreeFormUtils.isFreeFormEnable() || stack == null || !stack.inFreeformWindowingMode() || stack.shouldBeVisible((ActivityRecord) null)) {
            return HwActivityStackSupervisor.super.isStackInVisible(stack);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean startProcessOnExtDisplay(ActivityRecord record) {
        return HwActivityStackSupervisor.super.startProcessOnExtDisplay(record);
    }

    public boolean shouldNotKillProcWhenRemoveTask(String pkg) {
        if (!"com.tencent.mm".equals(pkg) || SystemProperties.getBoolean("hw.app.smart_cleaning", false)) {
            return false;
        }
        Slog.d("ActivityManager", " cleanUpRemovedTaskLocked, do not kill process : " + pkg);
        return true;
    }

    public boolean isInVisibleStack(String pkg) {
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean hasActivityInStackLocked(ActivityInfo activityInfo) {
        if (activityInfo == null) {
            return false;
        }
        String affinity = activityInfo.taskAffinity;
        String pkgName = activityInfo.packageName;
        String processName = activityInfo.processName;
        if (affinity == null || pkgName == null || processName == null || activityInfo.applicationInfo == null) {
            return false;
        }
        int userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        for (int displayNdx = this.mRootActivityContainer.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mRootActivityContainer.getChildAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ArrayList<TaskRecord> allTasks = stacks.get(stackNdx).getAllTasks();
                for (int taskNdx = allTasks.size() - 1; taskNdx >= 0; taskNdx--) {
                    TaskRecord task = allTasks.get(taskNdx);
                    if (task != null && task.userId == userId && task.affinity != null && task.affinity.equals(affinity)) {
                        return isActivityInStack(task, pkgName, processName);
                    }
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void uploadUnSupportSplitScreenAppPackageName(String pkgName) {
        if (this.mService.mContext != null) {
            Context context = this.mService.mContext;
            StatisticalUtils.reporte(context, 174, "{ Launcher fail, pkgName:" + pkgName + " }");
        }
    }

    /* access modifiers changed from: protected */
    public ActivityStack getTargetSplitTopStack(ActivityStack current) {
        ActivityStack topPrimaryStack;
        if (current.getWindowingMode() == 3) {
            ActivityStack topSecondaryStack = current.getDisplay().getTopStackInWindowingMode(4);
            ActivityRecord topSecondaryActivityRecord = null;
            if (topSecondaryStack != null) {
                topSecondaryActivityRecord = topSecondaryStack.topRunningActivityLocked();
            }
            if (topSecondaryActivityRecord != null && !topSecondaryActivityRecord.toString().contains(SPLIT_SCREEN_APP_NAME)) {
                return topSecondaryStack;
            }
            ActivityStack homeStack = current.getDisplay().getStack(4, 2);
            if (homeStack != null) {
                return homeStack;
            }
            return null;
        } else if (current.getWindowingMode() != 4 || current.getActivityType() != 1) {
            return null;
        } else {
            ActivityStack nextTargetStack = getNextStackInSplitSecondary(current);
            ActivityRecord nextTargetRecord = null;
            if (nextTargetStack != null) {
                nextTargetRecord = nextTargetStack.topRunningActivityLocked();
            }
            if (nextTargetRecord == null || nextTargetRecord.info == null || nextTargetRecord.info.name == null || !nextTargetRecord.info.name.contains(SPLIT_SCREEN_APP_NAME) || (topPrimaryStack = current.getDisplay().getTopStackInWindowingMode(3)) == null) {
                return null;
            }
            return topPrimaryStack;
        }
    }

    /* access modifiers changed from: protected */
    public ActivityStack getNextStackInSplitSecondary(ActivityStack current) {
        ArrayList<ActivityStack> stacks = current.getDisplay().mStacks;
        boolean isNext = false;
        for (int i = stacks.size() - 1; i >= 0; i--) {
            ActivityStack targetStack = stacks.get(i);
            if (isNext) {
                ActivityRecord targetRecord = targetStack.topRunningActivityLocked();
                if ((targetRecord == null || targetRecord.info == null || targetRecord.info.name == null || !targetRecord.info.name.contains(STACK_DIVIDER_APP_NAME)) && targetStack.getWindowingMode() == 4) {
                    return targetStack;
                }
            } else if (targetStack.getStackId() == current.getStackId()) {
                isNext = true;
            }
        }
        return null;
    }
}
