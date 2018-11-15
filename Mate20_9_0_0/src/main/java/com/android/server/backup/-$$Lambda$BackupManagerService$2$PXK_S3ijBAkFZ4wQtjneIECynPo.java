package com.android.server.backup;

import com.android.server.backup.BackupManagerService.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$2$PXK_S3ijBAkFZ4wQtjneIECynPo implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$BackupManagerService$2$PXK_S3ijBAkFZ4wQtjneIECynPo(AnonymousClass2 anonymousClass2, String str) {
        this.f$0 = anonymousClass2;
        this.f$1 = str;
    }

    public final void run() {
        BackupManagerService.this.mTransportManager.onPackageRemoved(this.f$1);
    }
}
