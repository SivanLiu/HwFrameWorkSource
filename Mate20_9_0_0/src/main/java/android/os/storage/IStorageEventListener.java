package android.os.storage;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IStorageEventListener extends IInterface {

    public static abstract class Stub extends Binder implements IStorageEventListener {
        private static final String DESCRIPTOR = "android.os.storage.IStorageEventListener";
        static final int TRANSACTION_onDiskDestroyed = 7;
        static final int TRANSACTION_onDiskScanned = 6;
        static final int TRANSACTION_onStorageStateChanged = 2;
        static final int TRANSACTION_onUsbMassStorageConnectionChanged = 1;
        static final int TRANSACTION_onVolumeForgotten = 5;
        static final int TRANSACTION_onVolumeRecordChanged = 4;
        static final int TRANSACTION_onVolumeStateChanged = 3;

        private static class Proxy implements IStorageEventListener {
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

            public void onUsbMassStorageConnectionChanged(boolean connected) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(connected);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onStorageStateChanged(String path, String oldState, String newState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    _data.writeString(oldState);
                    _data.writeString(newState);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (vol != null) {
                        _data.writeInt(1);
                        vol.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(oldState);
                    _data.writeInt(newState);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onVolumeRecordChanged(VolumeRecord rec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (rec != null) {
                        _data.writeInt(1);
                        rec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onVolumeForgotten(String fsUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fsUuid);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onDiskScanned(DiskInfo disk, int volumeCount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (disk != null) {
                        _data.writeInt(1);
                        disk.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(volumeCount);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onDiskDestroyed(DiskInfo disk) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (disk != null) {
                        _data.writeInt(1);
                        disk.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStorageEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IStorageEventListener)) {
                return new Proxy(obj);
            }
            return (IStorageEventListener) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                VolumeInfo _arg0 = null;
                DiskInfo _arg02;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        onUsbMassStorageConnectionChanged(data.readInt() != 0);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        onStorageStateChanged(data.readString(), data.readString(), data.readString());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (VolumeInfo) VolumeInfo.CREATOR.createFromParcel(data);
                        }
                        onVolumeStateChanged(_arg0, data.readInt(), data.readInt());
                        return true;
                    case 4:
                        VolumeRecord _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (VolumeRecord) VolumeRecord.CREATOR.createFromParcel(data);
                        }
                        onVolumeRecordChanged(_arg03);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        onVolumeForgotten(data.readString());
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (DiskInfo) DiskInfo.CREATOR.createFromParcel(data);
                        }
                        onDiskScanned(_arg02, data.readInt());
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (DiskInfo) DiskInfo.CREATOR.createFromParcel(data);
                        }
                        onDiskDestroyed(_arg02);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onDiskDestroyed(DiskInfo diskInfo) throws RemoteException;

    void onDiskScanned(DiskInfo diskInfo, int i) throws RemoteException;

    void onStorageStateChanged(String str, String str2, String str3) throws RemoteException;

    void onUsbMassStorageConnectionChanged(boolean z) throws RemoteException;

    void onVolumeForgotten(String str) throws RemoteException;

    void onVolumeRecordChanged(VolumeRecord volumeRecord) throws RemoteException;

    void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) throws RemoteException;
}
