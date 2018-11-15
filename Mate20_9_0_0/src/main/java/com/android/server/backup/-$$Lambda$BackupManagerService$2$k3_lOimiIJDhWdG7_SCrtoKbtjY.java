package com.android.server.backup;

import com.android.server.backup.BackupManagerService.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$2$k3_lOimiIJDhWdG7_SCrtoKbtjY implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ String[] f$2;

    public /* synthetic */ -$$Lambda$BackupManagerService$2$k3_lOimiIJDhWdG7_SCrtoKbtjY(AnonymousClass2 anonymousClass2, String str, String[] strArr) {
        this.f$0 = anonymousClass2;
        this.f$1 = str;
        this.f$2 = strArr;
    }

    public final void run() {
        BackupManagerService.this.mTransportManager.onPackageChanged(this.f$1, this.f$2);
    }
}
