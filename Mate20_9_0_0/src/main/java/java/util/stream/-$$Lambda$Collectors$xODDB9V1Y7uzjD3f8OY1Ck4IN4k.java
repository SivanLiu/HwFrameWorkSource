package java.util.stream;

import java.util.Map;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$xODDB9V1Y7uzjD3f8OY1Ck4IN4k implements Function {
    private final /* synthetic */ Function f$0;

    public /* synthetic */ -$$Lambda$Collectors$xODDB9V1Y7uzjD3f8OY1Ck4IN4k(Function function) {
        this.f$0 = function;
    }

    public final Object apply(Object obj) {
        return ((Map) obj).replaceAll(new -$$Lambda$Collectors$hNSw8Kk0nIafeklCUz0r3g25T08(this.f$0));
    }
}
