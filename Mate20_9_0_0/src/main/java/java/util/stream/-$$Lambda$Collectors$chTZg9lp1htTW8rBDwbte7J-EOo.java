package java.util.stream;

import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$chTZg9lp1htTW8rBDwbte7J-EOo implements Function {
    private final /* synthetic */ Collector f$0;

    public /* synthetic */ -$$Lambda$Collectors$chTZg9lp1htTW8rBDwbte7J-EOo(Collector collector) {
        this.f$0 = collector;
    }

    public final Object apply(Object obj) {
        return new Partition(this.f$0.finisher().apply(((Partition) obj).forTrue), this.f$0.finisher().apply(((Partition) obj).forFalse));
    }
}
