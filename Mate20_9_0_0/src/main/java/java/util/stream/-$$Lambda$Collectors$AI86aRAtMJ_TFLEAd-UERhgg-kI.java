package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$AI86aRAtMJ_TFLEAd-UERhgg-kI implements BiConsumer {
    private final /* synthetic */ ToLongFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$AI86aRAtMJ_TFLEAd-UERhgg-kI(ToLongFunction toLongFunction) {
        this.f$0 = toLongFunction;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$averagingLong$27(this.f$0, (long[]) obj, obj2);
    }
}
