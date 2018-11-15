package java.util.concurrent;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map.Entry;

final /* synthetic */ class -$Lambda$maBT9lzzYgJpLy8T-HMyKdSOI8M implements Comparator, Serializable {
    public static final /* synthetic */ -$Lambda$maBT9lzzYgJpLy8T-HMyKdSOI8M $INST$0 = new -$Lambda$maBT9lzzYgJpLy8T-HMyKdSOI8M();

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Comparable) ((Entry) arg0).getKey()).compareTo(((Entry) arg1).getKey());
    }

    private /* synthetic */ -$Lambda$maBT9lzzYgJpLy8T-HMyKdSOI8M() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
