package android.view.inputmethod;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InputMethodManager$jNoqB3BbMToNjx3pS-WwvtHoFfg implements Runnable {
    private final /* synthetic */ InputMethodManager f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$InputMethodManager$jNoqB3BbMToNjx3pS-WwvtHoFfg(InputMethodManager inputMethodManager, int i) {
        this.f$0 = inputMethodManager;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.startInputInner(this.f$1, null, 0, 0, 0);
    }
}
