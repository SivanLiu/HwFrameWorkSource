package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$2KrtdmjrY7Nagc4IRqzCk9gDuQU implements Consumer {
    private final /* synthetic */ WindowManagerService f$0;

    public /* synthetic */ -$$Lambda$2KrtdmjrY7Nagc4IRqzCk9gDuQU(WindowManagerService windowManagerService) {
        this.f$0 = windowManagerService;
    }

    public final void accept(Object obj) {
        this.f$0.makeWindowFreezingScreenIfNeededLocked((WindowState) obj);
    }
}
