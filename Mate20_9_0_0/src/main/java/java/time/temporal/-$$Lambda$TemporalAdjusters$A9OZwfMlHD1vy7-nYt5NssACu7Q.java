package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$A9OZwfMlHD1vy7-nYt5NssACu7Q implements TemporalAdjuster {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$TemporalAdjusters$A9OZwfMlHD1vy7-nYt5NssACu7Q(int i) {
        this.f$0 = i;
    }

    public final Temporal adjustInto(Temporal temporal) {
        return TemporalAdjusters.lambda$nextOrSame$10(this.f$0, temporal);
    }
}
