package java.util.stream;

import java.util.Set;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$SMVdf7W0ks2OOmS3zJw7DHc-Nhc implements BinaryOperator {
    public static final /* synthetic */ -$$Lambda$Collectors$SMVdf7W0ks2OOmS3zJw7DHc-Nhc INSTANCE = new -$$Lambda$Collectors$SMVdf7W0ks2OOmS3zJw7DHc-Nhc();

    private /* synthetic */ -$$Lambda$Collectors$SMVdf7W0ks2OOmS3zJw7DHc-Nhc() {
    }

    public final Object apply(Object obj, Object obj2) {
        return ((Set) obj).addAll((Set) obj2);
    }
}
