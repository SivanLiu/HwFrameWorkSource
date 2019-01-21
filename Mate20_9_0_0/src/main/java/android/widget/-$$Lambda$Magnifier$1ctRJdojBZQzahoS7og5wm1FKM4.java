package android.widget;

import android.graphics.Bitmap;
import android.view.PixelCopy.OnPixelCopyFinishedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Magnifier$1ctRJdojBZQzahoS7og5wm1FKM4 implements OnPixelCopyFinishedListener {
    private final /* synthetic */ Magnifier f$0;
    private final /* synthetic */ InternalPopupWindow f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;
    private final /* synthetic */ Bitmap f$5;

    public /* synthetic */ -$$Lambda$Magnifier$1ctRJdojBZQzahoS7og5wm1FKM4(Magnifier magnifier, InternalPopupWindow internalPopupWindow, boolean z, int i, int i2, Bitmap bitmap) {
        this.f$0 = magnifier;
        this.f$1 = internalPopupWindow;
        this.f$2 = z;
        this.f$3 = i;
        this.f$4 = i2;
        this.f$5 = bitmap;
    }

    public final void onPixelCopyFinished(int i) {
        Magnifier.lambda$performPixelCopy$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, i);
    }
}
