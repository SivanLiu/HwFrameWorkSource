package com.android.server.policy;

import com.android.server.policy.PhoneWindowManager.AnonymousClass14;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PhoneWindowManager$14$BoOeOHeJpJnJexDq9S0FNcstRfE implements Runnable {
    private final /* synthetic */ AnonymousClass14 f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$PhoneWindowManager$14$BoOeOHeJpJnJexDq9S0FNcstRfE(AnonymousClass14 anonymousClass14, boolean z) {
        this.f$0 = anonymousClass14;
        this.f$1 = z;
    }

    public final void run() {
        PhoneWindowManager.this.startDockOrHome(true, this.f$1);
    }
}
