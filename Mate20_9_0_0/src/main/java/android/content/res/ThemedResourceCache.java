package android.content.res;

import android.content.res.Resources.Theme;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import java.lang.ref.WeakReference;

abstract class ThemedResourceCache<T> {
    private LongSparseArray<WeakReference<T>> mNullThemedEntries;
    private ArrayMap<ThemeKey, LongSparseArray<WeakReference<T>>> mThemedEntries;
    private LongSparseArray<WeakReference<T>> mUnthemedEntries;

    protected abstract boolean shouldInvalidateEntry(T t, int i);

    ThemedResourceCache() {
    }

    public void put(long key, Theme theme, T entry) {
        put(key, theme, entry, true);
    }

    public void put(long key, Theme theme, T entry, boolean usesTheme) {
        if (entry != null) {
            synchronized (this) {
                LongSparseArray<WeakReference<T>> entries;
                if (usesTheme) {
                    entries = getThemedLocked(theme, true);
                } else {
                    try {
                        entries = getUnthemedLocked(true);
                    } catch (Throwable th) {
                    }
                }
                if (entries != null) {
                    entries.put(key, new WeakReference(entry));
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x002c, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public T get(long key, Theme theme) {
        synchronized (this) {
            WeakReference<T> themedEntry;
            LongSparseArray<WeakReference<T>> themedEntries = getThemedLocked(theme, false);
            if (themedEntries != null) {
                themedEntry = (WeakReference) themedEntries.get(key);
                if (themedEntry != null) {
                    Object obj = themedEntry.get();
                    return obj;
                }
            }
            LongSparseArray<WeakReference<T>> unthemedEntries = getUnthemedLocked(false);
            if (unthemedEntries != null) {
                themedEntry = (WeakReference) unthemedEntries.get(key);
                if (themedEntry != null) {
                    Object obj2 = themedEntry.get();
                    return obj2;
                }
            }
        }
    }

    public void onConfigurationChange(int configChanges) {
        prune(configChanges);
    }

    private LongSparseArray<WeakReference<T>> getThemedLocked(Theme t, boolean create) {
        if (t == null) {
            if (this.mNullThemedEntries == null && create) {
                this.mNullThemedEntries = new LongSparseArray(1);
            }
            return this.mNullThemedEntries;
        }
        if (this.mThemedEntries == null) {
            if (!create) {
                return null;
            }
            this.mThemedEntries = new ArrayMap(1);
        }
        ThemeKey key = t.getKey();
        LongSparseArray<WeakReference<T>> cache = (LongSparseArray) this.mThemedEntries.get(key);
        if (cache == null && create) {
            cache = new LongSparseArray(1);
            this.mThemedEntries.put(key.clone(), cache);
        }
        return cache;
    }

    private LongSparseArray<WeakReference<T>> getUnthemedLocked(boolean create) {
        if (this.mUnthemedEntries == null && create) {
            this.mUnthemedEntries = new LongSparseArray(1);
        }
        return this.mUnthemedEntries;
    }

    private boolean prune(int configChanges) {
        boolean z;
        synchronized (this) {
            z = true;
            if (this.mThemedEntries != null) {
                for (int i = this.mThemedEntries.size() - 1; i >= 0; i--) {
                    if (pruneEntriesLocked((LongSparseArray) this.mThemedEntries.valueAt(i), configChanges)) {
                        this.mThemedEntries.removeAt(i);
                    }
                }
            }
            pruneEntriesLocked(this.mNullThemedEntries, configChanges);
            pruneEntriesLocked(this.mUnthemedEntries, configChanges);
            if (this.mThemedEntries != null || this.mNullThemedEntries != null || this.mUnthemedEntries != null) {
                z = false;
            }
        }
        return z;
    }

    private boolean pruneEntriesLocked(LongSparseArray<WeakReference<T>> entries, int configChanges) {
        boolean z = true;
        if (entries == null) {
            return true;
        }
        for (int i = entries.size() - 1; i >= 0; i--) {
            WeakReference<T> ref = (WeakReference) entries.valueAt(i);
            if (ref == null || pruneEntryLocked(ref.get(), configChanges)) {
                entries.removeAt(i);
            }
        }
        if (entries.size() != 0) {
            z = false;
        }
        return z;
    }

    private boolean pruneEntryLocked(T entry, int configChanges) {
        return entry == null || (configChanges != 0 && shouldInvalidateEntry(entry, configChanges));
    }
}
