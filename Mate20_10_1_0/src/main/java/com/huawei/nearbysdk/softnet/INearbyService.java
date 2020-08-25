package com.huawei.nearbysdk.softnet;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nearbysdk.softnet.INearConnectionCallback;
import com.huawei.nearbysdk.softnet.INearDiscoveryCallback;

public interface INearbyService extends IInterface {
    boolean publish(String str, String str2, NearAdvertiseOption nearAdvertiseOption, INearConnectionCallback iNearConnectionCallback) throws RemoteException;

    boolean subscribe(String str, String str2, NearListenOption nearListenOption, INearDiscoveryCallback iNearDiscoveryCallback) throws RemoteException;

    boolean unpublish(String str, String str2) throws RemoteException;

    boolean unsubscribe(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements INearbyService {
        private static final String DESCRIPTOR = "com.huawei.nearbysdk.softnet.INearbyService";
        static final int TRANSACTION_publish = 1;
        static final int TRANSACTION_subscribe = 3;
        static final int TRANSACTION_unpublish = 2;
        static final int TRANSACTION_unsubscribe = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INearbyService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INearbyService)) {
                return new Proxy(obj);
            }
            return (INearbyService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1598968902) {
                NearAdvertiseOption _arg2 = null;
                NearListenOption _arg22 = null;
                switch (code) {
                    case 1:
                        data.enforceInterface(DESCRIPTOR);
                        String _arg0 = data.readString();
                        String _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = NearAdvertiseOption.CREATOR.createFromParcel(data);
                        }
                        boolean publish = publish(_arg0, _arg1, _arg2, INearConnectionCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(publish ? 1 : 0);
                        return true;
                    case 2:
                        data.enforceInterface(DESCRIPTOR);
                        boolean unpublish = unpublish(data.readString(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(unpublish ? 1 : 0);
                        return true;
                    case 3:
                        data.enforceInterface(DESCRIPTOR);
                        String _arg02 = data.readString();
                        String _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = NearListenOption.CREATOR.createFromParcel(data);
                        }
                        boolean subscribe = subscribe(_arg02, _arg12, _arg22, INearDiscoveryCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(subscribe ? 1 : 0);
                        return true;
                    case 4:
                        data.enforceInterface(DESCRIPTOR);
                        boolean unsubscribe = unsubscribe(data.readString(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(unsubscribe ? 1 : 0);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                reply.writeString(DESCRIPTOR);
                return true;
            }
        }

        private static class Proxy implements INearbyService {
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

            @Override // com.huawei.nearbysdk.softnet.INearbyService
            public boolean publish(String moduleId, String serviceId, NearAdvertiseOption option, INearConnectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(moduleId);
                    _data.writeString(serviceId);
                    boolean _result = true;
                    if (option != null) {
                        _data.writeInt(1);
                        option.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nearbysdk.softnet.INearbyService
            public boolean unpublish(String moduleId, String serviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(moduleId);
                    _data.writeString(serviceId);
                    boolean _result = false;
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nearbysdk.softnet.INearbyService
            public boolean subscribe(String moduleId, String serviceId, NearListenOption option, INearDiscoveryCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(moduleId);
                    _data.writeString(serviceId);
                    boolean _result = true;
                    if (option != null) {
                        _data.writeInt(1);
                        option.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nearbysdk.softnet.INearbyService
            public boolean unsubscribe(String moduleId, String serviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(moduleId);
                    _data.writeString(serviceId);
                    boolean _result = false;
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
