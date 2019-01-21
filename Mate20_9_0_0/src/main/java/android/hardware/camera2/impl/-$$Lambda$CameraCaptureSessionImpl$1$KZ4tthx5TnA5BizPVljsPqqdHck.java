package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$KZ4tthx5TnA5BizPVljsPqqdHck implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ long f$3;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$KZ4tthx5TnA5BizPVljsPqqdHck(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, int i, long j) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = i;
        this.f$3 = j;
    }

    public final void run() {
        this.f$1.onCaptureSequenceCompleted(CameraCaptureSessionImpl.this, this.f$2, this.f$3);
    }
}
