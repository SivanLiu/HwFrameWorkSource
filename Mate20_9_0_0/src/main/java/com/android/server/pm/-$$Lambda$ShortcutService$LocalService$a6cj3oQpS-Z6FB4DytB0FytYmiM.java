package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$LocalService$a6cj3oQpS-Z6FB4DytB0FytYmiM implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$ShortcutService$LocalService$a6cj3oQpS-Z6FB4DytB0FytYmiM(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return this.f$0.equals(((ShortcutInfo) obj).getId());
    }
}
