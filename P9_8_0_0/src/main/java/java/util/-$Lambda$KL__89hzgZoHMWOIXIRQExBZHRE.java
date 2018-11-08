package java.util;

import java.io.Serializable;
import java.util.Map.Entry;

final /* synthetic */ class -$Lambda$KL__89hzgZoHMWOIXIRQExBZHRE implements Comparator, Serializable {
    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Comparable) ((Entry) arg0).getKey()).compareTo(((Entry) arg1).getKey());
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
