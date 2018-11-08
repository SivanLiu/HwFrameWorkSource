package java.time.temporal;

import java.time.ZoneId;
import java.time.chrono.Chronology;

final /* synthetic */ class -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE implements TemporalQuery {
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$0 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 0);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$1 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 1);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$2 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 2);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$3 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 3);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$4 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 4);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$5 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 5);
    public static final /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE $INST$6 = new -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE((byte) 6);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(TemporalAccessor arg0) {
        return ((ZoneId) arg0.query(TemporalQueries.ZONE_ID));
    }

    private final /* synthetic */ Object $m$1(TemporalAccessor arg0) {
        return ((Chronology) arg0.query(TemporalQueries.CHRONO));
    }

    private final /* synthetic */ Object $m$2(TemporalAccessor arg0) {
        return ((TemporalUnit) arg0.query(TemporalQueries.PRECISION));
    }

    private final /* synthetic */ Object $m$3(TemporalAccessor arg0) {
        return TemporalQueries.lambda$-java_time_temporal_TemporalQueries_16950(arg0);
    }

    private final /* synthetic */ Object $m$4(TemporalAccessor arg0) {
        return TemporalQueries.lambda$-java_time_temporal_TemporalQueries_17282(arg0);
    }

    private final /* synthetic */ Object $m$5(TemporalAccessor arg0) {
        return TemporalQueries.lambda$-java_time_temporal_TemporalQueries_17553(arg0);
    }

    private final /* synthetic */ Object $m$6(TemporalAccessor arg0) {
        return TemporalQueries.lambda$-java_time_temporal_TemporalQueries_17862(arg0);
    }

    private /* synthetic */ -$Lambda$Tdj786c1ErPPulwwPKXine_EWQE(byte b) {
        this.$id = b;
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(temporalAccessor);
            case (byte) 1:
                return $m$1(temporalAccessor);
            case (byte) 2:
                return $m$2(temporalAccessor);
            case (byte) 3:
                return $m$3(temporalAccessor);
            case (byte) 4:
                return $m$4(temporalAccessor);
            case (byte) 5:
                return $m$5(temporalAccessor);
            case (byte) 6:
                return $m$6(temporalAccessor);
            default:
                throw new AssertionError();
        }
    }
}
