package android.app.timezone;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RulesManager$CallbackWrapper$t7a48uTTxaRuSo3YBKxBIbPQznY implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$RulesManager$CallbackWrapper$t7a48uTTxaRuSo3YBKxBIbPQznY(CallbackWrapper callbackWrapper, int i) {
        this.f$0 = callbackWrapper;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onFinished(this.f$1);
    }
}
