package com.android.server.wifi;

import android.os.Handler.Callback;
import android.os.Message;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AvailableNetworkNotifier$uFi1H-bLBjC8591OGivQMgKmiaU implements Callback {
    private final /* synthetic */ AvailableNetworkNotifier f$0;

    public /* synthetic */ -$$Lambda$AvailableNetworkNotifier$uFi1H-bLBjC8591OGivQMgKmiaU(AvailableNetworkNotifier availableNetworkNotifier) {
        this.f$0 = availableNetworkNotifier;
    }

    public final boolean handleMessage(Message message) {
        return AvailableNetworkNotifier.lambda$new$0(this.f$0, message);
    }
}
