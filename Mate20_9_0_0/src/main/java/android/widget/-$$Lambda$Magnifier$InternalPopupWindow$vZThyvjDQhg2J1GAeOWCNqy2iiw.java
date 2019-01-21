package android.widget;

import android.view.ThreadedRenderer.FrameDrawingCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Magnifier$InternalPopupWindow$vZThyvjDQhg2J1GAeOWCNqy2iiw implements FrameDrawingCallback {
    private final /* synthetic */ InternalPopupWindow f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ boolean f$4;

    public /* synthetic */ -$$Lambda$Magnifier$InternalPopupWindow$vZThyvjDQhg2J1GAeOWCNqy2iiw(InternalPopupWindow internalPopupWindow, int i, int i2, boolean z, boolean z2) {
        this.f$0 = internalPopupWindow;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = z;
        this.f$4 = z2;
    }

    public final void onFrameDraw(long j) {
        InternalPopupWindow.lambda$doDraw$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, j);
    }
}
