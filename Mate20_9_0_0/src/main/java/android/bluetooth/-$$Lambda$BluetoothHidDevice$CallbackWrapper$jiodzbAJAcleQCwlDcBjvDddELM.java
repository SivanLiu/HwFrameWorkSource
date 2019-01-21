package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$jiodzbAJAcleQCwlDcBjvDddELM implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$jiodzbAJAcleQCwlDcBjvDddELM(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
    }

    public final void run() {
        this.f$0.mCallback.onVirtualCableUnplug(this.f$1);
    }
}
