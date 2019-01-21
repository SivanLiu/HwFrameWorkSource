package android.app.slice;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceQuery$cG9kHpHpv4nbm7p3sCvlkQGlqQw implements Predicate {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ String[] f$1;
    private final /* synthetic */ String[] f$2;

    public /* synthetic */ -$$Lambda$SliceQuery$cG9kHpHpv4nbm7p3sCvlkQGlqQw(String str, String[] strArr, String[] strArr2) {
        this.f$0 = str;
        this.f$1 = strArr;
        this.f$2 = strArr2;
    }

    public final boolean test(Object obj) {
        return SliceQuery.lambda$find$2(this.f$0, this.f$1, this.f$2, (SliceItem) obj);
    }
}
