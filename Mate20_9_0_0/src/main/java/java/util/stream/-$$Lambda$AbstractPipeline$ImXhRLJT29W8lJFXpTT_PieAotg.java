package java.util.stream;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AbstractPipeline$ImXhRLJT29W8lJFXpTT_PieAotg implements Supplier {
    private final /* synthetic */ AbstractPipeline f$0;

    public /* synthetic */ -$$Lambda$AbstractPipeline$ImXhRLJT29W8lJFXpTT_PieAotg(AbstractPipeline abstractPipeline) {
        this.f$0 = abstractPipeline;
    }

    public final Object get() {
        return this.f$0.sourceSpliterator(0);
    }
}
