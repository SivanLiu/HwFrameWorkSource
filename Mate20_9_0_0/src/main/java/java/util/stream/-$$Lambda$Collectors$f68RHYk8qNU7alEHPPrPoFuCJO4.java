package java.util.stream;

import java.util.function.Function;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$f68RHYk8qNU7alEHPPrPoFuCJO4 implements Function {
    private final /* synthetic */ Supplier f$0;

    public /* synthetic */ -$$Lambda$Collectors$f68RHYk8qNU7alEHPPrPoFuCJO4(Supplier supplier) {
        this.f$0 = supplier;
    }

    public final Object apply(Object obj) {
        return this.f$0.get();
    }
}
