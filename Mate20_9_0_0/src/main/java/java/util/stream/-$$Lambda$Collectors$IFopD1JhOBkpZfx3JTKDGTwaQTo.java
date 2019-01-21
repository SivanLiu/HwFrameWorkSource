package java.util.stream;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$IFopD1JhOBkpZfx3JTKDGTwaQTo implements Supplier {
    private final /* synthetic */ Collector f$0;

    public /* synthetic */ -$$Lambda$Collectors$IFopD1JhOBkpZfx3JTKDGTwaQTo(Collector collector) {
        this.f$0 = collector;
    }

    public final Object get() {
        return new Partition(this.f$0.supplier().get(), this.f$0.supplier().get());
    }
}
