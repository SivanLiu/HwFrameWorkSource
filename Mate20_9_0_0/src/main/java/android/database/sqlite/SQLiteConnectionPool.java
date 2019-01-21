package android.database.sqlite;

import android.database.sqlite.SQLiteDebug.DbStats;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.OperationCanceledException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.PrefixPrinter;
import android.util.Printer;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class SQLiteConnectionPool implements Closeable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int CONNECTION_FLAG_EXCLUSIVE = 8;
    public static final int CONNECTION_FLAG_INTERACTIVE = 4;
    public static final int CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY = 2;
    public static final int CONNECTION_FLAG_READ_ONLY = 1;
    private static final long CONNECTION_POOL_BUSY_MILLIS = 30000;
    private static final String TAG = "SQLiteConnectionPool";
    private final String GALLERY_PROVIDER_DB_PATH = "/data/user/0/com.android.gallery3d/databases/gallery.db";
    private final String MEDIA_PROVIDER_DB_PATH = "/data/user/0/com.android.providers.media/databases/external.db";
    private final WeakHashMap<SQLiteConnection, AcquiredConnectionStatus> mAcquiredConnections = new WeakHashMap();
    private SQLiteConnection mAvailableExclusiveConnection;
    private final ArrayList<SQLiteConnection> mAvailableNonPrimaryConnections = new ArrayList();
    private SQLiteConnection mAvailablePrimaryConnection;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final SQLiteDatabaseConfiguration mConfiguration;
    private final AtomicBoolean mConnectionLeaked = new AtomicBoolean();
    private ConnectionWaiter mConnectionWaiterPool;
    private ConnectionWaiter mConnectionWaiterQueue;
    private boolean mEnableExclusiveConnection;
    @GuardedBy("mLock")
    private IdleConnectionHandler mIdleConnectionHandler;
    private boolean mIsOpen;
    private final Object mLock = new Object();
    private int mMaxConnectionPoolSize;
    private int mNextConnectionId;
    private final AtomicLong mTotalExecutionTimeCounter = new AtomicLong(0);

    enum AcquiredConnectionStatus {
        NORMAL,
        RECONFIGURE,
        DISCARD
    }

    private static final class ConnectionWaiter {
        public SQLiteConnection mAssignedConnection;
        public int mConnectionFlags;
        public RuntimeException mException;
        public ConnectionWaiter mNext;
        public int mNonce;
        public int mPriority;
        public String mSql;
        public long mStartTime;
        public Thread mThread;
        public boolean mWantExclusiveConnection;
        public boolean mWantPrimaryConnection;

        private ConnectionWaiter() {
        }

        /* synthetic */ ConnectionWaiter(AnonymousClass1 x0) {
            this();
        }
    }

    private class IdleConnectionHandler extends Handler {
        private final long mTimeout;

        IdleConnectionHandler(Looper looper, long timeout) {
            super(looper);
            this.mTimeout = timeout;
        }

        /* JADX WARNING: Missing block: B:13:0x0057, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            synchronized (SQLiteConnectionPool.this.mLock) {
                if (this != SQLiteConnectionPool.this.mIdleConnectionHandler) {
                } else if (SQLiteConnectionPool.this.closeAvailableConnectionLocked(msg.what) && Log.isLoggable(SQLiteConnectionPool.TAG, 3)) {
                    String str = SQLiteConnectionPool.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Closed idle connection ");
                    stringBuilder.append(SQLiteConnectionPool.this.mConfiguration.label);
                    stringBuilder.append(" ");
                    stringBuilder.append(msg.what);
                    stringBuilder.append(" after ");
                    stringBuilder.append(this.mTimeout);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }

        void connectionReleased(SQLiteConnection con) {
            sendEmptyMessageDelayed(con.getConnectionId(), this.mTimeout);
        }

        void connectionAcquired(SQLiteConnection con) {
            removeMessages(con.getConnectionId());
        }

        void connectionClosed(SQLiteConnection con) {
            removeMessages(con.getConnectionId());
        }
    }

    private SQLiteConnectionPool(SQLiteDatabaseConfiguration configuration) {
        this.mConfiguration = new SQLiteDatabaseConfiguration(configuration);
        setMaxConnectionPoolSizeLocked();
        if (this.mConfiguration.idleConnectionTimeoutMs != Long.MAX_VALUE) {
            setupIdleConnectionHandler(Looper.getMainLooper(), this.mConfiguration.idleConnectionTimeoutMs);
        }
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static SQLiteConnectionPool open(SQLiteDatabaseConfiguration configuration) {
        return open(configuration, false);
    }

    public static SQLiteConnectionPool open(SQLiteDatabaseConfiguration configuration, boolean enableExclusiveConnection) {
        if (configuration != null) {
            SQLiteConnectionPool pool = new SQLiteConnectionPool(configuration);
            pool.open();
            pool.setExclusiveConnectionEnabled(enableExclusiveConnection);
            return pool;
        }
        throw new IllegalArgumentException("configuration must not be null.");
    }

    private void open() {
        this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionReleased(this.mAvailablePrimaryConnection);
            }
        }
        this.mIsOpen = true;
        this.mCloseGuard.open("close");
    }

    public void close() {
        dispose(false);
    }

    private void dispose(boolean finalized) {
        if (this.mCloseGuard != null) {
            if (finalized) {
                this.mCloseGuard.warnIfOpen();
            }
            this.mCloseGuard.close();
        }
        if (!finalized) {
            synchronized (this.mLock) {
                throwIfClosedLocked();
                this.mIsOpen = false;
                closeAvailableConnectionsAndLogExceptionsLocked();
                int pendingCount = this.mAcquiredConnections.size();
                if (pendingCount != 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("The connection pool for ");
                    stringBuilder.append(this.mConfiguration.label);
                    stringBuilder.append(" has been closed but there are still ");
                    stringBuilder.append(pendingCount);
                    stringBuilder.append(" connections in use.  They will be closed as they are released back to the pool.");
                    Log.i(str, stringBuilder.toString());
                }
                wakeConnectionWaitersLocked();
            }
        }
    }

    public void reconfigure(SQLiteDatabaseConfiguration configuration) {
        if (configuration != null) {
            synchronized (this.mLock) {
                throwIfClosedLocked();
                boolean onlyCompatWalChanged = false;
                boolean walModeChanged = ((configuration.openFlags ^ this.mConfiguration.openFlags) & 536870912) != 0;
                if (configuration.configurationEnhancement) {
                    boolean z;
                    if (!walModeChanged && configuration.defaultWALEnabled == this.mConfiguration.defaultWALEnabled) {
                        if (configuration.explicitWALEnabled == this.mConfiguration.explicitWALEnabled) {
                            z = false;
                            walModeChanged = z;
                        }
                    }
                    z = true;
                    walModeChanged = z;
                }
                if (walModeChanged) {
                    if (this.mAcquiredConnections.isEmpty()) {
                        if (this.mAvailableExclusiveConnection != null) {
                            closeConnectionAndLogExceptionsLocked(this.mAvailableExclusiveConnection);
                            this.mAvailableExclusiveConnection = null;
                        }
                        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
                    } else {
                        throw new IllegalStateException("Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
                    }
                }
                if (configuration.foreignKeyConstraintsEnabled != this.mConfiguration.foreignKeyConstraintsEnabled) {
                    if (!this.mAcquiredConnections.isEmpty()) {
                        throw new IllegalStateException("Foreign Key Constraints cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
                    }
                }
                if ((this.mConfiguration.openFlags ^ configuration.openFlags) == 1073741824) {
                    onlyCompatWalChanged = true;
                }
                boolean configEnhanceChanged;
                if (this.mConfiguration.configurationEnhancement) {
                    configEnhanceChanged = walModeChanged;
                } else {
                    configEnhanceChanged = false;
                }
                if ((onlyCompatWalChanged || this.mConfiguration.openFlags == configuration.openFlags) && !configEnhanceChanged) {
                    this.mConfiguration.updateParametersFrom(configuration);
                    setMaxConnectionPoolSizeLocked();
                    closeExcessConnectionsAndLogExceptionsLocked();
                    reconfigureAllConnectionsLocked();
                } else {
                    if (walModeChanged) {
                        closeAvailableConnectionsAndLogExceptionsLocked();
                    }
                    SQLiteConnection newPrimaryConnection = openConnectionLocked(configuration, true);
                    closeAvailableConnectionsAndLogExceptionsLocked();
                    discardAcquiredConnectionsLocked();
                    this.mAvailablePrimaryConnection = newPrimaryConnection;
                    this.mConfiguration.updateParametersFrom(configuration);
                    setMaxConnectionPoolSizeLocked();
                }
                wakeConnectionWaitersLocked();
            }
            return;
        }
        throw new IllegalArgumentException("configuration must not be null.");
    }

    public SQLiteConnection acquireConnection(String sql, int connectionFlags, CancellationSignal cancellationSignal) {
        SQLiteConnection con = waitForConnection(sql, connectionFlags, cancellationSignal);
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionAcquired(con);
            }
        }
        return con;
    }

    public void releaseConnection(SQLiteConnection connection) {
        synchronized (this.mLock) {
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionReleased(connection);
            }
            AcquiredConnectionStatus status = (AcquiredConnectionStatus) this.mAcquiredConnections.remove(connection);
            if (status != null) {
                if (!this.mIsOpen) {
                    closeConnectionAndLogExceptionsLocked(connection);
                } else if (connection.isPrimaryConnection()) {
                    if (recycleConnectionLocked(connection, status)) {
                        this.mAvailablePrimaryConnection = connection;
                    }
                    wakeConnectionWaitersLocked();
                } else if (connection.isExclusiveConnection()) {
                    if (recycleConnectionLocked(connection, status)) {
                        this.mAvailableExclusiveConnection = connection;
                    }
                    wakeConnectionWaitersLocked();
                } else if (this.mAvailableNonPrimaryConnections.size() >= getMaxNonPrimaryConnectionSizeLocked()) {
                    closeConnectionAndLogExceptionsLocked(connection);
                } else {
                    if (recycleConnectionLocked(connection, status)) {
                        this.mAvailableNonPrimaryConnections.add(connection);
                    }
                    wakeConnectionWaitersLocked();
                }
            } else {
                throw new IllegalStateException("Cannot perform this operation because the specified connection was not acquired from this pool or has already been released.");
            }
        }
    }

    @GuardedBy("mLock")
    private boolean recycleConnectionLocked(SQLiteConnection connection, AcquiredConnectionStatus status) {
        if (status == AcquiredConnectionStatus.RECONFIGURE) {
            try {
                connection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to reconfigure released connection, closing it: ");
                stringBuilder.append(connection);
                Log.e(str, stringBuilder.toString(), ex);
                status = AcquiredConnectionStatus.DISCARD;
            }
        }
        if (status != AcquiredConnectionStatus.DISCARD) {
            return true;
        }
        closeConnectionAndLogExceptionsLocked(connection);
        return false;
    }

    public boolean shouldYieldConnection(SQLiteConnection connection, int connectionFlags) {
        synchronized (this.mLock) {
            if (!this.mAcquiredConnections.containsKey(connection)) {
                throw new IllegalStateException("Cannot perform this operation because the specified connection was not acquired from this pool or has already been released.");
            } else if (this.mIsOpen) {
                boolean isSessionBlockingImportantConnectionWaitersLocked = isSessionBlockingImportantConnectionWaitersLocked(connection.isPrimaryConnection(), connectionFlags);
                return isSessionBlockingImportantConnectionWaitersLocked;
            } else {
                return false;
            }
        }
    }

    public void collectDbStats(ArrayList<DbStats> dbStatsList) {
        synchronized (this.mLock) {
            if (this.mAvailablePrimaryConnection != null) {
                this.mAvailablePrimaryConnection.collectDbStats(dbStatsList);
            }
            Iterator it = this.mAvailableNonPrimaryConnections.iterator();
            while (it.hasNext()) {
                ((SQLiteConnection) it.next()).collectDbStats(dbStatsList);
            }
            if (this.mAvailableExclusiveConnection != null) {
                this.mAvailableExclusiveConnection.collectDbStats(dbStatsList);
            }
            for (SQLiteConnection connection : this.mAcquiredConnections.keySet()) {
                connection.collectDbStatsUnsafe(dbStatsList);
            }
        }
    }

    private SQLiteConnection openConnectionLocked(SQLiteDatabaseConfiguration configuration, boolean primaryConnection) {
        int connectionId = this.mNextConnectionId;
        this.mNextConnectionId = connectionId + 1;
        return SQLiteConnection.open(this, configuration, connectionId, primaryConnection);
    }

    void onConnectionLeaked() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("A SQLiteConnection object for database '");
        stringBuilder.append(this.mConfiguration.label);
        stringBuilder.append("' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.");
        Log.w(str, stringBuilder.toString());
        this.mConnectionLeaked.set(true);
    }

    void onStatementExecuted(long executionTimeMs) {
        this.mTotalExecutionTimeCounter.addAndGet(executionTimeMs);
    }

    @GuardedBy("mLock")
    private void closeAvailableConnectionsAndLogExceptionsLocked() {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
        if (this.mAvailablePrimaryConnection != null) {
            closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
            this.mAvailablePrimaryConnection = null;
        }
        if (this.mAvailableExclusiveConnection != null) {
            closeConnectionAndLogExceptionsLocked(this.mAvailableExclusiveConnection);
            this.mAvailableExclusiveConnection = null;
        }
    }

    @GuardedBy("mLock")
    private boolean closeAvailableConnectionLocked(int connectionId) {
        for (int i = this.mAvailableNonPrimaryConnections.size() - 1; i >= 0; i--) {
            SQLiteConnection c = (SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i);
            if (c.getConnectionId() == connectionId) {
                closeConnectionAndLogExceptionsLocked(c);
                this.mAvailableNonPrimaryConnections.remove(i);
                return true;
            }
        }
        if (this.mAvailablePrimaryConnection == null || this.mAvailablePrimaryConnection.getConnectionId() != connectionId) {
            return false;
        }
        closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
        this.mAvailablePrimaryConnection = null;
        return true;
    }

    @GuardedBy("mLock")
    private void closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked() {
        int count = this.mAvailableNonPrimaryConnections.size();
        for (int i = 0; i < count; i++) {
            closeConnectionAndLogExceptionsLocked((SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i));
        }
        this.mAvailableNonPrimaryConnections.clear();
    }

    void closeAvailableNonPrimaryConnectionsAndLogExceptions() {
        synchronized (this.mLock) {
            closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
        }
    }

    @GuardedBy("mLock")
    private void closeExcessConnectionsAndLogExceptionsLocked() {
        int availableCount = this.mAvailableNonPrimaryConnections.size();
        while (true) {
            int availableCount2 = availableCount - 1;
            if (availableCount > getMaxNonPrimaryConnectionSizeLocked()) {
                closeConnectionAndLogExceptionsLocked((SQLiteConnection) this.mAvailableNonPrimaryConnections.remove(availableCount2));
                availableCount = availableCount2;
            } else {
                return;
            }
        }
    }

    @GuardedBy("mLock")
    private void closeConnectionAndLogExceptionsLocked(SQLiteConnection connection) {
        try {
            connection.close();
            if (this.mIdleConnectionHandler != null) {
                this.mIdleConnectionHandler.connectionClosed(connection);
            }
        } catch (RuntimeException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to close connection, its fate is now in the hands of the merciful GC: ");
            stringBuilder.append(connection);
            Log.e(str, stringBuilder.toString(), ex);
        }
    }

    private void discardAcquiredConnectionsLocked() {
        markAcquiredConnectionsLocked(AcquiredConnectionStatus.DISCARD);
    }

    @GuardedBy("mLock")
    private void reconfigureAllConnectionsLocked() {
        String str;
        StringBuilder stringBuilder;
        if (this.mAvailablePrimaryConnection != null) {
            try {
                this.mAvailablePrimaryConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to reconfigure available primary connection, closing it: ");
                stringBuilder.append(this.mAvailablePrimaryConnection);
                Log.e(str, stringBuilder.toString(), ex);
                closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
                this.mAvailablePrimaryConnection = null;
            }
        }
        if (this.mAvailableExclusiveConnection != null) {
            try {
                this.mAvailableExclusiveConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to reconfigure available exclusive connection, closing it: ");
                stringBuilder.append(this.mAvailableExclusiveConnection);
                Log.e(str, stringBuilder.toString(), ex2);
                closeConnectionAndLogExceptionsLocked(this.mAvailableExclusiveConnection);
                this.mAvailableExclusiveConnection = null;
            }
        }
        int count = this.mAvailableNonPrimaryConnections.size();
        int i = 0;
        while (i < count) {
            SQLiteConnection connection = (SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i);
            try {
                connection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex3) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to reconfigure available non-primary connection, closing it: ");
                stringBuilder2.append(connection);
                Log.e(str2, stringBuilder2.toString(), ex3);
                closeConnectionAndLogExceptionsLocked(connection);
                int i2 = i - 1;
                this.mAvailableNonPrimaryConnections.remove(i);
                count--;
                i = i2;
            }
            i++;
        }
        markAcquiredConnectionsLocked(AcquiredConnectionStatus.RECONFIGURE);
    }

    private void markAcquiredConnectionsLocked(AcquiredConnectionStatus status) {
        if (!this.mAcquiredConnections.isEmpty()) {
            ArrayList<SQLiteConnection> keysToUpdate = new ArrayList(this.mAcquiredConnections.size());
            for (Entry<SQLiteConnection, AcquiredConnectionStatus> entry : this.mAcquiredConnections.entrySet()) {
                AcquiredConnectionStatus oldStatus = (AcquiredConnectionStatus) entry.getValue();
                if (!(status == oldStatus || oldStatus == AcquiredConnectionStatus.DISCARD)) {
                    keysToUpdate.add((SQLiteConnection) entry.getKey());
                }
            }
            int updateCount = keysToUpdate.size();
            for (int i = 0; i < updateCount; i++) {
                this.mAcquiredConnections.put((SQLiteConnection) keysToUpdate.get(i), status);
            }
        }
    }

    private SQLiteConnection waitForConnection(String sql, int connectionFlags, CancellationSignal cancellationSignal) {
        Throwable connection;
        Object obj;
        SQLiteConnection connection2;
        int i = connectionFlags;
        CancellationSignal cancellationSignal2 = cancellationSignal;
        boolean z = true;
        boolean wantPrimaryConnection = (i & 2) != 0;
        boolean wantExclusiveConnection = (i & 8) != 0;
        Object obj2 = this.mLock;
        synchronized (obj2) {
            boolean z2;
            try {
                throwIfClosedLocked();
                if (cancellationSignal2 != null) {
                    try {
                        cancellationSignal.throwIfCanceled();
                    } catch (Throwable th) {
                        connection = th;
                        obj = obj2;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                connection = th2;
                            }
                        }
                        throw connection;
                    }
                }
                connection2 = null;
                if (!wantPrimaryConnection && wantExclusiveConnection && isExclusiveConnectionEnabled()) {
                    connection2 = tryAcquireExclusiveConnectionLocked(i);
                }
                if (connection2 == null && !wantPrimaryConnection) {
                    connection2 = tryAcquireNonPrimaryConnectionLocked(sql, connectionFlags);
                }
                if (connection2 == null && (!isExclusiveConnectionEnabled() || (isExclusiveConnectionEnabled() && wantPrimaryConnection))) {
                    connection2 = tryAcquirePrimaryConnectionLocked(i);
                }
                if (connection2 != null) {
                } else {
                    int priority = getPriority(connectionFlags);
                    int priority2 = priority;
                    obj = obj2;
                    try {
                        final ConnectionWaiter waiter = obtainConnectionWaiterLocked(Thread.currentThread(), SystemClock.uptimeMillis(), priority, wantPrimaryConnection, wantExclusiveConnection, sql, i);
                        ConnectionWaiter predecessor = null;
                        ConnectionWaiter successor = this.mConnectionWaiterQueue;
                        while (successor != null) {
                            try {
                                if (priority2 > successor.mPriority) {
                                    waiter.mNext = successor;
                                    break;
                                }
                                predecessor = successor;
                                successor = successor.mNext;
                            } catch (Throwable th3) {
                                connection = th3;
                                while (true) {
                                    break;
                                }
                                throw connection;
                            }
                        }
                        if (predecessor != null) {
                            predecessor.mNext = waiter;
                        } else {
                            this.mConnectionWaiterQueue = waiter;
                        }
                        final int nonce = waiter.mNonce;
                        if (cancellationSignal2 != null) {
                            cancellationSignal2.setOnCancelListener(new OnCancelListener() {
                                public void onCancel() {
                                    synchronized (SQLiteConnectionPool.this.mLock) {
                                        if (waiter.mNonce == nonce) {
                                            SQLiteConnectionPool.this.cancelConnectionWaiterLocked(waiter);
                                        }
                                    }
                                }
                            });
                        }
                        long busyTimeoutMillis = 30000;
                        try {
                            RuntimeException ex;
                            long nextBusyTimeoutTime = waiter.mStartTime + 30000;
                            while (true) {
                                if (this.mConnectionLeaked.compareAndSet(z, false)) {
                                    try {
                                        synchronized (this.mLock) {
                                            wakeConnectionWaitersLocked();
                                        }
                                    } catch (Throwable th4) {
                                        connection = th4;
                                        z2 = wantPrimaryConnection;
                                    }
                                }
                                LockSupport.parkNanos(this, busyTimeoutMillis * 1000000);
                                Thread.interrupted();
                                synchronized (this.mLock) {
                                    try {
                                        throwIfClosedLocked();
                                        connection2 = waiter.mAssignedConnection;
                                        ex = waiter.mException;
                                        if (connection2 != null) {
                                            break;
                                        } else if (ex != null) {
                                            z2 = wantPrimaryConnection;
                                            break;
                                        } else {
                                            long nextBusyTimeoutTime2 = SystemClock.uptimeMillis();
                                            if (nextBusyTimeoutTime2 < nextBusyTimeoutTime) {
                                                busyTimeoutMillis = nextBusyTimeoutTime2 - nextBusyTimeoutTime;
                                                z2 = wantPrimaryConnection;
                                            } else {
                                                z2 = wantPrimaryConnection;
                                                logConnectionPoolBusyLocked(nextBusyTimeoutTime2 - waiter.mStartTime, i);
                                                busyTimeoutMillis = 30000;
                                                nextBusyTimeoutTime = nextBusyTimeoutTime2 + 30000;
                                            }
                                        }
                                    } catch (Throwable th5) {
                                        connection = th5;
                                        throw connection;
                                    }
                                }
                                wantPrimaryConnection = z2;
                                z = true;
                            }
                            recycleConnectionWaiterLocked(waiter);
                            if (connection2 != null) {
                                if (cancellationSignal2 != null) {
                                    cancellationSignal2.setOnCancelListener(null);
                                }
                                return connection2;
                            }
                            throw ex;
                        } catch (Throwable th6) {
                            connection = th6;
                            z2 = wantPrimaryConnection;
                            if (cancellationSignal2 != null) {
                                cancellationSignal2.setOnCancelListener(null);
                            }
                            throw connection;
                        }
                    } catch (Throwable th7) {
                        connection = th7;
                        z2 = wantPrimaryConnection;
                        while (true) {
                            break;
                        }
                        throw connection;
                    }
                }
            } catch (Throwable th8) {
                connection = th8;
                obj = obj2;
                z2 = wantPrimaryConnection;
                while (true) {
                    break;
                }
                throw connection;
            }
        }
        return connection2;
    }

    @GuardedBy("mLock")
    private void cancelConnectionWaiterLocked(ConnectionWaiter waiter) {
        if (waiter.mAssignedConnection == null && waiter.mException == null) {
            ConnectionWaiter predecessor = null;
            for (ConnectionWaiter current = this.mConnectionWaiterQueue; current != waiter; current = current.mNext) {
                predecessor = current;
            }
            if (predecessor != null) {
                predecessor.mNext = waiter.mNext;
            } else {
                this.mConnectionWaiterQueue = waiter.mNext;
            }
            waiter.mException = new OperationCanceledException();
            LockSupport.unpark(waiter.mThread);
            wakeConnectionWaitersLocked();
        }
    }

    private void logConnectionPoolBusyLocked(long waitMillis, int connectionFlags) {
        String description;
        Thread thread = Thread.currentThread();
        StringBuilder msg = new StringBuilder();
        msg.append("The connection pool for database '");
        msg.append(this.mConfiguration.label);
        msg.append("' has been unable to grant a connection to thread ");
        msg.append(thread.getId());
        msg.append(" (");
        msg.append(thread.getName());
        msg.append(") ");
        msg.append("with flags 0x");
        msg.append(Integer.toHexString(connectionFlags));
        msg.append(" for ");
        msg.append(((float) waitMillis) * 0.001f);
        msg.append(" seconds.\n");
        ArrayList<String> requests = new ArrayList();
        int activeConnections = 0;
        int idleConnections = 0;
        if (!this.mAcquiredConnections.isEmpty()) {
            for (SQLiteConnection connection : this.mAcquiredConnections.keySet()) {
                description = connection.describeCurrentOperationUnsafe();
                if (description != null) {
                    requests.add(description);
                    activeConnections++;
                } else {
                    idleConnections++;
                }
            }
        }
        int availableConnections = this.mAvailableNonPrimaryConnections.size();
        if (this.mAvailablePrimaryConnection != null) {
            availableConnections++;
        }
        if (this.mAvailableExclusiveConnection != null) {
            availableConnections++;
        }
        msg.append("Connections: ");
        msg.append(activeConnections);
        msg.append(" active, ");
        msg.append(idleConnections);
        msg.append(" idle, ");
        msg.append(availableConnections);
        msg.append(" available.\n");
        if (!requests.isEmpty()) {
            msg.append("\nRequests in progress:\n");
            Iterator it = requests.iterator();
            while (it.hasNext()) {
                description = (String) it.next();
                msg.append("  ");
                msg.append(description);
                msg.append("\n");
            }
        }
        Log.w(TAG, msg.toString());
    }

    @GuardedBy("mLock")
    private void wakeConnectionWaitersLocked() {
        ConnectionWaiter predecessor = null;
        ConnectionWaiter waiter = this.mConnectionWaiterQueue;
        boolean primaryConnectionNotAvailable = false;
        boolean nonPrimaryConnectionNotAvailable = false;
        boolean exclusiveConnectionNotAvailable = false;
        while (waiter != null) {
            boolean unpark = false;
            if (this.mIsOpen) {
                SQLiteConnection connection = null;
                try {
                    if (!(waiter.mWantPrimaryConnection || !waiter.mWantExclusiveConnection || exclusiveConnectionNotAvailable)) {
                        connection = tryAcquireExclusiveConnectionLocked(waiter.mConnectionFlags);
                        if (connection == null) {
                            exclusiveConnectionNotAvailable = true;
                        }
                    }
                    if (!(connection != null || waiter.mWantPrimaryConnection || nonPrimaryConnectionNotAvailable)) {
                        connection = tryAcquireNonPrimaryConnectionLocked(waiter.mSql, waiter.mConnectionFlags);
                        if (connection == null) {
                            nonPrimaryConnectionNotAvailable = true;
                        }
                    }
                    if (connection == null && (((isExclusiveConnectionEnabled() && waiter.mWantPrimaryConnection) || !isExclusiveConnectionEnabled()) && !primaryConnectionNotAvailable)) {
                        connection = tryAcquirePrimaryConnectionLocked(waiter.mConnectionFlags);
                        if (connection == null) {
                            primaryConnectionNotAvailable = true;
                        }
                    }
                    if (connection != null) {
                        waiter.mAssignedConnection = connection;
                        unpark = true;
                    } else if (nonPrimaryConnectionNotAvailable && primaryConnectionNotAvailable && exclusiveConnectionNotAvailable) {
                        return;
                    }
                } catch (RuntimeException ex) {
                    waiter.mException = ex;
                    unpark = true;
                }
            } else {
                unpark = true;
            }
            ConnectionWaiter successor = waiter.mNext;
            if (unpark) {
                if (predecessor != null) {
                    predecessor.mNext = successor;
                } else {
                    this.mConnectionWaiterQueue = successor;
                }
                waiter.mNext = null;
                LockSupport.unpark(waiter.mThread);
            } else {
                predecessor = waiter;
            }
            waiter = successor;
        }
    }

    @GuardedBy("mLock")
    private SQLiteConnection tryAcquirePrimaryConnectionLocked(int connectionFlags) {
        SQLiteConnection connection = this.mAvailablePrimaryConnection;
        if (connection != null) {
            this.mAvailablePrimaryConnection = null;
            finishAcquireConnectionLocked(connection, connectionFlags);
            return connection;
        }
        for (SQLiteConnection acquiredConnection : this.mAcquiredConnections.keySet()) {
            if (acquiredConnection.isPrimaryConnection()) {
                return null;
            }
        }
        connection = openConnectionLocked(this.mConfiguration, true);
        finishAcquireConnectionLocked(connection, connectionFlags);
        return connection;
    }

    @GuardedBy("mLock")
    private SQLiteConnection tryAcquireNonPrimaryConnectionLocked(String sql, int connectionFlags) {
        int i;
        int availableCount = this.mAvailableNonPrimaryConnections.size();
        if (availableCount > 1 && sql != null) {
            for (i = 0; i < availableCount; i++) {
                SQLiteConnection connection = (SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i);
                if (connection.isPreparedStatementInCache(sql)) {
                    this.mAvailableNonPrimaryConnections.remove(i);
                    finishAcquireConnectionLocked(connection, connectionFlags);
                    return connection;
                }
            }
        }
        SQLiteConnection connection2;
        if (availableCount > 0) {
            connection2 = (SQLiteConnection) this.mAvailableNonPrimaryConnections.remove(availableCount - 1);
            finishAcquireConnectionLocked(connection2, connectionFlags);
            return connection2;
        }
        i = this.mAcquiredConnections.size();
        if (this.mAvailablePrimaryConnection != null) {
            i++;
        }
        if (isExclusiveConnectionEnabled()) {
            i++;
        }
        if (i >= this.mMaxConnectionPoolSize) {
            return null;
        }
        connection2 = openConnectionLocked(this.mConfiguration, false);
        finishAcquireConnectionLocked(connection2, connectionFlags);
        return connection2;
    }

    @GuardedBy("mLock")
    private void finishAcquireConnectionLocked(SQLiteConnection connection, int connectionFlags) {
        try {
            connection.setOnlyAllowReadOnlyOperations((connectionFlags & 1) != 0);
            this.mAcquiredConnections.put(connection, AcquiredConnectionStatus.NORMAL);
        } catch (RuntimeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to prepare acquired connection for session, closing it: ");
            stringBuilder.append(connection);
            stringBuilder.append(", connectionFlags=");
            stringBuilder.append(connectionFlags);
            Log.e(TAG, stringBuilder.toString());
            closeConnectionAndLogExceptionsLocked(connection);
            throw ex;
        }
    }

    private boolean isSessionBlockingImportantConnectionWaitersLocked(boolean holdingPrimaryConnection, int connectionFlags) {
        ConnectionWaiter waiter = this.mConnectionWaiterQueue;
        if (waiter != null) {
            int priority = getPriority(connectionFlags);
            while (priority <= waiter.mPriority) {
                if (!holdingPrimaryConnection && waiter.mWantPrimaryConnection) {
                    waiter = waiter.mNext;
                    if (waiter == null) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static int getPriority(int connectionFlags) {
        if ((connectionFlags & 4) != 0) {
            return 2;
        }
        if ((connectionFlags & 8) == 0 || (connectionFlags & 2) == 0) {
            return 0;
        }
        return 1;
    }

    private void setMaxConnectionPoolSizeLocked() {
        if (this.mConfiguration.isInMemoryDb() || (this.mConfiguration.openFlags & 536870912) == 0) {
            this.mMaxConnectionPoolSize = 1;
            return;
        }
        int addedSize = getExtendConnectionCount(this.mConfiguration.path);
        if (!this.mConfiguration.configurationEnhancement) {
            this.mMaxConnectionPoolSize = SQLiteGlobal.getWALConnectionPoolSize();
            if (addedSize > 0) {
                this.mMaxConnectionPoolSize += addedSize;
            }
        } else if (this.mConfiguration.explicitWALEnabled) {
            this.mMaxConnectionPoolSize = SQLiteGlobal.getWALConnectionPoolSize();
            if (addedSize > 0) {
                this.mMaxConnectionPoolSize += addedSize;
            }
        } else {
            this.mMaxConnectionPoolSize = 1;
        }
    }

    @VisibleForTesting
    public void setupIdleConnectionHandler(Looper looper, long timeoutMs) {
        synchronized (this.mLock) {
            this.mIdleConnectionHandler = new IdleConnectionHandler(looper, timeoutMs);
        }
    }

    void disableIdleConnectionHandler() {
        synchronized (this.mLock) {
            this.mIdleConnectionHandler = null;
        }
    }

    private void throwIfClosedLocked() {
        if (!this.mIsOpen) {
            throw new IllegalStateException("Cannot perform this operation because the connection pool has been closed.");
        }
    }

    private ConnectionWaiter obtainConnectionWaiterLocked(Thread thread, long startTime, int priority, boolean wantPrimaryConnection, boolean wantExclusiveConnection, String sql, int connectionFlags) {
        ConnectionWaiter waiter = this.mConnectionWaiterPool;
        if (waiter != null) {
            this.mConnectionWaiterPool = waiter.mNext;
            waiter.mNext = null;
        } else {
            waiter = new ConnectionWaiter();
        }
        waiter.mThread = thread;
        waiter.mStartTime = startTime;
        waiter.mPriority = priority;
        waiter.mWantPrimaryConnection = wantPrimaryConnection;
        waiter.mSql = sql;
        waiter.mConnectionFlags = connectionFlags;
        waiter.mWantExclusiveConnection = wantExclusiveConnection;
        return waiter;
    }

    private void recycleConnectionWaiterLocked(ConnectionWaiter waiter) {
        waiter.mNext = this.mConnectionWaiterPool;
        waiter.mThread = null;
        waiter.mSql = null;
        waiter.mAssignedConnection = null;
        waiter.mException = null;
        waiter.mNonce++;
        this.mConnectionWaiterPool = waiter;
    }

    public void dump(Printer printer, boolean verbose) {
        Printer indentedPrinter = PrefixPrinter.create(printer, "    ");
        synchronized (this.mLock) {
            int count;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Connection pool for ");
            stringBuilder.append(this.mConfiguration.path);
            stringBuilder.append(":");
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Open: ");
            stringBuilder.append(this.mIsOpen);
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Max connections: ");
            stringBuilder.append(this.mMaxConnectionPoolSize);
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Total execution time: ");
            stringBuilder.append(this.mTotalExecutionTimeCounter);
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Configuration: openFlags=");
            stringBuilder.append(this.mConfiguration.openFlags);
            stringBuilder.append(", useCompatibilityWal=");
            stringBuilder.append(this.mConfiguration.useCompatibilityWal());
            stringBuilder.append(", journalMode=");
            stringBuilder.append(TextUtils.emptyIfNull(this.mConfiguration.journalMode));
            stringBuilder.append(", syncMode=");
            stringBuilder.append(TextUtils.emptyIfNull(this.mConfiguration.syncMode));
            printer.println(stringBuilder.toString());
            if (SQLiteCompatibilityWalFlags.areFlagsSet()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Compatibility WAL settings: compatibility_wal_supported=");
                stringBuilder.append(SQLiteCompatibilityWalFlags.isCompatibilityWalSupported());
                stringBuilder.append(", wal_syncmode=");
                stringBuilder.append(SQLiteCompatibilityWalFlags.getWALSyncMode());
                printer.println(stringBuilder.toString());
            }
            if (this.mConfiguration.isLookasideConfigSet()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Lookaside config: sz=");
                stringBuilder.append(this.mConfiguration.lookasideSlotSize);
                stringBuilder.append(" cnt=");
                stringBuilder.append(this.mConfiguration.lookasideSlotCount);
                printer.println(stringBuilder.toString());
            }
            if (this.mConfiguration.idleConnectionTimeoutMs != Long.MAX_VALUE) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Idle connection timeout: ");
                stringBuilder.append(this.mConfiguration.idleConnectionTimeoutMs);
                printer.println(stringBuilder.toString());
            }
            printer.println("  Available primary connection:");
            stringBuilder = new StringBuilder();
            stringBuilder.append("  configurationEnhancement:");
            stringBuilder.append(this.mConfiguration.configurationEnhancement);
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  defaultWALEnabled:");
            stringBuilder.append(this.mConfiguration.defaultWALEnabled);
            printer.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  explicitWALEnabled:");
            stringBuilder.append(this.mConfiguration.explicitWALEnabled);
            printer.println(stringBuilder.toString());
            if (this.mAvailablePrimaryConnection != null) {
                this.mAvailablePrimaryConnection.dump(indentedPrinter, verbose);
            } else {
                indentedPrinter.println("<none>");
            }
            if (this.mAvailableExclusiveConnection != null) {
                printer.println("  Available Exclusive connection:");
                this.mAvailableExclusiveConnection.dump(indentedPrinter, verbose);
            }
            printer.println("  Available non-primary connections:");
            if (this.mAvailableNonPrimaryConnections.isEmpty()) {
                indentedPrinter.println("<none>");
            } else {
                count = this.mAvailableNonPrimaryConnections.size();
                for (int i = 0; i < count; i++) {
                    ((SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i)).dump(indentedPrinter, verbose);
                }
            }
            printer.println("  Acquired connections:");
            if (this.mAcquiredConnections.isEmpty()) {
                indentedPrinter.println("<none>");
            } else {
                for (Entry<SQLiteConnection, AcquiredConnectionStatus> entry : this.mAcquiredConnections.entrySet()) {
                    ((SQLiteConnection) entry.getKey()).dumpUnsafe(indentedPrinter, verbose);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Status: ");
                    stringBuilder2.append(entry.getValue());
                    indentedPrinter.println(stringBuilder2.toString());
                }
            }
            printer.println("  Connection waiters:");
            if (this.mConnectionWaiterQueue != null) {
                count = 0;
                long now = SystemClock.uptimeMillis();
                ConnectionWaiter waiter = this.mConnectionWaiterQueue;
                while (waiter != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(count);
                    stringBuilder3.append(": waited for ");
                    stringBuilder3.append(((float) (now - waiter.mStartTime)) * 0.001f);
                    stringBuilder3.append(" ms - thread=");
                    stringBuilder3.append(waiter.mThread);
                    stringBuilder3.append(", priority=");
                    stringBuilder3.append(waiter.mPriority);
                    stringBuilder3.append(", sql='");
                    stringBuilder3.append(waiter.mSql);
                    stringBuilder3.append("'");
                    indentedPrinter.println(stringBuilder3.toString());
                    waiter = waiter.mNext;
                    count++;
                }
            } else {
                indentedPrinter.println("<none>");
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLiteConnectionPool: ");
        stringBuilder.append(this.mConfiguration.path);
        return stringBuilder.toString();
    }

    private int getExtendConnectionCount(String databasePath) {
        if ("/data/user/0/com.android.providers.media/databases/external.db".equals(this.mConfiguration.path)) {
            return 2;
        }
        if ("/data/user/0/com.android.gallery3d/databases/gallery.db".equals(this.mConfiguration.path)) {
            return 4;
        }
        return 0;
    }

    public void setExclusiveConnectionEnabled(boolean enabled) {
        this.mEnableExclusiveConnection = false;
        if (this.mConfiguration.configurationEnhancement) {
            if ((this.mConfiguration.openFlags & 536870912) != 0 && this.mConfiguration.explicitWALEnabled) {
                this.mEnableExclusiveConnection = enabled;
            }
        } else if ((this.mConfiguration.openFlags & 536870912) != 0) {
            this.mEnableExclusiveConnection = enabled;
        }
    }

    private boolean isExclusiveConnectionEnabled() {
        boolean z = false;
        if (this.mConfiguration.configurationEnhancement) {
            if ((this.mConfiguration.openFlags & 536870912) != 0 && this.mConfiguration.explicitWALEnabled && this.mEnableExclusiveConnection) {
                z = true;
            }
            return z;
        }
        if ((this.mConfiguration.openFlags & 536870912) != 0 && this.mEnableExclusiveConnection) {
            z = true;
        }
        return z;
    }

    private int getMaxNonPrimaryConnectionSizeLocked() {
        int maxCount = this.mMaxConnectionPoolSize - 1;
        return isExclusiveConnectionEnabled() ? maxCount - 1 : maxCount;
    }

    private SQLiteConnection tryAcquireExclusiveConnectionLocked(int connectionFlags) {
        SQLiteConnection connection = this.mAvailableExclusiveConnection;
        if (connection != null) {
            this.mAvailableExclusiveConnection = null;
            finishAcquireConnectionLocked(connection, connectionFlags);
            return connection;
        }
        for (SQLiteConnection acquiredConnection : this.mAcquiredConnections.keySet()) {
            if (acquiredConnection.isExclusiveConnection()) {
                return null;
            }
        }
        connection = openExclusiveConnectionLocked(this.mConfiguration);
        finishAcquireConnectionLocked(connection, connectionFlags);
        return connection;
    }

    private SQLiteConnection openExclusiveConnectionLocked(SQLiteDatabaseConfiguration configuration) {
        int connectionId = this.mNextConnectionId;
        this.mNextConnectionId = connectionId + 1;
        return SQLiteConnection.openExclusive(this, configuration, connectionId);
    }
}
