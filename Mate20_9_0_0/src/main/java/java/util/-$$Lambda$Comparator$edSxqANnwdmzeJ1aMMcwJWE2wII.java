package java.util;

import java.io.Serializable;
import java.util.function.ToDoubleFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII implements Comparator, Serializable {
    private final /* synthetic */ ToDoubleFunction f$0;

    public /* synthetic */ -$$Lambda$Comparator$edSxqANnwdmzeJ1aMMcwJWE2wII(ToDoubleFunction toDoubleFunction) {
        this.f$0 = toDoubleFunction;
    }

    public final int compare(Object obj, Object obj2) {
        return Double.compare(this.f$0.applyAsDouble(obj), this.f$0.applyAsDouble(obj2));
    }
}
