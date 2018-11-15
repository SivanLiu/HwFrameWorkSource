package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$1GkjuS_pAduq4UeRzcVLneyJbGM implements Consumer {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$DisplayContent$1GkjuS_pAduq4UeRzcVLneyJbGM(DisplayContent displayContent, int i, int i2) {
        this.f$0 = displayContent;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final void accept(Object obj) {
        ((WindowState) obj).mWinAnimator.seamlesslyRotateWindow(this.f$0.getPendingTransaction(), this.f$1, this.f$2);
    }
}
