package java.util;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

final /* synthetic */ class -$Lambda$Hazqao1eYCE_pmZR5Jlrc2GvLhk implements Comparator, Serializable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Comparable) ((Function) this.-$f0).apply(arg0)).compareTo(((Function) this.-$f0).apply(arg1));
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return Double.compare(((ToDoubleFunction) this.-$f0).applyAsDouble(arg0), ((ToDoubleFunction) this.-$f0).applyAsDouble(arg1));
    }

    private final /* synthetic */ int $m$2(Object arg0, Object arg1) {
        return Integer.compare(((ToIntFunction) this.-$f0).applyAsInt(arg0), ((ToIntFunction) this.-$f0).applyAsInt(arg1));
    }

    private final /* synthetic */ int $m$3(Object arg0, Object arg1) {
        return Long.compare(((ToLongFunction) this.-$f0).applyAsLong(arg0), ((ToLongFunction) this.-$f0).applyAsLong(arg1));
    }

    private final /* synthetic */ int $m$4(Object arg0, Object arg1) {
        return ((Comparator) this.-$f0).compare(((Entry) arg0).getKey(), ((Entry) arg1).getKey());
    }

    private final /* synthetic */ int $m$5(Object arg0, Object arg1) {
        return ((Comparator) this.-$f0).compare(((Entry) arg0).getValue(), ((Entry) arg1).getValue());
    }

    public /* synthetic */ -$Lambda$Hazqao1eYCE_pmZR5Jlrc2GvLhk(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            case (byte) 2:
                return $m$2(obj, obj2);
            case (byte) 3:
                return $m$3(obj, obj2);
            case (byte) 4:
                return $m$4(obj, obj2);
            case (byte) 5:
                return $m$5(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
