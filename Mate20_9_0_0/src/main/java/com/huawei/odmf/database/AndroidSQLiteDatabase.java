package com.huawei.odmf.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.CancellationSignal;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.exception.ODMFSQLiteDatabaseCorruptException;
import com.huawei.odmf.exception.ODMFSQLiteDiskIOException;
import com.huawei.odmf.exception.ODMFUnsupportedOperationException;
import com.huawei.odmf.utils.LOG;

public class AndroidSQLiteDatabase implements DataBase {
    private SQLiteDatabase mSQLiteDatabase = null;

    public AndroidSQLiteDatabase(SQLiteDatabase db) {
        this.mSQLiteDatabase = db;
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return this.mSQLiteDatabase.rawQuery(sql, selectionArgs);
    }

    public void execSQL(String sql) throws SQLException {
        this.mSQLiteDatabase.execSQL(sql);
    }

    public void beginTransaction() {
        SQLException e;
        try {
            this.mSQLiteDatabase.beginTransaction();
        } catch (SQLiteDatabaseCorruptException e2) {
            e = e2;
            LOG.logE("Begin Transaction failed : A SQLiteDatabaseCorruptException occurred when begin transaction.");
            throw new ODMFSQLiteDatabaseCorruptException("Close database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
            e = e3;
            LOG.logE("Begin Transaction failed : A SQLiteDatabaseCorruptException occurred when begin transaction.");
            throw new ODMFSQLiteDatabaseCorruptException("Close database failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException e4) {
            e = e4;
            LOG.logE("Begin Transaction failed : A SQLiteDiskIOException occurred when begin transaction..");
            throw new ODMFSQLiteDiskIOException("Close database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e5) {
            e = e5;
            LOG.logE("Begin Transaction failed : A SQLiteDiskIOException occurred when begin transaction..");
            throw new ODMFSQLiteDiskIOException("Close database failed : " + e.getMessage(), e);
        } catch (RuntimeException e6) {
            LOG.logE("Begin Transaction failed : A RuntimeException occurred when begin transaction.");
            throw new ODMFRuntimeException("Close database failed : " + e6.getMessage(), e6);
        }
    }

    public void endTransaction() {
        endTransaction(false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0033  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void endTransaction(boolean hasCorruptException) {
        SQLException e;
        try {
            this.mSQLiteDatabase.endTransaction();
            return;
        } catch (SQLiteDatabaseCorruptException e2) {
            e = e2;
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
            e = e3;
        } catch (SQLiteDiskIOException e4) {
            e = e4;
            LOG.logE("End Transaction failed : A SQLiteDiskIOException occurred when end transaction.");
            if (hasCorruptException) {
                throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
            }
            return;
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e5) {
            e = e5;
            LOG.logE("End Transaction failed : A SQLiteDiskIOException occurred when end transaction.");
            if (hasCorruptException) {
            }
        } catch (RuntimeException e6) {
            LOG.logE("End Transaction failed : A RuntimeException occurred when end transaction.");
            if (!hasCorruptException) {
                throw new ODMFRuntimeException("End Transaction failed : " + e6.getMessage(), e6);
            }
            return;
        }
        LOG.logE("End Transaction failed : A SQLiteDatabaseCorruptException occurred when end transaction.");
        if (!hasCorruptException) {
            throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
        }
    }

    public boolean inTransaction() {
        return this.mSQLiteDatabase.inTransaction();
    }

    public void setTransactionSuccessful() {
        this.mSQLiteDatabase.setTransactionSuccessful();
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        this.mSQLiteDatabase.execSQL(sql, bindArgs);
    }

    public Statement compileStatement(String sql) {
        return new AndroidSQLiteStatement(this.mSQLiteDatabase.compileStatement(sql));
    }

    public boolean isDbLockedByCurrentThread() {
        return this.mSQLiteDatabase.isDbLockedByCurrentThread();
    }

    public void close() {
        this.mSQLiteDatabase.close();
    }

    public boolean isOpen() {
        return this.mSQLiteDatabase.isOpen();
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        return this.mSQLiteDatabase.delete(table, whereClause, whereArgs);
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return this.mSQLiteDatabase.insertOrThrow(table, nullColumnHack, values);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor commonquery(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return this.mSQLiteDatabase.update(table, values, whereClause, whereArgs);
    }

    public String getPath() {
        return this.mSQLiteDatabase.getPath();
    }

    public SQLiteDatabase getAndroidSQLiteDatabase() {
        return this.mSQLiteDatabase;
    }

    public com.huawei.hwsqlite.SQLiteDatabase getODMFSQLiteDatabase() {
        return null;
    }

    public void resetDatabaseEncryptKey(byte[] oldKey, byte[] newKey) {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support changing key");
    }

    public void addAttachAlias(String alias, String path, byte[] key) throws SQLException {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support attaching a database");
    }

    public void removeAttachAlias(String alias) {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support  removing  a database");
    }

    public Cursor rawSelect(String sql, String[] selectionArgs, CancellationSignal cancellationSignal, boolean stepQuery) {
        return this.mSQLiteDatabase.rawQuery(sql, selectionArgs, cancellationSignal);
    }
}
