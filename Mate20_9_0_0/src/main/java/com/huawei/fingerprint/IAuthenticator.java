package com.huawei.fingerprint;

import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAuthenticator extends IInterface {

    public static abstract class Stub extends Binder implements IAuthenticator {
        private static final String DESCRIPTOR = "com.huawei.fingerprint.IAuthenticator";
        static final int TRANSACTION_verifyUser = 1;

        private static class Proxy implements IAuthenticator {
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

            public int verifyUser(IFingerprintServiceReceiver client, IAuthenticatorListener callback, int userid, byte[] nonce, String aaid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    if (callback != null) {
                        iBinder = callback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(userid);
                    _data.writeByteArray(nonce);
                    _data.writeString(aaid);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
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

        public static IAuthenticator asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAuthenticator)) {
                return new Proxy(obj);
            }
            return (IAuthenticator) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = reply;
            String descriptor = DESCRIPTOR;
            if (i == 1) {
                data.enforceInterface(descriptor);
                int _result = verifyUser(android.hardware.fingerprint.IFingerprintServiceReceiver.Stub.asInterface(data.readStrongBinder()), com.huawei.fingerprint.IAuthenticatorListener.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.createByteArray(), data.readString());
                reply.writeNoException();
                parcel.writeInt(_result);
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                parcel.writeString(descriptor);
                return true;
            }
        }
    }

    int verifyUser(IFingerprintServiceReceiver iFingerprintServiceReceiver, IAuthenticatorListener iAuthenticatorListener, int i, byte[] bArr, String str) throws RemoteException;
}
