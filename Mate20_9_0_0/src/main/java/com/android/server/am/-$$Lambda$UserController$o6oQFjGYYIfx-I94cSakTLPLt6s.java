package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$o6oQFjGYYIfx-I94cSakTLPLt6s implements Runnable {
    private final /* synthetic */ UserController f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ UserState f$2;

    public /* synthetic */ -$$Lambda$UserController$o6oQFjGYYIfx-I94cSakTLPLt6s(UserController userController, int i, UserState userState) {
        this.f$0 = userController;
        this.f$1 = i;
        this.f$2 = userState;
    }

    public final void run() {
        UserController.lambda$finishUserUnlocking$0(this.f$0, this.f$1, this.f$2);
    }
}
