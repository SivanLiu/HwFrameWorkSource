package android.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.radio.V1_0.RadioError;
import android.os.FileUtils;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.system.StructTimespec;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ExponentiallyBucketedHistogram;
import com.android.internal.util.XmlUtils;
import dalvik.system.BlockGuard;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import org.xmlpull.v1.XmlPullParserException;

final class SharedPreferencesImpl implements SharedPreferences {
    private static final Object CONTENT = new Object();
    private static final boolean DEBUG = false;
    private static final long MAX_FSYNC_DURATION_MILLIS = 256;
    private static final String TAG = "SharedPreferencesImpl";
    private final File mBackupFile;
    @GuardedBy("this")
    private long mCurrentMemoryStateGeneration;
    @GuardedBy("mWritingToDiskLock")
    private long mDiskStateGeneration;
    @GuardedBy("mLock")
    private int mDiskWritesInFlight = 0;
    private final File mFile;
    @GuardedBy("mLock")
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap();
    @GuardedBy("mLock")
    private boolean mLoaded = false;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Map<String, Object> mMap;
    private int mMode;
    private int mNumSync = 0;
    @GuardedBy("mLock")
    private long mStatSize;
    @GuardedBy("mLock")
    private StructTimespec mStatTimestamp;
    @GuardedBy("mWritingToDiskLock")
    private final ExponentiallyBucketedHistogram mSyncTimes = new ExponentiallyBucketedHistogram(16);
    @GuardedBy("mLock")
    private Throwable mThrowable;
    private final Object mWritingToDiskLock = new Object();

    private static class MemoryCommitResult {
        final List<String> keysModified;
        final Set<OnSharedPreferenceChangeListener> listeners;
        final Map<String, Object> mapToWriteToDisk;
        final long memoryStateGeneration;
        boolean wasWritten;
        @GuardedBy("mWritingToDiskLock")
        volatile boolean writeToDiskResult;
        final CountDownLatch writtenToDiskLatch;

        /* synthetic */ MemoryCommitResult(long x0, List x1, Set x2, Map x3, AnonymousClass1 x4) {
            this(x0, x1, x2, x3);
        }

        private MemoryCommitResult(long memoryStateGeneration, List<String> keysModified, Set<OnSharedPreferenceChangeListener> listeners, Map<String, Object> mapToWriteToDisk) {
            this.writtenToDiskLatch = new CountDownLatch(1);
            this.writeToDiskResult = false;
            this.wasWritten = false;
            this.memoryStateGeneration = memoryStateGeneration;
            this.keysModified = keysModified;
            this.listeners = listeners;
            this.mapToWriteToDisk = mapToWriteToDisk;
        }

        void setDiskWriteResult(boolean wasWritten, boolean result) {
            this.wasWritten = wasWritten;
            this.writeToDiskResult = result;
            this.writtenToDiskLatch.countDown();
        }
    }

    public final class EditorImpl implements Editor {
        @GuardedBy("mEditorLock")
        private boolean mClear = false;
        private final Object mEditorLock = new Object();
        @GuardedBy("mEditorLock")
        private final Map<String, Object> mModified = new HashMap();

        public Editor putString(String key, String value) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, value);
            }
            return this;
        }

        public Editor putStringSet(String key, Set<String> values) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, values == null ? null : new HashSet(values));
            }
            return this;
        }

        public Editor putInt(String key, int value) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, Integer.valueOf(value));
            }
            return this;
        }

        public Editor putLong(String key, long value) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, Long.valueOf(value));
            }
            return this;
        }

        public Editor putFloat(String key, float value) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, Float.valueOf(value));
            }
            return this;
        }

        public Editor putBoolean(String key, boolean value) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, Boolean.valueOf(value));
            }
            return this;
        }

        public Editor remove(String key) {
            synchronized (this.mEditorLock) {
                this.mModified.put(key, this);
            }
            return this;
        }

        public Editor clear() {
            synchronized (this.mEditorLock) {
                this.mClear = true;
            }
            return this;
        }

        public void apply() {
            final long startTime = System.currentTimeMillis();
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                public void run() {
                    try {
                        mcr.writtenToDiskLatch.await();
                    } catch (InterruptedException e) {
                    }
                }
            };
            QueuedWork.addFinisher(awaitCommit);
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, new Runnable() {
                public void run() {
                    awaitCommit.run();
                    QueuedWork.removeFinisher(awaitCommit);
                }
            });
            notifyListeners(mcr);
        }

        /* JADX WARNING: Removed duplicated region for block: B:43:0x00b2  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private MemoryCommitResult commitToMemory() {
            Map<String, Object> mapToWriteToDisk;
            long memoryStateGeneration;
            List<String> keysModified = null;
            Set<OnSharedPreferenceChangeListener> listeners = null;
            synchronized (SharedPreferencesImpl.this.mLock) {
                if (SharedPreferencesImpl.this.mDiskWritesInFlight > 0) {
                    SharedPreferencesImpl.this.mMap = new HashMap(SharedPreferencesImpl.this.mMap);
                }
                mapToWriteToDisk = SharedPreferencesImpl.this.mMap;
                SharedPreferencesImpl.this.mDiskWritesInFlight = SharedPreferencesImpl.this.mDiskWritesInFlight + 1;
                boolean hasListeners = SharedPreferencesImpl.this.mListeners.size() > 0;
                if (hasListeners) {
                    keysModified = new ArrayList();
                    listeners = new HashSet(SharedPreferencesImpl.this.mListeners.keySet());
                }
                synchronized (this.mEditorLock) {
                    boolean changesMade = false;
                    if (this.mClear) {
                        if (!mapToWriteToDisk.isEmpty()) {
                            changesMade = true;
                            mapToWriteToDisk.clear();
                        }
                        this.mClear = false;
                    }
                    for (Entry<String, Object> e : this.mModified.entrySet()) {
                        String k = (String) e.getKey();
                        EditorImpl v = e.getValue();
                        if (v != this) {
                            if (v != null) {
                                if (mapToWriteToDisk.containsKey(k)) {
                                    Object existingValue = mapToWriteToDisk.get(k);
                                    if (existingValue != null && existingValue.equals(v)) {
                                    }
                                }
                                mapToWriteToDisk.put(k, v);
                                changesMade = true;
                                if (hasListeners) {
                                    keysModified.add(k);
                                }
                            }
                        }
                        if (mapToWriteToDisk.containsKey(k)) {
                            mapToWriteToDisk.remove(k);
                            changesMade = true;
                            if (hasListeners) {
                            }
                        }
                    }
                    this.mModified.clear();
                    if (changesMade) {
                        SharedPreferencesImpl.this.mCurrentMemoryStateGeneration = 1 + SharedPreferencesImpl.this.mCurrentMemoryStateGeneration;
                    }
                    memoryStateGeneration = SharedPreferencesImpl.this.mCurrentMemoryStateGeneration;
                }
            }
            return new MemoryCommitResult(memoryStateGeneration, keysModified, listeners, mapToWriteToDisk, null);
        }

        public boolean commit() {
            MemoryCommitResult mcr = commitToMemory();
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, null);
            try {
                mcr.writtenToDiskLatch.await();
                notifyListeners(mcr);
                return mcr.writeToDiskResult;
            } catch (InterruptedException e) {
                return false;
            }
        }

        private void notifyListeners(MemoryCommitResult mcr) {
            if (mcr.listeners != null && mcr.keysModified != null && mcr.keysModified.size() != 0) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                        String key = (String) mcr.keysModified.get(i);
                        for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                            if (listener != null) {
                                listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                            }
                        }
                    }
                } else {
                    ActivityThread.sMainThreadHandler.post(new -$$Lambda$SharedPreferencesImpl$EditorImpl$3CAjkhzA131V3V-sLfP2uy0FWZ0(this, mcr));
                }
            }
        }
    }

    SharedPreferencesImpl(File file, int mode) {
        this.mFile = file;
        this.mBackupFile = makeBackupFile(file);
        this.mMode = mode;
        this.mLoaded = false;
        this.mMap = null;
        this.mThrowable = null;
        startLoadFromDisk();
    }

    public void setMode(int mode) {
        this.mMode = mode;
    }

    public void awaitLoaded() {
        synchronized (this) {
            while (!this.mLoaded) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void startLoadFromDisk() {
        synchronized (this.mLock) {
            this.mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            public void run() {
                SharedPreferencesImpl.this.loadFromDisk();
            }
        }.start();
    }

    /* JADX WARNING: Missing block: B:12:0x0024, code skipped:
            if (r8.mFile.exists() == false) goto L_0x004b;
     */
    /* JADX WARNING: Missing block: B:14:0x002c, code skipped:
            if (r8.mFile.canRead() != false) goto L_0x004b;
     */
    /* JADX WARNING: Missing block: B:15:0x002e, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("Attempt to read preferences file ");
            r1.append(r8.mFile);
            r1.append(" without permission");
            android.util.Log.w(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:16:0x004b, code skipped:
            r0 = null;
            r1 = null;
            r2 = null;
            r3 = null;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            r1 = android.system.Os.stat(r8.mFile.getPath());
     */
    /* JADX WARNING: Missing block: B:19:0x0060, code skipped:
            if (r8.mFile.canRead() == false) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r2 = new java.io.BufferedInputStream(new java.io.FileInputStream(r8.mFile), 16384);
     */
    /* JADX WARNING: Missing block: B:22:0x0076, code skipped:
            r0 = com.android.internal.util.XmlUtils.readMapXml(r2);
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            libcore.io.IoUtils.closeQuietly(r2);
     */
    /* JADX WARNING: Missing block: B:26:0x007d, code skipped:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:28:?, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("Cannot read ");
            r6.append(r8.mFile.getAbsolutePath());
            android.util.Log.w(r5, r6.toString(), r4);
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            libcore.io.IoUtils.closeQuietly(r2);
     */
    /* JADX WARNING: Missing block: B:31:0x009e, code skipped:
            libcore.io.IoUtils.closeQuietly(r2);
     */
    /* JADX WARNING: Missing block: B:33:0x00a2, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:0x00a3, code skipped:
            r3 = r2;
     */
    /* JADX WARNING: Missing block: B:36:0x00a7, code skipped:
            r2 = r1;
            r1 = r0;
     */
    /* JADX WARNING: Missing block: B:37:0x00ab, code skipped:
            monitor-enter(r8.mLock);
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            r8.mLoaded = true;
            r8.mThrowable = r3;
     */
    /* JADX WARNING: Missing block: B:41:0x00b1, code skipped:
            if (r3 == null) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:42:0x00b3, code skipped:
            if (r1 != null) goto L_0x00b5;
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            r8.mMap = r1;
            r8.mStatTimestamp = r2.st_mtim;
            r8.mStatSize = r2.st_size;
     */
    /* JADX WARNING: Missing block: B:47:0x00c2, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:49:0x00c4, code skipped:
            r8.mMap = new java.util.HashMap();
     */
    /* JADX WARNING: Missing block: B:51:?, code skipped:
            r8.mThrowable = r0;
     */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            r0 = r8.mLock;
     */
    /* JADX WARNING: Missing block: B:54:0x00d2, code skipped:
            r8.mLock.notifyAll();
     */
    /* JADX WARNING: Missing block: B:56:0x00d8, code skipped:
            r0 = r8.mLock;
     */
    /* JADX WARNING: Missing block: B:57:0x00da, code skipped:
            r0.notifyAll();
     */
    /* JADX WARNING: Missing block: B:59:0x00df, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadFromDisk() {
        synchronized (this.mLock) {
            if (this.mLoaded) {
            } else if (this.mBackupFile.exists()) {
                this.mFile.delete();
                this.mBackupFile.renameTo(this.mFile);
            }
        }
    }

    static File makeBackupFile(File prefsFile) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefsFile.getPath());
        stringBuilder.append(".bak");
        return new File(stringBuilder.toString());
    }

    void startReloadIfChangedUnexpectedly() {
        synchronized (this.mLock) {
            if (hasFileChangedUnexpectedly()) {
                startLoadFromDisk();
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000b, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:10:?, code skipped:
            dalvik.system.BlockGuard.getThreadPolicy().onReadFromDisk();
            r1 = android.system.Os.stat(r8.mFile.getPath());
     */
    /* JADX WARNING: Missing block: B:11:0x001d, code skipped:
            r3 = r8.mLock;
     */
    /* JADX WARNING: Missing block: B:12:0x0021, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x002a, code skipped:
            if (r1.st_mtim.equals(r8.mStatTimestamp) == false) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:17:0x0032, code skipped:
            if (r8.mStatSize == r1.st_size) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:19:0x0035, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:20:0x0037, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:21:0x0038, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:26:0x003d, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasFileChangedUnexpectedly() {
        synchronized (this.mLock) {
            if (this.mDiskWritesInFlight > 0) {
                return false;
            }
        }
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (this.mLock) {
            this.mListeners.put(listener, CONTENT);
        }
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (this.mLock) {
            this.mListeners.remove(listener);
        }
    }

    @GuardedBy("mLock")
    private void awaitLoadedLocked() {
        if (!this.mLoaded) {
            BlockGuard.getThreadPolicy().onReadFromDisk();
        }
        while (!this.mLoaded) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
            }
        }
        if (this.mThrowable != null) {
            throw new IllegalStateException(this.mThrowable);
        }
    }

    public Map<String, ?> getAll() {
        HashMap hashMap;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            hashMap = new HashMap(this.mMap);
        }
        return hashMap;
    }

    public String getString(String key, String defValue) {
        String str;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            String v = (String) this.mMap.get(key);
            str = v != null ? v : defValue;
        }
        return str;
    }

    public Set<String> getStringSet(String key, Set<String> defValues) {
        Set<String> set;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Set<String> v = (Set) this.mMap.get(key);
            set = v != null ? v : defValues;
        }
        return set;
    }

    public int getInt(String key, int defValue) {
        int intValue;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Integer v = (Integer) this.mMap.get(key);
            intValue = v != null ? v.intValue() : defValue;
        }
        return intValue;
    }

    public long getLong(String key, long defValue) {
        long longValue;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Long v = (Long) this.mMap.get(key);
            longValue = v != null ? v.longValue() : defValue;
        }
        return longValue;
    }

    public float getFloat(String key, float defValue) {
        float floatValue;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Float v = (Float) this.mMap.get(key);
            floatValue = v != null ? v.floatValue() : defValue;
        }
        return floatValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        boolean booleanValue;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Boolean v = (Boolean) this.mMap.get(key);
            booleanValue = v != null ? v.booleanValue() : defValue;
        }
        return booleanValue;
    }

    public boolean contains(String key) {
        boolean containsKey;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            containsKey = this.mMap.containsKey(key);
        }
        return containsKey;
    }

    public Editor edit() {
        synchronized (this.mLock) {
            awaitLoadedLocked();
        }
        return new EditorImpl();
    }

    private void enqueueDiskWrite(final MemoryCommitResult mcr, final Runnable postWriteRunnable) {
        boolean z = false;
        final boolean isFromSyncCommit = postWriteRunnable == null;
        Runnable writeToDiskRunnable = new Runnable() {
            public void run() {
                synchronized (SharedPreferencesImpl.this.mWritingToDiskLock) {
                    SharedPreferencesImpl.this.writeToFile(mcr, isFromSyncCommit);
                }
                synchronized (SharedPreferencesImpl.this.mLock) {
                    SharedPreferencesImpl.this.mDiskWritesInFlight = SharedPreferencesImpl.this.mDiskWritesInFlight - 1;
                }
                if (postWriteRunnable != null) {
                    postWriteRunnable.run();
                }
            }
        };
        if (isFromSyncCommit) {
            boolean wasEmpty;
            synchronized (this.mLock) {
                wasEmpty = this.mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
                writeToDiskRunnable.run();
                return;
            }
        }
        if (!isFromSyncCommit) {
            z = true;
        }
        QueuedWork.queue(writeToDiskRunnable, z);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        String str;
        StringBuilder stringBuilder;
        FileOutputStream str2 = null;
        try {
            str2 = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (parent.mkdir()) {
                FileUtils.setPermissions(parent.getPath(), RadioError.OEM_ERROR_5, -1, -1);
                try {
                    str2 = new FileOutputStream(file);
                } catch (FileNotFoundException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't create SharedPreferences file ");
                    stringBuilder.append(file);
                    Log.e(str, stringBuilder.toString(), e2);
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't create directory for SharedPreferences file ");
                stringBuilder.append(file);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
        return str2;
    }

    @GuardedBy("mWritingToDiskLock")
    private void writeToFile(MemoryCommitResult mcr, boolean isFromSyncCommit) {
        Throwable th;
        String str;
        StringBuilder stringBuilder;
        MemoryCommitResult memoryCommitResult = mcr;
        long startTime = 0;
        long existsTime;
        long backupExistsTime;
        if (this.mFile.exists()) {
            boolean needsWrite = false;
            existsTime = 0;
            if (this.mDiskStateGeneration >= memoryCommitResult.memoryStateGeneration) {
                backupExistsTime = 0;
            } else if (isFromSyncCommit) {
                needsWrite = true;
                backupExistsTime = 0;
            } else {
                synchronized (this.mLock) {
                    try {
                        backupExistsTime = 0;
                        if (this.mCurrentMemoryStateGeneration == memoryCommitResult.memoryStateGeneration) {
                            needsWrite = true;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
            if (!needsWrite) {
                memoryCommitResult.setDiskWriteResult(false, true);
                return;
            } else if (this.mBackupFile.exists()) {
                this.mFile.delete();
            } else if (!this.mFile.renameTo(this.mBackupFile)) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Couldn't rename file ");
                stringBuilder2.append(this.mFile);
                stringBuilder2.append(" to backup file ");
                stringBuilder2.append(this.mBackupFile);
                Log.e(str2, stringBuilder2.toString());
                memoryCommitResult.setDiskWriteResult(false, false);
                return;
            }
        }
        existsTime = 0;
        backupExistsTime = 0;
        try {
            FileOutputStream str3 = createFileOutputStream(this.mFile);
            if (str3 == null) {
                memoryCommitResult.setDiskWriteResult(false, false);
                return;
            }
            XmlUtils.writeMapXml(memoryCommitResult.mapToWriteToDisk, str3);
            long writeTime = System.currentTimeMillis();
            FileUtils.sync(str3);
            long fsyncTime = System.currentTimeMillis();
            str3.close();
            ContextImpl.setFilePermissionsFromMode(this.mFile.getPath(), this.mMode, 0);
            try {
                StructStat stat = Os.stat(this.mFile.getPath());
                synchronized (this.mLock) {
                    this.mStatTimestamp = stat.st_mtim;
                    this.mStatSize = stat.st_size;
                }
            } catch (ErrnoException e) {
            }
            this.mBackupFile.delete();
            this.mDiskStateGeneration = memoryCommitResult.memoryStateGeneration;
            memoryCommitResult.setDiskWriteResult(true, true);
            long fsyncDuration = fsyncTime - writeTime;
            this.mSyncTimes.add((int) fsyncDuration);
            this.mNumSync++;
            if (this.mNumSync % 1024 == 0 || fsyncDuration > 256) {
                ExponentiallyBucketedHistogram exponentiallyBucketedHistogram = this.mSyncTimes;
                String str4 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Time required to fsync ");
                stringBuilder3.append(this.mFile);
                stringBuilder3.append(": ");
                exponentiallyBucketedHistogram.log(str4, stringBuilder3.toString());
            }
        } catch (XmlPullParserException e2) {
            Log.w(TAG, "writeToFile: Got exception:", e2);
            if (this.mFile.exists() && !this.mFile.delete()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't clean up partially-written file ");
                stringBuilder.append(this.mFile);
                Log.e(str, stringBuilder.toString());
            }
            memoryCommitResult.setDiskWriteResult(false, false);
        } catch (IOException e3) {
            Log.w(TAG, "writeToFile: Got exception:", e3);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't clean up partially-written file ");
            stringBuilder.append(this.mFile);
            Log.e(str, stringBuilder.toString());
            memoryCommitResult.setDiskWriteResult(false, false);
        }
    }
}
