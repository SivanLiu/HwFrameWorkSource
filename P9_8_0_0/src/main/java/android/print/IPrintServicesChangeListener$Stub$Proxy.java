package android.print;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

class IPrintServicesChangeListener$Stub$Proxy implements IPrintServicesChangeListener {
    private IBinder mRemote;

    IPrintServicesChangeListener$Stub$Proxy(IBinder remote) {
        this.mRemote = remote;
    }

    public IBinder asBinder() {
        return this.mRemote;
    }

    public String getInterfaceDescriptor() {
        return "android.print.IPrintServicesChangeListener";
    }

    public void onPrintServicesChanged() throws RemoteException {
        Parcel _data = Parcel.obtain();
        try {
            _data.writeInterfaceToken("android.print.IPrintServicesChangeListener");
            this.mRemote.transact(1, _data, null, 1);
        } finally {
            _data.recycle();
        }
    }
}
