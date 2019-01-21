package java.util;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$9llQTmDvC2fDr-Gds5d6BexJH00 implements LongConsumer {
    private final /* synthetic */ Consumer f$0;

    public /* synthetic */ -$$Lambda$9llQTmDvC2fDr-Gds5d6BexJH00(Consumer consumer) {
        this.f$0 = consumer;
    }

    public final void accept(long j) {
        this.f$0.accept(Long.valueOf(j));
    }
}
