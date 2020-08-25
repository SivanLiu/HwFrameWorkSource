package com.huawei.server.security.behaviorcollect;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBehaviorAuthService extends IInterface {
    float getBotDetectResult(Bundle bundle) throws RemoteException;

    public static class Default implements IBehaviorAuthService {
        @Override // com.huawei.server.security.behaviorcollect.IBehaviorAuthService
        public float getBotDetectResult(Bundle data) throws RemoteException {
            return 0.0f;
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IBehaviorAuthService {
        private static final String DESCRIPTOR = "com.huawei.server.security.behaviorcollect.IBehaviorAuthService";
        static final int TRANSACTION_getBotDetectResult = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBehaviorAuthService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBehaviorAuthService)) {
                return new Proxy(obj);
            }
            return (IBehaviorAuthService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg0;
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                if (data.readInt() != 0) {
                    _arg0 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                } else {
                    _arg0 = null;
                }
                float _result = getBotDetectResult(_arg0);
                reply.writeNoException();
                reply.writeFloat(_result);
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IBehaviorAuthService {
            public static IBehaviorAuthService sDefaultImpl;
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

            @Override // com.huawei.server.security.behaviorcollect.IBehaviorAuthService
            public float getBotDetectResult(Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!this.mRemote.transact(1, _data, _reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getBotDetectResult(data);
                    }
                    _reply.readException();
                    float _result = _reply.readFloat();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IBehaviorAuthService impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IBehaviorAuthService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
