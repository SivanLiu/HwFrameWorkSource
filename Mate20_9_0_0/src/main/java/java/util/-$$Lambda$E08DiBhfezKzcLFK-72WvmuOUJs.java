package java.util;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$E08DiBhfezKzcLFK-72WvmuOUJs implements IntConsumer {
    private final /* synthetic */ Consumer f$0;

    public /* synthetic */ -$$Lambda$E08DiBhfezKzcLFK-72WvmuOUJs(Consumer consumer) {
        this.f$0 = consumer;
    }

    public final void accept(int i) {
        this.f$0.accept(Integer.valueOf(i));
    }
}
