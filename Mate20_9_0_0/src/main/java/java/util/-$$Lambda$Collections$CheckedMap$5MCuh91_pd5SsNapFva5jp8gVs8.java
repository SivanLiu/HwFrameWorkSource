package java.util;

import java.util.function.BiFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collections$CheckedMap$5MCuh91_pd5SsNapFva5jp8gVs8 implements BiFunction {
    private final /* synthetic */ CheckedMap f$0;
    private final /* synthetic */ BiFunction f$1;

    public /* synthetic */ -$$Lambda$Collections$CheckedMap$5MCuh91_pd5SsNapFva5jp8gVs8(CheckedMap checkedMap, BiFunction biFunction) {
        this.f$0 = checkedMap;
        this.f$1 = biFunction;
    }

    public final Object apply(Object obj, Object obj2) {
        return CheckedMap.lambda$typeCheck$0(this.f$0, this.f$1, obj, obj2);
    }
}
