package com.huawei.odmf.database;

import android.database.sqlite.SQLiteStatement;

public class AndroidSQLiteStatement implements Statement {
    private SQLiteStatement mSQLiteStatement;

    public AndroidSQLiteStatement(SQLiteStatement statement) {
        this.mSQLiteStatement = statement;
    }

    @Override // com.huawei.odmf.database.Statement
    public long executeInsert() {
        return this.mSQLiteStatement.executeInsert();
    }

    @Override // com.huawei.odmf.database.Statement
    public void execute() {
        this.mSQLiteStatement.execute();
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindLong(int index, long value) {
        this.mSQLiteStatement.bindLong(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void clearBindings() {
        this.mSQLiteStatement.clearBindings();
    }

    @Override // com.huawei.odmf.database.Statement
    public void executeUpdateDelete() {
        this.mSQLiteStatement.executeUpdateDelete();
    }

    @Override // com.huawei.odmf.database.Statement
    public void close() {
        this.mSQLiteStatement.close();
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindNull(int index) {
        this.mSQLiteStatement.bindNull(index);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindDouble(int index, double value) {
        this.mSQLiteStatement.bindDouble(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindString(int index, String value) {
        this.mSQLiteStatement.bindString(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindBlob(int index, byte[] value) {
        this.mSQLiteStatement.bindBlob(index, value);
    }

    public String toString() {
        return "AndroidSQLiteStatement{mSQLiteStatement=" + this.mSQLiteStatement + '}';
    }
}
