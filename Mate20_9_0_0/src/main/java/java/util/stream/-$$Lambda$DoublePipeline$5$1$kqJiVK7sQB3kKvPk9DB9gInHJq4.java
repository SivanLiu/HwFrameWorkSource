package java.util.stream;

import java.util.function.DoubleConsumer;
import java.util.stream.DoublePipeline.5.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoublePipeline$5$1$kqJiVK7sQB3kKvPk9DB9gInHJq4 implements DoubleConsumer {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$DoublePipeline$5$1$kqJiVK7sQB3kKvPk9DB9gInHJq4(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void accept(double d) {
        this.f$0.downstream.accept(d);
    }
}
