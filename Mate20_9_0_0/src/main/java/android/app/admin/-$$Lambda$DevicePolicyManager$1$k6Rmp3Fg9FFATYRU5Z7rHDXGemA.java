package android.app.admin;

import android.app.admin.DevicePolicyManager.OnClearApplicationUserDataListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DevicePolicyManager$1$k6Rmp3Fg9FFATYRU5Z7rHDXGemA implements Runnable {
    private final /* synthetic */ OnClearApplicationUserDataListener f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$DevicePolicyManager$1$k6Rmp3Fg9FFATYRU5Z7rHDXGemA(OnClearApplicationUserDataListener onClearApplicationUserDataListener, String str, boolean z) {
        this.f$0 = onClearApplicationUserDataListener;
        this.f$1 = str;
        this.f$2 = z;
    }

    public final void run() {
        this.f$0.onApplicationUserDataCleared(this.f$1, this.f$2);
    }
}
