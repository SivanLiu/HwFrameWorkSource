package java.util.stream;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$BwzHl6O1mjAgxLE58ctIeFoVBAM implements Supplier {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$BwzHl6O1mjAgxLE58ctIeFoVBAM(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final Object get() {
        return new Consumer<T>(this.f$0) {
            boolean present = false;
            T value = null;

            public void accept(T t) {
                if (this.present) {
                    this.value = op.apply(this.value, t);
                    return;
                }
                this.value = t;
                this.present = true;
            }
        };
    }
}
