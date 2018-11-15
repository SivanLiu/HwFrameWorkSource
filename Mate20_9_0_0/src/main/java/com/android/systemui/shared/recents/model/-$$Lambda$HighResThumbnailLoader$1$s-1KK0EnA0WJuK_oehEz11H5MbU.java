package com.android.systemui.shared.recents.model;

import com.android.systemui.shared.recents.model.HighResThumbnailLoader.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HighResThumbnailLoader$1$s-1KK0EnA0WJuK_oehEz11H5MbU implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ Task f$1;
    private final /* synthetic */ ThumbnailData f$2;

    public /* synthetic */ -$$Lambda$HighResThumbnailLoader$1$s-1KK0EnA0WJuK_oehEz11H5MbU(AnonymousClass1 anonymousClass1, Task task, ThumbnailData thumbnailData) {
        this.f$0 = anonymousClass1;
        this.f$1 = task;
        this.f$2 = thumbnailData;
    }

    public final void run() {
        AnonymousClass1.lambda$loadTask$0(this.f$0, this.f$1, this.f$2);
    }
}
