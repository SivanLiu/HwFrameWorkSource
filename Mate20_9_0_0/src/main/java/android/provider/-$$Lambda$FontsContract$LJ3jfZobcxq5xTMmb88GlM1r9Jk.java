package android.provider;

import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$LJ3jfZobcxq5xTMmb88GlM1r9Jk implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;

    public /* synthetic */ -$$Lambda$FontsContract$LJ3jfZobcxq5xTMmb88GlM1r9Jk(FontRequestCallback fontRequestCallback) {
        this.f$0 = fontRequestCallback;
    }

    public final void run() {
        this.f$0.onTypefaceRequestFailed(1);
    }
}
