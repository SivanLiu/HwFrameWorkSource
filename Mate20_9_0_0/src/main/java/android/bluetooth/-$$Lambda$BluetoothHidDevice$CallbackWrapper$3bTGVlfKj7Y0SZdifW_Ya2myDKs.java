package android.bluetooth;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BluetoothHidDevice$CallbackWrapper$3bTGVlfKj7Y0SZdifW_Ya2myDKs implements Runnable {
    private final /* synthetic */ CallbackWrapper f$0;
    private final /* synthetic */ BluetoothDevice f$1;
    private final /* synthetic */ byte f$2;
    private final /* synthetic */ byte f$3;
    private final /* synthetic */ byte[] f$4;

    public /* synthetic */ -$$Lambda$BluetoothHidDevice$CallbackWrapper$3bTGVlfKj7Y0SZdifW_Ya2myDKs(CallbackWrapper callbackWrapper, BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) {
        this.f$0 = callbackWrapper;
        this.f$1 = bluetoothDevice;
        this.f$2 = b;
        this.f$3 = b2;
        this.f$4 = bArr;
    }

    public final void run() {
        this.f$0.mCallback.onSetReport(this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
