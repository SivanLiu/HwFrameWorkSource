package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.impl.CallbackProxies.SessionStateCallbackProxy;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CallbackProxies$SessionStateCallbackProxy$gvbTsp9UPpKJAbdycdci_ZW5BeI implements Runnable {
    private final /* synthetic */ SessionStateCallbackProxy f$0;
    private final /* synthetic */ CameraCaptureSession f$1;

    public /* synthetic */ -$$Lambda$CallbackProxies$SessionStateCallbackProxy$gvbTsp9UPpKJAbdycdci_ZW5BeI(SessionStateCallbackProxy sessionStateCallbackProxy, CameraCaptureSession cameraCaptureSession) {
        this.f$0 = sessionStateCallbackProxy;
        this.f$1 = cameraCaptureSession;
    }

    public final void run() {
        this.f$0.mCallback.onConfigureFailed(this.f$1);
    }
}
