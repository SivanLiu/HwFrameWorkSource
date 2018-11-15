package com.android.server.autofill;

import com.android.internal.os.IResultReceiver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManagerServiceShellCommand$ww56nbkJspkRdVJ0yMdT4sroSiY implements Runnable {
    private final /* synthetic */ AutofillManagerServiceShellCommand f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ IResultReceiver f$2;

    public /* synthetic */ -$$Lambda$AutofillManagerServiceShellCommand$ww56nbkJspkRdVJ0yMdT4sroSiY(AutofillManagerServiceShellCommand autofillManagerServiceShellCommand, int i, IResultReceiver iResultReceiver) {
        this.f$0 = autofillManagerServiceShellCommand;
        this.f$1 = i;
        this.f$2 = iResultReceiver;
    }

    public final void run() {
        this.f$0.mService.destroySessions(this.f$1, this.f$2);
    }
}
