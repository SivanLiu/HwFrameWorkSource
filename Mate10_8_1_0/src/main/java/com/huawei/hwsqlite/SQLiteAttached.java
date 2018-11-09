package com.huawei.hwsqlite;

public final class SQLiteAttached {
    String alias;
    byte[] encryptKey;
    String path;

    SQLiteAttached(String path, String alias, byte[] encryptKey) {
        this.path = path;
        this.alias = alias;
        this.encryptKey = encryptKey;
    }

    boolean isAliasEqual(SQLiteAttached other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (this.alias != null) {
            return this.alias.equals(other.alias);
        }
        if (other.alias != null) {
            z = false;
        }
        return z;
    }
}
