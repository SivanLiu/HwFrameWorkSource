package android.provider;

import android.graphics.Typeface;
import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$xDMhIK5JxjXFDIXBeQbZ_hdXTBc implements Runnable {
    private final /* synthetic */ FontRequestCallback f$0;
    private final /* synthetic */ Typeface f$1;

    public /* synthetic */ -$$Lambda$FontsContract$xDMhIK5JxjXFDIXBeQbZ_hdXTBc(FontRequestCallback fontRequestCallback, Typeface typeface) {
        this.f$0 = fontRequestCallback;
        this.f$1 = typeface;
    }

    public final void run() {
        this.f$0.onTypefaceRetrieved(this.f$1);
    }
}
