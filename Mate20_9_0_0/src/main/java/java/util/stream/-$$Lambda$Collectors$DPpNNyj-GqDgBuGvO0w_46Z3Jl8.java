package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$DPpNNyj-GqDgBuGvO0w_46Z3Jl8 implements BiConsumer {
    private final /* synthetic */ BiConsumer f$0;
    private final /* synthetic */ Predicate f$1;

    public /* synthetic */ -$$Lambda$Collectors$DPpNNyj-GqDgBuGvO0w_46Z3Jl8(BiConsumer biConsumer, Predicate predicate) {
        this.f$0 = biConsumer;
        this.f$1 = predicate;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$partitioningBy$54(this.f$0, this.f$1, (Partition) obj, obj2);
    }
}
