package java.util;

import java.io.Serializable;
import java.util.Map.Entry;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Map$Entry$g8sc1MgjjhwTaK8zHulzMasixMw implements Comparator, Serializable {
    private final /* synthetic */ Comparator f$0;

    public /* synthetic */ -$$Lambda$Map$Entry$g8sc1MgjjhwTaK8zHulzMasixMw(Comparator comparator) {
        this.f$0 = comparator;
    }

    public final int compare(Object obj, Object obj2) {
        return this.f$0.compare(((Entry) obj).getKey(), ((Entry) obj2).getKey());
    }
}
