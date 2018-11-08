package java.time.temporal;

import java.time.ZoneId;

final /* synthetic */ class -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE implements TemporalQuery {
    private final /* synthetic */ Object $m$0(TemporalAccessor arg0) {
        return ((ZoneId) arg0.query(TemporalQueries.ZONE_ID));
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return $m$0(temporalAccessor);
    }
}
