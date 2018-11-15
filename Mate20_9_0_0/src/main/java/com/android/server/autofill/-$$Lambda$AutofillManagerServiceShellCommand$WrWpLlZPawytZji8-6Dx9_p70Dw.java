package com.android.server.autofill;

import com.android.internal.os.IResultReceiver;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManagerServiceShellCommand$WrWpLlZPawytZji8-6Dx9_p70Dw implements Runnable {
    private final /* synthetic */ AutofillManagerServiceShellCommand f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ IResultReceiver f$2;

    public /* synthetic */ -$$Lambda$AutofillManagerServiceShellCommand$WrWpLlZPawytZji8-6Dx9_p70Dw(AutofillManagerServiceShellCommand autofillManagerServiceShellCommand, int i, IResultReceiver iResultReceiver) {
        this.f$0 = autofillManagerServiceShellCommand;
        this.f$1 = i;
        this.f$2 = iResultReceiver;
    }

    public final void run() {
        this.f$0.mService.listSessions(this.f$1, this.f$2);
    }
}
