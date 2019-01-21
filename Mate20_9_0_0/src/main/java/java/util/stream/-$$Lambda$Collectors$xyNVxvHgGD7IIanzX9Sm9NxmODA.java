package java.util.stream;

import java.util.LongSummaryStatistics;
import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$xyNVxvHgGD7IIanzX9Sm9NxmODA implements BiConsumer {
    private final /* synthetic */ ToLongFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$xyNVxvHgGD7IIanzX9Sm9NxmODA(ToLongFunction toLongFunction) {
        this.f$0 = toLongFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ((LongSummaryStatistics) obj).accept(this.f$0.applyAsLong(obj2));
    }
}
