package java.time.temporal;

import java.time.ZoneId;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalQueries$IZUinmsZUz98YXPe0ftAd27ByiE implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$TemporalQueries$IZUinmsZUz98YXPe0ftAd27ByiE INSTANCE = new -$$Lambda$TemporalQueries$IZUinmsZUz98YXPe0ftAd27ByiE();

    private /* synthetic */ -$$Lambda$TemporalQueries$IZUinmsZUz98YXPe0ftAd27ByiE() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return ((ZoneId) temporalAccessor.query(TemporalQueries.ZONE_ID));
    }
}
