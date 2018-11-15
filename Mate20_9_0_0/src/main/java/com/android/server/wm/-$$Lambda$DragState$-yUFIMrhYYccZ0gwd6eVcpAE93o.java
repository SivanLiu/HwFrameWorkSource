package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DragState$-yUFIMrhYYccZ0gwd6eVcpAE93o implements Consumer {
    private final /* synthetic */ DragState f$0;
    private final /* synthetic */ float f$1;
    private final /* synthetic */ float f$2;

    public /* synthetic */ -$$Lambda$DragState$-yUFIMrhYYccZ0gwd6eVcpAE93o(DragState dragState, float f, float f2) {
        this.f$0 = dragState;
        this.f$1 = f;
        this.f$2 = f2;
    }

    public final void accept(Object obj) {
        this.f$0.sendDragStartedLocked((WindowState) obj, this.f$1, this.f$2, this.f$0.mDataDescription);
    }
}
