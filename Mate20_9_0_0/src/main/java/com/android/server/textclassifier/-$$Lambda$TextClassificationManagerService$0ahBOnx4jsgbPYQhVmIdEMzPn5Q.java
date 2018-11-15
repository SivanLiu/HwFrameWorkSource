package com.android.server.textclassifier;

import android.service.textclassifier.ITextClassificationCallback;
import android.view.textclassifier.TextClassification.Request;
import android.view.textclassifier.TextClassificationSessionId;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$0ahBOnx4jsgbPYQhVmIdEMzPn5Q implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationSessionId f$1;
    private final /* synthetic */ Request f$2;
    private final /* synthetic */ ITextClassificationCallback f$3;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$0ahBOnx4jsgbPYQhVmIdEMzPn5Q(TextClassificationManagerService textClassificationManagerService, TextClassificationSessionId textClassificationSessionId, Request request, ITextClassificationCallback iTextClassificationCallback) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationSessionId;
        this.f$2 = request;
        this.f$3 = iTextClassificationCallback;
    }

    public final void runOrThrow() {
        this.f$0.onClassifyText(this.f$1, this.f$2, this.f$3);
    }
}
