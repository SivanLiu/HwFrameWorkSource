package com.android.server.wm;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class HwMagicWinCombineManager {
    private static final int DEFAULT_CAPACITY = 2;
    private static final String INVALID_KEY = "INVALID_KEY";
    private static final String TAG = "HwMagicWinCombineManager";
    private static volatile HwMagicWinCombineManager sInstance = new HwMagicWinCombineManager();
    private final ConcurrentHashMap<String, CombineInfo> mCombineMap = new ConcurrentHashMap<>(2);
    private final Object mLock = new Object();

    private HwMagicWinCombineManager() {
    }

    public static HwMagicWinCombineManager getInstance() {
        return sInstance;
    }

    private TaskRecord getRecentsTaskWithStack(ActivityStack stack) {
        RecentTasks recentTasks = stack.mService.mRecentTasks;
        int recentsCount = recentTasks.getRawTasks().size();
        for (int i = 0; i < recentsCount; i++) {
            TaskRecord tr = (TaskRecord) recentTasks.getRawTasks().get(i);
            if (tr.getStackId() == stack.getStackId()) {
                return tr;
            }
        }
        return null;
    }

    private TaskRecord getRecentsRemoveTask(TaskRecord primaryTask, TaskRecord secondaryTask) {
        RecentTasks recentTasks = primaryTask.mService.mRecentTasks;
        int recentsCount = recentTasks.getRawTasks().size();
        for (int i = 0; i < recentsCount; i++) {
            TaskRecord tr = (TaskRecord) recentTasks.getRawTasks().get(i);
            if (primaryTask != tr && secondaryTask != tr && primaryTask.userId == tr.userId && secondaryTask.userId == tr.userId && ((primaryTask.affinity != null && primaryTask.affinity.equals(tr.affinity)) || (secondaryTask.affinity != null && secondaryTask.affinity.equals(tr.affinity)))) {
                return tr;
            }
        }
        return null;
    }

    private void removeTaskFromRecentsIfNeeded(ActivityStack primaryStack, ActivityStack secondaryStack) {
        if (primaryStack != null && secondaryStack != null && primaryStack.topTask() != null && secondaryStack.topTask() != null) {
            TaskRecord primaryTask = primaryStack.topTask();
            TaskRecord secondaryTask = secondaryStack.topTask();
            if (primaryTask.affinity == null || !primaryTask.affinity.equals(secondaryTask.affinity)) {
                TaskRecord removingTask = getRecentsRemoveTask(primaryTask, secondaryTask);
                if (removingTask != null) {
                    primaryStack.mService.mRecentTasks.remove(removingTask);
                    return;
                }
                return;
            }
            secondaryTask.mService.mRecentTasks.remove(secondaryTask);
            secondaryTask.mService.mRecentTasks.add(secondaryTask);
        }
    }

    private void addTaskToRecentsIfNeeded(ActivityStack primaryStack, ActivityStack secondaryStack) {
        if (primaryStack != null && secondaryStack != null && primaryStack.topTask() != null && secondaryStack.topTask() != null) {
            TaskRecord primaryTask = primaryStack.topTask();
            TaskRecord secondaryTask = secondaryStack.topTask();
            if (!primaryTask.inRecents) {
                primaryTask.mService.mRecentTasks.add(primaryTask);
            }
            if (!secondaryTask.inRecents) {
                secondaryTask.mService.mRecentTasks.add(secondaryTask);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0010  */
    private String getCombinePkgName(ActivityStack stack) {
        for (String key : this.mCombineMap.keySet()) {
            CombineInfo info = this.mCombineMap.get(key);
            if (info.mPrimaryStack.get() == stack || info.mSecondaryStack.get() == stack) {
                return key;
            }
            while (r1.hasNext()) {
            }
        }
        return null;
    }

    private void removeStack(final ActivityTaskManagerService service, final Set<Integer> stackIds) {
        service.mH.post(new Runnable() {
            /* class com.android.server.wm.HwMagicWinCombineManager.AnonymousClass1 */

            public void run() {
                for (Integer num : stackIds) {
                    service.removeStack(num.intValue());
                }
            }
        });
    }

    private String getUserPkgName(String pkgName, int userId) {
        if (pkgName == null) {
            return INVALID_KEY;
        }
        return pkgName + String.valueOf(userId);
    }

    public void removeStackReferenceIfNeeded(ActivityStack stack) {
        CombineInfo combineInfo;
        String userPkgName = getCombinePkgName(stack);
        if (userPkgName != null && (combineInfo = this.mCombineMap.get(userPkgName)) != null && !combineInfo.mSplitScreenStackIds.isEmpty()) {
            if (combineInfo.mPrimaryStack.get() == stack) {
                WeakReference unused = combineInfo.mPrimaryStack = new WeakReference(null);
            } else if (combineInfo.mSecondaryStack.get() == stack) {
                WeakReference unused2 = combineInfo.mSecondaryStack = new WeakReference(null);
            } else {
                return;
            }
            synchronized (this.mLock) {
                combineInfo.mSplitScreenStackIds.remove(Integer.valueOf(stack.getStackId()));
            }
            if (combineInfo.mPrimaryStack.get() == null && combineInfo.mSecondaryStack.get() == null) {
                Set<Integer> stackIds = new HashSet<>(combineInfo.mSplitScreenStackIds);
                this.mCombineMap.remove(userPkgName);
                removeStack(stack.mService, stackIds);
                return;
            }
            TaskRecord taskRecord = getRecentsTaskWithStack(stack);
            if (taskRecord != null) {
                stack.mService.mRecentTasks.remove(taskRecord);
            }
            if (combineInfo.mSplitScreenStackIds.size() < 2) {
                this.mCombineMap.remove(userPkgName);
            }
        }
    }

    public String getTaskPackageName(TaskRecord taskRecord) {
        if (taskRecord == null) {
            return null;
        }
        if (taskRecord.origActivity != null) {
            return taskRecord.origActivity.getPackageName();
        }
        if (taskRecord.realActivity != null) {
            return taskRecord.realActivity.getPackageName();
        }
        if (taskRecord.getTopActivity() != null) {
            return taskRecord.getTopActivity().packageName;
        }
        return null;
    }

    public boolean isForegroundTaskIds(int[] foregroundTaskIds, int taskId) {
        if (foregroundTaskIds == null) {
            return false;
        }
        if (foregroundTaskIds[0] == taskId || foregroundTaskIds[1] == taskId) {
            return true;
        }
        return false;
    }

    public int[] getForegroundTaskIds(String packageName, int userId) {
        CombineInfo combineInfo = this.mCombineMap.get(getUserPkgName(packageName, userId));
        if (combineInfo == null || combineInfo.mPrimaryStack.get() == null || combineInfo.mSecondaryStack.get() == null || ((ActivityStack) combineInfo.mPrimaryStack.get()).topTask() == null || ((ActivityStack) combineInfo.mSecondaryStack.get()).topTask() == null) {
            return null;
        }
        return new int[]{((ActivityStack) combineInfo.mPrimaryStack.get()).topTask().taskId, ((ActivityStack) combineInfo.mSecondaryStack.get()).topTask().taskId};
    }

    public Set<Integer> getSplitScreenStackIds(String packageName, int userId) {
        CombineInfo combineInfo = this.mCombineMap.get(getUserPkgName(packageName, userId));
        if (combineInfo == null || combineInfo.mSplitScreenStackIds.isEmpty() || getForegroundTaskIds(packageName, userId) == null) {
            return null;
        }
        return combineInfo.mSplitScreenStackIds;
    }

    public void updateForegroundTaskIds(String pkgName, int userId, HwMagicWinSplitManager splitManager) {
        CombineInfo combineInfo = this.mCombineMap.get(getUserPkgName(pkgName, userId));
        if (combineInfo != null) {
            ActivityStack primaryStack = splitManager.getMwStackByPosition(1, 0, pkgName, true, userId);
            ActivityStack secondaryStack = splitManager.getMwStackByPosition(2, 0, pkgName, true, userId);
            if (primaryStack != null && secondaryStack != null) {
                WeakReference unused = combineInfo.mPrimaryStack = new WeakReference(primaryStack);
                WeakReference unused2 = combineInfo.mSecondaryStack = new WeakReference(secondaryStack);
                addTaskToRecentsIfNeeded(primaryStack, secondaryStack);
            }
        }
    }

    public void addStackToSplitScreenList(ActivityStack stack, int position, String pkgName, int userId) {
        String userPkgName = getUserPkgName(pkgName, userId);
        CombineInfo combineInfo = this.mCombineMap.get(userPkgName);
        if (combineInfo == null) {
            combineInfo = new CombineInfo();
            this.mCombineMap.put(userPkgName, combineInfo);
        }
        synchronized (this.mLock) {
            combineInfo.mSplitScreenStackIds.add(Integer.valueOf(stack.getStackId()));
        }
        if (position == 1) {
            WeakReference unused = combineInfo.mPrimaryStack = new WeakReference(stack);
        } else {
            WeakReference unused2 = combineInfo.mSecondaryStack = new WeakReference(stack);
        }
        addTaskToRecentsIfNeeded((ActivityStack) combineInfo.mPrimaryStack.get(), (ActivityStack) combineInfo.mSecondaryStack.get());
    }

    public void clearSplitScreenList(String pkgName, int userId) {
        String userPkgName = getUserPkgName(pkgName, userId);
        CombineInfo combineInfo = this.mCombineMap.get(userPkgName);
        if (combineInfo != null) {
            this.mCombineMap.remove(userPkgName);
            removeTaskFromRecentsIfNeeded((ActivityStack) combineInfo.mPrimaryStack.get(), (ActivityStack) combineInfo.mSecondaryStack.get());
        }
    }

    public void removeStackFromSplitScreenList(ActivityStack stack, String pkgName, HwMagicWinSplitManager splitManager, int userId) {
        CombineInfo combineInfo = this.mCombineMap.get(getUserPkgName(pkgName, userId));
        if (combineInfo != null && !combineInfo.mSplitScreenStackIds.isEmpty()) {
            synchronized (this.mLock) {
                combineInfo.mSplitScreenStackIds.remove(Integer.valueOf(stack.getStackId()));
            }
            updateForegroundTaskIds(pkgName, userId, splitManager);
        }
    }

    class CombineInfo {
        /* access modifiers changed from: private */
        public WeakReference<ActivityStack> mPrimaryStack = new WeakReference<>(null);
        /* access modifiers changed from: private */
        public WeakReference<ActivityStack> mSecondaryStack = new WeakReference<>(null);
        /* access modifiers changed from: private */
        public final Set<Integer> mSplitScreenStackIds = new HashSet();

        CombineInfo() {
        }
    }
}
