package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$0hO4kVeN9EGHZquOI_a_qZ75htQ implements BiConsumer {
    private final /* synthetic */ ToDoubleFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$0hO4kVeN9EGHZquOI_a_qZ75htQ(ToDoubleFunction toDoubleFunction) {
        this.f$0 = toDoubleFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ((DoubleSummaryStatistics) obj).accept(this.f$0.applyAsDouble(obj2));
    }
}
