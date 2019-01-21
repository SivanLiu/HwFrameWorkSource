package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$Eyz_qG6mvTlh6a8Bp41ZoEJzQCQ implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ byte f$2;
    private final /* synthetic */ byte f$3;
    private final /* synthetic */ int f$4;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$Eyz_qG6mvTlh6a8Bp41ZoEJzQCQ(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, byte b, byte b2, int i) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = b;
        this.f$3 = b2;
        this.f$4 = i;
    }

    public final void run() {
        this.f$0.mCallback.onGetReport(this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
