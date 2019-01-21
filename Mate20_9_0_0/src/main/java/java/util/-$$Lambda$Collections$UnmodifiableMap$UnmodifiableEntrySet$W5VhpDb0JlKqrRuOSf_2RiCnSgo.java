package java.util;

import java.util.Map.Entry;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collections$UnmodifiableMap$UnmodifiableEntrySet$W5VhpDb0JlKqrRuOSf_2RiCnSgo implements Consumer {
    private final /* synthetic */ Consumer f$0;

    public /* synthetic */ -$$Lambda$Collections$UnmodifiableMap$UnmodifiableEntrySet$W5VhpDb0JlKqrRuOSf_2RiCnSgo(Consumer consumer) {
        this.f$0 = consumer;
    }

    public final void accept(Object obj) {
        this.f$0.accept(new UnmodifiableEntry((Entry) obj));
    }
}
