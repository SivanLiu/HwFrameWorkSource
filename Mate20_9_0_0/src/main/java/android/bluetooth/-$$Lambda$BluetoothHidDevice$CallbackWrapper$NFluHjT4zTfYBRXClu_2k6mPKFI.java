package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$NFluHjT4zTfYBRXClu_2k6mPKFI implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$NFluHjT4zTfYBRXClu_2k6mPKFI(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, boolean z) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = z;
    }

    public final void run() {
        this.f$0.mCallback.onAppStatusChanged(this.f$1, this.f$2);
    }
}
