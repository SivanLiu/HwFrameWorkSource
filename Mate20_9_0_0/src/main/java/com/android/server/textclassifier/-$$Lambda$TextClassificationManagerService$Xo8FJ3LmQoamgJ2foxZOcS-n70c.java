package com.android.server.textclassifier;

import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassificationSessionId;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$Xo8FJ3LmQoamgJ2foxZOcS-n70c implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationSessionId f$1;
    private final /* synthetic */ SelectionEvent f$2;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$Xo8FJ3LmQoamgJ2foxZOcS-n70c(TextClassificationManagerService textClassificationManagerService, TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationSessionId;
        this.f$2 = selectionEvent;
    }

    public final void runOrThrow() {
        this.f$0.onSelectionEvent(this.f$1, this.f$2);
    }
}
