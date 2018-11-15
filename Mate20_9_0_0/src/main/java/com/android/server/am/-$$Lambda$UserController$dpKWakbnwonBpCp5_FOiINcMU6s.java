package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$dpKWakbnwonBpCp5_FOiINcMU6s implements Runnable {
    private final /* synthetic */ UserController f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$UserController$dpKWakbnwonBpCp5_FOiINcMU6s(UserController userController, int i) {
        this.f$0 = userController;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mInjector.loadUserRecents(this.f$1);
    }
}
