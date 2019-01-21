package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$xW99-tc95OmGApoKnpQ9q1TXb9k implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ byte f$2;
    private final /* synthetic */ byte[] f$3;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$xW99-tc95OmGApoKnpQ9q1TXb9k(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, byte b, byte[] bArr) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = b;
        this.f$3 = bArr;
    }

    public final void run() {
        this.f$0.mCallback.onInterruptData(this.f$1, this.f$2, this.f$3);
    }
}
