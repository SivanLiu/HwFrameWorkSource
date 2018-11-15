package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$1C_-u_mpQFfKL_O8K1VFzBgPg50 implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$DisplayContent$1C_-u_mpQFfKL_O8K1VFzBgPg50(int i, int i2) {
        this.f$0 = i;
        this.f$1 = i2;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$updateSystemUiVisibility$22(this.f$0, this.f$1, (WindowState) obj);
    }
}
