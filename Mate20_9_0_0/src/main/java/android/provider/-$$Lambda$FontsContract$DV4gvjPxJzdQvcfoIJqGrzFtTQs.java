package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$DV4gvjPxJzdQvcfoIJqGrzFtTQs implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;

    public /* synthetic */ -$$Lambda$FontsContract$DV4gvjPxJzdQvcfoIJqGrzFtTQs(FontRequestCallback fontRequestCallback) {
        this.f$0 = fontRequestCallback;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(-3);
    }
}
