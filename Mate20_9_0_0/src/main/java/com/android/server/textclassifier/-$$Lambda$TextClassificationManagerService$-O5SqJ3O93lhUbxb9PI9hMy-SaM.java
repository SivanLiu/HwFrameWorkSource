package com.android.server.textclassifier;

import android.service.textclassifier.ITextLinksCallback;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextLinks.Request;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$-O5SqJ3O93lhUbxb9PI9hMy-SaM implements ThrowingRunnable {
    private final /* synthetic */ TextClassificationManagerService f$0;
    private final /* synthetic */ TextClassificationSessionId f$1;
    private final /* synthetic */ Request f$2;
    private final /* synthetic */ ITextLinksCallback f$3;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$-O5SqJ3O93lhUbxb9PI9hMy-SaM(TextClassificationManagerService textClassificationManagerService, TextClassificationSessionId textClassificationSessionId, Request request, ITextLinksCallback iTextLinksCallback) {
        this.f$0 = textClassificationManagerService;
        this.f$1 = textClassificationSessionId;
        this.f$2 = request;
        this.f$3 = iTextLinksCallback;
    }

    public final void runOrThrow() {
        this.f$0.onGenerateLinks(this.f$1, this.f$2, this.f$3);
    }
}
