package java.util;

import java.io.Serializable;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw implements Comparator, Serializable {
    private final /* synthetic */ Function f$0;

    public /* synthetic */ -$$Lambda$Comparator$SPB8K9Yj7Pw1mljm7LpasV7zxWw(Function function) {
        this.f$0 = function;
    }

    public final int compare(Object obj, Object obj2) {
        return ((Comparable) this.f$0.apply(obj)).compareTo(this.f$0.apply(obj2));
    }
}
