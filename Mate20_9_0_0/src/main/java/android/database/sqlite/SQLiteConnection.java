package android.database.sqlite;

import android.database.CursorWindow;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDebug.DbStats;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.LruCache;
import android.util.Printer;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public final class SQLiteConnection implements OnCancelListener {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean DEBUG = false;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String TAG = "SQLiteConnection";
    private static final boolean enableSqlAudit = SystemProperties.getBoolean("persist.sys.sql_audit_enable", false);
    private SQLiteAuditLog mAuditLog;
    private int mCancellationSignalAttachCount;
    private final CloseGuard mCloseGuard;
    private final SQLiteDatabaseConfiguration mConfiguration;
    private final int mConnectionId;
    private long mConnectionPtr;
    private final boolean mIsExclusiveConnection;
    private final boolean mIsPrimaryConnection;
    private final boolean mIsReadOnlyConnection;
    private boolean mOnlyAllowReadOnlyOperations;
    private final SQLiteConnectionPool mPool;
    private final PreparedStatementCache mPreparedStatementCache;
    private PreparedStatement mPreparedStatementPool;
    private final OperationLog mRecentOperations;

    private static final class Operation {
        private static final int MAX_TRACE_METHOD_NAME_LEN = 256;
        public ArrayList<Object> mBindArgs;
        public int mCookie;
        public long mEndTime;
        public Exception mException;
        public boolean mFinished;
        public String mKind;
        public String mSql;
        public long mStartTime;
        public long mStartWallTime;

        private Operation() {
        }

        public void describe(StringBuilder msg, boolean verbose) {
            msg.append(this.mKind);
            if (this.mFinished) {
                msg.append(" took ");
                msg.append(this.mEndTime - this.mStartTime);
                msg.append("ms");
            } else {
                msg.append(" started ");
                msg.append(System.currentTimeMillis() - this.mStartWallTime);
                msg.append("ms ago");
            }
            msg.append(" - ");
            msg.append(getStatus());
            if (this.mSql != null) {
                msg.append(", sql=\"");
                msg.append(SQLiteConnection.trimSqlForDisplay(this.mSql));
                msg.append("\"");
            }
            if (!(!verbose || this.mBindArgs == null || this.mBindArgs.size() == 0)) {
                msg.append(", bindArgs=[");
                int count = this.mBindArgs.size();
                for (int i = 0; i < count; i++) {
                    Object arg = this.mBindArgs.get(i);
                    if (i != 0) {
                        msg.append(", ");
                    }
                    if (arg == null) {
                        msg.append("null");
                    } else if (arg instanceof byte[]) {
                        msg.append("<byte[]>");
                    } else if (arg instanceof String) {
                        msg.append("\"");
                        msg.append((String) arg);
                        msg.append("\"");
                    } else {
                        msg.append(arg);
                    }
                }
                msg.append("]");
            }
            if (this.mException != null) {
                msg.append(", exception=\"");
                msg.append(this.mException.getMessage());
                msg.append("\"");
            }
        }

        private String getStatus() {
            if (!this.mFinished) {
                return "running";
            }
            return this.mException != null ? "failed" : "succeeded";
        }

        private String getTraceMethodName() {
            String methodName = new StringBuilder();
            methodName.append(this.mKind);
            methodName.append(" ");
            methodName.append(this.mSql);
            methodName = methodName.toString();
            if (methodName.length() > 256) {
                return methodName.substring(0, 256);
            }
            return methodName;
        }
    }

    private static final class OperationLog {
        private static final int COOKIE_GENERATION_SHIFT = 8;
        private static final int COOKIE_INDEX_MASK = 255;
        private static final int MAX_RECENT_OPERATIONS = 20;
        private int mGeneration;
        private int mIndex;
        private final Operation[] mOperations = new Operation[20];
        private final SQLiteConnectionPool mPool;

        OperationLog(SQLiteConnectionPool pool) {
            this.mPool = pool;
        }

        public int beginOperation(String kind, String sql, Object[] bindArgs) {
            int i;
            synchronized (this.mOperations) {
                int index = (this.mIndex + 1) % 20;
                Operation operation = this.mOperations[index];
                i = 0;
                if (operation == null) {
                    operation = new Operation();
                    this.mOperations[index] = operation;
                } else {
                    operation.mFinished = false;
                    operation.mException = null;
                    if (operation.mBindArgs != null) {
                        operation.mBindArgs.clear();
                    }
                }
                operation.mStartWallTime = System.currentTimeMillis();
                operation.mStartTime = SystemClock.uptimeMillis();
                operation.mKind = kind;
                operation.mSql = sql;
                if (bindArgs != null) {
                    if (operation.mBindArgs == null) {
                        operation.mBindArgs = new ArrayList();
                    } else {
                        operation.mBindArgs.clear();
                    }
                    while (i < bindArgs.length) {
                        Object arg = bindArgs[i];
                        if (arg == null || !(arg instanceof byte[])) {
                            operation.mBindArgs.add(arg);
                        } else {
                            operation.mBindArgs.add(SQLiteConnection.EMPTY_BYTE_ARRAY);
                        }
                        i++;
                    }
                }
                operation.mCookie = newOperationCookieLocked(index);
                if (Trace.isTagEnabled(1048576)) {
                    Trace.asyncTraceBegin(1048576, operation.getTraceMethodName(), operation.mCookie);
                }
                this.mIndex = index;
                i = operation.mCookie;
            }
            return i;
        }

        public void failOperation(int cookie, Exception ex) {
            synchronized (this.mOperations) {
                Operation operation = getOperationLocked(cookie);
                if (operation != null) {
                    operation.mException = ex;
                }
            }
        }

        public void endOperation(int cookie) {
            synchronized (this.mOperations) {
                if (endOperationDeferLogLocked(cookie)) {
                    logOperationLocked(cookie, null);
                }
            }
        }

        public boolean endOperationDeferLog(int cookie) {
            boolean endOperationDeferLogLocked;
            synchronized (this.mOperations) {
                endOperationDeferLogLocked = endOperationDeferLogLocked(cookie);
            }
            return endOperationDeferLogLocked;
        }

        public void logOperation(int cookie, String detail) {
            synchronized (this.mOperations) {
                logOperationLocked(cookie, detail);
            }
        }

        private boolean endOperationDeferLogLocked(int cookie) {
            Operation operation = getOperationLocked(cookie);
            boolean z = false;
            if (operation == null) {
                return false;
            }
            if (Trace.isTagEnabled(1048576)) {
                Trace.asyncTraceEnd(1048576, operation.getTraceMethodName(), operation.mCookie);
            }
            operation.mEndTime = SystemClock.uptimeMillis();
            operation.mFinished = true;
            long execTime = operation.mEndTime - operation.mStartTime;
            this.mPool.onStatementExecuted(execTime);
            if (SQLiteDebug.DEBUG_LOG_SLOW_QUERIES && SQLiteDebug.shouldLogSlowQuery(execTime)) {
                z = true;
            }
            return z;
        }

        private void logOperationLocked(int cookie, String detail) {
            Operation operation = getOperationLocked(cookie);
            StringBuilder msg = new StringBuilder();
            operation.describe(msg, false);
            if (detail != null) {
                msg.append(", ");
                msg.append(detail);
            }
            Log.d(SQLiteConnection.TAG, msg.toString());
        }

        private int newOperationCookieLocked(int index) {
            int generation = this.mGeneration;
            this.mGeneration = generation + 1;
            return (generation << 8) | index;
        }

        private Operation getOperationLocked(int cookie) {
            Operation operation = this.mOperations[cookie & 255];
            return operation.mCookie == cookie ? operation : null;
        }

        public String describeCurrentOperation() {
            synchronized (this.mOperations) {
                Operation operation = this.mOperations[this.mIndex];
                if (operation == null || operation.mFinished) {
                    return null;
                }
                StringBuilder msg = new StringBuilder();
                operation.describe(msg, false);
                String stringBuilder = msg.toString();
                return stringBuilder;
            }
        }

        public void dump(Printer printer, boolean verbose) {
            synchronized (this.mOperations) {
                printer.println("  Most recently executed operations:");
                int index = this.mIndex;
                Operation operation = this.mOperations[index];
                if (operation != null) {
                    SimpleDateFormat opDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    int n = 0;
                    do {
                        StringBuilder msg = new StringBuilder();
                        msg.append("    ");
                        msg.append(n);
                        msg.append(": [");
                        msg.append(opDF.format(new Date(operation.mStartWallTime)));
                        msg.append("] ");
                        operation.describe(msg, verbose);
                        printer.println(msg.toString());
                        if (index > 0) {
                            index--;
                        } else {
                            index = 19;
                        }
                        n++;
                        operation = this.mOperations[index];
                        if (operation == null) {
                            break;
                        }
                    } while (n < 20);
                } else {
                    printer.println("    <none>");
                }
            }
        }
    }

    private static final class PreparedStatement {
        public boolean mInCache;
        public boolean mInUse;
        public int mNumParameters;
        public PreparedStatement mPoolNext;
        public boolean mReadOnly;
        public String mSql;
        public long mStatementPtr;
        public int mType;

        private PreparedStatement() {
        }
    }

    private final class PreparedStatementCache extends LruCache<String, PreparedStatement> {
        public PreparedStatementCache(int size) {
            super(size);
        }

        protected void entryRemoved(boolean evicted, String key, PreparedStatement oldValue, PreparedStatement newValue) {
            oldValue.mInCache = false;
            if (!oldValue.mInUse) {
                SQLiteConnection.this.finalizePreparedStatement(oldValue);
            }
        }

        public void dump(Printer printer) {
            printer.println("  Prepared statement cache:");
            Map<String, PreparedStatement> cache = snapshot();
            if (cache.isEmpty()) {
                printer.println("    <none>");
                return;
            }
            int i = 0;
            for (Entry<String, PreparedStatement> entry : cache.entrySet()) {
                PreparedStatement statement = (PreparedStatement) entry.getValue();
                if (statement.mInCache) {
                    String sql = (String) entry.getKey();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("    ");
                    stringBuilder.append(i);
                    stringBuilder.append(": statementPtr=0x");
                    stringBuilder.append(Long.toHexString(statement.mStatementPtr));
                    stringBuilder.append(", numParameters=");
                    stringBuilder.append(statement.mNumParameters);
                    stringBuilder.append(", type=");
                    stringBuilder.append(statement.mType);
                    stringBuilder.append(", readOnly=");
                    stringBuilder.append(statement.mReadOnly);
                    stringBuilder.append(", sql=\"");
                    stringBuilder.append(SQLiteConnection.trimSqlForDisplay(sql));
                    stringBuilder.append("\"");
                    printer.println(stringBuilder.toString());
                }
                i++;
            }
        }
    }

    private static native void nativeBindBlob(long j, long j2, int i, byte[] bArr);

    private static native void nativeBindDouble(long j, long j2, int i, double d);

    private static native void nativeBindLong(long j, long j2, int i, long j3);

    private static native void nativeBindNull(long j, long j2, int i);

    private static native void nativeBindString(long j, long j2, int i, String str);

    private static native void nativeCancel(long j);

    private static native void nativeClose(long j);

    private static native void nativeExecute(long j, long j2);

    private static native int nativeExecuteForBlobFileDescriptor(long j, long j2);

    private static native int nativeExecuteForChangedRowCount(long j, long j2);

    private static native long nativeExecuteForCursorWindow(long j, long j2, long j3, int i, int i2, boolean z);

    private static native long nativeExecuteForLastInsertedRowId(long j, long j2);

    private static native long nativeExecuteForLong(long j, long j2);

    private static native String nativeExecuteForString(long j, long j2);

    private static native void nativeFinalizeStatement(long j, long j2);

    private static native int nativeGetColumnCount(long j, long j2);

    private static native String nativeGetColumnName(long j, long j2, int i);

    private static native int nativeGetDbLookaside(long j);

    private static native int nativeGetParameterCount(long j, long j2);

    private static native boolean nativeIsReadOnly(long j, long j2);

    private static native long nativeOpen(String str, int i, String str2, boolean z, boolean z2, int i2, int i3);

    private static native long nativePrepareStatement(long j, String str);

    private static native void nativeRegisterCustomFunction(long j, SQLiteCustomFunction sQLiteCustomFunction);

    private static native void nativeRegisterLocalizedCollators(long j, String str);

    private static native void nativeResetCancel(long j, boolean z);

    private static native void nativeResetStatementAndClearBindings(long j, long j2);

    private SQLiteConnection(SQLiteConnectionPool pool, SQLiteDatabaseConfiguration configuration, int connectionId, boolean primaryConnection) {
        this(pool, configuration, connectionId, primaryConnection, false);
    }

    private SQLiteConnection(SQLiteConnectionPool pool, SQLiteDatabaseConfiguration configuration, int connectionId, boolean primaryConnection, boolean exclusiveConnection) {
        this.mCloseGuard = CloseGuard.get();
        this.mAuditLog = null;
        this.mPool = pool;
        this.mRecentOperations = new OperationLog(this.mPool);
        this.mConfiguration = new SQLiteDatabaseConfiguration(configuration);
        this.mConnectionId = connectionId;
        this.mIsPrimaryConnection = primaryConnection;
        boolean z = true;
        if ((configuration.openFlags & 1) == 0) {
            z = false;
        }
        this.mIsReadOnlyConnection = z;
        this.mPreparedStatementCache = new PreparedStatementCache(this.mConfiguration.maxSqlCacheSize);
        this.mIsExclusiveConnection = exclusiveConnection;
        this.mCloseGuard.open("close");
    }

    protected void finalize() throws Throwable {
        try {
            if (!(this.mPool == null || this.mConnectionPtr == 0)) {
                this.mPool.onConnectionLeaked();
            }
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    static SQLiteConnection open(SQLiteConnectionPool pool, SQLiteDatabaseConfiguration configuration, int connectionId, boolean primaryConnection) {
        SQLiteConnection connection = new SQLiteConnection(pool, configuration, connectionId, primaryConnection);
        try {
            connection.open();
            return connection;
        } catch (SQLiteException ex) {
            connection.dispose(false);
            throw ex;
        }
    }

    void close() {
        dispose(false);
    }

    private void open() {
        this.mConnectionPtr = nativeOpen(this.mConfiguration.path, this.mConfiguration.openFlags, this.mConfiguration.label, SQLiteDebug.DEBUG_SQL_STATEMENTS, SQLiteDebug.DEBUG_SQL_TIME, this.mConfiguration.lookasideSlotSize, this.mConfiguration.lookasideSlotCount);
        setPageSize();
        setForeignKeyModeFromConfiguration();
        setWalModeFromConfiguration();
        setJournalSizeLimit();
        setAutoCheckpointInterval();
        setLocaleFromConfiguration();
        int functionCount = this.mConfiguration.customFunctions.size();
        for (int i = 0; i < functionCount; i++) {
            nativeRegisterCustomFunction(this.mConnectionPtr, (SQLiteCustomFunction) this.mConfiguration.customFunctions.get(i));
        }
        if (enableSqlAudit && this.mAuditLog == null) {
            this.mAuditLog = new SQLiteAuditLog(this.mConfiguration.path, this.mConnectionPtr);
        }
        if (this.mAuditLog != null) {
            this.mAuditLog.enableAudit();
        }
    }

    private void dispose(boolean finalized) {
        if (this.mCloseGuard != null) {
            if (finalized) {
                this.mCloseGuard.warnIfOpen();
            }
            this.mCloseGuard.close();
        }
        if (this.mConnectionPtr != 0) {
            int cookie = this.mRecentOperations.beginOperation("close", null, null);
            try {
                this.mPreparedStatementCache.evictAll();
                if (this.mAuditLog != null) {
                    this.mAuditLog.disableAudit();
                }
                nativeClose(this.mConnectionPtr);
                this.mConnectionPtr = 0;
            } finally {
                this.mRecentOperations.endOperation(cookie);
            }
        }
    }

    private void setPageSize() {
        if (!this.mConfiguration.isInMemoryDb() && !this.mIsReadOnlyConnection) {
            long newValue = (long) SQLiteGlobal.getDefaultPageSize();
            if (executeForLong("PRAGMA page_size", null, null) != newValue) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PRAGMA page_size=");
                stringBuilder.append(newValue);
                execute(stringBuilder.toString(), null, null);
            }
        }
    }

    private void setAutoCheckpointInterval() {
        if (!this.mConfiguration.isInMemoryDb() && !this.mIsReadOnlyConnection) {
            long newValue = (long) SQLiteGlobal.getWALAutoCheckpoint();
            if (executeForLong("PRAGMA wal_autocheckpoint", null, null) != newValue) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PRAGMA wal_autocheckpoint=");
                stringBuilder.append(newValue);
                executeForLong(stringBuilder.toString(), null, null);
            }
        }
    }

    private void setJournalSizeLimit() {
        if (!this.mConfiguration.isInMemoryDb() && !this.mIsReadOnlyConnection) {
            long newValue = (long) SQLiteGlobal.getJournalSizeLimit();
            if (executeForLong("PRAGMA journal_size_limit", null, null) != newValue) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PRAGMA journal_size_limit=");
                stringBuilder.append(newValue);
                executeForLong(stringBuilder.toString(), null, null);
            }
        }
    }

    private void setForeignKeyModeFromConfiguration() {
        if (!this.mIsReadOnlyConnection) {
            long newValue = this.mConfiguration.foreignKeyConstraintsEnabled ? 1 : 0;
            if (executeForLong("PRAGMA foreign_keys", null, null) != newValue) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PRAGMA foreign_keys=");
                stringBuilder.append(newValue);
                execute(stringBuilder.toString(), null, null);
            }
        }
    }

    private void setWalModeFromConfiguration() {
        if (!this.mConfiguration.isInMemoryDb() && !this.mIsReadOnlyConnection) {
            boolean walEnabled = (this.mConfiguration.openFlags & 536870912) != 0;
            boolean useCompatibilityWal = this.mConfiguration.useCompatibilityWal();
            if (walEnabled || useCompatibilityWal) {
                setJournalMode("WAL");
                String sWALSyncMode;
                if (this.mConfiguration.syncMode != null) {
                    setSyncMode(this.mConfiguration.syncMode);
                    return;
                } else if (useCompatibilityWal && SQLiteCompatibilityWalFlags.areFlagsSet()) {
                    sWALSyncMode = SQLiteCompatibilityWalFlags.getWALSyncMode();
                    if (!this.mConfiguration.configurationEnhancement || sWALSyncMode.equalsIgnoreCase("NORMAL")) {
                        setSyncMode(sWALSyncMode);
                        return;
                    } else {
                        setSyncMode("NORMAL");
                        return;
                    }
                } else {
                    sWALSyncMode = SQLiteGlobal.getWALSyncMode();
                    if (!this.mConfiguration.configurationEnhancement || sWALSyncMode.equalsIgnoreCase("NORMAL")) {
                        setSyncMode(sWALSyncMode);
                        return;
                    } else {
                        setSyncMode("NORMAL");
                        return;
                    }
                }
            }
            setJournalMode(this.mConfiguration.journalMode == null ? SQLiteGlobal.getDefaultJournalMode() : this.mConfiguration.journalMode);
            setSyncMode(this.mConfiguration.syncMode == null ? SQLiteGlobal.getDefaultSyncMode() : this.mConfiguration.syncMode);
        }
    }

    private void setSyncMode(String newValue) {
        if (!canonicalizeSyncMode(executeForString("PRAGMA synchronous", null, null)).equalsIgnoreCase(canonicalizeSyncMode(newValue))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PRAGMA synchronous=");
            stringBuilder.append(newValue);
            execute(stringBuilder.toString(), null, null);
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String canonicalizeSyncMode(String value) {
        Object obj;
        switch (value.hashCode()) {
            case 48:
                if (value.equals("0")) {
                    obj = null;
                    break;
                }
            case 49:
                if (value.equals("1")) {
                    obj = 1;
                    break;
                }
            case 50:
                if (value.equals("2")) {
                    obj = 2;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                return "OFF";
            case 1:
                return "NORMAL";
            case 2:
                return "FULL";
            default:
                return value;
        }
    }

    private void setJournalMode(String newValue) {
        String value = executeForString("PRAGMA journal_mode", null, null);
        if (!value.equalsIgnoreCase(newValue)) {
            StringBuilder stringBuilder;
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append("PRAGMA journal_mode=");
                stringBuilder.append(newValue);
                if (executeForString(stringBuilder.toString(), null, null).equalsIgnoreCase(newValue)) {
                    return;
                }
            } catch (SQLiteDatabaseLockedException e) {
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not change the database journal mode of '");
            stringBuilder.append(this.mConfiguration.label);
            stringBuilder.append("' from '");
            stringBuilder.append(value);
            stringBuilder.append("' to '");
            stringBuilder.append(newValue);
            stringBuilder.append("' because the database is locked.  This usually means that there are other open connections to the database which prevents the database from enabling or disabling write-ahead logging mode.  Proceeding without changing the journal mode.");
            Log.w(str, stringBuilder.toString());
        }
    }

    private void setLocaleFromConfiguration() {
        boolean success;
        if ((this.mConfiguration.openFlags & 16) == 0) {
            String newLocale = this.mConfiguration.locale.toString();
            nativeRegisterLocalizedCollators(this.mConnectionPtr, newLocale);
            if (!this.mIsReadOnlyConnection) {
                try {
                    execute("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)", null, null);
                    String oldLocale = executeForString("SELECT locale FROM android_metadata UNION SELECT NULL ORDER BY locale DESC LIMIT 1", null, null);
                    if (oldLocale == null || !oldLocale.equals(newLocale)) {
                        execute("BEGIN", null, null);
                        success = false;
                        execute("DELETE FROM android_metadata", null, null);
                        execute("INSERT INTO android_metadata (locale) VALUES(?)", new Object[]{newLocale}, null);
                        execute("REINDEX LOCALIZED", null, null);
                        execute(true ? "COMMIT" : "ROLLBACK", null, null);
                    }
                } catch (RuntimeException ex) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to change locale for db '");
                    stringBuilder.append(this.mConfiguration.label);
                    stringBuilder.append("' to '");
                    stringBuilder.append(newLocale);
                    stringBuilder.append("'.");
                    Log.w(str, stringBuilder.toString(), ex);
                } catch (Throwable th) {
                    execute(success ? "COMMIT" : "ROLLBACK", null, null);
                }
            }
        }
    }

    void reconfigure(SQLiteDatabaseConfiguration configuration) {
        boolean walModeChanged = false;
        this.mOnlyAllowReadOnlyOperations = false;
        int functionCount = configuration.customFunctions.size();
        for (int i = 0; i < functionCount; i++) {
            SQLiteCustomFunction function = (SQLiteCustomFunction) configuration.customFunctions.get(i);
            if (!this.mConfiguration.customFunctions.contains(function)) {
                nativeRegisterCustomFunction(this.mConnectionPtr, function);
            }
        }
        boolean foreignKeyModeChanged = configuration.foreignKeyConstraintsEnabled != this.mConfiguration.foreignKeyConstraintsEnabled;
        if (((configuration.openFlags ^ this.mConfiguration.openFlags) & 1610612736) != 0) {
            walModeChanged = true;
        }
        boolean localeChanged = configuration.locale.equals(this.mConfiguration.locale) ^ true;
        this.mConfiguration.updateParametersFrom(configuration);
        this.mPreparedStatementCache.resize(configuration.maxSqlCacheSize);
        if (foreignKeyModeChanged) {
            setForeignKeyModeFromConfiguration();
        }
        if (walModeChanged) {
            setWalModeFromConfiguration();
        }
        if (localeChanged) {
            setLocaleFromConfiguration();
        }
    }

    void setOnlyAllowReadOnlyOperations(boolean readOnly) {
        this.mOnlyAllowReadOnlyOperations = readOnly;
    }

    boolean isPreparedStatementInCache(String sql) {
        return this.mPreparedStatementCache.get(sql) != null;
    }

    public int getConnectionId() {
        return this.mConnectionId;
    }

    public boolean isPrimaryConnection() {
        return this.mIsPrimaryConnection;
    }

    public void prepare(String sql, SQLiteStatementInfo outStatementInfo) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("prepare", sql, null);
            PreparedStatement statement;
            try {
                statement = acquirePreparedStatement(sql);
                if (outStatementInfo != null) {
                    outStatementInfo.numParameters = statement.mNumParameters;
                    outStatementInfo.readOnly = statement.mReadOnly;
                    int columnCount = nativeGetColumnCount(this.mConnectionPtr, statement.mStatementPtr);
                    if (columnCount == 0) {
                        outStatementInfo.columnNames = EMPTY_STRING_ARRAY;
                    } else {
                        outStatementInfo.columnNames = new String[columnCount];
                        for (int i = 0; i < columnCount; i++) {
                            outStatementInfo.columnNames[i] = nativeGetColumnName(this.mConnectionPtr, statement.mStatementPtr, i);
                        }
                    }
                }
                releasePreparedStatement(statement);
                this.mRecentOperations.endOperation(cookie);
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th) {
                    this.mRecentOperations.endOperation(cookie);
                }
            } catch (Throwable th2) {
                releasePreparedStatement(statement);
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public void execute(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("execute", sql, bindArgs);
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    nativeExecute(this.mConnectionPtr, statement.mStatementPtr);
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    this.mRecentOperations.endOperation(cookie);
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    this.mRecentOperations.endOperation(cookie);
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public long executeForLong(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("executeForLong", sql, bindArgs);
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    long nativeExecuteForLong = nativeExecuteForLong(this.mConnectionPtr, statement.mStatementPtr);
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    this.mRecentOperations.endOperation(cookie);
                    return nativeExecuteForLong;
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    this.mRecentOperations.endOperation(cookie);
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public String executeForString(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("executeForString", sql, bindArgs);
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    String nativeExecuteForString = nativeExecuteForString(this.mConnectionPtr, statement.mStatementPtr);
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    this.mRecentOperations.endOperation(cookie);
                    return nativeExecuteForString;
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    this.mRecentOperations.endOperation(cookie);
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public ParcelFileDescriptor executeForBlobFileDescriptor(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("executeForBlobFileDescriptor", sql, bindArgs);
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    int fd = nativeExecuteForBlobFileDescriptor(this.mConnectionPtr, statement.mStatementPtr);
                    ParcelFileDescriptor adoptFd = fd >= 0 ? ParcelFileDescriptor.adoptFd(fd) : null;
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    this.mRecentOperations.endOperation(cookie);
                    return adoptFd;
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    this.mRecentOperations.endOperation(cookie);
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public int executeForChangedRowCount(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int changedRows = 0;
            int cookie = this.mRecentOperations.beginOperation("executeForChangedRowCount", sql, bindArgs);
            OperationLog operationLog;
            StringBuilder stringBuilder;
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    changedRows = nativeExecuteForChangedRowCount(this.mConnectionPtr, statement.mStatementPtr);
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                        operationLog = this.mRecentOperations;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("changedRows=");
                        stringBuilder.append(changedRows);
                        operationLog.logOperation(cookie, stringBuilder.toString());
                    }
                    return changedRows;
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                        operationLog = this.mRecentOperations;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("changedRows=");
                        stringBuilder.append(changedRows);
                        operationLog.logOperation(cookie, stringBuilder.toString());
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    public long executeForLastInsertedRowId(String sql, Object[] bindArgs, CancellationSignal cancellationSignal) {
        if (sql != null) {
            int cookie = this.mRecentOperations.beginOperation("executeForLastInsertedRowId", sql, bindArgs);
            try {
                PreparedStatement statement = acquirePreparedStatement(sql);
                try {
                    throwIfStatementForbidden(statement);
                    bindArguments(statement, bindArgs);
                    applyBlockGuardPolicy(statement);
                    attachCancellationSignal(cancellationSignal);
                    long nativeExecuteForLastInsertedRowId = nativeExecuteForLastInsertedRowId(this.mConnectionPtr, statement.mStatementPtr);
                    detachCancellationSignal(cancellationSignal);
                    releasePreparedStatement(statement);
                    this.mRecentOperations.endOperation(cookie);
                    return nativeExecuteForLastInsertedRowId;
                } catch (Throwable th) {
                    releasePreparedStatement(statement);
                }
            } catch (RuntimeException ex) {
                try {
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th2) {
                    this.mRecentOperations.endOperation(cookie);
                }
            }
        } else {
            throw new IllegalArgumentException("sql must not be null.");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:67:0x00f0 A:{Catch:{ all -> 0x0128 }} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00f0 A:{Catch:{ all -> 0x0128 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int executeForCursorWindow(String sql, Object[] bindArgs, CursorWindow window, int startPos, int requiredPos, boolean countAllRows, CancellationSignal cancellationSignal) {
        PreparedStatement statement;
        int cookie;
        int countedRows;
        int filledRows;
        Throwable th;
        RuntimeException ex;
        String str = sql;
        Object[] objArr = bindArgs;
        CursorWindow cursorWindow = window;
        int i = startPos;
        CancellationSignal cancellationSignal2 = cancellationSignal;
        if (str == null) {
            throw new IllegalArgumentException("sql must not be null.");
        } else if (cursorWindow != null) {
            window.acquireReference();
            int actualPos = -1;
            int countedRows2 = -1;
            int filledRows2 = -1;
            try {
                int cookie2 = this.mRecentOperations.beginOperation("executeForCursorWindow", str, objArr);
                int actualPos2;
                try {
                    PreparedStatement statement2 = acquirePreparedStatement(sql);
                    try {
                        throwIfStatementForbidden(statement2);
                        bindArguments(statement2, objArr);
                        applyBlockGuardPolicy(statement2);
                        attachCancellationSignal(cancellationSignal2);
                        try {
                            statement = statement2;
                            cookie = cookie2;
                            try {
                                long result = nativeExecuteForCursorWindow(this.mConnectionPtr, statement2.mStatementPtr, cursorWindow.mWindowPtr, i, requiredPos, countAllRows);
                                actualPos2 = (int) (result >> 32);
                                countedRows = (int) result;
                                try {
                                    filledRows = window.getNumRows();
                                    try {
                                        cursorWindow.setStartPosition(actualPos2);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        actualPos = actualPos2;
                                        countedRows2 = countedRows;
                                        filledRows2 = filledRows;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    actualPos = actualPos2;
                                    countedRows2 = countedRows;
                                    try {
                                        detachCancellationSignal(cancellationSignal2);
                                        throw th;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        try {
                                            releasePreparedStatement(statement);
                                            throw th;
                                        } catch (RuntimeException e) {
                                            ex = e;
                                            try {
                                                this.mRecentOperations.failOperation(cookie, ex);
                                                throw ex;
                                            } catch (Throwable th5) {
                                                th = th5;
                                                actualPos2 = actualPos;
                                                countedRows = countedRows2;
                                                filledRows = filledRows2;
                                                if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                                                    OperationLog operationLog = this.mRecentOperations;
                                                    StringBuilder stringBuilder = new StringBuilder();
                                                    stringBuilder.append("window='");
                                                    stringBuilder.append(cursorWindow);
                                                    stringBuilder.append("', startPos=");
                                                    stringBuilder.append(i);
                                                    stringBuilder.append(", actualPos=");
                                                    stringBuilder.append(actualPos2);
                                                    stringBuilder.append(", filledRows=");
                                                    stringBuilder.append(filledRows);
                                                    stringBuilder.append(", countedRows=");
                                                    stringBuilder.append(countedRows);
                                                    operationLog.logOperation(cookie, stringBuilder.toString());
                                                }
                                                throw th;
                                            }
                                        }
                                    }
                                }
                                try {
                                    detachCancellationSignal(cancellationSignal2);
                                    try {
                                        releasePreparedStatement(statement);
                                        if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                                            OperationLog operationLog2 = this.mRecentOperations;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("window='");
                                            stringBuilder2.append(cursorWindow);
                                            stringBuilder2.append("', startPos=");
                                            stringBuilder2.append(i);
                                            stringBuilder2.append(", actualPos=");
                                            stringBuilder2.append(actualPos2);
                                            stringBuilder2.append(", filledRows=");
                                            stringBuilder2.append(filledRows);
                                            stringBuilder2.append(", countedRows=");
                                            stringBuilder2.append(countedRows);
                                            operationLog2.logOperation(cookie, stringBuilder2.toString());
                                        }
                                        window.releaseReference();
                                        return countedRows;
                                    } catch (RuntimeException e2) {
                                        ex = e2;
                                        actualPos = actualPos2;
                                        countedRows2 = countedRows;
                                        filledRows2 = filledRows;
                                        this.mRecentOperations.failOperation(cookie, ex);
                                        throw ex;
                                    } catch (Throwable th6) {
                                        th = th6;
                                        if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    actualPos = actualPos2;
                                    countedRows2 = countedRows;
                                    filledRows2 = filledRows;
                                    releasePreparedStatement(statement);
                                    throw th;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                detachCancellationSignal(cancellationSignal2);
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            statement = statement2;
                            cookie = cookie2;
                            detachCancellationSignal(cancellationSignal2);
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        statement = statement2;
                        cookie = cookie2;
                        releasePreparedStatement(statement);
                        throw th;
                    }
                } catch (RuntimeException e3) {
                    ex = e3;
                    cookie = cookie2;
                    this.mRecentOperations.failOperation(cookie, ex);
                    throw ex;
                } catch (Throwable th11) {
                    th = th11;
                    cookie = cookie2;
                    actualPos2 = -1;
                    countedRows = -1;
                    filledRows = -1;
                    if (this.mRecentOperations.endOperationDeferLog(cookie)) {
                    }
                    throw th;
                }
            } catch (Throwable th12) {
                window.releaseReference();
            }
        } else {
            throw new IllegalArgumentException("window must not be null.");
        }
    }

    private PreparedStatement acquirePreparedStatement(String sql) {
        PreparedStatement statement = (PreparedStatement) this.mPreparedStatementCache.get(sql);
        boolean skipCache = false;
        if (statement != null) {
            if (!statement.mInUse) {
                return statement;
            }
            skipCache = true;
        }
        long statementPtr = nativePrepareStatement(this.mConnectionPtr, sql);
        try {
            int numParameters = nativeGetParameterCount(this.mConnectionPtr, statementPtr);
            int type = DatabaseUtils.getSqlStatementType(sql);
            statement = obtainPreparedStatement(sql, statementPtr, numParameters, type, nativeIsReadOnly(this.mConnectionPtr, statementPtr));
            if (!skipCache && isCacheable(type)) {
                this.mPreparedStatementCache.put(sql, statement);
                statement.mInCache = true;
            }
            statement.mInUse = true;
            return statement;
        } catch (RuntimeException ex) {
            if (statement == null || !statement.mInCache) {
                nativeFinalizeStatement(this.mConnectionPtr, statementPtr);
            }
            throw ex;
        }
    }

    private void releasePreparedStatement(PreparedStatement statement) {
        statement.mInUse = false;
        if (statement.mInCache) {
            try {
                nativeResetStatementAndClearBindings(this.mConnectionPtr, statement.mStatementPtr);
                return;
            } catch (SQLiteException e) {
                this.mPreparedStatementCache.remove(statement.mSql);
                return;
            }
        }
        finalizePreparedStatement(statement);
    }

    private void finalizePreparedStatement(PreparedStatement statement) {
        nativeFinalizeStatement(this.mConnectionPtr, statement.mStatementPtr);
        recyclePreparedStatement(statement);
    }

    private void attachCancellationSignal(CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
            this.mCancellationSignalAttachCount++;
            if (this.mCancellationSignalAttachCount == 1) {
                nativeResetCancel(this.mConnectionPtr, true);
                cancellationSignal.setOnCancelListener(this);
            }
        }
    }

    private void detachCancellationSignal(CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) {
            this.mCancellationSignalAttachCount--;
            if (this.mCancellationSignalAttachCount == 0) {
                cancellationSignal.setOnCancelListener(null);
                nativeResetCancel(this.mConnectionPtr, false);
            }
        }
    }

    public void onCancel() {
        nativeCancel(this.mConnectionPtr);
    }

    private void bindArguments(PreparedStatement statement, Object[] bindArgs) {
        int i = 0;
        int count = bindArgs != null ? bindArgs.length : 0;
        if (count != statement.mNumParameters) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected ");
            stringBuilder.append(statement.mNumParameters);
            stringBuilder.append(" bind arguments but ");
            stringBuilder.append(count);
            stringBuilder.append(" were provided.");
            throw new SQLiteBindOrColumnIndexOutOfRangeException(stringBuilder.toString());
        } else if (count != 0) {
            long statementPtr = statement.mStatementPtr;
            while (i < count) {
                Object arg = bindArgs[i];
                int typeOfObject = DatabaseUtils.getTypeOfObject(arg);
                if (typeOfObject != 4) {
                    switch (typeOfObject) {
                        case 0:
                            nativeBindNull(this.mConnectionPtr, statementPtr, i + 1);
                            break;
                        case 1:
                            nativeBindLong(this.mConnectionPtr, statementPtr, i + 1, ((Number) arg).longValue());
                            break;
                        case 2:
                            nativeBindDouble(this.mConnectionPtr, statementPtr, i + 1, ((Number) arg).doubleValue());
                            break;
                        default:
                            if (!(arg instanceof Boolean)) {
                                nativeBindString(this.mConnectionPtr, statementPtr, i + 1, arg.toString());
                                break;
                            } else {
                                nativeBindLong(this.mConnectionPtr, statementPtr, i + 1, ((Boolean) arg).booleanValue() ? 1 : 0);
                                break;
                            }
                    }
                }
                nativeBindBlob(this.mConnectionPtr, statementPtr, i + 1, (byte[]) arg);
                i++;
            }
        }
    }

    private void throwIfStatementForbidden(PreparedStatement statement) {
        if (this.mOnlyAllowReadOnlyOperations && !statement.mReadOnly) {
            throw new SQLiteException("Cannot execute this statement because it might modify the database but the connection is read-only.");
        }
    }

    private static boolean isCacheable(int statementType) {
        if (statementType == 2 || statementType == 1) {
            return true;
        }
        return false;
    }

    private void applyBlockGuardPolicy(PreparedStatement statement) {
        if (!this.mConfiguration.isInMemoryDb()) {
            if (statement.mReadOnly) {
                BlockGuard.getThreadPolicy().onReadFromDisk();
            } else {
                BlockGuard.getThreadPolicy().onWriteToDisk();
            }
        }
    }

    public void dump(Printer printer, boolean verbose) {
        dumpUnsafe(printer, verbose);
    }

    void dumpUnsafe(Printer printer, boolean verbose) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connection #");
        stringBuilder.append(this.mConnectionId);
        stringBuilder.append(":");
        printer.println(stringBuilder.toString());
        if (verbose) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  connectionPtr: 0x");
            stringBuilder.append(Long.toHexString(this.mConnectionPtr));
            printer.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("  isPrimaryConnection: ");
        stringBuilder.append(this.mIsPrimaryConnection);
        printer.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  onlyAllowReadOnlyOperations: ");
        stringBuilder.append(this.mOnlyAllowReadOnlyOperations);
        printer.println(stringBuilder.toString());
    }

    String describeCurrentOperationUnsafe() {
        return this.mRecentOperations.describeCurrentOperation();
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x00e0 A:{ExcHandler: all (th java.lang.Throwable), Splitter:B:9:0x003f} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00e0 A:{ExcHandler: all (th java.lang.Throwable), Splitter:B:9:0x003f} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:36:0x00e0, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void collectDbStats(ArrayList<DbStats> dbStatsList) {
        CursorWindow window;
        ArrayList<DbStats> arrayList = dbStatsList;
        int lookaside = nativeGetDbLookaside(this.mConnectionPtr);
        long pageCount = 0;
        long pageSize = 0;
        try {
            pageCount = executeForLong("PRAGMA page_count;", null, null);
            pageSize = executeForLong("PRAGMA page_size;", null, null);
        } catch (SQLiteException e) {
        }
        arrayList.add(getMainDbStatsUnsafe(lookaside, pageCount, pageSize));
        CursorWindow window2 = new CursorWindow("collectDbStats");
        try {
            window = window2;
            try {
                executeForCursorWindow("PRAGMA database_list;", null, window2, 0, 0, false, null);
                int i = 1;
                while (true) {
                    int i2 = i;
                    if (i2 >= window.getNumRows()) {
                        break;
                    }
                    String name = window.getString(i2, 1);
                    String path = window.getString(i2, 2);
                    long pageCount2 = 0;
                    long pageSize2 = 0;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PRAGMA ");
                    stringBuilder.append(name);
                    stringBuilder.append(".page_count;");
                    pageCount2 = executeForLong(stringBuilder.toString(), null, null);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PRAGMA ");
                    stringBuilder.append(name);
                    stringBuilder.append(".page_size;");
                    pageSize2 = executeForLong(stringBuilder.toString(), null, null);
                    String label = new StringBuilder();
                    label.append("  (attached) ");
                    label.append(name);
                    label = label.toString();
                    if (!path.isEmpty()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(label);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(path);
                        label = stringBuilder2.toString();
                    }
                    arrayList.add(new DbStats(label, pageCount2, pageSize2, 0, 0, 0, 0));
                    i = i2 + 1;
                }
            } catch (SQLiteException e2) {
            } catch (Throwable th) {
            }
        } catch (SQLiteException e3) {
            window = window2;
        } catch (Throwable th2) {
            Throwable th3 = th2;
            window = window2;
            window.close();
            throw th3;
        }
        window.close();
    }

    void collectDbStatsUnsafe(ArrayList<DbStats> dbStatsList) {
        dbStatsList.add(getMainDbStatsUnsafe(0, 0, 0));
    }

    private DbStats getMainDbStatsUnsafe(int lookaside, long pageCount, long pageSize) {
        String label = this.mConfiguration.path;
        if (!this.mIsPrimaryConnection) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(label);
            stringBuilder.append(" (");
            stringBuilder.append(this.mConnectionId);
            stringBuilder.append(")");
            label = stringBuilder.toString();
        }
        return new DbStats(label, pageCount, pageSize, lookaside, this.mPreparedStatementCache.hitCount(), this.mPreparedStatementCache.missCount(), this.mPreparedStatementCache.size());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLiteConnection: ");
        stringBuilder.append(this.mConfiguration.path);
        stringBuilder.append(" (");
        stringBuilder.append(this.mConnectionId);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private PreparedStatement obtainPreparedStatement(String sql, long statementPtr, int numParameters, int type, boolean readOnly) {
        PreparedStatement statement = this.mPreparedStatementPool;
        if (statement != null) {
            this.mPreparedStatementPool = statement.mPoolNext;
            statement.mPoolNext = null;
            statement.mInCache = false;
        } else {
            statement = new PreparedStatement();
        }
        statement.mSql = sql;
        statement.mStatementPtr = statementPtr;
        statement.mNumParameters = numParameters;
        statement.mType = type;
        statement.mReadOnly = readOnly;
        return statement;
    }

    private void recyclePreparedStatement(PreparedStatement statement) {
        statement.mSql = null;
        statement.mPoolNext = this.mPreparedStatementPool;
        this.mPreparedStatementPool = statement;
    }

    private static String trimSqlForDisplay(String sql) {
        return sql.replaceAll("[\\s]*\\n+[\\s]*", " ");
    }

    static SQLiteConnection openExclusive(SQLiteConnectionPool pool, SQLiteDatabaseConfiguration configuration, int connectionId) {
        SQLiteConnection connection = new SQLiteConnection(pool, configuration, connectionId, false, true);
        try {
            connection.open();
            return connection;
        } catch (SQLiteException ex) {
            connection.dispose(false);
            throw ex;
        }
    }

    public boolean isExclusiveConnection() {
        return this.mIsExclusiveConnection;
    }
}
