package com.android.server.location;

import android.os.SystemClock;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NtpTimeHelper$k0GtK3dcbMDplmz4dv1jWzspA54 implements Runnable {
    private final /* synthetic */ NtpTimeHelper f$0;
    private final /* synthetic */ InjectTimeRecord f$1;

    public /* synthetic */ -$$Lambda$NtpTimeHelper$k0GtK3dcbMDplmz4dv1jWzspA54(NtpTimeHelper ntpTimeHelper, InjectTimeRecord injectTimeRecord) {
        this.f$0 = ntpTimeHelper;
        this.f$1 = injectTimeRecord;
    }

    public final void run() {
        this.f$0.mCallback.injectTime(this.f$1.getInjectTime(), SystemClock.elapsedRealtime(), this.f$1.getUncertainty());
    }
}
