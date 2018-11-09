package android.database.sqlite;

import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.DefaultDatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabaseEx.DatabaseConnectionExclusiveHandler;
import android.database.sqlite.SQLiteDebug.DbStats;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Printer;
import com.android.internal.util.Preconditions;
import com.huawei.indexsearch.IndexSearchParser;
import dalvik.system.CloseGuard;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class SQLiteDatabase extends SQLiteClosable {
    static final /* synthetic */ boolean -assertionsDisabled = (SQLiteDatabase.class.desiredAssertionStatus() ^ 1);
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_REPLACE = 5;
    public static final int CONFLICT_ROLLBACK = 1;
    private static final String[] CONFLICT_VALUES = new String[]{"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};
    public static final int CREATE_IF_NECESSARY = 268435456;
    private static final boolean DEBUG_CLOSE_IDLE_CONNECTIONS = SystemProperties.getBoolean("persist.debug.sqlite.close_idle_connections", false);
    public static final int ENABLE_WRITE_AHEAD_LOGGING = 536870912;
    private static final int EVENT_DB_CORRUPT = 75004;
    public static final int MAX_SQL_CACHE_SIZE = 100;
    public static final int NO_LOCALIZED_COLLATORS = 16;
    public static final int OPEN_READONLY = 1;
    public static final int OPEN_READWRITE = 0;
    private static final int OPEN_READ_MASK = 1;
    public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;
    private static final String TAG = "SQLiteDatabase";
    private static WeakHashMap<SQLiteDatabase, Object> sActiveDatabases = new WeakHashMap();
    private boolean bJankMonitor = true;
    private String jank_dbname = "";
    private final CloseGuard mCloseGuardLocked = CloseGuard.get();
    private final SQLiteDatabaseConfiguration mConfigurationLocked;
    private DatabaseConnectionExclusiveHandler mConnectionExclusiveHandler;
    private SQLiteConnectionPool mConnectionPoolLocked;
    private final CursorFactory mCursorFactory;
    private boolean mEnableExclusiveConnection = false;
    private final DatabaseErrorHandler mErrorHandler;
    private boolean mHasAttachedDbsLocked;
    private IndexSearchParser mIndexSearchParser = null;
    private JankSqlite mJankDBStats = new JankSqlite();
    private final Object mLock = new Object();
    private final ThreadLocal<SQLiteSession> mThreadSession = ThreadLocal.withInitial(new -$Lambda$gPaS7kMbZ8xtrrEx06GlwJ2iDWE(this));

    public interface CursorFactory {
        Cursor newCursor(SQLiteDatabase sQLiteDatabase, SQLiteCursorDriver sQLiteCursorDriver, String str, SQLiteQuery sQLiteQuery);
    }

    public interface CustomFunction {
        void callback(String[] strArr);
    }

    public static final class OpenParams {
        private final CursorFactory mCursorFactory;
        private final DatabaseErrorHandler mErrorHandler;
        private long mIdleConnectionTimeout;
        private final int mLookasideSlotCount;
        private final int mLookasideSlotSize;
        private final int mOpenFlags;

        public static final class Builder {
            private CursorFactory mCursorFactory;
            private DatabaseErrorHandler mErrorHandler;
            private long mIdleConnectionTimeout = -1;
            private int mLookasideSlotCount = -1;
            private int mLookasideSlotSize = -1;
            private int mOpenFlags;

            public android.database.sqlite.SQLiteDatabase.OpenParams.Builder removeOpenFlags(int r1) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.database.sqlite.SQLiteDatabase.OpenParams.Builder.removeOpenFlags(int):android.database.sqlite.SQLiteDatabase$OpenParams$Builder
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 9 more
*/
                /*
                // Can't load method instructions.
                */
                throw new UnsupportedOperationException("Method not decompiled: android.database.sqlite.SQLiteDatabase.OpenParams.Builder.removeOpenFlags(int):android.database.sqlite.SQLiteDatabase$OpenParams$Builder");
            }

            public Builder(OpenParams params) {
                this.mLookasideSlotSize = params.mLookasideSlotSize;
                this.mLookasideSlotCount = params.mLookasideSlotCount;
                this.mOpenFlags = params.mOpenFlags;
                this.mCursorFactory = params.mCursorFactory;
                this.mErrorHandler = params.mErrorHandler;
            }

            public Builder setLookasideConfig(int slotSize, int slotCount) {
                boolean z;
                boolean z2 = true;
                Preconditions.checkArgument(slotSize >= 0, "lookasideSlotCount cannot be negative");
                if (slotCount >= 0) {
                    z = true;
                } else {
                    z = false;
                }
                Preconditions.checkArgument(z, "lookasideSlotSize cannot be negative");
                if ((slotSize <= 0 || slotCount <= 0) && !(slotCount == 0 && slotSize == 0)) {
                    z2 = false;
                }
                Preconditions.checkArgument(z2, "Invalid configuration: " + slotSize + ", " + slotCount);
                this.mLookasideSlotSize = slotSize;
                this.mLookasideSlotCount = slotCount;
                return this;
            }

            public boolean isWriteAheadLoggingEnabled() {
                return (this.mOpenFlags & 536870912) != 0;
            }

            public Builder setOpenFlags(int openFlags) {
                this.mOpenFlags = openFlags;
                return this;
            }

            public Builder addOpenFlags(int openFlags) {
                this.mOpenFlags |= openFlags;
                return this;
            }

            public void setWriteAheadLoggingEnabled(boolean enabled) {
                if (enabled) {
                    addOpenFlags(536870912);
                } else {
                    removeOpenFlags(536870912);
                }
            }

            public Builder setCursorFactory(CursorFactory cursorFactory) {
                this.mCursorFactory = cursorFactory;
                return this;
            }

            public Builder setErrorHandler(DatabaseErrorHandler errorHandler) {
                this.mErrorHandler = errorHandler;
                return this;
            }

            public Builder setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
                Preconditions.checkArgument(idleConnectionTimeoutMs >= 0, "idle connection timeout cannot be negative");
                this.mIdleConnectionTimeout = idleConnectionTimeoutMs;
                return this;
            }

            public OpenParams build() {
                return new OpenParams(this.mOpenFlags, this.mCursorFactory, this.mErrorHandler, this.mLookasideSlotSize, this.mLookasideSlotCount, this.mIdleConnectionTimeout);
            }
        }

        private OpenParams(int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler, int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeout) {
            this.mOpenFlags = openFlags;
            this.mCursorFactory = cursorFactory;
            this.mErrorHandler = errorHandler;
            this.mLookasideSlotSize = lookasideSlotSize;
            this.mLookasideSlotCount = lookasideSlotCount;
            this.mIdleConnectionTimeout = idleConnectionTimeout;
        }

        public int getLookasideSlotSize() {
            return this.mLookasideSlotSize;
        }

        public int getLookasideSlotCount() {
            return this.mLookasideSlotCount;
        }

        public int getOpenFlags() {
            return this.mOpenFlags;
        }

        public CursorFactory getCursorFactory() {
            return this.mCursorFactory;
        }

        public DatabaseErrorHandler getErrorHandler() {
            return this.mErrorHandler;
        }

        public long getIdleConnectionTimeout() {
            return this.mIdleConnectionTimeout;
        }

        public Builder toBuilder() {
            return new Builder(this);
        }
    }

    private void triggerDeletingCalendarAccounts(java.lang.String r9, java.lang.Object[] r10) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0070 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r8 = this;
        r6 = r8.mIndexSearchParser;
        if (r6 == 0) goto L_0x007e;
    L_0x0004:
        r6 = "DELETE FROM Calendars WHERE account_name=? AND account_type=?";
        r6 = r6.equals(r9);
        if (r6 == 0) goto L_0x007e;
    L_0x000d:
        r0 = 0;
        r1 = 0;
        r5 = 0;
        r4 = new java.util.ArrayList;
        r4.<init>();
        r6 = "SELECT _id FROM Calendars where account_name=? AND account_type=?";	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r10 = (java.lang.String[]) r10;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r0 = r8.rawQuery(r6, r10);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r0 == 0) goto L_0x0085;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0020:
        r6 = r0.getCount();	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r6 <= 0) goto L_0x0085;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0026:
        r6 = r0.getCount();	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r2 = new java.lang.String[r6];	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x002c:
        r6 = r0.moveToNext();	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r6 == 0) goto L_0x003c;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0032:
        r6 = 0;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r6 = r0.getString(r6);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r2[r5] = r6;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r5 = r5 + 1;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        goto L_0x002c;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x003c:
        r6 = "Events";	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r7 = "calendar_id IN (?)";	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r1 = r8.queryForIndexSearch(r6, r7, r2);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r1 == 0) goto L_0x0085;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0048:
        r6 = r1.getCount();	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r6 <= 0) goto L_0x0085;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x004e:
        r6 = r1.moveToNext();	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r6 == 0) goto L_0x007f;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0054:
        r6 = 0;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r6 = r1.getLong(r6);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r6 = java.lang.Long.valueOf(r6);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r4.add(r6);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        goto L_0x004e;
    L_0x0061:
        r3 = move-exception;
        r6 = "SQLiteDatabase";	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r7 = "triggerDeletingCalendarAccounts(): RuntimeException";	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        android.util.Log.e(r6, r7);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        if (r0 == 0) goto L_0x0070;
    L_0x006d:
        r0.close();
    L_0x0070:
        if (r1 == 0) goto L_0x0075;
    L_0x0072:
        r1.close();
    L_0x0075:
        r6 = r4.isEmpty();
        if (r6 != 0) goto L_0x007e;
    L_0x007b:
        r4.clear();
    L_0x007e:
        return;
    L_0x007f:
        r6 = r8.mIndexSearchParser;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r7 = 2;	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
        r6.notifyIndexSearchService(r4, r7);	 Catch:{ RuntimeException -> 0x0061, all -> 0x0099 }
    L_0x0085:
        if (r0 == 0) goto L_0x008a;
    L_0x0087:
        r0.close();
    L_0x008a:
        if (r1 == 0) goto L_0x008f;
    L_0x008c:
        r1.close();
    L_0x008f:
        r6 = r4.isEmpty();
        if (r6 != 0) goto L_0x007e;
    L_0x0095:
        r4.clear();
        goto L_0x007e;
    L_0x0099:
        r6 = move-exception;
        if (r0 == 0) goto L_0x009f;
    L_0x009c:
        r0.close();
    L_0x009f:
        if (r1 == 0) goto L_0x00a4;
    L_0x00a1:
        r1.close();
    L_0x00a4:
        r7 = r4.isEmpty();
        if (r7 != 0) goto L_0x00ad;
    L_0x00aa:
        r4.clear();
    L_0x00ad:
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.database.sqlite.SQLiteDatabase.triggerDeletingCalendarAccounts(java.lang.String, java.lang.Object[]):void");
    }

    /* synthetic */ SQLiteSession -android_database_sqlite_SQLiteDatabase-mthref-0() {
        return createSession();
    }

    private SQLiteDatabase(String path, int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler, int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeoutMs) {
        this.mCursorFactory = cursorFactory;
        if (errorHandler == null) {
            errorHandler = new DefaultDatabaseErrorHandler();
        }
        this.mErrorHandler = errorHandler;
        this.mConfigurationLocked = new SQLiteDatabaseConfiguration(path, openFlags);
        this.mConfigurationLocked.lookasideSlotSize = lookasideSlotSize;
        this.mConfigurationLocked.lookasideSlotCount = lookasideSlotCount;
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mConfigurationLocked.lookasideSlotCount = 0;
            this.mConfigurationLocked.lookasideSlotSize = 0;
        }
        long effectiveTimeoutMs = Long.MAX_VALUE;
        if (!this.mConfigurationLocked.isInMemoryDb()) {
            if (idleConnectionTimeoutMs >= 0) {
                effectiveTimeoutMs = idleConnectionTimeoutMs;
            } else if (DEBUG_CLOSE_IDLE_CONNECTIONS) {
                effectiveTimeoutMs = (long) SQLiteGlobal.getIdleConnectionTimeout();
            }
        }
        this.mConfigurationLocked.idleConnectionTimeoutMs = effectiveTimeoutMs;
        this.jank_dbname = path;
        if (this.jank_dbname.equals("JankEventDb.db")) {
            this.bJankMonitor = false;
        }
        this.mIndexSearchParser = IndexSearchParser.getInstance();
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    protected void onAllReferencesReleased() {
        dispose(false);
    }

    private void dispose(boolean finalized) {
        synchronized (this.mLock) {
            if (this.mCloseGuardLocked != null) {
                if (finalized) {
                    this.mCloseGuardLocked.warnIfOpen();
                }
                this.mCloseGuardLocked.close();
            }
            SQLiteConnectionPool pool = this.mConnectionPoolLocked;
            this.mConnectionPoolLocked = null;
        }
        if (!finalized) {
            synchronized (sActiveDatabases) {
                sActiveDatabases.remove(this);
            }
            if (pool != null) {
                pool.close();
            }
        }
    }

    public static int releaseMemory() {
        return SQLiteGlobal.releaseMemory();
    }

    @Deprecated
    public void setLockingEnabled(boolean lockingEnabled) {
    }

    String getLabel() {
        String str;
        synchronized (this.mLock) {
            str = this.mConfigurationLocked.label;
        }
        return str;
    }

    void onCorruption() {
        EventLog.writeEvent(EVENT_DB_CORRUPT, getLabel());
        this.mErrorHandler.onCorruption(this);
    }

    SQLiteSession getThreadSession() {
        return (SQLiteSession) this.mThreadSession.get();
    }

    SQLiteSession createSession() {
        SQLiteConnectionPool pool;
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            pool = this.mConnectionPoolLocked;
        }
        return new SQLiteSession(pool);
    }

    int getThreadDefaultConnectionFlags(boolean readOnly) {
        int flags;
        if (readOnly) {
            flags = 1;
        } else {
            flags = 2;
        }
        if (this.mEnableExclusiveConnection && checkConnectionExclusiveHandler()) {
            flags |= 8;
        }
        if (isMainThread()) {
            return flags | 4;
        }
        return flags;
    }

    private static boolean isMainThread() {
        Looper looper = Looper.myLooper();
        if (looper == null || looper != Looper.getMainLooper()) {
            return false;
        }
        return true;
    }

    public void beginTransaction() {
        beginTransaction(null, true);
    }

    public void beginTransactionNonExclusive() {
        beginTransaction(null, false);
    }

    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
        beginTransaction(transactionListener, true);
    }

    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener transactionListener) {
        beginTransaction(transactionListener, false);
    }

    private void beginTransaction(SQLiteTransactionListener transactionListener, boolean exclusive) {
        acquireReference();
        try {
            int i;
            SQLiteSession threadSession = getThreadSession();
            if (exclusive) {
                i = 2;
            } else {
                i = 1;
            }
            threadSession.beginTransaction(i, transactionListener, getThreadDefaultConnectionFlags(false), null);
        } finally {
            releaseReference();
        }
    }

    public void endTransaction() {
        acquireReference();
        try {
            getThreadSession().endTransaction(null);
            if (getThreadSession().isCommitSuccess()) {
                handleTransMap();
                getThreadSession().clearTransMap();
            }
            releaseReference();
        } catch (Throwable th) {
            releaseReference();
        }
    }

    private void handleTransMap() {
        if (this.mIndexSearchParser != null && getThreadSession().getTransMap().size() > 0) {
            ArrayList<Long> insertArgs = new ArrayList();
            ArrayList<Long> updateArgs = new ArrayList();
            ArrayList<Long> deleteArgs = new ArrayList();
            for (SQLInfo sqlinfo : getThreadSession().getTransMap().keySet()) {
                IndexSearchParser indexSearchParser = this.mIndexSearchParser;
                if (IndexSearchParser.isValidTable(sqlinfo.getTable())) {
                    switch (((Integer) getThreadSession().getTransMap().get(sqlinfo)).intValue()) {
                        case 0:
                            insertArgs.add(Long.valueOf(sqlinfo.getPrimaryKey()));
                            break;
                        case 1:
                            updateArgs.add(Long.valueOf(sqlinfo.getPrimaryKey()));
                            break;
                        case 2:
                            deleteArgs.add(Long.valueOf(sqlinfo.getPrimaryKey()));
                            break;
                        default:
                            break;
                    }
                }
            }
            if (insertArgs.size() >= 1) {
                this.mIndexSearchParser.notifyIndexSearchService(insertArgs, 0);
            }
            if (updateArgs.size() >= 1) {
                this.mIndexSearchParser.notifyIndexSearchService(updateArgs, 1);
            }
            if (deleteArgs.size() >= 1) {
                this.mIndexSearchParser.notifyIndexSearchService(deleteArgs, 2);
            }
        }
    }

    public void setTransactionSuccessful() {
        acquireReference();
        try {
            getThreadSession().setTransactionSuccessful();
        } finally {
            releaseReference();
        }
    }

    public boolean inTransaction() {
        acquireReference();
        try {
            boolean hasTransaction = getThreadSession().hasTransaction();
            return hasTransaction;
        } finally {
            releaseReference();
        }
    }

    public boolean isDbLockedByCurrentThread() {
        acquireReference();
        try {
            boolean hasConnection = getThreadSession().hasConnection();
            return hasConnection;
        } finally {
            releaseReference();
        }
    }

    @Deprecated
    public boolean isDbLockedByOtherThreads() {
        return false;
    }

    @Deprecated
    public boolean yieldIfContended() {
        return yieldIfContendedHelper(false, -1);
    }

    public boolean yieldIfContendedSafely() {
        return yieldIfContendedHelper(true, -1);
    }

    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return yieldIfContendedHelper(true, sleepAfterYieldDelay);
    }

    private boolean yieldIfContendedHelper(boolean throwIfUnsafe, long sleepAfterYieldDelay) {
        acquireReference();
        try {
            boolean yieldTransaction = getThreadSession().yieldTransaction(sleepAfterYieldDelay, throwIfUnsafe, null);
            return yieldTransaction;
        } finally {
            releaseReference();
        }
    }

    @Deprecated
    public Map<String, String> getSyncedTables() {
        return new HashMap(0);
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
        return openDatabase(path, factory, flags, null);
    }

    public static SQLiteDatabase openDatabase(File path, OpenParams openParams) {
        return openDatabase(path.getPath(), openParams);
    }

    private static SQLiteDatabase openDatabase(String path, OpenParams openParams) {
        Preconditions.checkArgument(openParams != null, "OpenParams cannot be null");
        SQLiteDatabase db = new SQLiteDatabase(path, openParams.mOpenFlags, openParams.mCursorFactory, openParams.mErrorHandler, openParams.mLookasideSlotSize, openParams.mLookasideSlotCount, openParams.mIdleConnectionTimeout);
        db.open();
        return db;
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags, DatabaseErrorHandler errorHandler) {
        SQLiteDatabase db = new SQLiteDatabase(path, flags, factory, errorHandler, -1, -1, -1);
        db.open();
        return db;
    }

    public static SQLiteDatabase openOrCreateDatabase(File file, CursorFactory factory) {
        return openOrCreateDatabase(file.getPath(), factory);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory) {
        return openDatabase(path, factory, 268435456, null);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openDatabase(path, factory, 268435456, errorHandler);
    }

    public static boolean deleteDatabase(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        boolean deleted = ((file.delete() | new File(file.getPath() + "-journal").delete()) | new File(file.getPath() + "-shm").delete()) | new File(file.getPath() + "-wal").delete();
        File dir = file.getParentFile();
        if (dir != null) {
            final String prefix = file.getName() + "-mj";
            File[] files = dir.listFiles(new FileFilter() {
                public boolean accept(File candidate) {
                    return candidate.getName().startsWith(prefix);
                }
            });
            if (files != null) {
                for (File masterJournal : files) {
                    deleted |= masterJournal.delete();
                }
            }
        }
        return deleted;
    }

    public void reopenReadWrite() {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            if (isReadOnlyLocked()) {
                int oldOpenFlags = this.mConfigurationLocked.openFlags;
                this.mConfigurationLocked.openFlags = (this.mConfigurationLocked.openFlags & -2) | 0;
                try {
                    this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
                    return;
                } catch (RuntimeException ex) {
                    this.mConfigurationLocked.openFlags = oldOpenFlags;
                    throw ex;
                }
            }
        }
    }

    private void open() {
        try {
            openInner();
        } catch (SQLiteDatabaseCorruptException e) {
            try {
                onCorruption();
                openInner();
            } catch (SQLiteException ex) {
                Log.e(TAG, "Failed to open database '" + getLabel() + "'.", ex);
                close();
                throw ex;
            }
        }
    }

    private void openInner() {
        synchronized (this.mLock) {
            if (-assertionsDisabled || this.mConnectionPoolLocked == null) {
                this.mConnectionPoolLocked = SQLiteConnectionPool.open(this.mConfigurationLocked, this.mEnableExclusiveConnection);
                this.mCloseGuardLocked.open("close");
            } else {
                throw new AssertionError();
            }
        }
        synchronized (sActiveDatabases) {
            sActiveDatabases.put(this, null);
        }
    }

    public static SQLiteDatabase create(CursorFactory factory) {
        return openDatabase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH, factory, 268435456);
    }

    public static SQLiteDatabase createInMemory(OpenParams openParams) {
        return openDatabase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH, openParams.toBuilder().addOpenFlags(268435456).build());
    }

    public void addCustomFunction(String name, int numArgs, CustomFunction function) {
        SQLiteCustomFunction wrapper = new SQLiteCustomFunction(name, numArgs, function);
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            this.mConfigurationLocked.customFunctions.add(wrapper);
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                this.mConfigurationLocked.customFunctions.remove(wrapper);
                throw ex;
            }
        }
    }

    public int getVersion() {
        return Long.valueOf(DatabaseUtils.longForQuery(this, "PRAGMA user_version;", null)).intValue();
    }

    public void setVersion(int version) {
        execSQL("PRAGMA user_version = " + version);
    }

    public long getMaximumSize() {
        return getPageSize() * DatabaseUtils.longForQuery(this, "PRAGMA max_page_count;", null);
    }

    public long setMaximumSize(long numBytes) {
        long pageSize = getPageSize();
        long numPages = numBytes / pageSize;
        if (numBytes % pageSize != 0) {
            numPages++;
        }
        return DatabaseUtils.longForQuery(this, "PRAGMA max_page_count = " + numPages, null) * pageSize;
    }

    public long getPageSize() {
        return DatabaseUtils.longForQuery(this, "PRAGMA page_size;", null);
    }

    public void setPageSize(long numBytes) {
        execSQL("PRAGMA page_size = " + numBytes);
    }

    @Deprecated
    public void markTableSyncable(String table, String deletedTable) {
    }

    @Deprecated
    public void markTableSyncable(String table, String foreignKey, String updateTable) {
    }

    public static String findEditTable(String tables) {
        if (TextUtils.isEmpty(tables)) {
            throw new IllegalStateException("Invalid tables");
        }
        int spacepos = tables.indexOf(32);
        int commapos = tables.indexOf(44);
        if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
            return tables.substring(0, spacepos);
        }
        if (commapos <= 0 || (commapos >= spacepos && spacepos >= 0)) {
            return tables;
        }
        return tables.substring(0, commapos);
    }

    public SQLiteStatement compileStatement(String sql) throws SQLException {
        acquireReference();
        try {
            SQLiteStatement sQLiteStatement = new SQLiteStatement(this, sql, null);
            return sQLiteStatement;
        } finally {
            releaseReference();
        }
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal) {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return queryWithFactory(cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor queryWithFactory(CursorFactory cursorFactory, boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal) {
        acquireReference();
        try {
            Cursor rawQueryWithFactory = rawQueryWithFactory(cursorFactory, SQLiteQueryBuilder.buildQueryString(distinct, table, columns, selection, groupBy, having, orderBy, limit), selectionArgs, findEditTable(table), cancellationSignal);
            return rawQueryWithFactory;
        } finally {
            releaseReference();
        }
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, null);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal) {
        return rawQueryWithFactory(null, sql, selectionArgs, null, cancellationSignal);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs, String editTable) {
        return rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable, null);
    }

    public Cursor rawQueryWithFactory(CursorFactory cursorFactory, String sql, String[] selectionArgs, String editTable, CancellationSignal cancellationSignal) {
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        try {
            SQLiteCursorDriver driver = new SQLiteDirectCursorDriver(this, sql, editTable, cancellationSignal);
            if (cursorFactory == null) {
                cursorFactory = this.mCursorFactory;
            }
            Cursor query = driver.query(cursorFactory, selectionArgs);
            return query;
        } finally {
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addQuery(SystemClock.uptimeMillis() - begin, editTable, this.jank_dbname);
            }
        }
    }

    private Cursor queryForIndexSearch(String table, String whereClause, String[] whereArgs) {
        StringBuilder sql = new StringBuilder();
        if ("files".equals(table)) {
            sql.append("SELECT _id FROM ").append(table).append(" WHERE ").append(whereClause).append(" AND ").append("((mime_type='text/plain') OR (mime_type='text/html') OR (mime_type='text/htm') OR (mime_type = 'application/msword') OR (mime_type = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') OR (mime_type = 'application/vnd.ms-excel') OR (mime_type = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') OR (mime_type = 'application/mspowerpoint') OR (mime_type = 'application/vnd.openxmlformats-officedocument.presentationml.presentation')) ");
        } else if ("Body".equals(table)) {
            sql.append("SELECT messageKey FROM Body WHERE ").append(whereClause);
        } else if ("Events".equals(table)) {
            sql.append("SELECT _id FROM ").append(table).append(" WHERE ").append(whereClause).append(" AND ").append("mutators IS NOT 'com.android.providers.contacts'");
        } else if ("Calendars".equals(table)) {
            if (whereArgs == null || whereArgs.length != 1) {
                sql.append("SELECT _id FROM ").append(table).append(" WHERE ").append(whereClause);
            } else {
                sql.append("SELECT _id FROM Events WHERE calendar_id IN (?)");
            }
        } else if ("Mailbox".equals(table)) {
            sql.append("SELECT _id FROM Message WHERE ").append(whereClause);
        } else if (!"fav_sms".equals(table)) {
            sql.append("SELECT _id FROM ").append(table).append(" WHERE ").append(whereClause);
        } else if (whereClause == null) {
            sql.append("SELECT _id FROM words WHERE table_to_use IS 8");
        } else {
            sql.append("SELECT _id FROM words WHERE ").append(whereClause.replace(DownloadManager.COLUMN_ID, "source_id")).append(" AND ").append("table_to_use IS 8");
        }
        return rawQuery(sql.toString(), whereArgs);
    }

    private void triggerUpdatingOrDeletingIndex(String table, String whereClause, String[] whereArgs, int operation) {
        if (this.mIndexSearchParser != null) {
            IndexSearchParser indexSearchParser = this.mIndexSearchParser;
            if (IndexSearchParser.isValidTable(table)) {
                Cursor cursor = null;
                try {
                    cursor = queryForIndexSearch(table, whereClause, whereArgs);
                    if (cursor == null || cursor.getCount() == 0) {
                        Log.v(TAG, "triggerBuildingIndex(): cursor is null or count is 0, return.");
                        if (cursor != null) {
                            cursor.close();
                        }
                        return;
                    }
                    if (getThreadSession().hasTransaction()) {
                        while (cursor.moveToNext()) {
                            String realTable;
                            if (table.equals("Body")) {
                                realTable = "Message";
                            } else {
                                realTable = table;
                            }
                            getThreadSession().insertTransMap(realTable, cursor.getLong(0), operation);
                        }
                    } else {
                        List<Long> ids = new ArrayList();
                        while (cursor.moveToNext()) {
                            ids.add(Long.valueOf(cursor.getLong(0)));
                        }
                        this.mIndexSearchParser.notifyIndexSearchService(ids, operation);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "triggerUpdatingOrDeletingIndex(): RuntimeException");
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "triggerUpdatingOrDeletingIndex(): Exception");
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    private void triggerAddingIndex(String table, long id) {
        if (id < 0) {
            Log.v(TAG, "triggerBuildingIndex(): invalid id, return.");
            return;
        }
        if (this.mIndexSearchParser != null) {
            IndexSearchParser indexSearchParser = this.mIndexSearchParser;
            if (IndexSearchParser.isValidTable(table)) {
                if (table.equals("Events")) {
                    Cursor cursor = null;
                    try {
                        cursor = rawQuery("SELECT _id FROM Events WHERE _id = " + id + " AND mutators IS NOT 'com.android.providers.contacts'", null);
                        if (cursor == null || cursor.getCount() == 0) {
                            if (cursor != null) {
                                cursor.close();
                            }
                            return;
                        } else if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                if (getThreadSession().hasTransaction()) {
                    String realTable;
                    if (table.equals("Body")) {
                        realTable = "Message";
                    } else {
                        realTable = table;
                    }
                    getThreadSession().insertTransMap(realTable, id, 0);
                } else {
                    this.mIndexSearchParser.notifyIndexSearchService(id, 0);
                }
            }
        }
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, 0);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + e.getMessage());
            return -1;
        }
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, values, 0);
    }

    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues, 5);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + e.getMessage());
            return -1;
        }
    }

    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, initialValues, 5);
    }

    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) {
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        SQLiteStatement sQLiteStatement;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT");
            sql.append(CONFLICT_VALUES[conflictAlgorithm]);
            sql.append(" INTO ");
            sql.append(table);
            sql.append('(');
            Object[] objArr = null;
            int size = (initialValues == null || (initialValues.isEmpty() ^ 1) == 0) ? 0 : initialValues.size();
            if (size > 0) {
                int i;
                objArr = new Object[size];
                int i2 = 0;
                for (String colName : initialValues.keySet()) {
                    sql.append(i2 > 0 ? "," : "");
                    sql.append(colName);
                    i = i2 + 1;
                    objArr[i2] = initialValues.get(colName);
                    i2 = i;
                }
                sql.append(')');
                sql.append(" VALUES (");
                i = 0;
                while (i < size) {
                    sql.append(i > 0 ? ",?" : "?");
                    i++;
                }
            } else {
                sql.append(nullColumnHack).append(") VALUES (NULL");
            }
            sql.append(')');
            sQLiteStatement = new SQLiteStatement(this, sql.toString(), objArr);
            long id = sQLiteStatement.executeInsert();
            triggerAddingIndex(table, id);
            sQLiteStatement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addInsert(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
            return id;
        } catch (Throwable th) {
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addInsert(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        SQLiteStatement statement;
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        try {
            statement = new SQLiteStatement(this, "DELETE FROM " + table + (!TextUtils.isEmpty(whereClause) ? " WHERE " + whereClause : ""), whereArgs);
            triggerUpdatingOrDeletingIndex(table, whereClause, whereArgs, 2);
            int executeUpdateDelete = statement.executeUpdateDelete();
            statement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addDelete(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
            return executeUpdateDelete;
        } catch (Throwable th) {
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addDelete(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
        }
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        triggerUpdatingOrDeletingIndex(table, whereClause, whereArgs, 1);
        return updateWithOnConflict(table, values, whereClause, whereArgs, 0);
    }

    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs, int conflictAlgorithm) {
        SQLiteStatement sQLiteStatement;
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }
        acquireReference();
        try {
            int i;
            StringBuilder sql = new StringBuilder(120);
            sql.append("UPDATE ");
            sql.append(CONFLICT_VALUES[conflictAlgorithm]);
            sql.append(table);
            sql.append(" SET ");
            int setValuesSize = values.size();
            int bindArgsSize = whereArgs == null ? setValuesSize : setValuesSize + whereArgs.length;
            Object[] bindArgs = new Object[bindArgsSize];
            int i2 = 0;
            for (String colName : values.keySet()) {
                sql.append(i2 > 0 ? "," : "");
                sql.append(colName);
                i = i2 + 1;
                bindArgs[i2] = values.get(colName);
                sql.append("=?");
                i2 = i;
            }
            if (whereArgs != null) {
                for (i = setValuesSize; i < bindArgsSize; i++) {
                    bindArgs[i] = whereArgs[i - setValuesSize];
                }
            }
            if (!TextUtils.isEmpty(whereClause)) {
                sql.append(" WHERE ");
                sql.append(whereClause);
            }
            sQLiteStatement = new SQLiteStatement(this, sql.toString(), bindArgs);
            int executeUpdateDelete = sQLiteStatement.executeUpdateDelete();
            sQLiteStatement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addUpdate(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
            return executeUpdateDelete;
        } catch (Throwable th) {
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addUpdate(SystemClock.uptimeMillis() - begin, table, this.jank_dbname);
            }
        }
    }

    public void execSQL(String sql) throws SQLException {
        executeSql(sql, null);
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        if (bindArgs == null) {
            throw new IllegalArgumentException("Empty bindArgs");
        }
        triggerDeletingCalendarAccounts(sql, bindArgs);
        executeSql(sql, bindArgs);
    }

    private int executeSql(String sql, Object[] bindArgs) throws SQLException {
        SQLiteStatement statement;
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        try {
            if (DatabaseUtils.getSqlStatementType(sql) == 3) {
                boolean disableWal = false;
                synchronized (this.mLock) {
                    if (!this.mHasAttachedDbsLocked) {
                        this.mHasAttachedDbsLocked = true;
                        disableWal = true;
                        this.mConnectionPoolLocked.disableIdleConnectionHandler();
                    }
                }
                if (disableWal) {
                    disableWriteAheadLogging();
                }
            }
            statement = new SQLiteStatement(this, sql, bindArgs);
            int executeUpdateDelete = statement.executeUpdateDelete();
            statement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addExecsql(SystemClock.uptimeMillis() - begin, "", this.jank_dbname);
            }
            return executeUpdateDelete;
        } catch (Throwable th) {
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addExecsql(SystemClock.uptimeMillis() - begin, "", this.jank_dbname);
            }
        }
    }

    public void validateSql(String sql, CancellationSignal cancellationSignal) {
        getThreadSession().prepare(sql, getThreadDefaultConnectionFlags(true), cancellationSignal, null);
    }

    public boolean isReadOnly() {
        boolean isReadOnlyLocked;
        synchronized (this.mLock) {
            isReadOnlyLocked = isReadOnlyLocked();
        }
        return isReadOnlyLocked;
    }

    private boolean isReadOnlyLocked() {
        return (this.mConfigurationLocked.openFlags & 1) == 1;
    }

    public boolean isInMemoryDatabase() {
        boolean isInMemoryDb;
        synchronized (this.mLock) {
            isInMemoryDb = this.mConfigurationLocked.isInMemoryDb();
        }
        return isInMemoryDb;
    }

    public boolean isOpen() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mConnectionPoolLocked != null;
        }
        return z;
    }

    public boolean needUpgrade(int newVersion) {
        return newVersion > getVersion();
    }

    public final String getPath() {
        String str;
        synchronized (this.mLock) {
            str = this.mConfigurationLocked.path;
        }
        return str;
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale must not be null.");
        }
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            Locale oldLocale = this.mConfigurationLocked.locale;
            this.mConfigurationLocked.locale = locale;
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                this.mConfigurationLocked.locale = oldLocale;
                throw ex;
            }
        }
    }

    public void setMaxSqlCacheSize(int cacheSize) {
        if (cacheSize > 100 || cacheSize < 0) {
            throw new IllegalStateException("expected value between 0 and 100");
        }
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            int oldMaxSqlCacheSize = this.mConfigurationLocked.maxSqlCacheSize;
            this.mConfigurationLocked.maxSqlCacheSize = cacheSize;
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                this.mConfigurationLocked.maxSqlCacheSize = oldMaxSqlCacheSize;
                throw ex;
            }
        }
    }

    public void setForeignKeyConstraintsEnabled(boolean enable) {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            if (this.mConfigurationLocked.foreignKeyConstraintsEnabled == enable) {
                return;
            }
            this.mConfigurationLocked.foreignKeyConstraintsEnabled = enable;
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                this.mConfigurationLocked.foreignKeyConstraintsEnabled = enable ^ 1;
                throw ex;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean enableWriteAheadLogging() {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            boolean defaultWALEnabled = this.mConfigurationLocked.defaultWALEnabled;
            boolean explicitWALEnabled = this.mConfigurationLocked.explicitWALEnabled;
            if (this.mConfigurationLocked.configurationEnhancement) {
                if ((this.mConfigurationLocked.openFlags & 536870912) != 0 && this.mConfigurationLocked.explicitWALEnabled) {
                    return true;
                }
            } else if ((this.mConfigurationLocked.openFlags & 536870912) != 0) {
                return true;
            }
            if (isReadOnlyLocked()) {
                return false;
            } else if (this.mConfigurationLocked.isInMemoryDb()) {
                Log.i(TAG, "can't enable WAL for memory databases.");
                return false;
            } else if (!this.mHasAttachedDbsLocked) {
                SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration = this.mConfigurationLocked;
                sQLiteDatabaseConfiguration.openFlags |= 536870912;
                if (this.mConfigurationLocked.configurationEnhancement) {
                    this.mConfigurationLocked.defaultWALEnabled = false;
                    this.mConfigurationLocked.explicitWALEnabled = true;
                }
                try {
                    this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
                    return true;
                } catch (RuntimeException ex) {
                    sQLiteDatabaseConfiguration = this.mConfigurationLocked;
                    sQLiteDatabaseConfiguration.openFlags &= -536870913;
                    if (this.mConfigurationLocked.configurationEnhancement) {
                        this.mConfigurationLocked.defaultWALEnabled = defaultWALEnabled;
                        this.mConfigurationLocked.explicitWALEnabled = explicitWALEnabled;
                    }
                    throw ex;
                }
            } else if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "this database: " + this.mConfigurationLocked.label + " has attached databases. can't  enable WAL.");
            }
        }
    }

    public void disableWriteAheadLogging() {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            boolean defaultWALEnabled = this.mConfigurationLocked.defaultWALEnabled;
            boolean explicitWALEnabled = this.mConfigurationLocked.explicitWALEnabled;
            if ((this.mConfigurationLocked.openFlags & 536870912) == 0) {
                return;
            }
            SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration = this.mConfigurationLocked;
            sQLiteDatabaseConfiguration.openFlags &= -536870913;
            if (this.mConfigurationLocked.configurationEnhancement) {
                this.mConfigurationLocked.defaultWALEnabled = false;
                this.mConfigurationLocked.explicitWALEnabled = false;
            }
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                sQLiteDatabaseConfiguration = this.mConfigurationLocked;
                sQLiteDatabaseConfiguration.openFlags |= 536870912;
                if (this.mConfigurationLocked.configurationEnhancement) {
                    this.mConfigurationLocked.defaultWALEnabled = defaultWALEnabled;
                    this.mConfigurationLocked.explicitWALEnabled = explicitWALEnabled;
                }
                throw ex;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isWriteAheadLoggingEnabled() {
        boolean z = true;
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            if (this.mConfigurationLocked.configurationEnhancement) {
                if ((this.mConfigurationLocked.openFlags & 536870912) == 0 || !this.mConfigurationLocked.explicitWALEnabled) {
                    z = false;
                }
            } else if ((this.mConfigurationLocked.openFlags & 536870912) == 0) {
                z = false;
            }
        }
    }

    static ArrayList<DbStats> getDbStats() {
        ArrayList<DbStats> dbStatsList = new ArrayList();
        for (SQLiteDatabase db : getActiveDatabases()) {
            db.collectDbStats(dbStatsList);
        }
        return dbStatsList;
    }

    private void collectDbStats(ArrayList<DbStats> dbStatsList) {
        synchronized (this.mLock) {
            if (this.mConnectionPoolLocked != null) {
                this.mConnectionPoolLocked.collectDbStats(dbStatsList);
            }
        }
    }

    private static ArrayList<SQLiteDatabase> getActiveDatabases() {
        ArrayList<SQLiteDatabase> databases = new ArrayList();
        synchronized (sActiveDatabases) {
            databases.addAll(sActiveDatabases.keySet());
        }
        return databases;
    }

    static void dumpAll(Printer printer, boolean verbose) {
        for (SQLiteDatabase db : getActiveDatabases()) {
            db.dump(printer, verbose);
        }
    }

    private void dump(Printer printer, boolean verbose) {
        synchronized (this.mLock) {
            if (this.mConnectionPoolLocked != null) {
                printer.println("");
                this.mConnectionPoolLocked.dump(printer, verbose);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<Pair<String, String>> getAttachedDbs() {
        ArrayList<Pair<String, String>> attachedDbs = new ArrayList();
        synchronized (this.mLock) {
            if (this.mConnectionPoolLocked == null) {
                return null;
            } else if (this.mHasAttachedDbsLocked) {
                acquireReference();
            } else {
                attachedDbs.add(new Pair("main", this.mConfigurationLocked.path));
                return attachedDbs;
            }
        }
    }

    public boolean isDatabaseIntegrityOk() {
        List<Pair<String, String>> attachedDbs;
        SQLiteStatement sQLiteStatement;
        List<Pair<String, String>> attachedDbs2;
        Throwable th;
        acquireReference();
        try {
            attachedDbs = getAttachedDbs();
            if (attachedDbs == null) {
                throw new IllegalStateException("databaselist for: " + getPath() + " couldn't " + "be retrieved. probably because the database is closed");
            }
        } catch (SQLiteException e) {
            attachedDbs2 = new ArrayList();
            attachedDbs2.add(new Pair("main", getPath()));
            attachedDbs = attachedDbs2;
        } catch (Throwable th2) {
            th = th2;
            attachedDbs = attachedDbs2;
        }
        int i = 0;
        while (i < attachedDbs.size()) {
            Pair<String, String> p = (Pair) attachedDbs.get(i);
            sQLiteStatement = null;
            sQLiteStatement = compileStatement("PRAGMA " + ((String) p.first) + ".integrity_check(1);");
            String rslt = sQLiteStatement.simpleQueryForString();
            if (rslt.equalsIgnoreCase("ok")) {
                if (sQLiteStatement != null) {
                    sQLiteStatement.close();
                }
                i++;
            } else {
                Log.e(TAG, "PRAGMA integrity_check on " + ((String) p.second) + " returned: " + rslt);
                if (sQLiteStatement != null) {
                    sQLiteStatement.close();
                }
                releaseReference();
                return false;
            }
        }
        releaseReference();
        return true;
        releaseReference();
        throw th;
    }

    public String toString() {
        return "SQLiteDatabase: " + getPath();
    }

    private void throwIfNotOpenLocked() {
        if (this.mConnectionPoolLocked == null) {
            throw new IllegalStateException("The database '" + this.mConfigurationLocked.label + "' is not open.");
        }
    }

    public void enableExclusiveConnection(boolean enabled, DatabaseConnectionExclusiveHandler connectionExclusiveHandler) {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            this.mEnableExclusiveConnection = enabled;
            this.mConnectionExclusiveHandler = connectionExclusiveHandler;
            if (this.mConnectionPoolLocked != null) {
                this.mConnectionPoolLocked.setExclusiveConnectionEnabled(this.mEnableExclusiveConnection);
            }
        }
    }

    private boolean checkConnectionExclusiveHandler() {
        if (this.mConnectionExclusiveHandler != null) {
            return this.mConnectionExclusiveHandler.onConnectionExclusive();
        }
        return false;
    }
}
