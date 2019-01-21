package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.util.ArrayMap;
import java.io.PrintWriter;

class TaskSnapshotCache {
    private final ArrayMap<AppWindowToken, Integer> mAppTaskMap = new ArrayMap();
    private TaskSnapshot mLastForegroundSnapshot;
    private final TaskSnapshotLoader mLoader;
    private final ArrayMap<Integer, CacheEntry> mRunningCache = new ArrayMap();
    private final WindowManagerService mService;

    private static final class CacheEntry {
        final TaskSnapshot snapshot;
        final AppWindowToken topApp;

        CacheEntry(TaskSnapshot snapshot, AppWindowToken topApp) {
            this.snapshot = snapshot;
            this.topApp = topApp;
        }
    }

    TaskSnapshotCache(WindowManagerService service, TaskSnapshotLoader loader) {
        this.mService = service;
        this.mLoader = loader;
    }

    void putForegroundSnapShot(Task task, TaskSnapshot snapshot) {
        this.mLastForegroundSnapshot = snapshot;
    }

    void clearForegroundTaskSnapshot() {
        this.mLastForegroundSnapshot = null;
    }

    TaskSnapshot getLastForegroundSnapshot() {
        return this.mLastForegroundSnapshot;
    }

    void putSnapshot(Task task, TaskSnapshot snapshot) {
        CacheEntry entry = (CacheEntry) this.mRunningCache.get(Integer.valueOf(task.mTaskId));
        if (entry != null) {
            this.mAppTaskMap.remove(entry.topApp);
        }
        this.mAppTaskMap.put((AppWindowToken) task.getTopChild(), Integer.valueOf(task.mTaskId));
        this.mRunningCache.put(Integer.valueOf(task.mTaskId), new CacheEntry(snapshot, (AppWindowToken) task.getTopChild()));
    }

    /* JADX WARNING: Missing block: B:14:0x0032, code skipped:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:15:0x0035, code skipped:
            if (r6 != false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:17:0x0038, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:19:0x003d, code skipped:
            return tryRestoreFromDisk(r4, r5, r7);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk, boolean reducedResolution) {
        TaskSnapshot valueOf;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ArrayMap arrayMap = this.mRunningCache;
                valueOf = Integer.valueOf(taskId);
                CacheEntry entry = (CacheEntry) arrayMap.get(valueOf);
                if (entry != null) {
                    valueOf = entry.snapshot.getSnapshot();
                    if (valueOf != null) {
                        valueOf = entry.snapshot.getSnapshot().isDestroyed();
                        if (valueOf == null) {
                            valueOf = entry.snapshot;
                        }
                    }
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return valueOf;
    }

    private TaskSnapshot tryRestoreFromDisk(int taskId, int userId, boolean reducedResolution) {
        TaskSnapshot snapshot = this.mLoader.loadTask(taskId, userId, reducedResolution);
        if (snapshot == null) {
            return null;
        }
        return snapshot;
    }

    void onAppRemoved(AppWindowToken wtoken) {
        Integer taskId = (Integer) this.mAppTaskMap.get(wtoken);
        if (taskId != null) {
            removeRunningEntry(taskId.intValue());
        }
    }

    void onAppDied(AppWindowToken wtoken) {
        Integer taskId = (Integer) this.mAppTaskMap.get(wtoken);
        if (taskId != null) {
            removeRunningEntry(taskId.intValue());
        }
    }

    void onTaskRemoved(int taskId) {
        removeRunningEntry(taskId);
    }

    private void removeRunningEntry(int taskId) {
        CacheEntry entry = (CacheEntry) this.mRunningCache.get(Integer.valueOf(taskId));
        if (entry != null) {
            this.mAppTaskMap.remove(entry.topApp);
            this.mRunningCache.remove(Integer.valueOf(taskId));
        }
    }

    void dump(PrintWriter pw, String prefix) {
        String doublePrefix = new StringBuilder();
        doublePrefix.append(prefix);
        doublePrefix.append("  ");
        doublePrefix = doublePrefix.toString();
        String triplePrefix = new StringBuilder();
        triplePrefix.append(doublePrefix);
        triplePrefix.append("  ");
        triplePrefix = triplePrefix.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("SnapshotCache");
        pw.println(stringBuilder.toString());
        for (int i = this.mRunningCache.size() - 1; i >= 0; i--) {
            CacheEntry entry = (CacheEntry) this.mRunningCache.valueAt(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(doublePrefix);
            stringBuilder2.append("Entry taskId=");
            stringBuilder2.append(this.mRunningCache.keyAt(i));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(triplePrefix);
            stringBuilder2.append("topApp=");
            stringBuilder2.append(entry.topApp);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(triplePrefix);
            stringBuilder2.append("snapshot=");
            stringBuilder2.append(entry.snapshot);
            pw.println(stringBuilder2.toString());
        }
    }
}
