package com.android.server.devicepolicy;

import android.os.UserHandle;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CertificateMonitor$nzwzuvk_fK7AIlili6jDKrKWLJM implements Runnable {
    private final /* synthetic */ CertificateMonitor f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$CertificateMonitor$nzwzuvk_fK7AIlili6jDKrKWLJM(CertificateMonitor certificateMonitor, int i) {
        this.f$0 = certificateMonitor;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.updateInstalledCertificates(UserHandle.of(this.f$1));
    }
}
