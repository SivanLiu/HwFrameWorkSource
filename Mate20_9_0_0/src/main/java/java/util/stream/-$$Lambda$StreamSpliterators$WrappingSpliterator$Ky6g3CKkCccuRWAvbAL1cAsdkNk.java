package java.util.stream;

import java.util.function.BooleanSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StreamSpliterators$WrappingSpliterator$Ky6g3CKkCccuRWAvbAL1cAsdkNk implements BooleanSupplier {
    private final /* synthetic */ WrappingSpliterator f$0;

    public /* synthetic */ -$$Lambda$StreamSpliterators$WrappingSpliterator$Ky6g3CKkCccuRWAvbAL1cAsdkNk(WrappingSpliterator wrappingSpliterator) {
        this.f$0 = wrappingSpliterator;
    }

    public final boolean getAsBoolean() {
        return this.f$0.spliterator.tryAdvance(this.f$0.bufferSink);
    }
}
