package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$jDXjrt99im0xRBROpkCAqoLkqR4 implements BiConsumer {
    private final /* synthetic */ ToLongFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$jDXjrt99im0xRBROpkCAqoLkqR4(ToLongFunction toLongFunction) {
        this.f$0 = toLongFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ((long[]) obj)[0] = ((long[]) obj)[0] + this.f$0.applyAsLong(obj2);
    }
}
