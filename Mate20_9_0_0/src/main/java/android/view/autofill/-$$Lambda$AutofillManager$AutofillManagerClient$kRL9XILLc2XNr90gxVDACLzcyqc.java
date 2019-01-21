package android.view.autofill;

import android.graphics.Rect;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$kRL9XILLc2XNr90gxVDACLzcyqc implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ AutofillId f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;
    private final /* synthetic */ Rect f$5;
    private final /* synthetic */ IAutofillWindowPresenter f$6;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$kRL9XILLc2XNr90gxVDACLzcyqc(AutofillManager autofillManager, int i, AutofillId autofillId, int i2, int i3, Rect rect, IAutofillWindowPresenter iAutofillWindowPresenter) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = autofillId;
        this.f$3 = i2;
        this.f$4 = i3;
        this.f$5 = rect;
        this.f$6 = iAutofillWindowPresenter;
    }

    public final void run() {
        this.f$0.requestShowFillUi(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
    }
}
