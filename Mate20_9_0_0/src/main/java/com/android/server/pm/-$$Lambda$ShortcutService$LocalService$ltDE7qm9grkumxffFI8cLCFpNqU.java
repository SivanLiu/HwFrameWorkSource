package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.ShortcutInfo;
import android.util.ArraySet;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$LocalService$ltDE7qm9grkumxffFI8cLCFpNqU implements Predicate {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ ArraySet f$1;
    private final /* synthetic */ ComponentName f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ boolean f$4;
    private final /* synthetic */ boolean f$5;
    private final /* synthetic */ boolean f$6;

    public /* synthetic */ -$$Lambda$ShortcutService$LocalService$ltDE7qm9grkumxffFI8cLCFpNqU(long j, ArraySet arraySet, ComponentName componentName, boolean z, boolean z2, boolean z3, boolean z4) {
        this.f$0 = j;
        this.f$1 = arraySet;
        this.f$2 = componentName;
        this.f$3 = z;
        this.f$4 = z2;
        this.f$5 = z3;
        this.f$6 = z4;
    }

    public final boolean test(Object obj) {
        return LocalService.lambda$getShortcutsInnerLocked$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, (ShortcutInfo) obj);
    }
}
