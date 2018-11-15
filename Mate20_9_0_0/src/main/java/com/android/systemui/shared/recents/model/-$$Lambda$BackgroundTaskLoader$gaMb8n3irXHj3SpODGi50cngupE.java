package com.android.systemui.shared.recents.model;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackgroundTaskLoader$gaMb8n3irXHj3SpODGi50cngupE implements Runnable {
    private final /* synthetic */ BackgroundTaskLoader f$0;

    public /* synthetic */ -$$Lambda$BackgroundTaskLoader$gaMb8n3irXHj3SpODGi50cngupE(BackgroundTaskLoader backgroundTaskLoader) {
        this.f$0 = backgroundTaskLoader;
    }

    public final void run() {
        this.f$0.mOnIdleChangedListener.onIdleChanged(true);
    }
}
