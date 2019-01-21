package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$OA1Yz_YgzMO8qcV8esRjyt7ykp4 implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ CaptureRequest f$2;
    private final /* synthetic */ TotalCaptureResult f$3;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$OA1Yz_YgzMO8qcV8esRjyt7ykp4(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = captureRequest;
        this.f$3 = totalCaptureResult;
    }

    public final void run() {
        this.f$1.onCaptureCompleted(CameraCaptureSessionImpl.this, this.f$2, this.f$3);
    }
}
