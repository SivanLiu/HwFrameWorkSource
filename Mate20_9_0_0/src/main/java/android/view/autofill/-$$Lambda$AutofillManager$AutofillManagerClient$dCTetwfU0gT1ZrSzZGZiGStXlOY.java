package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$dCTetwfU0gT1ZrSzZGZiGStXlOY implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ AutofillId f$1;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$dCTetwfU0gT1ZrSzZGZiGStXlOY(AutofillManager autofillManager, AutofillId autofillId) {
        this.f$0 = autofillManager;
        this.f$1 = autofillId;
    }

    public final void run() {
        this.f$0.requestHideFillUi(this.f$1, false);
    }
}
