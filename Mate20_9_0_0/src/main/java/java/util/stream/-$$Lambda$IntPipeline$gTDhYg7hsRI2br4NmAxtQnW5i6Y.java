package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntPipeline$gTDhYg7hsRI2br4NmAxtQnW5i6Y implements BinaryOperator {
    private final /* synthetic */ BiConsumer f$0;

    public /* synthetic */ -$$Lambda$IntPipeline$gTDhYg7hsRI2br4NmAxtQnW5i6Y(BiConsumer biConsumer) {
        this.f$0 = biConsumer;
    }

    public final Object apply(Object obj, Object obj2) {
        return this.f$0.accept(obj, obj2);
    }
}
