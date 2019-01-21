package com.android.internal.util;

import android.content.ComponentName.WithComponentName;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JwOUSWW2-Jzu15y4Kn4JuPh8tWM implements Predicate {
    public static final /* synthetic */ -$$Lambda$JwOUSWW2-Jzu15y4Kn4JuPh8tWM INSTANCE = new -$$Lambda$JwOUSWW2-Jzu15y4Kn4JuPh8tWM();

    private /* synthetic */ -$$Lambda$JwOUSWW2-Jzu15y4Kn4JuPh8tWM() {
    }

    public final boolean test(Object obj) {
        return DumpUtils.isNonPlatformPackage((WithComponentName) obj);
    }
}
