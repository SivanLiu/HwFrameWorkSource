package com.android.server.am;

import com.android.server.am.UserController.AnonymousClass4;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$4$P3Sj7pxBXLC7k_puCIIki2uVgGE implements Runnable {
    private final /* synthetic */ AnonymousClass4 f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ UserState f$2;

    public /* synthetic */ -$$Lambda$UserController$4$P3Sj7pxBXLC7k_puCIIki2uVgGE(AnonymousClass4 anonymousClass4, int i, UserState userState) {
        this.f$0 = anonymousClass4;
        this.f$1 = i;
        this.f$2 = userState;
    }

    public final void run() {
        UserController.this.finishUserStopping(this.f$1, this.f$2);
    }
}
