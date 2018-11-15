package com.android.server.slice;

import android.os.IBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedSliceState$KzxFkvfomRuMb5PD8_pIHDIhUUE implements DeathRecipient {
    private final /* synthetic */ PinnedSliceState f$0;

    public /* synthetic */ -$$Lambda$PinnedSliceState$KzxFkvfomRuMb5PD8_pIHDIhUUE(PinnedSliceState pinnedSliceState) {
        this.f$0 = pinnedSliceState;
    }

    public final void binderDied() {
        this.f$0.handleRecheckListeners();
    }
}
