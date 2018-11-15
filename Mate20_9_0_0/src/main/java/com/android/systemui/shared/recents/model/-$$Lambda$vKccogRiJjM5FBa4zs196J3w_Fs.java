package com.android.systemui.shared.recents.model;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$vKccogRiJjM5FBa4zs196J3w_Fs implements OnIdleChangedListener {
    private final /* synthetic */ HighResThumbnailLoader f$0;

    public /* synthetic */ -$$Lambda$vKccogRiJjM5FBa4zs196J3w_Fs(HighResThumbnailLoader highResThumbnailLoader) {
        this.f$0 = highResThumbnailLoader;
    }

    public final void onIdleChanged(boolean z) {
        this.f$0.setTaskLoadQueueIdle(z);
    }
}
