package java.util;

import java.io.Serializable;
import java.util.Map.Entry;

final /* synthetic */ class -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI implements Comparator, Serializable {
    public static final /* synthetic */ -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI $INST$0 = new -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI((byte) 0);
    public static final /* synthetic */ -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI $INST$1 = new -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI((byte) 1);
    public static final /* synthetic */ -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI $INST$2 = new -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI((byte) 2);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Comparable) ((Entry) arg0).getKey()).compareTo(((Entry) arg1).getKey());
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return ((Comparable) ((Entry) arg0).getValue()).compareTo(((Entry) arg1).getValue());
    }

    private final /* synthetic */ int $m$2(Object arg0, Object arg1) {
        return ((Comparable) ((Entry) arg0).getKey()).compareTo(((Entry) arg1).getKey());
    }

    private /* synthetic */ -$Lambda$mgsD_SQg4eUJB-7NqkY6phgRxHI(byte b) {
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
