package com.android.internal.os;

import java.util.Comparator;
import java.util.Map.Entry;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BinderCallsStats$jhdszMKzG9FSuIQ4Vz9B0exXKPk implements Comparator {
    public static final /* synthetic */ -$$Lambda$BinderCallsStats$jhdszMKzG9FSuIQ4Vz9B0exXKPk INSTANCE = new -$$Lambda$BinderCallsStats$jhdszMKzG9FSuIQ4Vz9B0exXKPk();

    private /* synthetic */ -$$Lambda$BinderCallsStats$jhdszMKzG9FSuIQ4Vz9B0exXKPk() {
    }

    public final int compare(Object obj, Object obj2) {
        return ((Long) ((Entry) obj2).getValue()).compareTo((Long) ((Entry) obj).getValue());
    }
}
