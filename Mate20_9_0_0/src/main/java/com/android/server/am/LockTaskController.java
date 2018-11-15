package com.android.server.am;

import android.app.admin.IDevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.telecom.TelecomManager;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.policy.IKeyguardDismissCallback.Stub;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.pm.DumpState;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class LockTaskController {
    private static final String LOCK_TASK_TAG = "Lock-to-App";
    private static final SparseArray<Pair<Integer, Integer>> STATUS_BAR_FLAG_MAP_LOCKED = new SparseArray();
    @VisibleForTesting
    static final int STATUS_BAR_MASK_LOCKED = 61210624;
    @VisibleForTesting
    static final int STATUS_BAR_MASK_PINNED = 43974656;
    private static final String TAG = "ActivityManager";
    private static final String TAG_LOCKTASK = "ActivityManager";
    private final Context mContext;
    @VisibleForTesting
    IDevicePolicyManager mDevicePolicyManager;
    private final Handler mHandler;
    @VisibleForTesting
    LockPatternUtils mLockPatternUtils;
    private final SparseIntArray mLockTaskFeatures = new SparseIntArray();
    private int mLockTaskModeState = 0;
    private final ArrayList<TaskRecord> mLockTaskModeTasks = new ArrayList();
    private final SparseArray<String[]> mLockTaskPackages = new SparseArray();
    @VisibleForTesting
    IStatusBarService mStatusBarService;
    private final ActivityStackSupervisor mSupervisor;
    @VisibleForTesting
    TelecomManager mTelecomManager;
    private final IBinder mToken = new Binder();
    @VisibleForTesting
    WindowManagerService mWindowManager;

    static {
        STATUS_BAR_FLAG_MAP_LOCKED.append(1, new Pair(Integer.valueOf(DumpState.DUMP_VOLUMES), Integer.valueOf(2)));
        STATUS_BAR_FLAG_MAP_LOCKED.append(2, new Pair(Integer.valueOf(393216), Integer.valueOf(4)));
        STATUS_BAR_FLAG_MAP_LOCKED.append(4, new Pair(Integer.valueOf(DumpState.DUMP_COMPILER_STATS), Integer.valueOf(0)));
        STATUS_BAR_FLAG_MAP_LOCKED.append(8, new Pair(Integer.valueOf(DumpState.DUMP_SERVICE_PERMISSIONS), Integer.valueOf(0)));
        STATUS_BAR_FLAG_MAP_LOCKED.append(16, new Pair(Integer.valueOf(0), Integer.valueOf(8)));
    }

    LockTaskController(Context context, ActivityStackSupervisor supervisor, Handler handler) {
        this.mContext = context;
        this.mSupervisor = supervisor;
        this.mHandler = handler;
    }

    void setWindowManager(WindowManagerService windowManager) {
        this.mWindowManager = windowManager;
    }

    int getLockTaskModeState() {
        return this.mLockTaskModeState;
    }

    @VisibleForTesting
    boolean isTaskLocked(TaskRecord task) {
        return this.mLockTaskModeTasks.contains(task);
    }

    private boolean isRootTask(TaskRecord task) {
        return this.mLockTaskModeTasks.indexOf(task) == 0;
    }

    boolean activityBlockedFromFinish(ActivityRecord activity) {
        TaskRecord task = activity.getTask();
        if (activity != task.getRootActivity() || activity != task.getTopActivity() || task.mLockTaskAuth == 4 || !isRootTask(task)) {
            return false;
        }
        Slog.i(ActivityManagerService.TAG, "Not finishing task in lock task mode");
        showLockTaskToast();
        return true;
    }

    boolean canMoveTaskToBack(TaskRecord task) {
        if (!isRootTask(task)) {
            return true;
        }
        showLockTaskToast();
        return false;
    }

    boolean isTaskWhitelisted(TaskRecord task) {
        switch (task.mLockTaskAuth) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    boolean isLockTaskModeViolation(TaskRecord task) {
        return isLockTaskModeViolation(task, false);
    }

    boolean isLockTaskModeViolation(TaskRecord task, boolean isNewClearTask) {
        if (!isLockTaskModeViolationInternal(task, isNewClearTask)) {
            return false;
        }
        showLockTaskToast();
        return true;
    }

    TaskRecord getRootTask() {
        if (this.mLockTaskModeTasks.isEmpty()) {
            return null;
        }
        return (TaskRecord) this.mLockTaskModeTasks.get(0);
    }

    private boolean isLockTaskModeViolationInternal(TaskRecord task, boolean isNewClearTask) {
        boolean z = false;
        if (isTaskLocked(task) && !isNewClearTask) {
            return false;
        }
        if (task.isActivityTypeRecents() && isRecentsAllowed(task.userId)) {
            return false;
        }
        if (isKeyguardAllowed(task.userId) && isEmergencyCallTask(task)) {
            return false;
        }
        if (!(isTaskWhitelisted(task) || this.mLockTaskModeTasks.isEmpty())) {
            z = true;
        }
        return z;
    }

    private boolean isRecentsAllowed(int userId) {
        return (getLockTaskFeaturesForUser(userId) & 8) != 0;
    }

    private boolean isKeyguardAllowed(int userId) {
        return (getLockTaskFeaturesForUser(userId) & 32) != 0;
    }

    private boolean isEmergencyCallTask(TaskRecord task) {
        Intent intent = task.intent;
        if (intent == null) {
            return false;
        }
        if (TelecomManager.EMERGENCY_DIALER_COMPONENT.equals(intent.getComponent()) || "android.intent.action.CALL_EMERGENCY".equals(intent.getAction())) {
            return true;
        }
        TelecomManager tm = getTelecomManager();
        String dialerPackage = tm != null ? tm.getSystemDialerPackage() : null;
        if (dialerPackage == null || !dialerPackage.equals(intent.getComponent().getPackageName())) {
            return false;
        }
        return true;
    }

    void stopLockTaskMode(TaskRecord task, boolean isSystemCaller, int callingUid) {
        if (this.mLockTaskModeState != 0) {
            if (isSystemCaller) {
                if (this.mLockTaskModeState == 2) {
                    clearLockedTasks("stopAppPinning");
                } else {
                    Slog.e(ActivityManagerService.TAG, "Attempted to stop LockTask with isSystemCaller=true");
                    showLockTaskToast();
                }
            } else if (task == null) {
                throw new IllegalArgumentException("can't stop LockTask for null task");
            } else if (callingUid == task.mLockTaskUid || (task.mLockTaskUid == 0 && callingUid == task.effectiveUid)) {
                clearLockedTask(task);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid uid, expected ");
                stringBuilder.append(task.mLockTaskUid);
                stringBuilder.append(" callingUid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(" effectiveUid=");
                stringBuilder.append(task.effectiveUid);
                throw new SecurityException(stringBuilder.toString());
            }
        }
    }

    void clearLockedTasks(String reason) {
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clearLockedTasks: ");
            stringBuilder.append(reason);
            Slog.i(str, stringBuilder.toString());
        }
        if (!this.mLockTaskModeTasks.isEmpty()) {
            clearLockedTask((TaskRecord) this.mLockTaskModeTasks.get(0));
        }
    }

    void clearLockedTask(TaskRecord task) {
        if (task != null && !this.mLockTaskModeTasks.isEmpty()) {
            if (task == this.mLockTaskModeTasks.get(0)) {
                for (int taskNdx = this.mLockTaskModeTasks.size() - 1; taskNdx > 0; taskNdx--) {
                    clearLockedTask((TaskRecord) this.mLockTaskModeTasks.get(taskNdx));
                }
            }
            removeLockedTask(task);
            if (!this.mLockTaskModeTasks.isEmpty()) {
                task.performClearTaskLocked();
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
            }
        }
    }

    private void removeLockedTask(TaskRecord task) {
        if (this.mLockTaskModeTasks.remove(task)) {
            String str;
            StringBuilder stringBuilder;
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("removeLockedTask: removed ");
                stringBuilder.append(task);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mLockTaskModeTasks.isEmpty()) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("removeLockedTask: task=");
                    stringBuilder.append(task);
                    stringBuilder.append(" last task, reverting locktask mode. Callers=");
                    stringBuilder.append(Debug.getCallers(3));
                    Slog.d(str, stringBuilder.toString());
                }
                this.mHandler.post(new -$$Lambda$LockTaskController$CFBSOJhWPyFwVT85DSWkAEj1wF0(this, task));
            }
        }
    }

    private void performStopLockTask(int userId) {
        try {
            setStatusBarState(0, userId);
            setKeyguardState(0, userId);
            if (this.mLockTaskModeState == 2) {
                lockKeyguardIfNeeded();
            }
            if (getDevicePolicyManager() != null) {
                getDevicePolicyManager().notifyLockTaskModeChanged(false, null, userId);
            }
            if (this.mLockTaskModeState == 2) {
                getStatusBarService().showPinningEnterExitToast(false);
            }
            this.mWindowManager.onLockTaskStateChanged(0);
            this.mLockTaskModeState = 0;
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        } catch (Throwable th) {
            this.mLockTaskModeState = 0;
        }
    }

    void showLockTaskToast() {
        if (this.mLockTaskModeState == 2) {
            try {
                getStatusBarService().showPinningEscapeToast();
            } catch (RemoteException e) {
                Slog.e(ActivityManagerService.TAG, "Failed to send pinning escape toast", e);
            }
        }
    }

    void startLockTaskMode(TaskRecord task, boolean isSystemCaller, int callingUid) {
        if (!isSystemCaller) {
            task.mLockTaskUid = callingUid;
            if (task.mLockTaskAuth == 1) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.w(ActivityManagerService.TAG, "Mode default, asking user");
                }
                StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                if (statusBarManager != null) {
                    statusBarManager.showScreenPinningRequest(task.taskId);
                }
                return;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(ActivityManagerService.TAG, isSystemCaller ? "Locking pinned" : "Locking fully");
        }
        setLockTaskMode(task, isSystemCaller ? 2 : 1, "startLockTask", true);
    }

    private void setLockTaskMode(TaskRecord task, int lockTaskModeState, String reason, boolean andResume) {
        if (task.mLockTaskAuth == 0) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(ActivityManagerService.TAG, "setLockTaskMode: Can't lock due to auth");
            }
        } else if (isLockTaskModeViolation(task)) {
            Slog.e(ActivityManagerService.TAG, "setLockTaskMode: Attempt to start an unauthorized lock task.");
        } else {
            Intent taskIntent = task.intent;
            if (this.mLockTaskModeTasks.isEmpty() && taskIntent != null) {
                this.mSupervisor.mRecentTasks.onLockTaskModeStateChanged(lockTaskModeState, task.userId);
                this.mHandler.post(new -$$Lambda$LockTaskController$utz-CwgPkuGXoN5jp5hMoe4EpuQ(this, taskIntent, task, lockTaskModeState));
            }
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setLockTaskMode: Locking to ");
                stringBuilder.append(task);
                stringBuilder.append(" Callers=");
                stringBuilder.append(Debug.getCallers(4));
                Slog.w(str, stringBuilder.toString());
            }
            if (!this.mLockTaskModeTasks.contains(task)) {
                this.mLockTaskModeTasks.add(task);
            }
            if (task.mLockTaskUid == -1) {
                task.mLockTaskUid = task.effectiveUid;
            }
            if (andResume) {
                this.mSupervisor.findTaskToMoveToFront(task, 0, null, reason, lockTaskModeState != 0);
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
                this.mWindowManager.executeAppTransition();
            } else if (lockTaskModeState != 0) {
                this.mSupervisor.handleNonResizableTaskIfNeeded(task, 0, 0, task.getStack(), true);
            }
        }
    }

    private void performStartLockTask(String packageName, int userId, int lockTaskModeState) {
        if (lockTaskModeState == 2) {
            try {
                getStatusBarService().showPinningEnterExitToast(true);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
        this.mWindowManager.onLockTaskStateChanged(lockTaskModeState);
        this.mLockTaskModeState = lockTaskModeState;
        setStatusBarState(lockTaskModeState, userId);
        setKeyguardState(lockTaskModeState, userId);
        if (getDevicePolicyManager() != null) {
            getDevicePolicyManager().notifyLockTaskModeChanged(true, packageName, userId);
        }
    }

    void updateLockTaskPackages(int userId, String[] packages) {
        TaskRecord lockedTask;
        String str;
        this.mLockTaskPackages.put(userId, packages);
        boolean taskChanged = false;
        int taskNdx = this.mLockTaskModeTasks.size() - 1;
        while (true) {
            boolean isWhitelisted = false;
            if (taskNdx < 0) {
                break;
            }
            lockedTask = (TaskRecord) this.mLockTaskModeTasks.get(taskNdx);
            boolean wasWhitelisted = lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3;
            lockedTask.setLockTaskAuth();
            if (lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3) {
                isWhitelisted = true;
            }
            if (this.mLockTaskModeState == 1 && lockedTask.userId == userId && wasWhitelisted && !isWhitelisted) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onLockTaskPackagesUpdated: removing ");
                    stringBuilder.append(lockedTask);
                    stringBuilder.append(" mLockTaskAuth()=");
                    stringBuilder.append(lockedTask.lockTaskAuthToString());
                    Slog.d(str, stringBuilder.toString());
                }
                removeLockedTask(lockedTask);
                lockedTask.performClearTaskLocked();
                taskChanged = true;
            }
            taskNdx--;
        }
        for (taskNdx = this.mSupervisor.getChildCount() - 1; taskNdx >= 0; taskNdx--) {
            this.mSupervisor.getChildAt(taskNdx).onLockTaskPackagesUpdated();
        }
        ActivityRecord r = this.mSupervisor.topRunningActivityLocked();
        lockedTask = r != null ? r.getTask() : null;
        if (this.mLockTaskModeTasks.isEmpty() && lockedTask != null && lockedTask.mLockTaskAuth == 2) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                str = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onLockTaskPackagesUpdated: starting new locktask task=");
                stringBuilder2.append(lockedTask);
                Slog.d(str, stringBuilder2.toString());
            }
            setLockTaskMode(lockedTask, 1, "package updated", false);
            taskChanged = true;
        }
        if (taskChanged) {
            this.mSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    boolean isPackageWhitelisted(int userId, String pkg) {
        if (pkg == null) {
            return false;
        }
        String[] whitelist = (String[]) this.mLockTaskPackages.get(userId);
        if (whitelist == null) {
            return false;
        }
        for (String whitelistedPkg : whitelist) {
            if (pkg.equals(whitelistedPkg)) {
                return true;
            }
        }
        return false;
    }

    void updateLockTaskFeatures(int userId, int flags) {
        if (flags != getLockTaskFeaturesForUser(userId)) {
            this.mLockTaskFeatures.put(userId, flags);
            if (!this.mLockTaskModeTasks.isEmpty() && userId == ((TaskRecord) this.mLockTaskModeTasks.get(0)).userId) {
                this.mHandler.post(new -$$Lambda$LockTaskController$HCwwKEV1_Hy1M3bHXdwhoMEXmJM(this, userId));
            }
        }
    }

    public static /* synthetic */ void lambda$updateLockTaskFeatures$2(LockTaskController lockTaskController, int userId) {
        if (lockTaskController.mLockTaskModeState == 1) {
            lockTaskController.setStatusBarState(lockTaskController.mLockTaskModeState, userId);
            lockTaskController.setKeyguardState(lockTaskController.mLockTaskModeState, userId);
        }
    }

    private void setStatusBarState(int lockTaskModeState, int userId) {
        IStatusBarService statusBar = getStatusBarService();
        if (statusBar == null) {
            Slog.e(ActivityManagerService.TAG, "Can't find StatusBarService");
            return;
        }
        int flags1 = 0;
        int flags2 = 0;
        if (lockTaskModeState == 2) {
            flags1 = STATUS_BAR_MASK_PINNED;
        } else if (lockTaskModeState == 1) {
            Pair<Integer, Integer> statusBarFlags = getStatusBarDisableFlags(getLockTaskFeaturesForUser(userId));
            flags1 = ((Integer) statusBarFlags.first).intValue();
            flags2 = ((Integer) statusBarFlags.second).intValue();
        }
        try {
            statusBar.disable(flags1, this.mToken, this.mContext.getPackageName());
            statusBar.disable2(flags2, this.mToken, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Slog.e(ActivityManagerService.TAG, "Failed to set status bar flags", e);
        }
    }

    private void setKeyguardState(int lockTaskModeState, int userId) {
        if (lockTaskModeState == 0) {
            this.mWindowManager.reenableKeyguard(this.mToken);
        } else if (lockTaskModeState != 1) {
            this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG);
        } else if (isKeyguardAllowed(userId)) {
            this.mWindowManager.reenableKeyguard(this.mToken);
        } else if (!this.mWindowManager.isKeyguardLocked() || this.mWindowManager.isKeyguardSecure()) {
            this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG);
        } else {
            this.mWindowManager.dismissKeyguard(new Stub() {
                public void onDismissError() throws RemoteException {
                    Slog.i(ActivityManagerService.TAG, "setKeyguardState: failed to dismiss keyguard");
                }

                public void onDismissSucceeded() throws RemoteException {
                    LockTaskController.this.mHandler.post(new -$$Lambda$LockTaskController$1$kkBevvrWTeS1ZnzjKqWe3h2dPhk(this));
                }

                public void onDismissCancelled() throws RemoteException {
                    Slog.i(ActivityManagerService.TAG, "setKeyguardState: dismiss cancelled");
                }
            }, null);
        }
    }

    private void lockKeyguardIfNeeded() {
        try {
            if (Secure.getIntForUser(this.mContext.getContentResolver(), "lock_to_app_exit_locked", -2) != 0) {
                this.mWindowManager.lockNow(null);
                this.mWindowManager.dismissKeyguard(null, null);
                getLockPatternUtils().requireCredentialEntry(-1);
            }
        } catch (SettingNotFoundException e) {
        }
    }

    @VisibleForTesting
    Pair<Integer, Integer> getStatusBarDisableFlags(int lockTaskFlags) {
        int flags1 = 67043328;
        int flags2 = 31;
        for (int i = STATUS_BAR_FLAG_MAP_LOCKED.size() - 1; i >= 0; i--) {
            Pair<Integer, Integer> statusBarFlags = (Pair) STATUS_BAR_FLAG_MAP_LOCKED.valueAt(i);
            if ((STATUS_BAR_FLAG_MAP_LOCKED.keyAt(i) & lockTaskFlags) != 0) {
                flags1 &= ~((Integer) statusBarFlags.first).intValue();
                flags2 &= ~((Integer) statusBarFlags.second).intValue();
            }
        }
        return new Pair(Integer.valueOf(flags1 & STATUS_BAR_MASK_LOCKED), Integer.valueOf(flags2));
    }

    private int getLockTaskFeaturesForUser(int userId) {
        return this.mLockTaskFeatures.get(userId, 0);
    }

    private IStatusBarService getStatusBarService() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
            if (this.mStatusBarService == null) {
                Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
            }
        }
        return this.mStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
            if (this.mDevicePolicyManager == null) {
                Slog.w(ActivityManagerService.TAG, "warning: no DEVICE_POLICY_SERVICE");
            }
        }
        return this.mDevicePolicyManager;
    }

    private LockPatternUtils getLockPatternUtils() {
        if (this.mLockPatternUtils == null) {
            return new LockPatternUtils(this.mContext);
        }
        return this.mLockPatternUtils;
    }

    private TelecomManager getTelecomManager() {
        if (this.mTelecomManager == null) {
            return (TelecomManager) this.mContext.getSystemService(TelecomManager.class);
        }
        return this.mTelecomManager;
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("LockTaskController");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        prefix = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mLockTaskModeState=");
        stringBuilder.append(lockTaskModeToString());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mLockTaskModeTasks=");
        pw.println(stringBuilder.toString());
        int i = 0;
        for (int i2 = 0; i2 < this.mLockTaskModeTasks.size(); i2++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("  #");
            stringBuilder2.append(i2);
            stringBuilder2.append(" ");
            stringBuilder2.append(this.mLockTaskModeTasks.get(i2));
            pw.println(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(prefix);
        stringBuilder3.append("mLockTaskPackages (userId:packages)=");
        pw.println(stringBuilder3.toString());
        while (i < this.mLockTaskPackages.size()) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("  u");
            stringBuilder3.append(this.mLockTaskPackages.keyAt(i));
            stringBuilder3.append(":");
            stringBuilder3.append(Arrays.toString((Object[]) this.mLockTaskPackages.valueAt(i)));
            pw.println(stringBuilder3.toString());
            i++;
        }
    }

    private String lockTaskModeToString() {
        switch (this.mLockTaskModeState) {
            case 0:
                return "NONE";
            case 1:
                return "LOCKED";
            case 2:
                return "PINNED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown=");
                stringBuilder.append(this.mLockTaskModeState);
                return stringBuilder.toString();
        }
    }
}
