package android.print;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPrintSpoolerClient extends IInterface {

    public static abstract class Stub extends Binder implements IPrintSpoolerClient {
        private static final String DESCRIPTOR = "android.print.IPrintSpoolerClient";
        static final int TRANSACTION_onAllPrintJobsForServiceHandled = 2;
        static final int TRANSACTION_onAllPrintJobsHandled = 3;
        static final int TRANSACTION_onPrintJobQueued = 1;
        static final int TRANSACTION_onPrintJobStateChanged = 4;

        private static class Proxy implements IPrintSpoolerClient {
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

            public void onPrintJobQueued(PrintJobInfo printJob) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (printJob != null) {
                        _data.writeInt(1);
                        printJob.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAllPrintJobsForServiceHandled(ComponentName printService) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (printService != null) {
                        _data.writeInt(1);
                        printService.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAllPrintJobsHandled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrintJobStateChanged(PrintJobInfo printJob) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (printJob != null) {
                        _data.writeInt(1);
                        printJob.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPrintSpoolerClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IPrintSpoolerClient)) {
                return new Proxy(obj);
            }
            return (IPrintSpoolerClient) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                PrintJobInfo _arg0 = null;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (PrintJobInfo) PrintJobInfo.CREATOR.createFromParcel(data);
                        }
                        onPrintJobQueued(_arg0);
                        return true;
                    case 2:
                        ComponentName _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        onAllPrintJobsForServiceHandled(_arg02);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        onAllPrintJobsHandled();
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (PrintJobInfo) PrintJobInfo.CREATOR.createFromParcel(data);
                        }
                        onPrintJobStateChanged(_arg0);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onAllPrintJobsForServiceHandled(ComponentName componentName) throws RemoteException;

    void onAllPrintJobsHandled() throws RemoteException;

    void onPrintJobQueued(PrintJobInfo printJobInfo) throws RemoteException;

    void onPrintJobStateChanged(PrintJobInfo printJobInfo) throws RemoteException;
}
