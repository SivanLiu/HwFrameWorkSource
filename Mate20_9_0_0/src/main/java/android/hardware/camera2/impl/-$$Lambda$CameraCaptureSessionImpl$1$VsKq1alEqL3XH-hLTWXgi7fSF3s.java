package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$VsKq1alEqL3XH-hLTWXgi7fSF3s implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ CaptureRequest f$2;
    private final /* synthetic */ CaptureFailure f$3;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$VsKq1alEqL3XH-hLTWXgi7fSF3s(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, CaptureRequest captureRequest, CaptureFailure captureFailure) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = captureRequest;
        this.f$3 = captureFailure;
    }

    public final void run() {
        this.f$1.onCaptureFailed(CameraCaptureSessionImpl.this, this.f$2, this.f$3);
    }
}
