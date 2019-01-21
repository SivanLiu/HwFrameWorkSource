package android.view;

import android.view.SurfaceView.AnonymousClass3;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SurfaceView$3$XvaZSTTyv1kHN4GtX5NDdmQTRp8 implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$SurfaceView$3$XvaZSTTyv1kHN4GtX5NDdmQTRp8(AnonymousClass3 anonymousClass3, boolean z) {
        this.f$0 = anonymousClass3;
        this.f$1 = z;
    }

    public final void run() {
        SurfaceView.this.setKeepScreenOn(this.f$1);
    }
}
