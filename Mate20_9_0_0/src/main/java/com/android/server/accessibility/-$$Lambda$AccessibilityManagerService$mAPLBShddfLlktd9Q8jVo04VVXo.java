package com.android.server.accessibility;

import com.android.server.accessibility.AccessibilityManagerService.UserState;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$mAPLBShddfLlktd9Q8jVo04VVXo implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$AccessibilityManagerService$mAPLBShddfLlktd9Q8jVo04VVXo INSTANCE = new -$$Lambda$AccessibilityManagerService$mAPLBShddfLlktd9Q8jVo04VVXo();

    private /* synthetic */ -$$Lambda$AccessibilityManagerService$mAPLBShddfLlktd9Q8jVo04VVXo() {
    }

    public final void accept(Object obj, Object obj2) {
        ((AccessibilityManagerService) obj).updateFingerprintGestureHandling((UserState) obj2);
    }
}
