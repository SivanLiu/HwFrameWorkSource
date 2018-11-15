package com.android.server.accessibility;

import android.view.accessibility.IAccessibilityManagerClient;
import com.android.internal.util.FunctionalUtils.RemoteExceptionIgnoringConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$K4sS36agT2_B03tVUTy8mldugxY implements RemoteExceptionIgnoringConsumer {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$AccessibilityManagerService$K4sS36agT2_B03tVUTy8mldugxY(int i) {
        this.f$0 = i;
    }

    public final void acceptOrThrow(Object obj) {
        ((IAccessibilityManagerClient) obj).setState(this.f$0);
    }
}
