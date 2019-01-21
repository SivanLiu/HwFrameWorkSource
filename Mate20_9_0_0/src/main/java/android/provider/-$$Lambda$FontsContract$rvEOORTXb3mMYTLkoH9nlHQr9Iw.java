package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$rvEOORTXb3mMYTLkoH9nlHQr9Iw implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$FontsContract$rvEOORTXb3mMYTLkoH9nlHQr9Iw(FontRequestCallback fontRequestCallback, int i) {
        this.f$0 = fontRequestCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(this.f$1);
    }
}
