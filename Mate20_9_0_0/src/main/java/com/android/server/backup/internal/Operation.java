package com.android.server.backup.internal;

import com.android.server.backup.BackupRestoreTask;

public class Operation {
    public final BackupRestoreTask callback;
    public int state;
    public final int type;

    public Operation(int initialState, BackupRestoreTask callbackObj, int type) {
        this.state = initialState;
        this.callback = callbackObj;
        this.type = type;
    }
}
