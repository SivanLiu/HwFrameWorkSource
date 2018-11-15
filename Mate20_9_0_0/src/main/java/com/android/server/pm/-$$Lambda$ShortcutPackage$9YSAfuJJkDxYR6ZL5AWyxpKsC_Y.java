package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutPackage$9YSAfuJJkDxYR6ZL5AWyxpKsC_Y implements Predicate {
    public static final /* synthetic */ -$$Lambda$ShortcutPackage$9YSAfuJJkDxYR6ZL5AWyxpKsC_Y INSTANCE = new -$$Lambda$ShortcutPackage$9YSAfuJJkDxYR6ZL5AWyxpKsC_Y();

    private /* synthetic */ -$$Lambda$ShortcutPackage$9YSAfuJJkDxYR6ZL5AWyxpKsC_Y() {
    }

    public final boolean test(Object obj) {
        return (((ShortcutInfo) obj).isManifestShortcut() ^ 1);
    }
}
