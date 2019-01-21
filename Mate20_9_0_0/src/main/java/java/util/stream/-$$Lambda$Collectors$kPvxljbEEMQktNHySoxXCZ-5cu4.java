package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$kPvxljbEEMQktNHySoxXCZ-5cu4 implements BiConsumer {
    private final /* synthetic */ ToDoubleFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$kPvxljbEEMQktNHySoxXCZ-5cu4(ToDoubleFunction toDoubleFunction) {
        this.f$0 = toDoubleFunction;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$averagingDouble$31(this.f$0, (double[]) obj, obj2);
    }
}
