package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$fJACJZmXtOEEtwlcx1f9zVkhr30 implements Consumer {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$fJACJZmXtOEEtwlcx1f9zVkhr30(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$onSeamlessRotationTimeout$26(this.f$0, (WindowState) obj);
    }
}
