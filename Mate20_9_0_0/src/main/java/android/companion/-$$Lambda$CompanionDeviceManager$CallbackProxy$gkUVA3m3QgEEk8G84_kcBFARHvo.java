package android.companion;

import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManager$CallbackProxy$gkUVA3m3QgEEk8G84_kcBFARHvo implements Runnable {
    private final /* synthetic */ CallbackProxy f$0;
    private final /* synthetic */ BiConsumer f$1;
    private final /* synthetic */ Object f$2;

    public /* synthetic */ -$$Lambda$CompanionDeviceManager$CallbackProxy$gkUVA3m3QgEEk8G84_kcBFARHvo(CallbackProxy callbackProxy, BiConsumer biConsumer, Object obj) {
        this.f$0 = callbackProxy;
        this.f$1 = biConsumer;
        this.f$2 = obj;
    }

    public final void run() {
        CallbackProxy.lambda$lockAndPost$0(this.f$0, this.f$1, this.f$2);
    }
}
