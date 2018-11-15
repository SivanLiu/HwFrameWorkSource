package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$_dZRryJQYdwokr9IRKPu2KCZZu4 implements Consumer {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ WindowManagerPolicy f$1;

    public /* synthetic */ -$$Lambda$DisplayContent$_dZRryJQYdwokr9IRKPu2KCZZu4(DisplayContent displayContent, WindowManagerPolicy windowManagerPolicy) {
        this.f$0 = displayContent;
        this.f$1 = windowManagerPolicy;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$waitForAllWindowsDrawn$25(this.f$0, this.f$1, (WindowState) obj);
    }
}
