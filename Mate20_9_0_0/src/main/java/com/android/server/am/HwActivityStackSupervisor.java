package com.android.server.am;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.pc.IHwPCManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.rms.iaware.DataContract.Apps;
import android.rms.iaware.DataContract.Apps.Builder;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.widget.Toast;
import com.android.server.HwServiceFactory;
import com.android.server.UiThread;
import com.android.server.am.ActivityStack.ActivityState;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.displayengine.IDisplayEngineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class HwActivityStackSupervisor extends ActivityStackSupervisor {
    private static final String ACTION_CONFIRM_APPLOCK_CREDENTIAL = "huawei.intent.action.APPLOCK_FRAMEWORK_MANAGER";
    private static final String ACTION_CONFIRM_APPLOCK_PACKAGENAME = "com.huawei.systemmanager";
    private static final int CRASH_INTERVAL_THRESHOLD = 60000;
    private static final int CRASH_TIMES_THRESHOLD = 3;
    private static final String SPLIT_SCREEN_APP_NAME = "splitscreen.SplitScreenAppActivity";
    private static final String STACK_DIVIDER_APP_NAME = "stackdivider.ForcedResizableInfoActivity";
    private int mCrashTimes;
    private long mFirstLaunchTime;
    private String mLastHomePkg;
    private int mNextPcFreeStackId = 1000000008;
    private int mNextVrFreeStackId = 1100000000;
    private Toast mToast = null;
    private final ArrayList<ActivityRecord> mWindowStateChangedActivities = new ArrayList();

    public HwActivityStackSupervisor(ActivityManagerService service, Looper looper) {
        super(service, looper);
    }

    private boolean isUninstallableApk(String pkgName) {
        if (pkgName == null || "android".equals(pkgName)) {
            return false;
        }
        try {
            PackageInfo pInfo = this.mService.mContext.getPackageManager().getPackageInfo(pkgName, 0);
            if (pInfo == null || (pInfo.applicationInfo.flags & 1) == 0) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public void recognitionMaliciousApp(IApplicationThread caller, Intent intent) {
        if (caller == null) {
            Intent homeIntent = this.mService.getHomeIntent();
            String action = intent.getAction();
            Set<String> category = intent.getCategories();
            if (action != null && action.equals(homeIntent.getAction()) && category != null && category.containsAll(homeIntent.getCategories())) {
                ComponentName cmp = intent.getComponent();
                String strPkg = null;
                if (cmp != null) {
                    strPkg = cmp.getPackageName();
                }
                String strPkg2 = strPkg;
                if (strPkg2 != null) {
                    if (HwDeviceManager.disallowOp(26, strPkg2)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(strPkg2);
                        stringBuilder.append(" is a ignored frequent relaunch app set by MDM!");
                        Slog.i("ActivityManager", stringBuilder.toString());
                    } else if (isUninstallableApk(strPkg2)) {
                        if (strPkg2.equals(this.mLastHomePkg)) {
                            this.mCrashTimes++;
                            long now = SystemClock.uptimeMillis();
                            if (this.mCrashTimes >= 3) {
                                if (now - this.mFirstLaunchTime < AppHibernateCst.DELAY_ONE_MINS) {
                                    try {
                                        ActivityThread.getPackageManager().clearPackagePreferredActivities(strPkg2);
                                    } catch (RemoteException e) {
                                    }
                                    this.mService.showUninstallLauncherDialog(strPkg2);
                                    this.mLastHomePkg = null;
                                    this.mCrashTimes = 0;
                                } else {
                                    this.mCrashTimes = 1;
                                    this.mFirstLaunchTime = now;
                                }
                            }
                        } else {
                            this.mLastHomePkg = strPkg2;
                            this.mCrashTimes = 0;
                            this.mFirstLaunchTime = SystemClock.uptimeMillis();
                        }
                    }
                }
            }
        }
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags, int filterCallingUid) {
        int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
        if (userId == callingUserId && userId == 0) {
            return super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
        }
        if (!this.mService.mUserController.mInjector.getUserManagerInternal().isSameGroupForClone(callingUserId, userId)) {
            return super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            ResolveInfo resolveIntent = super.resolveIntent(intent, resolvedType, userId, flags, filterCallingUid);
            return resolveIntent;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void noteActivityDisplayedEnd(String activityName, int uid, int pid, long time) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RES_APP)) && this.mService.mSystemReady) {
            Builder builder = Apps.builder();
            builder.addEvent(85013);
            builder.addActivityDisplayedInfoWithUid(activityName, uid, pid, time);
            CollectData appsData = builder.build();
            long id = Binder.clearCallingIdentity();
            resManager.reportData(appsData);
            Binder.restoreCallingIdentity(id);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_APP_ACTIVITY_DISPLAYED_FINISH reportData: ");
            stringBuilder.append(activityName);
            stringBuilder.append(" ");
            stringBuilder.append(pid);
            stringBuilder.append(" ");
            stringBuilder.append(time);
            Slog.d("ActivityManager", stringBuilder.toString());
        }
    }

    void reportActivityLaunchedLocked(boolean timeout, ActivityRecord r, long thisTime, long totalTime) {
        if (!(timeout || r == null || r.app == null || r.shortComponentName == null || r.app.pid <= 0)) {
            noteActivityDisplayedEnd(r.shortComponentName, r.app.uid, r.app.pid, thisTime);
        }
        super.reportActivityLaunchedLocked(timeout, r, thisTime, totalTime);
    }

    public ArrayList<ActivityRecord> getWindowStateChangedActivities() {
        return this.mWindowStateChangedActivities;
    }

    public void scheduleReportPCWindowStateChangedLocked(TaskRecord task) {
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
            if (!(r.app == null || r.app.thread == null)) {
                getWindowStateChangedActivities().add(r);
            }
        }
        if (!this.mHandler.hasMessages(IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT)) {
            this.mHandler.sendEmptyMessage(IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT);
        }
    }

    public void handlePCWindowStateChanged() {
        synchronized (this.mService) {
            List<ActivityRecord> list = getWindowStateChangedActivities();
            for (int i = list.size() - 1; i >= 0; i--) {
                ActivityRecord r = (ActivityRecord) list.remove(i);
                if (r instanceof HwActivityRecord) {
                    ((HwActivityRecord) r).schedulePCWindowStateChanged();
                }
            }
        }
    }

    protected int getNextVrStackId() {
        while (true) {
            if (this.mNextVrFreeStackId >= 1100000000 && getStack(this.mNextVrFreeStackId) == null) {
                return this.mNextVrFreeStackId;
            }
            this.mNextVrFreeStackId++;
        }
    }

    protected int getNextPcStackId() {
        while (true) {
            if (this.mNextPcFreeStackId >= 1100000000) {
                this.mNextPcFreeStackId = 1000000008;
            }
            if (this.mNextPcFreeStackId >= 1000000008 && getStack(this.mNextPcFreeStackId) == null) {
                return this.mNextPcFreeStackId;
            }
            this.mNextPcFreeStackId++;
        }
    }

    protected ActivityStack getValidLaunchStackOnDisplay(int displayId, ActivityRecord r) {
        ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        StringBuilder stringBuilder;
        if (activityDisplay == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Display with displayId=");
            stringBuilder.append(displayId);
            stringBuilder.append(" not found.");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (HwVRUtils.isValidVRDisplayId(displayId)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("vr getValidLaunchStackOnDisplay displayid is: ");
            stringBuilder.append(displayId);
            Slog.i("ActivityManager", stringBuilder.toString());
            return HwServiceFactory.createActivityStack(activityDisplay, getNextVrStackId(), this, r.getWindowingMode(), r.getActivityType(), true);
        } else if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            return HwServiceFactory.createActivityStack(activityDisplay, getNextPcStackId(), this, 10, 1, true);
        } else {
            if (HwPCUtils.isPcCastModeInServer() && displayId == 0) {
                HwPCUtils.log("ActivityManager", " create full screen stack because the stack is null when r is from pc");
                activityDisplay.getOrCreateStack(1, 0, true);
            }
            return super.getValidLaunchStackOnDisplay(displayId, r);
        }
    }

    protected boolean restoreRecentTaskLocked(TaskRecord task, ActivityOptions aOptions, boolean onTop) {
        if ((HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode()) && task == null) {
            return false;
        }
        return super.restoreRecentTaskLocked(task, aOptions, onTop);
    }

    protected void handleDisplayRemoved(int displayId) {
        int i = 0;
        ActivityDisplay activityDisplay;
        int size;
        ArrayList<ActivityStack> stacks;
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            synchronized (this.mService) {
                activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    size = activityDisplay.getChildCount();
                    stacks = new ArrayList();
                    while (i < size) {
                        stacks.add(activityDisplay.getChildAt(i));
                        i++;
                    }
                    onDisplayRemoved(stacks);
                    activityDisplay.remove();
                    this.mActivityDisplays.remove(displayId);
                }
            }
        } else if (HwVRUtils.isValidVRDisplayId(displayId)) {
            synchronized (this.mService) {
                activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    size = activityDisplay.getChildCount();
                    stacks = new ArrayList();
                    while (i < size) {
                        stacks.add(activityDisplay.getChildAt(i));
                        i++;
                    }
                    onVRdisplayRemoved(activityDisplay, stacks, displayId);
                }
            }
        } else {
            super.handleDisplayRemoved(displayId);
        }
    }

    protected boolean keepStackResumed(ActivityStack stack) {
        if (HwPCUtils.isPcCastModeInServer() && stack != null) {
            if (HwPCUtils.isPcDynamicStack(stack.mStackId) && stack.shouldBeVisible(null)) {
                return true;
            }
            if (HwPCUtils.isPcDynamicStack(this.mFocusedStack.mStackId) && !HwPCUtils.isPcDynamicStack(stack.mStackId)) {
                return true;
            }
        }
        return super.keepStackResumed(stack);
    }

    protected boolean isStackInVisible(ActivityStack stack) {
        if (stack != null && HwPCUtils.isExtDynamicStack(stack.mStackId) && stack.shouldBeVisible(null)) {
            return true;
        }
        return super.isStackInVisible(stack);
    }

    protected void showToast(final int displayId) {
        if (HwPCUtils.isPcCastModeInServer()) {
            Context context;
            if (HwPCUtils.isValidExtDisplayId(displayId)) {
                context = HwPCUtils.getDisplayContext(this.mService.mContext, displayId);
            } else {
                context = this.mService.mContext;
            }
            if (context != null) {
                UiThread.getHandler().post(new Runnable() {
                    public void run() {
                        if (HwActivityStackSupervisor.this.mToast != null) {
                            HwActivityStackSupervisor.this.mToast.cancel();
                        }
                        if (HwPCUtils.isValidExtDisplayId(displayId)) {
                            HwActivityStackSupervisor.this.mToast = Toast.makeText(context, context.getResources().getString(33685970), 0);
                        } else {
                            HwActivityStackSupervisor.this.mToast = Toast.makeText(context, context.getString(33685971), 0);
                        }
                        if (HwActivityStackSupervisor.this.mToast != null) {
                            HwActivityStackSupervisor.this.mToast.show();
                        }
                    }
                });
            }
        }
    }

    protected boolean startProcessOnExtDisplay(ActivityRecord r) {
        ActivityRecord activityRecord = r;
        if (r.getStack() == null || !HwPCUtils.isExtDynamicStack(r.getStack().mStackId) || r.getTask() == null || r.getTask().getLaunchBounds() == null) {
            return super.startProcessOnExtDisplay(r);
        }
        String[] args = null;
        if (r.getTask().getLaunchBounds() != null) {
            args = new String[]{String.valueOf(r.getDisplayId()), String.valueOf(r.getTask().getLaunchBounds().width()), String.valueOf(r.getTask().getLaunchBounds().height())};
        }
        this.mService.startProcessLocked(activityRecord.processName, activityRecord.info.applicationInfo, true, 0, "activity", activityRecord.intent.getComponent(), false, false, 0, true, null, null, args, null);
        return true;
    }

    public boolean shouldNotKillProcWhenRemoveTask(String pkg) {
        if (!"com.tencent.mm".equals(pkg)) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" cleanUpRemovedTaskLocked, do not kill process : ");
        stringBuilder.append(pkg);
        Slog.d("ActivityManager", stringBuilder.toString());
        return true;
    }

    public void onDisplayRemoved(ArrayList<ActivityStack> stacks) {
        ArrayList<Intent> mIntentList = new ArrayList();
        for (int i = stacks.size() - 1; i >= 0; i--) {
            ((ActivityStack) stacks.get(i)).mForceHidden = true;
        }
        ArrayList<ProcessRecord> procs = getPCProcessRecordList();
        while (!stacks.isEmpty()) {
            ActivityStack stack = (ActivityStack) stacks.get(0);
            TaskRecord tr = stack.topTask();
            if (tr != null) {
                if (!(tr.intent == null || stack.inPinnedWindowingMode())) {
                    mIntentList.add(tr.intent);
                }
                if (tr instanceof HwTaskRecord) {
                    removeProcessesActivityNotFinished(((HwTaskRecord) tr).getActivities(), procs);
                }
                stack.moveTaskToBackLocked(stack.getStackId());
                stack.finishAllActivitiesLocked(true);
            } else {
                stack.finishAllActivitiesLocked(true);
            }
            stacks.remove(stack);
        }
        killPCProcessesLocked(procs);
        IHwPCManager pcManager = HwPCUtils.getHwPCManager();
        if (pcManager != null) {
            try {
                pcManager.saveAppIntent(mIntentList);
            } catch (RemoteException e) {
                HwPCUtils.log("ActivityManager", "fail to saveAppIntent on display removed");
            }
        }
        if (this.mService instanceof HwActivityManagerService) {
            ((HwActivityManagerService) this.mService).mPkgDisplayMaps.clear();
        }
    }

    public void onVRdisplayRemoved(ActivityDisplay display, ArrayList<ActivityStack> stacks, int displayId) {
        Slog.i("ActivityManager", "onVRdisplayRemoved");
        if (stacks == null) {
            Slog.w("ActivityManager", "vr activitystack is null");
            return;
        }
        while (!stacks.isEmpty()) {
            ActivityStack stack = (ActivityStack) stacks.get(0);
            moveTasksToFullscreenStackLocked(stack, false);
            stacks.remove(stack);
        }
        display.remove();
        this.mActivityDisplays.remove(displayId);
        resumeFocusedStackTopActivityLocked();
    }

    protected boolean resumeAppLockActivityIfNeeded(ActivityStack stack, ActivityOptions targetOptions) {
        if (this.mCurrentUser != 0) {
            return false;
        }
        ActivityRecord r = stack.topRunningActivityLocked();
        if (r == null || r.isState(ActivityState.RESUMED) || !isKeyguardDismiss() || !isAppInLockList(r.packageName)) {
            return false;
        }
        Intent newIntent = new Intent(ACTION_CONFIRM_APPLOCK_CREDENTIAL);
        newIntent.setFlags(109051904);
        newIntent.setPackage("com.huawei.systemmanager");
        newIntent.putExtra("android.intent.extra.TASK_ID", r.task.taskId);
        newIntent.putExtra("android.intent.extra.PACKAGE_NAME", r.packageName);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchTaskId(r.task.taskId);
        this.mService.mContext.startActivity(newIntent, options.toBundle());
        return true;
    }

    protected boolean isAppInLockList(String pgkName) {
        if (pgkName != null && Secure.getInt(this.mService.mContext.getContentResolver(), "app_lock_func_status", 0) == 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Secure.getString(this.mService.mContext.getContentResolver(), "app_lock_list"));
            stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(pgkName);
            stringBuilder3.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            if (stringBuilder2.contains(stringBuilder3.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isKeyguardDismiss() {
        return getKeyguardController().isKeyguardLocked() ^ 1;
    }

    public boolean isInVisibleStack(String pkg) {
        if (pkg == null || pkg.isEmpty()) {
            return false;
        }
        int size = this.mActivityDisplays.size();
        HwActivityStack hwStack = null;
        ActivityDisplay activityDisplay = null;
        for (int displayNdx = 0; displayNdx < size; displayNdx++) {
            activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = activityDisplay.getChildAt(stackNdx);
                if ((stack instanceof HwActivityStack) && ((HwActivityStack) stack).isVisibleLocked(pkg, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean notKillProcessWhenRemoveTask(ProcessRecord processRecord) {
        boolean z = true;
        if (this.mService.mContext == null) {
            return true;
        }
        if (System.getInt(this.mService.mContext.getContentResolver(), "not_kill_process_when_remove_task", 1) != 1) {
            z = false;
        }
        return z;
    }

    boolean hasActivityInStackLocked(ActivityInfo aInfo) {
        HwActivityStackSupervisor hwActivityStackSupervisor = this;
        ActivityInfo activityInfo = aInfo;
        boolean z = false;
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
        int i = 1;
        int displayNdx = hwActivityStackSupervisor.mActivityDisplays.size() - 1;
        while (displayNdx >= 0) {
            int i2;
            ArrayList<ActivityStack> stacks = ((ActivityDisplay) hwActivityStackSupervisor.mActivityDisplays.valueAt(displayNdx)).mStacks;
            int stackNdx = stacks.size() - i;
            while (stackNdx >= 0) {
                ArrayList<TaskRecord> allTasks = ((ActivityStack) stacks.get(stackNdx)).getAllTasks();
                int taskNdx = allTasks.size() - i;
                while (taskNdx >= 0) {
                    TaskRecord task = (TaskRecord) allTasks.get(taskNdx);
                    if (task == null || task.userId != userId || task.affinity == null || !task.affinity.equals(affinity)) {
                        taskNdx--;
                        i = i;
                        z = false;
                    } else {
                        ArrayList<ActivityRecord> activities = task.mActivities;
                        int numActivities = activities.size();
                        if (numActivities <= 0) {
                            return z;
                        }
                        int activityNdx = numActivities - 1;
                        while (true) {
                            int activityNdx2 = activityNdx;
                            if (activityNdx2 < 0) {
                                return false;
                            }
                            ActivityRecord r = (ActivityRecord) activities.get(activityNdx2);
                            if (!r.finishing && !r.isState(ActivityState.DESTROYED) && r.app != null && pkgName.equals(r.packageName) && processName.equals(r.app.processName)) {
                                return true;
                            }
                            activityNdx = activityNdx2 - 1;
                            i = 1;
                        }
                    }
                }
                i2 = i;
                stackNdx--;
                z = false;
            }
            i2 = i;
            displayNdx--;
            hwActivityStackSupervisor = this;
            z = false;
        }
        return false;
    }

    private int getSpecialTaskId(boolean isDefaultDisplay, ActivityStack stack, String pkgName, boolean invisibleAlso) {
        ArrayList<TaskRecord> tasks;
        int taskNdx;
        TaskRecord tr;
        if (pkgName != null && !"".equals(pkgName)) {
            tasks = stack.getAllTasks();
            for (taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                tr = (TaskRecord) tasks.get(taskNdx);
                if (tr != null && (invisibleAlso || tr.isVisible())) {
                    candicatedArs = new ActivityRecord[2];
                    int arIdx = 0;
                    candicatedArs[0] = tr.topRunningActivityLocked();
                    candicatedArs[1] = tr.getRootActivity();
                    while (true) {
                        int arIdx2 = arIdx;
                        if (arIdx2 >= candicatedArs.length) {
                            continue;
                            break;
                        }
                        ActivityRecord ar = candicatedArs[arIdx2];
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getSpecialTaskId ar = ");
                        stringBuilder.append(ar);
                        stringBuilder.append(", tr.isVisible() = ");
                        stringBuilder.append(tr.isVisible());
                        HwPCUtils.log("ActivityManager", stringBuilder.toString());
                        if (ar != null && ar.packageName != null && ar.packageName.equals(pkgName)) {
                            return tr.taskId;
                        }
                        arIdx = arIdx2 + 1;
                    }
                }
            }
        } else if (isDefaultDisplay) {
            tasks = stack.getAllTasks();
            taskNdx = tasks.size() - 1;
            while (true) {
                int taskNdx2 = taskNdx;
                if (taskNdx2 < 0) {
                    break;
                }
                tr = (TaskRecord) tasks.get(taskNdx2);
                if (tr == null || !(invisibleAlso || tr.isVisible())) {
                    taskNdx = taskNdx2 - 1;
                }
            }
            return tr.taskId;
        } else {
            tr = stack.topTask();
            if (tr != null && (invisibleAlso || tr.isVisible())) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getSpecialTaskId tr.taskId = ");
                stringBuilder2.append(tr.taskId);
                HwPCUtils.log("ActivityManager", stringBuilder2.toString());
                return tr.taskId;
            }
        }
        return -1;
    }

    public int getTopTaskIdInDisplay(int displayId, String pkgName, boolean invisibleAlso) {
        int N = this.mActivityDisplays.size();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTopTaskIdInDisplay displayId = ");
        stringBuilder.append(displayId);
        stringBuilder.append(", N = ");
        stringBuilder.append(N);
        stringBuilder.append(", pkgName = ");
        stringBuilder.append(pkgName);
        HwPCUtils.log("ActivityManager", stringBuilder.toString());
        if (displayId < 0) {
            return -1;
        }
        ActivityDisplay activityDisplay = getActivityDisplay(displayId);
        if (activityDisplay == null) {
            HwPCUtils.log("ActivityManager", "getTopTaskIdInDisplay activityDisplay not exist");
            return -1;
        }
        for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            int taskId = getSpecialTaskId(displayId == 0 ? 1 : 0, activityDisplay.getChildAt(stackNdx), pkgName, invisibleAlso);
            if (taskId != -1) {
                return taskId;
            }
        }
        return -1;
    }

    public Rect getPCTopTaskBounds(int displayId) {
        int N = this.mActivityDisplays.size();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPCTopTaskBounds displayId = ");
        stringBuilder.append(displayId);
        stringBuilder.append(", N = ");
        stringBuilder.append(N);
        HwPCUtils.log("ActivityManager", stringBuilder.toString());
        if (displayId < 0) {
            return null;
        }
        ActivityDisplay activityDisplay = getActivityDisplay(displayId);
        if (activityDisplay == null) {
            HwPCUtils.log("ActivityManager", "getPCTopTaskBounds activityDisplay not exist");
            return null;
        }
        Rect rect = new Rect();
        int stackNdx = activityDisplay.getChildCount() - 1;
        while (stackNdx >= 0) {
            TaskRecord tr = activityDisplay.getChildAt(stackNdx).topTask();
            if (tr == null || !tr.isVisible()) {
                stackNdx--;
            } else {
                tr.getWindowContainerBounds(rect);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getTaskIdInPCDisplayLocked tr.taskId = ");
                stringBuilder2.append(tr.taskId);
                stringBuilder2.append(", rect = ");
                stringBuilder2.append(rect);
                HwPCUtils.log("ActivityManager", stringBuilder2.toString());
                return rect;
            }
        }
        return null;
    }

    protected void uploadUnSupportSplitScreenAppPackageName(String pkgName) {
        if (this.mService.mContext != null) {
            Context context = this.mService.mContext;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ Launcher fail, pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(" }");
            StatisticalUtils.reporte(context, 174, stringBuilder.toString());
        }
    }

    protected ActivityStack getTargetSplitTopStack(ActivityStack current) {
        ActivityStack topSecondaryStack;
        if (current.getWindowingMode() == 3) {
            topSecondaryStack = current.getDisplay().getTopStackInWindowingMode(4);
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
            topSecondaryStack = getNextStackInSplitSecondary(current);
            ActivityRecord nextTargetRecord = null;
            if (topSecondaryStack != null) {
                nextTargetRecord = topSecondaryStack.topRunningActivityLocked();
            }
            if (nextTargetRecord == null || !nextTargetRecord.info.name.contains(SPLIT_SCREEN_APP_NAME)) {
                return null;
            }
            ActivityStack topPrimaryStack = current.getDisplay().getTopStackInWindowingMode(3);
            if (topPrimaryStack != null) {
                return topPrimaryStack;
            }
            return null;
        }
    }

    protected ActivityStack getNextStackInSplitSecondary(ActivityStack current) {
        ArrayList<ActivityStack> mStacks = current.getDisplay().mStacks;
        boolean returnNext = false;
        for (int i = mStacks.size() - 1; i >= 0; i--) {
            ActivityStack targetStack = (ActivityStack) mStacks.get(i);
            if (returnNext) {
                if (!targetStack.topRunningActivityLocked().info.name.contains(STACK_DIVIDER_APP_NAME) && targetStack.getWindowingMode() == 4) {
                    return targetStack;
                }
            } else if (targetStack.getStackId() == current.getStackId()) {
                returnNext = true;
            }
        }
        return null;
    }

    private ArrayList<ProcessRecord> getPCProcessRecordList() {
        ArrayList<ProcessRecord> procs = new ArrayList();
        int NP = this.mService.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = (SparseArray) this.mService.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord proc = (ProcessRecord) apps.valueAt(ia);
                if (!(proc == this.mService.mHomeProcess || proc.mDisplayId == 0)) {
                    procs.add(proc);
                }
            }
        }
        return procs;
    }

    private void killPCProcessesLocked(ArrayList<ProcessRecord> procs) {
        int NU = procs.size();
        for (int iu = 0; iu < NU; iu++) {
            ProcessRecord pr = (ProcessRecord) procs.get(iu);
            pr.kill("HwPCUtils#DisplayRemoved", true);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("killPCProcessesLocked: pr = ");
            stringBuilder.append(pr);
            HwPCUtils.log("ActivityManager", stringBuilder.toString());
        }
    }

    private void removeProcessesActivityNotFinished(ArrayList<ActivityRecord> activities, ArrayList<ProcessRecord> procs) {
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
            int NP = procs.size();
            for (int i = 0; i < NP; i++) {
                if (((ProcessRecord) procs.get(i)).processName.equals(r.processName)) {
                    procs.remove(i);
                    break;
                }
            }
        }
    }
}
