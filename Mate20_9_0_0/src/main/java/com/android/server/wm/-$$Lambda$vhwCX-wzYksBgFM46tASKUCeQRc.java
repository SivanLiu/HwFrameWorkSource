package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$vhwCX-wzYksBgFM46tASKUCeQRc implements Consumer {
    public static final /* synthetic */ -$$Lambda$vhwCX-wzYksBgFM46tASKUCeQRc INSTANCE = new -$$Lambda$vhwCX-wzYksBgFM46tASKUCeQRc();

    private /* synthetic */ -$$Lambda$vhwCX-wzYksBgFM46tASKUCeQRc() {
    }

    public final void accept(Object obj) {
        ((WindowState) obj).resetDragResizingChangeReported();
    }
}
