package java.time.temporal;

import java.time.chrono.Chronology;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalQueries$thd4JmExRUYKd7nNlE7b5oT19ms implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$TemporalQueries$thd4JmExRUYKd7nNlE7b5oT19ms INSTANCE = new -$$Lambda$TemporalQueries$thd4JmExRUYKd7nNlE7b5oT19ms();

    private /* synthetic */ -$$Lambda$TemporalQueries$thd4JmExRUYKd7nNlE7b5oT19ms() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return ((Chronology) temporalAccessor.query(TemporalQueries.CHRONO));
    }
}
