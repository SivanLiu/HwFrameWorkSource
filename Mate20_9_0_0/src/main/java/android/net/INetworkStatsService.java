package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface INetworkStatsService extends IInterface {

    public static abstract class Stub extends Binder implements INetworkStatsService {
        private static final String DESCRIPTOR = "android.net.INetworkStatsService";
        static final int TRANSACTION_forceUpdate = 8;
        static final int TRANSACTION_forceUpdateIfaces = 7;
        static final int TRANSACTION_getDataLayerSnapshotForUid = 3;
        static final int TRANSACTION_getDetailedUidStats = 4;
        static final int TRANSACTION_getIfaceStats = 12;
        static final int TRANSACTION_getMobileIfaces = 5;
        static final int TRANSACTION_getTotalStats = 13;
        static final int TRANSACTION_getUidStats = 11;
        static final int TRANSACTION_incrementOperationCount = 6;
        static final int TRANSACTION_openSession = 1;
        static final int TRANSACTION_openSessionForUsageStats = 2;
        static final int TRANSACTION_registerUsageCallback = 9;
        static final int TRANSACTION_tagSocket = 14;
        static final int TRANSACTION_unregisterUsageRequest = 10;

        private static class Proxy implements INetworkStatsService {
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

            public INetworkStatsSession openSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    INetworkStatsSession _result = android.net.INetworkStatsSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INetworkStatsSession openSessionForUsageStats(int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    INetworkStatsSession _result = android.net.INetworkStatsSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NetworkStats getDataLayerSnapshotForUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public NetworkStats getDetailedUidStats(String[] requiredIfaces) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(requiredIfaces);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public String[] getMobileIfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void incrementOperationCount(int uid, int tag, int operationCount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(tag);
                    _data.writeInt(operationCount);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void forceUpdateIfaces(Network[] defaultNetworks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(defaultNetworks, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void forceUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public DataUsageRequest registerUsageCallback(String callingPackage, DataUsageRequest request, Messenger messenger, IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    DataUsageRequest _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (DataUsageRequest) DataUsageRequest.CREATOR.createFromParcel(_reply);
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

            public void unregisterUsageRequest(DataUsageRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getUidStats(int uid, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(type);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getIfaceStats(String iface, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(type);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getTotalStats(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int tagSocket(ParcelFileDescriptor pfd, int tag, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pfd != null) {
                        _data.writeInt(1);
                        pfd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(tag);
                    _data.writeInt(uid);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
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

        public static INetworkStatsService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetworkStatsService)) {
                return new Proxy(obj);
            }
            return (INetworkStatsService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                IBinder _arg2 = null;
                NetworkStats _result;
                long _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        INetworkStatsSession _result3 = openSession();
                        reply.writeNoException();
                        if (_result3 != null) {
                            _arg2 = _result3.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        INetworkStatsSession _result4 = openSessionForUsageStats(data.readInt(), data.readString());
                        reply.writeNoException();
                        if (_result4 != null) {
                            _arg2 = _result4.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _result = getDataLayerSnapshotForUid(data.readInt());
                        reply.writeNoException();
                        if (_result != null) {
                            reply.writeInt(1);
                            _result.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _result = getDetailedUidStats(data.createStringArray());
                        reply.writeNoException();
                        if (_result != null) {
                            reply.writeInt(1);
                            _result.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        String[] _result5 = getMobileIfaces();
                        reply.writeNoException();
                        reply.writeStringArray(_result5);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        incrementOperationCount(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        forceUpdateIfaces((Network[]) data.createTypedArray(Network.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        forceUpdate();
                        reply.writeNoException();
                        return true;
                    case 9:
                        DataUsageRequest _arg1;
                        Messenger _arg22;
                        data.enforceInterface(descriptor);
                        String _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (DataUsageRequest) DataUsageRequest.CREATOR.createFromParcel(data);
                        } else {
                            _arg1 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg22 = (Messenger) Messenger.CREATOR.createFromParcel(data);
                        }
                        DataUsageRequest _result6 = registerUsageCallback(_arg0, _arg1, _arg22, data.readStrongBinder());
                        reply.writeNoException();
                        if (_result6 != null) {
                            reply.writeInt(1);
                            _result6.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 10:
                        DataUsageRequest _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (DataUsageRequest) DataUsageRequest.CREATOR.createFromParcel(data);
                        }
                        unregisterUsageRequest(_arg02);
                        reply.writeNoException();
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _result2 = getUidStats(data.readInt(), data.readInt());
                        reply.writeNoException();
                        reply.writeLong(_result2);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        _result2 = getIfaceStats(data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeLong(_result2);
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        long _result7 = getTotalStats(data.readInt());
                        reply.writeNoException();
                        reply.writeLong(_result7);
                        return true;
                    case 14:
                        ParcelFileDescriptor _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                        }
                        int _result8 = tagSocket(_arg03, data.readInt(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result8);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void forceUpdate() throws RemoteException;

    void forceUpdateIfaces(Network[] networkArr) throws RemoteException;

    NetworkStats getDataLayerSnapshotForUid(int i) throws RemoteException;

    NetworkStats getDetailedUidStats(String[] strArr) throws RemoteException;

    long getIfaceStats(String str, int i) throws RemoteException;

    String[] getMobileIfaces() throws RemoteException;

    long getTotalStats(int i) throws RemoteException;

    long getUidStats(int i, int i2) throws RemoteException;

    void incrementOperationCount(int i, int i2, int i3) throws RemoteException;

    INetworkStatsSession openSession() throws RemoteException;

    INetworkStatsSession openSessionForUsageStats(int i, String str) throws RemoteException;

    DataUsageRequest registerUsageCallback(String str, DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder) throws RemoteException;

    int tagSocket(ParcelFileDescriptor parcelFileDescriptor, int i, int i2) throws RemoteException;

    void unregisterUsageRequest(DataUsageRequest dataUsageRequest) throws RemoteException;
}
