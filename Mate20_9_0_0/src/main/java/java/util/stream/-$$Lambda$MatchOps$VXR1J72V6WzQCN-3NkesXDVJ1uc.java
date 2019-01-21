package java.util.stream;

import java.util.function.DoublePredicate;
import java.util.function.Supplier;
import java.util.stream.MatchOps.AnonymousClass4MatchSink;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MatchOps$VXR1J72V6WzQCN-3NkesXDVJ1uc implements Supplier {
    private final /* synthetic */ MatchKind f$0;
    private final /* synthetic */ DoublePredicate f$1;

    public /* synthetic */ -$$Lambda$MatchOps$VXR1J72V6WzQCN-3NkesXDVJ1uc(MatchKind matchKind, DoublePredicate doublePredicate) {
        this.f$0 = matchKind;
        this.f$1 = doublePredicate;
    }

    public final Object get() {
        return new AnonymousClass4MatchSink(this.f$0, this.f$1);
    }
}
