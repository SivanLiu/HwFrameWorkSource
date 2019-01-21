package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Arrays$H0YqaggIxZUqId4_BJ1BLcUa93k implements IntConsumer {
    private final /* synthetic */ Object[] f$0;
    private final /* synthetic */ IntFunction f$1;

    public /* synthetic */ -$$Lambda$Arrays$H0YqaggIxZUqId4_BJ1BLcUa93k(Object[] objArr, IntFunction intFunction) {
        this.f$0 = objArr;
        this.f$1 = intFunction;
    }

    public final void accept(int i) {
        this.f$0[i] = this.f$1.apply(i);
    }
}
