package com.huawei.hwsqlite;

public class SQLiteOutOfMemoryException extends SQLiteException {
    public SQLiteOutOfMemoryException(String error) {
        super(error);
    }
}
