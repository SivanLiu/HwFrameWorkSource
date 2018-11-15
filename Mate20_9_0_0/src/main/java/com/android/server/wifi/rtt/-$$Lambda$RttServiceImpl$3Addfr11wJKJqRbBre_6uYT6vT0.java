package com.android.server.wifi.rtt;

import android.net.wifi.rtt.IRttCallback;
import android.net.wifi.rtt.RangingRequest;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.WorkSource;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0 implements Runnable {
    private final /* synthetic */ RttServiceImpl f$0;
    private final /* synthetic */ WorkSource f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ IBinder f$3;
    private final /* synthetic */ DeathRecipient f$4;
    private final /* synthetic */ String f$5;
    private final /* synthetic */ RangingRequest f$6;
    private final /* synthetic */ IRttCallback f$7;
    private final /* synthetic */ boolean f$8;

    public /* synthetic */ -$$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0(RttServiceImpl rttServiceImpl, WorkSource workSource, int i, IBinder iBinder, DeathRecipient deathRecipient, String str, RangingRequest rangingRequest, IRttCallback iRttCallback, boolean z) {
        this.f$0 = rttServiceImpl;
        this.f$1 = workSource;
        this.f$2 = i;
        this.f$3 = iBinder;
        this.f$4 = deathRecipient;
        this.f$5 = str;
        this.f$6 = rangingRequest;
        this.f$7 = iRttCallback;
        this.f$8 = z;
    }

    public final void run() {
        RttServiceImpl.lambda$startRanging$3(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8);
    }
}
