package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JBWLm7jbzHiLhHxYdB_IuO_vFO8 implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$JBWLm7jbzHiLhHxYdB_IuO_vFO8 INSTANCE = new -$$Lambda$JBWLm7jbzHiLhHxYdB_IuO_vFO8();

    private /* synthetic */ -$$Lambda$JBWLm7jbzHiLhHxYdB_IuO_vFO8() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return LocalDateTime.from(temporalAccessor);
    }
}
