package java.util.stream;

import java.util.function.IntConsumer;
import java.util.stream.Sink.OfInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$C9lt_0Cg-SARhdNFJsMyHSsCsGA implements OfInt {
    private final /* synthetic */ IntConsumer f$0;

    public /* synthetic */ -$$Lambda$C9lt_0Cg-SARhdNFJsMyHSsCsGA(IntConsumer intConsumer) {
        this.f$0 = intConsumer;
    }

    public final void accept(int i) {
        this.f$0.accept(i);
    }
}
