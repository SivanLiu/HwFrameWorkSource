package java.util.stream;

import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.MatchOps.AnonymousClass2MatchSink;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MatchOps$emK14UX33I4-nqH2o5l7hLEVAy8 implements Supplier {
    private final /* synthetic */ MatchKind f$0;
    private final /* synthetic */ IntPredicate f$1;

    public /* synthetic */ -$$Lambda$MatchOps$emK14UX33I4-nqH2o5l7hLEVAy8(MatchKind matchKind, IntPredicate intPredicate) {
        this.f$0 = matchKind;
        this.f$1 = intPredicate;
    }

    public final Object get() {
        return new AnonymousClass2MatchSink(this.f$0, this.f$1);
    }
}
