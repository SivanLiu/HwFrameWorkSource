package com.android.server.textclassifier;

import android.service.textclassifier.ITextSelectionCallback;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$m88mc8F7odBzfaVb5UMVTJhRQps implements ThrowingRunnable {
    private final /* synthetic */ ITextSelectionCallback f$0;

    public /* synthetic */ -$$Lambda$m88mc8F7odBzfaVb5UMVTJhRQps(ITextSelectionCallback iTextSelectionCallback) {
        this.f$0 = iTextSelectionCallback;
    }

    public final void runOrThrow() {
        this.f$0.onFailure();
    }
}
