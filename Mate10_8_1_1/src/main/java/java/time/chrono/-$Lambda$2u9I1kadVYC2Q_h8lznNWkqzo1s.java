package java.time.chrono;

import java.io.Serializable;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s implements Comparator, Serializable {
    public static final /* synthetic */ -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s $INST$0 = new -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s((byte) 0);
    public static final /* synthetic */ -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s $INST$1 = new -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s((byte) 1);
    public static final /* synthetic */ -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s $INST$2 = new -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s((byte) 2);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return Long.compare(((ChronoLocalDate) arg0).toEpochDay(), ((ChronoLocalDate) arg1).toEpochDay());
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return AbstractChronology.lambda$-java_time_chrono_AbstractChronology_6277((ChronoLocalDateTime) arg0, (ChronoLocalDateTime) arg1);
    }

    private final /* synthetic */ int $m$2(Object arg0, Object arg1) {
        return AbstractChronology.lambda$-java_time_chrono_AbstractChronology_6799((ChronoZonedDateTime) arg0, (ChronoZonedDateTime) arg1);
    }

    private /* synthetic */ -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s(byte b) {
        this.$id = b;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            case (byte) 2:
                return $m$2(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
