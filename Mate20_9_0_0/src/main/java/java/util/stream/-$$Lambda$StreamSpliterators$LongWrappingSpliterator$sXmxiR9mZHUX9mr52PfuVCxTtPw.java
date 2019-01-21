package java.util.stream;

import java.util.function.BooleanSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StreamSpliterators$LongWrappingSpliterator$sXmxiR9mZHUX9mr52PfuVCxTtPw implements BooleanSupplier {
    private final /* synthetic */ LongWrappingSpliterator f$0;

    public /* synthetic */ -$$Lambda$StreamSpliterators$LongWrappingSpliterator$sXmxiR9mZHUX9mr52PfuVCxTtPw(LongWrappingSpliterator longWrappingSpliterator) {
        this.f$0 = longWrappingSpliterator;
    }

    public final boolean getAsBoolean() {
        return this.f$0.spliterator.tryAdvance(this.f$0.bufferSink);
    }
}
