package com.android.internal.util;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FunctionalUtils$koCSI8D7Nu5vOJTVTEj0m3leo_U implements Runnable {
    private final /* synthetic */ ThrowingRunnable f$0;
    private final /* synthetic */ Consumer f$1;

    public /* synthetic */ -$$Lambda$FunctionalUtils$koCSI8D7Nu5vOJTVTEj0m3leo_U(ThrowingRunnable throwingRunnable, Consumer consumer) {
        this.f$0 = throwingRunnable;
        this.f$1 = consumer;
    }

    public final void run() {
        FunctionalUtils.lambda$handleExceptions$0(this.f$0, this.f$1);
    }
}
