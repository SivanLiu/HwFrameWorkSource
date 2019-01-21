package java.util.stream;

import java.util.function.BooleanSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StreamSpliterators$DoubleWrappingSpliterator$vGvekEV3XchaSAEI93tmYCeVG9A implements BooleanSupplier {
    private final /* synthetic */ DoubleWrappingSpliterator f$0;

    public /* synthetic */ -$$Lambda$StreamSpliterators$DoubleWrappingSpliterator$vGvekEV3XchaSAEI93tmYCeVG9A(DoubleWrappingSpliterator doubleWrappingSpliterator) {
        this.f$0 = doubleWrappingSpliterator;
    }

    public final boolean getAsBoolean() {
        return this.f$0.spliterator.tryAdvance(this.f$0.bufferSink);
    }
}
