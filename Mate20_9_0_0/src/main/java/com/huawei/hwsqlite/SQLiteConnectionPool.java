package com.huawei.hwsqlite;

import android.annotation.SuppressLint;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Printer;
import com.huawei.hwsqlite.SQLiteDebug.DbStats;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public final class SQLiteConnectionPool implements Closeable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int CONNECTION_FLAG_INTERACTIVE = 4;
    public static final int CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY = 2;
    public static final int CONNECTION_FLAG_READ_ONLY = 1;
    private static final long CONNECTION_POOL_BUSY_MILLIS = 30000;
    private static final String TAG = "SQLiteConnectionPool";
    private final WeakHashMap<SQLiteConnection, AcquiredConnectionStatus> mAcquiredConnections = new WeakHashMap();
    private final ArrayList<SQLiteConnection> mAvailableNonPrimaryConnections = new ArrayList();
    private SQLiteConnection mAvailablePrimaryConnection;
    private final SQLiteCloseGuard mCloseGuard = SQLiteCloseGuard.get();
    private final SQLiteDatabaseConfiguration mConfiguration;
    private final AtomicBoolean mConnectionLeaked = new AtomicBoolean();
    private ConnectionWaiter mConnectionWaiterPool;
    private ConnectionWaiter mConnectionWaiterQueue;
    private boolean mIsOpen;
    private final Object mLock = new Object();
    private int mMaxConnectionPoolSize;
    private int mNextConnectionId;

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
        public boolean mWantPrimaryConnection;

        private ConnectionWaiter() {
        }

        /* synthetic */ ConnectionWaiter(AnonymousClass1 x0) {
            this();
        }
    }

    private SQLiteConnectionPool(SQLiteDatabaseConfiguration configuration) {
        this.mConfiguration = new SQLiteDatabaseConfiguration(configuration);
        setMaxConnectionPoolSizeLocked();
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static SQLiteConnectionPool open(SQLiteDatabaseConfiguration configuration) {
        if (configuration != null) {
            SQLiteConnectionPool pool = new SQLiteConnectionPool(configuration);
            pool.open();
            return pool;
        }
        throw new IllegalArgumentException("configuration must not be null.");
    }

    private void open() {
        this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
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
                boolean foreignKeyModeChanged = false;
                boolean walModeChanged = ((configuration.openFlags ^ this.mConfiguration.openFlags) & 536870912) != 0;
                if (walModeChanged) {
                    if (this.mAcquiredConnections.isEmpty()) {
                        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
                    } else {
                        throw new IllegalStateException("Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
                    }
                }
                if (configuration.foreignKeyConstraintsEnabled != this.mConfiguration.foreignKeyConstraintsEnabled) {
                    foreignKeyModeChanged = true;
                }
                if (foreignKeyModeChanged) {
                    if (!this.mAcquiredConnections.isEmpty()) {
                        throw new IllegalStateException("Foreign Key Constraints cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.");
                    }
                }
                if (this.mConfiguration.openFlags != configuration.openFlags) {
                    if (walModeChanged) {
                        closeAvailableConnectionsAndLogExceptionsLocked();
                    }
                    SQLiteConnection newPrimaryConnection = openConnectionLocked(configuration, true);
                    closeAvailableConnectionsAndLogExceptionsLocked();
                    discardAcquiredConnectionsLocked();
                    this.mAvailablePrimaryConnection = newPrimaryConnection;
                    this.mConfiguration.updateParametersFrom(configuration);
                    setMaxConnectionPoolSizeLocked();
                } else {
                    this.mConfiguration.updateParametersFrom(configuration);
                    setMaxConnectionPoolSizeLocked();
                    closeExcessConnectionsAndLogExceptionsLocked();
                    reconfigureAllConnectionsLocked();
                }
                wakeConnectionWaitersLocked();
            }
            return;
        }
        throw new IllegalArgumentException("configuration must not be null.");
    }

    public void changeEncryptKey(SQLiteEncryptKeyLoader newKeyLoader) throws SQLiteException {
        synchronized (this.mLock) {
            throwIfClosedLocked();
            if (this.mAcquiredConnections.isEmpty()) {
                closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
                if (this.mAvailablePrimaryConnection == null) {
                    this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
                }
                this.mAvailablePrimaryConnection.changeEncryptKey(newKeyLoader);
                this.mConfiguration.updateEncryptKeyLoader(newKeyLoader);
                wakeConnectionWaitersLocked();
            } else {
                throw new SQLiteBusyException("The encryptKey cannot be changed while there are transactions in progress.Finish all transactions and release all active database connections first.");
            }
        }
    }

    public void addAttachAlias(SQLiteAttached attached) throws SQLiteException {
        if (attached != null) {
            synchronized (this.mLock) {
                throwIfClosedLocked();
                if (this.mAcquiredConnections.isEmpty()) {
                    closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
                    if (this.mAvailablePrimaryConnection == null) {
                        this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
                    }
                    this.mAvailablePrimaryConnection.addAttachAlias(attached);
                    this.mConfiguration.addAttachAlias(attached);
                    wakeConnectionWaitersLocked();
                } else {
                    throw new SQLiteBusyException("Attached alias cannot be added while there are transactions in progress. Finish all transactions and release all active database connections first.");
                }
            }
            return;
        }
        throw new IllegalArgumentException("attached parameter must not be null");
    }

    public void removeAttachedAlias(String alias) {
        if (alias == null || alias.length() == 0) {
            throw new IllegalArgumentException("Alias name must not be empty");
        }
        synchronized (this.mLock) {
            throwIfClosedLocked();
            if (this.mAcquiredConnections.isEmpty()) {
                closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
                if (this.mAvailablePrimaryConnection == null) {
                    this.mAvailablePrimaryConnection = openConnectionLocked(this.mConfiguration, true);
                }
                this.mAvailablePrimaryConnection.removeAttachAlias(alias);
                this.mConfiguration.removeAttachAlias(alias);
                wakeConnectionWaitersLocked();
            } else {
                throw new SQLiteBusyException("Detach cannot be done while there are transactions in progress. Finish all transactions and release all active database connections first.");
            }
        }
    }

    public SQLiteConnection acquireConnection(String sql, int connectionFlags, CancellationSignal cancellationSignal) {
        return waitForConnection(sql, connectionFlags, cancellationSignal);
    }

    public void releaseConnection(SQLiteConnection connection) {
        synchronized (this.mLock) {
            AcquiredConnectionStatus status = (AcquiredConnectionStatus) this.mAcquiredConnections.remove(connection);
            if (status != null) {
                if (!this.mIsOpen) {
                    closeConnectionAndLogExceptionsLocked(connection);
                } else if (connection.isPrimaryConnection()) {
                    if (recycleConnectionLocked(connection, status)) {
                        this.mAvailablePrimaryConnection = connection;
                    }
                    wakeConnectionWaitersLocked();
                } else if (this.mAvailableNonPrimaryConnections.size() >= this.mMaxConnectionPoolSize - 1) {
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

    private void closeAvailableConnectionsAndLogExceptionsLocked() {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked();
        if (this.mAvailablePrimaryConnection != null) {
            closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
            this.mAvailablePrimaryConnection = null;
        }
    }

    private void closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked() {
        int count = this.mAvailableNonPrimaryConnections.size();
        for (int i = 0; i < count; i++) {
            closeConnectionAndLogExceptionsLocked((SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i));
        }
        this.mAvailableNonPrimaryConnections.clear();
    }

    private void closeExcessConnectionsAndLogExceptionsLocked() {
        int availableCount = this.mAvailableNonPrimaryConnections.size();
        while (true) {
            int availableCount2 = availableCount - 1;
            if (availableCount > this.mMaxConnectionPoolSize - 1) {
                closeConnectionAndLogExceptionsLocked((SQLiteConnection) this.mAvailableNonPrimaryConnections.remove(availableCount2));
                availableCount = availableCount2;
            } else {
                return;
            }
        }
    }

    private void closeConnectionAndLogExceptionsLocked(SQLiteConnection connection) {
        try {
            connection.close();
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

    private void reconfigureAllConnectionsLocked() {
        if (this.mAvailablePrimaryConnection != null) {
            try {
                this.mAvailablePrimaryConnection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to reconfigure available primary connection, closing it: ");
                stringBuilder.append(this.mAvailablePrimaryConnection);
                Log.e(str, stringBuilder.toString(), ex);
                closeConnectionAndLogExceptionsLocked(this.mAvailablePrimaryConnection);
                this.mAvailablePrimaryConnection = null;
            }
        }
        int count = this.mAvailableNonPrimaryConnections.size();
        int i = 0;
        while (i < count) {
            SQLiteConnection connection = (SQLiteConnection) this.mAvailableNonPrimaryConnections.get(i);
            try {
                connection.reconfigure(this.mConfiguration);
            } catch (RuntimeException ex2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to reconfigure available non-primary connection, closing it: ");
                stringBuilder2.append(connection);
                Log.e(str2, stringBuilder2.toString(), ex2);
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

    /* JADX WARNING: Missing block: B:35:0x0067, code skipped:
            if (r11 == null) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:36:0x0069, code skipped:
            r11.setOnCancelListener(new com.huawei.hwsqlite.SQLiteConnectionPool.AnonymousClass1(r9));
     */
    /* JADX WARNING: Missing block: B:37:0x0071, code skipped:
            r3 = CONNECTION_POOL_BUSY_MILLIS;
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            r6 = r1.mStartTime + CONNECTION_POOL_BUSY_MILLIS;
     */
    /* JADX WARNING: Missing block: B:41:0x007d, code skipped:
            if (r9.mConnectionLeaked.compareAndSet(r13, false) == false) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:43:?, code skipped:
            r12 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:44:0x0081, code skipped:
            monitor-enter(r12);
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            wakeConnectionWaitersLocked();
     */
    /* JADX WARNING: Missing block: B:47:0x0085, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:53:0x008a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:54:0x008b, code skipped:
            r17 = r14;
     */
    /* JADX WARNING: Missing block: B:55:0x008e, code skipped:
            r17 = r14;
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            java.util.concurrent.locks.LockSupport.parkNanos(r9, r3 * 1000000);
            java.lang.Thread.interrupted();
            r12 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:58:0x009d, code skipped:
            monitor-enter(r12);
     */
    /* JADX WARNING: Missing block: B:60:?, code skipped:
            throwIfClosedLocked();
            r0 = r1.mAssignedConnection;
            r13 = r1.mException;
     */
    /* JADX WARNING: Missing block: B:61:0x00a5, code skipped:
            if (r0 != null) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:62:0x00a7, code skipped:
            if (r13 == null) goto L_0x00ac;
     */
    /* JADX WARNING: Missing block: B:63:0x00a9, code skipped:
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:64:0x00ac, code skipped:
            r14 = android.os.SystemClock.uptimeMillis();
     */
    /* JADX WARNING: Missing block: B:66:0x00b2, code skipped:
            if (r14 >= r6) goto L_0x00b7;
     */
    /* JADX WARNING: Missing block: B:67:0x00b4, code skipped:
            r3 = r14 - r6;
     */
    /* JADX WARNING: Missing block: B:68:0x00b7, code skipped:
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:70:?, code skipped:
            logConnectionPoolBusyLocked(r14 - r1.mStartTime, r10);
     */
    /* JADX WARNING: Missing block: B:71:0x00c0, code skipped:
            r3 = CONNECTION_POOL_BUSY_MILLIS;
            r6 = r14 + CONNECTION_POOL_BUSY_MILLIS;
     */
    /* JADX WARNING: Missing block: B:73:?, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:74:0x00c5, code skipped:
            r14 = r17;
            r13 = true;
     */
    /* JADX WARNING: Missing block: B:75:0x00c9, code skipped:
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:77:?, code skipped:
            recycleConnectionWaiterLocked(r1);
     */
    /* JADX WARNING: Missing block: B:78:0x00ce, code skipped:
            if (r0 == null) goto L_0x00d8;
     */
    /* JADX WARNING: Missing block: B:79:0x00d0, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:80:0x00d1, code skipped:
            if (r11 == null) goto L_0x00d7;
     */
    /* JADX WARNING: Missing block: B:81:0x00d3, code skipped:
            r11.setOnCancelListener(null);
     */
    /* JADX WARNING: Missing block: B:82:0x00d7, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:84:?, code skipped:
            throw r13;
     */
    /* JADX WARNING: Missing block: B:85:0x00d9, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:86:0x00da, code skipped:
            r6 = r18;
     */
    /* JADX WARNING: Missing block: B:87:0x00dd, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:88:0x00de, code skipped:
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:90:?, code skipped:
            monitor-exit(r12);
     */
    /* JADX WARNING: Missing block: B:92:?, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:93:0x00e2, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:94:0x00e4, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:95:0x00e6, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:96:0x00e7, code skipped:
            r17 = r14;
     */
    /* JADX WARNING: Missing block: B:97:0x00e9, code skipped:
            if (r11 != null) goto L_0x00eb;
     */
    /* JADX WARNING: Missing block: B:98:0x00eb, code skipped:
            r11.setOnCancelListener(null);
     */
    /* JADX WARNING: Missing block: B:99:0x00ef, code skipped:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SQLiteConnection waitForConnection(String sql, int connectionFlags, CancellationSignal cancellationSignal) {
        Throwable th;
        boolean z;
        int i = connectionFlags;
        CancellationSignal cancellationSignal2 = cancellationSignal;
        boolean z2 = true;
        boolean wantPrimaryConnection = (i & 2) != 0;
        synchronized (this.mLock) {
            try {
                throwIfClosedLocked();
                if (cancellationSignal2 != null) {
                    try {
                        cancellationSignal.throwIfCanceled();
                    } catch (Throwable th2) {
                        th = th2;
                        z = wantPrimaryConnection;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        throw th;
                    }
                }
                SQLiteConnection connection = null;
                if (!wantPrimaryConnection) {
                    connection = tryAcquireNonPrimaryConnectionLocked(sql, connectionFlags);
                }
                if (connection == null) {
                    connection = tryAcquirePrimaryConnectionLocked(i);
                }
                if (connection != null) {
                    return connection;
                }
                int priority = getPriority(connectionFlags);
                int priority2 = priority;
                final ConnectionWaiter waiter = obtainConnectionWaiterLocked(Thread.currentThread(), SystemClock.uptimeMillis(), priority, wantPrimaryConnection, sql, i);
                ConnectionWaiter predecessor = null;
                for (ConnectionWaiter successor = this.mConnectionWaiterQueue; successor != null; successor = successor.mNext) {
                    if (priority2 > successor.mPriority) {
                        waiter.mNext = successor;
                        break;
                    }
                    predecessor = successor;
                }
                if (predecessor != null) {
                    predecessor.mNext = waiter;
                } else {
                    this.mConnectionWaiterQueue = waiter;
                }
                final int nonce = waiter.mNonce;
            } catch (Throwable th4) {
                th = th4;
                z = wantPrimaryConnection;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

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

    @SuppressLint({"PreferForInArrayList"})
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

    private void wakeConnectionWaitersLocked() {
        ConnectionWaiter predecessor = null;
        ConnectionWaiter waiter = this.mConnectionWaiterQueue;
        boolean primaryConnectionNotAvailable = false;
        boolean nonPrimaryConnectionNotAvailable = false;
        while (waiter != null) {
            boolean unpark = false;
            if (this.mIsOpen) {
                SQLiteConnection connection = null;
                try {
                    if (!(waiter.mWantPrimaryConnection || nonPrimaryConnectionNotAvailable)) {
                        connection = tryAcquireNonPrimaryConnectionLocked(waiter.mSql, waiter.mConnectionFlags);
                        if (connection == null) {
                            nonPrimaryConnectionNotAvailable = true;
                        }
                    }
                    if (connection == null && !primaryConnectionNotAvailable) {
                        connection = tryAcquirePrimaryConnectionLocked(waiter.mConnectionFlags);
                        if (connection == null) {
                            primaryConnectionNotAvailable = true;
                        }
                    }
                    if (connection != null) {
                        waiter.mAssignedConnection = connection;
                        unpark = true;
                    } else if (nonPrimaryConnectionNotAvailable && primaryConnectionNotAvailable) {
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
        if (i >= this.mMaxConnectionPoolSize) {
            return null;
        }
        connection2 = openConnectionLocked(this.mConfiguration, false);
        finishAcquireConnectionLocked(connection2, connectionFlags);
        return connection2;
    }

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
        return (connectionFlags & 4) != 0 ? 1 : 0;
    }

    private void setMaxConnectionPoolSizeLocked() {
        if ((this.mConfiguration.openFlags & 536870912) == 0) {
            this.mMaxConnectionPoolSize = 1;
        } else if (this.mConfiguration.maxConnectionCount == 0) {
            this.mMaxConnectionPoolSize = SQLiteGlobal.getWALConnectionPoolSize();
        } else {
            this.mMaxConnectionPoolSize = this.mConfiguration.maxConnectionCount;
        }
    }

    private void throwIfClosedLocked() {
        if (!this.mIsOpen) {
            throw new IllegalStateException("Cannot perform this operation because the connection pool has been closed.");
        }
    }

    private ConnectionWaiter obtainConnectionWaiterLocked(Thread thread, long startTime, int priority, boolean wantPrimaryConnection, String sql, int connectionFlags) {
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
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLiteConnectionPool: ");
        stringBuilder.append(this.mConfiguration.path);
        return stringBuilder.toString();
    }
}
