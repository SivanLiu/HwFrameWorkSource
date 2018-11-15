package com.android.server.accessibility;

import android.view.accessibility.IAccessibilityManagerClient;
import com.android.internal.util.FunctionalUtils.RemoteExceptionIgnoringConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$ffR9e75U5oQEFdGZAdynWg701xE implements RemoteExceptionIgnoringConsumer {
    public static final /* synthetic */ -$$Lambda$AccessibilityManagerService$ffR9e75U5oQEFdGZAdynWg701xE INSTANCE = new -$$Lambda$AccessibilityManagerService$ffR9e75U5oQEFdGZAdynWg701xE();

    private /* synthetic */ -$$Lambda$AccessibilityManagerService$ffR9e75U5oQEFdGZAdynWg701xE() {
    }

    public final void acceptOrThrow(Object obj) {
        ((IAccessibilityManagerClient) obj).notifyServicesStateChanged();
    }
}
