package com.huawei.nb.searchmanager.callback;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.searchmanager.client.model.ChangedIndexContent;

public interface IIndexChangeCallback extends IInterface {
    void onDataChanged(String str, ChangedIndexContent changedIndexContent) throws RemoteException;

    public static abstract class Stub extends Binder implements IIndexChangeCallback {
        private static final String DESCRIPTOR = "com.huawei.nb.searchmanager.callback.IIndexChangeCallback";
        static final int TRANSACTION_onDataChanged = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIndexChangeCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IIndexChangeCallback)) {
                return new Proxy(obj);
            }
            return (IIndexChangeCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ChangedIndexContent _arg1;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = ChangedIndexContent.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    onDataChanged(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IIndexChangeCallback {
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

            @Override // com.huawei.nb.searchmanager.callback.IIndexChangeCallback
            public void onDataChanged(String pkgName, ChangedIndexContent changedIndexContent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    if (changedIndexContent != null) {
                        _data.writeInt(1);
                        changedIndexContent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
