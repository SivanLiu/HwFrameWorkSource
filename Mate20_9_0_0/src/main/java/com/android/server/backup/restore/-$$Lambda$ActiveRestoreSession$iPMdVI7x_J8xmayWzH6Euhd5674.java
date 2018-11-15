package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.transport.TransportClient;
import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActiveRestoreSession$iPMdVI7x_J8xmayWzH6Euhd5674 implements BiFunction {
    private final /* synthetic */ IRestoreObserver f$0;
    private final /* synthetic */ IBackupManagerMonitor f$1;
    private final /* synthetic */ long f$2;

    public /* synthetic */ -$$Lambda$ActiveRestoreSession$iPMdVI7x_J8xmayWzH6Euhd5674(IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j) {
        this.f$0 = iRestoreObserver;
        this.f$1 = iBackupManagerMonitor;
        this.f$2 = j;
    }

    public final Object apply(Object obj, Object obj2) {
        return RestoreParams.createForRestoreAll((TransportClient) obj, this.f$0, this.f$1, this.f$2, (OnTaskFinishedListener) obj2);
    }
}
