package java.time.temporal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalQueries$okxqZ6ZoOhHd_zSzW7k5qRIaLxM implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$TemporalQueries$okxqZ6ZoOhHd_zSzW7k5qRIaLxM INSTANCE = new -$$Lambda$TemporalQueries$okxqZ6ZoOhHd_zSzW7k5qRIaLxM();

    private /* synthetic */ -$$Lambda$TemporalQueries$okxqZ6ZoOhHd_zSzW7k5qRIaLxM() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return ((TemporalUnit) temporalAccessor.query(TemporalQueries.PRECISION));
    }
}
