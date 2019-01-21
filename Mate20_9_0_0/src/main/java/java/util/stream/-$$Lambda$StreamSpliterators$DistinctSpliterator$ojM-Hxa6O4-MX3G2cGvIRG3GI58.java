package java.util.stream;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StreamSpliterators$DistinctSpliterator$ojM-Hxa6O4-MX3G2cGvIRG3GI58 implements Consumer {
    private final /* synthetic */ DistinctSpliterator f$0;
    private final /* synthetic */ Consumer f$1;

    public /* synthetic */ -$$Lambda$StreamSpliterators$DistinctSpliterator$ojM-Hxa6O4-MX3G2cGvIRG3GI58(DistinctSpliterator distinctSpliterator, Consumer consumer) {
        this.f$0 = distinctSpliterator;
        this.f$1 = consumer;
    }

    public final void accept(Object obj) {
        DistinctSpliterator.lambda$forEachRemaining$0(this.f$0, this.f$1, obj);
    }
}
