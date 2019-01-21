package android.hardware.camera2;

import android.hardware.camera2.CameraManager.TorchCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraManager$CameraManagerGlobal$6Ptxoe4wF_VCkE_pml8t66mklao implements Runnable {
    private final /* synthetic */ TorchCallback f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$CameraManager$CameraManagerGlobal$6Ptxoe4wF_VCkE_pml8t66mklao(TorchCallback torchCallback, String str) {
        this.f$0 = torchCallback;
        this.f$1 = str;
    }

    public final void run() {
        this.f$0.onTorchModeUnavailable(this.f$1);
    }
}
