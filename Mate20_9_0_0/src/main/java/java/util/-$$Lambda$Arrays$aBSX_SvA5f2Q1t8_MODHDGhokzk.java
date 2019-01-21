package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Arrays$aBSX_SvA5f2Q1t8_MODHDGhokzk implements IntConsumer {
    private final /* synthetic */ long[] f$0;
    private final /* synthetic */ IntToLongFunction f$1;

    public /* synthetic */ -$$Lambda$Arrays$aBSX_SvA5f2Q1t8_MODHDGhokzk(long[] jArr, IntToLongFunction intToLongFunction) {
        this.f$0 = jArr;
        this.f$1 = intToLongFunction;
    }

    public final void accept(int i) {
        this.f$0[i] = this.f$1.applyAsLong(i);
    }
}
