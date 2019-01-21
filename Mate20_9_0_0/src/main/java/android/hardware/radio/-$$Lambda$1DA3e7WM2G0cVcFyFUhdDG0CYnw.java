package android.hardware.radio;

import android.hardware.radio.ProgramList.OnCompleteListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$1DA3e7WM2G0cVcFyFUhdDG0CYnw implements Runnable {
    private final /* synthetic */ OnCompleteListener f$0;

    public /* synthetic */ -$$Lambda$1DA3e7WM2G0cVcFyFUhdDG0CYnw(OnCompleteListener onCompleteListener) {
        this.f$0 = onCompleteListener;
    }

    public final void run() {
        this.f$0.onComplete();
    }
}
