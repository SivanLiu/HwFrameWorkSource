package com.android.server.accessibility;

import com.android.internal.util.FunctionalUtils.RemoteExceptionIgnoringConsumer;
import com.android.server.accessibility.AccessibilityManagerService.UserState;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$CNt8wbTQCYcsUnUkUCQHtKqr-tY implements RemoteExceptionIgnoringConsumer {
    private final /* synthetic */ AccessibilityManagerService f$0;
    private final /* synthetic */ UserState f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManagerService$CNt8wbTQCYcsUnUkUCQHtKqr-tY(AccessibilityManagerService accessibilityManagerService, UserState userState) {
        this.f$0 = accessibilityManagerService;
        this.f$1 = userState;
    }

    public final void acceptOrThrow(Object obj) {
        AccessibilityManagerService.lambda$updateRelevantEventsLocked$0(this.f$0, this.f$1, (Client) obj);
    }
}
