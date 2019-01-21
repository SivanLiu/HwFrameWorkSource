package java.util.stream;

import java.util.StringJoiner;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$pzPeDl3rCgtNVSeZPHZk5f2se60 implements Supplier {
    private final /* synthetic */ CharSequence f$0;
    private final /* synthetic */ CharSequence f$1;
    private final /* synthetic */ CharSequence f$2;

    public /* synthetic */ -$$Lambda$Collectors$pzPeDl3rCgtNVSeZPHZk5f2se60(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        this.f$0 = charSequence;
        this.f$1 = charSequence2;
        this.f$2 = charSequence3;
    }

    public final Object get() {
        return new StringJoiner(this.f$0, this.f$1, this.f$2);
    }
}
