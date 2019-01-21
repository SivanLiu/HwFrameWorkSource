package java.util.stream;

import java.util.function.IntConsumer;
import java.util.stream.IntPipeline.7.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntPipeline$7$1$E2wwNE1UnVxs0E9-n47lRWmnJGM implements IntConsumer {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$IntPipeline$7$1$E2wwNE1UnVxs0E9-n47lRWmnJGM(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void accept(int i) {
        this.f$0.downstream.accept(i);
    }
}
