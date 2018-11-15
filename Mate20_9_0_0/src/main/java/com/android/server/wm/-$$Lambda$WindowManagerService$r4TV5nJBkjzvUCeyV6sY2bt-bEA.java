package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$r4TV5nJBkjzvUCeyV6sY2bt-bEA implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$WindowManagerService$r4TV5nJBkjzvUCeyV6sY2bt-bEA(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return WindowManagerService.lambda$findWindow$4(this.f$0, (WindowState) obj);
    }
}
