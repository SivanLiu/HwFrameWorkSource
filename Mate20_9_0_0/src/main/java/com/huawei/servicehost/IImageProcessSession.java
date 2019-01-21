package com.huawei.servicehost;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImageProcessSession extends IInterface {

    public static abstract class Stub extends Binder implements IImageProcessSession {
        private static final String DESCRIPTOR = "com.huawei.servicehost.IImageProcessSession";
        static final int TRANSACTION_createIPObject = 4;
        static final int TRANSACTION_createIPRequest = 1;
        static final int TRANSACTION_process = 2;
        static final int TRANSACTION_setIPListener = 3;

        private static class Proxy implements IImageProcessSession {
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

            public IIPRequest createIPRequest(String type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(type);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IIPRequest _result = com.huawei.servicehost.IIPRequest.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void process(IIPRequest val, boolean isSync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val != null ? val.asBinder() : null);
                    _data.writeInt(isSync);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setIPListener(IIPListener val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(val != null ? val.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder createIPObject(String type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(type);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImageProcessSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImageProcessSession)) {
                return new Proxy(obj);
            }
            return (IImageProcessSession) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        IIPRequest _result = createIPRequest(data.readString());
                        reply.writeNoException();
                        reply.writeStrongBinder(_result != null ? _result.asBinder() : null);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        process(com.huawei.servicehost.IIPRequest.Stub.asInterface(data.readStrongBinder()), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        setIPListener(com.huawei.servicehost.IIPListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        IBinder _result2 = createIPObject(data.readString());
                        reply.writeNoException();
                        reply.writeStrongBinder(_result2);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    IBinder createIPObject(String str) throws RemoteException;

    IIPRequest createIPRequest(String str) throws RemoteException;

    void process(IIPRequest iIPRequest, boolean z) throws RemoteException;

    void setIPListener(IIPListener iIPListener) throws RemoteException;
}
