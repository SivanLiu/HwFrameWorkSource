package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UnknownAppVisibilityController$FYhcjOhYWVp6HX5hr3GGaPg67Gc implements Runnable {
    private final /* synthetic */ UnknownAppVisibilityController f$0;

    public /* synthetic */ -$$Lambda$UnknownAppVisibilityController$FYhcjOhYWVp6HX5hr3GGaPg67Gc(UnknownAppVisibilityController unknownAppVisibilityController) {
        this.f$0 = unknownAppVisibilityController;
    }

    public final void run() {
        this.f$0.notifyVisibilitiesUpdated();
    }
}
