package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LongConsumer$2wx0fq0YJI8kSCwhsFrV0qxRiZ4 implements LongConsumer {
    private final /* synthetic */ LongConsumer f$0;
    private final /* synthetic */ LongConsumer f$1;

    public /* synthetic */ -$$Lambda$LongConsumer$2wx0fq0YJI8kSCwhsFrV0qxRiZ4(LongConsumer longConsumer, LongConsumer longConsumer2) {
        this.f$0 = longConsumer;
        this.f$1 = longConsumer2;
    }

    public final void accept(long j) {
        LongConsumer.lambda$andThen$0(this.f$0, this.f$1, j);
    }
}
