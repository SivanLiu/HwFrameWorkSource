package com.android.server.backup;

import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$drk8n83Z0hBmm5D4bbaFMr5WuzA implements OnTaskFinishedListener {
    private final /* synthetic */ BackupManagerService f$0;
    private final /* synthetic */ TransportClient f$1;

    public /* synthetic */ -$$Lambda$BackupManagerService$drk8n83Z0hBmm5D4bbaFMr5WuzA(BackupManagerService backupManagerService, TransportClient transportClient) {
        this.f$0 = backupManagerService;
        this.f$1 = transportClient;
    }

    public final void onFinished(String str) {
        this.f$0.mTransportManager.disposeOfTransportClient(this.f$1, str);
    }
}
