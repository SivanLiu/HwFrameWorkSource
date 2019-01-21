package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$bLFahJqnd9gkPbDqB-OCiChzm_E implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;

    public /* synthetic */ -$$Lambda$FontsContract$bLFahJqnd9gkPbDqB-OCiChzm_E(FontRequestCallback fontRequestCallback) {
        this.f$0 = fontRequestCallback;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(-1);
    }
}
