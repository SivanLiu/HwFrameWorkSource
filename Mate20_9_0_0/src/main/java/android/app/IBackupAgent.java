package android.app;

import android.app.backup.IBackupManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IBackupAgent extends IInterface {

    public static abstract class Stub extends Binder implements IBackupAgent {
        private static final String DESCRIPTOR = "android.app.IBackupAgent";
        static final int TRANSACTION_doBackup = 1;
        static final int TRANSACTION_doFullBackup = 3;
        static final int TRANSACTION_doMeasureFullBackup = 4;
        static final int TRANSACTION_doQuotaExceeded = 5;
        static final int TRANSACTION_doRestore = 2;
        static final int TRANSACTION_doRestoreFile = 6;
        static final int TRANSACTION_doRestoreFinished = 7;
        static final int TRANSACTION_fail = 8;

        private static class Proxy implements IBackupAgent {
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

            public void doBackup(ParcelFileDescriptor oldState, ParcelFileDescriptor data, ParcelFileDescriptor newState, long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (oldState != null) {
                        _data.writeInt(1);
                        oldState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (newState != null) {
                        _data.writeInt(1);
                        newState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(quotaBytes);
                    _data.writeInt(token);
                    _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                    _data.writeInt(transportFlags);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void doRestore(ParcelFileDescriptor data, long appVersionCode, ParcelFileDescriptor newState, int token, IBackupManager callbackBinder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(appVersionCode);
                    if (newState != null) {
                        _data.writeInt(1);
                        newState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(token);
                    _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void doFullBackup(ParcelFileDescriptor data, long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(quotaBytes);
                    _data.writeInt(token);
                    _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                    _data.writeInt(transportFlags);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void doMeasureFullBackup(long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(quotaBytes);
                    _data.writeInt(token);
                    _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                    _data.writeInt(transportFlags);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void doQuotaExceeded(long backupDataBytes, long quotaBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(backupDataBytes);
                    _data.writeLong(quotaBytes);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void doRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String path, long mode, long mtime, int token, IBackupManager callbackBinder) throws RemoteException {
                Throwable th;
                long j;
                long j2;
                int i;
                String str;
                String str2;
                ParcelFileDescriptor parcelFileDescriptor = data;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parcelFileDescriptor != null) {
                        _data.writeInt(1);
                        parcelFileDescriptor.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(size);
                    try {
                        _data.writeInt(type);
                        try {
                            _data.writeString(domain);
                            try {
                                _data.writeString(path);
                            } catch (Throwable th2) {
                                th = th2;
                                j = mode;
                                j2 = mtime;
                                i = token;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            str = path;
                            j = mode;
                            j2 = mtime;
                            i = token;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        str2 = domain;
                        str = path;
                        j = mode;
                        j2 = mtime;
                        i = token;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(mode);
                        try {
                            _data.writeLong(mtime);
                            try {
                                _data.writeInt(token);
                                _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                            } catch (Throwable th5) {
                                th = th5;
                                _data.recycle();
                                throw th;
                            }
                            try {
                                this.mRemote.transact(6, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            i = token;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        j2 = mtime;
                        i = token;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    int i2 = type;
                    str2 = domain;
                    str = path;
                    j = mode;
                    j2 = mtime;
                    i = token;
                    _data.recycle();
                    throw th;
                }
            }

            public void doRestoreFinished(int token, IBackupManager callbackBinder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeStrongBinder(callbackBinder != null ? callbackBinder.asBinder() : null);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void fail(String message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(message);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBackupAgent asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBackupAgent)) {
                return new Proxy(obj);
            }
            return (IBackupAgent) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                ParcelFileDescriptor parcelFileDescriptor = null;
                ParcelFileDescriptor _arg0;
                switch (i) {
                    case 1:
                        ParcelFileDescriptor _arg1;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg1 = null;
                        }
                        if (data.readInt() != 0) {
                            parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        ParcelFileDescriptor _arg2 = parcelFileDescriptor;
                        doBackup(_arg0, _arg1, _arg2, data.readLong(), data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg0 = null;
                        }
                        long _arg12 = data.readLong();
                        if (data.readInt() != 0) {
                            parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        ParcelFileDescriptor _arg22 = parcelFileDescriptor;
                        doRestore(_arg0, _arg12, _arg22, data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        _arg0 = parcelFileDescriptor;
                        doFullBackup(_arg0, data.readLong(), data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        doMeasureFullBackup(data.readLong(), data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        doQuotaExceeded(data.readLong(), data.readLong());
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        _arg0 = parcelFileDescriptor;
                        doRestoreFile(_arg0, data.readLong(), data.readInt(), data.readString(), data.readString(), data.readLong(), data.readLong(), data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        doRestoreFinished(data.readInt(), android.app.backup.IBackupManager.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        fail(data.readString());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void doBackup(ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2, ParcelFileDescriptor parcelFileDescriptor3, long j, int i, IBackupManager iBackupManager, int i2) throws RemoteException;

    void doFullBackup(ParcelFileDescriptor parcelFileDescriptor, long j, int i, IBackupManager iBackupManager, int i2) throws RemoteException;

    void doMeasureFullBackup(long j, int i, IBackupManager iBackupManager, int i2) throws RemoteException;

    void doQuotaExceeded(long j, long j2) throws RemoteException;

    void doRestore(ParcelFileDescriptor parcelFileDescriptor, long j, ParcelFileDescriptor parcelFileDescriptor2, int i, IBackupManager iBackupManager) throws RemoteException;

    void doRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str, String str2, long j2, long j3, int i2, IBackupManager iBackupManager) throws RemoteException;

    void doRestoreFinished(int i, IBackupManager iBackupManager) throws RemoteException;

    void fail(String str) throws RemoteException;
}
