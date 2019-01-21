package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$tdo0RtAvE1xjo0CFx2-92T1yRzQ implements TemporalAdjuster {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$TemporalAdjusters$tdo0RtAvE1xjo0CFx2-92T1yRzQ(int i, int i2) {
        this.f$0 = i;
        this.f$1 = i2;
    }

    public final Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.DAY_OF_MONTH, 1);
    }
}
