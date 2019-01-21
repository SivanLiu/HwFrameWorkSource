package java.util.stream;

import java.util.function.IntFunction;
import java.util.function.LongFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Nodes$CollectorTask$OfRef$Zd2fdoB-mZW0DbPHybIpYjf-Pyo implements LongFunction {
    private final /* synthetic */ IntFunction f$0;

    public /* synthetic */ -$$Lambda$Nodes$CollectorTask$OfRef$Zd2fdoB-mZW0DbPHybIpYjf-Pyo(IntFunction intFunction) {
        this.f$0 = intFunction;
    }

    public final Object apply(long j) {
        return Nodes.builder(j, this.f$0);
    }
}
