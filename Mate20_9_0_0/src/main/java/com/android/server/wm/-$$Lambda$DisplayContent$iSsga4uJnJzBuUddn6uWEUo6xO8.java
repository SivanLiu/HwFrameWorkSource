package com.android.server.wm;

import java.io.PrintWriter;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$iSsga4uJnJzBuUddn6uWEUo6xO8 implements Consumer {
    private final /* synthetic */ PrintWriter f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int[] f$2;

    public /* synthetic */ -$$Lambda$DisplayContent$iSsga4uJnJzBuUddn6uWEUo6xO8(PrintWriter printWriter, String str, int[] iArr) {
        this.f$0 = printWriter;
        this.f$1 = str;
        this.f$2 = iArr;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$dumpWindowAnimators$18(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
