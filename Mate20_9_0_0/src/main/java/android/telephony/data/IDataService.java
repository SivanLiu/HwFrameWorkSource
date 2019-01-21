package android.telephony.data;

import android.net.LinkProperties;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IDataService extends IInterface {

    public static abstract class Stub extends Binder implements IDataService {
        private static final String DESCRIPTOR = "android.telephony.data.IDataService";
        static final int TRANSACTION_createDataServiceProvider = 1;
        static final int TRANSACTION_deactivateDataCall = 4;
        static final int TRANSACTION_getDataCallList = 7;
        static final int TRANSACTION_registerForDataCallListChanged = 8;
        static final int TRANSACTION_removeDataServiceProvider = 2;
        static final int TRANSACTION_setDataProfile = 6;
        static final int TRANSACTION_setInitialAttachApn = 5;
        static final int TRANSACTION_setupDataCall = 3;
        static final int TRANSACTION_unregisterForDataCallListChanged = 9;

        private static class Proxy implements IDataService {
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

            public void createDataServiceProvider(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void removeDataServiceProvider(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setupDataCall(int slotId, int accessNetwork, DataProfile dataProfile, boolean isRoaming, boolean allowRoaming, int reason, LinkProperties linkProperties, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(accessNetwork);
                    if (dataProfile != null) {
                        _data.writeInt(1);
                        dataProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isRoaming);
                    _data.writeInt(allowRoaming);
                    _data.writeInt(reason);
                    if (linkProperties != null) {
                        _data.writeInt(1);
                        linkProperties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deactivateDataCall(int slotId, int cid, int reason, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(cid);
                    _data.writeInt(reason);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setInitialAttachApn(int slotId, DataProfile dataProfile, boolean isRoaming, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    if (dataProfile != null) {
                        _data.writeInt(1);
                        dataProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isRoaming);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setDataProfile(int slotId, List<DataProfile> dps, boolean isRoaming, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeTypedList(dps);
                    _data.writeInt(isRoaming);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getDataCallList(int slotId, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void registerForDataCallListChanged(int slotId, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unregisterForDataCallListChanged(int slotId, IDataServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDataService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IDataService)) {
                return new Proxy(obj);
            }
            return (IDataService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                LinkProperties _arg1 = null;
                boolean _arg2 = false;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        createDataServiceProvider(data.readInt());
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        removeDataServiceProvider(data.readInt());
                        return true;
                    case 3:
                        DataProfile _arg22;
                        parcel.enforceInterface(descriptor);
                        int _arg0 = data.readInt();
                        int _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = (DataProfile) DataProfile.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        boolean _arg3 = data.readInt() != 0;
                        boolean _arg4 = data.readInt() != 0;
                        int _arg5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (LinkProperties) LinkProperties.CREATOR.createFromParcel(parcel);
                        }
                        setupDataCall(_arg0, _arg12, _arg22, _arg3, _arg4, _arg5, _arg1, android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        deactivateDataCall(data.readInt(), data.readInt(), data.readInt(), android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 5:
                        DataProfile _arg13;
                        parcel.enforceInterface(descriptor);
                        int _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (DataProfile) DataProfile.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setInitialAttachApn(_arg02, _arg13, _arg2, android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        int _arg03 = data.readInt();
                        List<DataProfile> _arg14 = parcel.createTypedArrayList(DataProfile.CREATOR);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setDataProfile(_arg03, _arg14, _arg2, android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        getDataCallList(data.readInt(), android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        registerForDataCallListChanged(data.readInt(), android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        unregisterForDataCallListChanged(data.readInt(), android.telephony.data.IDataServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void createDataServiceProvider(int i) throws RemoteException;

    void deactivateDataCall(int i, int i2, int i3, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void getDataCallList(int i, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void registerForDataCallListChanged(int i, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void removeDataServiceProvider(int i) throws RemoteException;

    void setDataProfile(int i, List<DataProfile> list, boolean z, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void setInitialAttachApn(int i, DataProfile dataProfile, boolean z, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void setupDataCall(int i, int i2, DataProfile dataProfile, boolean z, boolean z2, int i3, LinkProperties linkProperties, IDataServiceCallback iDataServiceCallback) throws RemoteException;

    void unregisterForDataCallListChanged(int i, IDataServiceCallback iDataServiceCallback) throws RemoteException;
}
