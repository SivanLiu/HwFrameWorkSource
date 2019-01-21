package java.util.stream;

import java.util.IntSummaryStatistics;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$49j2hRW8u6KMoxsVt77YSpMRb1g implements BiConsumer {
    private final /* synthetic */ ToIntFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$49j2hRW8u6KMoxsVt77YSpMRb1g(ToIntFunction toIntFunction) {
        this.f$0 = toIntFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ((IntSummaryStatistics) obj).accept(this.f$0.applyAsInt(obj2));
    }
}
