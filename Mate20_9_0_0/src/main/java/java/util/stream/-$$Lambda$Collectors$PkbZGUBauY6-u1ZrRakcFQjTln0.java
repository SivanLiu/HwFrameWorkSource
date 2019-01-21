package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$PkbZGUBauY6-u1ZrRakcFQjTln0 implements BiConsumer {
    private final /* synthetic */ ToIntFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$PkbZGUBauY6-u1ZrRakcFQjTln0(ToIntFunction toIntFunction) {
        this.f$0 = toIntFunction;
    }

    public final void accept(Object obj, Object obj2) {
        ((int[]) obj)[0] = ((int[]) obj)[0] + this.f$0.applyAsInt(obj2);
    }
}
