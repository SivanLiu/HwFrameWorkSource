package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Consumer$fZIgy_f2Fa5seBa8ztxXTExq2p4 implements Consumer {
    private final /* synthetic */ Consumer f$0;
    private final /* synthetic */ Consumer f$1;

    public /* synthetic */ -$$Lambda$Consumer$fZIgy_f2Fa5seBa8ztxXTExq2p4(Consumer consumer, Consumer consumer2) {
        this.f$0 = consumer;
        this.f$1 = consumer2;
    }

    public final void accept(Object obj) {
        Consumer.lambda$andThen$0(this.f$0, this.f$1, obj);
    }
}
