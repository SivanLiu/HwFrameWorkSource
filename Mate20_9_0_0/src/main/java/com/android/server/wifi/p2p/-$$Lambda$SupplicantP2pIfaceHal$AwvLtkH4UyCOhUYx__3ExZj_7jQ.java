package com.android.server.wifi.p2p;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SupplicantP2pIfaceHal$AwvLtkH4UyCOhUYx__3ExZj_7jQ implements DeathRecipient {
    private final /* synthetic */ SupplicantP2pIfaceHal f$0;

    public /* synthetic */ -$$Lambda$SupplicantP2pIfaceHal$AwvLtkH4UyCOhUYx__3ExZj_7jQ(SupplicantP2pIfaceHal supplicantP2pIfaceHal) {
        this.f$0 = supplicantP2pIfaceHal;
    }

    public final void serviceDied(long j) {
        SupplicantP2pIfaceHal.lambda$new$1(this.f$0, j);
    }
}
