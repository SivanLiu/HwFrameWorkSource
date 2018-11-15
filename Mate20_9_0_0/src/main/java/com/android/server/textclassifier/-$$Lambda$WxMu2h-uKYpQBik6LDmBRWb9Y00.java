package com.android.server.textclassifier;

import android.service.textclassifier.ITextLinksCallback;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WxMu2h-uKYpQBik6LDmBRWb9Y00 implements ThrowingRunnable {
    private final /* synthetic */ ITextLinksCallback f$0;

    public /* synthetic */ -$$Lambda$WxMu2h-uKYpQBik6LDmBRWb9Y00(ITextLinksCallback iTextLinksCallback) {
        this.f$0 = iTextLinksCallback;
    }

    public final void runOrThrow() {
        this.f$0.onFailure();
    }
}
