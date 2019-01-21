package com.android.internal.os;

import java.util.Comparator;
import java.util.Map.Entry;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BinderCallsStats$BeSOWJ8AoyB7S9CtX-6IPAXHyNQ implements Comparator {
    public static final /* synthetic */ -$$Lambda$BinderCallsStats$BeSOWJ8AoyB7S9CtX-6IPAXHyNQ INSTANCE = new -$$Lambda$BinderCallsStats$BeSOWJ8AoyB7S9CtX-6IPAXHyNQ();

    private /* synthetic */ -$$Lambda$BinderCallsStats$BeSOWJ8AoyB7S9CtX-6IPAXHyNQ() {
    }

    public final int compare(Object obj, Object obj2) {
        return ((Long) ((Entry) obj2).getValue()).compareTo((Long) ((Entry) obj).getValue());
    }
}
