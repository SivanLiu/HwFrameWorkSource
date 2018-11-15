package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutPackage$Uf55CaKs9xv-osb2umPmXq3W2lM implements Predicate {
    public static final /* synthetic */ -$$Lambda$ShortcutPackage$Uf55CaKs9xv-osb2umPmXq3W2lM INSTANCE = new -$$Lambda$ShortcutPackage$Uf55CaKs9xv-osb2umPmXq3W2lM();

    private /* synthetic */ -$$Lambda$ShortcutPackage$Uf55CaKs9xv-osb2umPmXq3W2lM() {
    }

    public final boolean test(Object obj) {
        return (((ShortcutInfo) obj).isDynamic() ^ 1);
    }
}
