package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$rF1ZhFUTWyZqcBK8Oea3g5-uNlM implements Consumer {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$rF1ZhFUTWyZqcBK8Oea3g5-uNlM(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$destroyLeakedSurfaces$16(this.f$0, (WindowState) obj);
    }
}
