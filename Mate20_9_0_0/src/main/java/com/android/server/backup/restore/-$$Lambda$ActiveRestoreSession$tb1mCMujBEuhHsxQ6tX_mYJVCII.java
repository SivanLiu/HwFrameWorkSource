package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.pm.PackageInfo;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.transport.TransportClient;
import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActiveRestoreSession$tb1mCMujBEuhHsxQ6tX_mYJVCII implements BiFunction {
    private final /* synthetic */ IRestoreObserver f$0;
    private final /* synthetic */ IBackupManagerMonitor f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ PackageInfo f$3;

    public /* synthetic */ -$$Lambda$ActiveRestoreSession$tb1mCMujBEuhHsxQ6tX_mYJVCII(IRestoreObserver iRestoreObserver, IBackupManagerMonitor iBackupManagerMonitor, long j, PackageInfo packageInfo) {
        this.f$0 = iRestoreObserver;
        this.f$1 = iBackupManagerMonitor;
        this.f$2 = j;
        this.f$3 = packageInfo;
    }

    public final Object apply(Object obj, Object obj2) {
        return RestoreParams.createForSinglePackage((TransportClient) obj, this.f$0, this.f$1, this.f$2, this.f$3, (OnTaskFinishedListener) obj2);
    }
}
