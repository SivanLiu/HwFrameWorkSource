package com.android.server.backup.params;

public class ClearRetryParams {
    public String packageName;
    public String transportName;

    public ClearRetryParams(String transport, String pkg) {
        this.transportName = transport;
        this.packageName = pkg;
    }
}
