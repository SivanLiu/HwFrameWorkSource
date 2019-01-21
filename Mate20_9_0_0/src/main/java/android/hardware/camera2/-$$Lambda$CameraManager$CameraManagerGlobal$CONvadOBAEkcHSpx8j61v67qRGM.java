package android.hardware.camera2;

import android.hardware.camera2.CameraManager.TorchCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraManager$CameraManagerGlobal$CONvadOBAEkcHSpx8j61v67qRGM implements Runnable {
    private final /* synthetic */ TorchCallback f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$CameraManager$CameraManagerGlobal$CONvadOBAEkcHSpx8j61v67qRGM(TorchCallback torchCallback, String str, int i) {
        this.f$0 = torchCallback;
        this.f$1 = str;
        this.f$2 = i;
    }

    public final void run() {
        CameraManagerGlobal.lambda$postSingleTorchUpdate$0(this.f$0, this.f$1, this.f$2);
    }
}
