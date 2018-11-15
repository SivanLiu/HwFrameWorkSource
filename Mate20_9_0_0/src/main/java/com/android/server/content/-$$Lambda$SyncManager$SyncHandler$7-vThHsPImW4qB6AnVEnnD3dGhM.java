package com.android.server.content;

import android.os.Bundle;
import android.os.RemoteCallback.OnResultListener;
import com.android.server.content.SyncStorageEngine.EndPoint;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$SyncHandler$7-vThHsPImW4qB6AnVEnnD3dGhM implements OnResultListener {
    private final /* synthetic */ SyncHandler f$0;
    private final /* synthetic */ EndPoint f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ long f$3;
    private final /* synthetic */ Bundle f$4;

    public /* synthetic */ -$$Lambda$SyncManager$SyncHandler$7-vThHsPImW4qB6AnVEnnD3dGhM(SyncHandler syncHandler, EndPoint endPoint, long j, long j2, Bundle bundle) {
        this.f$0 = syncHandler;
        this.f$1 = endPoint;
        this.f$2 = j;
        this.f$3 = j2;
        this.f$4 = bundle;
    }

    public final void onResult(Bundle bundle) {
        SyncHandler.lambda$updateOrAddPeriodicSyncH$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, bundle);
    }
}
