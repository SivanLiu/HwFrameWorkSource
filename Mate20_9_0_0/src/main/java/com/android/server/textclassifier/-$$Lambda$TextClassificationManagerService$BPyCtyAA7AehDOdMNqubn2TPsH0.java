package com.android.server.textclassifier;

import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationSessionId;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$BPyCtyAA7AehDOdMNqubn2TPsH0 implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationContext f$1;
    private final /* synthetic */ TextClassificationSessionId f$2;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$BPyCtyAA7AehDOdMNqubn2TPsH0(TextClassificationManagerService textClassificationManagerService, TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationContext;
        this.f$2 = textClassificationSessionId;
    }

    public final void runOrThrow() {
        this.f$0.onCreateTextClassificationSession(this.f$1, this.f$2);
    }
}
