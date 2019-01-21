package java.util.stream;

import java.util.Collection;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$iab6bVO3ydceISRiUEq_MRHYzoU implements BinaryOperator {
    public static final /* synthetic */ -$$Lambda$Collectors$iab6bVO3ydceISRiUEq_MRHYzoU INSTANCE = new -$$Lambda$Collectors$iab6bVO3ydceISRiUEq_MRHYzoU();

    private /* synthetic */ -$$Lambda$Collectors$iab6bVO3ydceISRiUEq_MRHYzoU() {
    }

    public final Object apply(Object obj, Object obj2) {
        return ((Collection) obj).addAll((Collection) obj2);
    }
}
