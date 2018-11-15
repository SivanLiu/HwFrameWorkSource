package com.android.server.textclassifier;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextClassificationManagerService$AlzZLOTDy6ySI7ijsc3zdoY2qPo implements Consumer {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$TextClassificationManagerService$AlzZLOTDy6ySI7ijsc3zdoY2qPo(String str) {
        this.f$0 = str;
    }

    public final void accept(Object obj) {
        TextClassificationManagerService.lambda$logOnFailure$6(this.f$0, (Throwable) obj);
    }
}
