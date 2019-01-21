package java.util;

import java.io.Serializable;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o implements Comparator, Serializable {
    private final /* synthetic */ Comparator f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Comparator$KVN0LWz1D1wyrL2gs1CbubvLa9o(Comparator comparator, Function function) {
        this.f$0 = comparator;
        this.f$1 = function;
    }

    public final int compare(Object obj, Object obj2) {
        return this.f$0.compare(this.f$1.apply(obj), this.f$1.apply(obj2));
    }
}
