package com.android.server.textclassifier;

import android.service.textclassifier.ITextClassificationCallback;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$6tTWS9-rW6CtxVP0xKRcg3Q5kmI implements ThrowingRunnable {
    private final /* synthetic */ ITextClassificationCallback f$0;

    public /* synthetic */ -$$Lambda$6tTWS9-rW6CtxVP0xKRcg3Q5kmI(ITextClassificationCallback iTextClassificationCallback) {
        this.f$0 = iTextClassificationCallback;
    }

    public final void runOrThrow() {
        this.f$0.onFailure();
    }
}
