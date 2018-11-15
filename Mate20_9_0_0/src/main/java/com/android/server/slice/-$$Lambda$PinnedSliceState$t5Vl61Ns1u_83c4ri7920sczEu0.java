package com.android.server.slice;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedSliceState$t5Vl61Ns1u_83c4ri7920sczEu0 implements Runnable {
    private final /* synthetic */ PinnedSliceState f$0;

    public /* synthetic */ -$$Lambda$PinnedSliceState$t5Vl61Ns1u_83c4ri7920sczEu0(PinnedSliceState pinnedSliceState) {
        this.f$0 = pinnedSliceState;
    }

    public final void run() {
        this.f$0.handleSendUnpinned();
    }
}
