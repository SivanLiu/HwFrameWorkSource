package java.util;

import java.io.Serializable;
import java.util.Map.Entry;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Map$Entry$Y3nKRmSXx8yzU_Ap-vOwqAqvBas implements Comparator, Serializable {
    private final /* synthetic */ Comparator f$0;

    public /* synthetic */ -$$Lambda$Map$Entry$Y3nKRmSXx8yzU_Ap-vOwqAqvBas(Comparator comparator) {
        this.f$0 = comparator;
    }

    public final int compare(Object obj, Object obj2) {
        return this.f$0.compare(((Entry) obj).getValue(), ((Entry) obj2).getValue());
    }
}
