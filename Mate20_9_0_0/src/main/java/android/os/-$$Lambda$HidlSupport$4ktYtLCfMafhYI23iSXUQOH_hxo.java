package android.os;

import java.util.function.IntPredicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HidlSupport$4ktYtLCfMafhYI23iSXUQOH_hxo implements IntPredicate {
    private final /* synthetic */ Object[] f$0;
    private final /* synthetic */ Object[] f$1;

    public /* synthetic */ -$$Lambda$HidlSupport$4ktYtLCfMafhYI23iSXUQOH_hxo(Object[] objArr, Object[] objArr2) {
        this.f$0 = objArr;
        this.f$1 = objArr2;
    }

    public final boolean test(int i) {
        return HidlSupport.deepEquals(this.f$0[i], this.f$1[i]);
    }
}
