package com.android.server.accessibility;

import com.android.internal.util.function.TriConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$zXJtauhUptSkQJSF-M55-grAVbo implements TriConsumer {
    public static final /* synthetic */ -$$Lambda$AccessibilityManagerService$zXJtauhUptSkQJSF-M55-grAVbo INSTANCE = new -$$Lambda$AccessibilityManagerService$zXJtauhUptSkQJSF-M55-grAVbo();

    private /* synthetic */ -$$Lambda$AccessibilityManagerService$zXJtauhUptSkQJSF-M55-grAVbo() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((AccessibilityManagerService) obj).sendStateToClients(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
    }
}
