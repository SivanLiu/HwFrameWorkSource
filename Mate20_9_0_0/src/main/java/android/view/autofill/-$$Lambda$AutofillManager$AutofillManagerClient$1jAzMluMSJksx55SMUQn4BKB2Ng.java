package android.view.autofill;

import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$1jAzMluMSJksx55SMUQn4BKB2Ng implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;
    private final /* synthetic */ List f$3;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$1jAzMluMSJksx55SMUQn4BKB2Ng(AutofillManager autofillManager, int i, List list, List list2) {
        this.f$0 = autofillManager;
        this.f$1 = i;
        this.f$2 = list;
        this.f$3 = list2;
    }

    public final void run() {
        this.f$0.autofill(this.f$1, this.f$2, this.f$3);
    }
}
