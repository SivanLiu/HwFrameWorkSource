package com.android.server.am;

import android.os.IProgressListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$itozNmdxq9RsTKqW4_f-sH8yPdY implements Runnable {
    private final /* synthetic */ UserController f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ IProgressListener f$3;

    public /* synthetic */ -$$Lambda$UserController$itozNmdxq9RsTKqW4_f-sH8yPdY(UserController userController, int i, boolean z, IProgressListener iProgressListener) {
        this.f$0 = userController;
        this.f$1 = i;
        this.f$2 = z;
        this.f$3 = iProgressListener;
    }

    public final void run() {
        this.f$0.startUser(this.f$1, this.f$2, this.f$3);
    }
}
