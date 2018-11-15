package com.android.systemui.shared.recents.model;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackgroundTaskLoader$XRsMGIp0x8MAJ36UKSTd3DJ9dTg implements Runnable {
    private final /* synthetic */ BackgroundTaskLoader f$0;

    public /* synthetic */ -$$Lambda$BackgroundTaskLoader$XRsMGIp0x8MAJ36UKSTd3DJ9dTg(BackgroundTaskLoader backgroundTaskLoader) {
        this.f$0 = backgroundTaskLoader;
    }

    public final void run() {
        this.f$0.mOnIdleChangedListener.onIdleChanged(false);
    }
}
