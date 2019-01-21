package android.database.sqlite;

import android.app.ActivityManager;
import android.common.HwFrameworkFactory;
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
import com.huawei.indexsearch.IIndexSearchParser;
import dalvik.system.CloseGuard;
import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class SQLiteDatabase extends SQLiteClosable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_REPLACE = 5;
    public static final int CONFLICT_ROLLBACK = 1;
    private static final String[] CONFLICT_VALUES = new String[]{"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};
    public static final int CREATE_IF_NECESSARY = 268435456;
    private static final boolean DEBUG_CLOSE_IDLE_CONNECTIONS = SystemProperties.getBoolean("persist.debug.sqlite.close_idle_connections", false);
    public static final int DISABLE_COMPATIBILITY_WAL = 1073741824;
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
    private IIndexSearchParser mIndexSearchParser = null;
    private JankSqlite mJankDBStats = new JankSqlite();
    private final Object mLock = new Object();
    private final ThreadLocal<SQLiteSession> mThreadSession = ThreadLocal.withInitial(new -$$Lambda$RBWjWVyGrOTsQrLCYzJ_G8Uk25Q(this));

    public interface CursorFactory {
        Cursor newCursor(SQLiteDatabase sQLiteDatabase, SQLiteCursorDriver sQLiteCursorDriver, String str, SQLiteQuery sQLiteQuery);
    }

    public interface CustomFunction {
        void callback(String[] strArr);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DatabaseOpenFlags {
    }

    public static final class OpenParams {
        private final CursorFactory mCursorFactory;
        private final DatabaseErrorHandler mErrorHandler;
        private final long mIdleConnectionTimeout;
        private final String mJournalMode;
        private final int mLookasideSlotCount;
        private final int mLookasideSlotSize;
        private final int mOpenFlags;
        private final String mSyncMode;

        public static final class Builder {
            private CursorFactory mCursorFactory;
            private DatabaseErrorHandler mErrorHandler;
            private long mIdleConnectionTimeout = -1;
            private String mJournalMode;
            private int mLookasideSlotCount = -1;
            private int mLookasideSlotSize = -1;
            private int mOpenFlags;
            private String mSyncMode;

            public Builder(OpenParams params) {
                this.mLookasideSlotSize = params.mLookasideSlotSize;
                this.mLookasideSlotCount = params.mLookasideSlotCount;
                this.mOpenFlags = params.mOpenFlags;
                this.mCursorFactory = params.mCursorFactory;
                this.mErrorHandler = params.mErrorHandler;
                this.mJournalMode = params.mJournalMode;
                this.mSyncMode = params.mSyncMode;
            }

            public Builder setLookasideConfig(int slotSize, int slotCount) {
                boolean z = false;
                Preconditions.checkArgument(slotSize >= 0, "lookasideSlotCount cannot be negative");
                Preconditions.checkArgument(slotCount >= 0, "lookasideSlotSize cannot be negative");
                if ((slotSize > 0 && slotCount > 0) || (slotCount == 0 && slotSize == 0)) {
                    z = true;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid configuration: ");
                stringBuilder.append(slotSize);
                stringBuilder.append(", ");
                stringBuilder.append(slotCount);
                Preconditions.checkArgument(z, stringBuilder.toString());
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

            public Builder removeOpenFlags(int openFlags) {
                this.mOpenFlags &= ~openFlags;
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

            public Builder setJournalMode(String journalMode) {
                Preconditions.checkNotNull(journalMode);
                this.mJournalMode = journalMode;
                return this;
            }

            public Builder setSynchronousMode(String syncMode) {
                Preconditions.checkNotNull(syncMode);
                this.mSyncMode = syncMode;
                return this;
            }

            public OpenParams build() {
                return new OpenParams(this.mOpenFlags, this.mCursorFactory, this.mErrorHandler, this.mLookasideSlotSize, this.mLookasideSlotCount, this.mIdleConnectionTimeout, this.mJournalMode, this.mSyncMode, null);
            }
        }

        /* synthetic */ OpenParams(int x0, CursorFactory x1, DatabaseErrorHandler x2, int x3, int x4, long x5, String x6, String x7, AnonymousClass1 x8) {
            this(x0, x1, x2, x3, x4, x5, x6, x7);
        }

        private OpenParams(int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler, int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeout, String journalMode, String syncMode) {
            this.mOpenFlags = openFlags;
            this.mCursorFactory = cursorFactory;
            this.mErrorHandler = errorHandler;
            this.mLookasideSlotSize = lookasideSlotSize;
            this.mLookasideSlotCount = lookasideSlotCount;
            this.mIdleConnectionTimeout = idleConnectionTimeout;
            this.mJournalMode = journalMode;
            this.mSyncMode = syncMode;
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

        public String getJournalMode() {
            return this.mJournalMode;
        }

        public String getSynchronousMode() {
            return this.mSyncMode;
        }

        public Builder toBuilder() {
            return new Builder(this);
        }
    }

    private SQLiteDatabase(String path, int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler, int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeoutMs, String journalMode, String syncMode) {
        String str = path;
        this.mCursorFactory = cursorFactory;
        this.mErrorHandler = errorHandler != null ? errorHandler : new DefaultDatabaseErrorHandler();
        this.mConfigurationLocked = new SQLiteDatabaseConfiguration(str, openFlags);
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
        this.mConfigurationLocked.journalMode = journalMode;
        this.mConfigurationLocked.syncMode = syncMode;
        if (!SQLiteGlobal.isCompatibilityWalSupported() || (SQLiteCompatibilityWalFlags.areFlagsSet() && !SQLiteCompatibilityWalFlags.isCompatibilityWalSupported())) {
            SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration = this.mConfigurationLocked;
            sQLiteDatabaseConfiguration.openFlags |= 1073741824;
        }
        this.jank_dbname = str;
        if (this.jank_dbname.equals("JankEventDb.db")) {
            this.bJankMonitor = false;
        }
        this.mIndexSearchParser = HwFrameworkFactory.getIndexSearchParser();
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
        SQLiteConnectionPool pool;
        synchronized (this.mLock) {
            if (this.mCloseGuardLocked != null) {
                if (finalized) {
                    this.mCloseGuardLocked.warnIfOpen();
                }
                this.mCloseGuardLocked.close();
            }
            pool = this.mConnectionPoolLocked;
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
        return looper != null && looper == Looper.getMainLooper();
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
                if (this.mIndexSearchParser.isValidTable(sqlinfo.getTable())) {
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
        SQLiteDatabase sQLiteDatabase = new SQLiteDatabase(path, openParams.mOpenFlags, openParams.mCursorFactory, openParams.mErrorHandler, openParams.mLookasideSlotSize, openParams.mLookasideSlotCount, openParams.mIdleConnectionTimeout, openParams.mJournalMode, openParams.mSyncMode);
        sQLiteDatabase.open();
        return sQLiteDatabase;
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags, DatabaseErrorHandler errorHandler) {
        SQLiteDatabase db = new SQLiteDatabase(path, flags, factory, errorHandler, -1, -1, -1, null, null);
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
        if (file != null) {
            boolean deleted = false | file.delete();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(file.getPath());
            stringBuilder.append("-journal");
            deleted |= new File(stringBuilder.toString()).delete();
            stringBuilder = new StringBuilder();
            stringBuilder.append(file.getPath());
            stringBuilder.append("-shm");
            deleted |= new File(stringBuilder.toString()).delete();
            stringBuilder = new StringBuilder();
            stringBuilder.append(file.getPath());
            stringBuilder.append("-wal");
            deleted |= new File(stringBuilder.toString()).delete();
            File dir = file.getParentFile();
            if (dir != null) {
                String prefix = new StringBuilder();
                prefix.append(file.getName());
                prefix.append("-mj");
                prefix = prefix.toString();
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
        throw new IllegalArgumentException("file must not be null");
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to open database '");
                stringBuilder.append(getLabel());
                stringBuilder.append("'.");
                Log.e(TAG, stringBuilder.toString(), ex);
                close();
                throw ex;
            }
        }
    }

    private void openInner() {
        synchronized (this.mLock) {
            this.mConnectionPoolLocked = SQLiteConnectionPool.open(this.mConfigurationLocked, this.mEnableExclusiveConnection);
            this.mCloseGuardLocked.open("close");
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PRAGMA user_version = ");
        stringBuilder.append(version);
        execSQL(stringBuilder.toString());
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PRAGMA max_page_count = ");
        stringBuilder.append(numPages);
        return DatabaseUtils.longForQuery(this, stringBuilder.toString(), null) * pageSize;
    }

    public long getPageSize() {
        return DatabaseUtils.longForQuery(this, "PRAGMA page_size;", null);
    }

    public void setPageSize(long numBytes) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PRAGMA page_size = ");
        stringBuilder.append(numBytes);
        execSQL(stringBuilder.toString());
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
        Cursor query;
        try {
            query = new SQLiteDirectCursorDriver(this, sql, editTable, cancellationSignal).query(cursorFactory != null ? cursorFactory : this.mCursorFactory, selectionArgs);
            return query;
        } finally {
            releaseReference();
            query = this.bJankMonitor;
            if (query != null) {
                query = this.mJankDBStats;
                query.addQuery(SystemClock.uptimeMillis() - begin, editTable, this.jank_dbname);
            }
        }
    }

    private Cursor queryForIndexSearch(String table, String whereClause, String[] whereArgs, int operation) {
        StringBuilder sql = new StringBuilder();
        if ("files".equals(table)) {
            sql.append("SELECT _id FROM ");
            sql.append(table);
            sql.append(" WHERE ");
            sql.append(whereClause);
            sql.append(" AND ");
            sql.append("((mime_type='text/plain') OR (mime_type='text/html') OR (mime_type='text/htm') OR (mime_type = 'application/msword') OR (mime_type = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') OR (mime_type = 'application/vnd.ms-excel') OR (mime_type = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') OR (mime_type = 'application/mspowerpoint') OR (mime_type = 'application/vnd.openxmlformats-officedocument.presentationml.presentation')) ");
        } else if ("Body".equals(table)) {
            sql.append("SELECT messageKey FROM Body WHERE ");
            sql.append(whereClause);
        } else if ("Events".equals(table)) {
            sql.append("SELECT _id FROM ");
            sql.append(table);
            sql.append(" WHERE ");
            sql.append(whereClause);
            sql.append(" AND ");
            sql.append("mutators IS NOT 'com.android.providers.contacts'");
        } else if ("Calendars".equals(table)) {
            if (whereArgs == null || whereArgs.length != 1) {
                sql.append("SELECT _id FROM ");
                sql.append(table);
                sql.append(" WHERE ");
                sql.append(whereClause);
            } else {
                sql.append("SELECT _id FROM Events WHERE calendar_id IN (?)");
            }
        } else if ("Mailbox".equals(table)) {
            if (2 != operation) {
                return null;
            }
            if (whereClause == null) {
                sql.append("SELECT _id FROM Message");
            } else {
                sql.append("SELECT _id FROM Message WHERE mailboxKey in (select _id FROM Mailbox WHERE ");
                sql.append(whereClause);
                sql.append(")");
            }
        } else if (!"fav_sms".equals(table)) {
            sql.append("SELECT _id FROM ");
            sql.append(table);
            sql.append(" WHERE ");
            sql.append(whereClause);
        } else if (whereClause == null) {
            sql.append("SELECT _id FROM words WHERE table_to_use IS 8");
        } else {
            sql.append("SELECT _id FROM words WHERE source_id in (select _id FROM fav_sms WHERE ");
            sql.append(whereClause);
            sql.append(" ) AND table_to_use IS 8");
        }
        return rawQuery(sql.toString(), whereArgs);
    }

    /* JADX WARNING: Missing block: B:39:0x0088, code skipped:
            if (r0 == null) goto L_0x0094;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void triggerUpdatingOrDeletingIndex(String table, String whereClause, String[] whereArgs, int operation) {
        if (this.mIndexSearchParser != null && this.mIndexSearchParser.isValidTable(table)) {
            Cursor c = null;
            try {
                c = queryForIndexSearch(table, whereClause, whereArgs, operation);
                if (c != null) {
                    if (c.getCount() != 0) {
                        if (getThreadSession().hasTransaction()) {
                            while (c.moveToNext()) {
                                String realTable;
                                if (table.equals("Body")) {
                                    realTable = "Message";
                                } else {
                                    realTable = table;
                                }
                                getThreadSession().insertTransMap(realTable, c.getLong(0), operation);
                            }
                        } else {
                            List<Long> ids = new ArrayList();
                            while (c.moveToNext()) {
                                ids.add(Long.valueOf(c.getLong(0)));
                            }
                            this.mIndexSearchParser.notifyIndexSearchService(ids, operation);
                        }
                        if (c != null) {
                            c.close();
                        }
                    }
                }
                Log.v(TAG, "triggerBuildingIndex(): cursor is null or count is 0, return.");
                if (c != null) {
                    c.close();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "triggerUpdatingOrDeletingIndex(): RuntimeException");
            } catch (Exception e2) {
                Log.e(TAG, "triggerUpdatingOrDeletingIndex(): Exception");
                if (c != null) {
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    /* JADX WARNING: Failed to extract finally block: empty outs */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void triggerDeletingCalendarAccounts(String sql, Object[] bindArgs) {
        if (this.mIndexSearchParser != null && "DELETE FROM Calendars WHERE account_name=? AND account_type=?".equals(sql)) {
            Cursor c1 = null;
            Cursor c2 = null;
            int i = 0;
            List<Long> eventsIds = new ArrayList();
            try {
                c1 = rawQuery("SELECT _id FROM Calendars where account_name=? AND account_type=?", (String[]) bindArgs);
                if (c1 != null && c1.getCount() > 0) {
                    String[] calendarIds = new String[c1.getCount()];
                    while (c1.moveToNext()) {
                        calendarIds[i] = c1.getString(0);
                        i++;
                    }
                    c2 = queryForIndexSearch("Events", "calendar_id IN (?)", calendarIds, 2);
                    if (c2 != null && c2.getCount() > 0) {
                        while (c2.moveToNext()) {
                            eventsIds.add(Long.valueOf(c2.getLong(0)));
                        }
                        this.mIndexSearchParser.notifyIndexSearchService(eventsIds, 2);
                    }
                }
                if (c1 != null) {
                    c1.close();
                }
                if (c2 != null) {
                    c2.close();
                }
                if (eventsIds.isEmpty()) {
                    return;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "triggerDeletingCalendarAccounts(): RuntimeException");
                if (c1 != null) {
                    c1.close();
                }
                if (c2 != null) {
                    c2.close();
                }
                if (eventsIds.isEmpty()) {
                    return;
                }
            } catch (Throwable th) {
                if (c1 != null) {
                    c1.close();
                }
                if (c2 != null) {
                    c2.close();
                }
                if (!eventsIds.isEmpty()) {
                    eventsIds.clear();
                }
                throw th;
            }
            eventsIds.clear();
        }
    }

    private void triggerAddingIndex(String table, long id) {
        if (id < 0) {
            Log.v(TAG, "triggerBuildingIndex(): invalid id, return.");
            return;
        }
        if (this.mIndexSearchParser != null && this.mIndexSearchParser.isValidTable(table)) {
            if (table.equals("Events")) {
                Cursor c = null;
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SELECT _id FROM Events WHERE _id = ");
                    stringBuilder.append(id);
                    stringBuilder.append(" AND mutators IS NOT 'com.android.providers.contacts'");
                    c = rawQuery(stringBuilder.toString(), null);
                    if (c == null || c.getCount() == 0) {
                        if (c != null) {
                            c.close();
                        }
                        return;
                    }
                } finally {
                    if (c != null) {
                        c.close();
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

    public long insert(String table, String nullColumnHack, ContentValues values) {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, 0);
        } catch (SQLException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error inserting ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error inserting ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, initialValues, 5);
    }

    /* JADX WARNING: Removed duplicated region for block: B:51:0x00e5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) {
        Throwable th;
        String str = table;
        ContentValues contentValues = initialValues;
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        String str2;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT");
            sql.append(CONFLICT_VALUES[conflictAlgorithm]);
            sql.append(" INTO ");
            sql.append(str);
            sql.append('(');
            Object[] bindArgs = null;
            int size = (contentValues == null || initialValues.isEmpty()) ? 0 : initialValues.size();
            if (size > 0) {
                bindArgs = new Object[size];
                int i = 0;
                for (String colName : initialValues.keySet()) {
                    sql.append(i > 0 ? "," : "");
                    sql.append(colName);
                    int i2 = i + 1;
                    bindArgs[i] = contentValues.get(colName);
                    i = i2;
                }
                sql.append(')');
                sql.append(" VALUES (");
                i = 0;
                while (i < size) {
                    sql.append(i > 0 ? ",?" : "?");
                    i++;
                }
                str2 = nullColumnHack;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    stringBuilder.append(nullColumnHack);
                    stringBuilder.append(") VALUES (NULL");
                    sql.append(stringBuilder.toString());
                } catch (Throwable th2) {
                    th = th2;
                    releaseReference();
                    if (this.bJankMonitor) {
                        this.mJankDBStats.addInsert(SystemClock.uptimeMillis() - begin, str, this.jank_dbname);
                    }
                    throw th;
                }
            }
            Object[] bindArgs2 = bindArgs;
            sql.append(')');
            SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs2);
            long id = statement.executeInsert();
            triggerAddingIndex(str, id);
            statement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addInsert(SystemClock.uptimeMillis() - begin, str, this.jank_dbname);
            }
            return id;
        } catch (Throwable th3) {
            th = th3;
            str2 = nullColumnHack;
            releaseReference();
            if (this.bJankMonitor) {
            }
            throw th;
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        SQLiteStatement statement;
        try {
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DELETE FROM ");
            stringBuilder.append(table);
            if (TextUtils.isEmpty(whereClause)) {
                str = "";
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" WHERE ");
                stringBuilder2.append(whereClause);
                str = stringBuilder2.toString();
            }
            stringBuilder.append(str);
            statement = new SQLiteStatement(this, stringBuilder.toString(), whereArgs);
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

    /* JADX WARNING: Removed duplicated region for block: B:52:0x00db  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs, int conflictAlgorithm) {
        Throwable th;
        String str = table;
        ContentValues contentValues = values;
        String[] strArr = whereArgs;
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        String str2;
        if (contentValues == null || values.isEmpty()) {
            str2 = whereClause;
            throw new IllegalArgumentException("Empty values");
        }
        acquireReference();
        try {
            StringBuilder sql = new StringBuilder(120);
            sql.append("UPDATE ");
            sql.append(CONFLICT_VALUES[conflictAlgorithm]);
            sql.append(str);
            sql.append(" SET ");
            int setValuesSize = values.size();
            int bindArgsSize = strArr == null ? setValuesSize : strArr.length + setValuesSize;
            Object[] bindArgs = new Object[bindArgsSize];
            int i = 0;
            for (String str22 : values.keySet()) {
                sql.append(i > 0 ? "," : "");
                sql.append(str22);
                int i2 = i + 1;
                bindArgs[i] = contentValues.get(str22);
                sql.append("=?");
                i = i2;
            }
            if (strArr != null) {
                i = setValuesSize;
                while (i < bindArgsSize) {
                    bindArgs[i] = strArr[i - setValuesSize];
                    i++;
                }
            }
            if (TextUtils.isEmpty(whereClause)) {
                str22 = whereClause;
            } else {
                sql.append(" WHERE ");
                try {
                    sql.append(whereClause);
                } catch (Throwable th2) {
                    th = th2;
                    releaseReference();
                    if (this.bJankMonitor) {
                    }
                    throw th;
                }
            }
            SQLiteStatement statement = new SQLiteStatement(this, sql.toString(), bindArgs);
            i = statement.executeUpdateDelete();
            statement.close();
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addUpdate(SystemClock.uptimeMillis() - begin, str, this.jank_dbname);
            }
            return i;
        } catch (Throwable th3) {
            th = th3;
            str22 = whereClause;
            releaseReference();
            if (this.bJankMonitor) {
                this.mJankDBStats.addUpdate(SystemClock.uptimeMillis() - begin, str, this.jank_dbname);
            }
            throw th;
        }
    }

    public void execSQL(String sql) throws SQLException {
        executeSql(sql, null);
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        if (bindArgs != null) {
            triggerDeletingCalendarAccounts(sql, bindArgs);
            executeSql(sql, bindArgs);
            return;
        }
        throw new IllegalArgumentException("Empty bindArgs");
    }

    public int executeSql(String sql, Object[] bindArgs) throws SQLException {
        long begin = 0;
        if (this.bJankMonitor) {
            begin = SystemClock.uptimeMillis();
        }
        acquireReference();
        try {
            int statementType = DatabaseUtils.getSqlStatementType(sql);
            if (statementType == 3) {
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
            SQLiteStatement statement;
            try {
                statement = new SQLiteStatement(this, sql, bindArgs);
                int executeUpdateDelete = statement.executeUpdateDelete();
                statement.close();
                if (statementType == 8) {
                    this.mConnectionPoolLocked.closeAvailableNonPrimaryConnectionsAndLogExceptions();
                }
                releaseReference();
                if (this.bJankMonitor) {
                    this.mJankDBStats.addExecsql(SystemClock.uptimeMillis() - begin, "", this.jank_dbname);
                }
                return executeUpdateDelete;
            } catch (Throwable th) {
                if (statementType == 8) {
                    this.mConnectionPoolLocked.closeAvailableNonPrimaryConnectionsAndLogExceptions();
                }
            }
        } catch (Throwable th2) {
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
        if (locale != null) {
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
            return;
        }
        throw new IllegalArgumentException("locale must not be null.");
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

    /* JADX WARNING: Missing block: B:30:0x0077, code skipped:
            return false;
     */
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
                sQLiteDatabaseConfiguration.openFlags = 536870912 | sQLiteDatabaseConfiguration.openFlags;
                if (this.mConfigurationLocked.configurationEnhancement) {
                    this.mConfigurationLocked.defaultWALEnabled = false;
                    this.mConfigurationLocked.explicitWALEnabled = true;
                }
                try {
                    this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
                    return true;
                } catch (RuntimeException ex) {
                    SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration2 = this.mConfigurationLocked;
                    sQLiteDatabaseConfiguration2.openFlags &= -536870913;
                    if (this.mConfigurationLocked.configurationEnhancement) {
                        this.mConfigurationLocked.defaultWALEnabled = defaultWALEnabled;
                        this.mConfigurationLocked.explicitWALEnabled = explicitWALEnabled;
                    }
                    throw ex;
                }
            } else if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("this database: ");
                stringBuilder.append(this.mConfigurationLocked.label);
                stringBuilder.append(" has attached databases. can't  enable WAL.");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public void disableWriteAheadLogging() {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            boolean defaultWALEnabled = this.mConfigurationLocked.defaultWALEnabled;
            boolean explicitWALEnabled = this.mConfigurationLocked.explicitWALEnabled;
            int oldFlags = this.mConfigurationLocked.openFlags;
            boolean walDisabled = (536870912 & oldFlags) == 0;
            boolean compatibilityWalDisabled = (oldFlags & 1073741824) != 0;
            if (walDisabled && compatibilityWalDisabled) {
                return;
            }
            SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration = this.mConfigurationLocked;
            sQLiteDatabaseConfiguration.openFlags &= -536870913;
            if (this.mConfigurationLocked.configurationEnhancement) {
                this.mConfigurationLocked.defaultWALEnabled = false;
                this.mConfigurationLocked.explicitWALEnabled = false;
            }
            SQLiteDatabaseConfiguration sQLiteDatabaseConfiguration2 = this.mConfigurationLocked;
            sQLiteDatabaseConfiguration2.openFlags = 1073741824 | sQLiteDatabaseConfiguration2.openFlags;
            try {
                this.mConnectionPoolLocked.reconfigure(this.mConfigurationLocked);
            } catch (RuntimeException ex) {
                this.mConfigurationLocked.openFlags = oldFlags;
                if (this.mConfigurationLocked.configurationEnhancement) {
                    this.mConfigurationLocked.defaultWALEnabled = defaultWALEnabled;
                    this.mConfigurationLocked.explicitWALEnabled = explicitWALEnabled;
                }
                throw ex;
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0020, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:16:0x002b, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isWriteAheadLoggingEnabled() {
        synchronized (this.mLock) {
            throwIfNotOpenLocked();
            boolean z = false;
            if (this.mConfigurationLocked.configurationEnhancement) {
                if ((this.mConfigurationLocked.openFlags & 536870912) != 0 && this.mConfigurationLocked.explicitWALEnabled) {
                    z = true;
                }
            } else if ((this.mConfigurationLocked.openFlags & 536870912) != 0) {
                z = true;
            }
        }
    }

    static ArrayList<DbStats> getDbStats() {
        ArrayList<DbStats> dbStatsList = new ArrayList();
        Iterator it = getActiveDatabases().iterator();
        while (it.hasNext()) {
            ((SQLiteDatabase) it.next()).collectDbStats(dbStatsList);
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
        Iterator it = getActiveDatabases().iterator();
        while (it.hasNext()) {
            ((SQLiteDatabase) it.next()).dump(printer, verbose);
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

    /* JADX WARNING: Missing block: B:14:0x0028, code skipped:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            r1 = rawQuery("pragma database_list;", null);
     */
    /* JADX WARNING: Missing block: B:18:0x0035, code skipped:
            if (r1.moveToNext() == false) goto L_0x004a;
     */
    /* JADX WARNING: Missing block: B:19:0x0037, code skipped:
            r0.add(new android.util.Pair(r1.getString(1), r1.getString(2)));
     */
    /* JADX WARNING: Missing block: B:21:0x004a, code skipped:
            if (r1 == null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:24:0x004f, code skipped:
            releaseReference();
     */
    /* JADX WARNING: Missing block: B:25:0x0053, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:27:0x0055, code skipped:
            if (r1 != null) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:29:?, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:33:0x005e, code skipped:
            releaseReference();
     */
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
        acquireReference();
        List<Pair<String, String>> attachedDbs = null;
        SQLiteStatement prog;
        try {
            attachedDbs = getAttachedDbs();
            if (attachedDbs != null) {
                int i = 0;
                while (i < attachedDbs.size()) {
                    Pair<String, String> p = (Pair) attachedDbs.get(i);
                    prog = null;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PRAGMA ");
                    stringBuilder.append((String) p.first);
                    stringBuilder.append(".integrity_check(1);");
                    prog = compileStatement(stringBuilder.toString());
                    String rslt = prog.simpleQueryForString();
                    if (rslt.equalsIgnoreCase("ok")) {
                        if (prog != null) {
                            prog.close();
                        }
                        i++;
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("PRAGMA integrity_check on ");
                        stringBuilder2.append((String) p.second);
                        stringBuilder2.append(" returned: ");
                        stringBuilder2.append(rslt);
                        Log.e(str, stringBuilder2.toString());
                        if (prog != null) {
                            prog.close();
                        }
                        releaseReference();
                        return false;
                    }
                }
                releaseReference();
                return true;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("databaselist for: ");
            stringBuilder3.append(getPath());
            stringBuilder3.append(" couldn't be retrieved. probably because the database is closed");
            throw new IllegalStateException(stringBuilder3.toString());
        } catch (SQLiteException e) {
            attachedDbs = new ArrayList();
            attachedDbs.add(new Pair("main", getPath()));
        } catch (Throwable th) {
            releaseReference();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLiteDatabase: ");
        stringBuilder.append(getPath());
        return stringBuilder.toString();
    }

    private void throwIfNotOpenLocked() {
        if (this.mConnectionPoolLocked == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The database '");
            stringBuilder.append(this.mConfigurationLocked.label);
            stringBuilder.append("' is not open.");
            throw new IllegalStateException(stringBuilder.toString());
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
