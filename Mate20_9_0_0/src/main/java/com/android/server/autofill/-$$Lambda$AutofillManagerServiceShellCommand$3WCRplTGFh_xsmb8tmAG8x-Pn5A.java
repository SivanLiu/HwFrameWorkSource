package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteCallback.OnResultListener;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManagerServiceShellCommand$3WCRplTGFh_xsmb8tmAG8x-Pn5A implements OnResultListener {
    private final /* synthetic */ PrintWriter f$0;
    private final /* synthetic */ CountDownLatch f$1;

    public /* synthetic */ -$$Lambda$AutofillManagerServiceShellCommand$3WCRplTGFh_xsmb8tmAG8x-Pn5A(PrintWriter printWriter, CountDownLatch countDownLatch) {
        this.f$0 = printWriter;
        this.f$1 = countDownLatch;
    }

    public final void onResult(Bundle bundle) {
        AutofillManagerServiceShellCommand.lambda$getFieldClassificationScore$0(this.f$0, this.f$1, bundle);
    }
}
