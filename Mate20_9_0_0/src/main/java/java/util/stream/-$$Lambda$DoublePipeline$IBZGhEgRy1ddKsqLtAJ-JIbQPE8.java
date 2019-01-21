package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoublePipeline$IBZGhEgRy1ddKsqLtAJ-JIbQPE8 implements BinaryOperator {
    private final /* synthetic */ BiConsumer f$0;

    public /* synthetic */ -$$Lambda$DoublePipeline$IBZGhEgRy1ddKsqLtAJ-JIbQPE8(BiConsumer biConsumer) {
        this.f$0 = biConsumer;
    }

    public final Object apply(Object obj, Object obj2) {
        return this.f$0.accept(obj, obj2);
    }
}
