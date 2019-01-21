package java.util.function;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BinaryOperator$V_WUclL0kAOZvMw9EtWtwAvmNJc implements BinaryOperator {
    private final /* synthetic */ Comparator f$0;

    public /* synthetic */ -$$Lambda$BinaryOperator$V_WUclL0kAOZvMw9EtWtwAvmNJc(Comparator comparator) {
        this.f$0 = comparator;
    }

    public final Object apply(Object obj, Object obj2) {
        return BinaryOperator.lambda$maxBy$1(this.f$0, obj, obj2);
    }
}
