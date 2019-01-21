package android.net.lowpan;

import android.net.lowpan.LowpanInterface.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanInterface$1$9yiRqHwJmFc-LEKn1vk5rA75W0M implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$LowpanInterface$1$9yiRqHwJmFc-LEKn1vk5rA75W0M(Callback callback, String str) {
        this.f$0 = callback;
        this.f$1 = str;
    }

    public final void run() {
        this.f$0.onRoleChanged(this.f$1);
    }
}
