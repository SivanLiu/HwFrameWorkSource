package java.util.stream;

import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$TRHsqgEycZfemtQqwivCY4ecHDM implements BiConsumer {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Supplier f$1;
    private final /* synthetic */ BiConsumer f$2;

    public /* synthetic */ -$$Lambda$Collectors$TRHsqgEycZfemtQqwivCY4ecHDM(Function function, Supplier supplier, BiConsumer biConsumer) {
        this.f$0 = function;
        this.f$1 = supplier;
        this.f$2 = biConsumer;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$groupingByConcurrent$51(this.f$0, this.f$1, this.f$2, (ConcurrentMap) obj, obj2);
    }
}
