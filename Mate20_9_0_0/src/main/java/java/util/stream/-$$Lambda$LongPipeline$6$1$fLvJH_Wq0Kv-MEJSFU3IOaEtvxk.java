package java.util.stream;

import java.util.function.LongConsumer;
import java.util.stream.LongPipeline.6.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongPipeline$6$1$fLvJH_Wq0Kv-MEJSFU3IOaEtvxk implements LongConsumer {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$LongPipeline$6$1$fLvJH_Wq0Kv-MEJSFU3IOaEtvxk(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void accept(long j) {
        this.f$0.downstream.accept(j);
    }
}
