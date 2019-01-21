package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$QIW-100CKwHzdHffwaus9KOEHCA implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$QIW-100CKwHzdHffwaus9KOEHCA(AutofillManager autofillManager, int i, boolean z) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = z;
    }

    public final void run() {
        this.f$0.setSaveUiState(this.f$1, this.f$2);
    }
}
