package com.android.systemui.shared.system;

import android.view.ThreadedRenderer.FrameDrawingCallback;
import com.android.systemui.shared.system.SyncRtSurfaceTransactionApplier.SurfaceParams;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncRtSurfaceTransactionApplier$odiZXJPwVQjUmyGhNuNxcMPZIOM implements FrameDrawingCallback {
    private final /* synthetic */ SyncRtSurfaceTransactionApplier f$0;
    private final /* synthetic */ SurfaceParams[] f$1;

    public /* synthetic */ -$$Lambda$SyncRtSurfaceTransactionApplier$odiZXJPwVQjUmyGhNuNxcMPZIOM(SyncRtSurfaceTransactionApplier syncRtSurfaceTransactionApplier, SurfaceParams[] surfaceParamsArr) {
        this.f$0 = syncRtSurfaceTransactionApplier;
        this.f$1 = surfaceParamsArr;
    }

    public final void onFrameDraw(long j) {
        SyncRtSurfaceTransactionApplier.lambda$scheduleApply$0(this.f$0, this.f$1, j);
    }
}
