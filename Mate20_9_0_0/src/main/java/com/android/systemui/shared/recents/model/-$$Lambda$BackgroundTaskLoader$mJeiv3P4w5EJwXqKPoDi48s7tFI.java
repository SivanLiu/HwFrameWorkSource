package com.android.systemui.shared.recents.model;

import android.graphics.drawable.Drawable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackgroundTaskLoader$mJeiv3P4w5EJwXqKPoDi48s7tFI implements Runnable {
    private final /* synthetic */ Task f$0;
    private final /* synthetic */ ThumbnailData f$1;
    private final /* synthetic */ Drawable f$2;

    public /* synthetic */ -$$Lambda$BackgroundTaskLoader$mJeiv3P4w5EJwXqKPoDi48s7tFI(Task task, ThumbnailData thumbnailData, Drawable drawable) {
        this.f$0 = task;
        this.f$1 = thumbnailData;
        this.f$2 = drawable;
    }

    public final void run() {
        this.f$0.notifyTaskDataLoaded(this.f$1, this.f$2);
    }
}
