package java.util.function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Predicate$17UUIF1CH_K9duk0ChtjSwOycuM implements Predicate {
    private final /* synthetic */ Predicate f$0;
    private final /* synthetic */ Predicate f$1;

    public /* synthetic */ -$$Lambda$Predicate$17UUIF1CH_K9duk0ChtjSwOycuM(Predicate predicate, Predicate predicate2) {
        this.f$0 = predicate;
        this.f$1 = predicate2;
    }

    public final boolean test(Object obj) {
        return Predicate.lambda$or$2(this.f$0, this.f$1, obj);
    }
}
