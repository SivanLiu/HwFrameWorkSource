package java.util.concurrent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ConcurrentMap$T12JRbgGLhxGbYCuTfff6_dTrMk implements BiConsumer {
    private final /* synthetic */ ConcurrentMap f$0;
    private final /* synthetic */ BiFunction f$1;

    public /* synthetic */ -$$Lambda$ConcurrentMap$T12JRbgGLhxGbYCuTfff6_dTrMk(ConcurrentMap concurrentMap, BiFunction biFunction) {
        this.f$0 = concurrentMap;
        this.f$1 = biFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ConcurrentMap.lambda$replaceAll$0(this.f$0, this.f$1, obj, obj2);
    }
}
