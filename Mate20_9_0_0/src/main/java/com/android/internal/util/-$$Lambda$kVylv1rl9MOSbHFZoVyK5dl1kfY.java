package com.android.internal.util;

import android.content.ComponentName.WithComponentName;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$kVylv1rl9MOSbHFZoVyK5dl1kfY implements Predicate {
    public static final /* synthetic */ -$$Lambda$kVylv1rl9MOSbHFZoVyK5dl1kfY INSTANCE = new -$$Lambda$kVylv1rl9MOSbHFZoVyK5dl1kfY();

    private /* synthetic */ -$$Lambda$kVylv1rl9MOSbHFZoVyK5dl1kfY() {
    }

    public final boolean test(Object obj) {
        return DumpUtils.isPlatformPackage((WithComponentName) obj);
    }
}
