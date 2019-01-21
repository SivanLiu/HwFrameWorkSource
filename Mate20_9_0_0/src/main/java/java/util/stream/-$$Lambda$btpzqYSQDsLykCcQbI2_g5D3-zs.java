package java.util.stream;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$btpzqYSQDsLykCcQbI2_g5D3-zs implements Sink {
    private final /* synthetic */ Consumer f$0;

    public /* synthetic */ -$$Lambda$btpzqYSQDsLykCcQbI2_g5D3-zs(Consumer consumer) {
        this.f$0 = consumer;
    }

    public final void accept(Object obj) {
        this.f$0.accept(obj);
    }
}
