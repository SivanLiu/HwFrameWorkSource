package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;
import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActiveRestoreSession$amDGbcwA180LGcZKUosvhspMk2E implements BiFunction {
    private final /* synthetic */ IRestoreObserver f$0;
    private final /* synthetic */ IBackupManagerMonitor f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ String[] f$3;

    public /* synthetic */ -$$Lambda$ActiveRestoreSession$amDGbcwA180LGcZKUosvhspMk2E(IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, String[] strArr) {
        this.f$0 = iRestoreObserver;
        this.f$1 = iBackupManagerMonitor;
        this.f$2 = j;
        this.f$3 = strArr;
    }

    public final Object apply(Object obj, Object obj2) {
        return ActiveRestoreSession.lambda$restoreSome$2(this.f$0, this.f$1, this.f$2, this.f$3, (TransportClient) obj, (OnTaskFinishedListener) obj2);
    }
}
