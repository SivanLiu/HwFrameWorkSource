package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$HYy_ivRYb5h7sLwkHNoi3DEuZxA implements DeathRecipient {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$HYy_ivRYb5h7sLwkHNoi3DEuZxA(SupplicantStaIfaceHal supplicantStaIfaceHal) {
        this.f$0 = supplicantStaIfaceHal;
    }

    public final void serviceDied(long j) {
        SupplicantStaIfaceHal.lambda$new$0(this.f$0, j);
    }
}
