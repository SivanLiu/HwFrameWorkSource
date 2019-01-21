package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INonHardwareAcceleratedPackagesManager extends IInterface {

    public static abstract class Stub extends Binder implements INonHardwareAcceleratedPackagesManager {
        private static final String DESCRIPTOR = "android.app.INonHardwareAcceleratedPackagesManager";
        static final int TRANSACTION_getForceEnabled = 2;
        static final int TRANSACTION_hasPackage = 3;
        static final int TRANSACTION_removePackage = 4;
        static final int TRANSACTION_setForceEnabled = 1;

        private static class Proxy implements INonHardwareAcceleratedPackagesManager {
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

            public void setForceEnabled(String pkgName, boolean force) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(force);
                    this.mRemote.transact(Stub.TRANSACTION_setForceEnabled, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getForceEnabled(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_getForceEnabled, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasPackage(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_hasPackage, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removePackage(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(Stub.TRANSACTION_removePackage, _data, _reply, 0);
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

        public static INonHardwareAcceleratedPackagesManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INonHardwareAcceleratedPackagesManager)) {
                return new Proxy(obj);
            }
            return (INonHardwareAcceleratedPackagesManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                boolean _result;
                switch (code) {
                    case TRANSACTION_setForceEnabled /*1*/:
                        data.enforceInterface(descriptor);
                        setForceEnabled(data.readString(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_getForceEnabled /*2*/:
                        data.enforceInterface(descriptor);
                        _result = getForceEnabled(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case TRANSACTION_hasPackage /*3*/:
                        data.enforceInterface(descriptor);
                        _result = hasPackage(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case TRANSACTION_removePackage /*4*/:
                        data.enforceInterface(descriptor);
                        removePackage(data.readString());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    boolean getForceEnabled(String str) throws RemoteException;

    boolean hasPackage(String str) throws RemoteException;

    void removePackage(String str) throws RemoteException;

    void setForceEnabled(String str, boolean z) throws RemoteException;
}
