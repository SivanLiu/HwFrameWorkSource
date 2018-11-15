package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import java.io.PrintWriter;

public class TaskKeyStrongCache<V> extends TaskKeyCache<V> {
    private static final String TAG = "TaskKeyCache";
    private final ArrayMap<Integer, V> mCache = new ArrayMap();

    final void copyEntries(TaskKeyStrongCache<V> other) {
        synchronized (other.mKeys) {
            for (int i = other.mKeys.size() - 1; i >= 0; i--) {
                TaskKey key = (TaskKey) other.mKeys.valueAt(i);
                if (key != null) {
                    put(key, other.getCacheEntry(key.id));
                }
            }
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("  ");
        innerPrefix = innerPrefix.toString();
        writer.print(prefix);
        writer.print(TAG);
        writer.print(" numEntries=");
        writer.print(this.mKeys.size());
        writer.println();
        int keyCount = this.mKeys.size();
        for (int i = 0; i < keyCount; i++) {
            writer.print(innerPrefix);
            writer.println(this.mKeys.get(this.mKeys.keyAt(i)));
        }
    }

    protected V getCacheEntry(int id) {
        V v;
        synchronized (this.mCache) {
            v = this.mCache.get(Integer.valueOf(id));
        }
        return v;
    }

    protected void putCacheEntry(int id, V value) {
        synchronized (this.mCache) {
            this.mCache.put(Integer.valueOf(id), value);
        }
    }

    protected void removeCacheEntry(int id) {
        synchronized (this.mCache) {
            this.mCache.remove(Integer.valueOf(id));
        }
    }

    protected void evictAllCache() {
        synchronized (this.mCache) {
            this.mCache.clear();
        }
    }
}
