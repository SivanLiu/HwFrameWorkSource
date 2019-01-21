package java.util.stream;

import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$xWcI8qM0AfOYfxDSjg08D8ztBFg implements BinaryOperator {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$xWcI8qM0AfOYfxDSjg08D8ztBFg(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final Object apply(Object obj, Object obj2) {
        return ((Object[]) obj)[0] = this.f$0.apply(((Object[]) obj)[0], ((Object[]) obj2)[0]);
    }
}
