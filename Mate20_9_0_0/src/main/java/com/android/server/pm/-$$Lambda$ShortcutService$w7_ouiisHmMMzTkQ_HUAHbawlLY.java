package com.android.server.pm;

import java.io.PrintWriter;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$w7_ouiisHmMMzTkQ_HUAHbawlLY implements Consumer {
    private final /* synthetic */ ShortcutService f$0;

    public /* synthetic */ -$$Lambda$ShortcutService$w7_ouiisHmMMzTkQ_HUAHbawlLY(ShortcutService shortcutService) {
        this.f$0 = shortcutService;
    }

    public final void accept(Object obj) {
        this.f$0.dumpInner((PrintWriter) obj);
    }
}
