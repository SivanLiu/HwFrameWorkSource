package android.app;

import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityTransitionCoordinator$fkaPvc8GCghP2GMwEgS_J5m_T_4 implements Runnable {
    private final /* synthetic */ ActivityTransitionCoordinator f$0;
    private final /* synthetic */ ArrayList f$1;

    public /* synthetic */ -$$Lambda$ActivityTransitionCoordinator$fkaPvc8GCghP2GMwEgS_J5m_T_4(ActivityTransitionCoordinator activityTransitionCoordinator, ArrayList arrayList) {
        this.f$0 = activityTransitionCoordinator;
        this.f$1 = arrayList;
    }

    public final void run() {
        this.f$0.notifySharedElementEnd(this.f$1);
    }
}
