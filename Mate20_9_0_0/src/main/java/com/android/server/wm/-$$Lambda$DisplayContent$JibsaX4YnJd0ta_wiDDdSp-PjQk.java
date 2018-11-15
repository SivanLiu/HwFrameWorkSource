package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$JibsaX4YnJd0ta_wiDDdSp-PjQk implements Consumer {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$JibsaX4YnJd0ta_wiDDdSp-PjQk(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final void accept(Object obj) {
        this.f$0.mService.mPolicy.applyPostLayoutPolicyLw((WindowState) obj, ((WindowState) obj).mAttrs, ((WindowState) obj).getParentWindow(), this.f$0.mService.mInputMethodTarget);
    }
}
