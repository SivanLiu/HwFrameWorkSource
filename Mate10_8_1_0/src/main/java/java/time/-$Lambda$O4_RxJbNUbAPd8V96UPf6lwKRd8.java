package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

final /* synthetic */ class -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 implements TemporalQuery {
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$0 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 0);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$1 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 1);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$2 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 2);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$3 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 3);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$4 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 4);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$5 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 5);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$6 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 6);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$7 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 7);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$8 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 8);
    public static final /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8 $INST$9 = new -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8((byte) 9);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(TemporalAccessor arg0) {
        return Instant.from(arg0);
    }

    private final /* synthetic */ Object $m$1(TemporalAccessor arg0) {
        return LocalDate.from(arg0);
    }

    private final /* synthetic */ Object $m$2(TemporalAccessor arg0) {
        return LocalDateTime.from(arg0);
    }

    private final /* synthetic */ Object $m$3(TemporalAccessor arg0) {
        return LocalTime.from(arg0);
    }

    private final /* synthetic */ Object $m$4(TemporalAccessor arg0) {
        return MonthDay.from(arg0);
    }

    private final /* synthetic */ Object $m$5(TemporalAccessor arg0) {
        return OffsetDateTime.from(arg0);
    }

    private final /* synthetic */ Object $m$6(TemporalAccessor arg0) {
        return OffsetTime.from(arg0);
    }

    private final /* synthetic */ Object $m$7(TemporalAccessor arg0) {
        return Year.from(arg0);
    }

    private final /* synthetic */ Object $m$8(TemporalAccessor arg0) {
        return YearMonth.from(arg0);
    }

    private final /* synthetic */ Object $m$9(TemporalAccessor arg0) {
        return ZonedDateTime.from(arg0);
    }

    private /* synthetic */ -$Lambda$O4_RxJbNUbAPd8V96UPf6lwKRd8(byte b) {
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
            case (byte) 7:
                return $m$7(temporalAccessor);
            case (byte) 8:
                return $m$8(temporalAccessor);
            case (byte) 9:
                return $m$9(temporalAccessor);
            default:
                throw new AssertionError();
        }
    }
}
