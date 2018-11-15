package com.android.server.fingerprint;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$l42rkDmfSgEoarEM7da3vinr3Iw implements Runnable {
    private final /* synthetic */ FingerprintService f$0;

    public /* synthetic */ -$$Lambda$l42rkDmfSgEoarEM7da3vinr3Iw(FingerprintService fingerprintService) {
        this.f$0 = fingerprintService;
    }

    public final void run() {
        this.f$0.getFingerprintDaemon();
    }
}
