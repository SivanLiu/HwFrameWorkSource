package java.util.stream;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$AfO_bLozmdhHTtbBN7DysDzpfYM implements BiConsumer {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Supplier f$1;
    private final /* synthetic */ BiConsumer f$2;

    public /* synthetic */ -$$Lambda$Collectors$AfO_bLozmdhHTtbBN7DysDzpfYM(Function function, Supplier supplier, BiConsumer biConsumer) {
        this.f$0 = function;
        this.f$1 = supplier;
        this.f$2 = biConsumer;
    }

    public final void accept(Object obj, Object obj2) {
        this.f$2.accept(((ConcurrentMap) obj).computeIfAbsent(Objects.requireNonNull(this.f$0.apply(obj2), "element cannot be mapped to a null key"), new -$$Lambda$Collectors$eESkXUxzUQd_kZxyXI8noD7gpIw(this.f$1)), obj2);
    }
}
