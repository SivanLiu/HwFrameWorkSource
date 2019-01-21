package com.huawei.hwsqlite;

public class SQLiteDatabaseLockedException extends SQLiteException {
    public SQLiteDatabaseLockedException(String error) {
        super(error);
    }
}
