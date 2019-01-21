package java.util.stream;

import java.util.stream.Sink.OfDouble;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$xWqUKn-t_aBWo9sD9bohYsGFiXg implements OfDouble {
    private final /* synthetic */ SpinedBuffer.OfDouble f$0;

    public /* synthetic */ -$$Lambda$xWqUKn-t_aBWo9sD9bohYsGFiXg(SpinedBuffer.OfDouble ofDouble) {
        this.f$0 = ofDouble;
    }

    public final void accept(double d) {
        this.f$0.accept(d);
    }
}
