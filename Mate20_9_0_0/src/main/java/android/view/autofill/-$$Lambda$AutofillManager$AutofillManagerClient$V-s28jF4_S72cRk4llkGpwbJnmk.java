package android.view.autofill;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManager$AutofillManagerClient$V-s28jF4_S72cRk4llkGpwbJnmk implements Runnable {
    private final /* synthetic */ AutofillManager f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$AutofillManager$AutofillManagerClient$V-s28jF4_S72cRk4llkGpwbJnmk(AutofillManager autofillManager, int i) {
        this.f$0 = autofillManager;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.setSessionFinished(this.f$1);
    }
}
