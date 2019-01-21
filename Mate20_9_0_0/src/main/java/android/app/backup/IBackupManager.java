package android.app.backup;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IBackupManager extends IInterface {

    public static abstract class Stub extends Binder implements IBackupManager {
        private static final String DESCRIPTOR = "android.app.backup.IBackupManager";
        static final int TRANSACTION_acknowledgeFullBackupOrRestore = 17;
        static final int TRANSACTION_adbBackup = 14;
        static final int TRANSACTION_adbRestore = 16;
        static final int TRANSACTION_agentConnected = 4;
        static final int TRANSACTION_agentDisconnected = 5;
        static final int TRANSACTION_backupNow = 13;
        static final int TRANSACTION_beginRestoreSession = 29;
        static final int TRANSACTION_cancelBackups = 37;
        static final int TRANSACTION_clearBackupData = 2;
        static final int TRANSACTION_dataChanged = 1;
        static final int TRANSACTION_filterAppsEligibleForBackup = 35;
        static final int TRANSACTION_fullTransportBackup = 15;
        static final int TRANSACTION_getAvailableRestoreToken = 33;
        static final int TRANSACTION_getConfigurationIntent = 25;
        static final int TRANSACTION_getCurrentTransport = 19;
        static final int TRANSACTION_getDataManagementIntent = 27;
        static final int TRANSACTION_getDataManagementLabel = 28;
        static final int TRANSACTION_getDestinationString = 26;
        static final int TRANSACTION_getTransportWhitelist = 22;
        static final int TRANSACTION_hasBackupPassword = 12;
        static final int TRANSACTION_initializeTransports = 3;
        static final int TRANSACTION_isAppEligibleForBackup = 34;
        static final int TRANSACTION_isBackupEnabled = 10;
        static final int TRANSACTION_isBackupServiceActive = 32;
        static final int TRANSACTION_listAllTransportComponents = 21;
        static final int TRANSACTION_listAllTransports = 20;
        static final int TRANSACTION_opComplete = 30;
        static final int TRANSACTION_requestBackup = 36;
        static final int TRANSACTION_restoreAtInstall = 6;
        static final int TRANSACTION_selectBackupTransport = 23;
        static final int TRANSACTION_selectBackupTransportAsync = 24;
        static final int TRANSACTION_setAutoRestore = 8;
        static final int TRANSACTION_setBackupEnabled = 7;
        static final int TRANSACTION_setBackupPassword = 11;
        static final int TRANSACTION_setBackupProvisioned = 9;
        static final int TRANSACTION_setBackupServiceActive = 31;
        static final int TRANSACTION_updateTransportAttributes = 18;

        private static class Proxy implements IBackupManager {
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

            public void dataChanged(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearBackupData(String transportName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transportName);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void initializeTransports(String[] transportNames, IBackupObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(transportNames);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void agentConnected(String packageName, IBinder agent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(agent);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void agentDisconnected(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void restoreAtInstall(String packageName, int token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(token);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBackupEnabled(boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isEnabled);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAutoRestore(boolean doAutoRestore) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(doAutoRestore);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBackupProvisioned(boolean isProvisioned) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isProvisioned);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBackupEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(10, _data, _reply, 0);
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

            public boolean setBackupPassword(String currentPw, String newPw) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(currentPw);
                    _data.writeString(newPw);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
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

            public boolean hasBackupPassword() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(12, _data, _reply, 0);
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

            public void backupNow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void adbBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean allApps, boolean allIncludesSystem, boolean doCompress, boolean doKeyValue, String[] packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(includeApks);
                    _data.writeInt(includeObbs);
                    _data.writeInt(includeShared);
                    _data.writeInt(doWidgets);
                    _data.writeInt(allApps);
                    _data.writeInt(allIncludesSystem);
                    _data.writeInt(doCompress);
                    _data.writeInt(doKeyValue);
                    _data.writeStringArray(packageNames);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void fullTransportBackup(String[] packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packageNames);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void adbRestore(ParcelFileDescriptor fd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void acknowledgeFullBackupOrRestore(int token, boolean allow, String curPassword, String encryptionPassword, IFullBackupRestoreObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeInt(allow);
                    _data.writeString(curPassword);
                    _data.writeString(encryptionPassword);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateTransportAttributes(ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, String dataManagementLabel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (transportComponent != null) {
                        _data.writeInt(1);
                        transportComponent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(name);
                    if (configurationIntent != null) {
                        _data.writeInt(1);
                        configurationIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(currentDestinationString);
                    if (dataManagementIntent != null) {
                        _data.writeInt(1);
                        dataManagementIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(dataManagementLabel);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCurrentTransport() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] listAllTransports() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ComponentName[] listAllTransportComponents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    ComponentName[] _result = (ComponentName[]) _reply.createTypedArray(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getTransportWhitelist() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String selectBackupTransport(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void selectBackupTransportAsync(ComponentName transport, ISelectBackupTransportCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (transport != null) {
                        _data.writeInt(1);
                        transport.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Intent getConfigurationIntent(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Intent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Intent) Intent.CREATOR.createFromParcel(_reply);
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

            public String getDestinationString(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Intent getDataManagementIntent(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Intent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Intent) Intent.CREATOR.createFromParcel(_reply);
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

            public String getDataManagementLabel(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IRestoreSession beginRestoreSession(String packageName, String transportID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(transportID);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    IRestoreSession _result = android.app.backup.IRestoreSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void opComplete(int token, long result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeLong(result);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBackupServiceActive(int whichUser, boolean makeActive) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(whichUser);
                    _data.writeInt(makeActive);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBackupServiceActive(int whichUser) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(whichUser);
                    boolean z = false;
                    this.mRemote.transact(32, _data, _reply, 0);
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

            public long getAvailableRestoreToken(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isAppEligibleForBackup(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(34, _data, _reply, 0);
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

            public String[] filterAppsEligibleForBackup(String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packages);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packages);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    if (monitor != null) {
                        iBinder = monitor.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(flags);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelBackups() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
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

        public static IBackupManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IBackupManager)) {
                return new Proxy(obj);
            }
            return (IBackupManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                ParcelFileDescriptor _arg0 = null;
                boolean _arg8 = false;
                boolean _result;
                boolean _result2;
                ComponentName _arg02;
                String[] _result3;
                String _result4;
                Intent _result5;
                boolean _result6;
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        dataChanged(data.readString());
                        reply.writeNoException();
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        clearBackupData(data.readString(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        initializeTransports(data.createStringArray(), android.app.backup.IBackupObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return z;
                    case 4:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        agentConnected(data.readString(), data.readStrongBinder());
                        reply.writeNoException();
                        return z;
                    case 5:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        agentDisconnected(data.readString());
                        reply.writeNoException();
                        return z;
                    case 6:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        restoreAtInstall(data.readString(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 7:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg8 = z;
                        }
                        setBackupEnabled(_arg8);
                        reply.writeNoException();
                        return z;
                    case 8:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg8 = z;
                        }
                        setAutoRestore(_arg8);
                        reply.writeNoException();
                        return z;
                    case 9:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg8 = z;
                        }
                        setBackupProvisioned(_arg8);
                        reply.writeNoException();
                        return z;
                    case 10:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = isBackupEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 11:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result2 = setBackupPassword(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return z;
                    case 12:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = hasBackupPassword();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 13:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        backupNow();
                        reply.writeNoException();
                        return z;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        ParcelFileDescriptor _arg03 = _arg0;
                        _result2 = data.readInt() != 0;
                        boolean _arg2 = data.readInt() != 0;
                        boolean _arg3 = data.readInt() != 0;
                        boolean _arg4 = data.readInt() != 0;
                        boolean _arg5 = data.readInt() != 0;
                        boolean _arg6 = data.readInt() != 0;
                        boolean _arg7 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg8 = true;
                        }
                        z = true;
                        adbBackup(_arg03, _result2, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, data.createStringArray());
                        reply.writeNoException();
                        return z;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        fullTransportBackup(data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        adbRestore(_arg0);
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        acknowledgeFullBackupOrRestore(data.readInt(), data.readInt() != 0, data.readString(), data.readString(), android.app.backup.IFullBackupRestoreObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 18:
                        ComponentName _arg04;
                        Intent _arg22;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg04 = null;
                        }
                        String _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        String _arg32 = data.readString();
                        if (data.readInt() != 0) {
                            _arg02 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        updateTransportAttributes(_arg04, _arg1, _arg22, _arg32, _arg02, data.readString());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        String _result7 = getCurrentTransport();
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _result3 = listAllTransports();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result3);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        ComponentName[] _result8 = listAllTransportComponents();
                        reply.writeNoException();
                        parcel2.writeTypedArray(_result8, 1);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _result3 = getTransportWhitelist();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result3);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result4 = selectBackupTransport(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result4);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        selectBackupTransportAsync(_arg02, android.app.backup.ISelectBackupTransportCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result5 = getConfigurationIntent(data.readString());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _result4 = getDestinationString(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result4);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        _result5 = getDataManagementIntent(data.readString());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _result4 = getDataManagementLabel(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result4);
                        return true;
                    case 29:
                        IBinder _arg05;
                        parcel.enforceInterface(descriptor);
                        IRestoreSession _result9 = beginRestoreSession(data.readString(), data.readString());
                        reply.writeNoException();
                        if (_result9 != null) {
                            _arg05 = _result9.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg05);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        opComplete(data.readInt(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        int _arg06 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg8 = true;
                        }
                        setBackupServiceActive(_arg06, _arg8);
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        _result6 = isBackupServiceActive(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        long _result10 = getAvailableRestoreToken(data.readString());
                        reply.writeNoException();
                        parcel2.writeLong(_result10);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _result6 = isAppEligibleForBackup(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        String[] _result11 = filterAppsEligibleForBackup(data.createStringArray());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result11);
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        int _result12 = requestBackup(data.createStringArray(), android.app.backup.IBackupObserver.Stub.asInterface(data.readStrongBinder()), android.app.backup.IBackupManagerMonitor.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result12);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        cancelBackups();
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    void acknowledgeFullBackupOrRestore(int i, boolean z, String str, String str2, IFullBackupRestoreObserver iFullBackupRestoreObserver) throws RemoteException;

    void adbBackup(ParcelFileDescriptor parcelFileDescriptor, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, boolean z8, String[] strArr) throws RemoteException;

    void adbRestore(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void agentConnected(String str, IBinder iBinder) throws RemoteException;

    void agentDisconnected(String str) throws RemoteException;

    void backupNow() throws RemoteException;

    IRestoreSession beginRestoreSession(String str, String str2) throws RemoteException;

    void cancelBackups() throws RemoteException;

    void clearBackupData(String str, String str2) throws RemoteException;

    void dataChanged(String str) throws RemoteException;

    String[] filterAppsEligibleForBackup(String[] strArr) throws RemoteException;

    void fullTransportBackup(String[] strArr) throws RemoteException;

    long getAvailableRestoreToken(String str) throws RemoteException;

    Intent getConfigurationIntent(String str) throws RemoteException;

    String getCurrentTransport() throws RemoteException;

    Intent getDataManagementIntent(String str) throws RemoteException;

    String getDataManagementLabel(String str) throws RemoteException;

    String getDestinationString(String str) throws RemoteException;

    String[] getTransportWhitelist() throws RemoteException;

    boolean hasBackupPassword() throws RemoteException;

    void initializeTransports(String[] strArr, IBackupObserver iBackupObserver) throws RemoteException;

    boolean isAppEligibleForBackup(String str) throws RemoteException;

    boolean isBackupEnabled() throws RemoteException;

    boolean isBackupServiceActive(int i) throws RemoteException;

    ComponentName[] listAllTransportComponents() throws RemoteException;

    String[] listAllTransports() throws RemoteException;

    void opComplete(int i, long j) throws RemoteException;

    int requestBackup(String[] strArr, IBackupObserver iBackupObserver, IBackupManagerMonitor iBackupManagerMonitor, int i) throws RemoteException;

    void restoreAtInstall(String str, int i) throws RemoteException;

    String selectBackupTransport(String str) throws RemoteException;

    void selectBackupTransportAsync(ComponentName componentName, ISelectBackupTransportCallback iSelectBackupTransportCallback) throws RemoteException;

    void setAutoRestore(boolean z) throws RemoteException;

    void setBackupEnabled(boolean z) throws RemoteException;

    boolean setBackupPassword(String str, String str2) throws RemoteException;

    void setBackupProvisioned(boolean z) throws RemoteException;

    void setBackupServiceActive(int i, boolean z) throws RemoteException;

    void updateTransportAttributes(ComponentName componentName, String str, Intent intent, String str2, Intent intent2, String str3) throws RemoteException;
}
