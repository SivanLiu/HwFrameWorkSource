package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.TaskDescription;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RecentTasks {
    private static final int DEFAULT_INITIAL_CAPACITY = 5;
    private static final boolean MOVE_AFFILIATED_TASKS_TO_FRONT = false;
    private static final ActivityInfo NO_ACTIVITY_INFO_TOKEN = new ActivityInfo();
    private static final ApplicationInfo NO_APPLICATION_INFO_TOKEN = new ApplicationInfo();
    private static final String TAG = "ActivityManager";
    private static final String TAG_RECENTS = "ActivityManager";
    private static final String TAG_TASKS = "ActivityManager";
    private static final Comparator<TaskRecord> TASK_ID_COMPARATOR = -$$Lambda$RecentTasks$NgzE6eN0wIO1cgLW7RzciPDBTHk.INSTANCE;
    private static final boolean TRIMMED = true;
    private long mActiveTasksSessionDurationMs;
    private final ArrayList<Callbacks> mCallbacks = new ArrayList();
    private int mGlobalMaxNumTasks;
    private boolean mHasVisibleRecentTasks;
    private final boolean mIsLite = SystemProperties.getBoolean("ro.build.hw_emui_ultra_lite", false);
    private int mMaxNumVisibleTasks;
    private int mMinNumVisibleTasks;
    private final SparseArray<SparseBooleanArray> mPersistedTaskIds = new SparseArray(5);
    private ComponentName mRecentsComponent = null;
    private int mRecentsUid = -1;
    private final ActivityManagerService mService;
    private final TaskPersister mTaskPersister;
    private final ArrayList<TaskRecord> mTasks = new ArrayList();
    private final HashMap<ComponentName, ActivityInfo> mTmpAvailActCache = new HashMap();
    private final HashMap<String, ApplicationInfo> mTmpAvailAppCache = new HashMap();
    private final SparseBooleanArray mTmpQuietProfileUserIds = new SparseBooleanArray();
    private final ArrayList<TaskRecord> mTmpRecents = new ArrayList();
    private final TaskActivitiesReport mTmpReport = new TaskActivitiesReport();
    private final UserController mUserController;
    private final SparseBooleanArray mUsersWithRecentsLoaded = new SparseBooleanArray(5);

    interface Callbacks {
        void onRecentTaskAdded(TaskRecord taskRecord);

        void onRecentTaskRemoved(TaskRecord taskRecord, boolean z);
    }

    @VisibleForTesting
    RecentTasks(ActivityManagerService service, TaskPersister taskPersister, UserController userController) {
        this.mService = service;
        this.mUserController = userController;
        this.mTaskPersister = taskPersister;
        this.mGlobalMaxNumTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mHasVisibleRecentTasks = true;
    }

    RecentTasks(ActivityManagerService service, ActivityStackSupervisor stackSupervisor) {
        File systemDir = Environment.getDataSystemDirectory();
        Resources res = service.mContext.getResources();
        this.mService = service;
        this.mUserController = service.mUserController;
        this.mTaskPersister = new TaskPersister(systemDir, stackSupervisor, service, this);
        this.mGlobalMaxNumTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mHasVisibleRecentTasks = res.getBoolean(17956982);
        loadParametersFromResources(res);
    }

    @VisibleForTesting
    void setParameters(int minNumVisibleTasks, int maxNumVisibleTasks, long activeSessionDurationMs) {
        this.mMinNumVisibleTasks = minNumVisibleTasks;
        this.mMaxNumVisibleTasks = maxNumVisibleTasks;
        this.mActiveTasksSessionDurationMs = activeSessionDurationMs;
    }

    @VisibleForTesting
    void setGlobalMaxNumTasks(int globalMaxNumTasks) {
        this.mGlobalMaxNumTasks = globalMaxNumTasks;
    }

    @VisibleForTesting
    void loadParametersFromResources(Resources res) {
        long toMillis;
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mMinNumVisibleTasks = res.getInteger(17694818);
            this.mMaxNumVisibleTasks = res.getInteger(17694810);
        } else if (SystemProperties.getBoolean("ro.recents.grid", false)) {
            this.mMinNumVisibleTasks = res.getInteger(17694817);
            this.mMaxNumVisibleTasks = res.getInteger(17694809);
        } else {
            this.mMinNumVisibleTasks = res.getInteger(17694816);
            this.mMaxNumVisibleTasks = res.getInteger(17694808);
        }
        int sessionDurationHrs = res.getInteger(17694728);
        if (sessionDurationHrs > 0) {
            toMillis = TimeUnit.HOURS.toMillis((long) sessionDurationHrs);
        } else {
            toMillis = -1;
        }
        this.mActiveTasksSessionDurationMs = toMillis;
    }

    void loadRecentsComponent(Resources res) {
        String rawRecentsComponent = res.getString(17039840);
        if (!TextUtils.isEmpty(rawRecentsComponent)) {
            ComponentName cn = ComponentName.unflattenFromString(rawRecentsComponent);
            if (cn != null) {
                try {
                    ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(cn.getPackageName(), 0, this.mService.mContext.getUserId());
                    if (appInfo != null) {
                        this.mRecentsUid = appInfo.uid;
                        this.mRecentsComponent = cn;
                    }
                } catch (RemoteException e) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not load application info for recents component: ");
                    stringBuilder.append(cn);
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }
    }

    boolean isCallerRecents(int callingUid) {
        return UserHandle.isSameApp(callingUid, this.mRecentsUid);
    }

    boolean isRecentsComponent(ComponentName cn, int uid) {
        return cn.equals(this.mRecentsComponent) && UserHandle.isSameApp(uid, this.mRecentsUid);
    }

    boolean isRecentsComponentHomeActivity(int userId) {
        ComponentName defaultHomeActivity = this.mService.getPackageManagerInternalLocked().getDefaultHomeActivity(userId);
        return (defaultHomeActivity == null || this.mRecentsComponent == null || !defaultHomeActivity.getPackageName().equals(this.mRecentsComponent.getPackageName())) ? false : true;
    }

    ComponentName getRecentsComponent() {
        return this.mRecentsComponent;
    }

    int getRecentsComponentUid() {
        return this.mRecentsUid;
    }

    void registerCallback(Callbacks callback) {
        this.mCallbacks.add(callback);
    }

    void unregisterCallback(Callbacks callback) {
        this.mCallbacks.remove(callback);
    }

    private void notifyTaskAdded(TaskRecord task) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callbacks) this.mCallbacks.get(i)).onRecentTaskAdded(task);
        }
    }

    private void notifyTaskRemoved(TaskRecord task, boolean wasTrimmed) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            ((Callbacks) this.mCallbacks.get(i)).onRecentTaskRemoved(task, wasTrimmed);
        }
    }

    void loadUserRecentsLocked(int userId) {
        if (!this.mUsersWithRecentsLoaded.get(userId) && !this.mIsLite) {
            loadPersistedTaskIdsForUserLocked(userId);
            SparseBooleanArray preaddedTasks = new SparseBooleanArray();
            Iterator it = this.mTasks.iterator();
            while (it.hasNext()) {
                TaskRecord task = (TaskRecord) it.next();
                if (task.userId == userId && shouldPersistTaskLocked(task)) {
                    preaddedTasks.put(task.taskId, true);
                }
            }
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading recents for user ");
            stringBuilder.append(userId);
            stringBuilder.append(" into memory.");
            Slog.i(str, stringBuilder.toString());
            this.mTasks.addAll(this.mTaskPersister.restoreTasksForUserLocked(userId, preaddedTasks));
            cleanupLocked(userId);
            this.mUsersWithRecentsLoaded.put(userId, true);
            if (preaddedTasks.size() > 0) {
                syncPersistentTaskIdsLocked();
            }
        }
    }

    private void loadPersistedTaskIdsForUserLocked(int userId) {
        if (this.mPersistedTaskIds.get(userId) == null) {
            this.mPersistedTaskIds.put(userId, this.mTaskPersister.loadPersistedTaskIdsForUser(userId));
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loaded persisted task ids for user ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
    }

    boolean containsTaskId(int taskId, int userId) {
        loadPersistedTaskIdsForUserLocked(userId);
        return ((SparseBooleanArray) this.mPersistedTaskIds.get(userId)).get(taskId);
    }

    SparseBooleanArray getTaskIdsForUser(int userId) {
        loadPersistedTaskIdsForUserLocked(userId);
        return (SparseBooleanArray) this.mPersistedTaskIds.get(userId);
    }

    void notifyTaskPersisterLocked(TaskRecord task, boolean flush) {
        ActivityStack stack = task != null ? task.getStack() : null;
        if (stack != null && stack.isHomeOrRecentsStack()) {
            return;
        }
        if ((stack == null || !HwPCUtils.isExtDynamicStack(stack.getStackId())) && !this.mIsLite) {
            syncPersistentTaskIdsLocked();
            this.mTaskPersister.wakeup(task, flush);
        }
    }

    private void syncPersistentTaskIdsLocked() {
        int i;
        for (i = this.mPersistedTaskIds.size() - 1; i >= 0; i--) {
            if (this.mUsersWithRecentsLoaded.get(this.mPersistedTaskIds.keyAt(i))) {
                ((SparseBooleanArray) this.mPersistedTaskIds.valueAt(i)).clear();
            }
        }
        for (i = this.mTasks.size() - 1; i >= 0; i--) {
            TaskRecord task = (TaskRecord) this.mTasks.get(i);
            if (shouldPersistTaskLocked(task)) {
                if (this.mPersistedTaskIds.get(task.userId) == null) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No task ids found for userId ");
                    stringBuilder.append(task.userId);
                    stringBuilder.append(". task=");
                    stringBuilder.append(task);
                    stringBuilder.append(" mPersistedTaskIds=");
                    stringBuilder.append(this.mPersistedTaskIds);
                    Slog.wtf(str, stringBuilder.toString());
                    this.mPersistedTaskIds.put(task.userId, new SparseBooleanArray());
                }
                ((SparseBooleanArray) this.mPersistedTaskIds.get(task.userId)).put(task.taskId, true);
            }
        }
    }

    private static boolean shouldPersistTaskLocked(TaskRecord task) {
        ActivityStack stack = task.getStack();
        return task.isPersistable && (stack == null || !stack.isHomeOrRecentsStack());
    }

    void onSystemReadyLocked() {
        if (!this.mIsLite) {
            loadRecentsComponent(this.mService.mContext.getResources());
            this.mTasks.clear();
            this.mTaskPersister.startPersisting();
        }
    }

    Bitmap getTaskDescriptionIcon(String path) {
        return this.mTaskPersister.getTaskDescriptionIcon(path);
    }

    void saveImage(Bitmap image, String path) {
        if (!this.mIsLite) {
            this.mTaskPersister.saveImage(image, path);
        }
    }

    void flush() {
        if (!this.mIsLite) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    syncPersistentTaskIdsLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            this.mTaskPersister.flush();
        }
    }

    int[] usersWithRecentsLoadedLocked() {
        int[] usersWithRecentsLoaded = new int[this.mUsersWithRecentsLoaded.size()];
        int len = 0;
        for (int i = 0; i < usersWithRecentsLoaded.length; i++) {
            int userId = this.mUsersWithRecentsLoaded.keyAt(i);
            if (this.mUsersWithRecentsLoaded.valueAt(i)) {
                int len2 = len + 1;
                usersWithRecentsLoaded[len] = userId;
                len = len2;
            }
        }
        if (len < usersWithRecentsLoaded.length) {
            return Arrays.copyOf(usersWithRecentsLoaded, len);
        }
        return usersWithRecentsLoaded;
    }

    void unloadUserDataFromMemoryLocked(int userId) {
        if (this.mUsersWithRecentsLoaded.get(userId)) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unloading recents for user ");
            stringBuilder.append(userId);
            stringBuilder.append(" from memory.");
            Slog.i(str, stringBuilder.toString());
            this.mUsersWithRecentsLoaded.delete(userId);
            removeTasksForUserLocked(userId);
        }
        this.mPersistedTaskIds.delete(userId);
        this.mTaskPersister.unloadUserDataFromMemory(userId);
    }

    private void removeTasksForUserLocked(int userId) {
        if (userId <= 0) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't remove recent task on user ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            return;
        }
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (tr.userId == userId) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("remove RecentTask ");
                    stringBuilder2.append(tr);
                    stringBuilder2.append(" when finishing user");
                    stringBuilder2.append(userId);
                    Slog.i(str2, stringBuilder2.toString());
                }
                remove((TaskRecord) this.mTasks.get(i));
            }
        }
    }

    void onPackagesSuspendedChanged(String[] packages, boolean suspended, int userId) {
        Set<String> packageNames = Sets.newHashSet(packages);
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (tr.realActivity != null && packageNames.contains(tr.realActivity.getPackageName()) && tr.userId == userId && tr.realActivitySuspended != suspended) {
                tr.realActivitySuspended = suspended;
                if (suspended) {
                    this.mService.mStackSupervisor.removeTaskByIdLocked(tr.taskId, false, true, "suspended-package");
                }
                notifyTaskPersisterLocked(tr, false);
            }
        }
    }

    void onLockTaskModeStateChanged(int lockTaskModeState, int userId) {
        if (lockTaskModeState == 1) {
            int i = this.mTasks.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    TaskRecord tr = (TaskRecord) this.mTasks.get(i2);
                    if (tr.userId == userId && !this.mService.getLockTaskController().isTaskWhitelisted(tr)) {
                        remove(tr);
                    }
                    i = i2 - 1;
                } else {
                    return;
                }
            }
        }
    }

    void removeTasksByPackageName(String packageName, int userId) {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            String taskPackageName = tr.getBaseIntent().getComponent().getPackageName();
            if (tr.userId == userId && taskPackageName.equals(packageName)) {
                this.mService.mStackSupervisor.removeTaskByIdLocked(tr.taskId, true, true, "remove-package-task");
            }
        }
    }

    void cleanupDisabledPackageTasksLocked(String packageName, Set<String> filterByClasses, int userId) {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (userId == -1 || tr.userId == userId) {
                ComponentName cn = tr.intent != null ? tr.intent.getComponent() : null;
                boolean sameComponent = cn != null && cn.getPackageName().equals(packageName) && (filterByClasses == null || filterByClasses.contains(cn.getClassName()));
                if (sameComponent) {
                    this.mService.mStackSupervisor.removeTaskByIdLocked(tr.taskId, false, true, "disabled-package");
                }
            }
        }
    }

    void cleanupLocked(int userId) {
        int recentsCount = this.mTasks.size();
        if (recentsCount != 0) {
            int i;
            this.mTmpAvailActCache.clear();
            this.mTmpAvailAppCache.clear();
            IPackageManager pm = AppGlobals.getPackageManager();
            for (i = recentsCount - 1; i >= 0; i--) {
                TaskRecord task = (TaskRecord) this.mTasks.get(i);
                if (userId == -1 || task.userId == userId) {
                    if (task.autoRemoveRecents && task.getTopActivity() == null) {
                        this.mTasks.remove(i);
                        notifyTaskRemoved(task, false);
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Removing auto-remove without activity: ");
                        stringBuilder.append(task);
                        Slog.w(str, stringBuilder.toString());
                    } else if (task.realActivity != null) {
                        ActivityInfo ai = (ActivityInfo) this.mTmpAvailActCache.get(task.realActivity);
                        if (ai == null) {
                            try {
                                ai = pm.getActivityInfo(task.realActivity, 268436480, userId);
                                if (ai == null) {
                                    ai = NO_ACTIVITY_INFO_TOKEN;
                                }
                                this.mTmpAvailActCache.put(task.realActivity, ai);
                            } catch (RemoteException e) {
                            }
                        }
                        String str2;
                        StringBuilder stringBuilder2;
                        if (ai == NO_ACTIVITY_INFO_TOKEN) {
                            ApplicationInfo app = (ApplicationInfo) this.mTmpAvailAppCache.get(task.realActivity.getPackageName());
                            if (app == null) {
                                try {
                                    app = pm.getApplicationInfo(task.realActivity.getPackageName(), 8192, userId);
                                    if (app == null) {
                                        app = NO_APPLICATION_INFO_TOKEN;
                                    }
                                    this.mTmpAvailAppCache.put(task.realActivity.getPackageName(), app);
                                } catch (RemoteException e2) {
                                }
                            }
                            if (app == NO_APPLICATION_INFO_TOKEN || (DumpState.DUMP_VOLUMES & app.flags) == 0) {
                                this.mTasks.remove(i);
                                notifyTaskRemoved(task, false);
                                str2 = ActivityManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Removing no longer valid recent: ");
                                stringBuilder2.append(task);
                                Slog.w(str2, stringBuilder2.toString());
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                                    String str3 = ActivityManagerService.TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Making recent unavailable: ");
                                    stringBuilder3.append(task);
                                    Slog.d(str3, stringBuilder3.toString());
                                }
                                task.isAvailable = false;
                            }
                        } else if (ai.enabled && ai.applicationInfo.enabled && (ai.applicationInfo.flags & DumpState.DUMP_VOLUMES) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS && !task.isAvailable) {
                                str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Making recent available: ");
                                stringBuilder4.append(task);
                                Slog.d(str2, stringBuilder4.toString());
                            }
                            task.isAvailable = true;
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                                String str4 = ActivityManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Making recent unavailable: ");
                                stringBuilder2.append(task);
                                stringBuilder2.append(" (enabled=");
                                stringBuilder2.append(ai.enabled);
                                stringBuilder2.append(SliceAuthority.DELIMITER);
                                stringBuilder2.append(ai.applicationInfo.enabled);
                                stringBuilder2.append(" flags=");
                                stringBuilder2.append(Integer.toHexString(ai.applicationInfo.flags));
                                stringBuilder2.append(")");
                                Slog.d(str4, stringBuilder2.toString());
                            }
                            task.isAvailable = false;
                        }
                    }
                }
            }
            i = 0;
            recentsCount = this.mTasks.size();
            while (i < recentsCount) {
                i = processNextAffiliateChainLocked(i);
            }
        }
    }

    private boolean canAddTaskWithoutTrim(TaskRecord task) {
        return findRemoveIndexForAddTask(task) == -1;
    }

    ArrayList<IBinder> getAppTasksList(int callingUid, String callingPackage) {
        ArrayList<IBinder> list = new ArrayList();
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (tr.effectiveUid == callingUid) {
                Intent intent = tr.getBaseIntent();
                if (intent != null && callingPackage.equals(intent.getComponent().getPackageName())) {
                    list.add(new AppTaskImpl(this.mService, createRecentTaskInfo(tr).persistentId, callingUid).asBinder());
                }
            }
        }
        return list;
    }

    ParceledListSlice<RecentTaskInfo> getRecentTasks(int maxNum, int flags, boolean getTasksAllowed, boolean getDetailedTasks, int userId, int callingUid) {
        RecentTasks recentTasks = this;
        int i = userId;
        int i2 = 0;
        boolean withExcluded = (flags & 1) != 0;
        if (recentTasks.mService.isUserRunning(i, 4)) {
            int i3;
            int i4;
            recentTasks.loadUserRecentsLocked(i);
            Set<Integer> includedUsers = recentTasks.mUserController.getProfileIds(i);
            includedUsers.add(Integer.valueOf(userId));
            ArrayList<RecentTaskInfo> res = new ArrayList();
            int size = recentTasks.mTasks.size();
            int numVisibleTasks = 0;
            while (i2 < size) {
                TaskRecord tr = (TaskRecord) recentTasks.mTasks.get(i2);
                if (recentTasks.isVisibleRecentTask(tr)) {
                    numVisibleTasks++;
                    if (recentTasks.isInVisibleRange(tr, numVisibleTasks)) {
                        if (res.size() < maxNum) {
                            String str;
                            StringBuilder stringBuilder;
                            if (includedUsers.contains(Integer.valueOf(tr.userId))) {
                                if (tr.realActivitySuspended) {
                                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                        str = ActivityManagerService.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Skipping, activity suspended: ");
                                        stringBuilder.append(tr);
                                        Slog.d(str, stringBuilder.toString());
                                    }
                                } else if (withExcluded || tr.intent == null || (tr.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0) {
                                    String str2;
                                    StringBuilder stringBuilder2;
                                    if (getTasksAllowed || tr.isActivityTypeHome()) {
                                        i3 = callingUid;
                                    } else if (tr.effectiveUid != callingUid) {
                                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                            str2 = ActivityManagerService.TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Skipping, not allowed: ");
                                            stringBuilder2.append(tr);
                                            Slog.d(str2, stringBuilder2.toString());
                                        }
                                        i2++;
                                        recentTasks = this;
                                    }
                                    if (tr.autoRemoveRecents && tr.getTopActivity() == null) {
                                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                            str2 = ActivityManagerService.TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Skipping, auto-remove without activity: ");
                                            stringBuilder2.append(tr);
                                            Slog.d(str2, stringBuilder2.toString());
                                        }
                                        i2++;
                                        recentTasks = this;
                                    } else if ((flags & 2) == 0 || tr.isAvailable) {
                                        if (tr.mUserSetupComplete) {
                                            RecentTaskInfo rti = recentTasks.createRecentTaskInfo(tr);
                                            if (!getDetailedTasks) {
                                                rti.baseIntent.replaceExtras((Bundle) null);
                                            }
                                            res.add(rti);
                                        } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                            str2 = ActivityManagerService.TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Skipping, user setup not complete: ");
                                            stringBuilder2.append(tr);
                                            Slog.d(str2, stringBuilder2.toString());
                                        }
                                        i2++;
                                        recentTasks = this;
                                    } else {
                                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                            str2 = ActivityManagerService.TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Skipping, unavail real act: ");
                                            stringBuilder2.append(tr);
                                            Slog.d(str2, stringBuilder2.toString());
                                        }
                                        i2++;
                                        recentTasks = this;
                                    }
                                }
                            } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                str = ActivityManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Skipping, not user: ");
                                stringBuilder.append(tr);
                                Slog.d(str, stringBuilder.toString());
                            }
                        }
                        i3 = callingUid;
                        i2++;
                        recentTasks = this;
                    }
                }
                i4 = maxNum;
                i3 = callingUid;
                i2++;
                recentTasks = this;
            }
            i4 = maxNum;
            i3 = callingUid;
            return new ParceledListSlice(res);
        }
        String str3 = ActivityManagerService.TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("user ");
        stringBuilder3.append(i);
        stringBuilder3.append(" is still locked. Cannot load recents");
        Slog.i(str3, stringBuilder3.toString());
        return ParceledListSlice.emptyList();
    }

    void getPersistableTaskIds(ArraySet<Integer> persistentTaskIds) {
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            TaskRecord task = (TaskRecord) this.mTasks.get(i);
            ActivityStack stack = task.getStack();
            if ((task.isPersistable || task.inRecents) && (stack == null || !stack.isHomeOrRecentsStack())) {
                persistentTaskIds.add(Integer.valueOf(task.taskId));
            }
        }
    }

    @VisibleForTesting
    ArrayList<TaskRecord> getRawTasks() {
        return this.mTasks;
    }

    SparseBooleanArray getRecentTaskIds() {
        SparseBooleanArray res = new SparseBooleanArray();
        int size = this.mTasks.size();
        int numVisibleTasks = 0;
        for (int i = 0; i < size; i++) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (isVisibleRecentTask(tr)) {
                numVisibleTasks++;
                if (isInVisibleRange(tr, numVisibleTasks)) {
                    res.put(tr.taskId, true);
                }
            }
        }
        return res;
    }

    TaskRecord getTask(int id) {
        int recentsCount = this.mTasks.size();
        for (int i = 0; i < recentsCount; i++) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (tr.taskId == id) {
                return tr;
            }
        }
        return null;
    }

    void add(TaskRecord task) {
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("add: task=");
            stringBuilder.append(task);
            Slog.d(str, stringBuilder.toString());
        }
        if (task.mStack == null || !HwPCUtils.isExtDynamicStack(task.mStack.mStackId)) {
            boolean isAffiliated = (task.mAffiliatedTaskId == task.taskId && task.mNextAffiliateTaskId == -1 && task.mPrevAffiliateTaskId == -1) ? false : true;
            int recentsCount = this.mTasks.size();
            String str2;
            StringBuilder stringBuilder2;
            if (task.voiceSession != null) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addRecent: not adding voice interaction ");
                    stringBuilder2.append(task);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if (!isAffiliated && recentsCount > 0 && this.mTasks.get(0) == task) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addRecent: already at top: ");
                    stringBuilder2.append(task);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if (isAffiliated && recentsCount > 0 && task.inRecents && task.mAffiliatedTaskId == ((TaskRecord) this.mTasks.get(0)).mAffiliatedTaskId) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("addRecent: affiliated ");
                    stringBuilder3.append(this.mTasks.get(0));
                    stringBuilder3.append(" at top when adding ");
                    stringBuilder3.append(task);
                    Slog.d(str2, stringBuilder3.toString());
                }
            } else {
                int taskIndex;
                String str3;
                StringBuilder stringBuilder4;
                String str4;
                StringBuilder stringBuilder5;
                boolean needAffiliationFix = false;
                if (task.inRecents) {
                    taskIndex = this.mTasks.indexOf(task);
                    if (taskIndex >= 0) {
                        this.mTasks.remove(taskIndex);
                        this.mTasks.add(0, task);
                        notifyTaskPersisterLocked(task, false);
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("addRecent: moving to top ");
                            stringBuilder2.append(task);
                            stringBuilder2.append(" from ");
                            stringBuilder2.append(taskIndex);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                        return;
                    }
                    str3 = ActivityManagerService.TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Task with inRecent not in recents: ");
                    stringBuilder4.append(task);
                    Slog.wtf(str3, stringBuilder4.toString());
                    needAffiliationFix = true;
                }
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    str4 = ActivityManagerService.TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("addRecent: trimming tasks for ");
                    stringBuilder5.append(task);
                    Slog.d(str4, stringBuilder5.toString());
                }
                removeForAddTask(task);
                task.inRecents = true;
                if (!isAffiliated || needAffiliationFix) {
                    this.mTasks.add(0, task);
                    notifyTaskAdded(task);
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        str2 = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("addRecent: adding ");
                        stringBuilder2.append(task);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                } else if (isAffiliated) {
                    TaskRecord other = task.mNextAffiliate;
                    if (other == null) {
                        other = task.mPrevAffiliate;
                    }
                    if (other != null) {
                        int otherIndex = this.mTasks.indexOf(other);
                        if (otherIndex >= 0) {
                            if (other == task.mNextAffiliate) {
                                taskIndex = otherIndex + 1;
                            } else {
                                taskIndex = otherIndex;
                            }
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                str3 = ActivityManagerService.TAG;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("addRecent: new affiliated task added at ");
                                stringBuilder4.append(taskIndex);
                                stringBuilder4.append(": ");
                                stringBuilder4.append(task);
                                Slog.d(str3, stringBuilder4.toString());
                            }
                            this.mTasks.add(taskIndex, task);
                            notifyTaskAdded(task);
                            if (!moveAffiliatedTasksToFront(task, taskIndex)) {
                                needAffiliationFix = true;
                            } else {
                                return;
                            }
                        }
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            str4 = ActivityManagerService.TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("addRecent: couldn't find other affiliation ");
                            stringBuilder5.append(other);
                            Slog.d(str4, stringBuilder5.toString());
                        }
                        needAffiliationFix = true;
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            String str5 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder6 = new StringBuilder();
                            stringBuilder6.append("addRecent: adding affiliated task without next/prev:");
                            stringBuilder6.append(task);
                            Slog.d(str5, stringBuilder6.toString());
                        }
                        needAffiliationFix = true;
                    }
                }
                if (needAffiliationFix) {
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(ActivityManagerService.TAG, "addRecent: regrouping affiliations");
                    }
                    cleanupLocked(task.userId);
                }
                trimInactiveRecentTasks();
            }
        }
    }

    boolean addToBottom(TaskRecord task) {
        if (!canAddTaskWithoutTrim(task)) {
            return false;
        }
        add(task);
        return true;
    }

    void remove(TaskRecord task) {
        this.mTasks.remove(task);
        notifyTaskRemoved(task, false);
    }

    private void trimInactiveRecentTasks() {
        int recentsCount = this.mTasks.size();
        while (recentsCount > this.mGlobalMaxNumTasks) {
            TaskRecord tr = (TaskRecord) this.mTasks.remove(recentsCount - 1);
            notifyTaskRemoved(tr, true);
            recentsCount--;
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Trimming over max-recents task=");
                stringBuilder.append(tr);
                stringBuilder.append(" max=");
                stringBuilder.append(this.mGlobalMaxNumTasks);
                Slog.d(str, stringBuilder.toString());
            }
        }
        int[] profileUserIds = this.mUserController.getCurrentProfileIds();
        this.mTmpQuietProfileUserIds.clear();
        for (int userId : profileUserIds) {
            UserInfo userInfo = this.mUserController.getUserInfo(userId);
            if (userInfo != null && userInfo.isManagedProfile() && userInfo.isQuietModeEnabled()) {
                this.mTmpQuietProfileUserIds.put(userId, true);
            }
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("User: ");
                stringBuilder2.append(userInfo);
                stringBuilder2.append(" quiet=");
                stringBuilder2.append(this.mTmpQuietProfileUserIds.get(userId));
                Slog.d(str2, stringBuilder2.toString());
            }
        }
        int i = 0;
        int i2 = 0;
        while (i2 < this.mTasks.size()) {
            TaskRecord task = (TaskRecord) this.mTasks.get(i2);
            String str3;
            StringBuilder stringBuilder3;
            if (isActiveRecentTask(task, this.mTmpQuietProfileUserIds)) {
                if (!this.mHasVisibleRecentTasks) {
                    i2++;
                } else if (isVisibleRecentTask(task)) {
                    i++;
                    if (isInVisibleRange(task, i) || !isTrimmable(task)) {
                        i2++;
                    } else if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                        str3 = ActivityManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Trimming out-of-range visible task=");
                        stringBuilder3.append(task);
                        Slog.d(str3, stringBuilder3.toString());
                    }
                } else {
                    i2++;
                }
            } else if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Trimming inactive task=");
                stringBuilder3.append(task);
                Slog.d(str3, stringBuilder3.toString());
            }
            this.mTasks.remove(task);
            notifyTaskRemoved(task, true);
            notifyTaskPersisterLocked(task, false);
        }
    }

    private boolean isActiveRecentTask(TaskRecord task, SparseBooleanArray quietProfileUserIds) {
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isActiveRecentTask: task=");
            stringBuilder.append(task);
            stringBuilder.append(" globalMax=");
            stringBuilder.append(this.mGlobalMaxNumTasks);
            Slog.d(str, stringBuilder.toString());
        }
        if (quietProfileUserIds.get(task.userId)) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(ActivityManagerService.TAG, "\tisQuietProfileTask=true");
            }
            return false;
        }
        if (!(task.mAffiliatedTaskId == -1 || task.mAffiliatedTaskId == task.taskId)) {
            TaskRecord affiliatedTask = getTask(task.mAffiliatedTaskId);
            if (!(affiliatedTask == null || isActiveRecentTask(affiliatedTask, quietProfileUserIds))) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("\taffiliatedWithTask=");
                    stringBuilder2.append(affiliatedTask);
                    stringBuilder2.append(" is not active");
                    Slog.d(str2, stringBuilder2.toString());
                }
                return false;
            }
        }
        return true;
    }

    private boolean isVisibleRecentTask(TaskRecord task) {
        String str;
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isVisibleRecentTask: task=");
            stringBuilder.append(task);
            stringBuilder.append(" minVis=");
            stringBuilder.append(this.mMinNumVisibleTasks);
            stringBuilder.append(" maxVis=");
            stringBuilder.append(this.mMaxNumVisibleTasks);
            stringBuilder.append(" sessionDuration=");
            stringBuilder.append(this.mActiveTasksSessionDurationMs);
            stringBuilder.append(" inactiveDuration=");
            stringBuilder.append(task.getInactiveDuration());
            stringBuilder.append(" activityType=");
            stringBuilder.append(task.getActivityType());
            stringBuilder.append(" windowingMode=");
            stringBuilder.append(task.getWindowingMode());
            stringBuilder.append(" intentFlags=");
            stringBuilder.append(task.getBaseIntent().getFlags());
            Slog.d(str, stringBuilder.toString());
        }
        switch (task.getActivityType()) {
            case 2:
            case 3:
                return false;
            case 4:
                if ((task.getBaseIntent().getFlags() & DumpState.DUMP_VOLUMES) == DumpState.DUMP_VOLUMES) {
                    return false;
                }
                break;
        }
        switch (task.getWindowingMode()) {
            case 2:
                return false;
            case 3:
                if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("\ttop=");
                    stringBuilder2.append(task.getStack().topTask());
                    Slog.d(str, stringBuilder2.toString());
                }
                ActivityStack stack = task.getStack();
                if (stack != null && stack.topTask() == task) {
                    return false;
                }
        }
        if (task == this.mService.getLockTaskController().getRootTask()) {
            return false;
        }
        return true;
    }

    private boolean isInVisibleRange(TaskRecord task, int numVisibleTasks) {
        boolean z = false;
        if ((task.getBaseIntent().getFlags() & DumpState.DUMP_VOLUMES) == DumpState.DUMP_VOLUMES) {
            if (!((task.getBaseIntent().getHwFlags() & 16384) == 16384)) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    Slog.d(ActivityManagerService.TAG, "\texcludeFromRecents=true");
                }
                if (numVisibleTasks == 1) {
                    z = true;
                }
                return z;
            }
        }
        if (this.mMinNumVisibleTasks >= 0 && numVisibleTasks <= this.mMinNumVisibleTasks) {
            return true;
        }
        if (this.mMaxNumVisibleTasks < 0) {
            return this.mActiveTasksSessionDurationMs > 0 && task.getInactiveDuration() <= this.mActiveTasksSessionDurationMs;
        } else {
            if (numVisibleTasks <= this.mMaxNumVisibleTasks) {
                z = true;
            }
            return z;
        }
    }

    protected boolean isTrimmable(TaskRecord task) {
        ActivityStack stack = task.getStack();
        ActivityStack homeStack = this.mService.mStackSupervisor.mHomeStack;
        boolean z = true;
        if (stack == null) {
            return true;
        }
        if (stack.getDisplay() != homeStack.getDisplay()) {
            return false;
        }
        ActivityDisplay display = stack.getDisplay();
        if (display.getIndexOf(stack) >= display.getIndexOf(homeStack)) {
            z = false;
        }
        return z;
    }

    private void removeForAddTask(TaskRecord task) {
        int removeIndex = findRemoveIndexForAddTask(task);
        if (removeIndex != -1) {
            TaskRecord removedTask = (TaskRecord) this.mTasks.remove(removeIndex);
            if (removedTask != task) {
                notifyTaskRemoved(removedTask, false);
                if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Trimming task=");
                    stringBuilder.append(removedTask);
                    stringBuilder.append(" for addition of task=");
                    stringBuilder.append(task);
                    Slog.d(str, stringBuilder.toString());
                }
            }
            notifyTaskPersisterLocked(removedTask, false);
        }
    }

    private int findRemoveIndexForAddTask(TaskRecord task) {
        TaskRecord taskRecord = task;
        int recentsCount = this.mTasks.size();
        Intent intent = taskRecord.intent;
        boolean z = true;
        boolean document = intent != null && intent.isDocument();
        int maxRecents = taskRecord.maxRecents - 1;
        int i = 0;
        while (i < recentsCount) {
            TaskRecord tr = (TaskRecord) this.mTasks.get(i);
            if (taskRecord != tr) {
                if (hasCompatibleActivityTypeAndWindowingMode(taskRecord, tr) && taskRecord.userId == tr.userId) {
                    Intent trIntent = tr.intent;
                    boolean sameAffinity = (taskRecord.affinity == null || !taskRecord.affinity.equals(tr.affinity)) ? false : z;
                    boolean sameIntent = (intent == null || !intent.filterEquals(trIntent)) ? false : z;
                    boolean multiTasksAllowed = false;
                    int flags = intent.getFlags();
                    if (!((268959744 & flags) == 0 || (134217728 & flags) == 0)) {
                        multiTasksAllowed = true;
                    }
                    boolean trIsDocument = (trIntent == null || !trIntent.isDocument()) ? false : z;
                    boolean bothDocuments = (document && trIsDocument) ? z : false;
                    if (sameAffinity || sameIntent || bothDocuments) {
                        if (bothDocuments) {
                            boolean sameActivity = (taskRecord.realActivity == null || tr.realActivity == null || !taskRecord.realActivity.equals(tr.realActivity)) ? false : true;
                            if (!sameActivity) {
                                continue;
                            } else if (maxRecents > 0) {
                                maxRecents--;
                                if (sameIntent && !multiTasksAllowed) {
                                }
                            }
                        } else if (!(document || trIsDocument)) {
                        }
                    }
                }
                i++;
                z = true;
            }
            return i;
        }
        return -1;
    }

    private int processNextAffiliateChainLocked(int start) {
        TaskRecord startTask = (TaskRecord) this.mTasks.get(start);
        int affiliateId = startTask.mAffiliatedTaskId;
        if (startTask.taskId == affiliateId && startTask.mPrevAffiliate == null && startTask.mNextAffiliate == null) {
            startTask.inRecents = true;
            return start + 1;
        }
        this.mTmpRecents.clear();
        for (int i = this.mTasks.size() - 1; i >= start; i--) {
            TaskRecord task = (TaskRecord) this.mTasks.get(i);
            if (task.mAffiliatedTaskId == affiliateId) {
                this.mTasks.remove(i);
                this.mTmpRecents.add(task);
            }
        }
        Collections.sort(this.mTmpRecents, TASK_ID_COMPARATOR);
        TaskRecord first = (TaskRecord) this.mTmpRecents.get(0);
        first.inRecents = true;
        if (first.mNextAffiliate != null) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Link error 1 first.next=");
            stringBuilder.append(first.mNextAffiliate);
            Slog.w(str, stringBuilder.toString());
            first.setNextAffiliate(null);
            notifyTaskPersisterLocked(first, false);
        }
        int tmpSize = this.mTmpRecents.size();
        for (int i2 = 0; i2 < tmpSize - 1; i2++) {
            String str2;
            StringBuilder stringBuilder2;
            TaskRecord next = (TaskRecord) this.mTmpRecents.get(i2);
            TaskRecord prev = (TaskRecord) this.mTmpRecents.get(i2 + 1);
            if (next.mPrevAffiliate != prev) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Link error 2 next=");
                stringBuilder2.append(next);
                stringBuilder2.append(" prev=");
                stringBuilder2.append(next.mPrevAffiliate);
                stringBuilder2.append(" setting prev=");
                stringBuilder2.append(prev);
                Slog.w(str2, stringBuilder2.toString());
                next.setPrevAffiliate(prev);
                notifyTaskPersisterLocked(next, false);
            }
            if (prev.mNextAffiliate != next) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Link error 3 prev=");
                stringBuilder2.append(prev);
                stringBuilder2.append(" next=");
                stringBuilder2.append(prev.mNextAffiliate);
                stringBuilder2.append(" setting next=");
                stringBuilder2.append(next);
                Slog.w(str2, stringBuilder2.toString());
                prev.setNextAffiliate(next);
                notifyTaskPersisterLocked(prev, false);
            }
            prev.inRecents = true;
        }
        TaskRecord last = (TaskRecord) this.mTmpRecents.get(tmpSize - 1);
        if (last.mPrevAffiliate != null) {
            String str3 = ActivityManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Link error 4 last.prev=");
            stringBuilder3.append(last.mPrevAffiliate);
            Slog.w(str3, stringBuilder3.toString());
            last.setPrevAffiliate(null);
            notifyTaskPersisterLocked(last, false);
        }
        this.mTasks.addAll(start, this.mTmpRecents);
        this.mTmpRecents.clear();
        return start + tmpSize;
    }

    private boolean moveAffiliatedTasksToFront(TaskRecord task, int taskIndex) {
        String str;
        StringBuilder stringBuilder;
        int recentsCount = this.mTasks.size();
        TaskRecord top = task;
        int topIndex = taskIndex;
        while (top.mNextAffiliate != null && topIndex > 0) {
            top = top.mNextAffiliate;
            topIndex--;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("addRecent: adding affilliates starting at ");
            stringBuilder2.append(topIndex);
            stringBuilder2.append(" from intial ");
            stringBuilder2.append(taskIndex);
            Slog.d(str2, stringBuilder2.toString());
        }
        boolean sane = top.mAffiliatedTaskId == task.mAffiliatedTaskId;
        int endIndex = topIndex;
        TaskRecord prev = top;
        while (endIndex < recentsCount) {
            String str3;
            StringBuilder stringBuilder3;
            TaskRecord cur = (TaskRecord) this.mTasks.get(endIndex);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("addRecent: looking at next chain @");
                stringBuilder3.append(endIndex);
                stringBuilder3.append(" ");
                stringBuilder3.append(cur);
                Slog.d(str3, stringBuilder3.toString());
            }
            if (cur != top) {
                if (!(cur.mNextAffiliate == prev && cur.mNextAffiliateTaskId == prev.taskId)) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Bad chain @");
                    stringBuilder3.append(endIndex);
                    stringBuilder3.append(": middle task ");
                    stringBuilder3.append(cur);
                    stringBuilder3.append(" @");
                    stringBuilder3.append(endIndex);
                    stringBuilder3.append(" has bad next affiliate ");
                    stringBuilder3.append(cur.mNextAffiliate);
                    stringBuilder3.append(" id ");
                    stringBuilder3.append(cur.mNextAffiliateTaskId);
                    stringBuilder3.append(", expected ");
                    stringBuilder3.append(prev);
                    Slog.wtf(str3, stringBuilder3.toString());
                    sane = false;
                    break;
                }
            } else if (!(cur.mNextAffiliate == null && cur.mNextAffiliateTaskId == -1)) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Bad chain @");
                stringBuilder3.append(endIndex);
                stringBuilder3.append(": first task has next affiliate: ");
                stringBuilder3.append(prev);
                Slog.wtf(str3, stringBuilder3.toString());
                sane = false;
                break;
            }
            if (cur.mPrevAffiliateTaskId == -1) {
                if (cur.mPrevAffiliate != null) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Bad chain @");
                    stringBuilder3.append(endIndex);
                    stringBuilder3.append(": last task ");
                    stringBuilder3.append(cur);
                    stringBuilder3.append(" has previous affiliate ");
                    stringBuilder3.append(cur.mPrevAffiliate);
                    Slog.wtf(str3, stringBuilder3.toString());
                    sane = false;
                }
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("addRecent: end of chain @");
                    stringBuilder3.append(endIndex);
                    Slog.d(str3, stringBuilder3.toString());
                }
            } else if (cur.mPrevAffiliate == null) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Bad chain @");
                stringBuilder3.append(endIndex);
                stringBuilder3.append(": task ");
                stringBuilder3.append(cur);
                stringBuilder3.append(" has previous affiliate ");
                stringBuilder3.append(cur.mPrevAffiliate);
                stringBuilder3.append(" but should be id ");
                stringBuilder3.append(cur.mPrevAffiliate);
                Slog.wtf(str3, stringBuilder3.toString());
                sane = false;
                break;
            } else if (cur.mAffiliatedTaskId != task.mAffiliatedTaskId) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Bad chain @");
                stringBuilder3.append(endIndex);
                stringBuilder3.append(": task ");
                stringBuilder3.append(cur);
                stringBuilder3.append(" has affiliated id ");
                stringBuilder3.append(cur.mAffiliatedTaskId);
                stringBuilder3.append(" but should be ");
                stringBuilder3.append(task.mAffiliatedTaskId);
                Slog.wtf(str3, stringBuilder3.toString());
                sane = false;
                break;
            } else {
                prev = cur;
                endIndex++;
                if (endIndex >= recentsCount) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Bad chain ran off index ");
                    stringBuilder3.append(endIndex);
                    stringBuilder3.append(": last task ");
                    stringBuilder3.append(prev);
                    Slog.wtf(str3, stringBuilder3.toString());
                    sane = false;
                    break;
                }
            }
        }
        if (sane && endIndex < taskIndex) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad chain @");
            stringBuilder.append(endIndex);
            stringBuilder.append(": did not extend to task ");
            stringBuilder.append(task);
            stringBuilder.append(" @");
            stringBuilder.append(taskIndex);
            Slog.wtf(str, stringBuilder.toString());
            sane = false;
        }
        if (!sane) {
            return false;
        }
        for (int i = topIndex; i <= endIndex; i++) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("addRecent: moving affiliated ");
                stringBuilder.append(task);
                stringBuilder.append(" from ");
                stringBuilder.append(i);
                stringBuilder.append(" to ");
                stringBuilder.append(i - topIndex);
                Slog.d(str, stringBuilder.toString());
            }
            this.mTasks.add(i - topIndex, (TaskRecord) this.mTasks.remove(i));
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            String str4 = ActivityManagerService.TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("addRecent: done moving tasks  ");
            stringBuilder4.append(topIndex);
            stringBuilder4.append(" to ");
            stringBuilder4.append(endIndex);
            Slog.d(str4, stringBuilder4.toString());
        }
        return true;
    }

    void dump(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        pw.println("ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mRecentsUid=");
        stringBuilder.append(this.mRecentsUid);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRecentsComponent=");
        stringBuilder.append(this.mRecentsComponent);
        pw.println(stringBuilder.toString());
        if (!this.mTasks.isEmpty()) {
            boolean printedAnything = false;
            boolean printedHeader = false;
            int size = this.mTasks.size();
            for (int i = 0; i < size; i++) {
                TaskRecord tr = (TaskRecord) this.mTasks.get(i);
                if (dumpPackage == null || (tr.realActivity != null && dumpPackage.equals(tr.realActivity.getPackageName()))) {
                    if (!printedHeader) {
                        pw.println("  Recent tasks:");
                        printedHeader = true;
                        printedAnything = true;
                    }
                    pw.print("  * Recent #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(tr);
                    if (dumpAll) {
                        tr.dump(pw, "    ");
                    }
                }
            }
            if (!printedAnything) {
                pw.println("  (nothing)");
            }
        }
    }

    RecentTaskInfo createRecentTaskInfo(TaskRecord tr) {
        RecentTaskInfo rti = new RecentTaskInfo();
        rti.id = tr.getTopActivity() == null ? -1 : tr.taskId;
        rti.persistentId = tr.taskId;
        rti.baseIntent = new Intent(tr.getBaseIntent());
        rti.origActivity = tr.origActivity;
        rti.realActivity = tr.realActivity;
        rti.description = tr.lastDescription;
        rti.stackId = tr.getStackId();
        rti.userId = tr.userId;
        rti.taskDescription = new TaskDescription(tr.lastTaskDescription);
        rti.lastActiveTime = tr.lastActiveTime;
        rti.affiliatedTaskId = tr.mAffiliatedTaskId;
        rti.affiliatedTaskColor = tr.mAffiliatedTaskColor;
        rti.numActivities = 0;
        if (!tr.matchParentBounds()) {
            rti.bounds = new Rect(tr.getOverrideBounds());
        }
        rti.supportsSplitScreenMultiWindow = tr.supportsSplitScreenWindowingMode();
        rti.resizeMode = tr.mResizeMode;
        rti.configuration.setTo(tr.getConfiguration());
        tr.getNumRunningActivities(this.mTmpReport);
        rti.numActivities = this.mTmpReport.numActivities;
        ComponentName componentName = null;
        rti.baseActivity = this.mTmpReport.base != null ? this.mTmpReport.base.intent.getComponent() : null;
        if (this.mTmpReport.top != null) {
            componentName = this.mTmpReport.top.intent.getComponent();
        }
        rti.topActivity = componentName;
        return rti;
    }

    private boolean hasCompatibleActivityTypeAndWindowingMode(TaskRecord t1, TaskRecord t2) {
        int activityType = t1.getActivityType();
        int windowingMode = t1.getWindowingMode();
        boolean isUndefinedType = activityType == 0;
        boolean isUndefinedMode = windowingMode == 0;
        int otherActivityType = t2.getActivityType();
        int otherWindowingMode = t2.getWindowingMode();
        boolean isOtherUndefinedType = otherActivityType == 0;
        boolean isOtherUndefinedMode = otherWindowingMode == 0;
        boolean isCompatibleType = activityType == otherActivityType || isUndefinedType || isOtherUndefinedType;
        boolean isCompatibleMode = windowingMode == otherWindowingMode || isUndefinedMode || isOtherUndefinedMode;
        if (isCompatibleType && isCompatibleMode) {
            return true;
        }
        return false;
    }
}
