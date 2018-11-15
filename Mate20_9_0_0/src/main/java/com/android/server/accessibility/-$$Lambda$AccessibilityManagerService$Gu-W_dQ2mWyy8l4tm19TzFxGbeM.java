package com.android.server.accessibility;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$Gu-W_dQ2mWyy8l4tm19TzFxGbeM implements Consumer {
    public static final /* synthetic */ -$$Lambda$AccessibilityManagerService$Gu-W_dQ2mWyy8l4tm19TzFxGbeM INSTANCE = new -$$Lambda$AccessibilityManagerService$Gu-W_dQ2mWyy8l4tm19TzFxGbeM();

    private /* synthetic */ -$$Lambda$AccessibilityManagerService$Gu-W_dQ2mWyy8l4tm19TzFxGbeM() {
    }

    public final void accept(Object obj) {
        ((AccessibilityManagerService) obj).announceNewUserIfNeeded();
    }
}
