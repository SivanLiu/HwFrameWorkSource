package com.android.server.wm;

import com.android.internal.util.ToBooleanFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwDisplayContent$gpSSKJ2u9mSSHwlUQNX0an81APk implements ToBooleanFunction {
    private final /* synthetic */ HwDisplayContent f$0;
    private final /* synthetic */ float f$1;
    private final /* synthetic */ float f$2;

    public /* synthetic */ -$$Lambda$HwDisplayContent$gpSSKJ2u9mSSHwlUQNX0an81APk(HwDisplayContent hwDisplayContent, float f, float f2) {
        this.f$0 = hwDisplayContent;
        this.f$1 = f;
        this.f$2 = f2;
    }

    public final boolean apply(Object obj) {
        return HwDisplayContent.lambda$shouldDropMotionEventForTouchPad$2(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
