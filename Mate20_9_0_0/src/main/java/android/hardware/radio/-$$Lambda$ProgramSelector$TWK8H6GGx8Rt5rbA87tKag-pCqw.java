package android.hardware.radio;

import android.hardware.radio.ProgramSelector.Identifier;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ProgramSelector$TWK8H6GGx8Rt5rbA87tKag-pCqw implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ProgramSelector$TWK8H6GGx8Rt5rbA87tKag-pCqw(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return ProgramSelector.lambda$withSecondaryPreferred$1(this.f$0, (Identifier) obj);
    }
}
