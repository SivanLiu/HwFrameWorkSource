package com.android.server.backup.params;

import android.content.pm.PackageInfo;
import com.android.internal.backup.IBackupTransport;

public class ClearParams {
    public PackageInfo packageInfo;
    public IBackupTransport transport;

    public ClearParams(IBackupTransport _transport, PackageInfo _info) {
        this.transport = _transport;
        this.packageInfo = _info;
    }
}
