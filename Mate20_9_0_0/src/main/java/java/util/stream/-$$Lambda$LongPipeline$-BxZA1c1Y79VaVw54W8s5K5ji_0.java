package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongPipeline$-BxZA1c1Y79VaVw54W8s5K5ji_0 implements BinaryOperator {
    private final /* synthetic */ BiConsumer f$0;

    public /* synthetic */ -$$Lambda$LongPipeline$-BxZA1c1Y79VaVw54W8s5K5ji_0(BiConsumer biConsumer) {
        this.f$0 = biConsumer;
    }

    public final Object apply(Object obj, Object obj2) {
        return this.f$0.accept(obj, obj2);
    }
}
