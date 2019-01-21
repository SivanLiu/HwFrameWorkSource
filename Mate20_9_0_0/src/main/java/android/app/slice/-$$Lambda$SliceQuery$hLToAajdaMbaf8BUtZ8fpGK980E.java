package android.app.slice;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceQuery$hLToAajdaMbaf8BUtZ8fpGK980E implements Predicate {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ String[] f$1;
    private final /* synthetic */ String[] f$2;

    public /* synthetic */ -$$Lambda$SliceQuery$hLToAajdaMbaf8BUtZ8fpGK980E(String str, String[] strArr, String[] strArr2) {
        this.f$0 = str;
        this.f$1 = strArr;
        this.f$2 = strArr2;
    }

    public final boolean test(Object obj) {
        return SliceQuery.lambda$findAll$1(this.f$0, this.f$1, this.f$2, (SliceItem) obj);
    }
}
