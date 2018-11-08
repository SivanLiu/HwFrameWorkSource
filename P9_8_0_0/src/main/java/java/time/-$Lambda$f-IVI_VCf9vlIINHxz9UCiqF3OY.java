package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

final /* synthetic */ class -$Lambda$f-IVI_VCf9vlIINHxz9UCiqF3OY implements TemporalQuery {
    private final /* synthetic */ Object $m$0(TemporalAccessor arg0) {
        return LocalDateTime.from(arg0);
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return $m$0(temporalAccessor);
    }
}
