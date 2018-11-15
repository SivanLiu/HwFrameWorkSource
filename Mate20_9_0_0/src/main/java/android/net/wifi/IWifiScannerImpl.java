package android.net.wifi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.server.wifi.wificond.NativeScanResult;
import com.android.server.wifi.wificond.PnoSettings;
import com.android.server.wifi.wificond.SingleScanSettings;

public interface IWifiScannerImpl extends IInterface {
    public static final int SCAN_TYPE_DEFAULT = -1;
    public static final int SCAN_TYPE_HIGH_ACCURACY = 2;
    public static final int SCAN_TYPE_LOW_POWER = 1;
    public static final int SCAN_TYPE_LOW_SPAN = 0;

    public static abstract class Stub extends Binder implements IWifiScannerImpl {
        private static final String DESCRIPTOR = "android.net.wifi.IWifiScannerImpl";
        static final int TRANSACTION_abortScan = 10;
        static final int TRANSACTION_getPnoScanResults = 2;
        static final int TRANSACTION_getScanResults = 1;
        static final int TRANSACTION_scan = 3;
        static final int TRANSACTION_startPnoScan = 8;
        static final int TRANSACTION_stopPnoScan = 9;
        static final int TRANSACTION_subscribePnoScanEvents = 6;
        static final int TRANSACTION_subscribeScanEvents = 4;
        static final int TRANSACTION_unsubscribePnoScanEvents = 7;
        static final int TRANSACTION_unsubscribeScanEvents = 5;

        private static class Proxy implements IWifiScannerImpl {
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

            public NativeScanResult[] getScanResults() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    NativeScanResult[] _result = (NativeScanResult[]) _reply.createTypedArray(NativeScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NativeScanResult[] getPnoScanResults() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    NativeScanResult[] _result = (NativeScanResult[]) _reply.createTypedArray(NativeScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean scan(SingleScanSettings scanSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (scanSettings != null) {
                        _data.writeInt(1);
                        scanSettings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
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

            public void subscribeScanEvents(IScanEvent handler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(handler != null ? handler.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unsubscribeScanEvents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void subscribePnoScanEvents(IPnoScanEvent handler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(handler != null ? handler.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unsubscribePnoScanEvents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public boolean startPnoScan(PnoSettings pnoSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (pnoSettings != null) {
                        _data.writeInt(1);
                        pnoSettings.writeToParcel(_data, 0);
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

            public boolean stopPnoScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(9, _data, _reply, 0);
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

            public void abortScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

        public static IWifiScannerImpl asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWifiScannerImpl)) {
                return new Proxy(obj);
            }
            return (IWifiScannerImpl) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                SingleScanSettings _arg0 = null;
                NativeScanResult[] _result;
                boolean _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = getScanResults();
                        reply.writeNoException();
                        reply.writeTypedArray(_result, 1);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result = getPnoScanResults();
                        reply.writeNoException();
                        reply.writeTypedArray(_result, 1);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (SingleScanSettings) SingleScanSettings.CREATOR.createFromParcel(data);
                        }
                        _result2 = scan(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        subscribeScanEvents(android.net.wifi.IScanEvent.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        unsubscribeScanEvents();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        subscribePnoScanEvents(android.net.wifi.IPnoScanEvent.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        unsubscribePnoScanEvents();
                        return true;
                    case 8:
                        PnoSettings _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (PnoSettings) PnoSettings.CREATOR.createFromParcel(data);
                        }
                        _result2 = startPnoScan(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        boolean _result3 = stopPnoScan();
                        reply.writeNoException();
                        reply.writeInt(_result3);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        abortScan();
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

    void abortScan() throws RemoteException;

    NativeScanResult[] getPnoScanResults() throws RemoteException;

    NativeScanResult[] getScanResults() throws RemoteException;

    boolean scan(SingleScanSettings singleScanSettings) throws RemoteException;

    boolean startPnoScan(PnoSettings pnoSettings) throws RemoteException;

    boolean stopPnoScan() throws RemoteException;

    void subscribePnoScanEvents(IPnoScanEvent iPnoScanEvent) throws RemoteException;

    void subscribeScanEvents(IScanEvent iScanEvent) throws RemoteException;

    void unsubscribePnoScanEvents() throws RemoteException;

    void unsubscribeScanEvents() throws RemoteException;
}
