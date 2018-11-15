package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedStackController$PinnedStackControllerCallback$MdGjZinCTxKrX3GJTl1CXkAuFro implements Runnable {
    private final /* synthetic */ PinnedStackControllerCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$PinnedStackController$PinnedStackControllerCallback$MdGjZinCTxKrX3GJTl1CXkAuFro(PinnedStackControllerCallback pinnedStackControllerCallback, int i) {
        this.f$0 = pinnedStackControllerCallback;
        this.f$1 = i;
    }

    public final void run() {
        PinnedStackController.this.mCurrentMinSize = Math.max(PinnedStackController.this.mDefaultMinSize, this.f$1);
    }
}
