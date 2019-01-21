package java.util.stream;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$oKi5061mJjDn56eRJcmESyO7x9k implements Function {
    private final /* synthetic */ Function f$0;

    public /* synthetic */ -$$Lambda$Collectors$oKi5061mJjDn56eRJcmESyO7x9k(Function function) {
        this.f$0 = function;
    }

    public final Object apply(Object obj) {
        return ((ConcurrentMap) obj).replaceAll(new -$$Lambda$Collectors$h1ksXokknmXSWBYxKkYfY6ov7ME(this.f$0));
    }
}
