package android.app;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityTransitionCoordinator$_HMo0E-15AzCK9fwQ8WHzdz8ZIw implements Runnable {
    private final /* synthetic */ ActivityTransitionCoordinator f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ActivityTransitionCoordinator$_HMo0E-15AzCK9fwQ8WHzdz8ZIw(ActivityTransitionCoordinator activityTransitionCoordinator, int i) {
        this.f$0 = activityTransitionCoordinator;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.setGhostVisibility(this.f$1);
    }
}
