package android.app.slice;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceQuery$fdDPNErwIni-vCQ6k-MlGGBunoE implements Predicate {
    private final /* synthetic */ SliceItem f$0;

    public /* synthetic */ -$$Lambda$SliceQuery$fdDPNErwIni-vCQ6k-MlGGBunoE(SliceItem sliceItem) {
        this.f$0 = sliceItem;
    }

    public final boolean test(Object obj) {
        return SliceQuery.lambda$contains$0(this.f$0, (SliceItem) obj);
    }
}
