package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$zuotCFMPpEd_pFOpcqCzvWNwmcE implements BiConsumer {
    private final /* synthetic */ ToDoubleFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$zuotCFMPpEd_pFOpcqCzvWNwmcE(ToDoubleFunction toDoubleFunction) {
        this.f$0 = toDoubleFunction;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$summingDouble$19(this.f$0, (double[]) obj, obj2);
    }
}
