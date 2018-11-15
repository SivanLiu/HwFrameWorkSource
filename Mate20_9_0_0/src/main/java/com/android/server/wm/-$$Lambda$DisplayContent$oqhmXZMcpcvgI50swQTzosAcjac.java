package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$oqhmXZMcpcvgI50swQTzosAcjac implements Consumer {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ WindowManagerPolicy f$1;

    public /* synthetic */ -$$Lambda$DisplayContent$oqhmXZMcpcvgI50swQTzosAcjac(DisplayContent displayContent, WindowManagerPolicy windowManagerPolicy) {
        this.f$0 = displayContent;
        this.f$1 = windowManagerPolicy;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$waitForAllWindowsDrawn$24(this.f$0, this.f$1, (WindowState) obj);
    }
}
