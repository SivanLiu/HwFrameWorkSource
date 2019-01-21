package java.util.stream;

import java.util.Optional;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$bjSXRjZ5UYwAzkW-XPKwqbJ9BRQ implements Predicate {
    public static final /* synthetic */ -$$Lambda$bjSXRjZ5UYwAzkW-XPKwqbJ9BRQ INSTANCE = new -$$Lambda$bjSXRjZ5UYwAzkW-XPKwqbJ9BRQ();

    private /* synthetic */ -$$Lambda$bjSXRjZ5UYwAzkW-XPKwqbJ9BRQ() {
    }

    public final boolean test(Object obj) {
        return ((Optional) obj).isPresent();
    }
}
