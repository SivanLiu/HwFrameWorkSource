package com.android.server.am;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityManagerInternal.SleepToken;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.HwCustNonHardwareAcceleratedPackagesManager;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.app.WindowConfiguration;
import android.app.WindowConfiguration.ActivityType;
import android.app.WindowConfiguration.WindowingMode;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.LaunchActivityItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.display.DisplayManagerInternal;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.rms.HwSysResource;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.IntArray;
import android.util.Jlog;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.TransferPipe;
import com.android.internal.os.logging.MetricsLoggerWrapper;
import com.android.internal.util.ArrayUtils;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SmartShrinker;
import com.android.server.UiThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.PinnedStackWindowController;
import com.android.server.wm.WindowManagerService;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.am.IHwActivityStackSupervisorEx;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ActivityStackSupervisor extends AbsActivityStackSupervisor implements DisplayListener, Callbacks {
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION = new ArrayMap();
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    static final boolean CREATE_IF_NEEDED = true;
    static final boolean DEFER_RESUME = true;
    static final int HANDLE_DISPLAY_ADDED = 105;
    static final int HANDLE_DISPLAY_CHANGED = 106;
    static final int HANDLE_DISPLAY_REMOVED = 107;
    static final int IDLE_NOW_MSG = 101;
    static final int IDLE_TIMEOUT = 10000;
    static final int IDLE_TIMEOUT_MSG = 100;
    static final boolean IS_DEBUG_VERSION;
    static final int LAUNCH_TASK_BEHIND_COMPLETE = 112;
    static final int LAUNCH_TIMEOUT = 10000;
    static final int LAUNCH_TIMEOUT_MSG = 104;
    static final int MATCH_TASK_IN_STACKS_ONLY = 0;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS = 1;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS_AND_RESTORE = 2;
    private static final int MAX_TASK_IDS_PER_USER = 100000;
    static final boolean ON_TOP = true;
    static final boolean PAUSE_IMMEDIATELY = true;
    static final boolean PRESERVE_WINDOWS = true;
    static final boolean REMOVE_FROM_RECENTS = true;
    static final int REPORT_MULTI_WINDOW_MODE_CHANGED_MSG = 114;
    static final int REPORT_PIP_MODE_CHANGED_MSG = 115;
    static final int RESUME_TOP_ACTIVITY_MSG = 102;
    static final int SLEEP_TIMEOUT = 5000;
    static final int SLEEP_TIMEOUT_MSG = 103;
    private static final String TAG = "ActivityManager";
    private static final String TAG_FOCUS = "ActivityManager";
    private static final String TAG_IDLE = "ActivityManager";
    static final String TAG_KEYGUARD = "ActivityManager_keyguard";
    private static final String TAG_PAUSE = "ActivityManager";
    private static final String TAG_RECENTS = "ActivityManager";
    private static final String TAG_RELEASE = "ActivityManager";
    private static final String TAG_STACK = "ActivityManager";
    private static final String TAG_STATES = "ActivityManager";
    private static final String TAG_SWITCH = "ActivityManager";
    static final String TAG_TASKS = "ActivityManager";
    static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private static final String VIRTUAL_DISPLAY_BASE_NAME = "ActivityViewVirtualDisplay";
    boolean inResumeTopActivity;
    final ArrayList<ActivityRecord> mActivitiesWaitingForVisibleActivity = new ArrayList();
    protected final SparseArray<ActivityDisplay> mActivityDisplays = new SparseArray();
    String mActivityLaunchTrack = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private ActivityMetricsLogger mActivityMetricsLogger;
    private boolean mAllowDockedStackResize = true;
    private HwSysResource mAppResource;
    boolean mAppVisibilitiesChangedSinceLastPause;
    private final SparseIntArray mCurTaskIdForUser = new SparseIntArray(20);
    int mCurrentUser;
    int mDefaultMinSizeOfResizeableTask = -1;
    private int mDeferResumeCount;
    private final SparseArray<IntArray> mDisplayAccessUIDs = new SparseArray();
    DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    private boolean mDockedStackResizing;
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList();
    ActivityStack mFocusedStack;
    WakeLock mGoingToSleep;
    final ArrayList<ActivityRecord> mGoingToSleepActivities = new ArrayList();
    final ActivityStackSupervisorHandler mHandler;
    private boolean mHasPendingDockedBounds;
    ActivityStack mHomeStack;
    private IHwActivityStackSupervisorEx mHwActivityStackSupervisorEx;
    private boolean mInitialized;
    boolean mIsDockMinimized;
    private KeyguardController mKeyguardController;
    private ActivityStack mLastFocusedStack;
    private LaunchParamsController mLaunchParamsController;
    private LaunchTimeTracker mLaunchTimeTracker = new LaunchTimeTracker();
    WakeLock mLaunchingActivity;
    final Looper mLooper;
    final ArrayList<ActivityRecord> mMultiWindowModeChangedActivities = new ArrayList();
    final ArrayList<ActivityRecord> mNoAnimActivities = new ArrayList();
    private Rect mPendingDockedBounds;
    private Rect mPendingTempDockedTaskBounds;
    private Rect mPendingTempDockedTaskInsetBounds;
    private Rect mPendingTempOtherTaskBounds;
    private Rect mPendingTempOtherTaskInsetBounds;
    final ArrayList<ActivityRecord> mPipModeChangedActivities = new ArrayList();
    Rect mPipModeChangedTargetStackBounds;
    private boolean mPowerHintSent;
    private PowerManager mPowerManager;
    RecentTasks mRecentTasks;
    private final ArraySet<Integer> mResizingTasksDuringAnimation = new ArraySet();
    private RunningTasks mRunningTasks;
    final ActivityManagerService mService;
    final ArrayList<SleepToken> mSleepTokens = new ArrayList();
    final ArrayList<UserState> mStartingUsers = new ArrayList();
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList();
    private boolean mTaskLayersChanged = true;
    private final ArrayList<ActivityRecord> mTmpActivityList = new ArrayList();
    private final FindTaskResult mTmpFindTaskResult = new FindTaskResult();
    private final ActivityOptions mTmpOptions = ActivityOptions.makeBasic();
    private SparseIntArray mTmpOrderedDisplayIds = new SparseIntArray();
    boolean mUserLeaving = false;
    SparseIntArray mUserStackInFront = new SparseIntArray(2);
    final ArrayList<WaitResult> mWaitingActivityLaunched = new ArrayList();
    private final ArrayList<WaitInfo> mWaitingForActivityVisible = new ArrayList();
    WindowManagerService mWindowManager;
    private final Rect tempRect = new Rect();

    protected final class ActivityStackSupervisorHandler extends Handler {
        public ActivityStackSupervisorHandler(Looper looper) {
            super(looper);
        }

        void activityIdleInternal(ActivityRecord r, boolean processPausingActivities) {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.activityIdleInternalLocked(r != null ? r.appToken : null, true, processPausingActivities, null);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            ActivityRecord r;
            if (i == 112) {
                synchronized (ActivityStackSupervisor.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        r = ActivityRecord.forTokenLocked((IBinder) msg.obj);
                        if (r != null) {
                            ActivityStackSupervisor.this.handleLaunchTaskBehindCompleteLocked(r);
                        }
                    } finally {
                        while (true) {
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } else if (i != 10001) {
                String str;
                switch (i) {
                    case 100:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("handleMessage: IDLE_TIMEOUT_MSG: r=");
                            stringBuilder.append(msg.obj);
                            Slog.d(str, stringBuilder.toString());
                        }
                        activityIdleInternal((ActivityRecord) msg.obj, true);
                        return;
                    case 101:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("handleMessage: IDLE_NOW_MSG: r=");
                            stringBuilder2.append(msg.obj);
                            Slog.d(str, stringBuilder2.toString());
                        }
                        activityIdleInternal((ActivityRecord) msg.obj, false);
                        return;
                    case 102:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ActivityStackSupervisor.this.resumeFocusedStackTopActivityLocked();
                            } finally {
                                while (true) {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    break;
                                }
                            }
                        }
                        return;
                    case 103:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityStackSupervisor.this.mService.isSleepingOrShuttingDownLocked()) {
                                    Slog.w(ActivityManagerService.TAG, "Sleep timeout!  Sleeping now.");
                                    ActivityStackSupervisor.this.checkReadyForSleepLocked(false);
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
                        str = null;
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                r = ActivityStackSupervisor.this.mFocusedStack.topRunningActivityLocked();
                                if (r == null) {
                                    Slog.w(ActivityManagerService.TAG, "Launch timeout,null top activity");
                                } else {
                                    str = r.packageName;
                                }
                                if (ActivityStackSupervisor.this.mLaunchingActivity.isHeld()) {
                                    Slog.w(ActivityManagerService.TAG, "Launch timeout has expired, giving up wake lock!");
                                    ActivityStackSupervisor.this.mLaunchingActivity.release();
                                }
                            } finally {
                                while (true) {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    break;
                                }
                            }
                        }
                        IZrHung appBF = HwFrameworkFactory.getZrHung("appeye_bootfail");
                        if (appBF != null) {
                            ZrHungData arg = new ZrHungData();
                            arg.putString("packageName", str);
                            appBF.sendEvent(arg);
                            return;
                        }
                        return;
                    case 105:
                        ActivityStackSupervisor.this.handleDisplayAdded(msg.arg1);
                        return;
                    case 106:
                        ActivityStackSupervisor.this.handleDisplayChanged(msg.arg1);
                        return;
                    case 107:
                        ActivityStackSupervisor.this.handleDisplayRemoved(msg.arg1);
                        return;
                    default:
                        switch (i) {
                            case 114:
                                synchronized (ActivityStackSupervisor.this.mService) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        for (int i2 = ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.size() - 1; i2 >= 0; i2--) {
                                            ActivityRecord r2 = (ActivityRecord) ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.remove(i2);
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("schedule multiwindow mode change callback for ");
                                            stringBuilder3.append(r2);
                                            Flog.i(101, stringBuilder3.toString());
                                            r2.updateMultiWindowMode();
                                        }
                                    } finally {
                                        while (true) {
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            break;
                                        }
                                    }
                                }
                                return;
                            case 115:
                                synchronized (ActivityStackSupervisor.this.mService) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        int i3 = ActivityStackSupervisor.this.mPipModeChangedActivities.size() - 1;
                                        while (true) {
                                            int i4 = i3;
                                            if (i4 >= 0) {
                                                ((ActivityRecord) ActivityStackSupervisor.this.mPipModeChangedActivities.remove(i4)).updatePictureInPictureMode(ActivityStackSupervisor.this.mPipModeChangedTargetStackBounds, false);
                                                i3 = i4 - 1;
                                            }
                                        }
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
            } else {
                ActivityStackSupervisor.this.handlePCWindowStateChanged();
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AnyTaskForIdMatchTaskMode {
    }

    static class FindTaskResult {
        boolean matchedByRootAffinity;
        ActivityRecord r;

        FindTaskResult() {
        }
    }

    static class PendingActivityLaunch {
        final ProcessRecord callerApp;
        final ActivityRecord r;
        final ActivityRecord sourceRecord;
        final ActivityStack stack;
        final int startFlags;

        PendingActivityLaunch(ActivityRecord _r, ActivityRecord _sourceRecord, int _startFlags, ActivityStack _stack, ProcessRecord _callerApp) {
            this.r = _r;
            this.sourceRecord = _sourceRecord;
            this.startFlags = _startFlags;
            this.stack = _stack;
            this.callerApp = _callerApp;
        }

        void sendErrorResult(String message) {
            try {
                if (this.callerApp.thread != null) {
                    this.callerApp.thread.scheduleCrash(message);
                }
            } catch (RemoteException e) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception scheduling crash of failed activity launcher sourceRecord=");
                stringBuilder.append(this.sourceRecord);
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
    }

    private final class SleepTokenImpl extends SleepToken {
        private final long mAcquireTime = SystemClock.uptimeMillis();
        private final int mDisplayId;
        private final String mTag;

        public SleepTokenImpl(String tag, int displayId) {
            this.mTag = tag;
            this.mDisplayId = displayId;
        }

        public void release() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.removeSleepTokenLocked(this);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{\"");
            stringBuilder.append(this.mTag);
            stringBuilder.append("\", display ");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append(", acquire at ");
            stringBuilder.append(TimeUtils.formatUptime(this.mAcquireTime));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static class WaitInfo {
        private final WaitResult mResult;
        private final ComponentName mTargetComponent;

        public WaitInfo(ComponentName targetComponent, WaitResult result) {
            this.mTargetComponent = targetComponent;
            this.mResult = result;
        }

        public boolean matches(ComponentName targetComponent) {
            return this.mTargetComponent == null || this.mTargetComponent.equals(targetComponent);
        }

        public WaitResult getResult() {
            return this.mResult;
        }

        public ComponentName getComponent() {
            return this.mTargetComponent;
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("WaitInfo:");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  mTargetComponent=");
            stringBuilder.append(this.mTargetComponent);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  mResult=");
            pw.println(stringBuilder.toString());
            this.mResult.dump(pw, prefix);
        }
    }

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.IMAGE_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.VIDEO_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.intent.action.CALL", "android.permission.CALL_PHONE");
    }

    protected int getChildCount() {
        return this.mActivityDisplays.size();
    }

    protected ActivityDisplay getChildAt(int index) {
        return (ActivityDisplay) this.mActivityDisplays.valueAt(index);
    }

    protected ConfigurationContainer getParent() {
        return null;
    }

    Configuration getDisplayOverrideConfiguration(int displayId) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay != null) {
            return activityDisplay.getOverrideConfiguration();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No display found with id: ");
        stringBuilder.append(displayId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void setDisplayOverrideConfiguration(Configuration overrideConfiguration, int displayId) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay != null) {
            activityDisplay.onOverrideConfigurationChanged(overrideConfiguration);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No display found with id: ");
        stringBuilder.append(displayId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean canPlaceEntityOnDisplay(int displayId, boolean resizeable, int callingPid, int callingUid, ActivityInfo activityInfo) {
        if (displayId == 0) {
            return true;
        }
        if (!this.mService.mSupportsMultiDisplay) {
            return false;
        }
        if ((resizeable || displayConfigMatchesGlobal(displayId)) && isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, displayId, activityInfo)) {
            return true;
        }
        return false;
    }

    private boolean displayConfigMatchesGlobal(int displayId) {
        if (displayId == 0) {
            return true;
        }
        if (displayId == -1) {
            return false;
        }
        ActivityDisplay targetDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (targetDisplay != null) {
            return getConfiguration().equals(targetDisplay.getConfiguration());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No display found with id: ");
        stringBuilder.append(displayId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ActivityStackSupervisor(ActivityManagerService service, Looper looper) {
        this.mService = service;
        this.mLooper = looper;
        this.mHandler = new ActivityStackSupervisorHandler(looper);
        this.mHwActivityStackSupervisorEx = HwServiceExFactory.getHwActivityStackSupervisorEx();
    }

    public void initialize() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            this.mRunningTasks = createRunningTasks();
            this.mActivityMetricsLogger = new ActivityMetricsLogger(this, this.mService.mContext, this.mHandler.getLooper());
            this.mKeyguardController = new KeyguardController(this.mService, this);
            this.mLaunchParamsController = new LaunchParamsController(this.mService);
            this.mLaunchParamsController.registerDefaultModifiers(this);
        }
    }

    public ActivityMetricsLogger getActivityMetricsLogger() {
        return this.mActivityMetricsLogger;
    }

    LaunchTimeTracker getLaunchTimeTracker() {
        return this.mLaunchTimeTracker;
    }

    public KeyguardController getKeyguardController() {
        return this.mKeyguardController;
    }

    void setRecentTasks(RecentTasks recentTasks) {
        this.mRecentTasks = recentTasks;
        this.mRecentTasks.registerCallback(this);
    }

    @VisibleForTesting
    RunningTasks createRunningTasks() {
        return new RunningTasks();
    }

    void initPowerManagement() {
        this.mPowerManager = (PowerManager) this.mService.mContext.getSystemService("power");
        this.mGoingToSleep = this.mPowerManager.newWakeLock(1, "ActivityManager-Sleep");
        this.mLaunchingActivity = this.mPowerManager.newWakeLock(1, "*launch*");
        this.mLaunchingActivity.setReferenceCounted(false);
    }

    void setWindowManager(WindowManagerService wm) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mWindowManager = wm;
                getKeyguardController().setWindowManager(wm);
                this.mDisplayManager = (DisplayManager) this.mService.mContext.getSystemService("display");
                this.mDisplayManager.registerDisplayListener(this, null);
                this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
                Display[] displays = this.mDisplayManager.getDisplays();
                for (int displayNdx = displays.length - 1; displayNdx >= 0; displayNdx--) {
                    Display display = displays[displayNdx];
                    ActivityDisplay activityDisplay = new ActivityDisplay(this, display);
                    this.mActivityDisplays.put(display.getDisplayId(), activityDisplay);
                    calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
                }
                ActivityStack orCreateStack = getDefaultDisplay().getOrCreateStack(1, 2, true);
                this.mLastFocusedStack = orCreateStack;
                this.mFocusedStack = orCreateStack;
                this.mHomeStack = orCreateStack;
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    ActivityStack getFocusedStack() {
        return this.mFocusedStack;
    }

    boolean isFocusable(ConfigurationContainer container, boolean alwaysFocusable) {
        boolean z = false;
        if (container.inSplitScreenPrimaryWindowingMode() && this.mIsDockMinimized) {
            return false;
        }
        if (container.getWindowConfiguration().canReceiveKeys() || alwaysFocusable) {
            z = true;
        }
        return z;
    }

    ActivityStack getLastStack() {
        return this.mLastFocusedStack;
    }

    boolean isFocusedStack(ActivityStack stack) {
        return stack != null && stack == this.mFocusedStack;
    }

    void setFocusStackUnchecked(String reason, ActivityStack focusCandidate) {
        if (!focusCandidate.isFocusable()) {
            focusCandidate = getNextFocusableStackLocked(focusCandidate, false);
        }
        if (focusCandidate != this.mFocusedStack) {
            int i;
            this.mLastFocusedStack = this.mFocusedStack;
            this.mFocusedStack = focusCandidate;
            if (this.mLastFocusedStack != null && HwVRUtils.isVRDynamicStack(this.mLastFocusedStack.getStackId())) {
                this.mLastFocusedStack.makeStackVisible(false);
            }
            int i2 = this.mCurrentUser;
            int i3 = -1;
            if (this.mFocusedStack == null) {
                i = -1;
            } else {
                i = this.mFocusedStack.getStackId();
            }
            if (this.mLastFocusedStack != null) {
                i3 = this.mLastFocusedStack.getStackId();
            }
            EventLogTags.writeAmFocusedStack(i2, i, i3, reason);
        }
        ActivityRecord r = topRunningActivityLocked();
        if ((this.mService.mBooting || !this.mService.mBooted) && r != null && r.idle) {
            checkFinishBootingLocked();
        }
    }

    void moveHomeStackToFront(String reason) {
        this.mHomeStack.moveToFront(reason);
    }

    void moveRecentsStackToFront(String reason) {
        ActivityStack recentsStack = getDefaultDisplay().getStack(0, 3);
        if (recentsStack != null) {
            recentsStack.moveToFront(reason);
        }
    }

    boolean moveHomeStackTaskToTop(String reason) {
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord top = getHomeActivity();
        if (top == null) {
            return false;
        }
        moveFocusableActivityStackToFrontLocked(top, reason);
        return true;
    }

    boolean resumeHomeStackTask(ActivityRecord prev, String reason) {
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord r = getHomeActivity();
        String myReason = new StringBuilder();
        myReason.append(reason);
        myReason.append(" resumeHomeStackTask");
        myReason = myReason.toString();
        if (r == null || r.finishing) {
            return this.mService.startHomeActivityLocked(this.mCurrentUser, myReason);
        }
        moveFocusableActivityStackToFrontLocked(r, myReason);
        return resumeFocusedStackTopActivityLocked(this.mHomeStack, prev, null);
    }

    TaskRecord anyTaskForIdLocked(int id) {
        return anyTaskForIdLocked(id, 2);
    }

    TaskRecord anyTaskForIdLocked(int id, int matchMode) {
        return anyTaskForIdLocked(id, matchMode, null, false);
    }

    TaskRecord anyTaskForIdLocked(int id, int matchMode, ActivityOptions aOptions, boolean onTop) {
        int i = id;
        int i2 = matchMode;
        ActivityOptions activityOptions = aOptions;
        boolean z = onTop;
        if (i2 == 2 || activityOptions == null) {
            int numDisplays = this.mActivityDisplays.size();
            int displayNdx = 0;
            while (true) {
                int displayNdx2 = displayNdx;
                if (displayNdx2 < numDisplays) {
                    ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx2);
                    int stackNdx = display.getChildCount() - 1;
                    while (true) {
                        int stackNdx2 = stackNdx;
                        if (stackNdx2 < 0) {
                            break;
                        }
                        ActivityStack stack = display.getChildAt(stackNdx2);
                        TaskRecord task = stack.taskForIdLocked(i);
                        if (task == null) {
                            stackNdx = stackNdx2 - 1;
                        } else {
                            TaskRecord task2;
                            if (activityOptions != null) {
                                ActivityStack launchStack = getLaunchStack(null, activityOptions, task, z);
                                if (!(launchStack == null || stack == launchStack)) {
                                    task2 = task;
                                    task.reparent(launchStack, z, z ? 0 : 2, true, true, "anyTaskForIdLocked");
                                    return task2;
                                }
                            }
                            task2 = task;
                            ActivityStack activityStack = stack;
                            return task2;
                        }
                    }
                } else if (i2 == 0) {
                    return null;
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Looking for task id=");
                        stringBuilder.append(i);
                        stringBuilder.append(" in recents");
                        Slog.v(str, stringBuilder.toString());
                    }
                    TaskRecord task3 = this.mRecentTasks.getTask(i);
                    String str2;
                    StringBuilder stringBuilder2;
                    if (task3 == null) {
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("\tDidn't find task id=");
                            stringBuilder2.append(i);
                            stringBuilder2.append(" in recents");
                            Slog.d(str2, stringBuilder2.toString());
                        }
                        return null;
                    } else if (i2 == 1) {
                        return task3;
                    } else {
                        if (restoreRecentTaskLocked(task3, activityOptions, z)) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                str2 = ActivityManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Restored task id=");
                                stringBuilder2.append(i);
                                stringBuilder2.append(" from in recents");
                                Slog.w(str2, stringBuilder2.toString());
                            }
                            return task3;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Couldn't restore task id=");
                            stringBuilder2.append(i);
                            stringBuilder2.append(" found in recents");
                            Slog.w(str2, stringBuilder2.toString());
                        }
                        return null;
                    }
                }
                displayNdx = displayNdx2 + 1;
            }
        } else {
            throw new IllegalArgumentException("Should not specify activity options for non-restore lookup");
        }
    }

    ActivityRecord isInAnyStackLocked(IBinder token) {
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = display.getChildAt(stackNdx).isInStackLocked(token);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private boolean taskTopActivityIsUser(TaskRecord task, int userId) {
        ActivityRecord activityRecord = task.getTopActivity();
        ActivityRecord resultTo = activityRecord != null ? activityRecord.resultTo : null;
        return (activityRecord != null && activityRecord.userId == userId) || (resultTo != null && resultTo.userId == userId);
    }

    void lockAllProfileTasks(int userId) {
        this.mWindowManager.deferSurfaceLayout();
        try {
            for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    List<TaskRecord> tasks = display.getChildAt(stackNdx).getAllTasks();
                    for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                        TaskRecord task = (TaskRecord) tasks.get(taskNdx);
                        if (taskTopActivityIsUser(task, userId)) {
                            this.mService.mTaskChangeNotificationController.notifyTaskProfileLocked(task.taskId, userId);
                        }
                    }
                }
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void setNextTaskIdForUserLocked(int taskId, int userId) {
        if (taskId > this.mCurTaskIdForUser.get(userId, -1)) {
            this.mCurTaskIdForUser.put(userId, taskId);
        }
    }

    static int nextTaskIdForUser(int taskId, int userId) {
        int nextTaskId = taskId + 1;
        if (nextTaskId == (userId + 1) * MAX_TASK_IDS_PER_USER) {
            return nextTaskId - MAX_TASK_IDS_PER_USER;
        }
        return nextTaskId;
    }

    int getNextTaskIdForUserLocked(int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, MAX_TASK_IDS_PER_USER * userId);
        int candidateTaskId = nextTaskIdForUser(currentTaskId, userId);
        while (true) {
            if (this.mRecentTasks.containsTaskId(candidateTaskId, userId) || anyTaskForIdLocked(candidateTaskId, 1) != null) {
                candidateTaskId = nextTaskIdForUser(candidateTaskId, userId);
                if (candidateTaskId == currentTaskId) {
                    throw new IllegalStateException("Cannot get an available task id. Reached limit of 100000 running tasks per user.");
                }
            } else {
                this.mCurTaskIdForUser.put(userId, candidateTaskId);
                return candidateTaskId;
            }
        }
    }

    ActivityRecord getResumedActivityLocked() {
        ActivityStack stack = this.mFocusedStack;
        if (stack == null) {
            return null;
        }
        ActivityRecord resumedActivity = stack.getResumedActivity();
        if (resumedActivity == null || resumedActivity.app == null) {
            resumedActivity = stack.mPausingActivity;
            if (resumedActivity == null || resumedActivity.app == null) {
                resumedActivity = stack.topRunningActivityLocked();
            }
        }
        return resumedActivity;
    }

    boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
        String processName = app.processName;
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (isFocusedStack(stack)) {
                    stack.getAllRunningVisibleActivitiesLocked(this.mTmpActivityList);
                    ActivityRecord top = stack.topRunningActivityLocked();
                    if (!(top == null || this.mTmpActivityList.contains(top))) {
                        this.mTmpActivityList.add(top);
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("attachApplicationLocked add top running activity: ");
                        stringBuilder.append(top);
                        Slog.d(str, stringBuilder.toString());
                    }
                    int size = this.mTmpActivityList.size();
                    boolean didSomething2 = didSomething;
                    for (int i = 0; i < size; i++) {
                        ActivityRecord activity = (ActivityRecord) this.mTmpActivityList.get(i);
                        if (activity.app == null && app.uid == activity.info.applicationInfo.uid && processName.equals(activity.processName)) {
                            try {
                                if (realStartActivityLocked(activity, app, top == activity, true)) {
                                    didSomething2 = true;
                                }
                            } catch (RemoteException e) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Exception in new application when starting activity ");
                                stringBuilder2.append(top.intent.getComponent().flattenToShortString());
                                Slog.w(ActivityManagerService.TAG, stringBuilder2.toString(), e);
                                throw e;
                            }
                        }
                    }
                    this.mTmpActivityList.clear();
                    didSomething = didSomething2;
                }
            }
        }
        if (!didSomething) {
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        return didSomething;
    }

    boolean allResumedActivitiesIdle() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (isFocusedStack(stack) && stack.numActivities() != 0) {
                    ActivityRecord resumedActivity = stack.getResumedActivity();
                    if (resumedActivity == null || !resumedActivity.idle) {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            String str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("allResumedActivitiesIdle: stack=");
                            stringBuilder.append(stack.mStackId);
                            stringBuilder.append(" ");
                            stringBuilder.append(resumedActivity);
                            stringBuilder.append(" not idle");
                            Slog.d(str, stringBuilder.toString());
                        }
                        return false;
                    }
                }
            }
        }
        sendPowerHintForLaunchEndIfNeeded();
        return true;
    }

    boolean allResumedActivitiesComplete() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (isFocusedStack(stack)) {
                    ActivityRecord r = stack.getResumedActivity();
                    if (!(r == null || r.isState(ActivityState.RESUMED))) {
                        return false;
                    }
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("allResumedActivitiesComplete: mLastFocusedStack changing from=");
            stringBuilder.append(this.mLastFocusedStack);
            stringBuilder.append(" to=");
            stringBuilder.append(this.mFocusedStack);
            Slog.d(str, stringBuilder.toString());
        }
        this.mLastFocusedStack = this.mFocusedStack;
        return true;
    }

    private boolean allResumedActivitiesVisible() {
        boolean foundResumed = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = display.getChildAt(stackNdx).getResumedActivity();
                if (r != null) {
                    if (!r.nowVisible || this.mActivitiesWaitingForVisibleActivity.contains(r)) {
                        return false;
                    }
                    foundResumed = true;
                }
            }
        }
        return foundResumed;
    }

    boolean pauseBackStacks(boolean userLeaving, ActivityRecord resuming, boolean dontWait) {
        boolean someActivityPaused = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (!(isFocusedStack(stack) || stack.getResumedActivity() == null)) {
                    String str;
                    StringBuilder stringBuilder;
                    if (this.mService.mHwAMSEx == null || !this.mService.mHwAMSEx.isPcMutiResumeStack(stack)) {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            str = ActivityManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("pauseBackStacks: stack=");
                            stringBuilder.append(stack);
                            stringBuilder.append(" mResumedActivity=");
                            stringBuilder.append(stack.getResumedActivity());
                            Slog.d(str, stringBuilder.toString());
                        }
                        someActivityPaused |= stack.startPausingLocked(userLeaving, false, resuming, dontWait);
                    } else {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("isPcMutiResumeStack, do not pauseBackStacks: stack=");
                        stringBuilder.append(stack);
                        stringBuilder.append(" mResumedActivity=");
                        stringBuilder.append(stack.getResumedActivity());
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }
        }
        return someActivityPaused;
    }

    boolean allPausedActivitiesComplete() {
        boolean pausing = true;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = display.getChildAt(stackNdx).mPausingActivity;
                if (!(r == null || r.isState(ActivityState.PAUSED, ActivityState.STOPPED, ActivityState.STOPPING))) {
                    if (!ActivityManagerDebugConfig.DEBUG_STATES) {
                        return false;
                    }
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("allPausedActivitiesComplete: r=");
                    stringBuilder.append(r);
                    stringBuilder.append(" state=");
                    stringBuilder.append(r.getState());
                    Slog.d(str, stringBuilder.toString());
                    pausing = false;
                }
            }
        }
        return pausing;
    }

    void cancelInitializingActivities() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).cancelInitializingActivities();
            }
        }
    }

    void waitActivityVisible(ComponentName name, WaitResult result) {
        this.mWaitingForActivityVisible.add(new WaitInfo(name, result));
    }

    void cleanupActivity(ActivityRecord r) {
        int i;
        this.mFinishingActivities.remove(r);
        this.mActivitiesWaitingForVisibleActivity.remove(r);
        boolean changed = false;
        for (i = this.mWaitingForActivityVisible.size() - 1; i >= 0; i--) {
            if (((WaitInfo) this.mWaitingForActivityVisible.get(i)).matches(r.realActivity)) {
                WaitInfo w = (WaitInfo) this.mWaitingForActivityVisible.remove(i);
                changed = true;
                w.mResult.who = new ComponentName(r.info.packageName, r.info.name);
                w.mResult.totalTime = SystemClock.uptimeMillis() - w.mResult.thisTime;
                w.mResult.thisTime = w.mResult.totalTime;
            }
        }
        for (i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            ComponentName cn = ((WaitResult) this.mWaitingActivityLaunched.get(i)).origin;
            Intent oriIntent = r.intent;
            if (!(cn == null || oriIntent == null || !cn.equals(oriIntent.getComponent()))) {
                WaitResult w2 = (WaitResult) this.mWaitingActivityLaunched.remove(i);
                changed = true;
                w2.who = new ComponentName(r.info.packageName, r.info.name);
                w2.totalTime = SystemClock.uptimeMillis() - w2.thisTime;
                w2.thisTime = w2.totalTime;
            }
        }
        if (changed) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" cleanupActivity notify r = ");
            stringBuilder.append(r);
            Flog.i(101, stringBuilder.toString());
            this.mService.notifyAll();
        }
    }

    void reportActivityVisibleLocked(ActivityRecord r) {
        sendWaitingVisibleReportLocked(r);
    }

    void sendWaitingVisibleReportLocked(ActivityRecord r) {
        boolean changed = false;
        for (int i = this.mWaitingForActivityVisible.size() - 1; i >= 0; i--) {
            WaitInfo w = (WaitInfo) this.mWaitingForActivityVisible.get(i);
            if (w.matches(r.realActivity)) {
                WaitResult result = w.getResult();
                changed = true;
                result.timeout = false;
                result.who = w.getComponent();
                result.totalTime = SystemClock.uptimeMillis() - result.thisTime;
                result.thisTime = result.totalTime;
                this.mWaitingForActivityVisible.remove(w);
            }
        }
        if (changed) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("waited activity visible, r=");
            stringBuilder.append(r);
            Flog.i(101, stringBuilder.toString());
            this.mService.notifyAll();
        }
    }

    void reportWaitingActivityLaunchedIfNeeded(ActivityRecord r, int result) {
        if (!this.mWaitingActivityLaunched.isEmpty()) {
            if (result == 3 || result == 2) {
                boolean changed = false;
                for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
                    WaitResult w = (WaitResult) this.mWaitingActivityLaunched.remove(i);
                    if (w.who == null) {
                        changed = true;
                        w.result = result;
                        if (result == 3) {
                            w.who = r.realActivity;
                        }
                    }
                }
                if (changed) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" reportTaskToFrontNoLaunch notify r = ");
                    stringBuilder.append(r);
                    Flog.i(101, stringBuilder.toString());
                    this.mService.notifyAll();
                }
            }
        }
    }

    void reportActivityLaunchedLocked(boolean timeout, ActivityRecord r, long thisTime, long totalTime) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            WaitResult w = (WaitResult) this.mWaitingActivityLaunched.remove(i);
            if (w.who == null) {
                changed = true;
                w.timeout = timeout;
                if (r != null) {
                    w.who = new ComponentName(r.info.packageName, r.info.name);
                }
                w.thisTime = thisTime;
                w.totalTime = totalTime;
            }
        }
        if (changed) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("waited activity launched, r= ");
            stringBuilder.append(r);
            Flog.i(101, stringBuilder.toString());
            this.mService.notifyAll();
        }
    }

    ActivityRecord topRunningActivityLocked() {
        return topRunningActivityLocked(false);
    }

    ActivityRecord topRunningActivityLocked(boolean considerKeyguardState) {
        ActivityStack focusedStack = this.mFocusedStack;
        ActivityRecord r = focusedStack.topRunningActivityLocked();
        if (r != null && isValidTopRunningActivity(r, considerKeyguardState)) {
            return r;
        }
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            int displayId = this.mTmpOrderedDisplayIds.get(i);
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(displayId);
            if (HwVRUtils.isVRMode() && !HwPCUtils.isValidExtDisplayId(displayId) && !HwVRUtils.isValidVRDisplayId(displayId)) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("topRunningActivityLocked is not ValidExtDisplayId ,displayId = ");
                stringBuilder.append(displayId);
                Slog.i(str, stringBuilder.toString());
            } else if (display == null) {
                continue;
            } else {
                ActivityStack topStack = display.getTopStack();
                if (!(topStack == null || !topStack.isFocusable() || topStack == focusedStack)) {
                    ActivityRecord topActivity = topStack.topRunningActivityLocked();
                    if (topActivity != null && isValidTopRunningActivity(topActivity, considerKeyguardState)) {
                        return topActivity;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidTopRunningActivity(ActivityRecord record, boolean considerKeyguardState) {
        if (considerKeyguardState && getKeyguardController().isKeyguardLocked()) {
            return record.canShowWhenLocked();
        }
        return true;
    }

    @VisibleForTesting
    void getRunningTasks(int maxNum, List<RunningTaskInfo> list, @ActivityType int ignoreActivityType, @WindowingMode int ignoreWindowingMode, int callingUid, boolean allowed) {
        this.mRunningTasks.getTasks(maxNum, list, ignoreActivityType, ignoreWindowingMode, this.mActivityDisplays, callingUid, allowed);
    }

    protected boolean keepStackResumed(ActivityStack stack) {
        return false;
    }

    protected boolean isStackInVisible(ActivityStack stack) {
        return false;
    }

    ActivityInfo resolveActivity(Intent intent, ResolveInfo rInfo, int startFlags, ProfilerInfo profilerInfo) {
        ActivityInfo aInfo = rInfo != null ? rInfo.activityInfo : null;
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            if (!aInfo.processName.equals("system")) {
                if ((startFlags & 2) != 0) {
                    this.mService.setDebugApp(aInfo.processName, true, false);
                }
                if ((startFlags & 8) != 0) {
                    this.mService.setNativeDebuggingAppLocked(aInfo.applicationInfo, aInfo.processName);
                }
                if ((startFlags & 4) != 0) {
                    this.mService.setTrackAllocationApp(aInfo.applicationInfo, aInfo.processName);
                }
                if (profilerInfo != null) {
                    this.mService.setProfileApp(aInfo.applicationInfo, aInfo.processName, profilerInfo);
                }
            }
            String intentLaunchToken = intent.getLaunchToken();
            if (aInfo.launchToken == null && intentLaunchToken != null) {
                aInfo.launchToken = intentLaunchToken;
            }
        }
        return aInfo;
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId) {
        return resolveIntent(intent, resolvedType, userId, 0, Binder.getCallingUid());
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags, int filterCallingUid) {
        Throwable th;
        try {
            Trace.traceBegin(64, "resolveIntent");
            int modifiedFlags = (flags | 65536) | 1024;
            if (intent.isWebIntent() || (intent.getFlags() & 2048) != 0) {
                modifiedFlags |= DumpState.DUMP_VOLUMES;
            }
            int modifiedFlags2 = modifiedFlags;
            long token = Binder.clearCallingIdentity();
            try {
                ResolveInfo resolveIntent = this.mService.getPackageManagerInternalLocked().resolveIntent(intent, resolvedType, modifiedFlags2, userId, true, filterCallingUid);
                Binder.restoreCallingIdentity(token);
                Trace.traceEnd(64);
                return resolveIntent;
            } catch (Throwable th2) {
                th = th2;
                Trace.traceEnd(64);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            Trace.traceEnd(64);
            throw th;
        }
    }

    ActivityInfo resolveActivity(Intent intent, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId, int filterCallingUid) {
        return resolveActivity(intent, resolveIntent(intent, resolvedType, userId, 0, filterCallingUid), startFlags, profilerInfo);
    }

    /* JADX WARNING: Removed duplicated region for block: B:118:0x0277 A:{Catch:{ RemoteException -> 0x04c6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x0275 A:{Catch:{ RemoteException -> 0x04c6 }} */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0281 A:{SYNTHETIC, Splitter:B:121:0x0281} */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0294 A:{SYNTHETIC, Splitter:B:126:0x0294} */
    /* JADX WARNING: Removed duplicated region for block: B:155:0x034b A:{SYNTHETIC, Splitter:B:155:0x034b} */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x032e A:{SYNTHETIC, Splitter:B:147:0x032e} */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0370  */
    /* JADX WARNING: Removed duplicated region for block: B:159:0x035d  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x037c A:{SYNTHETIC, Splitter:B:172:0x037c} */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x03ee  */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x042e  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0457  */
    /* JADX WARNING: Removed duplicated region for block: B:203:0x0464  */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x04b5  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x014d  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:245:0x0511  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x04d4 A:{Catch:{ all -> 0x051c }} */
    /* JADX WARNING: Missing block: B:42:0x00ab, code skipped:
            if (r2.appInfo.uid != r15) goto L_0x00ad;
     */
    /* JADX WARNING: Missing block: B:65:0x0144, code skipped:
            if (r13.getLockTaskModeState() != 1) goto L_0x0149;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig) throws RemoteException {
        Throwable profilerInfo;
        ProcessRecord processRecord;
        TaskRecord taskRecord;
        ActivityRecord activityRecord;
        ActivityStack activityStack;
        RemoteException e;
        LockTaskController lockTaskController;
        int i;
        ActivityStack stack;
        String str;
        ActivityRecord activityRecord2 = r;
        ProcessRecord processRecord2 = app;
        boolean z = andResume;
        String str2;
        StringBuilder stringBuilder;
        if (allPausedActivitiesComplete()) {
            TaskRecord task = r.getTask();
            StringBuilder stringBuilder2;
            if (task == null) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" null task for ActivityRecord: ");
                stringBuilder2.append(activityRecord2);
                Slog.e(str2, stringBuilder2.toString());
                return false;
            }
            ActivityStack stack2 = task.getStack();
            beginDeferResume();
            activityRecord2.startFreezingScreenLocked(processRecord2, 0);
            r.startLaunchTickingLocked();
            r.setProcess(app);
            if (getKeyguardController().isKeyguardLocked()) {
                try {
                    r.notifyUnknownVisibilityLaunched();
                } catch (Throwable th) {
                    profilerInfo = th;
                    processRecord = processRecord2;
                    taskRecord = task;
                    activityRecord = activityRecord2;
                    activityStack = stack2;
                }
            }
            if (checkConfig) {
                ensureVisibilityAndConfig(activityRecord2, r.getDisplayId(), false, true);
            }
            try {
                if (r.getStack().checkKeyguardVisibility(activityRecord2, true, true)) {
                    activityRecord2.setVisibility(true);
                }
                int applicationInfoUid = activityRecord2.info.applicationInfo != null ? activityRecord2.info.applicationInfo.uid : -1;
                if (activityRecord2.userId == processRecord2.userId) {
                }
                str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("User ID for activity changing for ");
                stringBuilder3.append(activityRecord2);
                stringBuilder3.append(" appInfo.uid=");
                stringBuilder3.append(activityRecord2.appInfo.uid);
                stringBuilder3.append(" info.ai.uid=");
                stringBuilder3.append(applicationInfoUid);
                stringBuilder3.append(" old=");
                stringBuilder3.append(activityRecord2.app);
                stringBuilder3.append(" new=");
                stringBuilder3.append(processRecord2);
                Slog.wtf(str2, stringBuilder3.toString());
                processRecord2.waitingToKill = null;
                activityRecord2.launchCount++;
                activityRecord2.lastLaunchTime = SystemClock.uptimeMillis();
                if (ActivityManagerDebugConfig.DEBUG_ALL) {
                    str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Launching: ");
                    stringBuilder4.append(activityRecord2);
                    Slog.v(str2, stringBuilder4.toString());
                }
                if (processRecord2.activities.indexOf(activityRecord2) < 0) {
                    processRecord2.activities.add(activityRecord2);
                }
                this.mService.updateLruProcessLocked(processRecord2, true, null);
                this.mService.updateOomAdjLocked();
                LockTaskController lockTaskController2 = this.mService.getLockTaskController();
                if (task.mLockTaskAuth != 2) {
                    if (task.mLockTaskAuth != 4) {
                        if (task.mLockTaskAuth == 3) {
                        }
                        if (processRecord2.thread == null) {
                            ProfilerInfo profilerInfo2;
                            boolean forceHardAccel;
                            MergedConfiguration mergedConfiguration;
                            ClientTransaction clientTransaction;
                            ActivityLifecycleItem lifecycleItem;
                            ArrayList arrayList = null;
                            ArrayList arrayList2 = null;
                            if (z) {
                                try {
                                    arrayList = activityRecord2.results;
                                    arrayList2 = activityRecord2.newIntents;
                                } catch (RemoteException e2) {
                                    e = e2;
                                    processRecord = processRecord2;
                                    taskRecord = task;
                                    lockTaskController = lockTaskController2;
                                    i = applicationInfoUid;
                                    activityRecord = activityRecord2;
                                    activityStack = stack2;
                                    try {
                                        if (activityRecord.launchFailed) {
                                        }
                                    } catch (Throwable th2) {
                                        profilerInfo = th2;
                                        endDeferResume();
                                        throw profilerInfo;
                                    }
                                }
                            }
                            List results = arrayList;
                            List newIntents = arrayList2;
                            if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                                str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Launching: ");
                                stringBuilder5.append(activityRecord2);
                                stringBuilder5.append(" icicle=");
                                stringBuilder5.append(activityRecord2.icicle);
                                stringBuilder5.append(" with results=");
                                stringBuilder5.append(results);
                                stringBuilder5.append(" newIntents=");
                                stringBuilder5.append(newIntents);
                                stringBuilder5.append(" andResume=");
                                stringBuilder5.append(z);
                                Slog.v(str2, stringBuilder5.toString());
                            }
                            EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY, new Object[]{Integer.valueOf(activityRecord2.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), activityRecord2.shortComponentName});
                            if (r.isActivityTypeHome()) {
                                this.mService.mHomeProcess = ((ActivityRecord) task.mActivities.get(0)).app;
                                this.mService.mHwAMSEx.reportHomeProcess(this.mService.mHomeProcess);
                            }
                            this.mService.notifyPackageUse(activityRecord2.intent.getComponent().getPackageName(), 0);
                            activityRecord2.sleeping = false;
                            activityRecord2.forceNewConfig = false;
                            this.mService.getAppWarningsLocked().onStartActivity(activityRecord2);
                            this.mService.showAskCompatModeDialogLocked(activityRecord2);
                            activityRecord2.compat = this.mService.compatibilityInfoForPackageLocked(activityRecord2.info.applicationInfo);
                            if (this.mService.mProfileApp != null) {
                                if (this.mService.mProfileApp.equals(processRecord2.processName) && (this.mService.mProfileProc == null || this.mService.mProfileProc == processRecord2)) {
                                    this.mService.mProfileProc = processRecord2;
                                    ProfilerInfo profilerInfoSvc = this.mService.mProfilerInfo;
                                    if (!(profilerInfoSvc == null || profilerInfoSvc.profileFile == null)) {
                                        if (profilerInfoSvc.profileFd != null) {
                                            try {
                                                profilerInfoSvc.profileFd = profilerInfoSvc.profileFd.dup();
                                            } catch (IOException e3) {
                                                profilerInfoSvc.closeFd();
                                            }
                                        }
                                        profilerInfo2 = new ProfilerInfo(profilerInfoSvc);
                                        processRecord2.hasShownUi = true;
                                        processRecord2.pendingUiClean = true;
                                        processRecord2.forceProcessStateUpTo(this.mService.mTopProcessState);
                                        forceHardAccel = HwCustNonHardwareAcceleratedPackagesManager.getDefault().shouldForceEnabled(activityRecord2.info, processRecord2.instr != null ? null : processRecord2.instr.mClass);
                                        if (forceHardAccel) {
                                            ActivityInfo activityInfo = activityRecord2.info;
                                            activityInfo.flags |= 512;
                                        }
                                        this.mActivityLaunchTrack = "launchActivity";
                                        if (Jlog.isPerfTest()) {
                                            Jlog.i(3036, Jlog.getMessage("ActivityStackSupervisor", "realStartActivityLocked", Intent.toPkgClsString(activityRecord2.realActivity)));
                                        }
                                        lockTaskController = lockTaskController2;
                                        this.mWindowManager.prepareForForceRotation(activityRecord2.appToken.asBinder(), activityRecord2.info.packageName, processRecord2.pid, activityRecord2.info.processName);
                                        mergedConfiguration = new MergedConfiguration(this.mService.getGlobalConfiguration(), r.getMergedOverrideConfiguration());
                                        activityRecord2.setLastReportedConfiguration(mergedConfiguration);
                                        logIfTransactionTooLarge(activityRecord2.intent, activityRecord2.icicle);
                                        clientTransaction = ClientTransaction.obtain(processRecord2.thread, activityRecord2.appToken);
                                        try {
                                            stack = stack2;
                                        } catch (RemoteException e4) {
                                            e = e4;
                                            processRecord = processRecord2;
                                            taskRecord = task;
                                            activityRecord = activityRecord2;
                                            activityStack = stack2;
                                            if (activityRecord.launchFailed) {
                                                str = ActivityManagerService.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Second failure launching ");
                                                stringBuilder2.append(activityRecord.intent.getComponent().flattenToShortString());
                                                stringBuilder2.append(", giving up");
                                                Slog.e(str, stringBuilder2.toString(), e);
                                                this.mService.appDiedLocked(processRecord);
                                                activityStack.requestFinishActivityLocked(activityRecord.appToken, 0, null, "2nd-crash", false);
                                                endDeferResume();
                                                return false;
                                            }
                                            LockTaskController task2 = lockTaskController;
                                            activityRecord.launchFailed = true;
                                            processRecord.activities.remove(activityRecord);
                                            throw e;
                                        }
                                        try {
                                            try {
                                                try {
                                                    clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(activityRecord2.intent), System.identityHashCode(r), activityRecord2.info, mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration(), activityRecord2.compat, activityRecord2.launchedFromPackage, task.voiceInteractor, processRecord2.repProcState, activityRecord2.icicle, activityRecord2.persistentState, results, newIntents, this.mService.isNextTransitionForward(), profilerInfo2));
                                                    if (z) {
                                                        lifecycleItem = PauseActivityItem.obtain();
                                                    } else {
                                                        try {
                                                            lifecycleItem = ResumeActivityItem.obtain(this.mService.isNextTransitionForward());
                                                        } catch (RemoteException e5) {
                                                            e = e5;
                                                            activityStack = stack;
                                                            activityRecord = r;
                                                        } catch (Throwable th3) {
                                                            profilerInfo = th3;
                                                            activityRecord = r;
                                                            processRecord = app;
                                                            endDeferResume();
                                                            throw profilerInfo;
                                                        }
                                                    }
                                                    clientTransaction.setLifecycleStateRequest(lifecycleItem);
                                                    this.mService.getLifecycleManager().scheduleTransaction(clientTransaction);
                                                    if (forceHardAccel) {
                                                        activityRecord = r;
                                                    } else {
                                                        activityRecord = r;
                                                        try {
                                                            ActivityInfo activityInfo2 = activityRecord.info;
                                                            activityInfo2.flags &= -513;
                                                        } catch (RemoteException e6) {
                                                            e = e6;
                                                            activityStack = stack;
                                                        } catch (Throwable th4) {
                                                            profilerInfo = th4;
                                                            activityStack = stack;
                                                            processRecord = app;
                                                            endDeferResume();
                                                            throw profilerInfo;
                                                        }
                                                    }
                                                    processRecord = app;
                                                    try {
                                                        if ((processRecord.info.privateFlags & 2) != 0) {
                                                            try {
                                                                if (this.mService.mHasHeavyWeightFeature && processRecord.processName.equals(processRecord.info.packageName)) {
                                                                    if (!(this.mService.mHeavyWeightProcess == null || this.mService.mHeavyWeightProcess == processRecord)) {
                                                                        str = ActivityManagerService.TAG;
                                                                        stringBuilder2 = new StringBuilder();
                                                                        stringBuilder2.append("Starting new heavy weight process ");
                                                                        stringBuilder2.append(processRecord);
                                                                        stringBuilder2.append(" when already running ");
                                                                        stringBuilder2.append(this.mService.mHeavyWeightProcess);
                                                                        Slog.w(str, stringBuilder2.toString());
                                                                    }
                                                                    this.mService.mHeavyWeightProcess = processRecord;
                                                                    Message msg = this.mService.mHandler.obtainMessage(24);
                                                                    msg.obj = activityRecord;
                                                                    this.mService.mHandler.sendMessage(msg);
                                                                }
                                                            } catch (RemoteException e7) {
                                                                e = e7;
                                                                activityStack = stack;
                                                                if (activityRecord.launchFailed) {
                                                                }
                                                            } catch (Throwable th5) {
                                                                profilerInfo = th5;
                                                                activityStack = stack;
                                                                endDeferResume();
                                                                throw profilerInfo;
                                                            }
                                                        }
                                                        endDeferResume();
                                                        activityRecord.launchFailed = false;
                                                        activityStack = stack;
                                                        if (activityStack.updateLRUListLocked(activityRecord)) {
                                                            str2 = ActivityManagerService.TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("Activity ");
                                                            stringBuilder.append(activityRecord);
                                                            stringBuilder.append(" being launched, but already in LRU list");
                                                            Slog.w(str2, stringBuilder.toString());
                                                        }
                                                        if (z || !readyToResume()) {
                                                            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                                                str2 = ActivityManagerService.TAG;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Moving to PAUSED: ");
                                                                stringBuilder.append(activityRecord);
                                                                stringBuilder.append(" (starting in paused state)");
                                                                Slog.v(str2, stringBuilder.toString());
                                                            }
                                                            activityRecord.setState(ActivityState.PAUSED, "realStartActivityLocked");
                                                        } else {
                                                            StringBuilder stringBuilder6 = new StringBuilder();
                                                            stringBuilder6.append(this.mActivityLaunchTrack);
                                                            stringBuilder6.append(" minmalResume");
                                                            this.mActivityLaunchTrack = stringBuilder6.toString();
                                                            activityStack.minimalResumeActivityLocked(activityRecord);
                                                        }
                                                        if (isFocusedStack(activityStack)) {
                                                            this.mService.getActivityStartController().startSetupActivity();
                                                        }
                                                        if (activityRecord.app != null) {
                                                            this.mService.mServices.updateServiceConnectionActivitiesLocked(activityRecord.app);
                                                        }
                                                        return true;
                                                    } catch (RemoteException e8) {
                                                        e = e8;
                                                        activityStack = stack;
                                                        if (activityRecord.launchFailed) {
                                                        }
                                                    } catch (Throwable th6) {
                                                        profilerInfo = th6;
                                                        activityStack = stack;
                                                        endDeferResume();
                                                        throw profilerInfo;
                                                    }
                                                } catch (RemoteException e9) {
                                                    e = e9;
                                                    activityStack = stack;
                                                    activityRecord = r;
                                                    processRecord = app;
                                                    if (activityRecord.launchFailed) {
                                                    }
                                                } catch (Throwable th7) {
                                                    profilerInfo = th7;
                                                    activityStack = stack;
                                                    activityRecord = r;
                                                    processRecord = app;
                                                    endDeferResume();
                                                    throw profilerInfo;
                                                }
                                            } catch (RemoteException e10) {
                                                e = e10;
                                                activityRecord = activityRecord2;
                                                activityStack = stack;
                                                processRecord = app;
                                                if (activityRecord.launchFailed) {
                                                }
                                            } catch (Throwable th8) {
                                                profilerInfo = th8;
                                                activityRecord = activityRecord2;
                                                activityStack = stack;
                                                processRecord = app;
                                                endDeferResume();
                                                throw profilerInfo;
                                            }
                                        } catch (RemoteException e11) {
                                            e = e11;
                                            processRecord = processRecord2;
                                            activityRecord = activityRecord2;
                                            activityStack = stack;
                                            if (activityRecord.launchFailed) {
                                            }
                                        } catch (Throwable th9) {
                                            profilerInfo = th9;
                                            processRecord = processRecord2;
                                            activityRecord = activityRecord2;
                                            activityStack = stack;
                                            endDeferResume();
                                            throw profilerInfo;
                                        }
                                    }
                                }
                            }
                            profilerInfo2 = null;
                            processRecord2.hasShownUi = true;
                            processRecord2.pendingUiClean = true;
                            processRecord2.forceProcessStateUpTo(this.mService.mTopProcessState);
                            if (processRecord2.instr != null) {
                            }
                            forceHardAccel = HwCustNonHardwareAcceleratedPackagesManager.getDefault().shouldForceEnabled(activityRecord2.info, processRecord2.instr != null ? null : processRecord2.instr.mClass);
                            if (forceHardAccel) {
                            }
                            this.mActivityLaunchTrack = "launchActivity";
                            if (Jlog.isPerfTest()) {
                            }
                            lockTaskController = lockTaskController2;
                            try {
                                this.mWindowManager.prepareForForceRotation(activityRecord2.appToken.asBinder(), activityRecord2.info.packageName, processRecord2.pid, activityRecord2.info.processName);
                                mergedConfiguration = new MergedConfiguration(this.mService.getGlobalConfiguration(), r.getMergedOverrideConfiguration());
                                activityRecord2.setLastReportedConfiguration(mergedConfiguration);
                                logIfTransactionTooLarge(activityRecord2.intent, activityRecord2.icicle);
                                clientTransaction = ClientTransaction.obtain(processRecord2.thread, activityRecord2.appToken);
                            } catch (RemoteException e12) {
                                e = e12;
                                processRecord = processRecord2;
                                taskRecord = task;
                                i = applicationInfoUid;
                                activityRecord = activityRecord2;
                                activityStack = stack2;
                                if (activityRecord.launchFailed) {
                                }
                            }
                            try {
                                stack = stack2;
                                clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(activityRecord2.intent), System.identityHashCode(r), activityRecord2.info, mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration(), activityRecord2.compat, activityRecord2.launchedFromPackage, task.voiceInteractor, processRecord2.repProcState, activityRecord2.icicle, activityRecord2.persistentState, results, newIntents, this.mService.isNextTransitionForward(), profilerInfo2));
                                if (z) {
                                }
                                clientTransaction.setLifecycleStateRequest(lifecycleItem);
                                this.mService.getLifecycleManager().scheduleTransaction(clientTransaction);
                                if (forceHardAccel) {
                                }
                                processRecord = app;
                                if ((processRecord.info.privateFlags & 2) != 0) {
                                }
                                endDeferResume();
                                activityRecord.launchFailed = false;
                                activityStack = stack;
                                if (activityStack.updateLRUListLocked(activityRecord)) {
                                }
                                if (z) {
                                }
                                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                }
                                activityRecord.setState(ActivityState.PAUSED, "realStartActivityLocked");
                                if (isFocusedStack(activityStack)) {
                                }
                                if (activityRecord.app != null) {
                                }
                                return true;
                            } catch (RemoteException e13) {
                                e = e13;
                                processRecord = processRecord2;
                                activityRecord = activityRecord2;
                                activityStack = stack2;
                                if (activityRecord.launchFailed) {
                                }
                            } catch (Throwable th10) {
                                profilerInfo = th10;
                                processRecord = processRecord2;
                                activityRecord = activityRecord2;
                                activityStack = stack2;
                                endDeferResume();
                                throw profilerInfo;
                            }
                        }
                        processRecord = processRecord2;
                        taskRecord = task;
                        lockTaskController = lockTaskController2;
                        i = applicationInfoUid;
                        activityRecord = activityRecord2;
                        activityStack = stack2;
                        try {
                            throw new RemoteException();
                        } catch (RemoteException e14) {
                            e = e14;
                            if (activityRecord.launchFailed) {
                            }
                        }
                    }
                }
                lockTaskController2.startLockTaskMode(task, false, 0);
                try {
                    if (processRecord2.thread == null) {
                    }
                } catch (RemoteException e15) {
                    e = e15;
                    processRecord = processRecord2;
                    taskRecord = task;
                    lockTaskController = lockTaskController2;
                    i = applicationInfoUid;
                    activityRecord = activityRecord2;
                    activityStack = stack2;
                    if (activityRecord.launchFailed) {
                    }
                }
            } catch (Throwable th11) {
                profilerInfo = th11;
                processRecord = processRecord2;
                taskRecord = task;
                activityRecord = activityRecord2;
                activityStack = stack2;
                endDeferResume();
                throw profilerInfo;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_STATES) {
            str2 = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("realStartActivityLocked: Skipping start of r=");
            stringBuilder.append(activityRecord2);
            stringBuilder.append(" some activities pausing...");
            Slog.v(str2, stringBuilder.toString());
        }
        return false;
        processRecord = app;
        if (activityRecord.launchFailed) {
        }
    }

    boolean ensureVisibilityAndConfig(ActivityRecord starting, int displayId, boolean markFrozenIfConfigChanged, boolean deferResume) {
        Configuration config = null;
        ensureActivitiesVisibleLocked(null, 0, false, false);
        WindowManagerService windowManagerService = this.mWindowManager;
        Configuration displayOverrideConfiguration = getDisplayOverrideConfiguration(displayId);
        if (starting != null && starting.mayFreezeScreenLocked(starting.app)) {
            config = starting.appToken;
        }
        config = windowManagerService.updateOrientationFromAppTokens(displayOverrideConfiguration, config, displayId, true);
        if (!(starting == null || !markFrozenIfConfigChanged || config == null)) {
            starting.frozenBeforeDestroy = true;
        }
        return this.mService.updateDisplayOverrideConfigurationLocked(config, starting, deferResume, displayId);
    }

    private void logIfTransactionTooLarge(Intent intent, Bundle icicle) {
        int extrasSize = 0;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                extrasSize = extras.getSize();
            }
        }
        int icicleSize = icicle == null ? 0 : icicle.getSize();
        if (extrasSize + icicleSize > 200000) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Transaction too large, intent: ");
            stringBuilder.append(intent);
            stringBuilder.append(", extras size: ");
            stringBuilder.append(extrasSize);
            stringBuilder.append(", icicle size: ");
            stringBuilder.append(icicleSize);
            Slog.e(str, stringBuilder.toString());
        }
    }

    protected void handlePCWindowStateChanged() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b0  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x00c8  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00ec A:{SYNTHETIC, Splitter:B:39:0x00ec} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x015c  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b0  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x00c8  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00ec A:{SYNTHETIC, Splitter:B:39:0x00ec} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x015c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig) {
        RemoteException e;
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        Set<String> categories;
        ActivityRecord activityRecord = r;
        boolean isStartingLauncher = true;
        ProcessRecord app = this.mService.getProcessRecordLocked(activityRecord.processName, activityRecord.info.applicationInfo.uid, true);
        getLaunchTimeTracker().setLaunchTime(activityRecord);
        if (activityRecord.app != null && this.mService.mSystemReady) {
            this.mService.getDAMonitor().noteActivityDisplayedStart(activityRecord.shortComponentName, activityRecord.app.uid, activityRecord.app.pid);
        }
        boolean z;
        boolean z2;
        if (app == null || app.thread == null) {
            z = andResume;
            z2 = checkConfig;
        } else {
            try {
                if ((activityRecord.info.flags & 1) == 0 || !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(activityRecord.info.packageName)) {
                    app.addPackage(activityRecord.info.packageName, activityRecord.info.applicationInfo.longVersionCode, this.mService.mProcessStats);
                }
                try {
                    realStartActivityLocked(activityRecord, app, andResume, checkConfig);
                    return;
                } catch (RemoteException e2) {
                    e = e2;
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception when starting activity ");
                    stringBuilder.append(activityRecord.intent.getComponent().flattenToShortString());
                    Slog.w(str, stringBuilder.toString(), e);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("start process for launching activity: ");
                    stringBuilder2.append(activityRecord);
                    Flog.i(101, stringBuilder2.toString());
                    if (this.mAppResource == null) {
                    }
                    categories = activityRecord.intent.getCategories();
                    if (categories != null) {
                    }
                    isStartingLauncher = false;
                    if (isStartingLauncher) {
                    }
                    if (this.mService.mUserController.hasStartedUserState(activityRecord.userId)) {
                    }
                }
            } catch (RemoteException e3) {
                e = e3;
                z = andResume;
                z2 = checkConfig;
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception when starting activity ");
                stringBuilder.append(activityRecord.intent.getComponent().flattenToShortString());
                Slog.w(str, stringBuilder.toString(), e);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("start process for launching activity: ");
                stringBuilder2.append(activityRecord);
                Flog.i(101, stringBuilder2.toString());
                if (this.mAppResource == null) {
                }
                categories = activityRecord.intent.getCategories();
                if (categories != null) {
                }
                isStartingLauncher = false;
                if (isStartingLauncher) {
                }
                if (this.mService.mUserController.hasStartedUserState(activityRecord.userId)) {
                }
            }
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("start process for launching activity: ");
        stringBuilder2.append(activityRecord);
        Flog.i(101, stringBuilder2.toString());
        if (this.mAppResource == null) {
            this.mAppResource = HwFrameworkFactory.getHwResource(18);
        }
        if (!(this.mAppResource == null || activityRecord.processName == null)) {
            categories = activityRecord.intent.getCategories();
            if (categories != null) {
                boolean contains = categories.contains("android.intent.category.LAUNCHER");
            }
        }
        if (activityRecord.intent.getComponent() == null || !"com.huawei.android.launcher".equals(activityRecord.intent.getComponent().getPackageName())) {
            isStartingLauncher = false;
        }
        if (isStartingLauncher) {
            try {
                this.mService.getPackageManagerInternalLocked().checkPackageStartable(activityRecord.intent.getComponent().getPackageName(), UserHandle.getUserId(activityRecord.info.applicationInfo.uid));
            } catch (SecurityException e4) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("skip launch freezen hwLauncher for uid: ");
                stringBuilder.append(activityRecord.info.applicationInfo.uid);
                Slog.i(str, stringBuilder.toString());
                return;
            }
        }
        if (this.mService.mUserController.hasStartedUserState(activityRecord.userId)) {
            String str2 = ActivityManagerService.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("skip launch r : ");
            stringBuilder2.append(activityRecord);
            stringBuilder2.append(": user ");
            stringBuilder2.append(activityRecord.userId);
            stringBuilder2.append(" is stopped");
            Slog.w(str2, stringBuilder2.toString());
            return;
        }
        if (!startProcessOnExtDisplay(r)) {
            this.mService.mHwAMSEx.setHbsMiniAppUid(activityRecord.info.applicationInfo, activityRecord.intent);
            this.mService.startProcessLocked(activityRecord.processName, activityRecord.info.applicationInfo, true, 0, "activity", activityRecord.intent.getComponent(), false, false, true);
        }
    }

    protected boolean startProcessOnExtDisplay(ActivityRecord r) {
        return false;
    }

    void sendPowerHintForLaunchStartIfNeeded(boolean forceSend, ActivityRecord targetActivity) {
        boolean sendHint = forceSend;
        if (!sendHint) {
            ActivityRecord resumedActivity = getResumedActivityLocked();
            boolean z = resumedActivity == null || resumedActivity.app == null || !resumedActivity.app.equals(targetActivity.app);
            sendHint = z;
        }
        if (sendHint && this.mService.mLocalPowerManager != null) {
            this.mService.mLocalPowerManager.powerHint(8, 1);
            this.mPowerHintSent = true;
        }
    }

    void sendPowerHintForLaunchEndIfNeeded() {
        if (this.mPowerHintSent && this.mService.mLocalPowerManager != null) {
            this.mService.mLocalPowerManager.powerHint(8, 0);
            this.mPowerHintSent = false;
        }
    }

    boolean checkStartAnyActivityPermission(Intent intent, ActivityInfo aInfo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, boolean ignoreTargetSecurity, boolean launchingInTask, ProcessRecord callerApp, ActivityRecord resultRecord, ActivityStack resultStack) {
        ActivityInfo activityInfo = aInfo;
        int i = callingPid;
        int i2 = callingUid;
        ProcessRecord processRecord = callerApp;
        boolean z = this.mService.getRecentTasks() != null && this.mService.getRecentTasks().isCallerRecents(i2);
        boolean isCallerRecents = z;
        String str;
        if (this.mService.checkPermission("android.permission.START_ANY_ACTIVITY", i, i2) == 0 || (isCallerRecents && launchingInTask)) {
            str = callingPackage;
            return true;
        }
        str = callingPackage;
        int componentRestriction = getComponentRestrictionForCallingPackage(activityInfo, str, i, i2, ignoreTargetSecurity);
        int actionRestriction = getActionRestrictionForCallingPackage(intent.getAction(), str, i, i2);
        if (componentRestriction == 1 || actionRestriction == 1) {
            String msg;
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            StringBuilder stringBuilder;
            if (actionRestriction == 1) {
                msg = new StringBuilder();
                msg.append("Permission Denial: starting ");
                msg.append(intent.toString());
                msg.append(" from ");
                msg.append(processRecord);
                msg.append(" (pid=");
                msg.append(i);
                msg.append(", uid=");
                msg.append(i2);
                msg.append(") with revoked permission ");
                msg.append((String) ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction()));
                msg = msg.toString();
            } else if (activityInfo.exported) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: starting ");
                stringBuilder.append(intent.toString());
                stringBuilder.append(" from ");
                stringBuilder.append(processRecord);
                stringBuilder.append(" (pid=");
                stringBuilder.append(i);
                stringBuilder.append(", uid=");
                stringBuilder.append(i2);
                stringBuilder.append(") requires ");
                stringBuilder.append(activityInfo.permission);
                msg = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: starting ");
                stringBuilder.append(intent.toString());
                stringBuilder.append(" from ");
                stringBuilder.append(processRecord);
                stringBuilder.append(" (pid=");
                stringBuilder.append(i);
                stringBuilder.append(", uid=");
                stringBuilder.append(i2);
                stringBuilder.append(") not exported from uid ");
                stringBuilder.append(activityInfo.applicationInfo.uid);
                msg = stringBuilder.toString();
            }
            Slog.w(ActivityManagerService.TAG, msg);
            throw new SecurityException(msg);
        } else if (actionRestriction == 2) {
            String message = new StringBuilder();
            message.append("Appop Denial: starting ");
            message.append(intent.toString());
            message.append(" from ");
            message.append(processRecord);
            message.append(" (pid=");
            message.append(i);
            message.append(", uid=");
            message.append(i2);
            message.append(") requires ");
            message.append(AppOpsManager.permissionToOp((String) ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction())));
            Slog.w(ActivityManagerService.TAG, message.toString());
            return false;
        } else if (componentRestriction != 2) {
            return true;
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Appop Denial: starting ");
            stringBuilder2.append(intent.toString());
            stringBuilder2.append(" from ");
            stringBuilder2.append(processRecord);
            stringBuilder2.append(" (pid=");
            stringBuilder2.append(i);
            stringBuilder2.append(", uid=");
            stringBuilder2.append(i2);
            stringBuilder2.append(") requires appop ");
            stringBuilder2.append(AppOpsManager.permissionToOp(activityInfo.permission));
            Slog.w(ActivityManagerService.TAG, stringBuilder2.toString());
            return false;
        }
    }

    boolean isCallerAllowedToLaunchOnDisplay(int callingPid, int callingUid, int launchDisplayId, ActivityInfo aInfo) {
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Launch on display check: displayId=");
            stringBuilder.append(launchDisplayId);
            stringBuilder.append(" callingPid=");
            stringBuilder.append(callingPid);
            stringBuilder.append(" callingUid=");
            stringBuilder.append(callingUid);
            Slog.d(str, stringBuilder.toString());
        }
        if (callingPid == -1 && callingUid == -1) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(ActivityManagerService.TAG, "Launch on display check: no caller info, skip check");
            }
            return true;
        }
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(launchDisplayId);
        if (activityDisplay == null) {
            Slog.w(ActivityManagerService.TAG, "Launch on display check: display not found");
            return false;
        } else if (this.mService.checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW", callingPid, callingUid) == 0) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(ActivityManagerService.TAG, "Launch on display check: allow launch any on display");
            }
            return true;
        } else {
            boolean uidPresentOnDisplay = activityDisplay.isUidPresent(callingUid);
            int displayOwnerUid = activityDisplay.mDisplay.getOwnerUid();
            if (!(activityDisplay.mDisplay.getType() != 5 || displayOwnerUid == 1000 || displayOwnerUid == aInfo.applicationInfo.uid || HwPCUtils.isPcCastModeInServer())) {
                if ((aInfo.flags & Integer.MIN_VALUE) == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(ActivityManagerService.TAG, "Launch on display check: disallow launch on virtual display for not-embedded activity.");
                    }
                    return false;
                } else if (this.mService.checkPermission("android.permission.ACTIVITY_EMBEDDING", callingPid, callingUid) == -1 && !uidPresentOnDisplay) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(ActivityManagerService.TAG, "Launch on display check: disallow activity embedding without permission.");
                    }
                    return false;
                }
            }
            if (!activityDisplay.isPrivate()) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(ActivityManagerService.TAG, "Launch on display check: allow launch on public display");
                }
                return true;
            } else if (displayOwnerUid == callingUid) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(ActivityManagerService.TAG, "Launch on display check: allow launch for owner of the display");
                }
                return true;
            } else if (uidPresentOnDisplay) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(ActivityManagerService.TAG, "Launch on display check: allow launch for caller present on the display");
                }
                return true;
            } else {
                Slog.w(ActivityManagerService.TAG, "Launch on display check: denied");
                return false;
            }
        }
    }

    void updateUIDsPresentOnDisplay() {
        this.mDisplayAccessUIDs.clear();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            if (activityDisplay.isPrivate()) {
                this.mDisplayAccessUIDs.append(activityDisplay.mDisplayId, activityDisplay.getPresentUIDs());
            }
        }
        this.mDisplayManagerInternal.setDisplayAccessUIDs(this.mDisplayAccessUIDs);
    }

    UserInfo getUserInfo(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = UserManager.get(this.mService.mContext).getUserInfo(userId);
            return userInfo;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String callingPackage, int callingPid, int callingUid, boolean ignoreTargetSecurity) {
        if (!ignoreTargetSecurity) {
            if (this.mService.checkComponentPermission(activityInfo.permission, callingPid, callingUid, activityInfo.applicationInfo.uid, activityInfo.exported) == -1) {
                return 1;
            }
        }
        if (activityInfo.permission == null) {
            return 0;
        }
        int opCode = AppOpsManager.permissionToOpCode(activityInfo.permission);
        if (opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0 || ignoreTargetSecurity) {
            return 0;
        }
        return 2;
    }

    private int getActionRestrictionForCallingPackage(String action, String callingPackage, int callingPid, int callingUid) {
        if (action == null) {
            return 0;
        }
        String permission = (String) ACTION_TO_RUNTIME_PERMISSION.get(action);
        if (permission == null) {
            return 0;
        }
        try {
            if (!ArrayUtils.contains(this.mService.mContext.getPackageManager().getPackageInfo(callingPackage, 4096).requestedPermissions, permission)) {
                return 0;
            }
            if (this.mService.checkPermission(permission, callingPid, callingUid) == -1) {
                return 1;
            }
            int opCode = AppOpsManager.permissionToOpCode(permission);
            if (opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0) {
                return 0;
            }
            return 2;
        } catch (NameNotFoundException e) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot find package info for ");
            stringBuilder.append(callingPackage);
            Slog.i(str, stringBuilder.toString());
            return 0;
        }
    }

    void setLaunchSource(int uid) {
        this.mLaunchingActivity.setWorkSource(new WorkSource(uid));
    }

    void acquireLaunchWakelock() {
        this.mLaunchingActivity.acquire();
        if (!this.mHandler.hasMessages(104)) {
            this.mHandler.sendEmptyMessageDelayed(104, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    @GuardedBy("mService")
    private boolean checkFinishBootingLocked() {
        boolean booting = this.mService.mBooting;
        boolean enableScreen = false;
        this.mService.mBooting = false;
        if (!this.mService.mBooted) {
            this.mService.mBooted = true;
            enableScreen = true;
        }
        if (booting || enableScreen) {
            this.mService.postFinishBooting(booting, enableScreen);
        }
        return booting;
    }

    @GuardedBy("mService")
    final ActivityRecord activityIdleInternalLocked(IBinder token, boolean fromTimeout, boolean processPausingActivities, Configuration config) {
        String str;
        StringBuilder stringBuilder;
        ActivityRecord r;
        ArrayList<ActivityRecord> finishes;
        boolean finishes2;
        ArrayList<ActivityRecord> finishes3;
        int i;
        ActivityStack stack;
        Configuration configuration = config;
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Activity idle: ");
            stringBuilder.append(token);
            Slog.v(str, stringBuilder.toString());
        } else {
            IBinder iBinder = token;
        }
        ArrayList<UserState> startingUsers = null;
        boolean booting = false;
        boolean activityRemoved = false;
        ActivityRecord r2 = ActivityRecord.forTokenLocked(token);
        if (r2 != null) {
            if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("activityIdleInternalLocked: Callers=");
                stringBuilder.append(Debug.getCallers(4));
                Slog.d(str, stringBuilder.toString());
            }
            this.mHandler.removeMessages(100, r2);
            r2.finishLaunchTickingLocked();
            if (fromTimeout) {
                r = r2;
                finishes = null;
                finishes2 = true;
                reportActivityLaunchedLocked(fromTimeout, r2, -1, -1);
            } else {
                r = r2;
                finishes = null;
                finishes2 = true;
            }
            if (configuration != null) {
                r.setLastReportedGlobalConfiguration(configuration);
            }
            r.idle = finishes2;
            if (r.app != null && r.app.foregroundActivities) {
                this.mService.mHwAMSEx.noteActivityStart(r.app.info.packageName, r.app.processName, r.realActivity != null ? r.realActivity.getClassName() : "NULL", r.app.pid, r.app.uid, false);
            }
            if (isFocusedStack(r.getStack()) || fromTimeout) {
                booting = checkFinishBootingLocked();
            }
        } else {
            r = r2;
            finishes = null;
            finishes2 = true;
        }
        int i2 = 0;
        if (allResumedActivitiesIdle()) {
            if (r != null) {
                this.mService.scheduleAppGcsLocked();
            }
            if (this.mLaunchingActivity.isHeld()) {
                this.mHandler.removeMessages(104);
                this.mLaunchingActivity.release();
            }
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        ArrayList<ActivityRecord> stops = processStoppingActivitiesLocked(r, finishes2, processPausingActivities);
        int NS = stops != null ? stops.size() : 0;
        int size = this.mFinishingActivities.size();
        int NF = size;
        if (size > 0) {
            finishes3 = new ArrayList(this.mFinishingActivities);
            this.mFinishingActivities.clear();
        } else {
            finishes3 = finishes;
        }
        if (this.mStartingUsers.size() > 0) {
            startingUsers = new ArrayList(this.mStartingUsers);
            this.mStartingUsers.clear();
        }
        for (i = 0; i < NS; i++) {
            r = (ActivityRecord) stops.get(i);
            stack = r.getStack();
            if (stack != null) {
                if (r.finishing) {
                    stack.finishCurrentActivityLocked(r, 0, false, "activityIdleInternalLocked");
                } else {
                    stack.stopActivityLocked(r);
                }
            }
        }
        for (i = 0; i < NF; i++) {
            r = (ActivityRecord) finishes3.get(i);
            stack = r.getStack();
            if (stack != null) {
                activityRemoved |= stack.destroyActivityLocked(r, finishes2, "finish-idle");
            }
        }
        if (!(booting || startingUsers == null)) {
            while (i2 < startingUsers.size()) {
                this.mService.mUserController.finishUserSwitch((UserState) startingUsers.get(i2));
                i2++;
            }
        }
        this.mService.trimApplications();
        if (activityRemoved) {
            resumeFocusedStackTopActivityLocked();
        }
        return r;
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        boolean hasVisibleActivities = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                hasVisibleActivities |= display.getChildAt(stackNdx).handleAppDiedLocked(app);
            }
        }
        return hasVisibleActivities;
    }

    void closeSystemDialogsLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).closeSystemDialogsLocked();
            }
        }
    }

    void removeUserLocked(int userId) {
        this.mUserStackInFront.delete(userId);
    }

    void updateUserStackLocked(int userId, ActivityStack stack) {
        if (userId != this.mCurrentUser) {
            this.mUserStackInFront.put(userId, stack != null ? stack.getStackId() : this.mHomeStack.mStackId);
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId) {
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                if (display.getChildAt(stackNdx).finishDisabledPackageActivitiesLocked(packageName, filterByClasses, doit, evenPersistent, userId)) {
                    didSomething = true;
                }
            }
        }
        return didSomething;
    }

    void updatePreviousProcessLocked(ActivityRecord r) {
        ProcessRecord fgApp = null;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            int stackNdx = display.getChildCount() - 1;
            while (stackNdx >= 0) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (isFocusedStack(stack)) {
                    ActivityRecord resumedActivity = stack.getResumedActivity();
                    if (resumedActivity != null) {
                        fgApp = resumedActivity.app;
                    } else if (stack.mPausingActivity != null) {
                        fgApp = stack.mPausingActivity.app;
                    }
                } else {
                    stackNdx--;
                }
            }
        }
        if (r.app != null && fgApp != null && r.app != fgApp && r.lastVisibleTime > this.mService.mPreviousProcessVisibleTime && r.app != this.mService.mHomeProcess) {
            this.mService.mPreviousProcess = r.app;
            this.mService.mPreviousProcessVisibleTime = r.lastVisibleTime;
            this.mService.mHwAMSEx.reportPreviousInfo(12, r.app);
        }
    }

    boolean resumeFocusedStackTopActivityLocked() {
        return resumeFocusedStackTopActivityLocked(null, null, null);
    }

    boolean resumeFocusedStackTopActivityLocked(ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
        if (!readyToResume()) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(ActivityManagerService.TAG, "It is not ready to resume");
            }
            return false;
        } else if (targetStack != null && isFocusedStack(targetStack) && !resumeAppLockActivityIfNeeded(targetStack, targetOptions)) {
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
        } else {
            ActivityRecord r = this.mFocusedStack.topRunningActivityLocked();
            if (r == null || !r.isState(ActivityState.RESUMED)) {
                if (!resumeAppLockActivityIfNeeded(this.mFocusedStack, targetOptions)) {
                    this.mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
                }
            } else if (r.isState(ActivityState.RESUMED)) {
                if (HwPCUtils.isPcCastModeInServer()) {
                    this.mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
                } else {
                    this.mFocusedStack.executeAppTransition(targetOptions);
                }
            }
            return false;
        }
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo aInfo) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).updateActivityApplicationInfoLocked(aInfo);
            }
        }
    }

    TaskRecord finishTopCrashedActivitiesLocked(ProcessRecord app, String reason) {
        TaskRecord finishedTask = null;
        ActivityStack focusedStack = getFocusedStack();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = 0; stackNdx < display.getChildCount(); stackNdx++) {
                ActivityStack stack = display.getChildAt(stackNdx);
                TaskRecord t = stack.finishTopCrashedActivityLocked(app, reason);
                if (stack == focusedStack || finishedTask == null) {
                    finishedTask = t;
                }
            }
        }
        return finishedTask;
    }

    void finishVoiceTask(IVoiceInteractionSession session) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            int numStacks = display.getChildCount();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                display.getChildAt(stackNdx).finishVoiceTask(session);
            }
        }
    }

    void findTaskToMoveToFront(TaskRecord task, int flags, ActivityOptions options, String reason, boolean forceNonResizeable) {
        TaskRecord taskRecord = task;
        ActivityOptions activityOptions = options;
        ActivityStack currentStack = task.getStack();
        String str;
        StringBuilder stringBuilder;
        if (currentStack == null) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findTaskToMoveToFront: can't move task=");
            stringBuilder.append(taskRecord);
            stringBuilder.append(" to front. Stack is null");
            Slog.e(str, stringBuilder.toString());
            return;
        }
        AppTimeTracker appTimeTracker;
        if ((flags & 2) == 0) {
            this.mUserLeaving = true;
        }
        ActivityRecord prev = topRunningActivityLocked();
        if ((flags & 1) != 0 || (prev != null && prev.isActivityTypeRecents())) {
            moveHomeStackToFront("findTaskToMoveToFront");
        }
        if (task.isResizeable() && canUseActivityOptionsLaunchBounds(activityOptions)) {
            Rect bounds = options.getLaunchBounds();
            taskRecord.updateOverrideConfiguration(bounds);
            ActivityStack stack = getLaunchStack(null, activityOptions, taskRecord, true);
            if (stack != currentStack) {
                taskRecord.reparent(stack, true, 1, false, true, "findTaskToMoveToFront");
                stack = currentStack;
            }
            if (stack.resizeStackWithLaunchBounds()) {
                resizeStackLocked(stack, bounds, null, null, false, true, null);
            } else {
                task.resizeWindowContainer();
            }
        }
        ActivityRecord r = task.getTopActivity();
        if (r == null) {
            appTimeTracker = null;
        } else {
            appTimeTracker = r.appTimeTracker;
        }
        currentStack.moveTaskToFrontLocked(taskRecord, false, activityOptions, appTimeTracker, reason);
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findTaskToMoveToFront: moved to front of stack=");
            stringBuilder.append(currentStack);
            Slog.d(str, stringBuilder.toString());
        }
        handleNonResizableTaskIfNeeded(taskRecord, 0, 0, currentStack, forceNonResizeable);
    }

    boolean canUseActivityOptionsLaunchBounds(ActivityOptions options) {
        boolean z = false;
        if (options == null || options.getLaunchBounds() == null) {
            return false;
        }
        if ((this.mService.mSupportsPictureInPicture && options.getLaunchWindowingMode() == 2) || this.mService.mSupportsFreeformWindowManagement) {
            z = true;
        }
        return z;
    }

    LaunchParamsController getLaunchParamsController() {
        return this.mLaunchParamsController;
    }

    protected <T extends ActivityStack> T getStack(int stackId) {
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            T stack = ((ActivityDisplay) this.mActivityDisplays.valueAt(i)).getStack(stackId);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    private <T extends ActivityStack> T getStack(int windowingMode, int activityType) {
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            T stack = ((ActivityDisplay) this.mActivityDisplays.valueAt(i)).getStack(windowingMode, activityType);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    int resolveActivityType(ActivityRecord r, ActivityOptions options, TaskRecord task) {
        int activityType = r != null ? r.getActivityType() : 0;
        if (activityType == 0 && task != null) {
            activityType = task.getActivityType();
        }
        if (activityType != 0) {
            return activityType;
        }
        if (options != null) {
            activityType = options.getLaunchActivityType();
        }
        return activityType != 0 ? activityType : 1;
    }

    <T extends ActivityStack> T getLaunchStack(ActivityRecord r, ActivityOptions options, TaskRecord candidateTask, boolean onTop) {
        return getLaunchStack(r, options, candidateTask, onTop, -1);
    }

    <T extends ActivityStack> T getLaunchStack(ActivityRecord r, ActivityOptions options, TaskRecord candidateTask, boolean onTop, int candidateDisplayId) {
        boolean z;
        T stack;
        ActivityDisplay display;
        ActivityRecord activityRecord = r;
        ActivityOptions activityOptions = options;
        TaskRecord taskRecord = candidateTask;
        int taskId = -1;
        int displayId = -1;
        if (activityOptions != null) {
            taskId = options.getLaunchTaskId();
            displayId = options.getLaunchDisplayId();
        }
        int taskId2 = taskId;
        if (taskId2 != -1) {
            activityOptions.setLaunchTaskId(-1);
            z = onTop;
            TaskRecord task = anyTaskForIdLocked(taskId2, 2, activityOptions, z);
            activityOptions.setLaunchTaskId(taskId2);
            if (task != null) {
                return task.getStack();
            }
        }
        z = onTop;
        int activityType = resolveActivityType(r, options, candidateTask);
        T stack2 = null;
        if (displayId == -1) {
            displayId = candidateDisplayId;
        }
        int displayId2 = displayId;
        if (displayId2 != -1 && canLaunchOnDisplay(activityRecord, displayId2)) {
            if (activityRecord != null) {
                stack2 = getValidLaunchStackOnDisplay(displayId2, activityRecord);
                if (stack2 != null) {
                    return stack2;
                }
            }
            stack = stack2;
            display = getActivityDisplayOrCreateLocked(displayId2);
            if (display != null) {
                stack2 = display.getOrCreateStack(activityRecord, activityOptions, taskRecord, activityType, z);
                if (stack2 != null) {
                    return stack2;
                }
            }
        }
        T stack3 = null;
        ActivityDisplay display2 = null;
        if (taskRecord != null) {
            stack3 = candidateTask.getStack();
        }
        if (stack3 == null && activityRecord != null) {
            stack3 = r.getStack();
        }
        stack = stack3;
        if (stack != null) {
            display2 = stack.getDisplay();
            if (display2 != null && canLaunchOnDisplay(activityRecord, display2.mDisplayId)) {
                taskId = display2.resolveWindowingMode(activityRecord, activityOptions, taskRecord, activityType);
                if (stack.isCompatible(taskId, activityType)) {
                    return stack;
                }
                if (taskId == 4 && display2.getSplitScreenPrimaryStack() == stack && taskRecord == stack.topTask()) {
                    return stack;
                }
            }
        }
        if (display2 != null && canLaunchOnDisplay(activityRecord, display2.mDisplayId) && (activityType == 1 || activityType == 0)) {
            display = display2;
        } else {
            display = getDefaultDisplay();
        }
        return display.getOrCreateStack(activityRecord, activityOptions, taskRecord, activityType, z);
    }

    private boolean canLaunchOnDisplay(ActivityRecord r, int displayId) {
        if (r == null) {
            return true;
        }
        return r.canBeLaunchedOnDisplay(displayId);
    }

    protected ActivityStack getValidLaunchStackOnDisplay(int displayId, ActivityRecord r) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Display with displayId=");
            stringBuilder.append(displayId);
            stringBuilder.append(" not found.");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!r.canBeLaunchedOnDisplay(displayId)) {
            return null;
        } else {
            for (int i = activityDisplay.getChildCount() - 1; i >= 0; i--) {
                ActivityStack stack = activityDisplay.getChildAt(i);
                if (isValidLaunchStack(stack, displayId, r)) {
                    return stack;
                }
            }
            if (displayId != 0) {
                return activityDisplay.createStack(r.getWindowingMode(), r.getActivityType(), true);
            }
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getValidLaunchStackOnDisplay: can't launch on displayId ");
            stringBuilder2.append(displayId);
            Slog.w(str, stringBuilder2.toString());
            return null;
        }
    }

    private boolean isValidLaunchStack(ActivityStack stack, int displayId, ActivityRecord r) {
        switch (stack.getActivityType()) {
            case 2:
                return r.isActivityTypeHome();
            case 3:
                return r.isActivityTypeRecents();
            case 4:
                return r.isActivityTypeAssistant();
            default:
                switch (stack.getWindowingMode()) {
                    case 1:
                        return true;
                    case 2:
                        return r.supportsPictureInPicture();
                    case 3:
                        return r.supportsSplitScreenWindowingMode();
                    case 4:
                        return r.supportsSplitScreenWindowingMode();
                    case 5:
                        return r.supportsFreeform();
                    default:
                        if (HwPCUtils.isPcDynamicStack(stack.getStackId()) && this.mService.mSupportsMultiDisplay && HwPCUtils.isPcCastModeInServer()) {
                            return true;
                        }
                        if (!stack.isOnHomeDisplay()) {
                            return r.canBeLaunchedOnDisplay(displayId);
                        }
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("isValidLaunchStack: Unexpected stack=");
                        stringBuilder.append(stack);
                        Slog.e(str, stringBuilder.toString());
                        return false;
                }
        }
    }

    ActivityStack getNextFocusableStackLocked(ActivityStack currentFocus, boolean ignoreCurrent) {
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        int currentWindowingMode = currentFocus != null ? currentFocus.getWindowingMode() : 0;
        ActivityStack candidate = null;
        this.mHwActivityStackSupervisorEx.adjustFocusDisplayOrder(this.mTmpOrderedDisplayIds, HwPCUtils.getPCDisplayID());
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            int displayId = this.mTmpOrderedDisplayIds.get(i);
            if (HwVRUtils.isVRMode() && displayId == 0) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getNextFocusableStackLocked, is VR mode, dispalyId  ");
                stringBuilder.append(displayId);
                stringBuilder.append(" continue");
                Slog.e(str, stringBuilder.toString());
            } else {
                ActivityDisplay display = getActivityDisplayOrCreateLocked(displayId);
                if (display == null) {
                    continue;
                } else {
                    for (int j = display.getChildCount() - 1; j >= 0; j--) {
                        ActivityStack stack = display.getChildAt(j);
                        if (!(ignoreCurrent && stack == currentFocus) && stack.isFocusable() && (stack.shouldBeVisible(null) || HwVRUtils.isVRDynamicStack(stack.getStackId()))) {
                            if (currentWindowingMode == 4 && candidate == null && stack.inSplitScreenPrimaryWindowingMode()) {
                                candidate = stack;
                            } else if (candidate == null || !stack.inSplitScreenSecondaryWindowingMode()) {
                                return stack;
                            } else {
                                return candidate;
                            }
                        }
                    }
                    continue;
                }
            }
        }
        return candidate;
    }

    ActivityStack getNextValidLaunchStackLocked(ActivityRecord r, int currentFocus) {
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int i = this.mTmpOrderedDisplayIds.size() - 1; i >= 0; i--) {
            int displayId = this.mTmpOrderedDisplayIds.get(i);
            if (displayId != currentFocus) {
                ActivityStack stack = getValidLaunchStackOnDisplay(displayId, r);
                if (stack != null) {
                    return stack;
                }
            }
        }
        return null;
    }

    ActivityRecord getHomeActivity() {
        return getHomeActivityForUser(this.mCurrentUser);
    }

    ActivityRecord getHomeActivityForUser(int userId) {
        ArrayList<TaskRecord> tasks = this.mHomeStack.getAllTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) tasks.get(taskNdx);
            if (task.isActivityTypeHome()) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                    if (r.isActivityTypeHome() && (userId == -1 || r.userId == userId)) {
                        return r;
                    }
                }
                continue;
            }
        }
        return null;
    }

    void resizeStackLocked(ActivityStack stack, Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds, boolean preserveWindows, boolean allowResizeInDockedMode, boolean deferResume) {
        Throwable th;
        ActivityStack activityStack = stack;
        if (activityStack.inSplitScreenPrimaryWindowingMode()) {
            resizeDockedStackLocked(bounds, tempTaskBounds, tempTaskInsetBounds, null, null, preserveWindows, deferResume);
            return;
        }
        boolean splitScreenActive = getDefaultDisplay().hasSplitScreenPrimaryStack();
        if (allowResizeInDockedMode || activityStack.getWindowConfiguration().tasksAreFloating() || !splitScreenActive) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("am.resizeStack_");
            stringBuilder.append(activityStack.mStackId);
            Trace.traceBegin(64, stringBuilder.toString());
            this.mWindowManager.deferSurfaceLayout();
            boolean z;
            try {
                if (activityStack.affectedBySplitScreenResize()) {
                    if (bounds == null && activityStack.inSplitScreenWindowingMode()) {
                        activityStack.setWindowingMode(1);
                    } else if (splitScreenActive) {
                        activityStack.setWindowingMode(4);
                    }
                }
                activityStack.resize(bounds, tempTaskBounds, tempTaskInsetBounds);
                if (deferResume) {
                    z = preserveWindows;
                } else {
                    try {
                        activityStack.ensureVisibleActivitiesConfigurationLocked(activityStack.topRunningActivityLocked(), preserveWindows);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            } catch (Throwable th3) {
                th = th3;
                z = preserveWindows;
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
                throw th;
            }
        }
    }

    void deferUpdateRecentsHomeStackBounds() {
        deferUpdateBounds(3);
        deferUpdateBounds(2);
    }

    void deferUpdateBounds(int activityType) {
        ActivityStack stack = getStack(null, activityType);
        if (stack != null) {
            stack.deferUpdateBounds();
        }
    }

    void continueUpdateRecentsHomeStackBounds() {
        continueUpdateBounds(3);
        continueUpdateBounds(2);
    }

    void continueUpdateBounds(int activityType) {
        ActivityStack stack = getStack(null, activityType);
        if (stack != null) {
            stack.continueUpdateBounds();
        }
    }

    void notifyAppTransitionDone() {
        continueUpdateRecentsHomeStackBounds();
        for (int i = this.mResizingTasksDuringAnimation.size() - 1; i >= 0; i--) {
            TaskRecord task = anyTaskForIdLocked(((Integer) this.mResizingTasksDuringAnimation.valueAt(i)).intValue(), 0);
            if (task != null) {
                try {
                    task.setTaskDockedResizing(false);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        this.mResizingTasksDuringAnimation.clear();
    }

    private void moveTasksToFullscreenStackInSurfaceTransaction(ActivityStack fromStack, int toDisplayId, boolean onTop) {
        ActivityStack activityStack = fromStack;
        this.mWindowManager.deferSurfaceLayout();
        try {
            int i;
            int windowingMode = fromStack.getWindowingMode();
            boolean inPinnedWindowingMode = windowingMode == 2;
            ActivityDisplay toDisplay = getActivityDisplay(toDisplayId);
            if (windowingMode == 3) {
                toDisplay.onExitingSplitScreenMode();
                int i2 = toDisplay.getChildCount() - 1;
                while (true) {
                    i = i2;
                    if (i < 0) {
                        break;
                    }
                    ActivityStack otherStack = toDisplay.getChildAt(i);
                    if (otherStack.inSplitScreenSecondaryWindowingMode()) {
                        resizeStackLocked(otherStack, null, null, null, true, true, 1);
                    }
                    i2 = i - 1;
                }
                this.mAllowDockedStackResize = false;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The dock stack was dismissed! With fromStack = ");
                stringBuilder.append(activityStack);
                Flog.i(101, stringBuilder.toString());
                activityStack.mWindowContainerController.resetBounds();
            }
            boolean schedulePictureInPictureModeChange = inPinnedWindowingMode;
            ArrayList<TaskRecord> tasks = fromStack.getAllTasks();
            if (!tasks.isEmpty()) {
                this.mTmpOptions.setLaunchWindowingMode(1);
                int size = tasks.size();
                int i3 = 0;
                while (true) {
                    i = i3;
                    if (i >= size) {
                        break;
                    }
                    ActivityDisplay toDisplay2;
                    TaskRecord task = (TaskRecord) tasks.get(i);
                    TaskRecord task2 = task;
                    int i4 = i;
                    ActivityStack toStack = toDisplay.getOrCreateStack(null, this.mTmpOptions, task, task.getActivityType(), onTop);
                    if (onTop) {
                        toDisplay2 = toDisplay;
                        task2.reparent(toStack, true, 0, i4 == size + -1, true, schedulePictureInPictureModeChange, "moveTasksToFullscreenStack - onTop");
                        TaskRecord task3 = task2;
                        MetricsLoggerWrapper.logPictureInPictureFullScreen(this.mService.mContext, task3.effectiveUid, task3.realActivity.flattenToString());
                    } else {
                        toDisplay2 = toDisplay;
                        task2.reparent(toStack, true, 2, false, true, schedulePictureInPictureModeChange, "moveTasksToFullscreenStack - NOT_onTop");
                    }
                    int i5 = toDisplayId;
                    i3 = i4 + 1;
                    toDisplay = toDisplay2;
                }
            }
            ensureActivitiesVisibleLocked(null, 0, true);
            resumeFocusedStackTopActivityLocked();
        } finally {
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void moveTasksToFullscreenStackLocked(ActivityStack fromStack, boolean onTop) {
        moveTasksToFullscreenStackLocked(fromStack, 0, onTop);
    }

    void moveTasksToFullscreenStackLocked(ActivityStack fromStack, int toDisplayId, boolean onTop) {
        this.mWindowManager.inSurfaceTransaction(new -$$Lambda$ActivityStackSupervisor$2EfPspQe887pLmnBFuHkVjyLdzE(this, fromStack, toDisplayId, onTop));
    }

    void setSplitScreenResizing(boolean resizing) {
        if (resizing != this.mDockedStackResizing) {
            this.mDockedStackResizing = resizing;
            this.mWindowManager.setDockedStackResizing(resizing);
            if (!resizing && this.mHasPendingDockedBounds) {
                resizeDockedStackLocked(this.mPendingDockedBounds, this.mPendingTempDockedTaskBounds, this.mPendingTempDockedTaskInsetBounds, this.mPendingTempOtherTaskBounds, this.mPendingTempOtherTaskInsetBounds, true);
                this.mHasPendingDockedBounds = false;
                this.mPendingDockedBounds = null;
                this.mPendingTempDockedTaskBounds = null;
                this.mPendingTempDockedTaskInsetBounds = null;
                this.mPendingTempOtherTaskBounds = null;
                this.mPendingTempOtherTaskInsetBounds = null;
            }
        }
    }

    void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows) {
        resizeDockedStackLocked(dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds, tempOtherTaskBounds, tempOtherTaskInsetBounds, preserveWindows, false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x00fa  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00f2  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows, boolean deferResume) {
        Throwable th;
        Rect rect = dockedBounds;
        if (this.mAllowDockedStackResize) {
            ActivityStack stack = getDefaultDisplay().getSplitScreenPrimaryStack();
            if (stack == null) {
                Slog.w(ActivityManagerService.TAG, "resizeDockedStackLocked: docked stack not found");
                return;
            }
            if (this.mDockedStackResizing) {
                this.mHasPendingDockedBounds = true;
                this.mPendingDockedBounds = Rect.copyOrNull(dockedBounds);
                this.mPendingTempDockedTaskBounds = Rect.copyOrNull(tempDockedTaskBounds);
                this.mPendingTempDockedTaskInsetBounds = Rect.copyOrNull(tempDockedTaskInsetBounds);
                this.mPendingTempOtherTaskBounds = Rect.copyOrNull(tempOtherTaskBounds);
                this.mPendingTempOtherTaskInsetBounds = Rect.copyOrNull(tempOtherTaskInsetBounds);
            }
            Trace.traceBegin(64, "am.resizeDockedStack");
            this.mWindowManager.deferSurfaceLayout();
            boolean z;
            try {
                this.mAllowDockedStackResize = false;
                ActivityRecord r = stack.topRunningActivityLocked();
                try {
                    stack.resize(rect, tempDockedTaskBounds, tempDockedTaskInsetBounds);
                    if (stack.getWindowingMode() != 1) {
                        if (rect != null || stack.isAttached()) {
                            ActivityDisplay display = getDefaultDisplay();
                            Rect otherTaskRect = new Rect();
                            int i = display.getChildCount() - 1;
                            while (true) {
                                int i2 = i;
                                if (i2 < 0) {
                                    break;
                                }
                                int i3;
                                Rect otherTaskRect2;
                                ActivityDisplay display2;
                                Rect rect2;
                                ActivityStack current = display.getChildAt(i2);
                                if (current.getWindowingMode() != 3) {
                                    if (current.affectedBySplitScreenResize()) {
                                        if (!this.mDockedStackResizing || current.isTopActivityVisible()) {
                                            current.setWindowingMode(4);
                                            Rect rect3 = tempOtherTaskBounds;
                                            current.getStackDockedModeBounds(rect3, this.tempRect, otherTaskRect, true);
                                            Rect rect4 = !this.tempRect.isEmpty() ? this.tempRect : null;
                                            Rect rect5 = !otherTaskRect.isEmpty() ? otherTaskRect : rect3;
                                            ActivityStack activityStack = current;
                                            rect3 = rect4;
                                            Rect rect6 = rect5;
                                            i3 = i2;
                                            otherTaskRect2 = otherTaskRect;
                                            display2 = display;
                                            resizeStackLocked(activityStack, rect3, rect6, tempOtherTaskInsetBounds, preserveWindows, true, deferResume);
                                            i = i3 - 1;
                                            rect2 = tempDockedTaskInsetBounds;
                                            otherTaskRect = otherTaskRect2;
                                            display = display2;
                                        }
                                    }
                                }
                                i3 = i2;
                                otherTaskRect2 = otherTaskRect;
                                display2 = display;
                                i = i3 - 1;
                                rect2 = tempDockedTaskInsetBounds;
                                otherTaskRect = otherTaskRect2;
                                display = display2;
                            }
                            if (deferResume) {
                                try {
                                    stack.ensureVisibleActivitiesConfigurationLocked(r, preserveWindows);
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } else {
                                z = preserveWindows;
                            }
                            this.mAllowDockedStackResize = true;
                            this.mWindowManager.continueSurfaceLayout();
                            Trace.traceEnd(64);
                        }
                    }
                    moveTasksToFullscreenStackLocked(stack, true);
                    r = null;
                    if (deferResume) {
                    }
                    this.mAllowDockedStackResize = true;
                    this.mWindowManager.continueSurfaceLayout();
                    Trace.traceEnd(64);
                } catch (Throwable th3) {
                    th = th3;
                    z = preserveWindows;
                    this.mAllowDockedStackResize = true;
                    this.mWindowManager.continueSurfaceLayout();
                    Trace.traceEnd(64);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                Rect rect7 = tempDockedTaskBounds;
                z = preserveWindows;
                this.mAllowDockedStackResize = true;
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
                throw th;
            }
        }
    }

    void resizePinnedStackLocked(Rect pinnedBounds, Rect tempPinnedTaskBounds) {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            HwPCUtils.log(ActivityManagerService.TAG, "ignore pinned stack in pad pc mode");
            return;
        }
        PinnedActivityStack stack = getDefaultDisplay().getPinnedStack();
        if (stack == null) {
            Slog.w(ActivityManagerService.TAG, "resizePinnedStackLocked: pinned stack not found");
        } else if (!((PinnedStackWindowController) stack.getWindowContainerController()).pinnedStackResizeDisallowed()) {
            Trace.traceBegin(64, "am.resizePinnedStack");
            this.mWindowManager.deferSurfaceLayout();
            try {
                ActivityRecord r = stack.topRunningActivityLocked();
                Rect insetBounds = null;
                if (tempPinnedTaskBounds != null && stack.isAnimatingBoundsToFullscreen()) {
                    insetBounds = this.tempRect;
                    insetBounds.top = 0;
                    insetBounds.left = 0;
                    insetBounds.right = tempPinnedTaskBounds.width();
                    insetBounds.bottom = tempPinnedTaskBounds.height();
                }
                if (pinnedBounds != null && tempPinnedTaskBounds == null) {
                    stack.onPipAnimationEndResize();
                }
                stack.resize(pinnedBounds, tempPinnedTaskBounds, insetBounds);
                stack.ensureVisibleActivitiesConfigurationLocked(r, false);
            } finally {
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
            }
        }
    }

    private void removeStackInSurfaceTransaction(ActivityStack stack) {
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        if (stack.getWindowingMode() == 2) {
            PinnedActivityStack pinnedStack = (PinnedActivityStack) stack;
            pinnedStack.mForceHidden = true;
            pinnedStack.ensureActivitiesVisibleLocked(null, 0, true);
            pinnedStack.mForceHidden = false;
            activityIdleInternalLocked(null, false, true, null);
            moveTasksToFullscreenStackLocked(pinnedStack, false);
            return;
        }
        for (int i = tasks.size() - 1; i >= 0; i--) {
            removeTaskByIdLocked(((TaskRecord) tasks.get(i)).taskId, true, true, "remove-stack");
        }
    }

    void removeStack(ActivityStack stack) {
        this.mWindowManager.inSurfaceTransaction(new -$$Lambda$ActivityStackSupervisor$x0Vocp-itdO3YPTBM6d_k8Yij7g(this, stack));
    }

    void removeStacksInWindowingModes(int... windowingModes) {
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ((ActivityDisplay) this.mActivityDisplays.valueAt(i)).removeStacksInWindowingModes(windowingModes);
        }
    }

    void removeStacksWithActivityTypes(int... activityTypes) {
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ((ActivityDisplay) this.mActivityDisplays.valueAt(i)).removeStacksWithActivityTypes(activityTypes);
        }
    }

    boolean removeTaskByIdLocked(int taskId, boolean killProcess, boolean removeFromRecents, String reason) {
        return removeTaskByIdLocked(taskId, killProcess, removeFromRecents, false, reason);
    }

    boolean removeTaskByIdLocked(int taskId, boolean killProcess, boolean removeFromRecents, boolean pauseImmediately, String reason) {
        TaskRecord tr = anyTaskForIdLocked(taskId, 1);
        String str;
        if (tr != null) {
            if (!(tr.getBaseIntent() == null || tr.getBaseIntent().getComponent() == null)) {
                String packageName = tr.getBaseIntent().getComponent().getPackageName();
                StringBuilder stringBuilder;
                if (HwDeviceManager.disallowOp(3, packageName)) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    stringBuilder.append(packageName);
                    stringBuilder.append("] is Persistent app,won't be killed");
                    Slog.i(str, stringBuilder.toString());
                    UiThread.getHandler().post(new Runnable() {
                        public void run() {
                            Context context = ActivityStackSupervisor.this.mService.mUiContext;
                            if (context != null) {
                                Toast toast = Toast.makeText(context, context.getString(33686102), 0);
                                LayoutParams windowParams = toast.getWindowParams();
                                windowParams.privateFlags |= 16;
                                toast.show();
                            }
                        }
                    });
                    return false;
                } else if (HwPCUtils.isPcCastModeInServer()) {
                    if (HwPCUtils.enabledInPad() && "com.android.incallui".equals(packageName)) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[");
                        stringBuilder.append(packageName);
                        stringBuilder.append("] is Service app,won't be killed");
                        Slog.i(str, stringBuilder.toString());
                        return false;
                    } else if (!killProcess && "com.chinamworld.main".equals(packageName)) {
                        String str2 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(packageName);
                        stringBuilder2.append("] remove task and kill process in pc mode");
                        Slog.i(str2, stringBuilder2.toString());
                        tr.removeTaskActivitiesLocked(pauseImmediately, reason);
                        cleanUpRemovedTaskLocked(tr, true, removeFromRecents);
                        if (tr.isPersistable) {
                            this.mService.notifyTaskPersisterLocked(null, true);
                        }
                        return true;
                    }
                }
            }
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && tr.getStack() != null) {
                tr.getStack().resetOtherStacksVisible(true);
            }
            tr.removeTaskActivitiesLocked(pauseImmediately, reason);
            cleanUpRemovedTaskLocked(tr, killProcess, removeFromRecents);
            this.mService.getLockTaskController().clearLockedTask(tr);
            if (tr.isPersistable) {
                this.mService.notifyTaskPersisterLocked(null, true);
            }
            return true;
        }
        str = ActivityManagerService.TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Request to remove task ignored for non-existent task ");
        stringBuilder3.append(taskId);
        Slog.w(str, stringBuilder3.toString());
        return false;
    }

    void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess, boolean removeFromRecents) {
        TaskRecord taskRecord = tr;
        if (removeFromRecents) {
            this.mRecentTasks.remove(taskRecord);
        }
        ComponentName component = tr.getBaseIntent().getComponent();
        String str;
        if (component == null) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No component for base intent of task: ");
            stringBuilder.append(taskRecord);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        this.mService.mServices.cleanUpRemovedTaskLocked(taskRecord, component, new Intent(tr.getBaseIntent()));
        if (killProcess) {
            String pkg = component.getPackageName();
            if (!shouldNotKillProcWhenRemoveTask(pkg)) {
                int i;
                if (this.mService.getRecordCust() != null) {
                    this.mService.getRecordCust().appExitRecord(pkg, "rkill");
                }
                ArrayList<ProcessRecord> procsToKill = new ArrayList();
                ArrayMap<String, SparseArray<ProcessRecord>> pmap = this.mService.mProcessNames.getMap();
                for (i = 0; i < pmap.size(); i++) {
                    SparseArray<ProcessRecord> uids = (SparseArray) pmap.valueAt(i);
                    for (int j = 0; j < uids.size(); j++) {
                        ProcessRecord proc = (ProcessRecord) uids.valueAt(j);
                        if (HwDeviceManager.disallowOp(22, proc.info.packageName)) {
                            String str2 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("[");
                            stringBuilder2.append(proc.info.packageName);
                            stringBuilder2.append("] is super-whitelist app,won't be killed by remove task");
                            Slog.i(str2, stringBuilder2.toString());
                        } else if (proc.userId == taskRecord.userId && proc != this.mService.mHomeProcess && proc.pkgList.containsKey(pkg)) {
                            int k = 0;
                            while (k < proc.activities.size()) {
                                TaskRecord otherTask = ((ActivityRecord) proc.activities.get(k)).getTask();
                                if (otherTask == null || taskRecord.taskId == otherTask.taskId || !otherTask.inRecents) {
                                    k++;
                                } else {
                                    return;
                                }
                            }
                            if (!proc.foregroundServices) {
                                procsToKill.add(proc);
                            } else {
                                return;
                            }
                        }
                    }
                }
                int i2 = 0;
                while (true) {
                    i = i2;
                    if (i < procsToKill.size()) {
                        ProcessRecord pr = (ProcessRecord) procsToKill.get(i);
                        if (pr != null) {
                            if (pr.curAdj >= 900 && (pr.info.flags & 1) != 0 && (pr.info.hwFlags & DumpState.DUMP_HANDLE) == 0 && notKillProcessWhenRemoveTask(pr)) {
                                str = ActivityManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(" the process ");
                                stringBuilder3.append(pr.processName);
                                stringBuilder3.append(" adj >= ");
                                stringBuilder3.append(900);
                                Slog.d(str, stringBuilder3.toString());
                                try {
                                    SmartShrinker.reclaim(pr.pid, 4);
                                    if (pr.thread != null) {
                                        pr.thread.scheduleTrimMemory(80);
                                    }
                                } catch (RemoteException e) {
                                }
                                pr.trimMemoryLevel = 80;
                            } else if (pr.setSchedGroup == 0 && pr.curReceivers.isEmpty()) {
                                pr.kill("remove task", true);
                            } else {
                                pr.waitingToKill = "remove task";
                            }
                        }
                        i2 = i + 1;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    boolean restoreRecentTaskLocked(TaskRecord task, ActivityOptions aOptions, boolean onTop) {
        ActivityStack stack = getLaunchStack(null, aOptions, task, onTop);
        ActivityStack currentStack = task.getStack();
        if (currentStack != null) {
            if (currentStack == stack) {
                return true;
            }
            currentStack.removeTask(task, "restoreRecentTaskLocked", 1);
        }
        stack.addTask(task, onTop, "restoreRecentTask");
        task.createWindowContainer(onTop, true);
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Added restored task=");
            stringBuilder.append(task);
            stringBuilder.append(" to stack=");
            stringBuilder.append(stack);
            Slog.v(str, stringBuilder.toString());
        }
        ArrayList<ActivityRecord> activities = task.mActivities;
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            ((ActivityRecord) activities.get(activityNdx)).createWindowContainer(((ActivityRecord) activities.get(activityNdx)).info.navigationHide);
        }
        return true;
    }

    public void onRecentTaskAdded(TaskRecord task) {
        task.touchActiveTime();
    }

    public void onRecentTaskRemoved(TaskRecord task, boolean wasTrimmed) {
        if (wasTrimmed) {
            removeTaskByIdLocked(task.taskId, false, false, false, "recent-task-trimmed");
        }
        task.removedFromRecents();
    }

    void moveStackToDisplayLocked(int stackId, int displayId, boolean onTop) {
        ActivityDisplay activityDisplay = getActivityDisplayOrCreateLocked(displayId);
        if (activityDisplay != null) {
            ActivityStack stack = getStack(stackId);
            if (stack != null) {
                ActivityDisplay currentDisplay = stack.getDisplay();
                StringBuilder stringBuilder;
                if (currentDisplay == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("moveStackToDisplayLocked: Stack with stack=");
                    stringBuilder.append(stack);
                    stringBuilder.append(" is not attached to any display.");
                    throw new IllegalStateException(stringBuilder.toString());
                } else if (currentDisplay.mDisplayId != displayId) {
                    stack.reparent(activityDisplay, onTop);
                    return;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Trying to move stack=");
                    stringBuilder.append(stack);
                    stringBuilder.append(" to its current displayId=");
                    stringBuilder.append(displayId);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("moveStackToDisplayLocked: Unknown stackId=");
            stringBuilder2.append(stackId);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("moveStackToDisplayLocked: Unknown displayId=");
        stringBuilder3.append(displayId);
        throw new IllegalArgumentException(stringBuilder3.toString());
    }

    ActivityStack getReparentTargetStack(TaskRecord task, ActivityStack stack, boolean toTop) {
        ActivityStack prevStack = task.getStack();
        int stackId = stack.mStackId;
        boolean inMultiWindowMode = stack.inMultiWindowMode();
        String str;
        StringBuilder stringBuilder;
        if (prevStack != null && prevStack.mStackId == stackId) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can not reparent to same stack, task=");
            stringBuilder.append(task);
            stringBuilder.append(" already in stackId=");
            stringBuilder.append(stackId);
            Slog.w(str, stringBuilder.toString());
            return prevStack;
        } else if (inMultiWindowMode && !this.mService.mSupportsMultiWindow) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Device doesn't support multi-window, can not reparent task=");
            stringBuilder.append(task);
            stringBuilder.append(" to stack=");
            stringBuilder.append(stack);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (stack.mDisplayId != 0 && !this.mService.mSupportsMultiDisplay) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Device doesn't support multi-display, can not reparent task=");
            stringBuilder.append(task);
            stringBuilder.append(" to stackId=");
            stringBuilder.append(stackId);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (stack.getWindowingMode() != 5 || this.mService.mSupportsFreeformWindowManagement) {
            if (inMultiWindowMode && !task.isResizeable()) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can not move unresizeable task=");
                stringBuilder.append(task);
                stringBuilder.append(" to multi-window stack=");
                stringBuilder.append(stack);
                stringBuilder.append(" Moving to a fullscreen stack instead.");
                Slog.w(str, stringBuilder.toString());
                if (prevStack != null) {
                    return prevStack;
                }
                stack = stack.getDisplay().createStack(1, stack.getActivityType(), toTop);
            }
            return stack;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Device doesn't support freeform, can not reparent task=");
            stringBuilder.append(task);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    boolean moveTopStackActivityToPinnedStackLocked(int stackId, Rect destBounds) {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) {
            HwPCUtils.log(ActivityManagerService.TAG, "ignore moveTopStackActivityToPinnedStackLocked in pad pc mode");
            return false;
        }
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            ActivityRecord r = stack.topRunningActivityLocked();
            String str;
            StringBuilder stringBuilder;
            if (r == null) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("moveTopStackActivityToPinnedStackLocked: No top running activity in stack=");
                stringBuilder.append(stack);
                Slog.w(str, stringBuilder.toString());
                return false;
            } else if (this.mService.mForceResizableActivities || r.supportsPictureInPicture()) {
                moveActivityToPinnedStackLocked(r, null, 0.0f, "moveTopActivityToPinnedStack");
                return true;
            } else {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("moveTopStackActivityToPinnedStackLocked: Picture-In-Picture not supported for  r=");
                stringBuilder.append(r);
                Slog.w(str, stringBuilder.toString());
                return false;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("moveTopStackActivityToPinnedStackLocked: Unknown stackId=");
        stringBuilder2.append(stackId);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    void moveActivityToPinnedStackLocked(ActivityRecord r, Rect sourceHintBounds, float aspectRatio, String reason) {
        Throwable th;
        Rect rect;
        ActivityStack activityStack;
        Rect stack;
        ActivityRecord activityRecord = r;
        this.mWindowManager.deferSurfaceLayout();
        ActivityDisplay display;
        Rect destBounds;
        try {
            ActivityStack stack2;
            Rect destBounds2;
            display = r.getStack().getDisplay();
            try {
                PinnedActivityStack destBounds3 = display.getPinnedStack();
                if (destBounds3 != null) {
                    moveTasksToFullscreenStackLocked(destBounds3, false);
                }
                stack2 = (PinnedActivityStack) display.getOrCreateStack(2, r.getActivityType(), true);
                try {
                    destBounds2 = stack2.getDefaultPictureInPictureBounds(aspectRatio);
                } catch (Throwable th2) {
                    th = th2;
                    rect = sourceHintBounds;
                    activityStack = stack2;
                    destBounds = null;
                    this.mWindowManager.continueSurfaceLayout();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                rect = sourceHintBounds;
                stack = null;
                destBounds = null;
                this.mWindowManager.continueSurfaceLayout();
                throw th;
            }
            try {
                PinnedActivityStack stack3;
                TaskRecord task = r.getTask();
                TaskRecord task2 = task;
                resizeStackLocked(stack2, task.getOverrideBounds(), null, null, false, true, null);
                if (task2.mActivities.size() == 1) {
                    destBounds = destBounds2;
                    stack3 = stack2;
                    try {
                        task2.reparent(stack2, true, 0, false, true, false, reason);
                    } catch (Throwable th4) {
                        th = th4;
                        rect = sourceHintBounds;
                        this.mWindowManager.continueSurfaceLayout();
                        throw th;
                    }
                }
                destBounds = destBounds2;
                stack3 = stack2;
                TaskRecord newTask = task2.getStack().createTaskRecord(getNextTaskIdForUserLocked(activityRecord.userId), activityRecord.info, activityRecord.intent, null, null, true);
                activityRecord.reparent(newTask, HwBootFail.STAGE_BOOT_SUCCESS, "moveActivityToStack");
                newTask.reparent((ActivityStack) stack3, true, 0, false, true, false, reason);
                activityRecord.supportsEnterPipOnTaskSwitch = false;
                this.mWindowManager.continueSurfaceLayout();
                stack3.animateResizePinnedStack(sourceHintBounds, destBounds, -1, true);
                ensureActivitiesVisibleLocked(null, 0, false);
                resumeFocusedStackTopActivityLocked();
                this.mService.mTaskChangeNotificationController.notifyActivityPinned(activityRecord);
                LogPower.push(NetdResponseCode.DnsProxyQueryResult, activityRecord.packageName);
            } catch (Throwable th5) {
                th = th5;
                rect = sourceHintBounds;
                activityStack = stack2;
                this.mWindowManager.continueSurfaceLayout();
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            rect = sourceHintBounds;
            display = null;
            stack = null;
            destBounds = null;
            this.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    boolean moveFocusableActivityStackToFrontLocked(ActivityRecord r, String reason) {
        if (r == null || !r.isFocusable()) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("moveActivityStackToFront: unfocusable r=");
                stringBuilder.append(r);
                Slog.d(str, stringBuilder.toString());
            }
            return false;
        }
        TaskRecord task = r.getTask();
        ActivityStack stack = r.getStack();
        String str2;
        StringBuilder stringBuilder2;
        if (stack == null) {
            str2 = ActivityManagerService.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("moveActivityStackToFront: invalid task or stack: r=");
            stringBuilder2.append(r);
            stringBuilder2.append(" task=");
            stringBuilder2.append(task);
            Slog.w(str2, stringBuilder2.toString());
            return false;
        } else if (stack == this.mFocusedStack && stack.topRunningActivityLocked() == r) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("moveActivityStackToFront: already on top, r=");
                stringBuilder2.append(r);
                Slog.d(str2, stringBuilder2.toString());
            }
            return false;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                String str3 = ActivityManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("moveActivityStackToFront: r=");
                stringBuilder3.append(r);
                Slog.d(str3, stringBuilder3.toString());
            }
            stack.moveToFront(reason, task);
            if (IS_DEBUG_VERSION) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put("checkType", "FocusWindowNullScene");
                params.put("looper", BackgroundThread.getHandler().getLooper());
                if (r != null) {
                    params.put("focusedActivityName", r.toString());
                }
                params.put("windowManager", this.mWindowManager);
                if (HwServiceFactory.getWinFreezeScreenMonitor() != null) {
                    HwServiceFactory.getWinFreezeScreenMonitor().checkFreezeScreen(params);
                }
            }
            return true;
        }
    }

    ActivityRecord findTaskLocked(ActivityRecord r, int displayId) {
        this.mTmpFindTaskResult.r = null;
        this.mTmpFindTaskResult.matchedByRootAffinity = false;
        ActivityRecord affinityMatch = null;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Looking for task of ");
            stringBuilder.append(r);
            Slog.d(str, stringBuilder.toString());
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                String str2;
                StringBuilder stringBuilder2;
                if (r.hasCompatibleActivityType(stack)) {
                    stack.findTaskLocked(r, this.mTmpFindTaskResult);
                    if (this.mTmpFindTaskResult.r == null) {
                        continue;
                    } else if (!this.mTmpFindTaskResult.matchedByRootAffinity) {
                        return this.mTmpFindTaskResult.r;
                    } else {
                        if (this.mTmpFindTaskResult.r.getDisplayId() == displayId) {
                            affinityMatch = this.mTmpFindTaskResult.r;
                        } else if (ActivityManagerDebugConfig.DEBUG_TASKS && this.mTmpFindTaskResult.matchedByRootAffinity) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Skipping match on different display ");
                            stringBuilder2.append(this.mTmpFindTaskResult.r.getDisplayId());
                            stringBuilder2.append(" ");
                            stringBuilder2.append(displayId);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping stack: (mismatch activity/stack) ");
                    stringBuilder2.append(stack);
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TASKS && affinityMatch == null) {
            Slog.d(ActivityManagerService.TAG, "No task found");
        }
        return affinityMatch;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord ar = display.getChildAt(stackNdx).findActivityLocked(intent, info, compareIntentFilters);
                if (ar != null) {
                    return ar;
                }
            }
        }
        return null;
    }

    boolean hasAwakeDisplay() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            if (!((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).shouldSleep()) {
                return true;
            }
        }
        return false;
    }

    void goingToSleepLocked() {
        scheduleSleepTimeout();
        if (!this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.acquire();
            if (this.mLaunchingActivity.isHeld()) {
                this.mLaunchingActivity.release();
                this.mService.mHandler.removeMessages(104);
            }
        }
        applySleepTokensLocked(false);
        checkReadyForSleepLocked(true);
    }

    void prepareForShutdownLocked() {
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            createSleepTokenLocked("shutdown", this.mActivityDisplays.keyAt(i));
        }
    }

    boolean shutdownLocked(int timeout) {
        goingToSleepLocked();
        boolean timedout = false;
        long endTime = System.currentTimeMillis() + ((long) timeout);
        while (!putStacksToSleepLocked(true, true)) {
            long timeRemaining = endTime - System.currentTimeMillis();
            if (timeRemaining <= 0) {
                Slog.w(ActivityManagerService.TAG, "Activity manager shutdown timed out");
                timedout = true;
                break;
            }
            try {
                this.mService.wait(timeRemaining);
            } catch (InterruptedException e) {
            }
        }
        checkReadyForSleepLocked(false);
        return timedout;
    }

    void comeOutOfSleepIfNeededLocked() {
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
    }

    void applySleepTokensLocked(boolean applyToStacks) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            boolean displayShouldSleep = display.shouldSleep();
            if (displayShouldSleep != display.isSleeping()) {
                display.setIsSleeping(displayShouldSleep);
                if (applyToStacks) {
                    for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                        ActivityStack stack = display.getChildAt(stackNdx);
                        if (displayShouldSleep) {
                            stack.goToSleepIfPossible(false);
                        } else {
                            stack.awakeFromSleepingLocked();
                            if (isFocusedStack(stack) && !getKeyguardController().isKeyguardOrAodShowing(display.mDisplayId)) {
                                this.mActivityLaunchTrack = "outofsleep";
                                this.inResumeTopActivity = false;
                                resumeFocusedStackTopActivityLocked();
                            } else if (stack.inMultiWindowMode() && (stack.getWindowingMode() == 3 || stack.getWindowingMode() == 4)) {
                                resumeAppLockActivityIfNeeded(stack, null);
                            }
                        }
                    }
                    if (!(displayShouldSleep || this.mGoingToSleepActivities.isEmpty())) {
                        Iterator<ActivityRecord> it = this.mGoingToSleepActivities.iterator();
                        while (it.hasNext()) {
                            if (((ActivityRecord) it.next()).getDisplayId() == display.mDisplayId) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    void activitySleptLocked(ActivityRecord r) {
        this.mGoingToSleepActivities.remove(r);
        ActivityStack s = r.getStack();
        if (s != null) {
            s.checkReadyForSleep();
        } else {
            checkReadyForSleepLocked(true);
        }
    }

    void checkReadyForSleepLocked(boolean allowDelay) {
        if (this.mService.isSleepingOrShuttingDownLocked() && putStacksToSleepLocked(allowDelay, false)) {
            sendPowerHintForLaunchEndIfNeeded();
            removeSleepTimeouts();
            if (this.mGoingToSleep.isHeld()) {
                this.mGoingToSleep.release();
            }
            if (this.mService.mShuttingDown) {
                this.mService.notifyAll();
            }
        }
    }

    private boolean putStacksToSleepLocked(boolean allowDelay, boolean shuttingDown) {
        boolean allSleep = true;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (allowDelay) {
                    allSleep &= stack.goToSleepIfPossible(shuttingDown);
                } else {
                    stack.goToSleep();
                }
            }
        }
        return allSleep;
    }

    boolean reportResumedActivityLocked(ActivityRecord r) {
        this.mStoppingActivities.remove(r);
        if (isFocusedStack(r.getStack())) {
            this.mService.updateUsageStats(r, true);
        }
        if (!allResumedActivitiesComplete()) {
            return false;
        }
        ensureActivitiesVisibleLocked(null, 0, false);
        this.mWindowManager.executeAppTransition();
        return true;
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).handleAppCrashLocked(app);
            }
        }
    }

    private void handleLaunchTaskBehindCompleteLocked(ActivityRecord r) {
        TaskRecord task = r.getTask();
        ActivityStack stack = task.getStack();
        r.mLaunchTaskBehind = false;
        this.mRecentTasks.add(task);
        this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
        r.setVisibility(false);
        ActivityRecord top = stack.getTopActivity();
        if (top != null) {
            top.getTask().touchActiveTime();
        }
    }

    void scheduleLaunchTaskBehindComplete(IBinder token) {
        this.mHandler.obtainMessage(112, token).sendToTarget();
    }

    void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        ensureActivitiesVisibleLocked(starting, configChanges, preserveWindows, true);
    }

    void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        getKeyguardController().beginActivityVisibilityUpdate();
        try {
            for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    display.getChildAt(stackNdx).ensureActivitiesVisibleLocked(starting, configChanges, preserveWindows, notifyClients);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Slog.e(ActivityManagerService.TAG, "ensureActivitiesVisibleLocked has Exception : IndexOutOfBoundsException");
        } catch (Throwable th) {
            getKeyguardController().endActivityVisibilityUpdate();
        }
        getKeyguardController().endActivityVisibilityUpdate();
    }

    void addStartingWindowsForVisibleActivities(boolean taskSwitch) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).addStartingWindowsForVisibleActivities(taskSwitch);
            }
        }
    }

    void invalidateTaskLayers() {
        this.mTaskLayersChanged = true;
    }

    void rankTaskLayersIfNeeded() {
        if (this.mTaskLayersChanged) {
            int displayNdx = 0;
            this.mTaskLayersChanged = false;
            while (displayNdx < this.mActivityDisplays.size()) {
                ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
                int baseLayer = 0;
                for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    baseLayer += display.getChildAt(stackNdx).rankTaskLayers(baseLayer);
                }
                displayNdx++;
            }
        }
    }

    void clearOtherAppTimeTrackers(AppTimeTracker except) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).clearOtherAppTimeTrackers(except);
            }
        }
    }

    void scheduleDestroyAllActivities(ProcessRecord app, String reason) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                display.getChildAt(stackNdx).scheduleDestroyActivities(app, reason);
            }
        }
    }

    void releaseSomeActivitiesLocked(ProcessRecord app, String reason) {
        String str;
        int i;
        ArraySet<TaskRecord> tasks = null;
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to release some activities in ");
            stringBuilder.append(app);
            Slog.d(str, stringBuilder.toString());
        }
        TaskRecord firstTask = null;
        for (i = 0; i < app.activities.size(); i++) {
            ActivityRecord r = (ActivityRecord) app.activities.get(i);
            if (r.finishing || r.isState(ActivityState.DESTROYING, ActivityState.DESTROYED)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Abort release; already destroying: ");
                    stringBuilder2.append(r);
                    Slog.d(str, stringBuilder2.toString());
                }
                return;
            }
            if (!r.visible && r.stopped && r.haveState && !r.isState(ActivityState.RESUMED, ActivityState.PAUSING, ActivityState.PAUSED, ActivityState.STOPPING)) {
                TaskRecord task = r.getTask();
                if (task != null) {
                    if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                        String str2 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Collecting release task ");
                        stringBuilder3.append(task);
                        stringBuilder3.append(" from ");
                        stringBuilder3.append(r);
                        Slog.d(str2, stringBuilder3.toString());
                    }
                    if (firstTask == null) {
                        firstTask = task;
                    } else if (firstTask != task) {
                        if (tasks == null) {
                            tasks = new ArraySet();
                            tasks.add(firstTask);
                        }
                        tasks.add(task);
                    }
                }
            } else if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                String str3 = ActivityManagerService.TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Not releasing in-use activity: ");
                stringBuilder4.append(r);
                Slog.d(str3, stringBuilder4.toString());
            }
        }
        if (tasks == null) {
            if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                Slog.d(ActivityManagerService.TAG, "Didn't find two or more tasks to release");
            }
            return;
        }
        i = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < i; displayNdx++) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            int stackCount = display.getChildCount();
            int stackNdx = 0;
            while (stackNdx < stackCount) {
                if (display.getChildAt(stackNdx).releaseSomeActivitiesLocked(app, tasks, reason) <= 0) {
                    stackNdx++;
                } else {
                    return;
                }
            }
        }
    }

    boolean switchUserLocked(int userId, UserState uss) {
        int focusStackId = this.mFocusedStack.getStackId();
        ActivityStack dockedStack = getDefaultDisplay().getSplitScreenPrimaryStack();
        if (dockedStack != null) {
            moveTasksToFullscreenStackLocked(dockedStack, this.mFocusedStack == dockedStack);
        }
        removeStacksInWindowingModes(2);
        this.mUserStackInFront.put(this.mCurrentUser, focusStackId);
        int restoreStackId = this.mUserStackInFront.get(userId, this.mHomeStack.mStackId);
        this.mCurrentUser = userId;
        this.mStartingUsers.add(uss);
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                stack.switchUserLocked(userId);
                TaskRecord task = stack.topTask();
                if (task != null) {
                    stack.positionChildWindowContainerAtTop(task);
                }
            }
        }
        ActivityStack stack2 = getStack(restoreStackId);
        if (stack2 == null) {
            stack2 = this.mHomeStack;
        }
        boolean homeInFront = stack2.isActivityTypeHome();
        if (stack2.isOnHomeDisplay()) {
            stack2.moveToFront("switchUserOnHomeDisplay");
        } else {
            resumeHomeStackTask(null, "switchUserOnOtherDisplay");
        }
        return homeInFront;
    }

    boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUser) {
            return true;
        }
        return this.mService.mUserController.isCurrentProfile(userId);
    }

    boolean isStoppingNoHistoryActivity() {
        Iterator it = this.mStoppingActivities.iterator();
        while (it.hasNext()) {
            if (((ActivityRecord) it.next()).isNoHistory()) {
                return true;
            }
        }
        return false;
    }

    final ArrayList<ActivityRecord> processStoppingActivitiesLocked(ActivityRecord idleActivity, boolean remove, boolean processPausingActivities) {
        ArrayList<ActivityRecord> stops = null;
        boolean nowVisible = allResumedActivitiesVisible();
        for (int activityNdx = this.mStoppingActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            String str;
            StringBuilder stringBuilder;
            ActivityRecord s = (ActivityRecord) this.mStoppingActivities.get(activityNdx);
            boolean waitingVisible = this.mActivitiesWaitingForVisibleActivity.contains(s);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Stopping ");
                stringBuilder.append(s);
                stringBuilder.append(": nowVisible=");
                stringBuilder.append(nowVisible);
                stringBuilder.append(" waitingVisible=");
                stringBuilder.append(waitingVisible);
                stringBuilder.append(" finishing=");
                stringBuilder.append(s.finishing);
                Slog.v(str, stringBuilder.toString());
            }
            if (waitingVisible && nowVisible) {
                this.mActivitiesWaitingForVisibleActivity.remove(s);
                waitingVisible = false;
                if (s.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Before stopping, can hide: ");
                        stringBuilder.append(s);
                        Slog.v(str, stringBuilder.toString());
                    }
                    s.setVisibility(false);
                }
            }
            if (remove) {
                ActivityStack stack = s.getStack();
                boolean shouldSleepOrShutDown;
                if (stack != null) {
                    shouldSleepOrShutDown = stack.shouldSleepOrShutDownActivities();
                } else {
                    shouldSleepOrShutDown = this.mService.isSleepingOrShuttingDownLocked();
                }
                if (!waitingVisible || shouldSleepOrShutDown) {
                    if (processPausingActivities || !s.isState(ActivityState.PAUSING)) {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            String str2 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Ready to stop: ");
                            stringBuilder2.append(s);
                            Slog.v(str2, stringBuilder2.toString());
                        }
                        if (stops == null) {
                            stops = new ArrayList();
                        }
                        stops.add(s);
                        this.mStoppingActivities.remove(activityNdx);
                    } else {
                        removeTimeoutsForActivityLocked(idleActivity);
                        scheduleIdleTimeoutLocked(idleActivity);
                    }
                }
            }
        }
        return stops;
    }

    void validateTopActivitiesLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                ActivityRecord r = stack.topRunningActivityLocked();
                ActivityState state = r == null ? ActivityState.DESTROYED : r.getState();
                ActivityRecord resumed;
                String str;
                StringBuilder stringBuilder;
                if (!isFocusedStack(stack)) {
                    resumed = stack.getResumedActivity();
                    if (resumed != null && resumed == r) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("validateTop...: back stack has resumed activity r=");
                        stringBuilder.append(r);
                        stringBuilder.append(" state=");
                        stringBuilder.append(state);
                        Slog.e(str, stringBuilder.toString());
                    }
                    if (r != null && (state == ActivityState.INITIALIZING || state == ActivityState.RESUMED)) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("validateTop...: activity in back resumed r=");
                        stringBuilder.append(r);
                        stringBuilder.append(" state=");
                        stringBuilder.append(state);
                        Slog.e(str, stringBuilder.toString());
                    }
                } else if (r == null) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("validateTop...: null top activity, stack=");
                    stringBuilder2.append(stack);
                    Slog.e(str2, stringBuilder2.toString());
                } else {
                    resumed = stack.mPausingActivity;
                    if (resumed != null && resumed == r) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("validateTop...: top stack has pausing activity r=");
                        stringBuilder.append(r);
                        stringBuilder.append(" state=");
                        stringBuilder.append(state);
                        Slog.e(str, stringBuilder.toString());
                    }
                    if (!(state == ActivityState.INITIALIZING || state == ActivityState.RESUMED)) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("validateTop...: activity in front not resumed r=");
                        stringBuilder.append(r);
                        stringBuilder.append(" state=");
                        stringBuilder.append(state);
                        Slog.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    public void dumpDisplays(PrintWriter pw) {
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[id:");
            stringBuilder.append(display.mDisplayId);
            stringBuilder.append(" stacks:");
            pw.print(stringBuilder.toString());
            display.dumpStacks(pw);
            pw.print("]");
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        int i;
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mFocusedStack=");
        stringBuilder.append(this.mFocusedStack);
        pw.print(stringBuilder.toString());
        pw.print(" mLastFocusedStack=");
        pw.println(this.mLastFocusedStack);
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurTaskIdForUser=");
        stringBuilder.append(this.mCurTaskIdForUser);
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("mUserStackInFront=");
        stringBuilder.append(this.mUserStackInFront);
        pw.println(stringBuilder.toString());
        for (i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ((ActivityDisplay) this.mActivityDisplays.valueAt(i)).dump(pw, prefix);
        }
        if (!this.mWaitingForActivityVisible.isEmpty()) {
            pw.print(prefix);
            pw.println("mWaitingForActivityVisible=");
            for (i = 0; i < this.mWaitingForActivityVisible.size(); i++) {
                pw.print(prefix);
                pw.print(prefix);
                ((WaitInfo) this.mWaitingForActivityVisible.get(i)).dump(pw, prefix);
            }
        }
        pw.print(prefix);
        pw.print("isHomeRecentsComponent=");
        pw.print(this.mRecentTasks.isRecentsComponentHomeActivity(this.mCurrentUser));
        getKeyguardController().dump(pw, prefix);
        this.mService.getLockTaskController().dump(pw, prefix);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        int displayNdx = 0;
        super.writeToProto(proto, 1146756268033L, false);
        while (displayNdx < this.mActivityDisplays.size()) {
            ((ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx)).writeToProto(proto, 2246267895810L);
            displayNdx++;
        }
        getKeyguardController().writeToProto(proto, 1146756268035L);
        if (this.mFocusedStack != null) {
            proto.write(1120986464260L, this.mFocusedStack.mStackId);
            ActivityRecord focusedActivity = getResumedActivityLocked();
            if (focusedActivity != null) {
                focusedActivity.writeIdentifierToProto(proto, 1146756268037L);
            }
        } else {
            proto.write(1120986464260L, -1);
        }
        proto.write(1133871366150L, this.mRecentTasks.isRecentsComponentHomeActivity(this.mCurrentUser));
        proto.end(token);
    }

    void dumpDisplayConfigs(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("Display override configurations:");
        int displayCount = this.mActivityDisplays.size();
        for (int i = 0; i < displayCount; i++) {
            ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(i);
            pw.print(prefix);
            pw.print("  ");
            pw.print(activityDisplay.mDisplayId);
            pw.print(": ");
            pw.println(activityDisplay.getOverrideConfiguration());
        }
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name, boolean dumpVisibleStacksOnly, boolean dumpFocusedStackOnly) {
        if (dumpFocusedStackOnly) {
            return this.mFocusedStack.getDumpActivitiesLocked(name);
        }
        ArrayList<ActivityRecord> activities = new ArrayList();
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if (!dumpVisibleStacksOnly || stack.shouldBeVisible(null)) {
                    activities.addAll(stack.getDumpActivitiesLocked(name));
                }
            }
        }
        return activities;
    }

    static boolean printThisActivity(PrintWriter pw, ActivityRecord activity, String dumpPackage, boolean needSep, String prefix) {
        if (activity == null || (dumpPackage != null && !dumpPackage.equals(activity.packageName))) {
            return false;
        }
        if (needSep) {
            pw.println();
        }
        pw.print(prefix);
        pw.println(activity);
        return true;
    }

    boolean dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        PrintWriter printWriter = pw;
        String str = dumpPackage;
        int displayNdx = 0;
        boolean printed = false;
        boolean needSep = false;
        while (true) {
            int displayNdx2 = displayNdx;
            if (displayNdx2 < this.mActivityDisplays.size()) {
                ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx2);
                printWriter.print("Display #");
                printWriter.print(activityDisplay.mDisplayId);
                printWriter.println(" (activities from top to bottom):");
                ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx2);
                int stackNdx = display.getChildCount() - 1;
                while (true) {
                    int stackNdx2 = stackNdx;
                    if (stackNdx2 < 0) {
                        break;
                    }
                    ActivityStack stack = display.getChildAt(stackNdx2);
                    pw.println();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Stack #");
                    stringBuilder.append(stack.mStackId);
                    stringBuilder.append(": type=");
                    stringBuilder.append(WindowConfiguration.activityTypeToString(stack.getActivityType()));
                    stringBuilder.append(" mode=");
                    stringBuilder.append(WindowConfiguration.windowingModeToString(stack.getWindowingMode()));
                    printWriter.println(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  isSleeping=");
                    stringBuilder.append(stack.shouldSleepActivities());
                    printWriter.println(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  mBounds=");
                    stringBuilder.append(stack.getOverrideBounds());
                    printWriter.println(stringBuilder.toString());
                    int stackNdx3 = stackNdx2;
                    ActivityDisplay display2 = display;
                    ActivityDisplay activityDisplay2 = activityDisplay;
                    int displayNdx3 = displayNdx2;
                    boolean printed2 = dumpHistoryList(fd, printWriter, stack.mLRUActivities, "    ", "Run", false, dumpAll ^ 1, false, str, true, "    Running activities (most recent first):", null) | (printed | stack.dumpActivitiesLocked(fd, printWriter, dumpAll, dumpClient, str, needSep));
                    boolean needSep2 = printed2;
                    ActivityStack stack2 = stack;
                    if (printThisActivity(printWriter, stack2.mPausingActivity, str, needSep2, "    mPausingActivity: ")) {
                        printed2 = true;
                        needSep2 = false;
                    }
                    if (printThisActivity(printWriter, stack2.getResumedActivity(), str, needSep2, "    mResumedActivity: ")) {
                        printed2 = true;
                        needSep2 = false;
                    }
                    if (dumpAll) {
                        if (printThisActivity(printWriter, stack2.mLastPausedActivity, str, needSep2, "    mLastPausedActivity: ")) {
                            printed2 = true;
                            needSep2 = true;
                        }
                        printed2 |= printThisActivity(printWriter, stack2.mLastNoHistoryActivity, str, needSep2, "    mLastNoHistoryActivity: ");
                    }
                    printed = printed2;
                    needSep = printed;
                    stackNdx = stackNdx3 - 1;
                    activityDisplay = activityDisplay2;
                    displayNdx2 = displayNdx3;
                    display = display2;
                }
                displayNdx = displayNdx2 + 1;
            } else {
                return dumpHistoryList(fd, printWriter, this.mGoingToSleepActivities, "  ", "Sleep", false, dumpAll ^ 1, false, str, true, "  Activities waiting to sleep:", null) | (((printed | dumpHistoryList(fd, printWriter, this.mFinishingActivities, "  ", "Fin", false, dumpAll ^ 1, false, str, true, "  Activities waiting to finish:", null)) | dumpHistoryList(fd, printWriter, this.mStoppingActivities, "  ", "Stop", false, dumpAll ^ 1, false, str, true, "  Activities waiting to stop:", null)) | dumpHistoryList(fd, printWriter, this.mActivitiesWaitingForVisibleActivity, "  ", "Wait", false, dumpAll ^ 1, false, str, true, "  Activities waiting for another to become visible:", null));
            }
        }
    }

    static boolean dumpHistoryList(FileDescriptor fd, PrintWriter pw, List<ActivityRecord> list, String prefix, String label, boolean complete, boolean brief, boolean client, String dumpPackage, boolean needNL, String header, TaskRecord lastTask) {
        TaskRecord lastTask2;
        FileDescriptor lastTask3;
        Throwable th;
        IOException e;
        StringBuilder stringBuilder;
        PrintWriter printWriter = pw;
        String str = prefix;
        String str2 = dumpPackage;
        boolean printed = false;
        int i = list.size() - 1;
        boolean needNL2 = needNL;
        String innerPrefix = null;
        String[] args = null;
        String header2 = header;
        TaskRecord lastTask4 = lastTask;
        while (i >= 0) {
            ActivityRecord r = (ActivityRecord) list.get(i);
            if (str2 == null || str2.equals(r.packageName)) {
                boolean full = false;
                if (innerPrefix == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(str);
                    stringBuilder2.append("      ");
                    innerPrefix = stringBuilder2.toString();
                    args = new String[0];
                }
                printed = true;
                if (!brief && (complete || !r.isInHistory())) {
                    full = true;
                }
                if (needNL2) {
                    printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    needNL2 = false;
                }
                if (header2 != null) {
                    printWriter.println(header2);
                    header2 = null;
                }
                String header3 = header2;
                if (lastTask4 != r.getTask()) {
                    lastTask4 = r.getTask();
                    printWriter.print(str);
                    printWriter.print(full ? "* " : "  ");
                    printWriter.println(lastTask4);
                    if (full) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(str);
                        stringBuilder3.append("  ");
                        lastTask4.dump(printWriter, stringBuilder3.toString());
                    } else if (complete && lastTask4.intent != null) {
                        printWriter.print(str);
                        printWriter.print("  ");
                        printWriter.println(lastTask4.intent.toInsecureStringWithClip());
                    }
                }
                printWriter.print(str);
                printWriter.print(full ? "  * " : "    ");
                printWriter.print(label);
                printWriter.print(" #");
                printWriter.print(i);
                printWriter.print(": ");
                printWriter.println(r);
                if (full) {
                    r.dump(printWriter, innerPrefix);
                } else if (complete) {
                    printWriter.print(innerPrefix);
                    printWriter.println(r.intent.toInsecureString());
                    if (r.app != null) {
                        printWriter.print(innerPrefix);
                        printWriter.println(r.app);
                    }
                }
                if (!client || r.app == null || r.app.thread == null) {
                    lastTask2 = lastTask4;
                    lastTask3 = fd;
                } else {
                    pw.flush();
                    try {
                        TransferPipe tp = new TransferPipe();
                        TransferPipe tp2;
                        try {
                            tp2 = tp;
                            try {
                                r.app.thread.dumpActivity(tp2.getWriteFd(), r.appToken, innerPrefix, args);
                                lastTask2 = lastTask4;
                            } catch (Throwable th2) {
                                th = th2;
                                lastTask2 = lastTask4;
                                lastTask3 = fd;
                                tp2.kill();
                                throw th;
                            }
                            try {
                                tp2.go(fd, 2000);
                                try {
                                    tp2.kill();
                                } catch (IOException e2) {
                                    e = e2;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(innerPrefix);
                                    stringBuilder.append("Failure while dumping the activity: ");
                                    stringBuilder.append(e);
                                    printWriter.println(stringBuilder.toString());
                                    needNL2 = true;
                                    header2 = header3;
                                    i--;
                                    lastTask4 = lastTask2;
                                    str = prefix;
                                    str2 = dumpPackage;
                                } catch (RemoteException e3) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(innerPrefix);
                                    stringBuilder.append("Got a RemoteException while dumping the activity");
                                    printWriter.println(stringBuilder.toString());
                                    needNL2 = true;
                                    header2 = header3;
                                    i--;
                                    lastTask4 = lastTask2;
                                    str = prefix;
                                    str2 = dumpPackage;
                                }
                                needNL2 = true;
                            } catch (Throwable th3) {
                                th = th3;
                                tp2.kill();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            lastTask2 = lastTask4;
                            tp2 = tp;
                            lastTask3 = fd;
                            tp2.kill();
                            throw th;
                        }
                    } catch (IOException e4) {
                        e = e4;
                        lastTask2 = lastTask4;
                        lastTask3 = fd;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(innerPrefix);
                        stringBuilder.append("Failure while dumping the activity: ");
                        stringBuilder.append(e);
                        printWriter.println(stringBuilder.toString());
                        needNL2 = true;
                        header2 = header3;
                        i--;
                        lastTask4 = lastTask2;
                        str = prefix;
                        str2 = dumpPackage;
                    } catch (RemoteException e5) {
                        lastTask2 = lastTask4;
                        lastTask3 = fd;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(innerPrefix);
                        stringBuilder.append("Got a RemoteException while dumping the activity");
                        printWriter.println(stringBuilder.toString());
                        needNL2 = true;
                        header2 = header3;
                        i--;
                        lastTask4 = lastTask2;
                        str = prefix;
                        str2 = dumpPackage;
                    }
                }
                header2 = header3;
            } else {
                lastTask2 = lastTask4;
                lastTask3 = fd;
            }
            i--;
            lastTask4 = lastTask2;
            str = prefix;
            str2 = dumpPackage;
        }
        List<ActivityRecord> list2 = list;
        lastTask2 = lastTask4;
        lastTask3 = fd;
        return printed;
    }

    void scheduleIdleTimeoutLocked(ActivityRecord next) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleIdleTimeoutLocked: Callers=");
            stringBuilder.append(Debug.getCallers(4));
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, next), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    final void scheduleIdleLocked() {
        this.mHandler.sendEmptyMessage(101);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord r) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeTimeoutsForActivity: Callers=");
            stringBuilder.append(Debug.getCallers(4));
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.removeMessages(100, r);
    }

    final void scheduleResumeTopActivities() {
        if (!this.mHandler.hasMessages(102)) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    void removeSleepTimeouts() {
        this.mHandler.removeMessages(103);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(103, 5000);
    }

    public void onDisplayAdded(int displayId) {
        String str = ActivityManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display added displayId=");
        stringBuilder.append(displayId);
        Slog.v(str, stringBuilder.toString());
        this.mHandler.sendMessage(this.mHandler.obtainMessage(105, displayId, 0));
    }

    public void onDisplayRemoved(int displayId) {
        String str = ActivityManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Display removed displayId=");
        stringBuilder.append(displayId);
        Slog.v(str, stringBuilder.toString());
        this.mHandler.sendMessage(this.mHandler.obtainMessage(107, displayId, 0));
    }

    public void onDisplayChanged(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Display changed displayId=");
            stringBuilder.append(displayId);
            Slog.v(str, stringBuilder.toString());
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(106, displayId, 0));
    }

    private void handleDisplayAdded(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                getActivityDisplayOrCreateLocked(displayId);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    boolean isDisplayAdded(int displayId) {
        return getActivityDisplayOrCreateLocked(displayId) != null;
    }

    ActivityDisplay getActivityDisplay(int displayId) {
        return (ActivityDisplay) this.mActivityDisplays.get(displayId);
    }

    ActivityDisplay getDefaultDisplay() {
        return (ActivityDisplay) this.mActivityDisplays.get(0);
    }

    ActivityDisplay getActivityDisplayOrCreateLocked(int displayId) {
        ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        if (activityDisplay != null) {
            return activityDisplay;
        }
        if (this.mDisplayManager == null) {
            return null;
        }
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display == null) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getActivityDisplayOrCreateLocked return null, displayId:");
            stringBuilder.append(displayId);
            Slog.i(str, stringBuilder.toString());
            return null;
        }
        activityDisplay = new ActivityDisplay(this, display);
        attachDisplay(activityDisplay);
        calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
        this.mWindowManager.onDisplayAdded(displayId);
        return activityDisplay;
    }

    @VisibleForTesting
    void attachDisplay(ActivityDisplay display) {
        this.mActivityDisplays.put(display.mDisplayId, display);
    }

    public void reCalculateDefaultMinimalSizeOfResizeableTasks() {
        calculateDefaultMinimalSizeOfResizeableTasks(getActivityDisplayOrCreateLocked(null));
    }

    private void calculateDefaultMinimalSizeOfResizeableTasks(ActivityDisplay display) {
        this.mDefaultMinSizeOfResizeableTask = this.mService.mContext.getResources().getDimensionPixelSize(17105017);
    }

    protected void handleDisplayRemoved(int displayId) {
        if (displayId != 0) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                    if (activityDisplay == null) {
                    } else {
                        activityDisplay.remove();
                        releaseSleepTokens(activityDisplay);
                        this.mActivityDisplays.remove(displayId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        } else {
            throw new IllegalArgumentException("Can't remove the primary display.");
        }
    }

    private void handleDisplayChanged(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = (ActivityDisplay) this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    if (displayId != 0) {
                        int displayState = activityDisplay.mDisplay.getState();
                        if (displayState == 1 && activityDisplay.mOffToken == null) {
                            activityDisplay.mOffToken = this.mService.acquireSleepToken("Display-off", displayId);
                        } else if (displayState == 2 && activityDisplay.mOffToken != null) {
                            activityDisplay.mOffToken.release();
                            activityDisplay.mOffToken = null;
                        }
                    }
                    activityDisplay.updateBounds();
                }
                this.mWindowManager.onDisplayChanged(displayId);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    SleepToken createSleepTokenLocked(String tag, int displayId) {
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(displayId);
        if (display != null) {
            SleepTokenImpl token = new SleepTokenImpl(tag, displayId);
            this.mSleepTokens.add(token);
            StringBuilder stringBuilder;
            if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                String str = TAG_KEYGUARD;
                stringBuilder = new StringBuilder();
                stringBuilder.append("add sleepToken:");
                stringBuilder.append(token);
                Slog.v(str, stringBuilder.toString(), new Exception());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("add sleepToken:");
                stringBuilder.append(token);
                Flog.i(107, stringBuilder.toString());
            }
            display.mAllSleepTokens.add(token);
            return token;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid display: ");
        stringBuilder2.append(displayId);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private void removeSleepTokenLocked(SleepTokenImpl token) {
        this.mSleepTokens.remove(token);
        if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
            String str = TAG_KEYGUARD;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove sleepToken:");
            stringBuilder.append(token);
            Slog.v(str, stringBuilder.toString(), new Exception());
        }
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(token.mDisplayId);
        if (display != null) {
            display.mAllSleepTokens.remove(token);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("remove sleepToken:");
            stringBuilder2.append(token);
            Flog.i(107, stringBuilder2.toString());
            if (!display.mAllSleepTokens.isEmpty()) {
                return;
            }
            if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || this.mService.mHwAMSEx == null) {
                this.mService.updateSleepIfNeededLocked();
            } else if (this.mService.mHwAMSEx.canUpdateSleepForPCMode()) {
                this.mService.updateSleepIfNeededLocked();
            }
        }
    }

    private void releaseSleepTokens(ActivityDisplay display) {
        if (!display.mAllSleepTokens.isEmpty()) {
            Iterator it = display.mAllSleepTokens.iterator();
            while (it.hasNext()) {
                this.mSleepTokens.remove((SleepToken) it.next());
            }
            Flog.i(107, "clear sleepToken");
            display.mAllSleepTokens.clear();
            this.mService.updateSleepIfNeededLocked();
        }
    }

    private StackInfo getStackInfo(ActivityStack stack) {
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(stack.mDisplayId);
        StackInfo info = new StackInfo();
        stack.getWindowContainerBounds(info.bounds);
        int i = 0;
        if (HwPCUtils.isExtDynamicStack(stack.getStackId())) {
            info.displayId = stack.mDisplayId;
        } else {
            info.displayId = 0;
        }
        info.stackId = stack.mStackId;
        info.userId = stack.mCurrentUser;
        ComponentName componentName = null;
        info.visible = stack.shouldBeVisible(null);
        info.position = display != null ? display.getIndexOf(stack) : 0;
        info.configuration.setTo(stack.getConfiguration());
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        int numTasks = tasks.size();
        int[] taskIds = new int[numTasks];
        String[] taskNames = new String[numTasks];
        Rect[] taskBounds = new Rect[numTasks];
        int[] taskUserIds = new int[numTasks];
        while (i < numTasks) {
            String flattenToString;
            TaskRecord task = (TaskRecord) tasks.get(i);
            taskIds[i] = task.taskId;
            if (task.origActivity != null) {
                flattenToString = task.origActivity.flattenToString();
            } else if (task.realActivity != null) {
                flattenToString = task.realActivity.flattenToString();
            } else if (task.getTopActivity() != null) {
                flattenToString = task.getTopActivity().packageName;
            } else {
                flattenToString = Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            taskNames[i] = flattenToString;
            taskBounds[i] = new Rect();
            task.getWindowContainerBounds(taskBounds[i]);
            taskUserIds[i] = task.userId;
            i++;
        }
        info.taskIds = taskIds;
        info.taskNames = taskNames;
        info.taskBounds = taskBounds;
        info.taskUserIds = taskUserIds;
        ActivityRecord top = stack.topRunningActivityLocked();
        if (top != null) {
            componentName = top.intent.getComponent();
        }
        info.topActivity = componentName;
        return info;
    }

    StackInfo getStackInfo(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            return getStackInfo(stack);
        }
        return null;
    }

    StackInfo getStackInfo(int windowingMode, int activityType) {
        ActivityStack stack = getStack(windowingMode, activityType);
        return stack != null ? getStackInfo(stack) : null;
    }

    ArrayList<StackInfo> getAllStackInfosLocked() {
        ArrayList<StackInfo> list = new ArrayList();
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                list.add(getStackInfo(display.getChildAt(stackNdx)));
            }
        }
        return list;
    }

    void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredWindowingMode, int preferredDisplayId, ActivityStack actualStack) {
        handleNonResizableTaskIfNeeded(task, preferredWindowingMode, preferredDisplayId, actualStack, false);
    }

    void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredWindowingMode, int preferredDisplayId, ActivityStack actualStack, boolean forceNonResizable) {
        boolean z = false;
        boolean isSecondaryDisplayPreferred = (preferredDisplayId == 0 || preferredDisplayId == -1) ? false : true;
        if (task != null) {
            String str;
            if (HwVRUtils.isVRMode() || HwVRUtils.isValidVRDisplayId(preferredDisplayId)) {
                str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleNonResizableTaskIfNeeded preferredStackId = ");
                stringBuilder.append(preferredDisplayId);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            boolean inSplitScreenMode = actualStack != null && actualStack.getDisplay().hasSplitScreenPrimaryStack();
            if (inSplitScreenMode || preferredWindowingMode == 3 || isSecondaryDisplayPreferred) {
                int reason = 2;
                if (task.isActivityTypeStandardOrUndefined() || getConfiguration().extraConfig.getConfigItem(2) == 2) {
                    if (isSecondaryDisplayPreferred) {
                        int actualDisplayId = task.getStack().mDisplayId;
                        if (task.canBeLaunchedOnDisplay(actualDisplayId)) {
                            if (!HwPCUtils.isPcDynamicStack(task.getStack().mStackId)) {
                                this.mService.setTaskWindowingMode(task.taskId, 4, true);
                            }
                            if (preferredDisplayId != actualDisplayId) {
                                this.mService.mTaskChangeNotificationController.notifyActivityLaunchOnSecondaryDisplayFailed();
                                return;
                            }
                        }
                        throw new IllegalStateException("Task resolved to incompatible display");
                    }
                    ActivityRecord topActivity = task.getTopActivity();
                    boolean isInMultiWinBlackList = false;
                    if (!(this.mService.mCustAms == null || topActivity == null)) {
                        isInMultiWinBlackList = this.mService.mCustAms.isInMultiWinBlackList(topActivity.appInfo.packageName, this.mService.mContext.getContentResolver());
                    }
                    if (!task.supportsSplitScreenWindowingMode() || forceNonResizable || ((topActivity != null && "com.huawei.systemmanager".equals(topActivity.appInfo.packageName)) || isInMultiWinBlackList)) {
                        if (!HwPCUtils.isPcDynamicStack(topActivity.getStackId())) {
                            this.mService.mTaskChangeNotificationController.notifyActivityDismissingDockedStack();
                            uploadUnSupportSplitScreenAppPackageName(topActivity.appInfo.packageName);
                        }
                        ActivityStack dockedStack = task.getStack().getDisplay().getSplitScreenPrimaryStack();
                        if (dockedStack != null) {
                            if (actualStack == dockedStack) {
                                z = true;
                            }
                            moveTasksToFullscreenStackLocked(dockedStack, z);
                        }
                    } else if (topActivity != null && topActivity.isNonResizableOrForcedResizable() && !topActivity.noDisplay && !HwPCUtils.isExtDynamicStack(topActivity.getStackId())) {
                        str = topActivity.appInfo.packageName;
                        if (!isSecondaryDisplayPreferred) {
                            reason = 1;
                        }
                        if ((1 & topActivity.appInfo.flags) == 0) {
                            this.mService.mTaskChangeNotificationController.notifyActivityForcedResizable(task.taskId, reason, str);
                        }
                    }
                }
            }
        }
    }

    void activityRelaunchedLocked(IBinder token) {
        this.mWindowManager.notifyAppRelaunchingFinished(token);
        ActivityRecord r = ActivityRecord.isInStackLocked(token);
        if (r != null && r.getStack().shouldSleepOrShutDownActivities()) {
            r.setSleeping(true, true);
        }
    }

    void activityRelaunchingLocked(ActivityRecord r) {
        this.mWindowManager.notifyAppRelaunching(r.appToken);
    }

    void logStackState() {
        try {
            this.mActivityMetricsLogger.logWindowState();
        } catch (Exception e) {
            Slog.e(ActivityManagerService.TAG, "stack state exception!");
        }
    }

    void scheduleUpdateMultiWindowMode(TaskRecord task) {
        if (!task.getStack().deferScheduleMultiWindowModeChanged()) {
            for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
                if (!(r.app == null || r.app.thread == null)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("add r ");
                    stringBuilder.append(r);
                    stringBuilder.append(" into list of multiwindow activities");
                    Flog.i(101, stringBuilder.toString());
                    this.mMultiWindowModeChangedActivities.add(r);
                }
            }
            if (!this.mHandler.hasMessages(114)) {
                this.mHandler.sendEmptyMessage(114);
            }
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord task, ActivityStack prevStack) {
        ActivityStack stack = task.getStack();
        if (prevStack != null && prevStack != stack && (prevStack.inPinnedWindowingMode() || stack.inPinnedWindowingMode())) {
            scheduleUpdatePictureInPictureModeIfNeeded(task, stack.getOverrideBounds());
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord task, Rect targetStackBounds) {
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
            if (!(r.app == null || r.app.thread == null)) {
                this.mPipModeChangedActivities.add(r);
                this.mMultiWindowModeChangedActivities.remove(r);
            }
        }
        this.mPipModeChangedTargetStackBounds = targetStackBounds;
        if (!this.mHandler.hasMessages(115)) {
            this.mHandler.sendEmptyMessage(115);
        }
    }

    void updatePictureInPictureMode(TaskRecord task, Rect targetStackBounds, boolean forceUpdate) {
        this.mHandler.removeMessages(115);
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) task.mActivities.get(i);
            if (!(r.app == null || r.app.thread == null)) {
                r.updatePictureInPictureMode(targetStackBounds, forceUpdate);
            }
        }
    }

    void setDockedStackMinimized(boolean minimized) {
        this.mIsDockMinimized = minimized;
        if (this.mIsDockMinimized) {
            ActivityStack current = getFocusedStack();
            if (current.inSplitScreenPrimaryWindowingMode()) {
                current.adjustFocusToNextFocusableStack("setDockedStackMinimized");
            }
        }
    }

    void wakeUp(String reason) {
        PowerManager powerManager = this.mPowerManager;
        long uptimeMillis = SystemClock.uptimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("android.server.am:TURN_ON:");
        stringBuilder.append(reason);
        powerManager.wakeUp(uptimeMillis, stringBuilder.toString());
    }

    private void beginDeferResume() {
        this.mDeferResumeCount++;
    }

    private void endDeferResume() {
        this.mDeferResumeCount--;
    }

    private boolean readyToResume() {
        return this.mDeferResumeCount == 0;
    }

    ActivityStack findStackBehind(ActivityStack stack) {
        ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.get(0);
        if (display == null) {
            return null;
        }
        int i = display.getChildCount() - 1;
        while (i >= 0) {
            if (display.getChildAt(i) == stack && i > 0) {
                return display.getChildAt(i - 1);
            }
            i--;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to find a stack behind stack=");
        stringBuilder.append(stack);
        stringBuilder.append(" in=");
        stringBuilder.append(display);
        throw new IllegalStateException(stringBuilder.toString());
    }

    void setResizingDuringAnimation(TaskRecord task) {
        this.mResizingTasksDuringAnimation.add(Integer.valueOf(task.taskId));
        task.setTaskDockedResizing(true);
    }

    /* JADX WARNING: Removed duplicated region for block: B:99:0x0225 A:{Catch:{ all -> 0x0231 }} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0225 A:{Catch:{ all -> 0x0231 }} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0225 A:{Catch:{ all -> 0x0231 }} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0225 A:{Catch:{ all -> 0x0231 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int startActivityFromRecents(int callingPid, int callingUid, int taskId, SafeActivityOptions options) {
        ActivityOptions options2;
        Throwable th;
        ActivityRecord targetActivity;
        int i;
        ActivityStack topSecondaryStack;
        boolean z;
        int i2 = taskId;
        SafeActivityOptions safeActivityOptions = options;
        TaskRecord task = null;
        int activityType = 0;
        int windowingMode = 0;
        if (safeActivityOptions != null) {
            options2 = safeActivityOptions.getOptions(this);
        } else {
            options2 = null;
        }
        ActivityOptions activityOptions = options2;
        if (activityOptions != null) {
            activityType = activityOptions.getLaunchActivityType();
            windowingMode = activityOptions.getLaunchWindowingMode();
        }
        int activityType2 = activityType;
        int windowingMode2 = windowingMode;
        int i3;
        ActivityOptions activityOptions2;
        if (activityType2 == 2 || activityType2 == 3) {
            i3 = activityType2;
            activityOptions2 = activityOptions;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startActivityFromRecents: Task ");
            stringBuilder.append(i2);
            stringBuilder.append(" can't be launch in the home/recents stack.");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mWindowManager.deferSurfaceLayout();
        if (windowingMode2 == 3) {
            try {
                this.mWindowManager.setDockedStackCreateState(activityOptions.getSplitScreenCreateMode(), null);
                deferUpdateRecentsHomeStackBounds();
                this.mWindowManager.prepareAppTransition(19, false);
            } catch (Throwable th2) {
                th = th2;
            }
        }
        try {
            TaskRecord task2 = anyTaskForIdLocked(i2, 2, activityOptions, true);
            StringBuilder stringBuilder2;
            if (task2 != null) {
                if (windowingMode2 != 3) {
                    try {
                        moveHomeStackToFront("startActivityFromRecents");
                    } catch (Throwable th3) {
                        th = th3;
                        task = task2;
                    }
                }
                try {
                    if (!this.mService.mUserController.shouldConfirmCredentials(task2.userId)) {
                        if (task2.getRootActivity() != null) {
                            targetActivity = task2.getTopActivity();
                            sendPowerHintForLaunchStartIfNeeded(true, targetActivity);
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("task.userId =");
                            stringBuilder2.append(task2.userId);
                            stringBuilder2.append(", task.taskId = ");
                            stringBuilder2.append(task2.taskId);
                            stringBuilder2.append(", task.getRootActivity() = ");
                            stringBuilder2.append(task2.getRootActivity());
                            stringBuilder2.append(", task.getTopActivity() = ");
                            stringBuilder2.append(task2.getTopActivity());
                            Flog.i(101, stringBuilder2.toString());
                            this.mActivityMetricsLogger.notifyActivityLaunching();
                            this.mService.moveTaskToFrontLocked(task2.taskId, 0, safeActivityOptions, true);
                            this.mActivityMetricsLogger.notifyActivityLaunched(2, targetActivity);
                            this.mService.getActivityStartController().postStartActivityProcessingForLastStarter(task2.getTopActivity(), 2, task2.getStack());
                            if (HwPCUtils.isPcCastModeInServer()) {
                                activityType = 0;
                                if (!(activityOptions == null || activityOptions.getLaunchDisplayId() == -1)) {
                                    activityType = activityOptions.getLaunchDisplayId();
                                }
                                if (!(task2.getStack() == null || task2.getStack().mDisplayId == activityType)) {
                                    showToast(activityType);
                                }
                            }
                            if (windowingMode2 == 3 && task2 != null) {
                                try {
                                    setResizingDuringAnimation(task2);
                                    if (task2.getStack().getDisplay().getTopStackInWindowingMode(4).isActivityTypeHome()) {
                                        moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                                        this.mWindowManager.checkSplitScreenMinimizedChanged(false);
                                    }
                                } catch (Throwable th4) {
                                    this.mWindowManager.continueSurfaceLayout();
                                }
                            }
                            this.mWindowManager.continueSurfaceLayout();
                            return 2;
                        }
                    }
                    String callingPackage = task2.mCallingPackage;
                    Intent intent = task2.intent;
                    intent.addFlags(DumpState.DUMP_DEXOPT);
                    TaskRecord task3 = task2;
                    int windowingMode3 = windowingMode2;
                    i3 = activityType2;
                    activityOptions2 = activityOptions;
                    try {
                        activityType = this.mService.getActivityStartController().startActivityInPackage(task2.mCallingUid, callingPid, callingUid, callingPackage, intent, null, null, null, 0, 0, options, task2.userId, task3, "startActivityFromRecents", false);
                        if (windowingMode3 == 3) {
                            task = task3;
                            if (task != null) {
                                try {
                                    setResizingDuringAnimation(task);
                                    if (task.getStack().getDisplay().getTopStackInWindowingMode(4).isActivityTypeHome()) {
                                        moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                                        this.mWindowManager.checkSplitScreenMinimizedChanged(false);
                                    }
                                } catch (Throwable th5) {
                                    this.mWindowManager.continueSurfaceLayout();
                                }
                            }
                        }
                        this.mWindowManager.continueSurfaceLayout();
                        return activityType;
                    } catch (Throwable th6) {
                        th = th6;
                        task = task3;
                        windowingMode = windowingMode3;
                        i = 3;
                        topSecondaryStack = 4;
                        z = false;
                        try {
                            setResizingDuringAnimation(task);
                            if (task.getStack().getDisplay().getTopStackInWindowingMode(topSecondaryStack).isActivityTypeHome()) {
                            }
                            this.mWindowManager.continueSurfaceLayout();
                            throw th;
                        } catch (Throwable th7) {
                            this.mWindowManager.continueSurfaceLayout();
                        }
                    }
                } catch (Throwable th8) {
                    th = th8;
                    task = task2;
                    topSecondaryStack = 4;
                    i = 3;
                    windowingMode = windowingMode2;
                    i3 = activityType2;
                    activityOptions2 = activityOptions;
                    z = false;
                    setResizingDuringAnimation(task);
                    if (task.getStack().getDisplay().getTopStackInWindowingMode(topSecondaryStack).isActivityTypeHome()) {
                    }
                    this.mWindowManager.continueSurfaceLayout();
                    throw th;
                }
            }
            task = task2;
            topSecondaryStack = 4;
            i = 3;
            windowingMode = windowingMode2;
            i3 = activityType2;
            activityOptions2 = activityOptions;
            z = false;
            try {
                continueUpdateRecentsHomeStackBounds();
                this.mWindowManager.executeAppTransition();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startActivityFromRecents: Task ");
                stringBuilder2.append(i2);
                stringBuilder2.append(" not found.");
                throw new IllegalArgumentException(stringBuilder2.toString());
            } catch (Throwable th9) {
                th = th9;
                if (windowingMode == i && task != null) {
                    setResizingDuringAnimation(task);
                    if (task.getStack().getDisplay().getTopStackInWindowingMode(topSecondaryStack).isActivityTypeHome()) {
                        moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                        this.mWindowManager.checkSplitScreenMinimizedChanged(z);
                    }
                }
                this.mWindowManager.continueSurfaceLayout();
                throw th;
            }
        } catch (Throwable th10) {
            th = th10;
            topSecondaryStack = 4;
            i = 3;
            windowingMode = windowingMode2;
            i3 = activityType2;
            activityOptions2 = activityOptions;
            z = false;
            setResizingDuringAnimation(task);
            if (task.getStack().getDisplay().getTopStackInWindowingMode(topSecondaryStack).isActivityTypeHome()) {
            }
            this.mWindowManager.continueSurfaceLayout();
            throw th;
        }
        topSecondaryStack = 4;
        i = 3;
        windowingMode = windowingMode2;
        z = false;
        setResizingDuringAnimation(task);
        if (task.getStack().getDisplay().getTopStackInWindowingMode(topSecondaryStack).isActivityTypeHome()) {
        }
        this.mWindowManager.continueSurfaceLayout();
        throw th;
    }

    List<IBinder> getTopVisibleActivities() {
        ArrayList<IBinder> topActivityTokens = new ArrayList();
        for (int i = this.mActivityDisplays.size() - 1; i >= 0; i--) {
            ActivityDisplay display = (ActivityDisplay) this.mActivityDisplays.valueAt(i);
            for (int j = display.getChildCount() - 1; j >= 0; j--) {
                ActivityStack stack = display.getChildAt(j);
                if (stack.shouldBeVisible(null)) {
                    ActivityRecord top = stack.getTopActivity();
                    if (top != null) {
                        if (stack == this.mFocusedStack) {
                            topActivityTokens.add(0, top.appToken);
                        } else {
                            topActivityTokens.add(top.appToken);
                        }
                    }
                }
            }
        }
        return topActivityTokens;
    }

    protected void showToast(int displayId) {
    }

    protected boolean notKillProcessWhenRemoveTask(ProcessRecord processRecord) {
        return true;
    }
}
