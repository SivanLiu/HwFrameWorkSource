package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$vmLceJDpkkH4HVeqPcL08DnO8yg implements BiConsumer {
    private final /* synthetic */ BiConsumer f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Collectors$vmLceJDpkkH4HVeqPcL08DnO8yg(BiConsumer biConsumer, Function function) {
        this.f$0 = biConsumer;
        this.f$1 = function;
    }

    public final void accept(Object obj, Object obj2) {
        this.f$0.accept(obj, this.f$1.apply(obj2));
    }
}
