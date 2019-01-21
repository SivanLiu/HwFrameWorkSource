package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DoubleConsumer$HNSB3MjwB-DXE7Kpt1C-BT9h3T8 implements DoubleConsumer {
    private final /* synthetic */ DoubleConsumer f$0;
    private final /* synthetic */ DoubleConsumer f$1;

    public /* synthetic */ -$$Lambda$DoubleConsumer$HNSB3MjwB-DXE7Kpt1C-BT9h3T8(DoubleConsumer doubleConsumer, DoubleConsumer doubleConsumer2) {
        this.f$0 = doubleConsumer;
        this.f$1 = doubleConsumer2;
    }

    public final void accept(double d) {
        DoubleConsumer.lambda$andThen$0(this.f$0, this.f$1, d);
    }
}
