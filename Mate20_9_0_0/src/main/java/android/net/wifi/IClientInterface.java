package android.net.wifi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IClientInterface extends IInterface {

    public static abstract class Stub extends Binder implements IClientInterface {
        private static final String DESCRIPTOR = "android.net.wifi.IClientInterface";
        static final int TRANSACTION_getInterfaceName = 4;
        static final int TRANSACTION_getMacAddress = 3;
        static final int TRANSACTION_getPacketCounters = 1;
        static final int TRANSACTION_getWifiScannerImpl = 5;
        static final int TRANSACTION_setMacAddress = 6;
        static final int TRANSACTION_signalPoll = 2;
        static final int TRANSACTION_subscribeVendorEvents = 7;
        static final int TRANSACTION_unsubscribeVendorEvents = 8;

        private static class Proxy implements IClientInterface {
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

            public int[] getPacketCounters() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] signalPoll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] getMacAddress() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getInterfaceName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IWifiScannerImpl getWifiScannerImpl() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    IWifiScannerImpl _result = android.net.wifi.IWifiScannerImpl.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setMacAddress(byte[] mac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(mac);
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

            public void subscribeVendorEvents(IHwVendorEvent handler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(handler != null ? handler.asBinder() : null);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unsubscribeVendorEvents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IClientInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IClientInterface)) {
                return new Proxy(obj);
            }
            return (IClientInterface) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                int[] _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = getPacketCounters();
                        reply.writeNoException();
                        reply.writeIntArray(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result = signalPoll();
                        reply.writeNoException();
                        reply.writeIntArray(_result);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        byte[] _result2 = getMacAddress();
                        reply.writeNoException();
                        reply.writeByteArray(_result2);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        String _result3 = getInterfaceName();
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        IWifiScannerImpl _result4 = getWifiScannerImpl();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        boolean _result5 = setMacAddress(data.createByteArray());
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        subscribeVendorEvents(android.net.wifi.IHwVendorEvent.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        unsubscribeVendorEvents();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    String getInterfaceName() throws RemoteException;

    byte[] getMacAddress() throws RemoteException;

    int[] getPacketCounters() throws RemoteException;

    IWifiScannerImpl getWifiScannerImpl() throws RemoteException;

    boolean setMacAddress(byte[] bArr) throws RemoteException;

    int[] signalPoll() throws RemoteException;

    void subscribeVendorEvents(IHwVendorEvent iHwVendorEvent) throws RemoteException;

    void unsubscribeVendorEvents() throws RemoteException;
}
