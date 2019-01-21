package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;
import android.view.Surface;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$VuYVXvwmJMkbTnKaOD-h-DOjJpE implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ CaptureRequest f$2;
    private final /* synthetic */ Surface f$3;
    private final /* synthetic */ long f$4;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$VuYVXvwmJMkbTnKaOD-h-DOjJpE(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, CaptureRequest captureRequest, Surface surface, long j) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = captureRequest;
        this.f$3 = surface;
        this.f$4 = j;
    }

    public final void run() {
        this.f$1.onCaptureBufferLost(CameraCaptureSessionImpl.this, this.f$2, this.f$3, this.f$4);
    }
}
