package android.hardware.camera2.utils;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskDrainer$Jb53sDskEXp_qIjiikQeCRx0wJs implements Runnable {
    private final /* synthetic */ TaskDrainer f$0;

    public /* synthetic */ -$$Lambda$TaskDrainer$Jb53sDskEXp_qIjiikQeCRx0wJs(TaskDrainer taskDrainer) {
        this.f$0 = taskDrainer;
    }

    public final void run() {
        this.f$0.mListener.onDrained();
    }
}
