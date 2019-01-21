package java.util.regex;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Pattern$2enjXc1GhR-FNtcTfhbg9qy2smk implements Predicate {
    private final /* synthetic */ Pattern f$0;

    public /* synthetic */ -$$Lambda$Pattern$2enjXc1GhR-FNtcTfhbg9qy2smk(Pattern pattern) {
        this.f$0 = pattern;
    }

    public final boolean test(Object obj) {
        return this.f$0.matcher((String) obj).find();
    }
}
