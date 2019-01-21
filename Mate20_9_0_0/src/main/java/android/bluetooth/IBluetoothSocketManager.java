package android.bluetooth;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.RemoteException;

public interface IBluetoothSocketManager extends IInterface {

    public static abstract class Stub extends Binder implements IBluetoothSocketManager {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothSocketManager";
        static final int TRANSACTION_connectSocket = 1;
        static final int TRANSACTION_createSocketChannel = 2;
        static final int TRANSACTION_requestMaximumTxDataLength = 3;

        private static class Proxy implements IBluetoothSocketManager {
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

            public ParcelFileDescriptor connectSocket(BluetoothDevice device, int type, ParcelUuid uuid, int port, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParcelFileDescriptor _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(type);
                    if (uuid != null) {
                        _data.writeInt(1);
                        uuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(port);
                    _data.writeInt(flag);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
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

            public ParcelFileDescriptor createSocketChannel(int type, String serviceName, ParcelUuid uuid, int port, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParcelFileDescriptor _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(serviceName);
                    if (uuid != null) {
                        _data.writeInt(1);
                        uuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(port);
                    _data.writeInt(flag);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
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

            public void requestMaximumTxDataLength(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
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

        public static IBluetoothSocketManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBluetoothSocketManager)) {
                return new Proxy(obj);
            }
            return (IBluetoothSocketManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                ParcelUuid _arg0 = null;
                BluetoothDevice _arg02;
                switch (i) {
                    case 1:
                        BluetoothDevice _arg03;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg03 = null;
                        }
                        int _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(parcel);
                        }
                        ParcelUuid _arg2 = _arg0;
                        ParcelFileDescriptor _result = connectSocket(_arg03, _arg1, _arg2, data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result != null) {
                            parcel2.writeInt(1);
                            _result.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        int _arg04 = data.readInt();
                        String _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg02 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(parcel);
                        }
                        BluetoothDevice _arg22 = _arg02;
                        ParcelFileDescriptor _result2 = createSocketChannel(_arg04, _arg12, _arg22, data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result2 != null) {
                            parcel2.writeInt(1);
                            _result2.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        }
                        requestMaximumTxDataLength(_arg02);
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    ParcelFileDescriptor connectSocket(BluetoothDevice bluetoothDevice, int i, ParcelUuid parcelUuid, int i2, int i3) throws RemoteException;

    ParcelFileDescriptor createSocketChannel(int i, String str, ParcelUuid parcelUuid, int i2, int i3) throws RemoteException;

    void requestMaximumTxDataLength(BluetoothDevice bluetoothDevice) throws RemoteException;
}
