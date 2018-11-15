package com.android.server.pm;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$LocalService$Q0t7aDuDFJ8HWAf1NHW1dGQjOf8 implements Consumer {
    private final /* synthetic */ LocalService f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$10;
    private final /* synthetic */ int f$11;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ List f$3;
    private final /* synthetic */ long f$4;
    private final /* synthetic */ ComponentName f$5;
    private final /* synthetic */ int f$6;
    private final /* synthetic */ int f$7;
    private final /* synthetic */ ArrayList f$8;
    private final /* synthetic */ int f$9;

    public /* synthetic */ -$$Lambda$ShortcutService$LocalService$Q0t7aDuDFJ8HWAf1NHW1dGQjOf8(LocalService localService, int i, String str, List list, long j, ComponentName componentName, int i2, int i3, ArrayList arrayList, int i4, int i5, int i6) {
        this.f$0 = localService;
        this.f$1 = i;
        this.f$2 = str;
        this.f$3 = list;
        this.f$4 = j;
        this.f$5 = componentName;
        this.f$6 = i2;
        this.f$7 = i3;
        this.f$8 = arrayList;
        this.f$9 = i4;
        this.f$10 = i5;
        this.f$11 = i6;
    }

    public final void accept(Object obj) {
        this.f$0.getShortcutsInnerLocked(this.f$1, this.f$2, ((ShortcutPackage) obj).getPackageName(), this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11);
    }
}
