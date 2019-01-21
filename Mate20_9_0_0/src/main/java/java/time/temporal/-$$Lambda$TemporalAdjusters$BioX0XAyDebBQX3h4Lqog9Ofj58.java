package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$BioX0XAyDebBQX3h4Lqog9Ofj58 implements TemporalAdjuster {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$TemporalAdjusters$BioX0XAyDebBQX3h4Lqog9Ofj58(int i, int i2) {
        this.f$0 = i;
        this.f$1 = i2;
    }

    public final Temporal adjustInto(Temporal temporal) {
        return TemporalAdjusters.lambda$dayOfWeekInMonth$8(this.f$0, this.f$1, temporal);
    }
}
