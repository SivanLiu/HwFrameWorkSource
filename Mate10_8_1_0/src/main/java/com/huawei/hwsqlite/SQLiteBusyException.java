package com.huawei.hwsqlite;

public class SQLiteBusyException extends SQLiteException {
    public SQLiteBusyException(String message) {
        super(message);
    }
}
