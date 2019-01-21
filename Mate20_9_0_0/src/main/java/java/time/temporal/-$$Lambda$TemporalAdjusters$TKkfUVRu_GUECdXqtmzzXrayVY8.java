package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$TKkfUVRu_GUECdXqtmzzXrayVY8 implements TemporalAdjuster {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$TemporalAdjusters$TKkfUVRu_GUECdXqtmzzXrayVY8(int i) {
        this.f$0 = i;
    }

    public final Temporal adjustInto(Temporal temporal) {
        return TemporalAdjusters.lambda$previousOrSame$12(this.f$0, temporal);
    }
}
