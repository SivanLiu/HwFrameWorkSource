package com.android.server.textclassifier;

import android.service.textclassifier.ITextSelectionCallback;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextSelection.Request;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$Oay4QGGKO1MM7dDcB0KN_1JmqZA implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationSessionId f$1;
    private final /* synthetic */ Request f$2;
    private final /* synthetic */ ITextSelectionCallback f$3;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$Oay4QGGKO1MM7dDcB0KN_1JmqZA(TextClassificationManagerService textClassificationManagerService, TextClassificationSessionId textClassificationSessionId, Request request, ITextSelectionCallback iTextSelectionCallback) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationSessionId;
        this.f$2 = request;
        this.f$3 = iTextSelectionCallback;
    }

    public final void runOrThrow() {
        this.f$0.onSuggestSelection(this.f$1, this.f$2, this.f$3);
    }
}
