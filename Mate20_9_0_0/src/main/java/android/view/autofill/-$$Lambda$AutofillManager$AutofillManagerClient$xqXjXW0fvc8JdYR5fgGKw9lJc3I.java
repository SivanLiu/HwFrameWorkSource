package android.view.autofill;

import android.view.KeyEvent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$xqXjXW0fvc8JdYR5fgGKw9lJc3I implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ AutofillId f$2;
    private final /* synthetic */ KeyEvent f$3;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$xqXjXW0fvc8JdYR5fgGKw9lJc3I(AutofillManager autofillManager, int i, AutofillId autofillId, KeyEvent keyEvent) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = autofillId;
        this.f$3 = keyEvent;
    }

    public final void run() {
        this.f$0.dispatchUnhandledKey(this.f$1, this.f$2, this.f$3);
    }
}
