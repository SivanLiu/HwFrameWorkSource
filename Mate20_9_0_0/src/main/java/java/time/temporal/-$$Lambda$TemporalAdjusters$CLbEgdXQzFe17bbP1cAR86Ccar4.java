package java.time.temporal;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TemporalAdjusters$CLbEgdXQzFe17bbP1cAR86Ccar4 implements TemporalAdjuster {
    private final /* synthetic */ UnaryOperator f$0;

    public /* synthetic */ -$$Lambda$TemporalAdjusters$CLbEgdXQzFe17bbP1cAR86Ccar4(UnaryOperator unaryOperator) {
        this.f$0 = unaryOperator;
    }

    public final Temporal adjustInto(Temporal temporal) {
        return temporal.with((LocalDate) this.f$0.apply(LocalDate.from(temporal)));
    }
}
