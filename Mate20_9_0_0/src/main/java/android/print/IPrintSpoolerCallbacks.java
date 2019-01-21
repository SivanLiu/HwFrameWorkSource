package android.print;

import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IPrintSpoolerCallbacks extends IInterface {

    public static abstract class Stub extends Binder implements IPrintSpoolerCallbacks {
        private static final String DESCRIPTOR = "android.print.IPrintSpoolerCallbacks";
        static final int TRANSACTION_customPrinterIconCacheCleared = 8;
        static final int TRANSACTION_onCancelPrintJobResult = 2;
        static final int TRANSACTION_onCustomPrinterIconCached = 7;
        static final int TRANSACTION_onGetCustomPrinterIconResult = 6;
        static final int TRANSACTION_onGetPrintJobInfoResult = 5;
        static final int TRANSACTION_onGetPrintJobInfosResult = 1;
        static final int TRANSACTION_onSetPrintJobStateResult = 3;
        static final int TRANSACTION_onSetPrintJobTagResult = 4;

        private static class Proxy implements IPrintSpoolerCallbacks {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void onGetPrintJobInfosResult(List<PrintJobInfo> printJob, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(printJob);
                    _data.writeInt(sequence);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCancelPrintJobResult(boolean canceled, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(canceled);
                    _data.writeInt(sequence);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSetPrintJobStateResult(boolean success, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(success);
                    _data.writeInt(sequence);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSetPrintJobTagResult(boolean success, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(success);
                    _data.writeInt(sequence);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (printJob != null) {
                        _data.writeInt(1);
                        printJob.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sequence);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetCustomPrinterIconResult(Icon icon, int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sequence);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCustomPrinterIconCached(int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sequence);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void customPrinterIconCacheCleared(int sequence) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sequence);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPrintSpoolerCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IPrintSpoolerCallbacks)) {
                return new Proxy(obj);
            }
            return (IPrintSpoolerCallbacks) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                PrintJobInfo _arg0 = null;
                boolean z = false;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        onGetPrintJobInfosResult(data.createTypedArrayList(PrintJobInfo.CREATOR), data.readInt());
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        onCancelPrintJobResult(z, data.readInt());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        onSetPrintJobStateResult(z, data.readInt());
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        onSetPrintJobTagResult(z, data.readInt());
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (PrintJobInfo) PrintJobInfo.CREATOR.createFromParcel(data);
                        }
                        onGetPrintJobInfoResult(_arg0, data.readInt());
                        return true;
                    case 6:
                        Icon _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Icon) Icon.CREATOR.createFromParcel(data);
                        }
                        onGetCustomPrinterIconResult(_arg02, data.readInt());
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        onCustomPrinterIconCached(data.readInt());
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        customPrinterIconCacheCleared(data.readInt());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void customPrinterIconCacheCleared(int i) throws RemoteException;

    void onCancelPrintJobResult(boolean z, int i) throws RemoteException;

    void onCustomPrinterIconCached(int i) throws RemoteException;

    void onGetCustomPrinterIconResult(Icon icon, int i) throws RemoteException;

    void onGetPrintJobInfoResult(PrintJobInfo printJobInfo, int i) throws RemoteException;

    void onGetPrintJobInfosResult(List<PrintJobInfo> list, int i) throws RemoteException;

    void onSetPrintJobStateResult(boolean z, int i) throws RemoteException;

    void onSetPrintJobTagResult(boolean z, int i) throws RemoteException;
}
