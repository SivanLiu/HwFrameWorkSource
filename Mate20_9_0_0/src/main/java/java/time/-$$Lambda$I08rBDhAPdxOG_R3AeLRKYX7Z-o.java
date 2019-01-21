package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$I08rBDhAPdxOG_R3AeLRKYX7Z-o implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$I08rBDhAPdxOG_R3AeLRKYX7Z-o INSTANCE = new -$$Lambda$I08rBDhAPdxOG_R3AeLRKYX7Z-o();

    private /* synthetic */ -$$Lambda$I08rBDhAPdxOG_R3AeLRKYX7Z-o() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return OffsetTime.from(temporalAccessor);
    }
}
