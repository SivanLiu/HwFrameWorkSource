package java.time.chrono;

import java.io.Serializable;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$2u9I1kadVYC2Q_h8lznNWkqzo1s implements Comparator, Serializable {
    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return Long.compare(((ChronoLocalDate) arg0).toEpochDay(), ((ChronoLocalDate) arg1).toEpochDay());
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
