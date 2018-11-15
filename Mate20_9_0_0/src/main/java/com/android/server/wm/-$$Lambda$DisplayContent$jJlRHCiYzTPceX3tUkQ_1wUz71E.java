package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy.WindowState;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$jJlRHCiYzTPceX3tUkQ_1wUz71E implements Predicate {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ WindowState f$1;
    private final /* synthetic */ WindowState f$2;

    public /* synthetic */ -$$Lambda$DisplayContent$jJlRHCiYzTPceX3tUkQ_1wUz71E(DisplayContent displayContent, WindowState windowState, WindowState windowState2) {
        this.f$0 = displayContent;
        this.f$1 = windowState;
        this.f$2 = windowState2;
    }

    public final boolean test(Object obj) {
        return DisplayContent.lambda$getNeedsMenu$17(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
