package com.android.server.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NtpTimeHelper$xWqlqJuq4jBJ5-xhFLCwEKGVB0k implements Runnable {
    private final /* synthetic */ NtpTimeHelper f$0;

    public /* synthetic */ -$$Lambda$NtpTimeHelper$xWqlqJuq4jBJ5-xhFLCwEKGVB0k(NtpTimeHelper ntpTimeHelper) {
        this.f$0 = ntpTimeHelper;
    }

    public final void run() {
        this.f$0.blockingGetNtpTimeAndInject();
    }
}
