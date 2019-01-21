package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$102LK-VjqD_Dw4HKR2kUw-BMsRk implements TemporalQuery {
    public static final /* synthetic */ -$$Lambda$102LK-VjqD_Dw4HKR2kUw-BMsRk INSTANCE = new -$$Lambda$102LK-VjqD_Dw4HKR2kUw-BMsRk();

    private /* synthetic */ -$$Lambda$102LK-VjqD_Dw4HKR2kUw-BMsRk() {
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        return YearMonth.from(temporalAccessor);
    }
}
