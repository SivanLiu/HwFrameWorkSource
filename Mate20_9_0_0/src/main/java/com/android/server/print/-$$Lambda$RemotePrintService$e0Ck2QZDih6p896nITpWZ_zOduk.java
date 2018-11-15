package com.android.server.print;

import android.print.PrinterId;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$e0Ck2QZDih6p896nITpWZ_zOduk implements Runnable {
    private final /* synthetic */ RemotePrintService f$0;
    private final /* synthetic */ PrinterId f$1;

    public /* synthetic */ -$$Lambda$RemotePrintService$e0Ck2QZDih6p896nITpWZ_zOduk(RemotePrintService remotePrintService, PrinterId printerId) {
        this.f$0 = remotePrintService;
        this.f$1 = printerId;
    }

    public final void run() {
        this.f$0.handleRequestCustomPrinterIcon(this.f$1);
    }
}
