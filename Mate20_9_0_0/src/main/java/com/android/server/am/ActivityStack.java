package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.IApplicationThread;
import android.app.ResultInfo;
import android.app.WindowConfiguration;
import android.app.WindowConfiguration.ActivityType;
import android.app.WindowConfiguration.WindowingMode;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.DestroyActivityItem;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.app.servertransaction.StopActivityItem;
import android.app.servertransaction.WindowVisibilityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwSlog;
import android.util.HwVRUtils;
import android.util.IntArray;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BatteryStatsImpl.Uid.Proc;
import com.android.server.LocalServices;
import com.android.server.Watchdog;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.StackWindowController;
import com.android.server.wm.StackWindowListener;
import com.android.server.wm.WindowManagerService;
import com.huawei.android.audio.HwAudioServiceManager;
import com.huawei.pgmng.log.LogPower;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ActivityStack<T extends StackWindowController> extends AbsActivityStack implements StackWindowListener {
    static final int DESTROY_ACTIVITIES_MSG = 105;
    private static final int DESTROY_TIMEOUT = 10000;
    static final int DESTROY_TIMEOUT_MSG = 102;
    static final int FINISH_AFTER_PAUSE = 1;
    static final int FINISH_AFTER_VISIBLE = 2;
    static final int FINISH_IMMEDIATELY = 0;
    private static final int FIT_WITHIN_BOUNDS_DIVIDER = 3;
    static final int LAUNCH_TICK = 500;
    static final int LAUNCH_TICK_MSG = 103;
    private static final int MAX_STOPPING_TO_FORCE = 3;
    private static final int PAUSE_TIMEOUT = 500;
    static final int PAUSE_TIMEOUT_MSG = 101;
    @VisibleForTesting
    protected static final int REMOVE_TASK_MODE_DESTROYING = 0;
    static final int REMOVE_TASK_MODE_MOVING = 1;
    static final int REMOVE_TASK_MODE_MOVING_TO_TOP = 2;
    private static final String SETTINGS_DASHBROAED_ACTIVITY_NAME = "com.android.settings.Settings$AppAndNotificationDashboardActivity";
    private static final boolean SHOW_APP_STARTING_PREVIEW = true;
    private static final int STOP_TIMEOUT = 11000;
    static final int STOP_TIMEOUT_MSG = 104;
    protected static final String TAG = "ActivityManager";
    private static final String TAG_ADD_REMOVE = "ActivityManager";
    private static final String TAG_APP = "ActivityManager";
    private static final String TAG_CLEANUP = "ActivityManager";
    private static final String TAG_CONTAINERS = "ActivityManager";
    private static final String TAG_PAUSE = "ActivityManager";
    private static final String TAG_RELEASE = "ActivityManager";
    private static final String TAG_RESULTS = "ActivityManager";
    private static final String TAG_SAVED_STATE = "ActivityManager";
    private static final String TAG_STACK = "ActivityManager";
    private static final String TAG_STATES = "ActivityManager";
    private static final String TAG_SWITCH = "ActivityManager";
    private static final String TAG_TASKS = "ActivityManager";
    private static final String TAG_TRANSITION = "ActivityManager";
    private static final String TAG_USER_LEAVING = "ActivityManager";
    private static final String TAG_VISIBILITY;
    private static final long TRANSLUCENT_CONVERSION_TIMEOUT = 2000;
    static final int TRANSLUCENT_TIMEOUT_MSG = 106;
    protected static final boolean VALIDATE_TOKENS = false;
    boolean mConfigWillChange;
    int mCurrentUser;
    private final Rect mDeferredBounds;
    private final Rect mDeferredTaskBounds;
    private final Rect mDeferredTaskInsetBounds;
    int mDisplayId;
    boolean mForceHidden;
    final Handler mHandler;
    final ArrayList<ActivityRecord> mLRUActivities = new ArrayList();
    ActivityRecord mLastNoHistoryActivity;
    ActivityRecord mLastPausedActivity;
    ActivityRecord mPausingActivity;
    ActivityRecord mResumedActivity;
    final ActivityManagerService mService;
    final int mStackId;
    protected final ActivityStackSupervisor mStackSupervisor;
    protected ArrayList<TaskRecord> mTaskHistory = new ArrayList();
    private final ArrayList<ActivityRecord> mTmpActivities;
    private final SparseArray<Rect> mTmpBounds;
    private final SparseArray<Rect> mTmpInsetBounds;
    private final ActivityOptions mTmpOptions;
    private final Rect mTmpRect2;
    private boolean mTopActivityOccludesKeyguard;
    private ActivityRecord mTopDismissingKeyguardActivity;
    ActivityRecord mTranslucentActivityWaiting;
    ArrayList<ActivityRecord> mUndrawnActivitiesBelowTopTranslucent;
    private boolean mUpdateBoundsDeferred;
    private boolean mUpdateBoundsDeferredCalled;
    T mWindowContainerController;
    private final WindowManagerService mWindowManager;
    String mshortComponentName;

    private class ActivityStackHandler extends Handler {
        ActivityStackHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            IBinder iBinder = null;
            ActivityRecord r;
            ProcessRecord processRecord;
            long j;
            StringBuilder stringBuilder;
            String str;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 101:
                    r = (ActivityRecord) msg.obj;
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Activity pause timeout for ");
                    stringBuilder3.append(r);
                    Slog.w(str2, stringBuilder3.toString());
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r.app != null) {
                                ActivityManagerService activityManagerService = ActivityStack.this.mService;
                                processRecord = r.app;
                                j = r.pauseTime;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("pausing ");
                                stringBuilder.append(r);
                                activityManagerService.logAppTooSlow(processRecord, j, stringBuilder.toString());
                            }
                            ActivityStack.this.activityPausedLocked(r.appToken, true);
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 102:
                    r = (ActivityRecord) msg.obj;
                    str = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Activity destroy timeout for ");
                    stringBuilder2.append(r);
                    Slog.w(str, stringBuilder2.toString());
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack activityStack = ActivityStack.this;
                            if (r != null) {
                                iBinder = r.appToken;
                            }
                            activityStack.activityDestroyedLocked(iBinder, "destroyTimeout");
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 103:
                    r = (ActivityRecord) msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r.continueLaunchTickingLocked()) {
                                ActivityManagerService activityManagerService2 = ActivityStack.this.mService;
                                processRecord = r.app;
                                j = r.launchTickTime;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("launching ");
                                stringBuilder.append(r);
                                activityManagerService2.logAppTooSlow(processRecord, j, stringBuilder.toString());
                            }
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 104:
                    r = msg.obj;
                    str = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Activity stop timeout for ");
                    stringBuilder2.append(r);
                    Slog.w(str, stringBuilder2.toString());
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r.isInHistory()) {
                                r.activityStoppedLocked(null, null, null);
                            }
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 105:
                    ScheduleDestroyArgs args = msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.destroyActivitiesLocked(args.mOwner, args.mReason);
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 106:
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.notifyActivityDrawnLocked(null);
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    enum ActivityState {
        INITIALIZING,
        RESUMED,
        PAUSING,
        PAUSED,
        STOPPING,
        STOPPED,
        FINISHING,
        DESTROYING,
        DESTROYED
    }

    private static class ScheduleDestroyArgs {
        final ProcessRecord mOwner;
        final String mReason;

        ScheduleDestroyArgs(ProcessRecord owner, String reason) {
            this.mOwner = owner;
            this.mReason = reason;
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ActivityManagerService.TAG);
        stringBuilder.append(ActivityManagerDebugConfig.POSTFIX_VISIBILITY);
        TAG_VISIBILITY = stringBuilder.toString();
    }

    protected int getChildCount() {
        return this.mTaskHistory.size();
    }

    protected ConfigurationContainer getChildAt(int index) {
        return (ConfigurationContainer) this.mTaskHistory.get(index);
    }

    protected ConfigurationContainer getParent() {
        return getDisplay();
    }

    protected void onParentChanged() {
        super.onParentChanged();
        this.mStackSupervisor.updateUIDsPresentOnDisplay();
    }

    int numActivities() {
        int count = 0;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            count += ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities.size();
        }
        return count;
    }

    public ActivityStack(ActivityDisplay display, int stackId, ActivityStackSupervisor supervisor, int windowingMode, int activityType, boolean onTop) {
        Rect rect = null;
        this.mPausingActivity = null;
        this.mLastPausedActivity = null;
        this.mLastNoHistoryActivity = null;
        this.mResumedActivity = null;
        this.mTranslucentActivityWaiting = null;
        this.mUndrawnActivitiesBelowTopTranslucent = new ArrayList();
        this.mForceHidden = false;
        this.mDeferredBounds = new Rect();
        this.mDeferredTaskBounds = new Rect();
        this.mDeferredTaskInsetBounds = new Rect();
        this.mTmpBounds = new SparseArray();
        this.mTmpInsetBounds = new SparseArray();
        this.mTmpRect2 = new Rect();
        this.mTmpOptions = ActivityOptions.makeBasic();
        this.mTmpActivities = new ArrayList();
        this.mshortComponentName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        this.mStackSupervisor = supervisor;
        this.mService = supervisor.mService;
        this.mHandler = new ActivityStackHandler(this.mService.mHandler.getLooper());
        this.mWindowManager = this.mService.mWindowManager;
        this.mStackId = stackId;
        this.mCurrentUser = this.mService.mUserController.getCurrentUserId();
        this.mTmpRect2.setEmpty();
        this.mDisplayId = display.mDisplayId;
        setActivityType(activityType);
        setWindowingMode(windowingMode);
        this.mWindowContainerController = createStackWindowController(display.mDisplayId, onTop, this.mTmpRect2);
        if (!this.mTmpRect2.isEmpty()) {
            rect = this.mTmpRect2;
        }
        postAddToDisplay(display, rect, onTop);
    }

    T createStackWindowController(int displayId, boolean onTop, Rect outBounds) {
        return new StackWindowController(this.mStackId, this, displayId, onTop, outBounds, this.mStackSupervisor.mWindowManager);
    }

    T getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void onActivityStateChanged(ActivityRecord record, ActivityState state, String reason) {
        StringBuilder stringBuilder;
        if (record == this.mResumedActivity && state != ActivityState.RESUMED) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(reason);
            stringBuilder.append(" - onActivityStateChanged");
            setResumedActivity(null, stringBuilder.toString());
        }
        if (state == ActivityState.RESUMED) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                String str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set resumed activity to:");
                stringBuilder.append(record);
                stringBuilder.append(" reason:");
                stringBuilder.append(reason);
                Slog.v(str, stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(reason);
            stringBuilder2.append(" - onActivityStateChanged");
            setResumedActivity(record, stringBuilder2.toString());
            this.mService.setResumedActivityUncheckLocked(record, reason);
            this.mStackSupervisor.mRecentTasks.add(record.getTask());
        }
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        int prevWindowingMode = getWindowingMode();
        super.onConfigurationChanged(newParentConfig);
        ActivityDisplay display = getDisplay();
        if (display != null && prevWindowingMode != getWindowingMode()) {
            display.onStackWindowingModeChanged(this);
        }
    }

    public void setWindowingMode(int windowingMode) {
        setWindowingMode(windowingMode, false, false, false, false);
    }

    void setWindowingMode(int preferredWindowingMode, boolean animate, boolean showRecents, boolean enteringSplitScreenMode, boolean deferEnsuringVisibility) {
        int i = preferredWindowingMode;
        boolean creating = this.mWindowContainerController == null;
        int currentMode = getWindowingMode();
        ActivityDisplay display = getDisplay();
        TaskRecord topTask = topTask();
        AbsActivityStack splitScreenStack = display.getSplitScreenPrimaryStack();
        this.mTmpOptions.setLaunchWindowingMode(i);
        int windowingMode = creating ? i : display.resolveWindowingMode(null, this.mTmpOptions, topTask, getActivityType());
        if (splitScreenStack == this && windowingMode == 4) {
            windowingMode = 1;
        }
        boolean alreadyInSplitScreenMode = display.hasSplitScreenPrimaryStack();
        boolean sendNonResizeableNotification = enteringSplitScreenMode ^ 1;
        if (alreadyInSplitScreenMode && windowingMode == 1 && sendNonResizeableNotification && isActivityTypeStandardOrUndefined()) {
            boolean preferredSplitScreen = i == 3 || i == 4;
            if (preferredSplitScreen || creating) {
                this.mService.mTaskChangeNotificationController.notifyActivityDismissingDockedStack();
                display.getSplitScreenPrimaryStack().setWindowingMode(1, false, false, false, true);
            }
        }
        if (currentMode != windowingMode) {
            WindowManagerService wm = this.mService.mWindowManager;
            ActivityRecord topActivity = getTopActivity();
            if (!(!sendNonResizeableNotification || windowingMode == 1 || topActivity == null || !topActivity.isNonResizableOrForcedResizable() || topActivity.noDisplay)) {
                this.mService.mTaskChangeNotificationController.notifyActivityForcedResizable(topTask.taskId, 1, topActivity.appInfo.packageName);
            }
            wm.deferSurfaceLayout();
            if (!(animate || topActivity == null)) {
                try {
                    this.mStackSupervisor.mNoAnimActivities.add(topActivity);
                } catch (Throwable th) {
                    wm.continueSurfaceLayout();
                }
            }
            super.setWindowingMode(windowingMode);
            if (creating) {
                if (showRecents && !alreadyInSplitScreenMode) {
                    try {
                        if (this.mDisplayId == 0 && windowingMode == 3) {
                            display.getOrCreateStack(4, 3, true).moveToFront("setWindowingMode");
                            this.mService.mWindowManager.showRecentApps();
                        }
                    } catch (Throwable th2) {
                        wm.continueSurfaceLayout();
                    }
                }
                wm.continueSurfaceLayout();
            } else if (windowingMode == 2 || currentMode == 2) {
                throw new IllegalArgumentException("Changing pinned windowing mode not currently supported");
            } else if (windowingMode != 3 || splitScreenStack == null) {
                this.mTmpRect2.setEmpty();
                if (windowingMode != 1) {
                    this.mWindowContainerController.getRawBounds(this.mTmpRect2);
                    if (windowingMode == 5 && topTask != null) {
                        Rect bounds = topTask().getLaunchBounds();
                        if (bounds != null) {
                            this.mTmpRect2.set(bounds);
                        }
                    }
                }
                if (!Objects.equals(getOverrideBounds(), this.mTmpRect2)) {
                    resize(this.mTmpRect2, null, null);
                }
                if (showRecents && !alreadyInSplitScreenMode) {
                    try {
                        if (this.mDisplayId == 0 && windowingMode == 3) {
                            display.getOrCreateStack(4, 3, true).moveToFront("setWindowingMode");
                            this.mService.mWindowManager.showRecentApps();
                        }
                    } catch (Throwable th3) {
                        wm.continueSurfaceLayout();
                    }
                }
                wm.continueSurfaceLayout();
                if (!deferEnsuringVisibility) {
                    this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, true);
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            } else {
                throw new IllegalArgumentException("Setting primary split-screen windowing mode while there is already one isn't currently supported");
            }
        }
    }

    public boolean isCompatible(int windowingMode, int activityType) {
        if (activityType == 0) {
            activityType = 1;
        }
        ActivityDisplay display = getDisplay();
        if (display != null && activityType == 1 && windowingMode == 0) {
            windowingMode = display.getWindowingMode();
        }
        return super.isCompatible(windowingMode, activityType);
    }

    void reparent(ActivityDisplay activityDisplay, boolean onTop) {
        removeFromDisplay();
        this.mTmpRect2.setEmpty();
        this.mWindowContainerController.reparent(activityDisplay.mDisplayId, this.mTmpRect2, onTop);
        postAddToDisplay(activityDisplay, this.mTmpRect2.isEmpty() ? null : this.mTmpRect2, onTop);
        adjustFocusToNextFocusableStack("reparent", true);
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private void postAddToDisplay(ActivityDisplay activityDisplay, Rect bounds, boolean onTop) {
        this.mDisplayId = activityDisplay.mDisplayId;
        setBounds(bounds);
        onParentChanged();
        activityDisplay.addChild(this, onTop ? HwBootFail.STAGE_BOOT_SUCCESS : Integer.MIN_VALUE);
        if (inSplitScreenPrimaryWindowingMode()) {
            this.mStackSupervisor.resizeDockedStackLocked(getOverrideBounds(), null, null, null, null, true);
        }
    }

    private void removeFromDisplay() {
        ActivityDisplay display = getDisplay();
        if (display != null) {
            display.removeChild(this);
        }
        this.mDisplayId = -1;
    }

    void remove() {
        removeFromDisplay();
        this.mWindowContainerController.removeContainer();
        this.mWindowContainerController = null;
        onParentChanged();
    }

    ActivityDisplay getDisplay() {
        return this.mStackSupervisor.getActivityDisplay(this.mDisplayId);
    }

    void getStackDockedModeBounds(Rect currentTempTaskBounds, Rect outStackBounds, Rect outTempTaskBounds, boolean ignoreVisibility) {
        this.mWindowContainerController.getStackDockedModeBounds(currentTempTaskBounds, outStackBounds, outTempTaskBounds, ignoreVisibility);
    }

    void prepareFreezingTaskBounds() {
        this.mWindowContainerController.prepareFreezingTaskBounds();
    }

    void getWindowContainerBounds(Rect outBounds) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.getBounds(outBounds);
        } else {
            outBounds.setEmpty();
        }
    }

    void getBoundsForNewConfiguration(Rect outBounds) {
        this.mWindowContainerController.getBoundsForNewConfiguration(outBounds);
    }

    void positionChildWindowContainerAtTop(TaskRecord child) {
        this.mWindowContainerController.positionChildAtTop(child.getWindowContainerController(), true);
    }

    boolean deferScheduleMultiWindowModeChanged() {
        return false;
    }

    void deferUpdateBounds() {
        if (!this.mUpdateBoundsDeferred) {
            this.mUpdateBoundsDeferred = true;
            this.mUpdateBoundsDeferredCalled = false;
        }
    }

    void continueUpdateBounds() {
        boolean wasDeferred = this.mUpdateBoundsDeferred;
        this.mUpdateBoundsDeferred = false;
        if (wasDeferred && this.mUpdateBoundsDeferredCalled) {
            Rect rect;
            Rect rect2;
            Rect rect3 = null;
            if (this.mDeferredBounds.isEmpty()) {
                rect = null;
            } else {
                rect = this.mDeferredBounds;
            }
            if (this.mDeferredTaskBounds.isEmpty()) {
                rect2 = null;
            } else {
                rect2 = this.mDeferredTaskBounds;
            }
            if (!this.mDeferredTaskInsetBounds.isEmpty()) {
                rect3 = this.mDeferredTaskInsetBounds;
            }
            resize(rect, rect2, rect3);
        }
    }

    boolean updateBoundsAllowed(Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds) {
        if (!this.mUpdateBoundsDeferred) {
            return true;
        }
        if (bounds != null) {
            this.mDeferredBounds.set(bounds);
        } else {
            this.mDeferredBounds.setEmpty();
        }
        if (tempTaskBounds != null) {
            this.mDeferredTaskBounds.set(tempTaskBounds);
        } else {
            this.mDeferredTaskBounds.setEmpty();
        }
        if (tempTaskInsetBounds != null) {
            this.mDeferredTaskInsetBounds.set(tempTaskInsetBounds);
        } else {
            this.mDeferredTaskInsetBounds.setEmpty();
        }
        this.mUpdateBoundsDeferredCalled = true;
        return false;
    }

    public int setBounds(Rect bounds) {
        return super.setBounds(!inMultiWindowMode() ? null : bounds);
    }

    ActivityRecord topRunningActivityLocked() {
        return topRunningActivityLocked(false);
    }

    void getAllRunningVisibleActivitiesLocked(ArrayList<ActivityRecord> outActivities) {
        outActivities.clear();
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ((TaskRecord) this.mTaskHistory.get(taskNdx)).getAllRunningVisibleActivitiesLocked(outActivities);
        }
    }

    private ActivityRecord topRunningActivityLocked(boolean focusableOnly) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ActivityRecord r = ((TaskRecord) this.mTaskHistory.get(taskNdx)).topRunningActivityLocked();
            if (r != null && (!focusableOnly || r.isFocusable())) {
                return r;
            }
        }
        return null;
    }

    ActivityRecord topRunningNonOverlayTaskActivity() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (!r.finishing && !r.mTaskOverlay) {
                    return r;
                }
            }
        }
        return null;
    }

    ActivityRecord topRunningNonDelayedActivityLocked(ActivityRecord notTop) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (!r.finishing && !r.delayedResume && r != notTop && r.okToShowLocked()) {
                    return r;
                }
            }
        }
        return null;
    }

    final ActivityRecord topRunningActivityLocked(IBinder token, int taskId) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx);
            if (task.taskId != taskId) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int i = activities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = (ActivityRecord) activities.get(i);
                    if (!r.finishing && token != r.appToken && r.okToShowLocked()) {
                        return r;
                    }
                }
                continue;
            }
        }
        return null;
    }

    ActivityRecord getTopActivity() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ActivityRecord r = ((TaskRecord) this.mTaskHistory.get(taskNdx)).getTopActivity();
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    final TaskRecord topTask() {
        int size = this.mTaskHistory.size();
        if (size > 0) {
            return (TaskRecord) this.mTaskHistory.get(size - 1);
        }
        return null;
    }

    private TaskRecord bottomTask() {
        if (this.mTaskHistory.isEmpty()) {
            return null;
        }
        return (TaskRecord) this.mTaskHistory.get(0);
    }

    TaskRecord taskForIdLocked(int id) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx);
            if (task.taskId == id) {
                return task;
            }
        }
        return null;
    }

    ActivityRecord isInStackLocked(IBinder token) {
        return isInStackLocked(ActivityRecord.forTokenLocked(token));
    }

    ActivityRecord isInStackLocked(ActivityRecord r) {
        if (r == null) {
            return null;
        }
        TaskRecord task = r.getTask();
        ActivityStack stack = r.getStack();
        if (stack == null || !task.mActivities.contains(r) || !this.mTaskHistory.contains(task)) {
            return null;
        }
        if (stack != this) {
            Slog.w(ActivityManagerService.TAG, "Illegal state! task does not point to stack it is in.");
        }
        return r;
    }

    boolean isInStackLocked(TaskRecord task) {
        return this.mTaskHistory.contains(task);
    }

    boolean isUidPresent(int uid) {
        Iterator it = this.mTaskHistory.iterator();
        while (it.hasNext()) {
            Iterator it2 = ((TaskRecord) it.next()).mActivities.iterator();
            while (it2.hasNext()) {
                if (((ActivityRecord) it2.next()).getUid() == uid) {
                    return true;
                }
            }
        }
        return false;
    }

    void getPresentUIDs(IntArray presentUIDs) {
        Iterator it = this.mTaskHistory.iterator();
        while (it.hasNext()) {
            Iterator it2 = ((TaskRecord) it.next()).mActivities.iterator();
            while (it2.hasNext()) {
                presentUIDs.add(((ActivityRecord) it2.next()).getUid());
            }
        }
    }

    final void removeActivitiesFromLRUListLocked(TaskRecord task) {
        Iterator it = task.mActivities.iterator();
        while (it.hasNext()) {
            this.mLRUActivities.remove((ActivityRecord) it.next());
        }
    }

    final boolean updateLRUListLocked(ActivityRecord r) {
        boolean hadit = this.mLRUActivities.remove(r);
        this.mLRUActivities.add(r);
        return hadit;
    }

    final boolean isHomeOrRecentsStack() {
        return isActivityTypeHome() || isActivityTypeRecents();
    }

    final boolean isOnHomeDisplay() {
        return this.mDisplayId == 0;
    }

    private boolean returnsToHomeStack() {
        if (inMultiWindowMode() || this.mTaskHistory.isEmpty() || !((TaskRecord) this.mTaskHistory.get(0)).returnsToHomeStack()) {
            return false;
        }
        return true;
    }

    void moveToFront(String reason) {
        moveToFront(reason, null);
    }

    protected void moveToFront(String reason, TaskRecord task) {
        if (isAttached()) {
            ActivityDisplay display = getDisplay();
            if (inSplitScreenSecondaryWindowingMode()) {
                ActivityStack topFullScreenStack = display.getTopStackInWindowingMode(1);
                if (topFullScreenStack != null) {
                    ActivityStack primarySplitScreenStack = display.getSplitScreenPrimaryStack();
                    if (primarySplitScreenStack != null && display.getIndexOf(topFullScreenStack) > display.getIndexOf(primarySplitScreenStack)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(reason);
                        stringBuilder.append(" splitScreenToTop");
                        primarySplitScreenStack.moveToFront(stringBuilder.toString());
                    }
                }
            }
            if (!isActivityTypeHome() && returnsToHomeStack()) {
                ActivityStackSupervisor activityStackSupervisor = this.mStackSupervisor;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(reason);
                stringBuilder2.append(" returnToHome");
                activityStackSupervisor.moveHomeStackToFront(stringBuilder2.toString());
            }
            display.positionChildAtTop(this);
            this.mStackSupervisor.setFocusStackUnchecked(reason, this);
            if (task != null) {
                insertTaskAtTop(task, null);
            }
        }
    }

    void moveToBack(String reason, TaskRecord task) {
        if (isAttached()) {
            ActivityStack targetStack = this.mStackSupervisor.getTargetSplitTopStack(this);
            if (targetStack != null) {
                this.mWindowManager.mShouldResetTime = true;
                this.mWindowManager.startFreezingScreen(0, 0);
                getDisplay().positionChildAtTop(targetStack);
                if (getWindowingMode() != 3) {
                    targetStack.setWindowingMode(1);
                }
            }
            if (getWindowingMode() == 3) {
                setWindowingMode(1);
            }
            if (targetStack != null) {
                this.mWindowManager.stopFreezingScreen();
            }
            getDisplay().positionChildAtBottom(this);
            this.mStackSupervisor.setFocusStackUnchecked(reason, targetStack != null ? targetStack : getDisplay().getTopStack());
            if (task != null) {
                insertTaskAtBottom(task);
            }
        }
    }

    boolean isFocusable() {
        ActivityRecord r = topRunningActivityLocked();
        ActivityStackSupervisor activityStackSupervisor = this.mStackSupervisor;
        boolean z = r != null && r.isFocusable();
        return activityStackSupervisor.isFocusable(this, z);
    }

    final boolean isAttached() {
        return getParent() != null;
    }

    void findTaskLocked(ActivityRecord target, FindTaskResult result) {
        int userId;
        ActivityStack activityStack = this;
        ConfigurationContainer configurationContainer = target;
        FindTaskResult findTaskResult = result;
        Intent intent = configurationContainer.intent;
        ActivityInfo info = configurationContainer.info;
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId2 = UserHandle.getUserId(info.applicationInfo.uid);
        boolean z = false;
        boolean isDocument = (intent != null) & intent.isDocument();
        Uri documentData = isDocument ? intent.getData() : null;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Looking for task of ");
            stringBuilder.append(configurationContainer);
            stringBuilder.append(" in ");
            stringBuilder.append(activityStack);
            Slog.d(str, stringBuilder.toString());
        }
        int taskNdx = activityStack.mTaskHistory.size() - 1;
        while (taskNdx >= 0) {
            ActivityInfo info2;
            TaskRecord task = (TaskRecord) activityStack.mTaskHistory.get(taskNdx);
            String str2;
            StringBuilder stringBuilder2;
            if (task.voiceSession != null) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping ");
                    stringBuilder2.append(task);
                    stringBuilder2.append(": voice session");
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if (task.userId == userId2) {
                ActivityRecord r = task.getTopActivity(z);
                if (r == null || r.finishing) {
                    info2 = info;
                    userId = userId2;
                } else if ((r.userId != userId2 && (!activityStack.mStackSupervisor.isCurrentProfileLocked(r.userId) || !activityStack.mStackSupervisor.isCurrentProfileLocked(userId2))) || r.launchMode == 3) {
                    info2 = info;
                    userId = userId2;
                } else if (r.hasCompatibleActivityType(configurationContainer)) {
                    Uri taskDocumentData;
                    StringBuilder stringBuilder3;
                    Intent taskIntent = task.intent;
                    Intent affinityIntent = task.affinityIntent;
                    boolean taskIsDocument;
                    if (taskIntent != null && taskIntent.isDocument()) {
                        taskIsDocument = true;
                        taskDocumentData = taskIntent.getData();
                    } else if (affinityIntent == null || !affinityIntent.isDocument()) {
                        taskIsDocument = false;
                        taskDocumentData = null;
                    } else {
                        taskIsDocument = true;
                        taskDocumentData = affinityIntent.getData();
                    }
                    Uri taskDocumentData2 = taskDocumentData;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        String str3 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        userId = userId2;
                        stringBuilder4.append("Comparing existing cls=");
                        stringBuilder4.append(taskIntent.getComponent().flattenToShortString());
                        stringBuilder4.append("/aff=");
                        stringBuilder4.append(r.getTask().rootAffinity);
                        stringBuilder4.append(" to new cls=");
                        stringBuilder4.append(intent.getComponent().flattenToShortString());
                        stringBuilder4.append("/aff=");
                        stringBuilder4.append(info.taskAffinity);
                        Slog.d(str3, stringBuilder4.toString());
                    } else {
                        userId = userId2;
                    }
                    Uri taskDocumentData3;
                    if (taskIntent == null || taskIntent.getComponent() == null || taskIntent.getComponent().compareTo(cls) != 0) {
                        info2 = info;
                        taskDocumentData3 = taskDocumentData2;
                    } else {
                        taskDocumentData3 = taskDocumentData2;
                        if (Objects.equals(documentData, taskDocumentData3)) {
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                String str4 = ActivityManagerService.TAG;
                                stringBuilder3 = new StringBuilder();
                                info2 = info;
                                stringBuilder3.append("Found matching taskIntent for ");
                                stringBuilder3.append(intent);
                                stringBuilder3.append(" bringing to top: ");
                                stringBuilder3.append(r.intent);
                                Slog.d(str4, stringBuilder3.toString());
                            }
                            findTaskResult.r = r;
                            findTaskResult.matchedByRootAffinity = false;
                            return;
                        }
                        info2 = info;
                    }
                    String str5;
                    if (affinityIntent != null && affinityIntent.getComponent() != null && affinityIntent.getComponent().compareTo(cls) == 0 && Objects.equals(documentData, taskDocumentData3)) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            str5 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Found matching affinityIntent For ");
                            stringBuilder5.append(intent);
                            stringBuilder5.append(" bringing to top: ");
                            stringBuilder5.append(r.intent);
                            Slog.d(str5, stringBuilder5.toString());
                        }
                        findTaskResult.r = r;
                        findTaskResult.matchedByRootAffinity = false;
                        return;
                    } else if (isDocument || taskIsDocument || findTaskResult.r != null || task.rootAffinity == null) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            str5 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Not a match: ");
                            stringBuilder3.append(task);
                            Slog.d(str5, stringBuilder3.toString());
                        }
                        taskNdx--;
                        userId2 = userId;
                        info = info2;
                        activityStack = this;
                        z = false;
                    } else {
                        if (task.rootAffinity.equals(configurationContainer.taskAffinity)) {
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(ActivityManagerService.TAG, "Found matching affinity candidate!");
                            }
                            findTaskResult.r = r;
                            findTaskResult.matchedByRootAffinity = true;
                        } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(ActivityManagerService.TAG, "Not found matching affinity candidate!");
                        }
                        taskNdx--;
                        userId2 = userId;
                        info = info2;
                        activityStack = this;
                        z = false;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping ");
                    stringBuilder2.append(task);
                    stringBuilder2.append(": mismatch activity type");
                    Slog.d(str2, stringBuilder2.toString());
                }
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    String str6 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Skipping ");
                    stringBuilder6.append(task);
                    stringBuilder6.append(": mismatch root ");
                    stringBuilder6.append(r);
                    Slog.d(str6, stringBuilder6.toString());
                }
                taskNdx--;
                userId2 = userId;
                info = info2;
                activityStack = this;
                z = false;
            } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                String str7 = ActivityManagerService.TAG;
                StringBuilder stringBuilder7 = new StringBuilder();
                stringBuilder7.append("Skipping ");
                stringBuilder7.append(task);
                stringBuilder7.append(": different user");
                Slog.d(str7, stringBuilder7.toString());
            }
            info2 = info;
            userId = userId2;
            taskNdx--;
            userId2 = userId;
            info = info2;
            activityStack = this;
            z = false;
        }
        userId = userId2;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.okToShowLocked() && !r.finishing && r.userId == userId) {
                    if (compareIntentFilters) {
                        if (r.intent.filterEquals(intent)) {
                            return r;
                        }
                    } else if (r.intent.getComponent().equals(cls)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    final void switchUserLocked(int userId) {
        if (this.mCurrentUser != userId) {
            this.mCurrentUser = userId;
            int index = this.mTaskHistory.size();
            int i = 0;
            while (i < index) {
                TaskRecord task = (TaskRecord) this.mTaskHistory.get(i);
                ensureActivitiesVisibleLockedForSwitchUser(task);
                if (task.okToShowLocked()) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("switchUserLocked: stack=");
                        stringBuilder.append(getStackId());
                        stringBuilder.append(" moving ");
                        stringBuilder.append(task);
                        stringBuilder.append(" to top");
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mTaskHistory.remove(i);
                    this.mTaskHistory.add(task);
                    index--;
                } else {
                    i++;
                }
            }
        }
    }

    void ensureActivitiesVisibleLockedForSwitchUser(TaskRecord task) {
        if (!this.mStackSupervisor.isCurrentProfileLocked(task.userId)) {
            ActivityRecord top = task.getTopActivity();
            if (top != null && top != task.topRunningActivityLocked() && top.visible && top.isState(ActivityState.STOPPING, ActivityState.STOPPED)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Making invisible for switch user:  top: ");
                stringBuilder.append(top);
                stringBuilder.append(", finishing: ");
                stringBuilder.append(top.finishing);
                stringBuilder.append(" state: ");
                stringBuilder.append(top.getState());
                Flog.i(101, stringBuilder.toString());
                try {
                    top.setVisible(false);
                    switch (top.getState()) {
                        case STOPPING:
                        case STOPPED:
                            if (top.app != null && top.app.thread != null) {
                                this.mService.getLifecycleManager().scheduleTransaction(top.app.thread, top.appToken, WindowVisibilityItem.obtain(false));
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                } catch (Exception e) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("for switch user Exception thrown making hidden: ");
                    stringBuilder2.append(top.intent.getComponent());
                    Slog.w(str, stringBuilder2.toString(), e);
                }
            }
        }
    }

    void minimalResumeActivityLocked(ActivityRecord r) {
        String str;
        StringBuilder stringBuilder;
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Moving to RESUMED: ");
            stringBuilder.append(r);
            stringBuilder.append(" (starting new instance) callers=");
            stringBuilder.append(Debug.getCallers(5));
            Slog.v(str, stringBuilder.toString());
        }
        if (!this.mService.getActivityStartController().mCurActivityPkName.equals(r.packageName)) {
            Jlog.d(142, r.packageName, r.app.pid, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            LogPower.push(113, r.packageName);
            this.mService.getActivityStartController().mCurActivityPkName = r.packageName;
        }
        r.setState(ActivityState.RESUMED, "minimalResumeActivityLocked");
        r.completeResumeLocked();
        this.mStackSupervisor.getLaunchTimeTracker().setLaunchTime(r);
        if (r.app != null && this.mService.mSystemReady) {
            this.mService.getDAMonitor().noteActivityDisplayedStart(r.shortComponentName, r.app.uid, r.app.pid);
        }
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Launch completed; removing icicle of ");
            stringBuilder.append(r.icicle);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void clearLaunchTime(ActivityRecord r) {
        if (this.mStackSupervisor.mWaitingActivityLaunched.isEmpty()) {
            r.fullyDrawnStartTime = 0;
            r.displayStartTime = 0;
            if (r.task != null) {
                r.task.isLaunching = false;
                return;
            }
            return;
        }
        this.mStackSupervisor.removeTimeoutsForActivityLocked(r);
        this.mStackSupervisor.scheduleIdleTimeoutLocked(r);
    }

    void awakeFromSleepingLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ((ActivityRecord) activities.get(activityNdx)).setSleeping(false);
            }
        }
        if (this.mPausingActivity != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Previously pausing activity ");
            stringBuilder.append(this.mPausingActivity.shortComponentName);
            stringBuilder.append(" state : ");
            stringBuilder.append(this.mPausingActivity.getState());
            Flog.i(101, stringBuilder.toString());
            activityPausedLocked(this.mPausingActivity.appToken, true);
        }
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo aInfo) {
        if (aInfo != null) {
            String packageName = aInfo.packageName;
            int userId = UserHandle.getUserId(aInfo.uid);
            for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
                List<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord ar = (ActivityRecord) activities.get(activityNdx);
                    if (userId == ar.userId && packageName.equals(ar.packageName)) {
                        ar.updateApplicationInfo(aInfo);
                    }
                }
            }
        }
    }

    void checkReadyForSleep() {
        if (shouldSleepActivities() && goToSleepIfPossible(false)) {
            this.mStackSupervisor.checkReadyForSleepLocked(true);
        }
    }

    boolean goToSleepIfPossible(boolean shuttingDown) {
        String str;
        StringBuilder stringBuilder;
        boolean shouldSleep = true;
        if (this.mResumedActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Sleep still need to pause ");
                stringBuilder.append(this.mResumedActivity);
                Slog.v(str, stringBuilder.toString());
            }
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(ActivityManagerService.TAG, "Sleep => pause with userLeaving=false");
            }
            startPausingLocked(false, true, null, false);
            shouldSleep = false;
        } else if (this.mPausingActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Sleep still waiting to pause ");
                stringBuilder.append(this.mPausingActivity);
                Slog.v(str, stringBuilder.toString());
            }
            shouldSleep = false;
        }
        if (!shuttingDown) {
            if (containsActivityFromStack(this.mStackSupervisor.mStoppingActivities)) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Sleep still need to stop ");
                    stringBuilder.append(this.mStackSupervisor.mStoppingActivities.size());
                    stringBuilder.append(" activities");
                    Slog.v(str, stringBuilder.toString());
                }
                this.mStackSupervisor.scheduleIdleLocked();
                shouldSleep = false;
            }
            if (containsActivityFromStack(this.mStackSupervisor.mGoingToSleepActivities)) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Sleep still need to sleep ");
                    stringBuilder.append(this.mStackSupervisor.mGoingToSleepActivities.size());
                    stringBuilder.append(" activities");
                    Slog.v(str, stringBuilder.toString());
                }
                shouldSleep = false;
            }
        }
        if (shouldSleep) {
            goToSleep();
        }
        return shouldSleep;
    }

    void goToSleep() {
        ensureActivitiesVisibleLocked(null, 0, false);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.isState(ActivityState.STOPPING, ActivityState.STOPPED, ActivityState.PAUSED, ActivityState.PAUSING)) {
                    r.setSleeping(true);
                }
            }
        }
    }

    private boolean containsActivityFromStack(List<ActivityRecord> rs) {
        for (ActivityRecord r : rs) {
            if (r.getStack() == this) {
                return true;
            }
        }
        return false;
    }

    private void schedulePauseTimeout(ActivityRecord r) {
        Message msg = this.mHandler.obtainMessage(101);
        msg.obj = r;
        r.pauseTime = SystemClock.uptimeMillis();
        this.mHandler.sendMessageDelayed(msg, 500);
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(ActivityManagerService.TAG, "Waiting for pause to complete...");
        }
    }

    final boolean startPausingLocked(boolean userLeaving, boolean uiSleeping, ActivityRecord resuming, boolean pauseImmediately) {
        StringBuilder stringBuilder;
        if (this.mPausingActivity != null) {
            String str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Going to pause when pause is already pending for ");
            stringBuilder.append(this.mPausingActivity);
            stringBuilder.append(" state=");
            stringBuilder.append(this.mPausingActivity.getState());
            Slog.wtf(str, stringBuilder.toString());
            if (!shouldSleepActivities()) {
                completePauseLocked(false, resuming);
            }
        }
        ActivityRecord prev = this.mResumedActivity;
        if (prev == null) {
            if (resuming == null) {
                Slog.wtf(ActivityManagerService.TAG, "Trying to pause when nothing is resumed");
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
            return false;
        } else if (prev == resuming) {
            Slog.wtf(ActivityManagerService.TAG, "Trying to pause activity that is in process of being resumed");
            return false;
        } else {
            String str2;
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Moving to PAUSING: ");
                stringBuilder2.append(prev);
                stringBuilder2.append(" in stack ");
                stringBuilder2.append(this.mStackId);
                Slog.v(str2, stringBuilder2.toString(), new Exception());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving to PAUSING: ");
                stringBuilder.append(prev);
                stringBuilder.append(" in stack ");
                stringBuilder.append(this.mStackId);
                Flog.i(101, stringBuilder.toString());
            }
            this.mPausingActivity = prev;
            this.mLastPausedActivity = prev;
            ActivityRecord activityRecord = ((prev.intent.getFlags() & 1073741824) == 0 && (prev.info.flags & 128) == 0) ? null : prev;
            this.mLastNoHistoryActivity = activityRecord;
            prev.setState(ActivityState.PAUSING, "startPausingLocked");
            prev.getTask().touchActiveTime();
            clearLaunchTime(prev);
            this.mStackSupervisor.getLaunchTimeTracker().stopFullyDrawnTraceIfNeeded(getWindowingMode());
            this.mService.updateCpuStats();
            if (prev.app == null || prev.app.thread == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Clear pausing activity ");
                stringBuilder.append(this.mPausingActivity);
                stringBuilder.append(" in stack ");
                stringBuilder.append(this.mStackId);
                stringBuilder.append(" for tha app is not ready");
                Flog.i(101, stringBuilder.toString());
                this.mPausingActivity = null;
                this.mLastPausedActivity = null;
                this.mLastNoHistoryActivity = null;
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Enqueueing pending pause: ");
                    stringBuilder3.append(prev);
                    Slog.v(str2, stringBuilder3.toString());
                }
                try {
                    int i = prev.userId;
                    int identityHashCode = System.identityHashCode(prev);
                    String str3 = prev.shortComponentName;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("userLeaving=");
                    stringBuilder4.append(userLeaving);
                    EventLogTags.writeAmPauseActivity(i, identityHashCode, str3, stringBuilder4.toString());
                    this.mService.updateUsageStats(prev, false);
                    if (Jlog.isPerfTest()) {
                        Jlog.i(3024, Jlog.getMessage("ActivityStack", "startPausingLocked", Intent.toPkgClsString(prev.realActivity, "who")));
                    }
                    this.mService.getLifecycleManager().scheduleTransaction(prev.app.thread, prev.appToken, PauseActivityItem.obtain(prev.finishing, userLeaving, prev.configChangeFlags, pauseImmediately));
                } catch (Exception e) {
                    Slog.w(ActivityManagerService.TAG, "Exception thrown during pause", e);
                    this.mPausingActivity = null;
                    this.mLastPausedActivity = null;
                    this.mLastNoHistoryActivity = null;
                }
            }
            if (!(uiSleeping || this.mService.isSleepingOrShuttingDownLocked())) {
                this.mStackSupervisor.acquireLaunchWakelock();
            }
            if (this.mPausingActivity != null) {
                if (!uiSleeping) {
                    prev.pauseKeyDispatchingLocked();
                } else if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(ActivityManagerService.TAG, "Key dispatch not paused for screen off");
                }
                if (pauseImmediately) {
                    completePauseLocked(false, resuming);
                    return false;
                }
                schedulePauseTimeout(prev);
                return true;
            }
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(ActivityManagerService.TAG, "Activity not running, resuming next.");
            }
            if (resuming == null) {
                this.mStackSupervisor.mActivityLaunchTrack = " activityNotRunning";
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
            return false;
        }
    }

    final void activityPausedLocked(IBinder token, boolean timeout) {
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Activity paused: token=");
            stringBuilder.append(token);
            stringBuilder.append(", timeout=");
            stringBuilder.append(timeout);
            Slog.v(str, stringBuilder.toString());
        }
        ActivityRecord r = isInStackLocked(token);
        if (r != null) {
            if (Jlog.isPerfTest()) {
                Jlog.i(3029, Jlog.getMessage("ActivityStack", "activityPausedLocked", Intent.toPkgClsString(r.realActivity, "who")));
            }
            this.mHandler.removeMessages(101, r);
            if (this.mPausingActivity == r) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Moving to PAUSED: ");
                stringBuilder2.append(r);
                stringBuilder2.append(timeout ? " (due to timeout)" : " (pause complete)");
                stringBuilder2.append(" in stack ");
                stringBuilder2.append(this.mStackId);
                Flog.i(101, stringBuilder2.toString());
                this.mService.mWindowManager.deferSurfaceLayout();
                try {
                    completePauseLocked(true, null);
                    return;
                } finally {
                    this.mService.mWindowManager.mAppTransitTrack = "activitypause";
                    this.mService.mWindowManager.continueSurfaceLayout();
                }
            } else {
                Object[] objArr = new Object[4];
                objArr[0] = Integer.valueOf(r.userId);
                objArr[1] = Integer.valueOf(System.identityHashCode(r));
                objArr[2] = r.shortComponentName;
                objArr[3] = this.mPausingActivity != null ? this.mPausingActivity.shortComponentName : "(none)";
                EventLog.writeEvent(EventLogTags.AM_FAILED_TO_PAUSE, objArr);
                if (r.isState(ActivityState.PAUSING)) {
                    r.setState(ActivityState.PAUSED, "activityPausedLocked");
                    StringBuilder stringBuilder3;
                    if (r.finishing) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Executing finish of failed to pause activity: ");
                        stringBuilder3.append(r);
                        Flog.i(101, stringBuilder3.toString());
                        finishCurrentActivityLocked(r, 2, false, "activityPausedLocked");
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Not process of failed to pause activity: ");
                        stringBuilder3.append(r);
                        Flog.i(101, stringBuilder3.toString());
                    }
                }
            }
        } else {
            ActivityRecord record = ActivityRecord.forTokenLocked(token);
            if (record != null) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("FAILED to find record ");
                stringBuilder4.append(record);
                stringBuilder4.append(" in stack ");
                stringBuilder4.append(this.mStackId);
                stringBuilder4.append(" while pausing ");
                stringBuilder4.append(this.mPausingActivity);
                Flog.i(101, stringBuilder4.toString());
            }
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private void completePauseLocked(boolean resumeNext, ActivityRecord resuming) {
        String str;
        StringBuilder stringBuilder;
        ActivityRecord prev = this.mPausingActivity;
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Complete pause: ");
            stringBuilder.append(prev);
            Slog.v(str, stringBuilder.toString());
        }
        if (prev != null) {
            HwAudioServiceManager.setSoundEffectState(false, prev.packageName, false, null);
        }
        if (prev != null) {
            prev.setWillCloseOrEnterPip(false);
            boolean wasStopping = prev.isState(ActivityState.STOPPING);
            prev.setState(ActivityState.PAUSED, "completePausedLocked");
            String str2;
            StringBuilder stringBuilder2;
            if (prev.finishing) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Executing finish of activity: ");
                    stringBuilder2.append(prev);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (prev.info != null && prev.frontOfTask) {
                    this.mService.getRecordCust().appExitRecord(prev.info.packageName, "finish");
                }
                prev = finishCurrentActivityLocked(prev, 2, false, "completedPausedLocked");
            } else if (prev.app != null) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Enqueue pending stop if needed: ");
                    stringBuilder2.append(prev);
                    stringBuilder2.append(" wasStopping=");
                    stringBuilder2.append(wasStopping);
                    stringBuilder2.append(" visible=");
                    stringBuilder2.append(prev.visible);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(prev) && (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE)) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Complete pause, no longer waiting: ");
                    stringBuilder2.append(prev);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (prev.deferRelaunchUntilPaused) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Re-launching after pause: ");
                    stringBuilder2.append(prev);
                    Slog.v(str2, stringBuilder2.toString());
                    prev.relaunchActivityLocked(false, prev.preserveWindowOnDeferredRelaunch);
                } else if (wasStopping) {
                    prev.setState(ActivityState.STOPPING, "completePausedLocked");
                } else if (!prev.visible || shouldSleepOrShutDownActivities()) {
                    prev.setDeferHidingClient(false);
                    addToStopping(prev, true, false);
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("App died during pause, not stopping: ");
                    stringBuilder2.append(prev);
                    Slog.v(str2, stringBuilder2.toString());
                }
                prev = null;
            }
            if (prev != null) {
                prev.stopFreezingScreenLocked(true);
            }
            this.mPausingActivity = null;
        }
        if (resumeNext) {
            ActivityStack topStack = this.mStackSupervisor.getFocusedStack();
            if (topStack.shouldSleepOrShutDownActivities()) {
                checkReadyForSleep();
                ActivityRecord top = topStack.topRunningActivityLocked();
                if (top == null || !(prev == null || top == prev)) {
                    this.mStackSupervisor.mActivityLaunchTrack = "sleepingNoMoreActivityRun";
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            } else {
                this.mStackSupervisor.mActivityLaunchTrack = "activityPaused";
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked(topStack, prev, null);
            }
        }
        if (prev != null) {
            prev.resumeKeyDispatchingLocked();
            if (prev.app != null && prev.cpuTimeAtResume > 0 && this.mService.mBatteryStatsService.isOnBattery()) {
                long diff = this.mService.mProcessCpuTracker.getCpuTimeForPid(prev.app.pid) - prev.cpuTimeAtResume;
                if (diff > 0) {
                    BatteryStatsImpl bsi = this.mService.mBatteryStatsService.getActiveStatistics();
                    synchronized (bsi) {
                        Proc ps = bsi.getProcessStatsLocked(prev.info.applicationInfo.uid, prev.info.packageName);
                        if (ps != null) {
                            ps.addForegroundTimeLocked(diff);
                        }
                    }
                }
            }
            prev.cpuTimeAtResume = 0;
        }
        if (this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause || (getDisplay() != null && getDisplay().hasPinnedStack())) {
            this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
            this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = false;
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(resuming, 0, false);
        if (getDisplay() == null) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getDisplay() == null, DisplayId: ");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append("  StackId: ");
            stringBuilder.append(this.mStackId);
            Slog.i(str, stringBuilder.toString());
        }
    }

    void addToStopping(ActivityRecord r, boolean scheduleIdle, boolean idleDelayed) {
        if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
            this.mStackSupervisor.mStoppingActivities.add(r);
        }
        boolean z = true;
        if (this.mStackSupervisor.mStoppingActivities.size() <= 3 && (!r.frontOfTask || this.mTaskHistory.size() > 1)) {
            z = false;
        }
        boolean forceIdle = z;
        if (scheduleIdle || forceIdle) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Scheduling idle now: forceIdle=");
                stringBuilder.append(forceIdle);
                stringBuilder.append("immediate=");
                stringBuilder.append(idleDelayed ^ 1);
                Slog.v(str, stringBuilder.toString());
            }
            if (idleDelayed) {
                this.mStackSupervisor.scheduleIdleTimeoutLocked(r);
                return;
            } else {
                this.mStackSupervisor.scheduleIdleLocked();
                return;
            }
        }
        checkReadyForSleep();
    }

    @VisibleForTesting
    boolean isStackTranslucent(ActivityRecord starting) {
        if (!isAttached() || this.mForceHidden) {
            return true;
        }
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "It is in finishing activity now");
                    }
                } else if (r.visibleIgnoringKeyguard || r == starting) {
                    if (r.fullscreen || r.hasWallpaper) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG_VISIBILITY, "Stack has at least one fullscreen activity -> untranslucent");
                        }
                        return false;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG_VISIBILITY, "It is not the currently starting activity");
                }
            }
        }
        return true;
    }

    boolean isTopStackOnDisplay() {
        if (getDisplay() == null) {
            return false;
        }
        return getDisplay().isTopStack(this);
    }

    boolean isTopActivityVisible() {
        ActivityRecord topActivity = getTopActivity();
        return topActivity != null && topActivity.visible;
    }

    protected boolean shouldBeVisible(ActivityRecord starting) {
        ActivityRecord activityRecord = starting;
        if (!isAttached() || this.mForceHidden) {
            return false;
        }
        if (this.mStackSupervisor.isFocusedStack(this)) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "It is the focusedStack -> visible");
            }
            return true;
        } else if (topRunningActivityLocked() == null && isInStackLocked(starting) == null && !isTopStackOnDisplay()) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "No running activities -> invisible");
            }
            return false;
        } else {
            ActivityDisplay display = getDisplay();
            boolean gotSplitScreenStack = false;
            boolean gotOpaqueSplitScreenPrimary = false;
            boolean gotOpaqueSplitScreenSecondary = false;
            int windowingMode = getWindowingMode();
            boolean isAssistantType = isActivityTypeAssistant();
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG_VISIBILITY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Current stack windowingMode:");
                stringBuilder.append(windowingMode);
                stringBuilder.append(" activityType:");
                stringBuilder.append(getActivityType());
                Slog.v(str, stringBuilder.toString());
            }
            for (int i = display.getChildCount() - 1; i >= 0; i--) {
                ActivityStack other = display.getChildAt(i);
                if (other == this) {
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "No other stack occluding -> visible");
                    }
                    return true;
                }
                int otherWindowingMode = other.getWindowingMode();
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    String str2 = TAG_VISIBILITY;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Other stack:");
                    stringBuilder2.append(other.toShortString());
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (otherWindowingMode == 1) {
                    int activityType = other.getActivityType();
                    if (windowingMode == 3 && (activityType == 2 || (activityType == 4 && this.mWindowManager.getRecentsAnimationController() != null))) {
                        return true;
                    }
                    if (!other.isStackTranslucent(activityRecord)) {
                        return false;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "It is behind a translucent fullscreen stack");
                    }
                } else {
                    if (otherWindowingMode == 3 && !gotOpaqueSplitScreenPrimary) {
                        gotSplitScreenStack = true;
                        gotOpaqueSplitScreenPrimary = other.isStackTranslucent(activityRecord) ^ 1;
                        if (windowingMode == 1) {
                            ActivityStack currentFocusStack = this.mStackSupervisor.getFocusedStack();
                            if (currentFocusStack != null && currentFocusStack.inSplitScreenWindowingMode()) {
                                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                    Slog.v(TAG_VISIBILITY, "current is in split or exiting split mode,not show fullscreen stack");
                                }
                                return false;
                            }
                        }
                        if (windowingMode == 3 && gotOpaqueSplitScreenPrimary) {
                            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.v(TAG_VISIBILITY, "It is behind another opaque stack in ssp mode -> invisible");
                            }
                            return false;
                        }
                    } else if (otherWindowingMode == 4 && !gotOpaqueSplitScreenSecondary) {
                        gotSplitScreenStack = true;
                        gotOpaqueSplitScreenSecondary = other.isStackTranslucent(activityRecord) ^ 1;
                        if (windowingMode == 4 && gotOpaqueSplitScreenSecondary) {
                            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.v(TAG_VISIBILITY, "It is behind another opaque stack in sss mode -> invisible");
                            }
                            return false;
                        }
                    }
                    if (gotOpaqueSplitScreenPrimary && gotOpaqueSplitScreenSecondary) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG_VISIBILITY, "It is in ssw mode -> invisible");
                        }
                        return false;
                    } else if (isAssistantType && gotSplitScreenStack) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG_VISIBILITY, "Assistant stack can't be visible behind split-screen -> invisible");
                        }
                        return false;
                    }
                }
            }
            return true;
        }
    }

    final int rankTaskLayers(int baseLayer) {
        int layer = 0;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx);
            ActivityRecord r = task.topRunningActivityLocked();
            if (r == null || r.finishing || !r.visible) {
                task.mLayerRank = -1;
            } else {
                int layer2 = layer + 1;
                task.mLayerRank = layer + baseLayer;
                layer = layer2;
            }
        }
        return layer;
    }

    final void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        ensureActivitiesVisibleLocked(starting, configChanges, preserveWindows, true);
    }

    /* JADX WARNING: Missing block: B:127:0x02df, code:
            if (r7.mTranslucentActivityWaiting == null) goto L_0x02ed;
     */
    /* JADX WARNING: Missing block: B:129:0x02e7, code:
            if (r7.mUndrawnActivitiesBelowTopTranslucent.isEmpty() == false) goto L_0x02ed;
     */
    /* JADX WARNING: Missing block: B:130:0x02e9, code:
            notifyActivityDrawnLocked(null);
     */
    /* JADX WARNING: Missing block: B:131:0x02ed, code:
            r7.mStackSupervisor.getKeyguardController().endActivityVisibilityUpdate();
     */
    /* JADX WARNING: Missing block: B:132:0x02f7, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        Throwable th;
        ActivityRecord activityRecord = starting;
        boolean task = notifyClients;
        boolean activities = false;
        this.mTopActivityOccludesKeyguard = false;
        this.mTopDismissingKeyguardActivity = null;
        this.mStackSupervisor.getKeyguardController().beginActivityVisibilityUpdate();
        int configChanges2;
        try {
            ActivityRecord top = topRunningActivityLocked();
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG_VISIBILITY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ensureActivitiesVisible behind ");
                stringBuilder.append(top);
                stringBuilder.append(" configChanges=0x");
                stringBuilder.append(Integer.toHexString(configChanges));
                Slog.v(str, stringBuilder.toString());
            }
            if (top != null) {
                checkTranslucentActivityWaiting(top);
            }
            boolean z = true;
            boolean aboveTop = top != null;
            boolean stackShouldBeVisible = shouldBeVisible(starting);
            boolean behindFullscreenActivity = !stackShouldBeVisible;
            boolean resumeNextActivity = this.mStackSupervisor.isFocusedStack(this) && isInStackLocked(starting) == null;
            boolean z2 = isAttached() && getDisplay().isTopNotPinnedStack(this);
            boolean isTopNotPinnedStack = z2;
            int taskNdx = this.mTaskHistory.size() - 1;
            int configChanges3 = configChanges;
            while (true) {
                int taskNdx2 = taskNdx;
                if (taskNdx2 < 0) {
                    break;
                }
                try {
                    ArrayList<ActivityRecord> reallyVisible;
                    TaskRecord task2;
                    boolean z3;
                    TaskRecord task3 = (TaskRecord) this.mTaskHistory.get(taskNdx2);
                    ArrayList<ActivityRecord> activities2 = task3.mActivities;
                    int activityNdx = activities2.size() - 1;
                    boolean resumeNextActivity2 = resumeNextActivity;
                    configChanges2 = configChanges3;
                    while (true) {
                        configChanges3 = activityNdx;
                        if (configChanges3 < 0) {
                            break;
                        }
                        try {
                            ActivityRecord r = (ActivityRecord) activities2.get(configChanges3);
                            if (!r.finishing) {
                                boolean isTop = r == top ? z : false;
                                if (!aboveTop || isTop) {
                                    aboveTop = r.shouldBeVisibleIgnoringKeyguard(behindFullscreenActivity);
                                    r.visibleIgnoringKeyguard = aboveTop;
                                    activities = (isTop && isTopNotPinnedStack) ? z : false;
                                    activities = checkKeyguardVisibility(r, aboveTop, activities);
                                    if (aboveTop) {
                                        if (stackShouldBeVisible) {
                                            z = false;
                                        }
                                        z = updateBehindFullscreen(z, behindFullscreenActivity, r);
                                    } else {
                                        z = behindFullscreenActivity;
                                    }
                                    String str2;
                                    boolean z4;
                                    int activityNdx2;
                                    if (activities) {
                                        boolean visibleIgnoringKeyguard;
                                        ActivityRecord r2;
                                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                            str2 = TAG_VISIBILITY;
                                            activities = new StringBuilder();
                                            visibleIgnoringKeyguard = aboveTop;
                                            activities.append("Make visible? ");
                                            activities.append(r);
                                            activities.append(" finishing=");
                                            activities.append(r.finishing);
                                            activities.append(" state=");
                                            activities.append(r.getState());
                                            Slog.v(str2, activities.toString());
                                        } else {
                                            visibleIgnoringKeyguard = aboveTop;
                                        }
                                        if (r == activityRecord || !task) {
                                            behindFullscreenActivity = preserveWindows;
                                        } else {
                                            r.ensureActivityConfiguration(0, preserveWindows, true);
                                        }
                                        if (r.app && r.app.thread) {
                                            if (r.visible) {
                                                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                                    activities = TAG_VISIBILITY;
                                                    StringBuilder stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Skipping: already visible at ");
                                                    stringBuilder2.append(r);
                                                    Slog.v(activities, stringBuilder2.toString());
                                                }
                                                if (r.mClientVisibilityDeferred && task) {
                                                    r.makeClientVisible();
                                                }
                                                if (r.handleAlreadyVisible()) {
                                                    resumeNextActivity2 = false;
                                                    r2 = r;
                                                    reallyVisible = activities2;
                                                    task2 = task3;
                                                    z4 = visibleIgnoringKeyguard;
                                                    z3 = false;
                                                    configChanges2 |= r2.configChangeFlags;
                                                }
                                            } else {
                                                if (!this.mService.getActivityStartController().mCurActivityPkName.equals(r.appInfo.packageName)) {
                                                    LogPower.push(true, "visible", r.appInfo.packageName);
                                                }
                                                r.makeVisibleIfNeeded(activityRecord, task);
                                            }
                                            r2 = r;
                                            reallyVisible = activities2;
                                            activityNdx2 = configChanges3;
                                            task2 = task3;
                                            z4 = visibleIgnoringKeyguard;
                                            z3 = false;
                                        } else {
                                            z3 = false;
                                            r2 = r;
                                            reallyVisible = activities2;
                                            activityNdx2 = configChanges3;
                                            task2 = task3;
                                            if (makeVisibleAndRestartIfNeeded(activityRecord, configChanges2, isTop, resumeNextActivity2, r2)) {
                                                if (activityNdx2 >= reallyVisible.size()) {
                                                    configChanges3 = reallyVisible.size() - 1;
                                                    configChanges2 |= r2.configChangeFlags;
                                                } else {
                                                    resumeNextActivity2 = false;
                                                }
                                            }
                                        }
                                        configChanges3 = activityNdx2;
                                        configChanges2 |= r2.configChangeFlags;
                                    } else {
                                        z4 = aboveTop;
                                        aboveTop = r;
                                        reallyVisible = activities2;
                                        activityNdx2 = configChanges3;
                                        task2 = task3;
                                        z3 = false;
                                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY || aboveTop.isState(ActivityState.RESUMED)) {
                                            str2 = TAG_VISIBILITY;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Make invisible? ");
                                            stringBuilder3.append(aboveTop);
                                            stringBuilder3.append(" finishing=");
                                            stringBuilder3.append(aboveTop.finishing);
                                            stringBuilder3.append(" state=");
                                            stringBuilder3.append(aboveTop.getState());
                                            stringBuilder3.append(" stackShouldBeVisible=");
                                            stringBuilder3.append(stackShouldBeVisible);
                                            stringBuilder3.append(" behindFullscreenActivity=");
                                            stringBuilder3.append(z);
                                            stringBuilder3.append(" mLaunchTaskBehind=");
                                            stringBuilder3.append(aboveTop.mLaunchTaskBehind);
                                            stringBuilder3.append(" keyguardShowing = ");
                                            stringBuilder3.append(this.mStackSupervisor.getKeyguardController().isKeyguardShowing(this.mDisplayId != -1 ? this.mDisplayId : 0));
                                            stringBuilder3.append(" keyguardLocked = ");
                                            stringBuilder3.append(this.mStackSupervisor.getKeyguardController().isKeyguardLocked());
                                            stringBuilder3.append(" r.visibleIgnoringKeyguard = ");
                                            stringBuilder3.append(aboveTop.visibleIgnoringKeyguard);
                                            Slog.v(str2, stringBuilder3.toString());
                                        }
                                        makeInvisible(aboveTop);
                                        configChanges3 = activityNdx2;
                                    }
                                    behindFullscreenActivity = z;
                                    aboveTop = false;
                                    activityNdx = configChanges3 - 1;
                                    activities2 = reallyVisible;
                                    task3 = task2;
                                    activities = z3;
                                    activityRecord = starting;
                                    task = notifyClients;
                                    z = true;
                                }
                            }
                            reallyVisible = activities2;
                            task2 = task3;
                            z3 = false;
                            activityNdx = configChanges3 - 1;
                            activities2 = reallyVisible;
                            task3 = task2;
                            activities = z3;
                            activityRecord = starting;
                            task = notifyClients;
                            z = true;
                        } catch (Throwable th2) {
                            th = th2;
                            this.mStackSupervisor.getKeyguardController().endActivityVisibilityUpdate();
                            throw th;
                        }
                    }
                    z3 = activities;
                    reallyVisible = activities2;
                    task2 = task3;
                    if (getWindowingMode() == 5) {
                        behindFullscreenActivity = !stackShouldBeVisible ? true : z3;
                    } else if (isActivityTypeHome()) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            String str3 = TAG_VISIBILITY;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Home task: at ");
                            stringBuilder4.append(task2);
                            stringBuilder4.append(" stackShouldBeVisible=");
                            stringBuilder4.append(stackShouldBeVisible);
                            stringBuilder4.append(" behindFullscreenActivity=");
                            stringBuilder4.append(behindFullscreenActivity);
                            Slog.v(str3, stringBuilder4.toString());
                        }
                        if (task2.getTopActivity() != null) {
                            behindFullscreenActivity = true;
                        }
                    }
                    taskNdx = taskNdx2 - 1;
                    resumeNextActivity = resumeNextActivity2;
                    configChanges3 = configChanges2;
                    activities = z3;
                    activityRecord = starting;
                    task = notifyClients;
                    z = true;
                } catch (Throwable th3) {
                    th = th3;
                    configChanges2 = configChanges3;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            configChanges2 = configChanges;
            this.mStackSupervisor.getKeyguardController().endActivityVisibilityUpdate();
            throw th;
        }
    }

    void addStartingWindowsForVisibleActivities(boolean taskSwitch) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ((TaskRecord) this.mTaskHistory.get(taskNdx)).addStartingWindowsForVisibleActivities(taskSwitch);
        }
    }

    boolean topActivityOccludesKeyguard() {
        return this.mTopActivityOccludesKeyguard;
    }

    boolean resizeStackWithLaunchBounds() {
        return inPinnedWindowingMode();
    }

    public boolean supportsSplitScreenWindowingMode() {
        TaskRecord topTask = topTask();
        return super.supportsSplitScreenWindowingMode() && (topTask == null || topTask.supportsSplitScreenWindowingMode());
    }

    boolean affectedBySplitScreenResize() {
        boolean z = false;
        if (!supportsSplitScreenWindowingMode()) {
            return false;
        }
        int windowingMode = getWindowingMode();
        if (!(windowingMode == 5 || windowingMode == 2)) {
            z = true;
        }
        return z;
    }

    ActivityRecord getTopDismissingKeyguardActivity() {
        return this.mTopDismissingKeyguardActivity;
    }

    boolean checkKeyguardVisibility(ActivityRecord r, boolean shouldBeVisible, boolean isTop) {
        if (!shouldBeVisible) {
            return shouldBeVisible;
        }
        boolean z = false;
        boolean keyguardOrAodShowing = this.mStackSupervisor.getKeyguardController().isKeyguardOrAodShowing(this.mDisplayId != -1 ? this.mDisplayId : 0);
        boolean keyguardLocked = this.mStackSupervisor.getKeyguardController().isKeyguardLocked();
        boolean showWhenLocked = r.canShowWhenLocked();
        boolean dismissKeyguard = r.hasDismissKeyguardWindows();
        if (keyguardLocked) {
            if (!this.mService.mHwAMSEx.isAllowToStartActivity(this.mService.mContext, this.mService.mContext.getPackageName(), r.info, keyguardLocked, ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getLastResumedActivity())) {
                showWhenLocked = false;
                dismissKeyguard = false;
            }
        }
        if (shouldBeVisible) {
            if (dismissKeyguard && this.mTopDismissingKeyguardActivity == null) {
                this.mTopDismissingKeyguardActivity = r;
            }
            if (isTop) {
                this.mTopActivityOccludesKeyguard |= showWhenLocked;
            }
            boolean canShowWithKeyguard = canShowWithInsecureKeyguard() && this.mStackSupervisor.getKeyguardController().canDismissKeyguard();
            if (canShowWithKeyguard) {
                return true;
            }
        }
        if (keyguardOrAodShowing) {
            if (shouldBeVisible && this.mStackSupervisor.getKeyguardController().canShowActivityWhileKeyguardShowing(r, dismissKeyguard)) {
                z = true;
            }
            return z;
        } else if (!keyguardLocked) {
            return shouldBeVisible;
        } else {
            if (shouldBeVisible && this.mStackSupervisor.getKeyguardController().canShowWhileOccluded(dismissKeyguard, showWhenLocked)) {
                z = true;
            }
            return z;
        }
    }

    private boolean canShowWithInsecureKeyguard() {
        ActivityDisplay activityDisplay = getDisplay();
        if (activityDisplay != null) {
            return (activityDisplay.mDisplay.getFlags() & 32) != 0;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack is not attached to any display, stackId=");
            stringBuilder.append(this.mStackId);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private void checkTranslucentActivityWaiting(ActivityRecord top) {
        if (this.mTranslucentActivityWaiting != top) {
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            if (this.mTranslucentActivityWaiting != null) {
                notifyActivityDrawnLocked(null);
                this.mTranslucentActivityWaiting = null;
            }
            this.mHandler.removeMessages(106);
        }
    }

    private boolean makeVisibleAndRestartIfNeeded(ActivityRecord starting, int configChanges, boolean isTop, boolean andResume, ActivityRecord r) {
        if (isTop || !r.visible) {
            String str;
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                str = TAG_VISIBILITY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Start and freeze screen for ");
                stringBuilder.append(r);
                Slog.v(str, stringBuilder.toString());
            }
            if (r != starting) {
                r.startFreezingScreenLocked(r.app, configChanges);
            }
            if (!r.visible || r.mLaunchTaskBehind) {
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    str = TAG_VISIBILITY;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Starting and making visible: ");
                    stringBuilder2.append(r);
                    Slog.v(str, stringBuilder2.toString());
                }
                r.setVisible(true);
            }
            if (r != starting) {
                this.mStackSupervisor.startSpecificActivityLocked(r, andResume, false);
                return true;
            }
        }
        return false;
    }

    private void makeInvisible(ActivityRecord r) {
        StringBuilder stringBuilder;
        if (r.visible) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Making invisible: ");
            stringBuilder.append(r);
            stringBuilder.append(" ");
            stringBuilder.append(r.getState());
            Flog.i(106, stringBuilder.toString());
            String str;
            try {
                boolean canEnterPictureInPicture = r.checkEnterPictureInPictureState("makeInvisible", true);
                boolean deferHidingClient = canEnterPictureInPicture && !r.isState(ActivityState.STOPPING, ActivityState.STOPPED, ActivityState.PAUSED);
                r.setDeferHidingClient(deferHidingClient);
                r.setVisible(false);
                switch (r.getState()) {
                    case STOPPING:
                    case STOPPED:
                        if (!(r.app == null || r.app.thread == null)) {
                            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                str = TAG_VISIBILITY;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Scheduling invisibility: ");
                                stringBuilder2.append(r);
                                Slog.v(str, stringBuilder2.toString());
                            }
                            this.mService.getLifecycleManager().scheduleTransaction(r.app.thread, r.appToken, WindowVisibilityItem.obtain(false));
                        }
                        r.supportsEnterPipOnTaskSwitch = false;
                        break;
                    case INITIALIZING:
                    case RESUMED:
                    case PAUSING:
                    case PAUSED:
                        addToStopping(r, true, canEnterPictureInPicture);
                        break;
                }
            } catch (Exception e) {
                str = ActivityManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Exception thrown making hidden: ");
                stringBuilder3.append(r.intent.getComponent());
                Slog.w(str, stringBuilder3.toString(), e);
            }
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            String str2 = TAG_VISIBILITY;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Already invisible: ");
            stringBuilder.append(r);
            Slog.v(str2, stringBuilder.toString());
        }
    }

    private boolean updateBehindFullscreen(boolean stackInvisible, boolean behindFullscreenActivity, ActivityRecord r) {
        if (!r.fullscreen) {
            return behindFullscreenActivity;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            String str = TAG_VISIBILITY;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fullscreen: at ");
            stringBuilder.append(r);
            stringBuilder.append(" stackInvisible=");
            stringBuilder.append(stackInvisible);
            stringBuilder.append(" behindFullscreenActivity=");
            stringBuilder.append(behindFullscreenActivity);
            Slog.v(str, stringBuilder.toString());
        }
        return true;
    }

    void convertActivityToTranslucent(ActivityRecord r) {
        this.mTranslucentActivityWaiting = r;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.sendEmptyMessageDelayed(106, TRANSLUCENT_CONVERSION_TIMEOUT);
    }

    void clearOtherAppTimeTrackers(AppTimeTracker except) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.appTimeTracker != except) {
                    r.appTimeTracker = null;
                }
            }
        }
    }

    void notifyActivityDrawnLocked(ActivityRecord r) {
        if (r == null || (this.mUndrawnActivitiesBelowTopTranslucent.remove(r) && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty())) {
            ActivityRecord waitingActivity = this.mTranslucentActivityWaiting;
            this.mTranslucentActivityWaiting = null;
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            this.mHandler.removeMessages(106);
            if (waitingActivity != null) {
                boolean z = false;
                this.mWindowManager.setWindowOpaque(waitingActivity.appToken, false);
                if (waitingActivity.app != null && waitingActivity.app.thread != null) {
                    try {
                        IApplicationThread iApplicationThread = waitingActivity.app.thread;
                        IBinder iBinder = waitingActivity.appToken;
                        if (r != null) {
                            z = true;
                        }
                        iApplicationThread.scheduleTranslucentConversionComplete(iBinder, z);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    void cancelInitializingActivities() {
        ActivityRecord topActivity = topRunningActivityLocked();
        boolean aboveTop = true;
        boolean behindFullscreenActivity = false;
        if (!shouldBeVisible(null)) {
            aboveTop = false;
            behindFullscreenActivity = true;
        }
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                int i;
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (aboveTop) {
                    if (r == topActivity) {
                        aboveTop = false;
                    }
                    i = r.fullscreen;
                } else {
                    r.removeOrphanedStartingWindow(behindFullscreenActivity);
                    i = r.fullscreen;
                }
                behindFullscreenActivity |= i;
            }
        }
    }

    @GuardedBy("mService")
    boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
        if (this.mStackSupervisor.inResumeTopActivity) {
            Flog.i(101, "It is now in resume top activity");
            return false;
        }
        boolean result = false;
        try {
            this.mStackSupervisor.inResumeTopActivity = true;
            result = resumeTopActivityInnerLocked(prev, options);
            ActivityRecord next = topRunningActivityLocked(true);
            if (next == null || !next.canTurnScreenOn()) {
                checkReadyForSleep();
            }
            this.mStackSupervisor.inResumeTopActivity = false;
            return result;
        } catch (Throwable th) {
            this.mStackSupervisor.inResumeTopActivity = false;
        }
    }

    protected ActivityRecord getResumedActivity() {
        return this.mResumedActivity;
    }

    private void setResumedActivity(ActivityRecord r, String reason) {
        if (this.mResumedActivity != r) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setResumedActivity stack:");
                stringBuilder.append(this);
                stringBuilder.append(" + from: ");
                stringBuilder.append(this.mResumedActivity);
                stringBuilder.append(" to:");
                stringBuilder.append(r);
                stringBuilder.append(" reason:");
                stringBuilder.append(reason);
                Slog.d(str, stringBuilder.toString());
            }
            this.mResumedActivity = r;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:290:0x060c A:{SYNTHETIC, Splitter: B:290:0x060c} */
    /* JADX WARNING: Removed duplicated region for block: B:299:0x067f  */
    /* JADX WARNING: Removed duplicated region for block: B:305:0x06ab A:{SYNTHETIC, Splitter: B:305:0x06ab} */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x0795 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:345:0x07bb A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:349:0x07e1 A:{SKIP, Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:348:0x07dd A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:355:0x07f9 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x0795 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:345:0x07bb A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:348:0x07dd A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:349:0x07e1 A:{SKIP, Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:355:0x07f9 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:342:0x0795 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:345:0x07bb A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:349:0x07e1 A:{SKIP, Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:348:0x07dd A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:355:0x07f9 A:{Catch:{ all -> 0x0801, all -> 0x0805 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x05fa A:{Splitter: B:265:0x05b9, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:25:0x004f, code:
            if (com.android.server.am.ActivityManagerService.isInCallActivity(r12) != false) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:218:0x04b8, code:
            if (r21 == false) goto L_0x04c3;
     */
    /* JADX WARNING: Missing block: B:261:0x05aa, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:282:0x05fa, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:283:0x05fb, code:
            r10 = r6;
     */
    /* JADX WARNING: Missing block: B:285:0x05ff, code:
            r22 = r4;
            r10 = r6;
     */
    /* JADX WARNING: Missing block: B:310:?, code:
            r0 = new java.lang.StringBuilder();
            r1 = r7.mStackSupervisor;
            r0.append(r1.mActivityLaunchTrack);
            r0.append(" resumeTopComplete");
            r1.mActivityLaunchTrack = r0.toString();
            r12.completeResumeLocked();
     */
    /* JADX WARNING: Missing block: B:312:0x06e3, code:
            if (android.util.Jlog.isUBMEnable() == false) goto L_0x074e;
     */
    /* JADX WARNING: Missing block: B:313:0x06e5, code:
            r1 = new java.lang.StringBuilder();
            r1.append("AS#");
            r1.append(r12.intent.getComponent().flattenToShortString());
            r1.append("(");
            r1.append(r12.app.pid);
            r1.append(",");
     */
    /* JADX WARNING: Missing block: B:314:0x070f, code:
            if (r8 == null) goto L_0x0721;
     */
    /* JADX WARNING: Missing block: B:316:0x0713, code:
            if (r8.intent != null) goto L_0x0716;
     */
    /* JADX WARNING: Missing block: B:317:0x0716, code:
            r2 = r8.intent.getComponent().flattenToShortString();
     */
    /* JADX WARNING: Missing block: B:318:0x0721, code:
            r2 = "null";
     */
    /* JADX WARNING: Missing block: B:319:0x0724, code:
            r1.append(r2);
            r1.append(",");
     */
    /* JADX WARNING: Missing block: B:320:0x072c, code:
            if (r8 == null) goto L_0x073c;
     */
    /* JADX WARNING: Missing block: B:322:0x0730, code:
            if (r8.app != null) goto L_0x0733;
     */
    /* JADX WARNING: Missing block: B:323:0x0733, code:
            r2 = java.lang.Integer.valueOf(r8.app.pid);
     */
    /* JADX WARNING: Missing block: B:324:0x073c, code:
            r2 = "unknow";
     */
    /* JADX WARNING: Missing block: B:325:0x073f, code:
            r1.append(r2);
            r1.append(")");
            android.util.Jlog.d(273, r1.toString());
     */
    /* JADX WARNING: Missing block: B:326:0x074e, code:
            r10 = r23;
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:327:0x0754, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:328:0x0755, code:
            r1 = com.android.server.am.ActivityManagerService.TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Exception thrown during resume of ");
            r2.append(r12);
            android.util.Slog.w(r1, r2.toString(), r0);
            r10 = r23;
            requestFinishActivityLocked(r12.appToken, 0, null, "resume-exception", true);
     */
    /* JADX WARNING: Missing block: B:329:0x077b, code:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_STACK != false) goto L_0x077d;
     */
    /* JADX WARNING: Missing block: B:330:0x077d, code:
            r7.mStackSupervisor.validateTopActivitiesLocked();
     */
    /* JADX WARNING: Missing block: B:332:0x0783, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mService")
    private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) {
        String str;
        Throwable th;
        ActivityStack activityStack;
        ActivityStack lastStack;
        StringBuilder stringBuilder;
        ActivityRecord activityRecord = prev;
        ActivityOptions activityOptions = options;
        if (this.mService.mBooting || this.mService.mBooted) {
            ActivityRecord next = topRunningActivityLocked(true);
            boolean hasRunningActivity = next != null;
            if (hasRunningActivity && !isAttached()) {
                return false;
            }
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && hasRunningActivity && next.task != null) {
                ActivityManagerService activityManagerService = this.mService;
                if (!ActivityManagerService.isTimerAlertActivity(next)) {
                    activityManagerService = this.mService;
                }
                next.task.activityResumedInTop();
            }
            this.mStackSupervisor.cancelInitializingActivities();
            boolean userLeaving = this.mStackSupervisor.mUserLeaving;
            this.mStackSupervisor.mUserLeaving = false;
            StringBuilder stringBuilder2;
            if (hasRunningActivity) {
                next.delayedResume = false;
                if (this.mResumedActivity == next && next.isState(ActivityState.RESUMED) && this.mStackSupervisor.allResumedActivitiesComplete()) {
                    executeAppTransition(activityOptions);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Top activity resumed ");
                    stringBuilder2.append(next);
                    Flog.i(101, stringBuilder2.toString());
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return false;
                } else if (shouldSleepOrShutDownActivities() && this.mLastPausedActivity == next && this.mStackSupervisor.allPausedActivitiesComplete()) {
                    executeAppTransition(activityOptions);
                    Flog.i(101, "Going to sleep and all paused");
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return false;
                } else if (this.mService.mUserController.hasStartedUserState(next.userId)) {
                    String str2;
                    StringBuilder stringBuilder3;
                    this.mStackSupervisor.mStoppingActivities.remove(next);
                    this.mStackSupervisor.mGoingToSleepActivities.remove(next);
                    next.sleeping = false;
                    this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(next);
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                        str2 = ActivityManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Resuming ");
                        stringBuilder3.append(next);
                        Slog.v(str2, stringBuilder3.toString(), new Exception());
                    }
                    if (this.mStackSupervisor.allPausedActivitiesComplete()) {
                        String str3;
                        ActivityRecord lastResumed;
                        boolean z;
                        this.mStackSupervisor.setLaunchSource(next.info.applicationInfo.uid);
                        boolean lastResumedCanPip = false;
                        ActivityRecord lastResumed2 = null;
                        ActivityStack lastFocusedStack = this.mStackSupervisor.getLastStack();
                        if (!(lastFocusedStack == null || lastFocusedStack == this)) {
                            lastResumed2 = lastFocusedStack.mResumedActivity;
                            if (userLeaving && inMultiWindowMode() && lastFocusedStack.shouldBeVisible(next)) {
                                if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                                    str3 = ActivityManagerService.TAG;
                                    lastResumed = new StringBuilder();
                                    lastResumed.append("Overriding userLeaving to false next=");
                                    lastResumed.append(next);
                                    lastResumed.append(" lastResumed=");
                                    lastResumed.append(lastResumed2);
                                    Slog.i(str3, lastResumed.toString());
                                }
                                userLeaving = false;
                            }
                            z = lastResumed2 != null && lastResumed2.checkEnterPictureInPictureState("resumeTopActivity", userLeaving);
                            lastResumedCanPip = z;
                        }
                        boolean userLeaving2 = userLeaving;
                        lastResumed = lastResumed2;
                        userLeaving = ((next.info.flags & 16384) == 0 || lastResumedCanPip) ? false : true;
                        boolean resumeWhilePausing = userLeaving;
                        userLeaving = this.mStackSupervisor.pauseBackStacks(userLeaving2, next, false);
                        if (this.mResumedActivity != null) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Start pausing ");
                            stringBuilder2.append(this.mResumedActivity);
                            stringBuilder2.append(" in stack ");
                            stringBuilder2.append(this.mStackId);
                            Flog.i(101, stringBuilder2.toString());
                            userLeaving |= startPausingLocked(userLeaving2, false, next, false);
                        }
                        String str4;
                        StringBuilder stringBuilder4;
                        if (userLeaving && !resumeWhilePausing) {
                            Flog.i(101, "Skip resume: need to wait pause finished");
                            if (!(next.app == null || next.app.thread == null)) {
                                this.mService.updateLruProcessLocked(next.app, true, null);
                            }
                            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                this.mStackSupervisor.validateTopActivitiesLocked();
                            }
                            if (lastResumed != null) {
                                lastResumed.setWillCloseOrEnterPip(true);
                            }
                            return true;
                        } else if (this.mResumedActivity == next && next.isState(ActivityState.RESUMED) && this.mStackSupervisor.allResumedActivitiesComplete()) {
                            executeAppTransition(activityOptions);
                            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                str4 = ActivityManagerService.TAG;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Top activity resumed (dontWaitForPause) ");
                                stringBuilder4.append(next);
                                Slog.d(str4, stringBuilder4.toString());
                            }
                            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                this.mStackSupervisor.validateTopActivitiesLocked();
                            }
                            return true;
                        } else {
                            ProcessRecord processRecord;
                            boolean z2;
                            if (!shouldSleepActivities() || this.mLastNoHistoryActivity == null || this.mLastNoHistoryActivity.finishing) {
                                processRecord = null;
                                ActivityRecord activityRecord2 = lastResumed;
                                boolean z3 = userLeaving2;
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                    str4 = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("no-history finish of ");
                                    stringBuilder4.append(this.mLastNoHistoryActivity);
                                    stringBuilder4.append(" on new resume");
                                    Slog.d(str4, stringBuilder4.toString());
                                }
                                processRecord = null;
                                requestFinishActivityLocked(this.mLastNoHistoryActivity.appToken, 0, null, "resume-no-history", false);
                                this.mLastNoHistoryActivity = processRecord;
                            }
                            if (!(activityRecord == null || activityRecord == next)) {
                                if (!this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord) && next != null && !next.nowVisible) {
                                    this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.add(activityRecord);
                                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                        str4 = ActivityManagerService.TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Resuming top, waiting visible to hide: ");
                                        stringBuilder4.append(activityRecord);
                                        Slog.v(str4, stringBuilder4.toString());
                                    }
                                } else if (activityRecord.finishing) {
                                    activityRecord.setVisibility(false);
                                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                        str4 = ActivityManagerService.TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Not waiting for visible to hide: ");
                                        stringBuilder4.append(activityRecord);
                                        stringBuilder4.append(", waitingVisible=");
                                        stringBuilder4.append(this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord));
                                        stringBuilder4.append(", nowVisible=");
                                        stringBuilder4.append(next.nowVisible);
                                        Slog.v(str4, stringBuilder4.toString());
                                    }
                                } else if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                    str4 = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Previous already visible but still waiting to hide: ");
                                    stringBuilder4.append(activityRecord);
                                    stringBuilder4.append(", waitingVisible=");
                                    stringBuilder4.append(this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord));
                                    stringBuilder4.append(", nowVisible=");
                                    stringBuilder4.append(next.nowVisible);
                                    Slog.v(str4, stringBuilder4.toString());
                                }
                            }
                            try {
                                AppGlobals.getPackageManager().setPackageStoppedState(next.packageName, false, next.userId);
                            } catch (RemoteException e) {
                            } catch (IllegalArgumentException e2) {
                                str = ActivityManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed trying to unstop package ");
                                stringBuilder2.append(next.packageName);
                                stringBuilder2.append(": ");
                                stringBuilder2.append(e2);
                                Slog.w(str, stringBuilder2.toString());
                            }
                            userLeaving = true;
                            int i = 6;
                            if (activityRecord == null) {
                                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                                    Slog.v(ActivityManagerService.TAG, "Prepare open transition: no previous");
                                }
                                if (this.mStackSupervisor.mNoAnimActivities.contains(next)) {
                                    userLeaving = false;
                                    this.mWindowManager.prepareAppTransition(0, false);
                                } else {
                                    this.mWindowManager.prepareAppTransition(6, false);
                                }
                            } else if (activityRecord.finishing) {
                                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                                    str = ActivityManagerService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Prepare close transition: prev=");
                                    stringBuilder2.append(activityRecord);
                                    Slog.v(str, stringBuilder2.toString());
                                }
                                if (this.mStackSupervisor.mNoAnimActivities.contains(activityRecord)) {
                                    userLeaving = false;
                                    this.mWindowManager.prepareAppTransition(0, false);
                                } else {
                                    int i2;
                                    WindowManagerService windowManagerService = this.mWindowManager;
                                    if (prev.getTask() == next.getTask()) {
                                        i2 = 7;
                                    } else {
                                        i2 = 9;
                                    }
                                    windowManagerService.prepareAppTransition(i2, false);
                                }
                                activityRecord.setVisibility(false);
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                                    str2 = ActivityManagerService.TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Prepare open transition: prev=");
                                    stringBuilder3.append(activityRecord);
                                    Slog.v(str2, stringBuilder3.toString());
                                }
                                if (this.mStackSupervisor.mNoAnimActivities.contains(next)) {
                                    userLeaving = false;
                                    this.mWindowManager.prepareAppTransition(0, false);
                                } else {
                                    WindowManagerService windowManagerService2 = this.mWindowManager;
                                    if (prev.getTask() != next.getTask()) {
                                        if (next.mLaunchTaskBehind) {
                                            i = 16;
                                        } else {
                                            i = 8;
                                        }
                                    }
                                    windowManagerService2.prepareAppTransition(i, false);
                                }
                            }
                            if (userLeaving) {
                                next.applyOptionsLocked();
                            } else {
                                next.clearOptionsLocked();
                            }
                            setKeepPortraitFR();
                            this.mStackSupervisor.mNoAnimActivities.clear();
                            ActivityStack lastStack2 = this.mStackSupervisor.getLastStack();
                            if (next.app == null || next.app.thread == null) {
                                if (next.hasBeenLaunched) {
                                    next.showStartingWindow(null, false, false);
                                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                        str4 = ActivityManagerService.TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Restarting: ");
                                        stringBuilder4.append(next);
                                        Slog.v(str4, stringBuilder4.toString());
                                    }
                                } else {
                                    next.hasBeenLaunched = true;
                                }
                                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                    str4 = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("No process,need to restart ");
                                    stringBuilder4.append(next);
                                    Slog.d(str4, stringBuilder4.toString());
                                }
                                z2 = true;
                                this.mStackSupervisor.startSpecificActivityLocked(next, true, true);
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                    str4 = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Resume running: ");
                                    stringBuilder4.append(next);
                                    stringBuilder4.append(" stopped=");
                                    stringBuilder4.append(next.stopped);
                                    stringBuilder4.append(" visible=");
                                    stringBuilder4.append(next.visible);
                                    Slog.v(str4, stringBuilder4.toString());
                                }
                                userLeaving = lastStack2 != null && (lastStack2.inMultiWindowMode() || !(lastStack2.mLastPausedActivity == null || lastStack2.mLastPausedActivity.fullscreen));
                                boolean lastActivityTranslucent = userLeaving;
                                synchronized (this.mWindowManager.getWindowManagerLock()) {
                                    StringBuilder stringBuilder5;
                                    if (next.visible) {
                                        try {
                                            if (!next.stopped) {
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            activityStack = lastStack2;
                                            throw th;
                                        }
                                    }
                                    next.setVisibility(true);
                                    next.startLaunchTickingLocked();
                                    ActivityRecord lastResumedActivity = lastStack2 == null ? processRecord : lastStack2.mResumedActivity;
                                    ActivityState lastState = next.getState();
                                    this.mService.updateCpuStats();
                                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                        str4 = ActivityManagerService.TAG;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("Moving to RESUMED: ");
                                        stringBuilder5.append(next);
                                        stringBuilder5.append(" (in existing)");
                                        Slog.v(str4, stringBuilder5.toString());
                                    }
                                    try {
                                        next.setState(ActivityState.RESUMED, "resumeTopActivityInnerLocked");
                                        if (!this.mService.getActivityStartController().mCurActivityPkName.equals(next.packageName)) {
                                            Jlog.warmLaunchingAppBegin(next.packageName);
                                            Jlog.d(142, next.packageName, next.app.pid, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                            LogPower.push(113, next.packageName);
                                            this.mService.getActivityStartController().mCurActivityPkName = next.packageName;
                                        }
                                        this.mService.updateLruProcessLocked(next.app, true, processRecord);
                                        updateLRUListLocked(next);
                                        this.mService.updateOomAdjLocked();
                                        if (this.mStackSupervisor.isFocusedStack(this)) {
                                            z = this.mStackSupervisor.ensureVisibilityAndConfig(next, this.mDisplayId, true, false) ^ true;
                                        } else {
                                            z = true;
                                        }
                                        if (z) {
                                            ActivityRecord nextNext = topRunningActivityLocked();
                                            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                                                String str5 = ActivityManagerService.TAG;
                                                StringBuilder stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("Activity config changed during resume: ");
                                                stringBuilder6.append(next);
                                                stringBuilder6.append(", new next: ");
                                                stringBuilder6.append(nextNext);
                                                Slog.i(str5, stringBuilder6.toString());
                                            }
                                            if (nextNext != next) {
                                                this.mStackSupervisor.scheduleResumeTopActivities();
                                            }
                                            this.mService.setFocusedActivityLockedForNavi(next);
                                            if (!next.visible || next.stopped) {
                                                next.setVisibility(true);
                                            }
                                            next.completeResumeLocked();
                                            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                                this.mStackSupervisor.validateTopActivitiesLocked();
                                            }
                                        } else {
                                            try {
                                                ClientTransaction transaction = ClientTransaction.obtain(next.app.thread, next.appToken);
                                                ArrayList<ResultInfo> a = next.results;
                                                if (a != null) {
                                                    try {
                                                        int N = a.size();
                                                        if (!next.finishing && N > 0) {
                                                            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                                                                String str6 = ActivityManagerService.TAG;
                                                                stringBuilder5 = new StringBuilder();
                                                                lastStack = lastStack2;
                                                                try {
                                                                    stringBuilder5.append("Delivering results to ");
                                                                    stringBuilder5.append(next);
                                                                    stringBuilder5.append(": ");
                                                                    stringBuilder5.append(a);
                                                                    Slog.v(str6, stringBuilder5.toString());
                                                                } catch (Exception e3) {
                                                                    activityStack = lastStack;
                                                                } catch (Throwable th3) {
                                                                    th = th3;
                                                                    activityStack = lastStack;
                                                                    throw th;
                                                                }
                                                            }
                                                            lastStack = lastStack2;
                                                            transaction.addCallback(ActivityResultItem.obtain(a));
                                                            if (next.newIntents != null) {
                                                                transaction.addCallback(NewIntentItem.obtain(next.newIntents, false));
                                                            }
                                                            next.notifyAppResumed(next.stopped);
                                                            EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, new Object[]{Integer.valueOf(next.userId), Integer.valueOf(System.identityHashCode(next)), Integer.valueOf(next.getTask().taskId), next.shortComponentName});
                                                            next.sleeping = false;
                                                            this.mService.getAppWarningsLocked().onResumeActivity(next);
                                                            this.mService.showAskCompatModeDialogLocked(next);
                                                            next.app.pendingUiClean = true;
                                                            next.app.forceProcessStateUpTo(this.mService.mTopProcessState);
                                                            next.clearOptionsLocked();
                                                            resumeCustomActivity(next);
                                                            if (Jlog.isPerfTest()) {
                                                                Jlog.i(3044, Intent.toPkgClsString(next.realActivity, "who"));
                                                            }
                                                            transaction.setLifecycleStateRequest(ResumeActivityItem.obtain(next.app.repProcState, this.mService.isNextTransitionForward()));
                                                            this.mService.getLifecycleManager().scheduleTransaction(transaction);
                                                            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                                str3 = ActivityManagerService.TAG;
                                                                StringBuilder stringBuilder7 = new StringBuilder();
                                                                stringBuilder7.append("resumeTopActivityLocked: Resumed ");
                                                                stringBuilder7.append(next);
                                                                Slog.d(str3, stringBuilder7.toString());
                                                            }
                                                        }
                                                    } catch (Exception e4) {
                                                        activityStack = lastStack2;
                                                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                        }
                                                        next.setState(lastState, "resumeTopActivityInnerLocked");
                                                        if (lastResumedActivity != null) {
                                                        }
                                                        str3 = ActivityManagerService.TAG;
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Restarting because process died: ");
                                                        stringBuilder.append(next);
                                                        Slog.i(str3, stringBuilder.toString());
                                                        if (next.hasBeenLaunched) {
                                                        }
                                                        this.mStackSupervisor.startSpecificActivityLocked(next, true, false);
                                                        if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                                        }
                                                        return true;
                                                    } catch (Throwable th4) {
                                                    }
                                                }
                                                lastStack = lastStack2;
                                                try {
                                                    if (next.newIntents != null) {
                                                    }
                                                    next.notifyAppResumed(next.stopped);
                                                    EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, new Object[]{Integer.valueOf(next.userId), Integer.valueOf(System.identityHashCode(next)), Integer.valueOf(next.getTask().taskId), next.shortComponentName});
                                                    next.sleeping = false;
                                                    this.mService.getAppWarningsLocked().onResumeActivity(next);
                                                    this.mService.showAskCompatModeDialogLocked(next);
                                                    next.app.pendingUiClean = true;
                                                    next.app.forceProcessStateUpTo(this.mService.mTopProcessState);
                                                    next.clearOptionsLocked();
                                                    resumeCustomActivity(next);
                                                    if (Jlog.isPerfTest()) {
                                                    }
                                                    transaction.setLifecycleStateRequest(ResumeActivityItem.obtain(next.app.repProcState, this.mService.isNextTransitionForward()));
                                                    this.mService.getLifecycleManager().scheduleTransaction(transaction);
                                                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                    }
                                                } catch (Exception e5) {
                                                    activityStack = lastStack;
                                                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                    }
                                                    next.setState(lastState, "resumeTopActivityInnerLocked");
                                                    if (lastResumedActivity != null) {
                                                    }
                                                    str3 = ActivityManagerService.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Restarting because process died: ");
                                                    stringBuilder.append(next);
                                                    Slog.i(str3, stringBuilder.toString());
                                                    if (next.hasBeenLaunched) {
                                                    }
                                                    this.mStackSupervisor.startSpecificActivityLocked(next, true, false);
                                                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                                    }
                                                    return true;
                                                }
                                            } catch (Exception e6) {
                                                boolean z4 = z;
                                                activityStack = lastStack2;
                                                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                    str3 = ActivityManagerService.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Resume failed; resetting state to ");
                                                    stringBuilder.append(lastState);
                                                    stringBuilder.append(": ");
                                                    stringBuilder.append(next);
                                                    Slog.v(str3, stringBuilder.toString());
                                                }
                                                next.setState(lastState, "resumeTopActivityInnerLocked");
                                                if (lastResumedActivity != null) {
                                                    lastResumedActivity.setState(ActivityState.RESUMED, "resumeTopActivityInnerLocked");
                                                }
                                                str3 = ActivityManagerService.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Restarting because process died: ");
                                                stringBuilder.append(next);
                                                Slog.i(str3, stringBuilder.toString());
                                                if (next.hasBeenLaunched) {
                                                    next.hasBeenLaunched = true;
                                                } else if (activityStack != null && activityStack.isTopStackOnDisplay()) {
                                                    next.showStartingWindow(null, false, false);
                                                }
                                                this.mStackSupervisor.startSpecificActivityLocked(next, true, false);
                                                if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                                    this.mStackSupervisor.validateTopActivitiesLocked();
                                                }
                                                return true;
                                            }
                                            try {
                                            } catch (Throwable th5) {
                                                th = th5;
                                                activityStack = lastStack;
                                                throw th;
                                            }
                                        }
                                    } catch (Throwable th6) {
                                        th = th6;
                                        throw th;
                                    }
                                }
                            }
                            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                                this.mStackSupervisor.validateTopActivitiesLocked();
                            }
                            return z2;
                        }
                    }
                    Flog.i(101, "Skip resume: some activity is pausing.");
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return false;
                } else {
                    str = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping resume of top activity ");
                    stringBuilder2.append(next);
                    stringBuilder2.append(": user ");
                    stringBuilder2.append(next.userId);
                    stringBuilder2.append(" is stopped");
                    Slog.w(str, stringBuilder2.toString());
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return false;
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No activities left in the stack: ");
            stringBuilder2.append(this);
            Flog.i(101, stringBuilder2.toString());
            return resumeTopActivityInNextFocusableStack(activityRecord, activityOptions, "noMoreActivities");
        }
        Flog.i(101, "It is not ready yet");
        return false;
    }

    private boolean resumeTopActivityInNextFocusableStack(ActivityRecord prev, ActivityOptions options, String reason) {
        boolean z = false;
        if (adjustFocusToNextFocusableStack(reason)) {
            ActivityStack nextFocusableStack = this.mStackSupervisor.getFocusedStack();
            if (!(nextFocusableStack == null || nextFocusableStack.topRunningActivityLocked(true) == null)) {
                this.mStackSupervisor.inResumeTopActivity = false;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("adjust focus to next focusable stack: ");
                stringBuilder.append(nextFocusableStack);
                Flog.i(101, stringBuilder.toString());
            }
            return this.mStackSupervisor.resumeFocusedStackTopActivityLocked(nextFocusableStack, prev, null);
        }
        ActivityOptions.abort(options);
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("resumeTopActivityInNextFocusableStack: ");
            stringBuilder2.append(reason);
            stringBuilder2.append(", go home");
            Slog.d(str, stringBuilder2.toString());
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            this.mStackSupervisor.validateTopActivitiesLocked();
        }
        Jlog.d(24, "JL_LAUNCHER_STARTUP");
        if (isOnHomeDisplay() && this.mStackSupervisor.resumeHomeStackTask(prev, reason)) {
            z = true;
        }
        return z;
    }

    private TaskRecord getNextTask(TaskRecord targetTask) {
        int index = this.mTaskHistory.indexOf(targetTask);
        if (index >= 0) {
            int numTasks = this.mTaskHistory.size();
            for (int i = index + 1; i < numTasks; i++) {
                TaskRecord task = (TaskRecord) this.mTaskHistory.get(i);
                if (task.userId == targetTask.userId || (this.mStackSupervisor.isCurrentProfileLocked(task.userId) && this.mStackSupervisor.isCurrentProfileLocked(targetTask.userId))) {
                    return task;
                }
            }
        }
        return null;
    }

    int getAdjustedPositionForTask(TaskRecord task, int suggestedPosition, ActivityRecord starting) {
        int maxPosition = this.mTaskHistory.size();
        if ((starting != null && starting.okToShowLocked()) || (starting == null && task.okToShowLocked())) {
            return Math.min(suggestedPosition, maxPosition);
        }
        while (maxPosition > 0) {
            TaskRecord tmpTask = (TaskRecord) this.mTaskHistory.get(maxPosition - 1);
            if (!this.mStackSupervisor.isCurrentProfileLocked(tmpTask.userId) || tmpTask.topRunningActivityLocked() == null) {
                break;
            }
            maxPosition--;
        }
        return Math.min(suggestedPosition, maxPosition);
    }

    private void insertTaskAtPosition(TaskRecord task, int position) {
        if (position >= this.mTaskHistory.size()) {
            insertTaskAtTop(task, null);
        } else if (position <= 0) {
            insertTaskAtBottom(task);
        } else {
            position = getAdjustedPositionForTask(task, position, null);
            this.mTaskHistory.remove(task);
            this.mTaskHistory.add(position, task);
            this.mWindowContainerController.positionChildAt(task.getWindowContainerController(), position);
            updateTaskMovement(task, true);
        }
    }

    private void insertTaskAtTop(TaskRecord task, ActivityRecord starting) {
        this.mTaskHistory.remove(task);
        this.mTaskHistory.add(getAdjustedPositionForTask(task, this.mTaskHistory.size(), starting), task);
        updateTaskMovement(task, true);
        this.mWindowContainerController.positionChildAtTop(task.getWindowContainerController(), true);
    }

    private void insertTaskAtBottom(TaskRecord task) {
        this.mTaskHistory.remove(task);
        this.mTaskHistory.add(getAdjustedPositionForTask(task, 0, null), task);
        updateTaskMovement(task, true);
        this.mWindowContainerController.positionChildAtBottom(task.getWindowContainerController(), true);
    }

    void startActivityLocked(ActivityRecord r, ActivityRecord focusedTopActivity, boolean newTask, boolean keepCurTransition, ActivityOptions options) {
        StringBuilder stringBuilder;
        String str;
        ActivityRecord activityRecord = r;
        ActivityRecord activityRecord2 = focusedTopActivity;
        boolean z = newTask;
        boolean z2 = keepCurTransition;
        ActivityOptions activityOptions = options;
        TaskRecord rTask = r.getTask();
        int taskId = rTask.taskId;
        if (!activityRecord.mLaunchTaskBehind && (taskForIdLocked(taskId) == null || z)) {
            insertTaskAtTop(rTask, activityRecord);
        }
        TaskRecord task = null;
        if (!z) {
            boolean startIt = true;
            for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
                task = (TaskRecord) this.mTaskHistory.get(taskNdx);
                if (task.getTopActivity() != null) {
                    if (task == rTask) {
                        if (!startIt) {
                            if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                String str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Adding activity ");
                                stringBuilder2.append(activityRecord);
                                stringBuilder2.append(" to task ");
                                stringBuilder2.append(task);
                                Slog.i(str2, stringBuilder2.toString(), new RuntimeException("here").fillInStackTrace());
                            }
                            activityRecord.createWindowContainer(activityRecord.info.navigationHide);
                            ActivityOptions.abort(options);
                            return;
                        }
                    } else if (task.numFullscreen > 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("starting r: ");
                        stringBuilder.append(activityRecord);
                        stringBuilder.append(" blocked by task: ");
                        stringBuilder.append(task);
                        Flog.i(101, stringBuilder.toString());
                        startIt = false;
                    }
                }
            }
        }
        TaskRecord activityTask = r.getTask();
        if (task == activityTask && this.mTaskHistory.indexOf(task) != this.mTaskHistory.size() - 1) {
            this.mStackSupervisor.mUserLeaving = false;
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(ActivityManagerService.TAG, "startActivity() behind front, mUserLeaving=false");
            }
        }
        task = activityTask;
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Adding activity ");
            stringBuilder.append(activityRecord);
            stringBuilder.append(" to stack to task ");
            stringBuilder.append(task);
            Slog.i(str, stringBuilder.toString(), new RuntimeException("here").fillInStackTrace());
        }
        if (r.getWindowContainerController() == null) {
            activityRecord.createWindowContainer(activityRecord.info.navigationHide);
        }
        task.setFrontOfTask();
        if (!isHomeOrRecentsStack() || numActivities() > 0) {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Prepare open transition: starting ");
                stringBuilder.append(activityRecord);
                Slog.v(str, stringBuilder.toString());
            }
            if ((activityRecord.intent.getFlags() & 65536) != 0) {
                this.mWindowManager.prepareAppTransition(0, z2);
                this.mStackSupervisor.mNoAnimActivities.add(activityRecord);
            } else {
                int transit = 6;
                if (z) {
                    if (activityRecord.mLaunchTaskBehind) {
                        transit = 16;
                    } else {
                        if (canEnterPipOnTaskSwitch(activityRecord2, null, activityRecord, activityOptions)) {
                            activityRecord2.supportsEnterPipOnTaskSwitch = true;
                        }
                        transit = 8;
                    }
                }
                this.mWindowManager.prepareAppTransition(transit, z2);
                this.mStackSupervisor.mNoAnimActivities.remove(activityRecord);
            }
            boolean doShow = true;
            if (z) {
                if ((activityRecord.intent.getFlags() & DumpState.DUMP_COMPILER_STATS) != 0) {
                    resetTaskIfNeededLocked(activityRecord, activityRecord);
                    doShow = topRunningNonDelayedActivityLocked(null) == activityRecord;
                }
            } else if (activityOptions != null && options.getAnimationType() == 5) {
                doShow = false;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("startActivityLocked doShow= ");
            stringBuilder3.append(doShow);
            stringBuilder3.append(" mLaunchTaskBehind= ");
            stringBuilder3.append(activityRecord.mLaunchTaskBehind);
            Flog.i(301, stringBuilder3.toString());
            if (activityRecord.mLaunchTaskBehind) {
                activityRecord.setVisibility(true);
                ensureActivitiesVisibleLocked(null, 0, false);
            } else if (doShow) {
                TaskRecord prevTask = r.getTask();
                ActivityRecord prev = prevTask.topRunningActivityWithStartingWindowLocked();
                if (prev != null) {
                    if (prev.getTask() != prevTask) {
                        prev = null;
                    } else if (prev.nowVisible) {
                        prev = null;
                    }
                }
                if (isSplitActivity(activityRecord.intent)) {
                    this.mWindowManager.setSplittable(true);
                } else if (this.mWindowManager.isSplitMode()) {
                    this.mWindowManager.setSplittable(false);
                }
                activityRecord.showStartingWindow(prev, z, isTaskSwitch(r, focusedTopActivity));
            }
        } else {
            ActivityOptions.abort(options);
        }
    }

    private boolean canEnterPipOnTaskSwitch(ActivityRecord pipCandidate, TaskRecord toFrontTask, ActivityRecord toFrontActivity, ActivityOptions opts) {
        if ((opts != null && opts.disallowEnterPictureInPictureWhileLaunching()) || pipCandidate == null || pipCandidate.inPinnedWindowingMode()) {
            return false;
        }
        ActivityStack targetStack = toFrontTask != null ? toFrontTask.getStack() : toFrontActivity.getStack();
        if (targetStack == null || !targetStack.isActivityTypeAssistant()) {
            return true;
        }
        return false;
    }

    private boolean isTaskSwitch(ActivityRecord r, ActivityRecord topFocusedActivity) {
        return (topFocusedActivity == null || r.getTask() == topFocusedActivity.getTask()) ? false : true;
    }

    private ActivityOptions resetTargetTaskIfNeededLocked(TaskRecord task, boolean forceReset) {
        TaskRecord taskRecord = task;
        ArrayList<ActivityRecord> activities = taskRecord.mActivities;
        int numActivities = activities.size();
        int rootActivityNdx = task.findEffectiveRootIndex();
        int i = numActivities - 1;
        ActivityOptions topOptions = null;
        int replyChainEnd = -1;
        boolean canMoveOptions = true;
        while (true) {
            int i2 = i;
            if (i2 <= rootActivityNdx) {
                break;
            }
            ActivityRecord target = (ActivityRecord) activities.get(i2);
            int i3;
            if (target.frontOfTask) {
                i3 = numActivities;
                break;
            }
            int flags = target.info.flags;
            boolean finishOnTaskLaunch = (flags & 2) != 0;
            boolean allowTaskReparenting = (flags & 64) != 0;
            boolean clearWhenTaskReset = (target.intent.getFlags() & DumpState.DUMP_FROZEN) != 0;
            StringBuilder stringBuilder;
            if (finishOnTaskLaunch || clearWhenTaskReset || target.resultTo == null) {
                int end;
                boolean noOptions;
                ActivityRecord p;
                if (finishOnTaskLaunch || clearWhenTaskReset || !allowTaskReparenting || target.taskAffinity == null || target.taskAffinity.equals(taskRecord.affinity)) {
                    i3 = numActivities;
                    numActivities = target;
                    if (forceReset || finishOnTaskLaunch || clearWhenTaskReset) {
                        if (clearWhenTaskReset) {
                            end = activities.size() - 1;
                        } else if (replyChainEnd < 0) {
                            end = i2;
                        } else {
                            end = replyChainEnd;
                        }
                        noOptions = canMoveOptions;
                        ActivityOptions topOptions2 = topOptions;
                        int end2 = end;
                        end = i2;
                        while (true) {
                            flags = end;
                            if (flags > end2) {
                                break;
                            }
                            p = (ActivityRecord) activities.get(flags);
                            if (!p.finishing) {
                                canMoveOptions = false;
                                if (noOptions && topOptions2 == null) {
                                    topOptions2 = p.takeOptionsLocked();
                                    if (topOptions2 != null) {
                                        noOptions = false;
                                    }
                                }
                                boolean noOptions2 = noOptions;
                                ActivityOptions topOptions3 = topOptions2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("resetTaskIntendedTask: calling finishActivity on ");
                                stringBuilder.append(p);
                                Flog.i(105, stringBuilder.toString());
                                int i4 = 105;
                                int srcPos = flags;
                                if (finishActivityLocked(p, 0, null, "reset-task", 0)) {
                                    end2--;
                                    flags = srcPos - 1;
                                    topOptions2 = topOptions3;
                                    noOptions = noOptions2;
                                } else {
                                    topOptions2 = topOptions3;
                                    noOptions = noOptions2;
                                    flags = srcPos;
                                }
                            }
                            end = flags + 1;
                        }
                        replyChainEnd = -1;
                        topOptions = topOptions2;
                    } else {
                        end = -1;
                    }
                } else {
                    TaskRecord targetTask;
                    ActivityRecord activityRecord = (this.mTaskHistory.isEmpty() || ((TaskRecord) this.mTaskHistory.get(0)).mActivities.isEmpty()) ? null : (ActivityRecord) ((TaskRecord) this.mTaskHistory.get(0)).mActivities.get(0);
                    ActivityRecord bottom = activityRecord;
                    StringBuilder stringBuilder2;
                    if (bottom == null || target.taskAffinity == null || !target.taskAffinity.equals(bottom.getTask().affinity)) {
                        int i5 = 105;
                        i3 = numActivities;
                        numActivities = target;
                        targetTask = createTaskRecord(this.mStackSupervisor.getNextTaskIdForUserLocked(target.userId), target.info, null, null, null, null);
                        targetTask.affinityIntent = numActivities.intent;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ResetTask:Start pushing activity ");
                        stringBuilder2.append(numActivities);
                        stringBuilder2.append(" out to new task ");
                        stringBuilder2.append(targetTask);
                        Flog.i(105, stringBuilder2.toString());
                    } else {
                        targetTask = bottom.getTask();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ResetTask:Start pushing activity ");
                        stringBuilder2.append(target);
                        stringBuilder2.append(" out to bottom task ");
                        stringBuilder2.append(targetTask);
                        Flog.i(105, stringBuilder2.toString());
                        ActivityRecord activityRecord2 = bottom;
                        int i6 = flags;
                        i3 = numActivities;
                        numActivities = target;
                        target = 105;
                    }
                    noOptions = canMoveOptions;
                    int start = replyChainEnd < 0 ? i2 : replyChainEnd;
                    boolean noOptions3 = noOptions;
                    int srcPos2 = start;
                    while (srcPos2 >= i2) {
                        int start2;
                        p = (ActivityRecord) activities.get(srcPos2);
                        if (p.finishing != 0) {
                            start2 = start;
                        } else {
                            if (noOptions3 && topOptions == null) {
                                topOptions = p.takeOptionsLocked();
                                if (topOptions != null) {
                                    noOptions3 = false;
                                }
                            }
                            if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                String str = ActivityManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                start2 = start;
                                stringBuilder3.append("Removing activity ");
                                stringBuilder3.append(p);
                                stringBuilder3.append(" from task=");
                                stringBuilder3.append(taskRecord);
                                stringBuilder3.append(" adding to task=");
                                stringBuilder3.append(targetTask);
                                stringBuilder3.append(" Callers=");
                                stringBuilder3.append(Debug.getCallers(4));
                                Slog.i(str, stringBuilder3.toString());
                            } else {
                                start2 = start;
                            }
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("ResetTask:Pushing next activity ");
                            stringBuilder4.append(p);
                            stringBuilder4.append(" out to target's task ");
                            stringBuilder4.append(numActivities);
                            Flog.i(105, stringBuilder4.toString());
                            p.reparent(targetTask, 0, "resetTargetTaskIfNeeded");
                            canMoveOptions = false;
                        }
                        srcPos2--;
                        start = start2;
                    }
                    this.mWindowContainerController.positionChildAtBottom(targetTask.getWindowContainerController(), false);
                    end = -1;
                }
                replyChainEnd = end;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ResetTask:Keeping the end of the reply chain, target= ");
                stringBuilder.append(target.task);
                stringBuilder.append(" targetI=");
                stringBuilder.append(i2);
                stringBuilder.append(" replyChainEnd=");
                stringBuilder.append(replyChainEnd);
                Flog.i(105, stringBuilder.toString());
                if (replyChainEnd < 0) {
                    replyChainEnd = i2;
                }
                i3 = numActivities;
            }
            i = i2 - 1;
            numActivities = i3;
        }
        return topOptions;
    }

    private int resetAffinityTaskIfNeededLocked(TaskRecord affinityTask, TaskRecord task, boolean topTaskIsHigher, boolean forceReset, int taskInsertionPoint) {
        int taskId;
        String taskAffinity;
        int numActivities;
        int rootActivityNdx;
        Object obj = affinityTask;
        TaskRecord taskRecord = task;
        int taskId2 = taskRecord.taskId;
        String taskAffinity2 = taskRecord.affinity;
        ArrayList<ActivityRecord> activities = obj.mActivities;
        int numActivities2 = activities.size();
        int rootActivityNdx2 = affinityTask.findEffectiveRootIndex();
        int i = numActivities2 - 1;
        int replyChainEnd = -1;
        int taskInsertionPoint2 = taskInsertionPoint;
        while (i > rootActivityNdx2) {
            ActivityRecord target = (ActivityRecord) activities.get(i);
            if (target.frontOfTask) {
                break;
            }
            TaskRecord taskRecord2;
            int flags = target.info.flags;
            boolean allowTaskReparenting = false;
            boolean finishOnTaskLaunch = (flags & 2) != 0;
            if ((flags & 64) != 0) {
                allowTaskReparenting = true;
            }
            if (target.resultTo != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ResetTaskAffinity:Keeping the end of the reply chain, target= ");
                stringBuilder.append(target.task);
                stringBuilder.append(" targetI=");
                stringBuilder.append(i);
                stringBuilder.append(" replyChainEnd=");
                stringBuilder.append(replyChainEnd);
                Flog.i(105, stringBuilder.toString());
                if (replyChainEnd < 0) {
                    replyChainEnd = i;
                }
                taskId = taskId2;
                taskAffinity = taskAffinity2;
                numActivities = numActivities2;
                rootActivityNdx = rootActivityNdx2;
            } else if (topTaskIsHigher && allowTaskReparenting && taskAffinity2 != null && taskAffinity2.equals(target.taskAffinity)) {
                ActivityStack activityStack;
                StringBuilder stringBuilder2;
                int taskInsertionPoint3;
                if (forceReset) {
                    activityStack = this;
                    taskId = taskId2;
                    taskAffinity = taskAffinity2;
                    numActivities = numActivities2;
                    rootActivityNdx = rootActivityNdx2;
                } else if (finishOnTaskLaunch) {
                    activityStack = this;
                    taskId = taskId2;
                    taskAffinity = taskAffinity2;
                    numActivities = numActivities2;
                    rootActivityNdx = rootActivityNdx2;
                } else {
                    ActivityRecord p;
                    if (taskInsertionPoint2 < 0) {
                        taskId = taskId2;
                        taskInsertionPoint2 = taskRecord.mActivities.size();
                    } else {
                        taskId = taskId2;
                    }
                    taskId2 = replyChainEnd >= 0 ? replyChainEnd : i;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        taskAffinity = taskAffinity2;
                        taskAffinity2 = ActivityManagerService.TAG;
                        numActivities = numActivities2;
                        stringBuilder2 = new StringBuilder();
                        rootActivityNdx = rootActivityNdx2;
                        stringBuilder2.append("Reparenting from task=");
                        stringBuilder2.append(obj);
                        stringBuilder2.append(":");
                        stringBuilder2.append(taskId2);
                        stringBuilder2.append("-");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" to task=");
                        stringBuilder2.append(taskRecord);
                        stringBuilder2.append(":");
                        stringBuilder2.append(taskInsertionPoint2);
                        Slog.v(taskAffinity2, stringBuilder2.toString());
                    } else {
                        taskAffinity = taskAffinity2;
                        numActivities = numActivities2;
                        rootActivityNdx = rootActivityNdx2;
                    }
                    int srcPos = taskId2;
                    while (srcPos >= i) {
                        StringBuilder stringBuilder3;
                        p = (ActivityRecord) activities.get(srcPos);
                        p.reparent(taskRecord, taskInsertionPoint2, "resetAffinityTaskIfNeededLocked");
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                            String str = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            taskInsertionPoint3 = taskInsertionPoint2;
                            stringBuilder3.append("Removing and adding activity ");
                            stringBuilder3.append(p);
                            stringBuilder3.append(" to stack at ");
                            stringBuilder3.append(taskRecord);
                            stringBuilder3.append(" callers=");
                            stringBuilder3.append(Debug.getCallers(3));
                            Slog.i(str, stringBuilder3.toString());
                        } else {
                            taskInsertionPoint3 = taskInsertionPoint2;
                        }
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("ResetTaskAffinity:Pulling activity ");
                        stringBuilder3.append(p);
                        stringBuilder3.append(" from ");
                        stringBuilder3.append(srcPos);
                        stringBuilder3.append(" in to resetting task ");
                        stringBuilder3.append(taskRecord);
                        Flog.i(105, stringBuilder3.toString());
                        srcPos--;
                        taskInsertionPoint2 = taskInsertionPoint3;
                        taskRecord2 = affinityTask;
                    }
                    taskInsertionPoint3 = taskInsertionPoint2;
                    this.mWindowContainerController.positionChildAtTop(task.getWindowContainerController(), true);
                    if (target.info.launchMode == 1) {
                        taskInsertionPoint2 = taskRecord.mActivities;
                        taskAffinity2 = taskInsertionPoint2.indexOf(target);
                        if (taskAffinity2 > null) {
                            p = (ActivityRecord) taskInsertionPoint2.get(taskAffinity2 - 1);
                            if (p.intent.getComponent().equals(target.intent.getComponent())) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("ResetTaskAffinity:Drop singleTop activity ");
                                stringBuilder4.append(p);
                                stringBuilder4.append(" for target ");
                                stringBuilder4.append(target);
                                Flog.i(105, stringBuilder4.toString());
                                finishActivityLocked(p, 0, null, "replace", false);
                            }
                        }
                    }
                    replyChainEnd = -1;
                    taskInsertionPoint2 = taskInsertionPoint3;
                }
                int start = replyChainEnd >= 0 ? replyChainEnd : i;
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Finishing task at index ");
                    stringBuilder5.append(start);
                    stringBuilder5.append(" to ");
                    stringBuilder5.append(i);
                    Slog.v(str2, stringBuilder5.toString());
                }
                for (taskId2 = start; taskId2 >= i; taskId2--) {
                    ActivityRecord taskAffinity3 = (ActivityRecord) activities.get(taskId2);
                    if (!taskAffinity3.finishing) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ResetTaskAffinity:finishActivity pos:  ");
                        stringBuilder2.append(taskId2);
                        stringBuilder2.append(" acitivity: ");
                        stringBuilder2.append(taskAffinity3);
                        Flog.i(105, stringBuilder2.toString());
                        activityStack.finishActivityLocked(taskAffinity3, 0, null, "move-affinity", false);
                    }
                }
                taskInsertionPoint3 = taskInsertionPoint2;
                replyChainEnd = -1;
                taskInsertionPoint2 = taskInsertionPoint3;
            } else {
                taskId = taskId2;
                taskAffinity = taskAffinity2;
                numActivities = numActivities2;
                rootActivityNdx = rootActivityNdx2;
            }
            i--;
            taskId2 = taskId;
            taskAffinity2 = taskAffinity;
            numActivities2 = numActivities;
            rootActivityNdx2 = rootActivityNdx;
            taskRecord2 = affinityTask;
            taskRecord = task;
        }
        taskId = taskId2;
        taskAffinity = taskAffinity2;
        numActivities = numActivities2;
        rootActivityNdx = rootActivityNdx2;
        return taskInsertionPoint2;
    }

    final ActivityRecord resetTaskIfNeededLocked(ActivityRecord taskTop, ActivityRecord newActivity) {
        boolean forceReset = (newActivity.info.flags & 4) != 0;
        TaskRecord task = taskTop.getTask();
        int i = this.mTaskHistory.size() - 1;
        boolean taskFound = false;
        ActivityOptions topOptions = null;
        int reparentInsertionPoint = -1;
        while (true) {
            int i2 = i;
            if (i2 < 0) {
                break;
            }
            TaskRecord targetTask = (TaskRecord) this.mTaskHistory.get(i2);
            if (targetTask == task) {
                topOptions = resetTargetTaskIfNeededLocked(task, forceReset);
                taskFound = true;
            } else {
                reparentInsertionPoint = resetAffinityTaskIfNeededLocked(targetTask, task, taskFound, forceReset, reparentInsertionPoint);
            }
            i = i2 - 1;
        }
        int taskNdx = this.mTaskHistory.indexOf(task);
        if (taskNdx >= 0) {
            int taskNdx2;
            while (true) {
                taskNdx2 = taskNdx - 1;
                taskTop = ((TaskRecord) this.mTaskHistory.get(taskNdx)).getTopActivity();
                if (taskTop != null || taskNdx2 < 0) {
                    taskNdx = taskNdx2;
                } else {
                    taskNdx = taskNdx2;
                }
            }
            taskNdx = taskNdx2;
        }
        if (topOptions != null) {
            if (taskTop != null) {
                taskTop.updateOptionsLocked(topOptions);
            } else {
                topOptions.abort();
            }
        }
        return taskTop;
    }

    void sendActivityResultLocked(int callingUid, ActivityRecord r, String resultWho, int requestCode, int resultCode, Intent data) {
        if (callingUid > 0) {
            this.mService.grantUriPermissionFromIntentLocked(callingUid, r.packageName, data, r.getUriPermissionsLocked(), r.userId);
        }
        if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Send activity result to ");
            stringBuilder.append(r);
            stringBuilder.append(" : who=");
            stringBuilder.append(resultWho);
            stringBuilder.append(" req=");
            stringBuilder.append(requestCode);
            stringBuilder.append(" res=");
            stringBuilder.append(resultCode);
            stringBuilder.append(" data=");
            stringBuilder.append(data);
            Slog.v(str, stringBuilder.toString());
        }
        if (!(this.mResumedActivity != r || r.app == null || r.app.thread == null)) {
            try {
                ArrayList<ResultInfo> list = new ArrayList();
                list.add(new ResultInfo(resultWho, requestCode, resultCode, data));
                this.mService.getLifecycleManager().scheduleTransaction(r.app.thread, r.appToken, ActivityResultItem.obtain(list));
                return;
            } catch (Exception e) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception thrown sending result to ");
                stringBuilder2.append(r);
                Slog.w(str2, stringBuilder2.toString(), e);
            }
        }
        r.addResultLocked(null, resultWho, requestCode, resultCode, data);
    }

    private boolean isATopFinishingTask(TaskRecord task) {
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            TaskRecord current = (TaskRecord) this.mTaskHistory.get(i);
            if (current.topRunningActivityLocked() != null) {
                return false;
            }
            if (current == task) {
                return true;
            }
        }
        return false;
    }

    private void adjustFocusedActivityStack(ActivityRecord r, String reason) {
        if (r != null && this.mStackSupervisor.isFocusedStack(this) && (this.mResumedActivity == r || this.mResumedActivity == null)) {
            ActivityRecord next = topRunningActivityLocked();
            String myReason = new StringBuilder();
            myReason.append(reason);
            myReason.append(" adjustFocus");
            myReason = myReason.toString();
            if (next == r) {
                this.mStackSupervisor.moveFocusableActivityStackToFrontLocked(this.mStackSupervisor.topRunningActivityLocked(), myReason);
            } else if (next != null && isFocusable()) {
            } else {
                if (r.getTask() == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("activity no longer associated with task:");
                    stringBuilder.append(r);
                    throw new IllegalStateException(stringBuilder.toString());
                } else if (!adjustFocusToNextFocusableStack(myReason)) {
                    this.mStackSupervisor.moveHomeStackTaskToTop(myReason);
                }
            }
        }
    }

    boolean adjustFocusToNextFocusableStack(String reason) {
        return adjustFocusToNextFocusableStack(reason, false);
    }

    private boolean adjustFocusToNextFocusableStack(String reason, boolean allowFocusSelf) {
        ActivityStack stack = this.mStackSupervisor.getNextFocusableStackLocked(this, allowFocusSelf ^ 1);
        String myReason = new StringBuilder();
        myReason.append(reason);
        myReason.append(" adjustFocusToNextFocusableStack");
        myReason = myReason.toString();
        if (stack == null) {
            return false;
        }
        ActivityRecord top = stack.topRunningActivityLocked();
        if (checkAdjustToPrimarySplitScreenStack(stack, top)) {
            Slog.w(ActivityManagerService.TAG, "adjustFocusToNextFocusableStack to primary split screen stack");
            return true;
        } else if (stack.isActivityTypeHome() && (top == null || !top.visible)) {
            return this.mStackSupervisor.moveHomeStackTaskToTop(reason);
        } else {
            stack.moveToFront(myReason);
            return true;
        }
    }

    private boolean checkAdjustToPrimarySplitScreenStack(ActivityStack targetStack, ActivityRecord targetActivityRecord) {
        if (getWindowingMode() == 4 && getActivityType() == 1) {
            if (((targetActivityRecord != null && targetActivityRecord.toString().contains("splitscreen.SplitScreenAppActivity")) || targetStack.getActivityType() == 3) && primarySplitScreenStackToFullScreen(null)) {
                return true;
            }
            if (targetStack.getWindowingMode() == 3 && primarySplitScreenStackToFullScreen(targetStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean primarySplitScreenStackToFullScreen(ActivityStack topPrimaryStack) {
        if (topPrimaryStack == null) {
            topPrimaryStack = getDisplay().getTopStackInWindowingMode(3);
        }
        if (topPrimaryStack == null) {
            return false;
        }
        this.mWindowManager.mShouldResetTime = true;
        this.mWindowManager.startFreezingScreen(0, 0);
        topPrimaryStack.moveToFront("adjustFocusedToSplitPrimaryStack");
        topPrimaryStack.setWindowingMode(1);
        this.mWindowManager.stopFreezingScreen();
        return true;
    }

    final void stopActivityLocked(ActivityRecord r) {
        String str;
        StringBuilder stringBuilder;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Stopping: ");
            stringBuilder.append(r);
            Slog.d(str, stringBuilder.toString());
        }
        if (!(((r.intent.getFlags() & 1073741824) == 0 && (r.info.flags & 128) == 0) || r.finishing)) {
            if (!shouldSleepActivities()) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("no-history finish of ");
                    stringBuilder.append(r);
                    Slog.d(str, stringBuilder.toString());
                }
                if (requestFinishActivityLocked(r.appToken, 0, null, "stop-no-history", false)) {
                    r.resumeKeyDispatchingLocked();
                    return;
                }
            } else if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not finishing noHistory ");
                stringBuilder.append(r);
                stringBuilder.append(" on stop because we're just sleeping");
                Slog.d(str, stringBuilder.toString());
            }
        }
        if (!(r.app == null || r.app.thread == null)) {
            adjustFocusedActivityStack(r, "stopActivity");
            r.resumeKeyDispatchingLocked();
            String str2;
            StringBuilder stringBuilder2;
            try {
                r.stopped = false;
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Moving to STOPPING: ");
                    stringBuilder2.append(r);
                    stringBuilder2.append(" (stop requested)");
                    Slog.v(str2, stringBuilder2.toString());
                }
                r.setState(ActivityState.STOPPING, "stopActivityLocked");
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    str2 = TAG_VISIBILITY;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Stopping visible=");
                    stringBuilder2.append(r.visible);
                    stringBuilder2.append(" for ");
                    stringBuilder2.append(r);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (!r.visible) {
                    r.setVisible(false);
                }
                EventLogTags.writeAmStopActivity(r.userId, System.identityHashCode(r), r.shortComponentName);
                this.mService.getLifecycleManager().scheduleTransaction(r.app.thread, r.appToken, StopActivityItem.obtain(r.visible, r.configChangeFlags));
                this.mService.notifyActivityState(r, ActivityState.STOPPED);
                if (shouldSleepOrShutDownActivities()) {
                    r.setSleeping(true);
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(104, r), 11000);
            } catch (Exception e) {
                Slog.w(ActivityManagerService.TAG, "Exception thrown during pause", e);
                r.stopped = true;
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Stop failed; moving to STOPPED: ");
                    stringBuilder2.append(r);
                    Slog.v(str2, stringBuilder2.toString());
                }
                r.setState(ActivityState.STOPPED, "stopActivityLocked");
                if (r.deferRelaunchUntilPaused) {
                    destroyActivityLocked(r, true, "stop-except");
                }
            }
        }
    }

    final boolean requestFinishActivityLocked(IBinder token, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        ActivityRecord r = isInStackLocked(token);
        if (ActivityManagerDebugConfig.DEBUG_RESULTS || ActivityManagerDebugConfig.DEBUG_STATES || HwSlog.HW_DEBUG_STATES) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Finishing activity token=");
            stringBuilder.append(token);
            stringBuilder.append(" r=, result=");
            stringBuilder.append(resultCode);
            stringBuilder.append(", data=");
            stringBuilder.append(resultData);
            stringBuilder.append(", reason=");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        if (r == null) {
            return false;
        }
        finishActivityLocked(r, resultCode, resultData, reason, oomAdj);
        return true;
    }

    final void finishSubActivityLocked(ActivityRecord self, String resultWho, int requestCode) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.resultTo == self && r.requestCode == requestCode && ((r.resultWho == null && resultWho == null) || (r.resultWho != null && r.resultWho.equals(resultWho)))) {
                    finishActivityLocked(r, 0, null, "request-sub", false);
                }
            }
        }
        this.mService.updateOomAdjLocked();
    }

    final TaskRecord finishTopCrashedActivityLocked(ProcessRecord app, String reason) {
        ActivityRecord r = topRunningActivityLocked();
        if (r == null || r.app != app) {
            return null;
        }
        String str = ActivityManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  finishTopCrashedActivityLocked Force finishing activity ");
        stringBuilder.append(r.intent.getComponent().flattenToShortString());
        Slog.w(str, stringBuilder.toString());
        TaskRecord finishedTask = r.getTask();
        int taskNdx = this.mTaskHistory.indexOf(finishedTask);
        int activityNdx = finishedTask.mActivities.indexOf(r);
        this.mWindowManager.prepareAppTransition(26, false);
        finishActivityLocked(r, 0, null, reason, false);
        activityNdx--;
        if (activityNdx < 0) {
            do {
                taskNdx--;
                if (taskNdx < 0) {
                    break;
                }
                activityNdx = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities.size() - 1;
            } while (activityNdx < 0);
        }
        if (activityNdx >= 0 && taskNdx < this.mTaskHistory.size() && activityNdx < ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities.size()) {
            r = (ActivityRecord) ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities.get(activityNdx);
            if (r.isState(ActivityState.RESUMED, ActivityState.PAUSING, ActivityState.PAUSED) && !(r.isActivityTypeHome() && this.mService.mHomeProcess == r.app)) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  finishTopCrashedActivityLocked non_home Force finishing activity ");
                stringBuilder2.append(r.intent.getComponent().flattenToShortString());
                Slog.w(str2, stringBuilder2.toString());
                finishActivityLocked(r, 0, null, reason, false);
            }
        }
        return finishedTask;
    }

    final void finishVoiceTask(IVoiceInteractionSession session) {
        IBinder sessionBinder = session.asBinder();
        boolean didOne = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord tr = (TaskRecord) this.mTaskHistory.get(taskNdx);
            int activityNdx;
            ActivityRecord r;
            if (tr.voiceSession == null || tr.voiceSession.asBinder() != sessionBinder) {
                for (activityNdx = tr.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                    r = (ActivityRecord) tr.mActivities.get(activityNdx);
                    if (r.voiceSession != null && r.voiceSession.asBinder() == sessionBinder) {
                        r.clearVoiceSessionLocked();
                        try {
                            r.app.thread.scheduleLocalVoiceInteractionStarted(r.appToken, null);
                        } catch (RemoteException e) {
                        }
                        this.mService.finishRunningVoiceLocked();
                        break;
                    }
                }
            } else {
                for (activityNdx = tr.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                    r = (ActivityRecord) tr.mActivities.get(activityNdx);
                    if (!r.finishing) {
                        finishActivityLocked(r, 0, null, "finish-voice", false);
                        didOne = true;
                    }
                }
            }
        }
        if (didOne) {
            this.mService.updateOomAdjLocked();
        }
    }

    final boolean finishActivityAffinityLocked(ActivityRecord r) {
        ArrayList<ActivityRecord> activities = r.getTask().mActivities;
        for (int index = activities.indexOf(r); index >= 0; index--) {
            ActivityRecord cur = (ActivityRecord) activities.get(index);
            if (!Objects.equals(cur.taskAffinity, r.taskAffinity)) {
                break;
            }
            finishActivityLocked(cur, 0, null, "request-affinity", true);
        }
        return true;
    }

    private void finishActivityResultsLocked(ActivityRecord r, int resultCode, Intent resultData) {
        ActivityRecord resultTo = r.resultTo;
        String str;
        StringBuilder stringBuilder;
        if (resultTo != null) {
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Adding result to ");
                stringBuilder.append(resultTo);
                stringBuilder.append(" who=");
                stringBuilder.append(r.resultWho);
                stringBuilder.append(" req=");
                stringBuilder.append(r.requestCode);
                stringBuilder.append(" res=");
                stringBuilder.append(resultCode);
                stringBuilder.append(" data=");
                stringBuilder.append(resultData);
                Slog.v(str, stringBuilder.toString());
            }
            if (!(resultTo.userId == r.userId || resultData == null)) {
                resultData.prepareToLeaveUser(r.userId);
            }
            if (r.info.applicationInfo.uid > 0) {
                this.mService.grantUriPermissionFromIntentLocked(r.info.applicationInfo.uid, resultTo.packageName, resultData, resultTo.getUriPermissionsLocked(), resultTo.userId);
            }
            resultTo.addResultLocked(r, r.resultWho, r.requestCode, resultCode, resultData);
            r.resultTo = null;
        } else if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("No result destination from ");
            stringBuilder.append(r);
            Slog.v(str, stringBuilder.toString());
        }
        r.results = null;
        r.pendingResults = null;
        r.newIntents = null;
        r.icicle = null;
    }

    final boolean finishActivityLocked(ActivityRecord r, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        return finishActivityLocked(r, resultCode, resultData, reason, oomAdj, false);
    }

    /* JADX WARNING: Missing block: B:34:0x00b5, code:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_TRANSITION != false) goto L_0x00b7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final boolean finishActivityLocked(ActivityRecord r, int resultCode, Intent resultData, String reason, boolean oomAdj, boolean pauseImmediately) {
        Throwable th;
        ActivityRecord activityRecord = r;
        boolean removedActivity = false;
        if (activityRecord.finishing) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Duplicate finish request for ");
            stringBuilder.append(activityRecord);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
        this.mWindowManager.deferSurfaceLayout();
        boolean z;
        boolean z2;
        try {
            r.makeFinishingLocked();
            TaskRecord task = r.getTask();
            if (task == null) {
                Slog.w(ActivityManagerService.TAG, "finishActivityLocked: r.getTask is null!");
                this.mWindowManager.continueSurfaceLayout();
                return false;
            }
            r5 = new Object[5];
            int finishMode = 2;
            r5[2] = Integer.valueOf(task.taskId);
            r5[3] = activityRecord.shortComponentName;
            r5[4] = reason;
            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, r5);
            ArrayList<ActivityRecord> activities = task.mActivities;
            int index = activities.indexOf(activityRecord);
            if (index < activities.size() - 1) {
                task.setFrontOfTask();
                if ((activityRecord.intent.getFlags() & DumpState.DUMP_FROZEN) != 0) {
                    ((ActivityRecord) activities.get(index + 1)).intent.addFlags(DumpState.DUMP_FROZEN);
                }
            }
            r.pauseKeyDispatchingLocked();
            adjustFocusedActivityStack(activityRecord, "finishActivity");
            finishActivityResultsLocked(r, resultCode, resultData);
            boolean endTask = index <= 0 && !task.isClearingToReuseTask();
            int transit = endTask ? 9 : 7;
            String str2;
            StringBuilder stringBuilder2;
            if (this.mResumedActivity == activityRecord) {
                if (!ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                }
                try {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Prepare close transition: finishing ");
                    stringBuilder2.append(activityRecord);
                    Slog.v(str2, stringBuilder2.toString());
                    if (endTask) {
                        this.mService.mTaskChangeNotificationController.notifyTaskRemovalStarted(task.taskId);
                    }
                    this.mWindowManager.prepareAppTransition(transit, false);
                    activityRecord.setVisibility(false);
                    if (this.mPausingActivity == null) {
                        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Finish needs to pause: ");
                            stringBuilder2.append(activityRecord);
                            Slog.v(str2, stringBuilder2.toString());
                        }
                        if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                            Slog.v(ActivityManagerService.TAG, "finish() => pause with userLeaving=false");
                        }
                        startPausingLocked(false, false, null, pauseImmediately);
                    } else {
                        z = pauseImmediately;
                    }
                    if (endTask) {
                        this.mService.getLockTaskController().clearLockedTask(task);
                    }
                    z2 = oomAdj;
                } catch (Throwable th2) {
                    th = th2;
                    z2 = oomAdj;
                    this.mWindowManager.continueSurfaceLayout();
                    throw th;
                }
            }
            z = pauseImmediately;
            if (activityRecord.isState(ActivityState.PAUSING)) {
                z2 = oomAdj;
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Finish waiting for pause of: ");
                    stringBuilder2.append(activityRecord);
                    Slog.v(str2, stringBuilder2.toString());
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    String str3 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Finish not pausing: ");
                    stringBuilder3.append(activityRecord);
                    Slog.v(str3, stringBuilder3.toString());
                }
                if (activityRecord.visible && !r.isSplitMode()) {
                    prepareActivityHideTransitionAnimation(activityRecord, transit);
                }
                if (!(activityRecord.visible || activityRecord.nowVisible)) {
                    finishMode = 1;
                }
                try {
                    if (finishCurrentActivityLocked(activityRecord, finishMode, oomAdj, "finishActivityLocked") == null) {
                        removedActivity = true;
                    }
                    if (task.onlyHasTaskOverlayActivities(true)) {
                        Iterator it = task.mActivities.iterator();
                        while (it.hasNext()) {
                            ActivityRecord taskOverlay = (ActivityRecord) it.next();
                            if (taskOverlay.mTaskOverlay) {
                                prepareActivityHideTransitionAnimation(taskOverlay, transit);
                            }
                        }
                    }
                    this.mWindowManager.continueSurfaceLayout();
                    return removedActivity;
                } catch (Throwable th3) {
                    th = th3;
                    this.mWindowManager.continueSurfaceLayout();
                    throw th;
                }
            }
            this.mWindowManager.continueSurfaceLayout();
            return false;
        } catch (Throwable th4) {
            th = th4;
            z2 = oomAdj;
            z = pauseImmediately;
            this.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    private void prepareActivityHideTransitionAnimation(ActivityRecord r, int transit) {
        this.mWindowManager.prepareAppTransition(transit, false);
        r.setVisibility(false);
        this.mWindowManager.executeAppTransition();
        if (!this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(r)) {
            this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.add(r);
        }
    }

    final ActivityRecord finishCurrentActivityLocked(ActivityRecord r, int mode, boolean oomAdj, String reason) {
        ActivityRecord next = this.mStackSupervisor.topRunningActivityLocked(true);
        StringBuilder stringBuilder;
        if (mode != 2 || (!(r.visible || r.nowVisible) || next == null || (next.nowVisible && !(HwPCUtils.isPcCastModeInServer() && HwPCUtils.isPcDynamicStack(r.getStackId()))))) {
            this.mStackSupervisor.mStoppingActivities.remove(r);
            this.mStackSupervisor.mGoingToSleepActivities.remove(r);
            this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(r);
            ActivityState prevState = r.getState();
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Moving to FINISHING: ");
                stringBuilder2.append(r);
                Slog.v(str, stringBuilder2.toString());
            }
            r.setState(ActivityState.FINISHING, "finishCurrentActivityLocked");
            boolean finishingActivityInNonFocusedStack = r.getStack() != this.mStackSupervisor.getFocusedStack() && prevState == ActivityState.PAUSED && mode == 2;
            String str2;
            StringBuilder stringBuilder3;
            if (mode == 0 || ((prevState == ActivityState.PAUSED && (mode == 1 || inPinnedWindowingMode())) || finishingActivityInNonFocusedStack || prevState == ActivityState.STOPPING || prevState == ActivityState.STOPPED || prevState == ActivityState.INITIALIZING)) {
                r.makeFinishingLocked();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("finish-imm:");
                stringBuilder4.append(reason);
                boolean activityRemoved = destroyActivityLocked(r, true, stringBuilder4.toString());
                if (finishingActivityInNonFocusedStack && this.mDisplayId != -1) {
                    this.mStackSupervisor.ensureVisibilityAndConfig(next, this.mDisplayId, false, true);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Moving to FINISHING r=");
                    stringBuilder.append(r);
                    stringBuilder.append(" destroy returned removed=");
                    stringBuilder.append(activityRemoved);
                    Flog.i(101, stringBuilder.toString());
                }
                if (activityRemoved) {
                    this.mStackSupervisor.mActivityLaunchTrack = "finishImmAtivityRemoved";
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
                if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("destroyActivityLocked: finishCurrentActivityLocked r=");
                    stringBuilder3.append(r);
                    stringBuilder3.append(" destroy returned removed=");
                    stringBuilder3.append(activityRemoved);
                    Slog.d(str2, stringBuilder3.toString());
                }
                return activityRemoved ? null : r;
            }
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                str2 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Enqueueing pending finish: ");
                stringBuilder3.append(r);
                Slog.v(str2, stringBuilder3.toString());
            }
            this.mStackSupervisor.mFinishingActivities.add(r);
            r.resumeKeyDispatchingLocked();
            this.mStackSupervisor.mActivityLaunchTrack = "enqueueFinishResume";
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            return r;
        }
        if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
            addToStopping(r, false, false);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Moving to STOPPING: ");
        stringBuilder.append(r);
        stringBuilder.append(" (finish requested)");
        Flog.i(101, stringBuilder.toString());
        r.setState(ActivityState.STOPPING, "finishCurrentActivityLocked");
        if (oomAdj) {
            this.mService.updateOomAdjLocked();
        }
        return r;
    }

    void finishAllActivitiesLocked(boolean immediately) {
        boolean noActivitiesInStack = true;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                noActivitiesInStack = false;
                if (!r.finishing || immediately) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("finishAllActivitiesLocked: finishing ");
                    stringBuilder.append(r);
                    stringBuilder.append(" immediately");
                    Slog.d(str, stringBuilder.toString());
                    finishCurrentActivityLocked(r, 0, false, "finishAllActivitiesLocked");
                }
            }
        }
        if (noActivitiesInStack) {
            remove();
        }
    }

    boolean inFrontOfStandardStack() {
        ActivityDisplay display = getDisplay();
        if (display == null) {
            return false;
        }
        int index = display.getIndexOf(this);
        if (index == 0) {
            return false;
        }
        return display.getChildAt(index - 1).isActivityTypeStandard();
    }

    boolean shouldUpRecreateTaskLocked(ActivityRecord srec, String destAffinity) {
        if (srec == null || srec.getTask().affinity == null || !srec.getTask().affinity.equals(destAffinity)) {
            return true;
        }
        TaskRecord task = srec.getTask();
        if (srec.frontOfTask && task.getBaseIntent() != null && task.getBaseIntent().isDocument()) {
            if (!inFrontOfStandardStack()) {
                return true;
            }
            int taskIdx = this.mTaskHistory.indexOf(task);
            if (taskIdx <= 0) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("shouldUpRecreateTask: task not in history for ");
                stringBuilder.append(srec);
                Slog.w(str, stringBuilder.toString());
                return false;
            }
            if (!task.affinity.equals(((TaskRecord) this.mTaskHistory.get(taskIdx)).affinity)) {
                return true;
            }
        }
        return false;
    }

    final boolean navigateUpToLocked(ActivityRecord srec, Intent destIntent, int resultCode, Intent resultData) {
        ActivityRecord activityRecord = srec;
        Intent intent = destIntent;
        TaskRecord task = srec.getTask();
        ArrayList<ActivityRecord> activities = task.mActivities;
        int start = activities.indexOf(activityRecord);
        if (!this.mTaskHistory.contains(task) || start < 0) {
            return false;
        }
        ComponentName dest;
        long origId;
        int finishTo;
        int finishTo2 = start - 1;
        ActivityRecord parent = finishTo2 < 0 ? null : (ActivityRecord) activities.get(finishTo2);
        boolean foundParentInTask = false;
        ComponentName dest2 = destIntent.getComponent();
        if (start > 0 && dest2 != null) {
            for (int i = finishTo2; i >= 0; i--) {
                ActivityRecord r = (ActivityRecord) activities.get(i);
                if (r.info.packageName.equals(dest2.getPackageName()) && r.info.name.equals(dest2.getClassName())) {
                    finishTo2 = i;
                    parent = r;
                    foundParentInTask = true;
                    break;
                }
            }
        }
        int finishTo3 = finishTo2;
        ActivityRecord parent2 = parent;
        boolean foundParentInTask2 = foundParentInTask;
        IActivityController controller = this.mService.mController;
        if (controller != null) {
            parent = topRunningActivityLocked(activityRecord.appToken, 0);
            if (parent != null) {
                foundParentInTask = true;
                try {
                    foundParentInTask = controller.activityResuming(parent.packageName);
                } catch (RemoteException e) {
                    this.mService.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                }
                if (!foundParentInTask) {
                    return false;
                }
            }
        }
        long origId2 = Binder.clearCallingIdentity();
        int resultCode2 = resultCode;
        Intent resultData2 = resultData;
        finishTo2 = start;
        while (finishTo2 > finishTo3) {
            parent = (ActivityRecord) activities.get(finishTo2);
            dest = dest2;
            origId = origId2;
            IActivityController controller2 = controller;
            finishTo = finishTo3;
            requestFinishActivityLocked(parent.appToken, resultCode2, resultData2, "navigate-up", 1);
            resultCode2 = 0;
            resultData2 = null;
            finishTo2--;
            origId2 = origId;
            controller = controller2;
            finishTo3 = finishTo;
            dest2 = dest;
        }
        finishTo = finishTo3;
        dest = dest2;
        origId = origId2;
        if (parent2 != null && foundParentInTask2) {
            int i2;
            finishTo3 = parent2.info.launchMode;
            int destIntentFlags = destIntent.getFlags();
            if (!(finishTo3 == 3 || finishTo3 == 2)) {
                boolean z = true;
                if (finishTo3 != 1) {
                    if ((destIntentFlags & 67108864) != 0) {
                        i2 = finishTo3;
                        parent2.deliverNewIntentLocked(activityRecord.info.applicationInfo.uid, intent, activityRecord.packageName);
                    } else {
                        RemoteException e2;
                        try {
                            if (this.mService.getActivityStartController().obtainStarter(intent, "navigateUpTo").setCaller(activityRecord.app.thread).setActivityInfo(AppGlobals.getPackageManager().getActivityInfo(destIntent.getComponent(), 1024, activityRecord.userId)).setResultTo(parent2.appToken).setCallingPid(-1).setCallingUid(parent2.launchedFromUid).setCallingPackage(parent2.launchedFromPackage).setRealCallingPid(-1).setRealCallingUid(parent2.launchedFromUid).setComponentSpecified(true).execute() != 0) {
                                z = false;
                            }
                            e2 = z;
                        } catch (RemoteException e3) {
                            e2 = null;
                        }
                        foundParentInTask2 = e2;
                        requestFinishActivityLocked(parent2.appToken, resultCode2, resultData2, "navigate-top", 1);
                    }
                }
            }
            i2 = finishTo3;
            parent2.deliverNewIntentLocked(activityRecord.info.applicationInfo.uid, intent, activityRecord.packageName);
        }
        Binder.restoreCallingIdentity(origId);
        return foundParentInTask2;
    }

    void onActivityRemovedFromStack(ActivityRecord r) {
        removeTimeoutsForActivityLocked(r);
        if (this.mResumedActivity != null && this.mResumedActivity == r) {
            setResumedActivity(null, "onActivityRemovedFromStack");
        }
        if (this.mPausingActivity != null && this.mPausingActivity == r) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remove the pausingActivity ");
            stringBuilder.append(this.mPausingActivity);
            stringBuilder.append(" in stack ");
            stringBuilder.append(this.mStackId);
            Flog.i(101, stringBuilder.toString());
            this.mPausingActivity = null;
        }
    }

    void onActivityAddedToStack(ActivityRecord r) {
        if (r.getState() == ActivityState.RESUMED) {
            setResumedActivity(r, "onActivityAddedToStack");
        }
    }

    private void cleanUpActivityLocked(ActivityRecord r, boolean cleanServices, boolean setState) {
        onActivityRemovedFromStack(r);
        r.deferRelaunchUntilPaused = false;
        r.frozenBeforeDestroy = false;
        if (setState) {
            String str;
            StringBuilder stringBuilder;
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Moving to DESTROYED: ");
                stringBuilder.append(r);
                stringBuilder.append(" (cleaning up)");
                Slog.v(str, stringBuilder.toString());
            }
            r.setState(ActivityState.DESTROYED, "cleanupActivityLocked");
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Clearing app during cleanUp for activity ");
                stringBuilder.append(r);
                Slog.v(str, stringBuilder.toString());
            }
            r.app = null;
        }
        this.mStackSupervisor.cleanupActivity(r);
        if (r.finishing && r.pendingResults != null) {
            Iterator it = r.pendingResults.iterator();
            while (it.hasNext()) {
                PendingIntentRecord rec = (PendingIntentRecord) ((WeakReference) it.next()).get();
                if (rec != null) {
                    this.mService.cancelIntentSenderLocked(rec, false);
                }
            }
            r.pendingResults = null;
        }
        if (cleanServices) {
            cleanUpActivityServicesLocked(r);
        }
        removeTimeoutsForActivityLocked(r);
        this.mWindowManager.notifyAppRelaunchesCleared(r.appToken);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord r) {
        if (r != null) {
            this.mStackSupervisor.removeTimeoutsForActivityLocked(r);
            this.mHandler.removeMessages(101, r);
            this.mHandler.removeMessages(104, r);
            this.mHandler.removeMessages(102, r);
            r.finishLaunchTickingLocked();
        }
    }

    private void removeActivityFromHistoryLocked(ActivityRecord r, String reason) {
        String str;
        StringBuilder stringBuilder;
        finishActivityResultsLocked(r, 0, null);
        r.makeFinishingLocked();
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Removing activity ");
            stringBuilder.append(r);
            stringBuilder.append(" from stack callers=");
            stringBuilder.append(Debug.getCallers(5));
            Slog.i(str, stringBuilder.toString());
        }
        r.takeFromHistory();
        removeTimeoutsForActivityLocked(r);
        if (ActivityManagerDebugConfig.DEBUG_STATES || HwSlog.HW_DEBUG_STATES) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Moving to DESTROYED: ");
            stringBuilder.append(r);
            stringBuilder.append(" (removed from history)");
            Slog.v(str, stringBuilder.toString());
        }
        r.setState(ActivityState.DESTROYED, "removeActivityFromHistoryLocked");
        if (ActivityManagerDebugConfig.DEBUG_APP) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Clearing app during remove for activity ");
            stringBuilder.append(r);
            Slog.v(str, stringBuilder.toString());
        }
        r.app = null;
        r.removeWindowContainer();
        TaskRecord task = r.getTask();
        boolean lastActivity = task != null ? task.removeActivity(r) : false;
        boolean onlyHasTaskOverlays = task != null ? task.onlyHasTaskOverlayActivities(false) : false;
        if (lastActivity || onlyHasTaskOverlays) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("removeActivityFromHistoryLocked: last activity removed from ");
                stringBuilder2.append(this);
                stringBuilder2.append(" onlyHasTaskOverlays=");
                stringBuilder2.append(onlyHasTaskOverlays);
                Slog.i(str2, stringBuilder2.toString());
            }
            if (onlyHasTaskOverlays) {
                this.mStackSupervisor.removeTaskByIdLocked(task.taskId, false, false, true, reason);
            }
            if (lastActivity) {
                removeTask(task, reason, 0);
            }
        }
        cleanUpActivityServicesLocked(r);
        r.removeUriPermissionsLocked();
    }

    private void cleanUpActivityServicesLocked(ActivityRecord r) {
        if (r.connections != null) {
            Iterator<ConnectionRecord> it = r.connections.iterator();
            while (it.hasNext()) {
                this.mService.mServices.removeConnectionLocked((ConnectionRecord) it.next(), null, r);
            }
            r.connections = null;
        }
    }

    final void scheduleDestroyActivities(ProcessRecord owner, String reason) {
        Message msg = this.mHandler.obtainMessage(105);
        msg.obj = new ScheduleDestroyArgs(owner, reason);
        this.mHandler.sendMessage(msg);
    }

    private void destroyActivitiesLocked(ProcessRecord owner, String reason) {
        boolean lastIsOpaque = false;
        boolean activityRemoved = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (!r.finishing) {
                    if (r.fullscreen) {
                        lastIsOpaque = true;
                    }
                    if ((owner == null || r.app == owner) && lastIsOpaque && r.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                            String str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Destroying ");
                            stringBuilder.append(r);
                            stringBuilder.append(" in state ");
                            stringBuilder.append(r.getState());
                            stringBuilder.append(" resumed=");
                            stringBuilder.append(this.mResumedActivity);
                            stringBuilder.append(" pausing=");
                            stringBuilder.append(this.mPausingActivity);
                            stringBuilder.append(" for reason ");
                            stringBuilder.append(reason);
                            Slog.v(str, stringBuilder.toString());
                        }
                        if (destroyActivityLocked(r, true, reason)) {
                            activityRemoved = true;
                        }
                    }
                }
            }
        }
        if (activityRemoved) {
            this.mStackSupervisor.mActivityLaunchTrack = "destroyedAtivityRemoved";
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    final boolean safelyDestroyActivityLocked(ActivityRecord r, String reason) {
        if (!r.isDestroyable()) {
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Destroying ");
            stringBuilder.append(r);
            stringBuilder.append(" in state ");
            stringBuilder.append(r.getState());
            stringBuilder.append(" resumed=");
            stringBuilder.append(this.mResumedActivity);
            stringBuilder.append(" pausing=");
            stringBuilder.append(this.mPausingActivity);
            stringBuilder.append(" for reason ");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        return destroyActivityLocked(r, true, reason);
    }

    final int releaseSomeActivitiesLocked(ProcessRecord app, ArraySet<TaskRecord> tasks, String reason) {
        String str;
        StringBuilder stringBuilder;
        ProcessRecord processRecord = app;
        String str2 = reason;
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to release some activities in ");
            stringBuilder.append(processRecord);
            Slog.d(str, stringBuilder.toString());
        }
        int maxTasks = tasks.size() / 4;
        if (maxTasks < 1) {
            maxTasks = 1;
        }
        int numReleased = 0;
        int maxTasks2 = maxTasks;
        maxTasks = 0;
        while (maxTasks < this.mTaskHistory.size() && maxTasks2 > 0) {
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(maxTasks);
            if (tasks.contains(task)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    String str3 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Looking for activities to release in ");
                    stringBuilder2.append(task);
                    Slog.d(str3, stringBuilder2.toString());
                }
                ArrayList<ActivityRecord> activities = task.mActivities;
                int curNum = 0;
                int actNdx = 0;
                while (actNdx < activities.size()) {
                    ActivityRecord activity = (ActivityRecord) activities.get(actNdx);
                    if (activity.app == processRecord && activity.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                            String str4 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Destroying ");
                            stringBuilder3.append(activity);
                            stringBuilder3.append(" in state ");
                            stringBuilder3.append(activity.getState());
                            stringBuilder3.append(" resumed=");
                            stringBuilder3.append(this.mResumedActivity);
                            stringBuilder3.append(" pausing=");
                            stringBuilder3.append(this.mPausingActivity);
                            stringBuilder3.append(" for reason ");
                            stringBuilder3.append(str2);
                            Slog.v(str4, stringBuilder3.toString());
                        }
                        destroyActivityLocked(activity, true, str2);
                        if (activities.get(actNdx) != activity) {
                            actNdx--;
                        }
                        curNum++;
                    }
                    actNdx++;
                }
                if (curNum > 0) {
                    numReleased += curNum;
                    maxTasks2--;
                    if (this.mTaskHistory.get(maxTasks) != task) {
                        maxTasks--;
                    }
                }
            }
            maxTasks++;
        }
        ArraySet<TaskRecord> arraySet = tasks;
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Done releasing: did ");
            stringBuilder.append(numReleased);
            stringBuilder.append(" activities");
            Slog.d(str, stringBuilder.toString());
        }
        return numReleased;
    }

    final boolean destroyActivityLocked(ActivityRecord r, boolean removeFromApp, String reason) {
        String str;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing activity from ");
            stringBuilder.append(reason);
            stringBuilder.append(": token=");
            stringBuilder.append(r);
            stringBuilder.append(", app=");
            stringBuilder.append(r.app != null ? r.app.processName : "(null)");
            Slog.v(str, stringBuilder.toString());
        }
        if (r.isState(ActivityState.DESTROYING, ActivityState.DESTROYED)) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("activity ");
                stringBuilder2.append(r);
                stringBuilder2.append(" already destroying.skipping request with reason:");
                stringBuilder2.append(reason);
                Slog.v(str, stringBuilder2.toString());
            }
            return false;
        }
        StringBuilder stringBuilder3;
        r2 = new Object[5];
        boolean z = true;
        r2[1] = Integer.valueOf(System.identityHashCode(r));
        r2[2] = Integer.valueOf(r.getTask().taskId);
        r2[3] = r.shortComponentName;
        r2[4] = reason;
        EventLog.writeEvent(EventLogTags.AM_DESTROY_ACTIVITY, r2);
        this.mService.notifyActivityState(r, ActivityState.DESTROYED);
        boolean removedFromHistory = false;
        cleanUpActivityLocked(r, false, false);
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.i(ActivityManagerService.TAG, "Activity has been cleaned up!");
        }
        if (r.app != null) {
            this.mService.recognizeFakeActivity(r.shortComponentName, r.app.pid, r.app.uid);
        }
        if (r.app == null) {
            z = false;
        }
        boolean hadApp = z;
        StringBuilder stringBuilder4;
        if (hadApp) {
            String str2;
            StringBuilder stringBuilder5;
            if (removeFromApp) {
                r.app.activities.remove(r);
                if (this.mService.mHeavyWeightProcess == r.app && r.app.activities.size() <= 0) {
                    this.mService.mHeavyWeightProcess = null;
                    this.mService.mHandler.sendEmptyMessage(25);
                }
                if (r.app.activities.isEmpty()) {
                    this.mService.mServices.updateServiceConnectionActivitiesLocked(r.app);
                    this.mService.updateLruProcessLocked(r.app, false, null);
                    this.mService.updateOomAdjLocked();
                }
            }
            z = false;
            try {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Destroying: ");
                    stringBuilder5.append(r);
                    Slog.i(str2, stringBuilder5.toString());
                }
                this.mService.getLifecycleManager().scheduleTransaction(r.app.thread, r.appToken, DestroyActivityItem.obtain(r.finishing, r.configChangeFlags));
            } catch (Exception e) {
                if (r.finishing) {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(reason);
                    stringBuilder5.append(" exceptionInScheduleDestroy");
                    removeActivityFromHistoryLocked(r, stringBuilder5.toString());
                    removedFromHistory = true;
                    z = true;
                }
            }
            r.nowVisible = false;
            if (!r.finishing || skipDestroy) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Moving to DESTROYED: ");
                    stringBuilder5.append(r);
                    stringBuilder5.append(" (destroy skipped) in stack ");
                    stringBuilder5.append(this.mStackId);
                    Slog.v(str2, stringBuilder5.toString());
                }
                r.setState(ActivityState.DESTROYED, "destroyActivityLocked. not finishing or skipping destroy");
                if (ActivityManagerDebugConfig.DEBUG_APP) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Clearing app during destroy for activity ");
                    stringBuilder5.append(r);
                    Slog.v(str2, stringBuilder5.toString());
                }
                r.app = null;
            } else {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    String str3 = ActivityManagerService.TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Moving to DESTROYING: ");
                    stringBuilder4.append(r);
                    stringBuilder4.append(" (destroy requested) in stack ");
                    stringBuilder4.append(this.mStackId);
                    Slog.v(str3, stringBuilder4.toString());
                }
                r.setState(ActivityState.DESTROYING, "destroyActivityLocked. finishing and not skipping destroy");
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(102, r), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            }
        } else if (r.finishing) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(reason);
            stringBuilder3.append(" hadNoApp");
            removeActivityFromHistoryLocked(r, stringBuilder3.toString());
            removedFromHistory = true;
        } else {
            String str4;
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str4 = ActivityManagerService.TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Moving to DESTROYED: ");
                stringBuilder4.append(r);
                stringBuilder4.append(" (no app)");
                Slog.v(str4, stringBuilder4.toString());
            }
            r.setState(ActivityState.DESTROYED, "destroyActivityLocked. not finishing and had no app");
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                str4 = ActivityManagerService.TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Clearing app during destroy for activity ");
                stringBuilder4.append(r);
                Slog.v(str4, stringBuilder4.toString());
            }
            r.app = null;
        }
        r.configChangeFlags = 0;
        if (!this.mLRUActivities.remove(r) && hadApp) {
            String str5 = ActivityManagerService.TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Activity ");
            stringBuilder3.append(r);
            stringBuilder3.append(" being finished, but not in LRU list");
            Slog.w(str5, stringBuilder3.toString());
        }
        return removedFromHistory;
    }

    final void activityDestroyedLocked(IBinder token, String reason) {
        long origId = Binder.clearCallingIdentity();
        try {
            activityDestroyedLocked(ActivityRecord.forTokenLocked(token), reason);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    final void activityDestroyedLocked(ActivityRecord record, String reason) {
        if (record != null) {
            this.mHandler.removeMessages(102, record);
        }
        if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("activityDestroyedLocked: r=");
            stringBuilder.append(record);
            Slog.d(str, stringBuilder.toString());
        }
        if (isInStackLocked(record) != null && record.isState(ActivityState.DESTROYING, ActivityState.DESTROYED)) {
            cleanUpActivityLocked(record, true, false);
            removeActivityFromHistoryLocked(record, reason);
        }
        this.mStackSupervisor.mActivityLaunchTrack = "activityDestroyed";
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
    }

    private void removeHistoryRecordsForAppLocked(ArrayList<ActivityRecord> list, ProcessRecord app, String listName) {
        int i = list.size();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing app ");
            stringBuilder.append(app);
            stringBuilder.append(" from list ");
            stringBuilder.append(listName);
            stringBuilder.append(" with ");
            stringBuilder.append(i);
            stringBuilder.append(" entries");
            Slog.v(str, stringBuilder.toString());
        }
        while (i > 0) {
            i--;
            ActivityRecord r = (ActivityRecord) list.get(i);
            if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Record #");
                stringBuilder2.append(i);
                stringBuilder2.append(" ");
                stringBuilder2.append(r);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (r.app == app) {
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(ActivityManagerService.TAG, "---> REMOVING this entry!");
                }
                list.remove(i);
                removeTimeoutsForActivityLocked(r);
            }
        }
    }

    private boolean removeHistoryRecordsForAppLocked(ProcessRecord app) {
        ProcessRecord processRecord = app;
        removeHistoryRecordsForAppLocked(this.mLRUActivities, processRecord, "mLRUActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mStoppingActivities, processRecord, "mStoppingActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mGoingToSleepActivities, processRecord, "mGoingToSleepActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mActivitiesWaitingForVisibleActivity, processRecord, "mActivitiesWaitingForVisibleActivity");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mFinishingActivities, processRecord, "mFinishingActivities");
        boolean hasVisibleActivities = false;
        int i = numActivities();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing app ");
            stringBuilder.append(processRecord);
            stringBuilder.append(" from history with ");
            stringBuilder.append(i);
            stringBuilder.append(" entries");
            Slog.v(str, stringBuilder.toString());
        }
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            this.mTmpActivities.clear();
            this.mTmpActivities.addAll(activities);
            while (!this.mTmpActivities.isEmpty()) {
                int targetIndex = this.mTmpActivities.size() - 1;
                ActivityRecord r = (ActivityRecord) this.mTmpActivities.remove(targetIndex);
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Record #");
                    stringBuilder2.append(targetIndex);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(r);
                    stringBuilder2.append(": app=");
                    stringBuilder2.append(r.app);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (r.app == processRecord) {
                    boolean remove;
                    String str3;
                    StringBuilder stringBuilder3;
                    if (r.visible) {
                        hasVisibleActivities = true;
                    }
                    if ((!r.haveState && !r.stateNotNeeded) || r.finishing) {
                        remove = true;
                    } else if (!r.visible && r.launchCount > 2 && r.lastLaunchTime > SystemClock.uptimeMillis() - 60000) {
                        remove = true;
                    } else if (r.launchCount <= 5 || r.lastLaunchTime <= SystemClock.uptimeMillis() - TRANSLUCENT_CONVERSION_TIMEOUT) {
                        remove = false;
                    } else {
                        remove = true;
                        str3 = ActivityManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("too many launcher times, remove : ");
                        stringBuilder3.append(r);
                        Slog.v(str3, stringBuilder3.toString());
                    }
                    if (remove) {
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                            str3 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Removing activity ");
                            stringBuilder3.append(r);
                            stringBuilder3.append(" from stack at ");
                            stringBuilder3.append(i);
                            stringBuilder3.append(": haveState=");
                            stringBuilder3.append(r.haveState);
                            stringBuilder3.append(" stateNotNeeded=");
                            stringBuilder3.append(r.stateNotNeeded);
                            stringBuilder3.append(" finishing=");
                            stringBuilder3.append(r.finishing);
                            stringBuilder3.append(" state=");
                            stringBuilder3.append(r.getState());
                            stringBuilder3.append(" callers=");
                            stringBuilder3.append(Debug.getCallers(5));
                            Slog.i(str3, stringBuilder3.toString());
                        }
                        if (!r.finishing) {
                            str3 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Force removing ");
                            stringBuilder3.append(r);
                            stringBuilder3.append(": app died, no saved state");
                            Slog.w(str3, stringBuilder3.toString());
                            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, new Object[]{Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.getTask().taskId), r.shortComponentName, "proc died without state saved"});
                            if (r.getState() == ActivityState.RESUMED) {
                                this.mService.updateUsageStats(r, false);
                            }
                        }
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_ALL) {
                            Slog.v(ActivityManagerService.TAG, "Keeping entry, setting app to null");
                        }
                        if (ActivityManagerDebugConfig.DEBUG_APP) {
                            String str4 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Clearing app during removeHistory for activity ");
                            stringBuilder4.append(r);
                            Slog.v(str4, stringBuilder4.toString());
                        }
                        r.app = null;
                        r.nowVisible = r.visible;
                        if (!r.haveState) {
                            if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
                                String str5 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("App died, clearing saved state of ");
                                stringBuilder5.append(r);
                                Slog.i(str5, stringBuilder5.toString());
                            }
                            r.icicle = null;
                        }
                    }
                    cleanUpActivityLocked(r, true, true);
                    if (remove) {
                        removeActivityFromHistoryLocked(r, "appDied");
                    }
                }
            }
        }
        return hasVisibleActivities;
    }

    private void updateTransitLocked(int transit, ActivityOptions options) {
        if (options != null) {
            ActivityRecord r = topRunningActivityLocked();
            if (r == null || r.isState(ActivityState.RESUMED)) {
                ActivityOptions.abort(options);
            } else {
                r.updateOptionsLocked(options);
            }
        }
        this.mWindowManager.prepareAppTransition(transit, false);
    }

    private void updateTaskMovement(TaskRecord task, boolean toFront) {
        if (task.isPersistable) {
            task.mLastTimeMoved = System.currentTimeMillis();
            if (!toFront) {
                task.mLastTimeMoved *= -1;
            }
        }
        this.mStackSupervisor.invalidateTaskLayers();
    }

    void moveHomeStackTaskToTop() {
        if (isActivityTypeHome()) {
            int top = this.mTaskHistory.size() - 1;
            if (top >= 0) {
                TaskRecord task = (TaskRecord) this.mTaskHistory.get(top);
                if (ActivityManagerDebugConfig.DEBUG_TASKS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("moveHomeStackTaskToTop: moving ");
                    stringBuilder.append(task);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mTaskHistory.remove(top);
                this.mTaskHistory.add(top, task);
                updateTaskMovement(task, true);
                return;
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Calling moveHomeStackTaskToTop() on non-home stack: ");
        stringBuilder2.append(this);
        throw new IllegalStateException(stringBuilder2.toString());
    }

    protected void moveTaskToFrontLocked(TaskRecord tr, boolean noAnimation, ActivityOptions options, AppTimeTracker timeTracker, String reason) {
        TaskRecord taskRecord = tr;
        ActivityOptions activityOptions = options;
        AppTimeTracker appTimeTracker = timeTracker;
        String str = reason;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("moveTaskToFront: ");
        stringBuilder.append(taskRecord);
        stringBuilder.append(", reason: ");
        stringBuilder.append(str);
        Flog.i(101, stringBuilder.toString());
        ActivityStack topStack = getDisplay().getTopStack();
        ActivityRecord topActivity = topStack != null ? topStack.getTopActivity() : null;
        int numTasks = this.mTaskHistory.size();
        int index = this.mTaskHistory.indexOf(taskRecord);
        if (numTasks == 0 || index < 0) {
            if (noAnimation) {
                ActivityOptions.abort(options);
            } else {
                updateTransitLocked(10, activityOptions);
            }
            return;
        }
        if (appTimeTracker != null) {
            for (int i = taskRecord.mActivities.size() - 1; i >= 0; i--) {
                ((ActivityRecord) taskRecord.mActivities.get(i)).appTimeTracker = appTimeTracker;
            }
        }
        ActivityDisplay ad;
        try {
            getDisplay().deferUpdateImeTarget();
            insertTaskAtTop(taskRecord, null);
            ActivityRecord top = tr.getTopActivity();
            ActivityDisplay activityDisplay;
            String str2;
            StringBuilder stringBuilder2;
            if (top == null || !top.okToShowLocked()) {
                if (top != null) {
                    this.mStackSupervisor.mRecentTasks.add(top.getTask());
                }
                ActivityOptions.abort(options);
                activityDisplay = getDisplay();
                if (activityDisplay != null) {
                    activityDisplay.continueUpdateImeTarget();
                } else {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("activityDisplay is null for ");
                    stringBuilder2.append(this.mDisplayId);
                    Slog.e(str2, stringBuilder2.toString());
                    if (!(HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode() || this.mDisplayId == -1)) {
                        ad = this.mStackSupervisor.getActivityDisplayOrCreateLocked(this.mDisplayId);
                        if (ad != null) {
                            ad.continueUpdateImeTarget();
                        }
                    }
                }
                return;
            }
            ActivityRecord r = topRunningActivityLocked();
            this.mStackSupervisor.moveFocusableActivityStackToFrontLocked(r, str);
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                String str3 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Prepare to front transition: task=");
                stringBuilder2.append(taskRecord);
                Slog.v(str3, stringBuilder2.toString());
            }
            if (noAnimation) {
                this.mWindowManager.prepareAppTransition(0, false);
                if (r != null) {
                    this.mStackSupervisor.mNoAnimActivities.add(r);
                }
                ActivityOptions.abort(options);
            } else {
                updateTransitLocked(10, activityOptions);
            }
            if (canEnterPipOnTaskSwitch(topActivity, taskRecord, null, activityOptions)) {
                topActivity.supportsEnterPipOnTaskSwitch = true;
            }
            this.mStackSupervisor.mActivityLaunchTrack = "taskMove";
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            EventLog.writeEvent(EventLogTags.AM_TASK_TO_FRONT, new Object[]{Integer.valueOf(taskRecord.userId), Integer.valueOf(taskRecord.taskId)});
            this.mService.mTaskChangeNotificationController.notifyTaskMovedToFront(taskRecord.taskId);
            activityDisplay = getDisplay();
            if (activityDisplay != null) {
                activityDisplay.continueUpdateImeTarget();
            } else {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("activityDisplay is null for ");
                stringBuilder2.append(this.mDisplayId);
                Slog.e(str2, stringBuilder2.toString());
                if (!(HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode() || this.mDisplayId == -1)) {
                    ad = this.mStackSupervisor.getActivityDisplayOrCreateLocked(this.mDisplayId);
                    if (ad != null) {
                        ad.continueUpdateImeTarget();
                    }
                }
            }
        } catch (Throwable th) {
            ad = getDisplay();
            if (ad == null) {
                String str4 = ActivityManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("activityDisplay is null for ");
                stringBuilder3.append(this.mDisplayId);
                Slog.e(str4, stringBuilder3.toString());
                if (!(HwPCUtils.isPcCastModeInServer() || HwVRUtils.isVRMode() || this.mDisplayId == -1)) {
                    ActivityDisplay ad2 = this.mStackSupervisor.getActivityDisplayOrCreateLocked(this.mDisplayId);
                    if (ad2 != null) {
                        ad2.continueUpdateImeTarget();
                    }
                }
            } else {
                ad.continueUpdateImeTarget();
            }
        }
    }

    protected boolean moveTaskToBackLocked(int taskId) {
        TaskRecord tr = taskForIdLocked(taskId);
        String str;
        StringBuilder stringBuilder;
        if (tr == null) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("moveTaskToBack: bad taskId=");
            stringBuilder.append(taskId);
            Slog.i(str, stringBuilder.toString());
            return false;
        }
        str = ActivityManagerService.TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("moveTaskToBack: ");
        stringBuilder.append(tr);
        Slog.i(str, stringBuilder.toString());
        if (!this.mService.getLockTaskController().canMoveTaskToBack(tr)) {
            return false;
        }
        if (isTopStackOnDisplay() && this.mService.mController != null) {
            ActivityRecord next = topRunningActivityLocked(null, taskId);
            if (next == null) {
                next = topRunningActivityLocked(null, 0);
            }
            if (next != null) {
                boolean moveOK = true;
                try {
                    moveOK = this.mService.mController.activityResuming(next.packageName);
                } catch (RemoteException e) {
                    this.mService.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                }
                if (!moveOK) {
                    return false;
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Prepare to back transition: task=");
            stringBuilder2.append(taskId);
            Slog.v(str, stringBuilder2.toString());
        }
        this.mTaskHistory.remove(tr);
        this.mTaskHistory.add(0, tr);
        updateTaskMovement(tr, false);
        this.mWindowManager.prepareAppTransition(11, false);
        moveToBack("moveTaskToBackLocked", tr);
        if (inPinnedWindowingMode()) {
            this.mStackSupervisor.removeStack(this);
            return true;
        }
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return true;
    }

    static void logStartActivity(int tag, ActivityRecord r, TaskRecord task) {
        Uri data = r.intent.getData();
        String strData = data != null ? data.toSafeString() : null;
        EventLog.writeEvent(tag, new Object[]{Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName, r.intent.getAction(), r.intent.getType(), strData, Integer.valueOf(r.intent.getFlags())});
    }

    void ensureVisibleActivitiesConfigurationLocked(ActivityRecord start, boolean preserveWindow) {
        if (start != null && start.visible) {
            boolean behindFullscreen = false;
            boolean updatedConfig = false;
            for (int taskIndex = this.mTaskHistory.indexOf(start.getTask()); taskIndex >= 0; taskIndex--) {
                TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskIndex);
                ArrayList<ActivityRecord> activities = task.mActivities;
                int activityIndex = start.getTask() == task ? activities.indexOf(start) : activities.size() - 1;
                while (activityIndex >= 0) {
                    ActivityRecord r = (ActivityRecord) activities.get(activityIndex);
                    updatedConfig |= r.ensureActivityConfiguration(0, preserveWindow);
                    if (r.fullscreen) {
                        behindFullscreen = true;
                        break;
                    }
                    activityIndex--;
                }
                if (behindFullscreen) {
                    break;
                }
            }
            if (updatedConfig) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
        }
    }

    public void requestResize(Rect bounds) {
        this.mService.resizeStack(this.mStackId, bounds, true, false, false, -1);
    }

    void resize(Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds) {
        if (updateBoundsAllowed(bounds, tempTaskBounds, tempTaskInsetBounds)) {
            Rect taskBounds = tempTaskBounds != null ? tempTaskBounds : bounds;
            Rect insetBounds = tempTaskInsetBounds != null ? tempTaskInsetBounds : taskBounds;
            this.mTmpBounds.clear();
            this.mTmpInsetBounds.clear();
            synchronized (this.mWindowManager.getWindowManagerLock()) {
                for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
                    TaskRecord task = (TaskRecord) this.mTaskHistory.get(i);
                    if (task.isResizeable()) {
                        if (inFreeformWindowingMode() || HwPCUtils.isExtDynamicStack(this.mStackId)) {
                            this.mTmpRect2.set(task.getOverrideBounds());
                            fitWithinBounds(this.mTmpRect2, bounds);
                            task.updateOverrideConfiguration(this.mTmpRect2);
                        } else {
                            task.updateOverrideConfiguration(taskBounds, insetBounds);
                        }
                    }
                    this.mTmpBounds.put(task.taskId, task.getOverrideBounds());
                    if (tempTaskInsetBounds != null) {
                        this.mTmpInsetBounds.put(task.taskId, tempTaskInsetBounds);
                    }
                }
                this.mWindowContainerController.resize(bounds, this.mTmpBounds, this.mTmpInsetBounds);
                setBounds(bounds);
            }
        }
    }

    void onPipAnimationEndResize() {
        this.mWindowContainerController.onPipAnimationEndResize();
    }

    private static void fitWithinBounds(Rect bounds, Rect stackBounds) {
        if (stackBounds != null && !stackBounds.isEmpty() && !stackBounds.contains(bounds)) {
            int maxRight;
            int horizontalDiff;
            if (bounds.left < stackBounds.left || bounds.right > stackBounds.right) {
                maxRight = stackBounds.right - (stackBounds.width() / 3);
                horizontalDiff = stackBounds.left - bounds.left;
                if ((horizontalDiff < 0 && bounds.left >= maxRight) || bounds.left + horizontalDiff >= maxRight) {
                    horizontalDiff = maxRight - bounds.left;
                }
                bounds.left += horizontalDiff;
                bounds.right += horizontalDiff;
            }
            if (bounds.top < stackBounds.top || bounds.bottom > stackBounds.bottom) {
                maxRight = stackBounds.bottom - (stackBounds.height() / 3);
                horizontalDiff = stackBounds.top - bounds.top;
                if ((horizontalDiff < 0 && bounds.top >= maxRight) || bounds.top + horizontalDiff >= maxRight) {
                    horizontalDiff = maxRight - bounds.top;
                }
                bounds.top += horizontalDiff;
                bounds.bottom += horizontalDiff;
            }
        }
    }

    boolean willActivityBeVisibleLocked(IBinder token) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if (r.appToken == token) {
                    return true;
                }
                if (r.fullscreen && !r.finishing) {
                    return false;
                }
            }
        }
        ActivityRecord r2 = ActivityRecord.forTokenLocked(token);
        if (r2 == null) {
            return false;
        }
        if (r2.finishing) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("willActivityBeVisibleLocked: Returning false, would have returned true for r=");
            stringBuilder.append(r2);
            Slog.e(str, stringBuilder.toString());
        }
        return true ^ r2.finishing;
    }

    void closeSystemDialogsLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                if ((r.info.flags & 256) != 0) {
                    finishActivityLocked(r, 0, null, "close-sys", true);
                }
            }
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId) {
        String str = packageName;
        Set<String> set = filterByClasses;
        int i = userId;
        boolean didSomething = false;
        TaskRecord lastTask = null;
        ComponentName homeActivity = null;
        int taskNdx = this.mTaskHistory.size() - 1;
        while (true) {
            int taskNdx2 = taskNdx;
            if (taskNdx2 < 0) {
                return didSomething;
            }
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx2)).mActivities;
            this.mTmpActivities.clear();
            this.mTmpActivities.addAll(activities);
            while (!this.mTmpActivities.isEmpty()) {
                boolean z = false;
                ActivityRecord r = (ActivityRecord) this.mTmpActivities.remove(0);
                if ((r.packageName.equals(str) && (set == null || set.contains(r.realActivity.getClassName()))) || (str == null && r.userId == i)) {
                    z = true;
                }
                boolean sameComponent = z;
                if ((i == -1 || r.userId == i) && ((sameComponent || r.getTask() == lastTask) && (r.app == null || evenPersistent || !r.app.persistent))) {
                    if (doit) {
                        if (r.isActivityTypeHome()) {
                            if (homeActivity == null || !homeActivity.equals(r.realActivity)) {
                                homeActivity = r.realActivity;
                            } else {
                                String str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Skip force-stop again ");
                                stringBuilder.append(r);
                                Slog.i(str2, stringBuilder.toString());
                            }
                        }
                        ComponentName homeActivity2 = homeActivity;
                        String str3 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  finishDisabledPackageActivitiesLocked Force finishing activity ");
                        stringBuilder2.append(r);
                        Slog.i(str3, stringBuilder2.toString());
                        if (sameComponent) {
                            if (r.app != null) {
                                r.app.removed = true;
                            }
                            r.app = null;
                        }
                        TaskRecord lastTask2 = r.getTask();
                        finishActivityLocked(r, 0, null, "force-stop", true);
                        homeActivity = homeActivity2;
                        didSomething = true;
                        lastTask = lastTask2;
                    } else if (!r.finishing) {
                        return true;
                    }
                }
            }
            taskNdx = taskNdx2 - 1;
        }
    }

    void getRunningTasks(List<TaskRecord> tasksOut, @ActivityType int ignoreActivityType, @WindowingMode int ignoreWindowingMode, int callingUid, boolean allowed) {
        boolean focusedStack = this.mStackSupervisor.getFocusedStack() == this;
        boolean topTask = true;
        int taskNdx = this.mTaskHistory.size() - 1;
        while (true) {
            int taskNdx2 = taskNdx;
            if (taskNdx2 >= 0) {
                TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx2);
                if (task.getTopActivity() != null && ((allowed || task.isActivityTypeHome() || task.effectiveUid == callingUid) && ((ignoreActivityType == 0 || task.getActivityType() != ignoreActivityType) && (ignoreWindowingMode == 0 || task.getWindowingMode() != ignoreWindowingMode)))) {
                    if (focusedStack && topTask) {
                        task.lastActiveTime = SystemClock.elapsedRealtime();
                        topTask = false;
                    }
                    tasksOut.add(task);
                }
                taskNdx = taskNdx2 - 1;
            } else {
                return;
            }
        }
    }

    void unhandledBackLocked() {
        int top = this.mTaskHistory.size() - 1;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Performing unhandledBack(): top activity at ");
            stringBuilder.append(top);
            Slog.d(str, stringBuilder.toString());
        }
        if (top >= 0) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(top)).mActivities;
            int activityTop = activities.size() - 1;
            if (activityTop >= 0) {
                finishActivityLocked((ActivityRecord) activities.get(activityTop), 0, null, "unhandled-back", true);
            }
        }
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        if (this.mPausingActivity != null && this.mPausingActivity.app == app) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("App died while pausing: ");
                stringBuilder.append(this.mPausingActivity);
                Slog.v(str, stringBuilder.toString());
            }
            this.mPausingActivity = null;
        }
        if (this.mLastPausedActivity != null && this.mLastPausedActivity.app == app) {
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        }
        return removeHistoryRecordsForAppLocked(app);
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = null;
                if (activityNdx < activities.size()) {
                    r = (ActivityRecord) activities.get(activityNdx);
                }
                if (r != null && r.app == app) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  handleAppCrashLocked Force finishing activity ");
                    stringBuilder.append(r.intent.getComponent().flattenToShortString());
                    Slog.w(str, stringBuilder.toString());
                    r.app = null;
                    this.mWindowManager.prepareAppTransition(26, false);
                    finishCurrentActivityLocked(r, 0, false, "handleAppCrashedLocked");
                }
            }
        }
    }

    boolean dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage, boolean needSep) {
        PrintWriter printWriter = pw;
        if (this.mTaskHistory.isEmpty()) {
            return false;
        }
        String prefix = "    ";
        int taskNdx = this.mTaskHistory.size() - 1;
        while (true) {
            int taskNdx2 = taskNdx;
            if (taskNdx2 < 0) {
                return true;
            }
            TaskRecord task = (TaskRecord) this.mTaskHistory.get(taskNdx2);
            if (needSep) {
                printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    Task id #");
            stringBuilder.append(task.taskId);
            printWriter.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mBounds=");
            stringBuilder.append(task.getOverrideBounds());
            printWriter.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mMinWidth=");
            stringBuilder.append(task.mMinWidth);
            printWriter.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mMinHeight=");
            stringBuilder.append(task.mMinHeight);
            printWriter.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mLastNonFullscreenBounds=");
            stringBuilder.append(task.mLastNonFullscreenBounds);
            printWriter.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    * ");
            stringBuilder.append(task);
            printWriter.println(stringBuilder.toString());
            task.dump(printWriter, "      ");
            int taskNdx3 = taskNdx2;
            ActivityStackSupervisor.dumpHistoryList(fd, printWriter, ((TaskRecord) this.mTaskHistory.get(taskNdx2)).mActivities, "    ", "Hist", true, dumpAll ^ 1, dumpClient, dumpPackage, false, null, task);
            taskNdx = taskNdx3 - 1;
        }
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name) {
        ArrayList<ActivityRecord> activities = new ArrayList();
        int taskNdx;
        if ("all".equals(name)) {
            for (taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
                activities.addAll(((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities);
            }
        } else if ("top".equals(name)) {
            taskNdx = this.mTaskHistory.size() - 1;
            if (taskNdx >= 0) {
                ArrayList<ActivityRecord> list = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
                int listTop = list.size() - 1;
                if (listTop >= 0) {
                    activities.add((ActivityRecord) list.get(listTop));
                }
            }
        } else {
            ItemMatcher matcher = new ItemMatcher();
            matcher.build(name);
            for (int taskNdx2 = this.mTaskHistory.size() - 1; taskNdx2 >= 0; taskNdx2--) {
                Iterator it = ((TaskRecord) this.mTaskHistory.get(taskNdx2)).mActivities.iterator();
                while (it.hasNext()) {
                    ActivityRecord r1 = (ActivityRecord) it.next();
                    if (matcher.match(r1, r1.intent.getComponent())) {
                        activities.add(r1);
                    }
                }
            }
        }
        return activities;
    }

    ActivityRecord restartPackage(String packageName) {
        ActivityRecord starting = topRunningActivityLocked();
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = ((TaskRecord) this.mTaskHistory.get(taskNdx)).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord a = (ActivityRecord) activities.get(activityNdx);
                if (a.info.packageName.equals(packageName)) {
                    a.forceNewConfig = true;
                    if (starting != null && a == starting && a.visible) {
                        a.startFreezingScreenLocked(starting.app, 256);
                    }
                }
            }
        }
        return starting;
    }

    void removeTask(TaskRecord task, String reason, int mode) {
        Iterator it = task.mActivities.iterator();
        while (it.hasNext()) {
            onActivityRemovedFromStack((ActivityRecord) it.next());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Task removed: ");
        stringBuilder.append(task);
        stringBuilder.append(", reason: ");
        stringBuilder.append(reason);
        stringBuilder.append(", mode: ");
        stringBuilder.append(mode);
        Flog.i(101, stringBuilder.toString());
        boolean isVoiceSession = false;
        if (this.mTaskHistory.remove(task)) {
            EventLog.writeEvent(EventLogTags.AM_REMOVE_TASK, new Object[]{Integer.valueOf(task.taskId), Integer.valueOf(getStackId())});
        }
        removeActivitiesFromLRUListLocked(task);
        updateTaskMovement(task, true);
        if (mode == 0 && task.mActivities.isEmpty()) {
            if (task.voiceSession != null) {
                isVoiceSession = true;
            }
            if (isVoiceSession) {
                try {
                    task.voiceSession.taskFinished(task.intent, task.taskId);
                } catch (RemoteException e) {
                }
            }
            if (task.autoRemoveFromRecents() || isVoiceSession) {
                this.mStackSupervisor.mRecentTasks.remove(task);
            }
            task.removeWindowContainer();
        }
        if (this.mTaskHistory.isEmpty() && !reason.contains("swapDockedAndFullscreenStack")) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("removeTask: removing stack=");
            stringBuilder2.append(this);
            Flog.i(101, stringBuilder2.toString());
            if (isOnHomeDisplay() && mode != 2 && this.mStackSupervisor.isFocusedStack(this)) {
                String myReason = new StringBuilder();
                myReason.append(reason);
                myReason.append(" leftTaskHistoryEmpty");
                myReason = myReason.toString();
                if (!(inMultiWindowMode() && adjustFocusToNextFocusableStack(myReason))) {
                    this.mStackSupervisor.moveHomeStackToFront(myReason);
                }
            }
            if (isAttached()) {
                getDisplay().positionChildAtBottom(this);
            }
            if (!isActivityTypeHome()) {
                remove();
            }
        }
        task.setStack(null);
        if (inPinnedWindowingMode()) {
            this.mService.mTaskChangeNotificationController.notifyActivityUnpinned();
            LogPower.push(NetdResponseCode.ClatdStatusResult);
        }
    }

    TaskRecord createTaskRecord(int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean toTop) {
        return createTaskRecord(taskId, info, intent, voiceSession, voiceInteractor, toTop, null, null, null);
    }

    TaskRecord createTaskRecord(int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean toTop, ActivityRecord activity, ActivityRecord source, ActivityOptions options) {
        ActivityInfo activityInfo = info;
        boolean z = toTop;
        TaskRecord task = TaskRecord.create(this.mService, taskId, activityInfo, intent, voiceSession, voiceInteractor);
        addTask(task, z, "createTaskRecord");
        boolean z2 = false;
        boolean isLockscreenShown = this.mService.mStackSupervisor.getKeyguardController().isKeyguardOrAodShowing(this.mDisplayId != -1 ? this.mDisplayId : 0);
        if (!(this.mStackSupervisor.getLaunchParamsController().layoutTask(task, activityInfo.windowLayout, activity, source, options) || matchParentBounds() || !task.isResizeable() || isLockscreenShown)) {
            task.updateOverrideConfiguration(getOverrideBounds());
        }
        if ((activityInfo.flags & 1024) != 0) {
            z2 = true;
        }
        task.createWindowContainer(z, z2);
        return task;
    }

    ArrayList<TaskRecord> getAllTasks() {
        return new ArrayList(this.mTaskHistory);
    }

    void addTask(TaskRecord task, boolean toTop, String reason) {
        addTask(task, toTop ? HwBootFail.STAGE_BOOT_SUCCESS : 0, true, reason);
        if (toTop) {
            this.mWindowContainerController.positionChildAtTop(task.getWindowContainerController(), true);
        }
    }

    void addTask(TaskRecord task, int position, boolean schedulePictureInPictureModeChange, String reason) {
        this.mTaskHistory.remove(task);
        position = getAdjustedPositionForTask(task, position, null);
        boolean toTop = position >= this.mTaskHistory.size();
        ActivityStack prevStack = preAddTask(task, reason, toTop);
        this.mTaskHistory.add(position, task);
        task.setStack(this);
        updateTaskMovement(task, toTop);
        postAddTask(task, prevStack, schedulePictureInPictureModeChange);
    }

    void positionChildAt(TaskRecord task, int index) {
        if (task.getStack() == this) {
            task.updateOverrideConfigurationForStack(this);
            ActivityRecord topRunningActivity = task.topRunningActivityLocked();
            boolean wasResumed = topRunningActivity == task.getStack().mResumedActivity;
            insertTaskAtPosition(task, index);
            task.setStack(this);
            postAddTask(task, null, true);
            if (wasResumed) {
                if (this.mResumedActivity != null) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mResumedActivity was already set when moving mResumedActivity from other stack to this stack mResumedActivity=");
                    stringBuilder.append(this.mResumedActivity);
                    stringBuilder.append(" other mResumedActivity=");
                    stringBuilder.append(topRunningActivity);
                    Log.wtf(str, stringBuilder.toString());
                }
                topRunningActivity.setState(ActivityState.RESUMED, "positionChildAt");
            }
            ensureActivitiesVisibleLocked(null, 0, false);
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("AS.positionChildAt: task=");
        stringBuilder2.append(task);
        stringBuilder2.append(" is not a child of stack=");
        stringBuilder2.append(this);
        stringBuilder2.append(" current parent=");
        stringBuilder2.append(task.getStack());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private ActivityStack preAddTask(TaskRecord task, String reason, boolean toTop) {
        ActivityStack prevStack = task.getStack();
        if (!(prevStack == null || prevStack == this)) {
            prevStack.removeTask(task, reason, toTop ? 2 : 1);
        }
        return prevStack;
    }

    private void postAddTask(TaskRecord task, ActivityStack prevStack, boolean schedulePictureInPictureModeChange) {
        if (schedulePictureInPictureModeChange && prevStack != null) {
            this.mStackSupervisor.scheduleUpdatePictureInPictureModeIfNeeded(task, prevStack);
        } else if (task.voiceSession != null) {
            try {
                task.voiceSession.taskStarted(task.intent, task.taskId);
            } catch (RemoteException e) {
            }
        }
    }

    void moveToFrontAndResumeStateIfNeeded(ActivityRecord r, boolean moveToFront, boolean setResume, boolean setPause, String reason) {
        if (moveToFront) {
            if (setResume) {
                r.setState(ActivityState.RESUMED, "moveToFrontAndResumeStateIfNeeded");
                updateLRUListLocked(r);
            }
            if (setPause) {
                this.mPausingActivity = r;
                schedulePauseTimeout(r);
            }
            moveToFront(reason);
        }
    }

    public int getStackId() {
        return this.mStackId;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActivityStack{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" stackId=");
        stringBuilder.append(this.mStackId);
        stringBuilder.append(" type=");
        stringBuilder.append(WindowConfiguration.activityTypeToString(getActivityType()));
        stringBuilder.append(" mode=");
        stringBuilder.append(WindowConfiguration.windowingModeToString(getWindowingMode()));
        stringBuilder.append(" visible=");
        stringBuilder.append(shouldBeVisible(null));
        stringBuilder.append(" translucent=");
        stringBuilder.append(isStackTranslucent(null));
        stringBuilder.append(", ");
        stringBuilder.append(this.mTaskHistory.size());
        stringBuilder.append(" tasks}");
        return stringBuilder.toString();
    }

    public String toShortString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActivityStack{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" stackId=");
        stringBuilder.append(this.mStackId);
        stringBuilder.append(" type=");
        stringBuilder.append(WindowConfiguration.activityTypeToString(getActivityType()));
        stringBuilder.append(" mode=");
        stringBuilder.append(WindowConfiguration.windowingModeToString(getWindowingMode()));
        stringBuilder.append(", ");
        stringBuilder.append(this.mTaskHistory.size());
        stringBuilder.append(" tasks}");
        return stringBuilder.toString();
    }

    void onLockTaskPackagesUpdated() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ((TaskRecord) this.mTaskHistory.get(taskNdx)).setLockTaskAuth();
        }
    }

    void executeAppTransition(ActivityOptions options) {
        this.mWindowManager.executeAppTransition();
        ActivityOptions.abort(options);
    }

    boolean shouldSleepActivities() {
        ActivityDisplay display = getDisplay();
        if (this.mStackSupervisor.getFocusedStack() == this && this.mStackSupervisor.getKeyguardController().isKeyguardGoingAway()) {
            if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                Flog.i(107, "Skip sleeping activities for keyguard is in the process of going away");
            }
            return false;
        }
        return display != null ? display.isSleeping() : this.mService.isSleepingLocked();
    }

    boolean shouldSleepOrShutDownActivities() {
        return shouldSleepActivities() || this.mService.isShuttingDownLocked();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, false);
        proto.write(1120986464258L, this.mStackId);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ((TaskRecord) this.mTaskHistory.get(taskNdx)).writeToProto(proto, 2246267895811L);
        }
        if (this.mResumedActivity != null) {
            this.mResumedActivity.writeIdentifierToProto(proto, 1146756268036L);
        }
        proto.write(1120986464261L, this.mDisplayId);
        if (!matchParentBounds()) {
            getOverrideBounds().writeToProto(proto, 1146756268039L);
        }
        proto.write(1133871366150L, matchParentBounds());
        proto.end(token);
    }

    protected void resetOtherStacksVisible(boolean visible) {
    }
}
