package com.android.server.backup;

import com.android.server.backup.transport.OnTransportRegisteredListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$QlgHuOXOPKAZpwyUhPFAintPnqM implements OnTransportRegisteredListener {
    private final /* synthetic */ BackupManagerService f$0;

    public /* synthetic */ -$$Lambda$BackupManagerService$QlgHuOXOPKAZpwyUhPFAintPnqM(BackupManagerService backupManagerService) {
        this.f$0 = backupManagerService;
    }

    public final void onTransportRegistered(String str, String str2) {
        this.f$0.onTransportRegistered(str, str2);
    }
}
