package android.bluetooth;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IBluetoothHidDevice extends IInterface {

    public static abstract class Stub extends Binder implements IBluetoothHidDevice {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothHidDevice";
        static final int TRANSACTION_connect = 7;
        static final int TRANSACTION_disconnect = 8;
        static final int TRANSACTION_getConnectedDevices = 9;
        static final int TRANSACTION_getConnectionState = 11;
        static final int TRANSACTION_getDevicesMatchingConnectionStates = 10;
        static final int TRANSACTION_getUserAppName = 12;
        static final int TRANSACTION_registerApp = 1;
        static final int TRANSACTION_replyReport = 4;
        static final int TRANSACTION_reportError = 5;
        static final int TRANSACTION_sendReport = 3;
        static final int TRANSACTION_unplug = 6;
        static final int TRANSACTION_unregisterApp = 2;

        private static class Proxy implements IBluetoothHidDevice {
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

            public boolean registerApp(BluetoothHidDeviceAppSdpSettings sdp, BluetoothHidDeviceAppQosSettings inQos, BluetoothHidDeviceAppQosSettings outQos, IBluetoothHidDeviceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (sdp != null) {
                        _data.writeInt(1);
                        sdp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (inQos != null) {
                        _data.writeInt(1);
                        inQos.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (outQos != null) {
                        _data.writeInt(1);
                        outQos.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unregisterApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(2, _data, _reply, 0);
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

            public boolean sendReport(BluetoothDevice device, int id, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(id);
                    _data.writeByteArray(data);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean replyReport(BluetoothDevice device, byte type, byte id, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByte(type);
                    _data.writeByte(id);
                    _data.writeByteArray(data);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean reportError(BluetoothDevice device, byte error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByte(error);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unplug(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean connect(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean disconnect(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<BluetoothDevice> getConnectedDevices() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(states);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    List<BluetoothDevice> _result = _reply.createTypedArrayList(BluetoothDevice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getConnectionState(BluetoothDevice device) throws RemoteException {
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
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getUserAppName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
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

        public static IBluetoothHidDevice asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBluetoothHidDevice)) {
                return new Proxy(obj);
            }
            return (IBluetoothHidDevice) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                BluetoothHidDeviceAppQosSettings _arg0 = null;
                boolean _result;
                BluetoothDevice _arg02;
                boolean _result2;
                switch (code) {
                    case 1:
                        BluetoothHidDeviceAppSdpSettings _arg03;
                        BluetoothHidDeviceAppQosSettings _arg1;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothHidDeviceAppSdpSettings) BluetoothHidDeviceAppSdpSettings.CREATOR.createFromParcel(data);
                        } else {
                            _arg03 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (BluetoothHidDeviceAppQosSettings) BluetoothHidDeviceAppQosSettings.CREATOR.createFromParcel(data);
                        } else {
                            _arg1 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (BluetoothHidDeviceAppQosSettings) BluetoothHidDeviceAppQosSettings.CREATOR.createFromParcel(data);
                        }
                        _result = registerApp(_arg03, _arg1, _arg0, android.bluetooth.IBluetoothHidDeviceCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        boolean _result3 = unregisterApp();
                        reply.writeNoException();
                        reply.writeInt(_result3);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        boolean _result4 = sendReport(_arg02, data.readInt(), data.createByteArray());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        _result = replyReport(_arg02, data.readByte(), data.readByte(), data.createByteArray());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        boolean _result5 = reportError(_arg02, data.readByte());
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        _result2 = unplug(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        _result2 = connect(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        _result2 = disconnect(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        List<BluetoothDevice> _result6 = getConnectedDevices();
                        reply.writeNoException();
                        reply.writeTypedList(_result6);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        List<BluetoothDevice> _result7 = getDevicesMatchingConnectionStates(data.createIntArray());
                        reply.writeNoException();
                        reply.writeTypedList(_result7);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(data);
                        }
                        int _result8 = getConnectionState(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result8);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        String _result9 = getUserAppName();
                        reply.writeNoException();
                        reply.writeString(_result9);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    boolean connect(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean disconnect(BluetoothDevice bluetoothDevice) throws RemoteException;

    List<BluetoothDevice> getConnectedDevices() throws RemoteException;

    int getConnectionState(BluetoothDevice bluetoothDevice) throws RemoteException;

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) throws RemoteException;

    String getUserAppName() throws RemoteException;

    boolean registerApp(BluetoothHidDeviceAppSdpSettings bluetoothHidDeviceAppSdpSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings2, IBluetoothHidDeviceCallback iBluetoothHidDeviceCallback) throws RemoteException;

    boolean replyReport(BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) throws RemoteException;

    boolean reportError(BluetoothDevice bluetoothDevice, byte b) throws RemoteException;

    boolean sendReport(BluetoothDevice bluetoothDevice, int i, byte[] bArr) throws RemoteException;

    boolean unplug(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean unregisterApp() throws RemoteException;
}
