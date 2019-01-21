package java.util.stream;

import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$ad8ZNOGiSpsXDY-HtABmtE9E1UA implements BinaryOperator {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$ad8ZNOGiSpsXDY-HtABmtE9E1UA(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final Object apply(Object obj, Object obj2) {
        return ((Object[]) obj)[0] = this.f$0.apply(((Object[]) obj)[0], ((Object[]) obj2)[0]);
    }
}
