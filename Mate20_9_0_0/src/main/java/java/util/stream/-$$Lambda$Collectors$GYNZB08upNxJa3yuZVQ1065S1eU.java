package java.util.stream;

import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$GYNZB08upNxJa3yuZVQ1065S1eU implements BinaryOperator {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$GYNZB08upNxJa3yuZVQ1065S1eU(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final Object apply(Object obj, Object obj2) {
        return new Partition(this.f$0.apply(((Partition) obj).forTrue, ((Partition) obj2).forTrue), this.f$0.apply(((Partition) obj).forFalse, ((Partition) obj2).forFalse));
    }
}
