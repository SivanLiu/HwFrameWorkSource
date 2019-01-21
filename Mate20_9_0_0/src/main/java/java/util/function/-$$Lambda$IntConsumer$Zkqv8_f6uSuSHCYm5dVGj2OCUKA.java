package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IntConsumer$Zkqv8_f6uSuSHCYm5dVGj2OCUKA implements IntConsumer {
    private final /* synthetic */ IntConsumer f$0;
    private final /* synthetic */ IntConsumer f$1;

    public /* synthetic */ -$$Lambda$IntConsumer$Zkqv8_f6uSuSHCYm5dVGj2OCUKA(IntConsumer intConsumer, IntConsumer intConsumer2) {
        this.f$0 = intConsumer;
        this.f$1 = intConsumer2;
    }

    public final void accept(int i) {
        IntConsumer.lambda$andThen$0(this.f$0, this.f$1, i);
    }
}
