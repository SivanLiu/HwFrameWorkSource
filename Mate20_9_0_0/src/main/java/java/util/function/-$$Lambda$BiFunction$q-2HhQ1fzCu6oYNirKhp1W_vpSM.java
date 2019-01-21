package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiFunction$q-2HhQ1fzCu6oYNirKhp1W_vpSM implements BiFunction {
    private final /* synthetic */ BiFunction f$0;
    private final /* synthetic */ Function f$1;

    public /* synthetic */ -$$Lambda$BiFunction$q-2HhQ1fzCu6oYNirKhp1W_vpSM(BiFunction biFunction, Function function) {
        this.f$0 = biFunction;
        this.f$1 = function;
    }

    public final Object apply(Object obj, Object obj2) {
        return this.f$1.apply(this.f$0.apply(obj, obj2));
    }
}
