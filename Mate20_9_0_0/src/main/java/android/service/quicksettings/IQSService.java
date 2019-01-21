package android.service.quicksettings;

import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IQSService extends IInterface {

    public static abstract class Stub extends Binder implements IQSService {
        private static final String DESCRIPTOR = "android.service.quicksettings.IQSService";
        static final int TRANSACTION_getTile = 1;
        static final int TRANSACTION_isLocked = 6;
        static final int TRANSACTION_isSecure = 7;
        static final int TRANSACTION_onDialogHidden = 9;
        static final int TRANSACTION_onShowDialog = 4;
        static final int TRANSACTION_onStartActivity = 5;
        static final int TRANSACTION_onStartSuccessful = 10;
        static final int TRANSACTION_startUnlockAndRun = 8;
        static final int TRANSACTION_updateQsTile = 2;
        static final int TRANSACTION_updateStatusIcon = 3;

        private static class Proxy implements IQSService {
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

            public Tile getTile(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Tile _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Tile) Tile.CREATOR.createFromParcel(_reply);
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

            public void updateQsTile(Tile tile, IBinder service) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (tile != null) {
                        _data.writeInt(1);
                        tile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(service);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateStatusIcon(IBinder tile, Icon icon, String contentDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(contentDescription);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onShowDialog(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onStartActivity(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(6, _data, _reply, 0);
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

            public boolean isSecure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(7, _data, _reply, 0);
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

            public void startUnlockAndRun(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onDialogHidden(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onStartSuccessful(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(10, _data, _reply, 0);
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

        public static IQSService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IQSService)) {
                return new Proxy(obj);
            }
            return (IQSService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                Tile _arg1 = null;
                boolean _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        Tile _result2 = getTile(data.readStrongBinder());
                        reply.writeNoException();
                        if (_result2 != null) {
                            reply.writeInt(1);
                            _result2.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = (Tile) Tile.CREATOR.createFromParcel(data);
                        }
                        updateQsTile(_arg1, data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 3:
                        Icon _arg12;
                        data.enforceInterface(descriptor);
                        IBinder _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg12 = (Icon) Icon.CREATOR.createFromParcel(data);
                        }
                        updateStatusIcon(_arg0, _arg12, data.readString());
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        onShowDialog(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        onStartActivity(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _result = isLocked();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _result = isSecure();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        startUnlockAndRun(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        onDialogHidden(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        onStartSuccessful(data.readStrongBinder());
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

    Tile getTile(IBinder iBinder) throws RemoteException;

    boolean isLocked() throws RemoteException;

    boolean isSecure() throws RemoteException;

    void onDialogHidden(IBinder iBinder) throws RemoteException;

    void onShowDialog(IBinder iBinder) throws RemoteException;

    void onStartActivity(IBinder iBinder) throws RemoteException;

    void onStartSuccessful(IBinder iBinder) throws RemoteException;

    void startUnlockAndRun(IBinder iBinder) throws RemoteException;

    void updateQsTile(Tile tile, IBinder iBinder) throws RemoteException;

    void updateStatusIcon(IBinder iBinder, Icon icon, String str) throws RemoteException;
}
