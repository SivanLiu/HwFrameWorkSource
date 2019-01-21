package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;

public interface IImsConfig extends IInterface {

    public static abstract class Stub extends Binder implements IImsConfig {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsConfig";
        static final int TRANSACTION_addImsConfigCallback = 1;
        static final int TRANSACTION_getConfigInt = 3;
        static final int TRANSACTION_getConfigString = 4;
        static final int TRANSACTION_getImsConfig = 8;
        static final int TRANSACTION_removeImsConfigCallback = 2;
        static final int TRANSACTION_setConfigInt = 5;
        static final int TRANSACTION_setConfigString = 6;
        static final int TRANSACTION_setImsConfig = 7;

        private static class Proxy implements IImsConfig {
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

            public void addImsConfigCallback(IImsConfigCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeImsConfigCallback(IImsConfigCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getConfigInt(int item) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(item);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getConfigString(int item) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(item);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setConfigInt(int item, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(item);
                    _data.writeInt(value);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setConfigString(int item, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(item);
                    _data.writeString(value);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setImsConfig(String configKey, PersistableBundle configValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(configKey);
                    if (configValue != null) {
                        _data.writeInt(1);
                        configValue.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public PersistableBundle getImsConfig(String configKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    PersistableBundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(configKey);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsConfig asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImsConfig)) {
                return new Proxy(obj);
            }
            return (IImsConfig) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                int _result;
                PersistableBundle _arg1;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        addImsConfigCallback(android.telephony.ims.aidl.IImsConfigCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        removeImsConfigCallback(android.telephony.ims.aidl.IImsConfigCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        int _result2 = getConfigInt(data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        String _result3 = getConfigString(data.readInt());
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result = setConfigInt(data.readInt(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _result = setConfigString(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        String _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(data);
                        } else {
                            _arg1 = null;
                        }
                        _result = setImsConfig(_arg0, _arg1);
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        _arg1 = getImsConfig(data.readString());
                        reply.writeNoException();
                        if (_arg1 != null) {
                            reply.writeInt(1);
                            _arg1.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void addImsConfigCallback(IImsConfigCallback iImsConfigCallback) throws RemoteException;

    int getConfigInt(int i) throws RemoteException;

    String getConfigString(int i) throws RemoteException;

    PersistableBundle getImsConfig(String str) throws RemoteException;

    void removeImsConfigCallback(IImsConfigCallback iImsConfigCallback) throws RemoteException;

    int setConfigInt(int i, int i2) throws RemoteException;

    int setConfigString(int i, String str) throws RemoteException;

    int setImsConfig(String str, PersistableBundle persistableBundle) throws RemoteException;
}
