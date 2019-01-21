package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$K79QnIPRaZuikYDQdsLcIUBhqiI implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ AutofillId f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$K79QnIPRaZuikYDQdsLcIUBhqiI(AutofillManager autofillManager, int i, AutofillId autofillId, int i2) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = autofillId;
        this.f$3 = i2;
    }

    public final void run() {
        this.f$0.notifyNoFillUi(this.f$1, this.f$2, this.f$3);
    }
}
