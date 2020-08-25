package com.huawei.wallet.sdk.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.wallet.sdk.service.IWalletFactoryResetCallBack;

public interface IWalletFactoryResetInterface extends IInterface {
    void factoryReset(IWalletFactoryResetCallBack iWalletFactoryResetCallBack) throws RemoteException;

    public static class Default implements IWalletFactoryResetInterface {
        @Override // com.huawei.wallet.sdk.service.IWalletFactoryResetInterface
        public void factoryReset(IWalletFactoryResetCallBack callback) throws RemoteException {
        }

        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IWalletFactoryResetInterface {
        private static final String DESCRIPTOR = "com.huawei.wallet.sdk.service.IWalletFactoryResetInterface";
        static final int TRANSACTION_factoryReset = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWalletFactoryResetInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWalletFactoryResetInterface)) {
                return new Proxy(obj);
            }
            return (IWalletFactoryResetInterface) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1) {
                data.enforceInterface(DESCRIPTOR);
                factoryReset(IWalletFactoryResetCallBack.Stub.asInterface(data.readStrongBinder()));
                return true;
            } else if (code != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements IWalletFactoryResetInterface {
            public static IWalletFactoryResetInterface sDefaultImpl;
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

            @Override // com.huawei.wallet.sdk.service.IWalletFactoryResetInterface
            public void factoryReset(IWalletFactoryResetCallBack callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (this.mRemote.transact(1, _data, null, 1) || Stub.getDefaultImpl() == null) {
                        _data.recycle();
                    } else {
                        Stub.getDefaultImpl().factoryReset(callback);
                    }
                } finally {
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IWalletFactoryResetInterface impl) {
            if (Proxy.sDefaultImpl != null || impl == null) {
                return false;
            }
            Proxy.sDefaultImpl = impl;
            return true;
        }

        public static IWalletFactoryResetInterface getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
