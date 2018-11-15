package com.android.server.backup.transport;

import com.android.server.backup.transport.TransportStats.Stats;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA implements BinaryOperator {
    public static final /* synthetic */ -$$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA INSTANCE = new -$$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA();

    private /* synthetic */ -$$Lambda$bnpJn6l0a4iWMupJTDnTAfwT1eA() {
    }

    public final Object apply(Object obj, Object obj2) {
        return Stats.merge((Stats) obj, (Stats) obj2);
    }
}
