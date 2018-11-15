package com.android.server.connectivity;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Nat464Xlat$40jKHQd7R0zgcegyEyc9zPHKXVA implements Runnable {
    private final /* synthetic */ Nat464Xlat f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$Nat464Xlat$40jKHQd7R0zgcegyEyc9zPHKXVA(Nat464Xlat nat464Xlat, String str, boolean z) {
        this.f$0 = nat464Xlat;
        this.f$1 = str;
        this.f$2 = z;
    }

    public final void run() {
        this.f$0.handleInterfaceLinkStateChanged(this.f$1, this.f$2);
    }
}
