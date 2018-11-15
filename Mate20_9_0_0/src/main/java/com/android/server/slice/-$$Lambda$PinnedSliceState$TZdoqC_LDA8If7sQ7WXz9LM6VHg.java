package com.android.server.slice;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedSliceState$TZdoqC_LDA8If7sQ7WXz9LM6VHg implements Runnable {
    private final /* synthetic */ PinnedSliceState f$0;

    public /* synthetic */ -$$Lambda$PinnedSliceState$TZdoqC_LDA8If7sQ7WXz9LM6VHg(PinnedSliceState pinnedSliceState) {
        this.f$0 = pinnedSliceState;
    }

    public final void run() {
        this.f$0.handleSendPinned();
    }
}
