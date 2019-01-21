package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$FCawscMFN_8Qxcb2EdA5gdE-O2k implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;

    public /* synthetic */ -$$Lambda$FontsContract$FCawscMFN_8Qxcb2EdA5gdE-O2k(FontRequestCallback fontRequestCallback) {
        this.f$0 = fontRequestCallback;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(-3);
    }
}
