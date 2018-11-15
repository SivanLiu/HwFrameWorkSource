package com.android.server.backup.fullbackup;

import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PerformFullTransportBackupTask$ymLoQLrsEpmGaMrcudrdAgsU1Zk implements OnTaskFinishedListener {
    private final /* synthetic */ TransportManager f$0;
    private final /* synthetic */ TransportClient f$1;

    public /* synthetic */ -$$Lambda$PerformFullTransportBackupTask$ymLoQLrsEpmGaMrcudrdAgsU1Zk(TransportManager transportManager, TransportClient transportClient) {
        this.f$0 = transportManager;
        this.f$1 = transportClient;
    }

    public final void onFinished(String str) {
        this.f$0.disposeOfTransportClient(this.f$1, str);
    }
}
