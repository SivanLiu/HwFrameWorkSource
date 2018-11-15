package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$2HHBX1R6lnY5GedkE9LUBwsCPoE implements Consumer {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$2HHBX1R6lnY5GedkE9LUBwsCPoE(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$onWindowFreezeTimeout$23(this.f$0, (WindowState) obj);
    }
}
