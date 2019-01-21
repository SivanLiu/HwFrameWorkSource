package java.util;

import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collections$CheckedMap$UHd_dm3NjSdMnE1bZpQe0MXp_Gk implements BiFunction {
    private final /* synthetic */ CheckedMap f$0;
    private final /* synthetic */ BiFunction f$1;

    public /* synthetic */ -$$Lambda$Collections$CheckedMap$UHd_dm3NjSdMnE1bZpQe0MXp_Gk(CheckedMap checkedMap, BiFunction biFunction) {
        this.f$0 = checkedMap;
        this.f$1 = biFunction;
    }

    public final Object apply(Object obj, Object obj2) {
        return CheckedMap.lambda$merge$2(this.f$0, this.f$1, obj, obj2);
    }
}
