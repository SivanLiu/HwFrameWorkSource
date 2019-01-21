package com.android.internal.telephony.ims;

import android.os.Handler.Callback;
import android.os.Message;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsResolver$pNx4XUM9FmR6cV_MCAGiEt8F4pg implements Callback {
    private final /* synthetic */ ImsResolver f$0;

    public /* synthetic */ -$$Lambda$ImsResolver$pNx4XUM9FmR6cV_MCAGiEt8F4pg(ImsResolver imsResolver) {
        this.f$0 = imsResolver;
    }

    public final boolean handleMessage(Message message) {
        return ImsResolver.lambda$new$0(this.f$0, message);
    }
}
