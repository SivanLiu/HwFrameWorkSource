package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$PTd6jsJ7t0481HRFfH8tnGifDqw implements BiConsumer {
    private final /* synthetic */ BinaryOperator f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Collectors$PTd6jsJ7t0481HRFfH8tnGifDqw(BinaryOperator binaryOperator, Function function) {
        this.f$0 = binaryOperator;
        this.f$1 = function;
    }

    public final void accept(Object obj, Object obj2) {
        ((Object[]) obj)[0] = this.f$0.apply(((Object[]) obj)[0], this.f$1.apply(obj2));
    }
}
