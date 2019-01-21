package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$qtStwQVkGfOs2iJIiePWqJJpi0w implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$qtStwQVkGfOs2iJIiePWqJJpi0w(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, int i) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.mCallback.onConnectionStateChanged(this.f$1, this.f$2);
    }
}
