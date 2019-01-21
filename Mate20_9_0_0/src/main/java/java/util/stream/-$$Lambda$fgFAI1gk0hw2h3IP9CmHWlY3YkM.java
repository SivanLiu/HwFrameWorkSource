package java.util.stream;

import java.util.function.DoubleConsumer;
import java.util.stream.Sink.OfDouble;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$fgFAI1gk0hw2h3IP9CmHWlY3YkM implements OfDouble {
    private final /* synthetic */ DoubleConsumer f$0;

    public /* synthetic */ -$$Lambda$fgFAI1gk0hw2h3IP9CmHWlY3YkM(DoubleConsumer doubleConsumer) {
        this.f$0 = doubleConsumer;
    }

    public final void accept(double d) {
        this.f$0.accept(d);
    }
}
