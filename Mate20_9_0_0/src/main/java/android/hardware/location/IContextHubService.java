package android.hardware.location;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IContextHubService extends IInterface {

    public static abstract class Stub extends Binder implements IContextHubService {
        private static final String DESCRIPTOR = "android.hardware.location.IContextHubService";
        static final int TRANSACTION_createClient = 9;
        static final int TRANSACTION_disableNanoApp = 14;
        static final int TRANSACTION_enableNanoApp = 13;
        static final int TRANSACTION_findNanoAppOnHub = 7;
        static final int TRANSACTION_getContextHubHandles = 2;
        static final int TRANSACTION_getContextHubInfo = 3;
        static final int TRANSACTION_getContextHubs = 10;
        static final int TRANSACTION_getNanoAppInstanceInfo = 6;
        static final int TRANSACTION_loadNanoApp = 4;
        static final int TRANSACTION_loadNanoAppOnHub = 11;
        static final int TRANSACTION_queryNanoApps = 15;
        static final int TRANSACTION_registerCallback = 1;
        static final int TRANSACTION_sendMessage = 8;
        static final int TRANSACTION_unloadNanoApp = 5;
        static final int TRANSACTION_unloadNanoAppFromHub = 12;

        private static class Proxy implements IContextHubService {
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

            public int registerCallback(IContextHubCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getContextHubHandles() throws RemoteException {
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

            public ContextHubInfo getContextHubInfo(int contextHubHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ContextHubInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubHandle);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ContextHubInfo) ContextHubInfo.CREATOR.createFromParcel(_reply);
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

            public int loadNanoApp(int contextHubHandle, NanoApp nanoApp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubHandle);
                    if (nanoApp != null) {
                        _data.writeInt(1);
                        nanoApp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int unloadNanoApp(int nanoAppHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nanoAppHandle);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NanoAppInstanceInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nanoAppHandle);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NanoAppInstanceInfo) NanoAppInstanceInfo.CREATOR.createFromParcel(_reply);
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

            public int[] findNanoAppOnHub(int contextHubHandle, NanoAppFilter filter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubHandle);
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int sendMessage(int contextHubHandle, int nanoAppHandle, ContextHubMessage msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubHandle);
                    _data.writeInt(nanoAppHandle);
                    if (msg != null) {
                        _data.writeInt(1);
                        msg.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IContextHubClient createClient(IContextHubClientCallback client, int contextHubId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(contextHubId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    IContextHubClient _result = android.hardware.location.IContextHubClient.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<ContextHubInfo> getContextHubs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    List<ContextHubInfo> _result = _reply.createTypedArrayList(ContextHubInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void loadNanoAppOnHub(int contextHubId, IContextHubTransactionCallback transactionCallback, NanoAppBinary nanoAppBinary) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubId);
                    _data.writeStrongBinder(transactionCallback != null ? transactionCallback.asBinder() : null);
                    if (nanoAppBinary != null) {
                        _data.writeInt(1);
                        nanoAppBinary.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unloadNanoAppFromHub(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubId);
                    _data.writeStrongBinder(transactionCallback != null ? transactionCallback.asBinder() : null);
                    _data.writeLong(nanoAppId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubId);
                    _data.writeStrongBinder(transactionCallback != null ? transactionCallback.asBinder() : null);
                    _data.writeLong(nanoAppId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubId);
                    _data.writeStrongBinder(transactionCallback != null ? transactionCallback.asBinder() : null);
                    _data.writeLong(nanoAppId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void queryNanoApps(int contextHubId, IContextHubTransactionCallback transactionCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(contextHubId);
                    _data.writeStrongBinder(transactionCallback != null ? transactionCallback.asBinder() : null);
                    this.mRemote.transact(15, _data, _reply, 0);
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

        public static IContextHubService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IContextHubService)) {
                return new Proxy(obj);
            }
            return (IContextHubService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                NanoApp _arg1 = null;
                int _result;
                int _arg0;
                int _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = registerCallback(android.hardware.location.IContextHubCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        int[] _result3 = getContextHubHandles();
                        reply.writeNoException();
                        reply.writeIntArray(_result3);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        ContextHubInfo _result4 = getContextHubInfo(data.readInt());
                        reply.writeNoException();
                        if (_result4 != null) {
                            reply.writeInt(1);
                            _result4.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (NanoApp) NanoApp.CREATOR.createFromParcel(data);
                        }
                        _result2 = loadNanoApp(_arg0, _arg1);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result = unloadNanoApp(data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        NanoAppInstanceInfo _result5 = getNanoAppInstanceInfo(data.readInt());
                        reply.writeNoException();
                        if (_result5 != null) {
                            reply.writeInt(1);
                            _result5.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 7:
                        NanoAppFilter _arg12;
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (NanoAppFilter) NanoAppFilter.CREATOR.createFromParcel(data);
                        }
                        int[] _result6 = findNanoAppOnHub(_arg0, _arg12);
                        reply.writeNoException();
                        reply.writeIntArray(_result6);
                        return true;
                    case 8:
                        ContextHubMessage _arg2;
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _result2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (ContextHubMessage) ContextHubMessage.CREATOR.createFromParcel(data);
                        }
                        int _result7 = sendMessage(_arg0, _result2, _arg2);
                        reply.writeNoException();
                        reply.writeInt(_result7);
                        return true;
                    case 9:
                        IBinder _arg22;
                        data.enforceInterface(descriptor);
                        IContextHubClient _result8 = createClient(android.hardware.location.IContextHubClientCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        if (_result8 != null) {
                            _arg22 = _result8.asBinder();
                        }
                        reply.writeStrongBinder(_arg22);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        List<ContextHubInfo> _result9 = getContextHubs();
                        reply.writeNoException();
                        reply.writeTypedList(_result9);
                        return true;
                    case 11:
                        NanoAppBinary _arg23;
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        IContextHubTransactionCallback _arg13 = android.hardware.location.IContextHubTransactionCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg23 = (NanoAppBinary) NanoAppBinary.CREATOR.createFromParcel(data);
                        }
                        loadNanoAppOnHub(_arg0, _arg13, _arg23);
                        reply.writeNoException();
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        unloadNanoAppFromHub(data.readInt(), android.hardware.location.IContextHubTransactionCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        enableNanoApp(data.readInt(), android.hardware.location.IContextHubTransactionCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        disableNanoApp(data.readInt(), android.hardware.location.IContextHubTransactionCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        queryNanoApps(data.readInt(), android.hardware.location.IContextHubTransactionCallback.Stub.asInterface(data.readStrongBinder()));
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

    IContextHubClient createClient(IContextHubClientCallback iContextHubClientCallback, int i) throws RemoteException;

    void disableNanoApp(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException;

    void enableNanoApp(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException;

    int[] findNanoAppOnHub(int i, NanoAppFilter nanoAppFilter) throws RemoteException;

    int[] getContextHubHandles() throws RemoteException;

    ContextHubInfo getContextHubInfo(int i) throws RemoteException;

    List<ContextHubInfo> getContextHubs() throws RemoteException;

    NanoAppInstanceInfo getNanoAppInstanceInfo(int i) throws RemoteException;

    int loadNanoApp(int i, NanoApp nanoApp) throws RemoteException;

    void loadNanoAppOnHub(int i, IContextHubTransactionCallback iContextHubTransactionCallback, NanoAppBinary nanoAppBinary) throws RemoteException;

    void queryNanoApps(int i, IContextHubTransactionCallback iContextHubTransactionCallback) throws RemoteException;

    int registerCallback(IContextHubCallback iContextHubCallback) throws RemoteException;

    int sendMessage(int i, int i2, ContextHubMessage contextHubMessage) throws RemoteException;

    int unloadNanoApp(int i) throws RemoteException;

    void unloadNanoAppFromHub(int i, IContextHubTransactionCallback iContextHubTransactionCallback, long j) throws RemoteException;
}
