package java.util.stream;

import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$ZHtBI7Du2F_qzRSEqDnG6y4R0Lw implements BiConsumer {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Function f$1;
    private final /* synthetic */ BinaryOperator f$2;

    public /* synthetic */ -$$Lambda$Collectors$ZHtBI7Du2F_qzRSEqDnG6y4R0Lw(Function function, Function function2, BinaryOperator binaryOperator) {
        this.f$0 = function;
        this.f$1 = function2;
        this.f$2 = binaryOperator;
    }

    public final void accept(Object obj, Object obj2) {
        ((ConcurrentMap) obj).merge(this.f$0.apply(obj2), this.f$1.apply(obj2), this.f$2);
    }
}
