package java.util.stream;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$F7-we3W7I2plNaGHqh_d2lzmvho implements BiConsumer {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Supplier f$1;
    private final /* synthetic */ BiConsumer f$2;

    public /* synthetic */ -$$Lambda$Collectors$F7-we3W7I2plNaGHqh_d2lzmvho(Function function, Supplier supplier, BiConsumer biConsumer) {
        this.f$0 = function;
        this.f$1 = supplier;
        this.f$2 = biConsumer;
    }

    public final void accept(Object obj, Object obj2) {
        this.f$2.accept(((Map) obj).computeIfAbsent(Objects.requireNonNull(this.f$0.apply(obj2), "element cannot be mapped to a null key"), new -$$Lambda$Collectors$f68RHYk8qNU7alEHPPrPoFuCJO4(this.f$1)), obj2);
    }
}
