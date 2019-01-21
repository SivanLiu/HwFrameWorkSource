package android.view;

import android.view.ThreadedRenderer.FrameCompleteCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ViewRootImpl$zmAX2p20-kqxknxcUyGhSNjsJvM implements FrameCompleteCallback {
    private final /* synthetic */ ViewRootImpl f$0;

    public /* synthetic */ -$$Lambda$ViewRootImpl$zmAX2p20-kqxknxcUyGhSNjsJvM(ViewRootImpl viewRootImpl) {
        this.f$0 = viewRootImpl;
    }

    public final void onFrameComplete(long j) {
        this.f$0.pendingDrawFinished();
    }
}
