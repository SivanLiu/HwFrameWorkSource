package com.huawei.hwsqlite;

public class SQLiteReadOnlyDatabaseException extends SQLiteException {
    public SQLiteReadOnlyDatabaseException(String error) {
        super(error);
    }
}
