package com.android.server.backup.params;

public class ClearRetryParams {
    public String packageName;
    public String transportName;

    public ClearRetryParams(String transportName, String packageName) {
        this.transportName = transportName;
        this.packageName = packageName;
    }
}
