package com.android.server.backup;

import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$XAHW8jFVbxm2U5esUnLTgJC_Z6Y implements OnTaskFinishedListener {
    private final /* synthetic */ BackupManagerService f$0;
    private final /* synthetic */ TransportClient f$1;

    public /* synthetic */ -$$Lambda$BackupManagerService$XAHW8jFVbxm2U5esUnLTgJC_Z6Y(BackupManagerService backupManagerService, TransportClient transportClient) {
        this.f$0 = backupManagerService;
        this.f$1 = transportClient;
    }

    public final void onFinished(String str) {
        BackupManagerService.lambda$restoreAtInstall$6(this.f$0, this.f$1, str);
    }
}
