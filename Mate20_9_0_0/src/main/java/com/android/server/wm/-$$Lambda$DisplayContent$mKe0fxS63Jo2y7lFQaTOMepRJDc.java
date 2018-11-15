package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$mKe0fxS63Jo2y7lFQaTOMepRJDc implements Consumer {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$DisplayContent$mKe0fxS63Jo2y7lFQaTOMepRJDc(DisplayContent displayContent, boolean z) {
        this.f$0 = displayContent;
        this.f$1 = z;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$updateRotationUnchecked$11(this.f$0, this.f$1, (WindowState) obj);
    }
}
