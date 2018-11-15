package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$d0zeElfogOIugnQQLWhCzumk53k implements Runnable {
    private final /* synthetic */ UserController f$0;
    private final /* synthetic */ UserState f$1;

    public /* synthetic */ -$$Lambda$UserController$d0zeElfogOIugnQQLWhCzumk53k(UserController userController, UserState userState) {
        this.f$0 = userController;
        this.f$1 = userState;
    }

    public final void run() {
        this.f$0.finishUserUnlockedCompleted(this.f$1);
    }
}
