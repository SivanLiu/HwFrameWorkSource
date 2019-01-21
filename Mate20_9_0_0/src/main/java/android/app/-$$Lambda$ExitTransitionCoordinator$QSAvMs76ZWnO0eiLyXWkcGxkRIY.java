package android.app;

import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ExitTransitionCoordinator$QSAvMs76ZWnO0eiLyXWkcGxkRIY implements Runnable {
    private final /* synthetic */ ExitTransitionCoordinator f$0;
    private final /* synthetic */ ArrayList f$1;

    public /* synthetic */ -$$Lambda$ExitTransitionCoordinator$QSAvMs76ZWnO0eiLyXWkcGxkRIY(ExitTransitionCoordinator exitTransitionCoordinator, ArrayList arrayList) {
        this.f$0 = exitTransitionCoordinator;
        this.f$1 = arrayList;
    }

    public final void run() {
        this.f$0.setSharedElementState(this.f$0.mExitSharedElementBundle, this.f$1);
    }
}
