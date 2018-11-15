package com.android.server.backup.internal;

public class BackupRequest {
    public String packageName;

    public BackupRequest(String pkgName) {
        this.packageName = pkgName;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BackupRequest{pkg=");
        stringBuilder.append(this.packageName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
