package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Function$T8wYIfMRq5hbW0Q4qNkHIIrI-BA implements Function {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Function$T8wYIfMRq5hbW0Q4qNkHIIrI-BA(Function function, Function function2) {
        this.f$0 = function;
        this.f$1 = function2;
    }

    public final Object apply(Object obj) {
        return this.f$1.apply(this.f$0.apply(obj));
    }
}
