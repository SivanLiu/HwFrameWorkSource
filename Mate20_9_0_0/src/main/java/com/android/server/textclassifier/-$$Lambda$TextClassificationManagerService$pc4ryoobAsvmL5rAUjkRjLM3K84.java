package com.android.server.textclassifier;

import android.view.textclassifier.TextClassificationSessionId;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$pc4ryoobAsvmL5rAUjkRjLM3K84 implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationSessionId f$1;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$pc4ryoobAsvmL5rAUjkRjLM3K84(TextClassificationManagerService textClassificationManagerService, TextClassificationSessionId textClassificationSessionId) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationSessionId;
    }

    public final void runOrThrow() {
        this.f$0.onDestroyTextClassificationSession(this.f$1);
    }
}
