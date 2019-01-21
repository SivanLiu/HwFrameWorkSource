package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Function$kjgb589uNKoZ3YFTNw1Kwl-DNBo implements Function {
    private final /* synthetic */ Function f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$Function$kjgb589uNKoZ3YFTNw1Kwl-DNBo(Function function, Function function2) {
        this.f$0 = function;
        this.f$1 = function2;
    }

    public final Object apply(Object obj) {
        return this.f$0.apply(this.f$1.apply(obj));
    }
}
