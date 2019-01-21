package java.util.stream;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.MatchOps.AnonymousClass1MatchSink;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MatchOps$_LtFSpSMfVwoPv-8p_1cMGGcaHA implements Supplier {
    private final /* synthetic */ MatchKind f$0;
    private final /* synthetic */ Predicate f$1;

    public /* synthetic */ -$$Lambda$MatchOps$_LtFSpSMfVwoPv-8p_1cMGGcaHA(MatchKind matchKind, Predicate predicate) {
        this.f$0 = matchKind;
        this.f$1 = predicate;
    }

    public final Object get() {
        return new AnonymousClass1MatchSink(this.f$0, this.f$1);
    }
}
