package android.view;

import android.graphics.Bitmap;
import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ViewDebug$SYbJuwHeGrHQLha0YsHp4VI9JLg implements Runnable {
    private final /* synthetic */ View f$0;
    private final /* synthetic */ Bitmap[] f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ CountDownLatch f$3;

    public /* synthetic */ -$$Lambda$ViewDebug$SYbJuwHeGrHQLha0YsHp4VI9JLg(View view, Bitmap[] bitmapArr, boolean z, CountDownLatch countDownLatch) {
        this.f$0 = view;
        this.f$1 = bitmapArr;
        this.f$2 = z;
        this.f$3 = countDownLatch;
    }

    public final void run() {
        ViewDebug.lambda$performViewCapture$4(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
