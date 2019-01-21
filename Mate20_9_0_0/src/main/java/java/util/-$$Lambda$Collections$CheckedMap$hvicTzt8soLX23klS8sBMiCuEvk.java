package java.util;

import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collections$CheckedMap$hvicTzt8soLX23klS8sBMiCuEvk implements Function {
    private final /* synthetic */ CheckedMap f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Collections$CheckedMap$hvicTzt8soLX23klS8sBMiCuEvk(CheckedMap checkedMap, Function function) {
        this.f$0 = checkedMap;
        this.f$1 = function;
    }

    public final Object apply(Object obj) {
        return CheckedMap.lambda$computeIfAbsent$1(this.f$0, this.f$1, obj);
    }
}
