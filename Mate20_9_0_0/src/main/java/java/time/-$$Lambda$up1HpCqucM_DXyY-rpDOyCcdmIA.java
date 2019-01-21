package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$up1HpCqucM_DXyY-rpDOyCcdmIA implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$up1HpCqucM_DXyY-rpDOyCcdmIA INSTANCE = new -$$Lambda$up1HpCqucM_DXyY-rpDOyCcdmIA();

    private /* synthetic */ -$$Lambda$up1HpCqucM_DXyY-rpDOyCcdmIA() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return ZonedDateTime.from(temporalAccessor);
    }
}
