package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$7mSdNTTAoYA0D3ITDxzDJKGykz0 implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ CaptureRequest f$2;
    private final /* synthetic */ CaptureResult f$3;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$7mSdNTTAoYA0D3ITDxzDJKGykz0(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, CaptureRequest captureRequest, CaptureResult captureResult) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = captureRequest;
        this.f$3 = captureResult;
    }

    public final void run() {
        this.f$1.onCaptureProgressed(CameraCaptureSessionImpl.this, this.f$2, this.f$3);
    }
}
