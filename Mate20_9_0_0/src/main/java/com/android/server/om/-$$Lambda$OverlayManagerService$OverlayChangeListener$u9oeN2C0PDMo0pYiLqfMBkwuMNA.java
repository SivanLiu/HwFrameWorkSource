package com.android.server.om;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OverlayManagerService$OverlayChangeListener$u9oeN2C0PDMo0pYiLqfMBkwuMNA implements Runnable {
    private final /* synthetic */ OverlayChangeListener f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$OverlayManagerService$OverlayChangeListener$u9oeN2C0PDMo0pYiLqfMBkwuMNA(OverlayChangeListener overlayChangeListener, int i, String str) {
        this.f$0 = overlayChangeListener;
        this.f$1 = i;
        this.f$2 = str;
    }

    public final void run() {
        OverlayChangeListener.lambda$onOverlaysChanged$0(this.f$0, this.f$1, this.f$2);
    }
}
