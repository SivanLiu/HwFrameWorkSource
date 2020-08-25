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

    @Override // com.huawei.odmf.database.DataBase
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return this.mSQLiteDatabase.rawQuery(sql, selectionArgs);
    }

    @Override // com.huawei.odmf.database.DataBase
    public void execSQL(String sql) throws SQLException {
        this.mSQLiteDatabase.execSQL(sql);
    }

    @Override // com.huawei.odmf.database.DataBase
    public void beginTransaction() {
        try {
            this.mSQLiteDatabase.beginTransaction();
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e) {
            LOG.logE("Begin Transaction failed : A SQLiteDatabaseCorruptException occurred when begin transaction.");
            throw new ODMFSQLiteDatabaseCorruptException("Close database failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e2) {
            LOG.logE("Begin Transaction failed : A SQLiteDiskIOException occurred when begin transaction..");
            throw new ODMFSQLiteDiskIOException("Close database failed : " + e2.getMessage(), e2);
        } catch (RuntimeException e3) {
            LOG.logE("Begin Transaction failed : A RuntimeException occurred when begin transaction.");
            throw new ODMFRuntimeException("Close database failed : " + e3.getMessage(), e3);
        }
    }

    @Override // com.huawei.odmf.database.DataBase
    public void endTransaction() {
        endTransaction(false);
    }

    @Override // com.huawei.odmf.database.DataBase
    public void endTransaction(boolean hasCorruptException) {
        try {
            this.mSQLiteDatabase.endTransaction();
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e) {
            LOG.logE("End Transaction failed : A SQLiteDatabaseCorruptException occurred when end transaction.");
            if (!hasCorruptException) {
                throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
            }
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e2) {
            LOG.logE("End Transaction failed : A SQLiteDiskIOException occurred when end transaction.");
            if (!hasCorruptException) {
                throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e2.getMessage(), e2);
            }
        } catch (RuntimeException e3) {
            LOG.logE("End Transaction failed : A RuntimeException occurred when end transaction.");
            if (!hasCorruptException) {
                throw new ODMFRuntimeException("End Transaction failed : " + e3.getMessage(), e3);
            }
        }
    }

    @Override // com.huawei.odmf.database.DataBase
    public boolean inTransaction() {
        return this.mSQLiteDatabase.inTransaction();
    }

    @Override // com.huawei.odmf.database.DataBase
    public void setTransactionSuccessful() {
        this.mSQLiteDatabase.setTransactionSuccessful();
    }

    @Override // com.huawei.odmf.database.DataBase
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        this.mSQLiteDatabase.execSQL(sql, bindArgs);
    }

    @Override // com.huawei.odmf.database.DataBase
    public Statement compileStatement(String sql) {
        return new AndroidSQLiteStatement(this.mSQLiteDatabase.compileStatement(sql));
    }

    @Override // com.huawei.odmf.database.DataBase
    public boolean isDbLockedByCurrentThread() {
        return this.mSQLiteDatabase.isDbLockedByCurrentThread();
    }

    @Override // com.huawei.odmf.database.DataBase
    public void close() {
        this.mSQLiteDatabase.close();
    }

    @Override // com.huawei.odmf.database.DataBase
    public boolean isOpen() {
        return this.mSQLiteDatabase.isOpen();
    }

    @Override // com.huawei.odmf.database.DataBase
    public int delete(String table, String whereClause, String[] whereArgs) {
        return this.mSQLiteDatabase.delete(table, whereClause, whereArgs);
    }

    @Override // com.huawei.odmf.database.DataBase
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return this.mSQLiteDatabase.insertOrThrow(table, nullColumnHack, values);
    }

    @Override // com.huawei.odmf.database.DataBase
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override // com.huawei.odmf.database.DataBase
    public Cursor commonquery(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override // com.huawei.odmf.database.DataBase
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return this.mSQLiteDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override // com.huawei.odmf.database.DataBase
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return this.mSQLiteDatabase.update(table, values, whereClause, whereArgs);
    }

    @Override // com.huawei.odmf.database.DataBase
    public String getPath() {
        return this.mSQLiteDatabase.getPath();
    }

    @Override // com.huawei.odmf.database.DataBase
    public SQLiteDatabase getAndroidSQLiteDatabase() {
        return this.mSQLiteDatabase;
    }

    @Override // com.huawei.odmf.database.DataBase
    public com.huawei.hwsqlite.SQLiteDatabase getODMFSQLiteDatabase() {
        return null;
    }

    @Override // com.huawei.odmf.database.DataBase
    public void resetDatabaseEncryptKey(byte[] oldKey, byte[] newKey) {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support changing key");
    }

    @Override // com.huawei.odmf.database.DataBase
    public void addAttachAlias(String alias, String path, byte[] key) throws SQLException {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support attaching a database");
    }

    @Override // com.huawei.odmf.database.DataBase
    public void removeAttachAlias(String alias) {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support removing a database");
    }

    @Override // com.huawei.odmf.database.DataBase
    public Cursor rawSelect(String sql, String[] selectionArgs, CancellationSignal cancellationSignal, boolean stepQuery) {
        return this.mSQLiteDatabase.rawQuery(sql, selectionArgs, cancellationSignal);
    }

    @Override // com.huawei.odmf.database.DataBase
    public String[] getSQLTables(String sql, int rwFlags, CancellationSignal cancellationSignal) {
        throw new ODMFUnsupportedOperationException("Android SQLiteDatabase does not support to get tables in a sql.");
    }
}
