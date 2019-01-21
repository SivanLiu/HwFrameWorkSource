package java.util.stream;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.DistinctOps.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DistinctOps$1$y8chmSlaKpIKb_VPPBaXdCNT51c implements Consumer {
    private final /* synthetic */ AtomicBoolean f$0;
    private final /* synthetic */ ConcurrentHashMap f$1;

    public /* synthetic */ -$$Lambda$DistinctOps$1$y8chmSlaKpIKb_VPPBaXdCNT51c(AtomicBoolean atomicBoolean, ConcurrentHashMap concurrentHashMap) {
        this.f$0 = atomicBoolean;
        this.f$1 = concurrentHashMap;
    }

    public final void accept(Object obj) {
        AnonymousClass1.lambda$opEvaluateParallel$0(this.f$0, this.f$1, obj);
    }
}
