package com.huawei.odmf.database;

import com.huawei.hwsqlite.SQLiteStatement;

public class ODMFSQLiteStatement implements Statement {
    private SQLiteStatement mODMFStatement;

    public ODMFSQLiteStatement(SQLiteStatement statement) {
        this.mODMFStatement = statement;
    }

    @Override // com.huawei.odmf.database.Statement
    public long executeInsert() {
        return this.mODMFStatement.executeInsert();
    }

    @Override // com.huawei.odmf.database.Statement
    public void execute() {
        this.mODMFStatement.execute();
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindLong(int index, long value) {
        this.mODMFStatement.bindLong(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void clearBindings() {
        this.mODMFStatement.clearBindings();
    }

    @Override // com.huawei.odmf.database.Statement
    public void executeUpdateDelete() {
        this.mODMFStatement.executeUpdateDelete();
    }

    @Override // com.huawei.odmf.database.Statement
    public void close() {
        this.mODMFStatement.close();
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindNull(int index) {
        this.mODMFStatement.bindNull(index);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindDouble(int index, double value) {
        this.mODMFStatement.bindDouble(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindString(int index, String value) {
        this.mODMFStatement.bindString(index, value);
    }

    @Override // com.huawei.odmf.database.Statement
    public void bindBlob(int index, byte[] value) {
        this.mODMFStatement.bindBlob(index, value);
    }

    public String toString() {
        return "ODMFSQLiteStatement{mODMFStatement=" + this.mODMFStatement + '}';
    }
}
