package com.android.systemui.shared.recents.model;

import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task.TaskKey;

public abstract class TaskKeyCache<V> {
    protected static final String TAG = "TaskKeyCache";
    protected final SparseArray<TaskKey> mKeys = new SparseArray();

    protected abstract void evictAllCache();

    protected abstract V getCacheEntry(int i);

    protected abstract void putCacheEntry(int i, V v);

    protected abstract void removeCacheEntry(int i);

    final V get(TaskKey key) {
        return getCacheEntry(key.id);
    }

    final V getAndInvalidateIfModified(TaskKey key) {
        TaskKey lastKey;
        synchronized (this.mKeys) {
            lastKey = (TaskKey) this.mKeys.get(key.id);
        }
        if (lastKey == null || (lastKey.windowingMode == key.windowingMode && lastKey.lastActiveTime == key.lastActiveTime)) {
            return getCacheEntry(key.id);
        }
        remove(key);
        return null;
    }

    final void put(TaskKey key, V value) {
        if (key == null) {
            Log.e(TAG, "Unexpected key == null");
            return;
        }
        synchronized (this.mKeys) {
            this.mKeys.put(key.id, key);
        }
        putCacheEntry(key.id, value);
    }

    final void remove(TaskKey key) {
        removeCacheEntry(key.id);
        synchronized (this.mKeys) {
            this.mKeys.remove(key.id);
        }
    }

    final void evictAll() {
        evictAllCache();
        synchronized (this.mKeys) {
            this.mKeys.clear();
        }
    }
}
