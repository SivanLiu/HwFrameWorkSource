package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.impl.CallbackProxies.SessionStateCallbackProxy;
import android.view.Surface;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CallbackProxies$SessionStateCallbackProxy$tuajQwbKz3BV5CZZJdjl97HF6Tw implements Runnable {
    private final /* synthetic */ SessionStateCallbackProxy f$0;
    private final /* synthetic */ CameraCaptureSession f$1;
    private final /* synthetic */ Surface f$2;

    public /* synthetic */ -$$Lambda$CallbackProxies$SessionStateCallbackProxy$tuajQwbKz3BV5CZZJdjl97HF6Tw(SessionStateCallbackProxy sessionStateCallbackProxy, CameraCaptureSession cameraCaptureSession, Surface surface) {
        this.f$0 = sessionStateCallbackProxy;
        this.f$1 = cameraCaptureSession;
        this.f$2 = surface;
    }

    public final void run() {
        this.f$0.mCallback.onSurfacePrepared(this.f$1, this.f$2);
    }
}
