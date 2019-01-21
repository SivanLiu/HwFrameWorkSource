package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$5sBFPk12YoFTd83smSoPj46DB_A implements BiConsumer {
    private final /* synthetic */ ToIntFunction f$0;

    public /* synthetic */ -$$Lambda$Collectors$5sBFPk12YoFTd83smSoPj46DB_A(ToIntFunction toIntFunction) {
        this.f$0 = toIntFunction;
    }

    public final void accept(Object obj, Object obj2) {
        Collectors.lambda$averagingInt$23(this.f$0, (long[]) obj, obj2);
    }
}
