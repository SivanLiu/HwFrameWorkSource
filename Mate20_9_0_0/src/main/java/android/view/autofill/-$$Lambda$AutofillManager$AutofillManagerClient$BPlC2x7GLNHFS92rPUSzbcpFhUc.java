package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$BPlC2x7GLNHFS92rPUSzbcpFhUc implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ AutofillId[] f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ boolean f$4;
    private final /* synthetic */ AutofillId[] f$5;
    private final /* synthetic */ AutofillId f$6;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$BPlC2x7GLNHFS92rPUSzbcpFhUc(AutofillManager autofillManager, int i, AutofillId[] autofillIdArr, boolean z, boolean z2, AutofillId[] autofillIdArr2, AutofillId autofillId) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = autofillIdArr;
        this.f$3 = z;
        this.f$4 = z2;
        this.f$5 = autofillIdArr2;
        this.f$6 = autofillId;
    }

    public final void run() {
        this.f$0.setTrackedViews(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
    }
}
