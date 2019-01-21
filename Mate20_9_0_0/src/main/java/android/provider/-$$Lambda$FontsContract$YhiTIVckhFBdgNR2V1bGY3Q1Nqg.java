package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$YhiTIVckhFBdgNR2V1bGY3Q1Nqg implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;

    public /* synthetic */ -$$Lambda$FontsContract$YhiTIVckhFBdgNR2V1bGY3Q1Nqg(FontRequestCallback fontRequestCallback) {
        this.f$0 = fontRequestCallback;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(-2);
    }
}
