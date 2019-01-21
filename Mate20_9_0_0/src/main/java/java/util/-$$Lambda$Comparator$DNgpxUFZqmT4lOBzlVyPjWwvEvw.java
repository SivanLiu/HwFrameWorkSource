package java.util;

import java.io.Serializable;
import java.util.function.ToIntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw implements Comparator, Serializable {
    private final /* synthetic */ ToIntFunction f$0;

    public /* synthetic */ -$$Lambda$Comparator$DNgpxUFZqmT4lOBzlVyPjWwvEvw(ToIntFunction toIntFunction) {
        this.f$0 = toIntFunction;
    }

    public final int compare(Object obj, Object obj2) {
        return Integer.compare(this.f$0.applyAsInt(obj), this.f$0.applyAsInt(obj2));
    }
}
