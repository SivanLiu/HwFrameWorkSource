package java.util.stream;

import java.util.function.LongPredicate;
import java.util.function.Supplier;
import java.util.stream.MatchOps.AnonymousClass3MatchSink;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MatchOps$kCrOdGmndcbZklBaJ6Z4blQ1F5M implements Supplier {
    private final /* synthetic */ MatchKind f$0;
    private final /* synthetic */ LongPredicate f$1;

    public /* synthetic */ -$$Lambda$MatchOps$kCrOdGmndcbZklBaJ6Z4blQ1F5M(MatchKind matchKind, LongPredicate longPredicate) {
        this.f$0 = matchKind;
        this.f$1 = longPredicate;
    }

    public final Object get() {
        return new AnonymousClass3MatchSink(this.f$0, this.f$1);
    }
}
