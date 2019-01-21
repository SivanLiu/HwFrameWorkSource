package java.util;

import java.io.Serializable;
import java.util.function.ToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo implements Comparator, Serializable {
    private final /* synthetic */ ToLongFunction f$0;

    public /* synthetic */ -$$Lambda$Comparator$4V5k8aLimtS0VsEILEAqQ9UGZYo(ToLongFunction toLongFunction) {
        this.f$0 = toLongFunction;
    }

    public final int compare(Object obj, Object obj2) {
        return Long.compare(this.f$0.applyAsLong(obj), this.f$0.applyAsLong(obj2));
    }
}
