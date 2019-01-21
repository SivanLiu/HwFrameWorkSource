package com.huawei.hwsqlite;

public class SQLiteDatabaseCorruptException extends SQLiteException {
    public SQLiteDatabaseCorruptException(String error) {
        super(error);
    }
}
