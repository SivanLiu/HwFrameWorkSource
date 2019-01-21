package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$V76JiQu509LCUz3-ckpb-nB3JhA implements Runnable {
    private final /* synthetic */ IAutoFillManager f$0;
    private final /* synthetic */ IAutoFillManagerClient f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$AutofillManager$V76JiQu509LCUz3-ckpb-nB3JhA(IAutoFillManager iAutoFillManager, IAutoFillManagerClient iAutoFillManagerClient, int i) {
        this.f$0 = iAutoFillManager;
        this.f$1 = iAutoFillManagerClient;
        this.f$2 = i;
    }

    public final void run() {
        AutofillManager.lambda$ensureServiceClientAddedIfNeededLocked$1(this.f$0, this.f$1, this.f$2);
    }
}
