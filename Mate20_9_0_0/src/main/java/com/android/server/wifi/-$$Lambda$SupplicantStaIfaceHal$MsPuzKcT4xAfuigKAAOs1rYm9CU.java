package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantStaIfaceHal$MsPuzKcT4xAfuigKAAOs1rYm9CU implements DeathRecipient {
    private final /* synthetic */ SupplicantStaIfaceHal f$0;

    public /* synthetic */ -$$Lambda$SupplicantStaIfaceHal$MsPuzKcT4xAfuigKAAOs1rYm9CU(SupplicantStaIfaceHal supplicantStaIfaceHal) {
        this.f$0 = supplicantStaIfaceHal;
    }

    public final void serviceDied(long j) {
        SupplicantStaIfaceHal.lambda$new$1(this.f$0, j);
    }
}
