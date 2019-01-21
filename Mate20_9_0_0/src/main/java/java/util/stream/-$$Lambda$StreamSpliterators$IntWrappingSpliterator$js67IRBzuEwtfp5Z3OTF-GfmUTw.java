package java.util.stream;

import java.util.function.BooleanSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StreamSpliterators$IntWrappingSpliterator$js67IRBzuEwtfp5Z3OTF-GfmUTw implements BooleanSupplier {
    private final /* synthetic */ IntWrappingSpliterator f$0;

    public /* synthetic */ -$$Lambda$StreamSpliterators$IntWrappingSpliterator$js67IRBzuEwtfp5Z3OTF-GfmUTw(IntWrappingSpliterator intWrappingSpliterator) {
        this.f$0 = intWrappingSpliterator;
    }

    public final boolean getAsBoolean() {
        return this.f$0.spliterator.tryAdvance(this.f$0.bufferSink);
    }
}
