package java.util.stream;

import java.util.function.LongConsumer;
import java.util.stream.Sink.OfLong;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$G3FiaNZPcIIAnGkHVY7Mdu42X5g implements OfLong {
    private final /* synthetic */ LongConsumer f$0;

    public /* synthetic */ -$$Lambda$G3FiaNZPcIIAnGkHVY7Mdu42X5g(LongConsumer longConsumer) {
        this.f$0 = longConsumer;
    }

    public final void accept(long j) {
        this.f$0.accept(j);
    }
}
