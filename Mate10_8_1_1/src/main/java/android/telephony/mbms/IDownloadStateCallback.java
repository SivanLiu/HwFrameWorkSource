package android.telephony.mbms;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDownloadStateCallback extends IInterface {

    public static abstract class Stub extends Binder implements IDownloadStateCallback {
        private static final String DESCRIPTOR = "android.telephony.mbms.IDownloadStateCallback";
        static final int TRANSACTION_onProgressUpdated = 1;
        static final int TRANSACTION_onStateUpdated = 2;

        private static class Proxy implements IDownloadStateCallback {
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

            public void onProgressUpdated(DownloadRequest request, FileInfo fileInfo, int currentDownloadSize, int fullDownloadSize, int currentDecodedSize, int fullDecodedSize) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (fileInfo != null) {
                        _data.writeInt(1);
                        fileInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(currentDownloadSize);
                    _data.writeInt(fullDownloadSize);
                    _data.writeInt(currentDecodedSize);
                    _data.writeInt(fullDecodedSize);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onStateUpdated(DownloadRequest request, FileInfo fileInfo, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (fileInfo != null) {
                        _data.writeInt(1);
                        fileInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDownloadStateCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IDownloadStateCallback)) {
                return new Proxy(obj);
            }
            return (IDownloadStateCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            DownloadRequest downloadRequest;
            FileInfo fileInfo;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        downloadRequest = (DownloadRequest) DownloadRequest.CREATOR.createFromParcel(data);
                    } else {
                        downloadRequest = null;
                    }
                    if (data.readInt() != 0) {
                        fileInfo = (FileInfo) FileInfo.CREATOR.createFromParcel(data);
                    } else {
                        fileInfo = null;
                    }
                    onProgressUpdated(downloadRequest, fileInfo, data.readInt(), data.readInt(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        downloadRequest = (DownloadRequest) DownloadRequest.CREATOR.createFromParcel(data);
                    } else {
                        downloadRequest = null;
                    }
                    if (data.readInt() != 0) {
                        fileInfo = (FileInfo) FileInfo.CREATOR.createFromParcel(data);
                    } else {
                        fileInfo = null;
                    }
                    onStateUpdated(downloadRequest, fileInfo, data.readInt());
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onProgressUpdated(DownloadRequest downloadRequest, FileInfo fileInfo, int i, int i2, int i3, int i4) throws RemoteException;

    void onStateUpdated(DownloadRequest downloadRequest, FileInfo fileInfo, int i) throws RemoteException;
}
