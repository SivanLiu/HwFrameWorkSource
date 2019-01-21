package android.view.autofill;

import android.content.Intent;
import android.content.IntentSender;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$qyxZ4PACUgHFGSvMBHzgwjJ3yns implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ IntentSender f$3;
    private final /* synthetic */ Intent f$4;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$qyxZ4PACUgHFGSvMBHzgwjJ3yns(AutofillManager autofillManager, int i, int i2, IntentSender intentSender, Intent intent) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = intentSender;
        this.f$4 = intent;
    }

    public final void run() {
        this.f$0.authenticate(this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
