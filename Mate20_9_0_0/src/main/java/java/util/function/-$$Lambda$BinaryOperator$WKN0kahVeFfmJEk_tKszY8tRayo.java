package java.util.function;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BinaryOperator$WKN0kahVeFfmJEk_tKszY8tRayo implements BinaryOperator {
    private final /* synthetic */ Comparator f$0;

    public /* synthetic */ -$$Lambda$BinaryOperator$WKN0kahVeFfmJEk_tKszY8tRayo(Comparator comparator) {
        this.f$0 = comparator;
    }

    public final Object apply(Object obj, Object obj2) {
        return BinaryOperator.lambda$minBy$0(this.f$0, obj, obj2);
    }
}
