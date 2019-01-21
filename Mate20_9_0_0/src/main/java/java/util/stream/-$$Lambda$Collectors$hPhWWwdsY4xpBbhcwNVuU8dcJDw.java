package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$hPhWWwdsY4xpBbhcwNVuU8dcJDw implements BiConsumer {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$hPhWWwdsY4xpBbhcwNVuU8dcJDw(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final void accept(Object obj, Object obj2) {
        ((Object[]) obj)[0] = this.f$0.apply(((Object[]) obj)[0], obj2);
    }
}
