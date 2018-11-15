package com.android.server.backup;

import android.content.ComponentName;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TransportManager$_dxJobf45tWiMkaNlKY-z26kB2Q implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$TransportManager$_dxJobf45tWiMkaNlKY-z26kB2Q(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return this.f$0.equals(((ComponentName) obj).getPackageName());
    }
}
