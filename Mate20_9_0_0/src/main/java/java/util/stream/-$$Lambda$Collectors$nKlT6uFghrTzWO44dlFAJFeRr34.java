package java.util.stream;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$nKlT6uFghrTzWO44dlFAJFeRr34 implements BiConsumer {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Function f$1;
    private final /* synthetic */ BinaryOperator f$2;

    public /* synthetic */ -$$Lambda$Collectors$nKlT6uFghrTzWO44dlFAJFeRr34(Function function, Function function2, BinaryOperator binaryOperator) {
        this.f$0 = function;
        this.f$1 = function2;
        this.f$2 = binaryOperator;
    }

    public final void accept(Object obj, Object obj2) {
        ((Map) obj).merge(this.f$0.apply(obj2), this.f$1.apply(obj2), this.f$2);
    }
}
