package java.time;

import java.util.Comparator;

final /* synthetic */ class -$Lambda$Kb7yteiMa_ottCEBOYEBjPQ4aes implements Comparator {
    public static final /* synthetic */ -$Lambda$Kb7yteiMa_ottCEBOYEBjPQ4aes $INST$0 = new -$Lambda$Kb7yteiMa_ottCEBOYEBjPQ4aes();

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return OffsetDateTime.compareInstant((OffsetDateTime) arg0, (OffsetDateTime) arg1);
    }

    private /* synthetic */ -$Lambda$Kb7yteiMa_ottCEBOYEBjPQ4aes() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
