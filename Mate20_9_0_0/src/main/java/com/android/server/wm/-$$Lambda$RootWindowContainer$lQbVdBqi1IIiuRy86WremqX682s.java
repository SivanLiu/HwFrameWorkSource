package com.android.server.wm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$lQbVdBqi1IIiuRy86WremqX682s implements Consumer {
    private final /* synthetic */ ArrayList f$0;
    private final /* synthetic */ PrintWriter f$1;
    private final /* synthetic */ int[] f$2;
    private final /* synthetic */ boolean f$3;

    public /* synthetic */ -$$Lambda$RootWindowContainer$lQbVdBqi1IIiuRy86WremqX682s(ArrayList arrayList, PrintWriter printWriter, int[] iArr, boolean z) {
        this.f$0 = arrayList;
        this.f$1 = printWriter;
        this.f$2 = iArr;
        this.f$3 = z;
    }

    public final void accept(Object obj) {
        RootWindowContainer.lambda$dumpWindowsNoHeader$8(this.f$0, this.f$1, this.f$2, this.f$3, (WindowState) obj);
    }
}
