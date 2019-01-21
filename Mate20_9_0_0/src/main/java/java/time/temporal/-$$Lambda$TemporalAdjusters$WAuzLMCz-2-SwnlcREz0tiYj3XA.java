package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$WAuzLMCz-2-SwnlcREz0tiYj3XA implements TemporalAdjuster {
    public static final /* synthetic */ -$$Lambda$TemporalAdjusters$WAuzLMCz-2-SwnlcREz0tiYj3XA INSTANCE = new -$$Lambda$TemporalAdjusters$WAuzLMCz-2-SwnlcREz0tiYj3XA();

    private /* synthetic */ -$$Lambda$TemporalAdjusters$WAuzLMCz-2-SwnlcREz0tiYj3XA() {
    }

    public final Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.DAY_OF_MONTH, temporal.range(ChronoField.DAY_OF_MONTH).getMaximum());
    }
}
