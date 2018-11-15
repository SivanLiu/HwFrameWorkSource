package com.huawei.hwsqlite;

public class SQLiteNotSupportException extends SQLiteException {
    public SQLiteNotSupportException(String error) {
        super(error);
    }
}
