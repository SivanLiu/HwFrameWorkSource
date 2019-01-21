package android.view.autofill;

import android.content.Intent;
import android.content.IntentSender;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$pM5e3ez5KTBdZt4d8qLEERBUSiU implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ IntentSender f$1;
    private final /* synthetic */ Intent f$2;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$pM5e3ez5KTBdZt4d8qLEERBUSiU(AutofillManager autofillManager, IntentSender intentSender, Intent intent) {
        this.f$0 = autofillManager;
        this.f$1 = intentSender;
        this.f$2 = intent;
    }

    public final void run() {
        AutofillManagerClient.lambda$startIntentSender$7(this.f$0, this.f$1, this.f$2);
    }
}
