package com.android.server.backup;

import java.util.List;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$Yom7ZUYhsBBc6e92Mh_gepfydaQ implements Consumer {
    private final /* synthetic */ BackupManagerService f$0;
    private final /* synthetic */ List f$1;
    private final /* synthetic */ List f$2;

    public /* synthetic */ -$$Lambda$BackupManagerService$Yom7ZUYhsBBc6e92Mh_gepfydaQ(BackupManagerService backupManagerService, List list, List list2) {
        this.f$0 = backupManagerService;
        this.f$1 = list;
        this.f$2 = list2;
    }

    public final void accept(Object obj) {
        BackupManagerService.lambda$setBackupEnabled$4(this.f$0, this.f$1, this.f$2, (String) obj);
    }
}
