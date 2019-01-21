package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$ypkr5GGxsAkGSBiLjIRwg-PzqCM implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ byte f$2;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$ypkr5GGxsAkGSBiLjIRwg-PzqCM(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, byte b) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = b;
    }

    public final void run() {
        this.f$0.mCallback.onSetProtocol(this.f$1, this.f$2);
    }
}
