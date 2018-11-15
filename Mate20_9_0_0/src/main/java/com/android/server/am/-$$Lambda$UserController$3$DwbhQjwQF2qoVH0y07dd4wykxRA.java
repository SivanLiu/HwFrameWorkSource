package com.android.server.am;

import com.android.server.am.UserController.AnonymousClass3;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$3$DwbhQjwQF2qoVH0y07dd4wykxRA implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$UserController$3$DwbhQjwQF2qoVH0y07dd4wykxRA(AnonymousClass3 anonymousClass3, int i, boolean z) {
        this.f$0 = anonymousClass3;
        this.f$1 = i;
        this.f$2 = z;
    }

    public final void run() {
        UserController.this.startUser(this.f$1, this.f$2);
    }
}
