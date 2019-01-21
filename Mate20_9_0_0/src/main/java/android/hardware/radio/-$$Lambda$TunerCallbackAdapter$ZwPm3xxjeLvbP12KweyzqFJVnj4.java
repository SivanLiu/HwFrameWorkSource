package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$ZwPm3xxjeLvbP12KweyzqFJVnj4 implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$ZwPm3xxjeLvbP12KweyzqFJVnj4(TunerCallbackAdapter tunerCallbackAdapter, boolean z) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.mCallback.onEmergencyAnnouncement(this.f$1);
    }
}
