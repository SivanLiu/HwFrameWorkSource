package android.provider;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;
import android.provider.FontsContract.FontRequestCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FontsContract$dFs2m4XF5xdir4W3T-ncUQAVX8k implements Runnable {
    private final /* synthetic */ Context f$0;
    private final /* synthetic */ CancellationSignal f$1;
    private final /* synthetic */ FontRequest f$2;
    private final /* synthetic */ Handler f$3;
    private final /* synthetic */ FontRequestCallback f$4;

    public /* synthetic */ -$$Lambda$FontsContract$dFs2m4XF5xdir4W3T-ncUQAVX8k(Context context, CancellationSignal cancellationSignal, FontRequest fontRequest, Handler handler, FontRequestCallback fontRequestCallback) {
        this.f$0 = context;
        this.f$1 = cancellationSignal;
        this.f$2 = fontRequest;
        this.f$3 = handler;
        this.f$4 = fontRequestCallback;
    }

    public final void run() {
        FontsContract.lambda$requestFonts$12(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
