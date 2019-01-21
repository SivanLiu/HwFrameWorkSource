package com.android.ims;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsManager$jpd4qPKM1aFT_1D7HiZpiM__ddY implements Runnable {
    private final /* synthetic */ ImsManager f$0;
    private final /* synthetic */ boolean f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$ImsManager$jpd4qPKM1aFT_1D7HiZpiM__ddY(ImsManager imsManager, boolean z, int i) {
        this.f$0 = imsManager;
        this.f$1 = z;
        this.f$2 = i;
    }

    public final void run() {
        ImsManager.lambda$setRttEnabled$2(this.f$0, this.f$1, this.f$2);
    }
}
