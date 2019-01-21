package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.impl.CameraCaptureSessionImpl.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraCaptureSessionImpl$1$TIJELOXvjSbPh6mpBLfBJ5ciNic implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ CaptureCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$CameraCaptureSessionImpl$1$TIJELOXvjSbPh6mpBLfBJ5ciNic(AnonymousClass1 anonymousClass1, CaptureCallback captureCallback, int i) {
        this.f$0 = anonymousClass1;
        this.f$1 = captureCallback;
        this.f$2 = i;
    }

    public final void run() {
        this.f$1.onCaptureSequenceAborted(CameraCaptureSessionImpl.this, this.f$2);
    }
}
